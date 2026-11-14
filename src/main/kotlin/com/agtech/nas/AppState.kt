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

enum class Screen { LOGIN, HOME, FILES, CONTAINERS, APPS, SETTINGS, DETAIL, MEDIA }

data class FilesState(
    val root: String = "Karen",
    val path: String = "/",
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
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

                val s = sys.await(); val d = dsk.await(); val a = aps.await()
                val c = cnt.await(); val e = evt.await(); val v = svc.await()

                s?.let { if (it.isSuccessful) system = it.body() else error = "HTTP ${it.code()}" }
                d?.let { if (it.isSuccessful) disks = it.body() }
                a?.let { if (it.isSuccessful) apps = it.body() }
                c?.let { if (it.isSuccessful) containers = it.body() }
                e?.let { if (it.isSuccessful) events = it.body() }
                v?.let { if (it.isSuccessful) services = it.body() }
            } catch (e: Exception) {
                error = "Error de red: ${e.message}"
            } finally { loading = false }
        }
    }

    fun loadFiles(root: String = filesState.root, path: String = filesState.path) {
        filesState = filesState.copy(root = root, path = path, loading = true, error = null)
        scope.launch {
            try {
                val api = client.api()
                val r = withContext(Dispatchers.IO) { api.files(root, path) }
                if (r.isSuccessful) {
                    filesState = FilesState(root, path, r.body()?.items ?: emptyList(), false, null)
                } else {
                    filesState = filesState.copy(loading = false, error = "HTTP ${r.code()}")
                }
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
        screen = Screen.LOGIN
        postNotice("Sesión cerrada")
    }
}
