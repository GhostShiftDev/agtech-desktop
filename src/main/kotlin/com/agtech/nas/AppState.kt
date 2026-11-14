package com.agtech.nas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import retrofit2.Response

enum class Screen { LOGIN, HOME, FILES, CONTAINERS, APPS, SETTINGS, DETAIL, MEDIA, WEB }

/** Raíces de archivos válidas en nas-api (Sistema no es una raíz de ficheros). */
val FILE_ROOTS = listOf("Grabaciones", "Karen")

data class FilesState(
    val root: String = "Grabaciones",
    val path: String = "/",
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/** Acceso que se está mostrando en el navegador embebido. */
data class WebTarget(
    val appId: String,
    val name: String,
    val url: String
)

data class DetailState(
    val id: String = "",
    val name: String = "",
    val stats: ContainerStats? = null,
    val logs: List<String> = emptyList(),
    val working: Boolean = false,
    val error: String? = null
)

/**
 * Estado central de la aplicación de escritorio. Equivalente a NasViewModel
 * del módulo Android, sin dependencias de Android.
 */
class AppState {

    val config = SecureConfig()
    private val client = NasClient(config)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    var screen by mutableStateOf(if (config.isPaired()) Screen.HOME else Screen.LOGIN)
        private set

    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)

    var system by mutableStateOf<SystemInfo?>(null); private set
    var disks by mutableStateOf<DisksResponse?>(null); private set
    var apps by mutableStateOf<AppsResponse?>(null); private set
    var containers by mutableStateOf<ContainersResponse?>(null); private set
    var services by mutableStateOf<ServicesStatusResponse?>(null); private set
    var events by mutableStateOf<EventsResponse?>(null); private set
    var detail by mutableStateOf(DetailState())

    var filesState by mutableStateOf(FilesState()); private set

    // Navegador embebido + credenciales para auto-login
    var webTarget by mutableStateOf<WebTarget?>(null); private set
    var autoLoginEnabled by mutableStateOf(true)
    var credentials by mutableStateOf<Map<String, AppCredential>>(emptyMap()); private set
    var useNasCredentialsForAll by mutableStateOf(true)
    var nasPasswordSet by mutableStateOf(config.loginPassword().isNotBlank()); private set

    // Actualizaciones
    var updateInfo by mutableStateOf<UpdateInfo?>(null); private set
    var updateChecking by mutableStateOf(false); private set
    var updateError by mutableStateOf<String?>(null)
    var downloadingUpdate by mutableStateOf(false); private set
    var downloadProgress by mutableStateOf(0f); private set
    var downloadedUpdate by mutableStateOf<java.io.File?>(null); private set

    init {
        if (config.isPaired()) refresh()
    }

    private fun <T> safe(r: Response<T>): T? {
        if (r.isSuccessful) return r.body()
        val raw = runCatching { r.errorBody()?.string() }.getOrNull().orEmpty()
        // Extrae "detail":"..." sin dependencia de org.json (no está en JVM estándar).
        val detail = Regex("\"detail\"\\s*:\\s*\"(.*?)\"").find(raw)?.groupValues?.get(1)
        error = "HTTP ${r.code()} ${detail ?: r.message()}"
        return null
    }

    fun navigate(s: Screen) { screen = s }
    fun postNotice(msg: String) { notice = msg; error = null }
    fun clearMessages() { notice = null; error = null }

    fun login(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) { error = "Usuario y contraseña son obligatorios"; return }
        loading = true; error = null
        scope.launch {
            try {
                val api = client.api()
                val res = withContext(Dispatchers.IO) { api.auth(AuthRequest(user.trim(), pass)) }
                val body = safe(res)
                if (body != null) {
                    config.saveSession(body.token, body.service_token, body.user)
                    config.saveLoginPassword(pass)
                    nasPasswordSet = true
                    postNotice("Sesión iniciada ✔")
                    screen = Screen.HOME
                    refresh()
                }
            } catch (e: Exception) {
                error = "No se pudo iniciar sesión: ${e.message}"
            } finally { loading = false }
        }
    }

    fun pair(code: String) {
        if (code.isBlank()) { error = "Introduce el código de emparejamiento"; return }
        loading = true; error = null
        scope.launch {
            try {
                val api = client.api()
                val res = withContext(Dispatchers.IO) { api.pair(PairRequest(code.trim())) }
                val body = safe(res)
                if (body != null) {
                    config.saveSession(body.token, body.service_token, body.user)
                    postNotice("Dispositivo vinculado ✔")
                    screen = Screen.HOME
                    refresh()
                }
            } catch (e: Exception) {
                error = "No se pudo vincular: ${e.message}"
            } finally { loading = false }
        }
    }

    fun saveBaseUrl(url: String) {
        config.saveBaseUrl(url)
        postNotice("URL guardada: ${url.trim()}")
    }

    fun refresh() {
        if (!config.isPaired()) { screen = Screen.LOGIN; return }
        loading = true; error = null
        scope.launch {
            try {
                val api = client.api(config.baseUrl())
                val sys = async { withContext(Dispatchers.IO) { runCatching { api.system() }.getOrNull() } }
                val dsk = async { withContext(Dispatchers.IO) { runCatching { api.disks() }.getOrNull() } }
                val aps = async { withContext(Dispatchers.IO) { runCatching { api.apps() }.getOrNull() } }
                val cnt = async { withContext(Dispatchers.IO) { runCatching { api.containers() }.getOrNull() } }
                val evt = async { withContext(Dispatchers.IO) { runCatching { api.events() }.getOrNull() } }
                val svc = async { withContext(Dispatchers.IO) { runCatching { api.appsStatus() }.getOrNull() } }
                val crd = async { withContext(Dispatchers.IO) { runCatching { api.credentials() }.getOrNull() } }

                val s = sys.await(); val d = dsk.await(); val a = aps.await()
                val c = cnt.await(); val e = evt.await(); val v = svc.await()
                val k = crd.await()

                s?.let { if (it.isSuccessful) system = it.body() else error = "HTTP ${it.code()}" }
                d?.let { if (it.isSuccessful) disks = it.body() }
                a?.let { if (it.isSuccessful) apps = it.body() }
                c?.let { if (it.isSuccessful) containers = it.body() }
                e?.let { if (it.isSuccessful) events = it.body() }
                v?.let { if (it.isSuccessful) services = it.body() }
                k?.let { if (it.isSuccessful) credentials = mergeCredentials(it.body()?.credentials ?: emptyMap()) }
            } catch (e: Exception) {
                error = "Error de red: ${e.message}"
            } finally { loading = false }
        }
    }

    fun loadFiles(root: String = filesState.root, path: String = filesState.path) {
        val safeRoot = if (root in FILE_ROOTS) root else "Grabaciones"
        filesState = filesState.copy(root = safeRoot, path = path, loading = true, error = null)
        scope.launch {
            try {
                val api = client.api()
                val r = withContext(Dispatchers.IO) { api.files(safeRoot, path) }
                if (r.isSuccessful) {
                    filesState = FilesState(safeRoot, path, r.body()?.items ?: emptyList(), false, null)
                    return@launch
                }

                // 503 = disco desconectado (Karen suele estarlo). 400 = raíz no válida.
                if ((r.code() == 503 || r.code() == 400) && safeRoot != "Grabaciones") {
                    val alt = withContext(Dispatchers.IO) { api.files("Grabaciones", "/") }
                    if (alt.isSuccessful) {
                        filesState = FilesState(
                            "Grabaciones", "/", alt.body()?.items ?: emptyList(), false,
                            "«$safeRoot» no está disponible (HTTP ${r.code()}). Mostrando Grabaciones."
                        )
                        return@launch
                    }
                }

                val raw = runCatching { r.errorBody()?.string() }.getOrNull().orEmpty()
                val detail = Regex("\"detail\"\\s*:\\s*\"(.*?)\"").find(raw)?.groupValues?.get(1)
                filesState = filesState.copy(loading = false, error = "HTTP ${r.code()} ${detail.orEmpty()}".trim())
            } catch (e: Exception) {
                filesState = filesState.copy(loading = false, error = e.message)
            }
        }
    }

    fun filesUp() {
        val cur = filesState.path.trimEnd('/')
        val parent = cur.substringBeforeLast('/', "").ifBlank { "/" }
        loadFiles(filesState.root, parent)
    }

    fun setFilesRoot(root: String) {
        loadFiles(root, "/")
    }

    // ---------------------------------------------------------- accesos web
    private fun mergeCredentials(server: Map<String, AppCredential>): Map<String, AppCredential> {
        val local = config.appCredentials()
        val out = mutableMapOf<String, AppCredential>()
        (server.keys + local.keys).forEach { id ->
            val s = server[id]
            val l = local[id]
            val login = l?.login?.takeIf { it.isNotBlank() } ?: s?.login.orEmpty()
            val pass = l?.password?.takeIf { it.isNotBlank() } ?: s?.password.orEmpty()
            if (login.isNotBlank() && pass.isNotBlank()) out[id] = AppCredential(login, pass)
        }
        return out
    }

    /** Credencial que se inyectará en el formulario del acceso, si existe. */
    fun credentialFor(appId: String): AppCredential? {
        val explicit = config.appCredentials()[appId]
            ?: credentials[appId]
        if (explicit != null && explicit.login.isNotBlank() && explicit.password.isNotBlank()) {
            return explicit
        }
        if (useNasCredentialsForAll) {
            val user = config.user().ifBlank { "AxelGE" }
            val pass = config.loginPassword()
            if (pass.isNotBlank()) return AppCredential(user, pass)
        }
        return null
    }

    fun hasCredential(appId: String): Boolean = credentialFor(appId) != null

    fun saveAppCredential(appId: String, login: String, password: String) {
        config.saveAppCredential(appId, login, password)
        credentials = mergeCredentials(credentials)
        postNotice("Credenciales guardadas para $appId ✔")
    }

    fun saveNasPassword(password: String) {
        if (password.isBlank()) { error = "La contraseña no puede estar vacía"; return }
        config.saveLoginPassword(password)
        nasPasswordSet = true
        postNotice("Contraseña del NAS guardada para el autorrelleno ✔")
    }

    fun removeAppCredential(appId: String) {
        config.removeAppCredential(appId)
        credentials = credentials - appId
        postNotice("Credenciales eliminadas para $appId")
    }

    fun openApp(app: AppItem) {
        val raw = (app.wan ?: app.lan).orEmpty().trim()
        if (raw.isBlank()) {
            postNotice("«${app.name}» no tiene URL pública configurada")
            return
        }
        val url = if (raw.startsWith("http", true)) raw else "https://$raw"
        webTarget = WebTarget(app.id, app.name, url)
        screen = Screen.WEB
    }

    fun closeWeb() {
        webTarget = null
        screen = Screen.APPS
    }

    // ---------------------------------------------------------- actualizaciones
    fun checkUpdates() {
        if (updateChecking) return
        updateChecking = true
        updateError = null
        scope.launch {
            val result = Updater.check(APP_VERSION)
            result.onSuccess { updateInfo = it }
                .onFailure { updateError = it.message ?: "No se pudo comprobar la actualización" }
            updateChecking = false
        }
    }

    fun downloadUpdate() {
        val info = updateInfo ?: return
        if (downloadingUpdate) return
        if (info.assetUrl.isBlank()) {
            updateError = "La release no tiene instalador adjunto (${info.latestVersion})"
            return
        }
        downloadingUpdate = true
        downloadProgress = 0f
        updateError = null
        downloadedUpdate = null
        scope.launch {
            try {
                val file = Updater.download(info) { p ->
                    scope.launch(Dispatchers.Swing) { downloadProgress = p }
                }
                downloadedUpdate = file
                postNotice("Actualización descargada: ${file.name}")
            } catch (e: Exception) {
                updateError = "Descarga fallida: ${e.message}"
            } finally {
                downloadingUpdate = false
            }
        }
    }

    fun openDownloadedUpdate() {
        val f = downloadedUpdate ?: return
        runCatching {
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(f)
            else postNotice("Instalador en: ${f.absolutePath}")
        }.onFailure { postNotice("Instalador en: ${f.absolutePath}") }
    }

    fun openDownloadFolder() {
        val f = downloadedUpdate ?: return
        runCatching {
            val dir = f.parentFile
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(dir)
        }
    }

    fun openContainer(c: Container) {
        detail = DetailState(id = c.id, name = c.name, working = true)
        screen = Screen.DETAIL
        scope.launch {
            try {
                val api = client.api()
                val st = async { withContext(Dispatchers.IO) { runCatching { api.stats(c.id) }.getOrNull() } }
                val lg = async { withContext(Dispatchers.IO) { runCatching { api.logs(c.id, 200) }.getOrNull() } }
                val s = st.await(); val l = lg.await()
                detail = detail.copy(
                    stats = s?.takeIf { it.isSuccessful }?.body(),
                    logs = l?.takeIf { it.isSuccessful }?.body()?.lines ?: emptyList(),
                    working = false
                )
            } catch (e: Exception) {
                detail = detail.copy(working = false, error = e.message)
            }
        }
    }

    fun containerAction(cid: String, action: String) {
        detail = detail.copy(working = true, error = null)
        scope.launch {
            try {
                val api = client.api()
                val r = withContext(Dispatchers.IO) { api.action(cid, action) }
                if (r.isSuccessful) { postNotice("Acción '$action' enviada"); refresh() }
                else detail = detail.copy(working = false, error = "HTTP ${r.code()}")
            } catch (e: Exception) {
                detail = detail.copy(working = false, error = e.message)
            }
        }
    }

    fun logout() {
        config.clearSession()
        system = null; disks = null; apps = null; containers = null; services = null; events = null
        detail = DetailState()
        webTarget = null
        credentials = emptyMap()
        screen = Screen.LOGIN
        postNotice("Sesión cerrada")
    }
}
