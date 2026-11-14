package com.agtech.nas

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

// Se puede sobrescribir con -Dagtech.version=x.y.z (útil para probar el actualizador).
val APP_VERSION: String = System.getProperty("agtech.version")?.takeIf { it.isNotBlank() } ?: "1.1.0"

data class UpdateInfo(
    val available: Boolean = false,
    val currentVersion: String = APP_VERSION,
    val latestVersion: String = "",
    val notes: String = "",
    val htmlUrl: String = "",
    val assetName: String = "",
    val assetUrl: String = "",
    val assetSize: Long = 0L
)

/**
 * Comprobación y descarga de actualizaciones desde GitHub Releases
 * (sin abrir el navegador: todo dentro de Ajustes).
 */
object Updater {

    private const val REPO = "GhostShiftDev/agtech-desktop"
    private val apiUrl = "https://api.github.com/repos/$REPO/releases/latest"

    private val moshi: Moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter(Map::class.java)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun clean(v: String) = v.trim().removePrefix("v").removePrefix("V")

    fun compare(a: String, b: String): Int {
        val pa = clean(a).split('.', '-', '+')
        val pb = clean(b).split('.', '-', '+')
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrNull(i)?.toIntOrNull() ?: 0
            val y = pb.getOrNull(i)?.toIntOrNull() ?: 0
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }

    suspend fun check(current: String = APP_VERSION): Result<UpdateInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AGTech-Desktop/$current")
                .build()

            http.newCall(req).execute().use { resp ->
                if (resp.code == 404) {
                    // Todavía no hay ninguna release publicada.
                    return@withContext UpdateInfo(
                        available = false,
                        currentVersion = current,
                        latestVersion = current,
                        notes = "Aún no hay actualizaciones publicadas."
                    )
                }
                if (!resp.isSuccessful) error("GitHub respondió HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                @Suppress("UNCHECKED_CAST")
                val json = mapAdapter.fromJson(body) as? Map<String, Any?>
                    ?: error("Respuesta de GitHub no válida")

                val tag = clean(json["tag_name"] as? String ?: "")
                val notes = (json["body"] as? String).orEmpty()
                val html = (json["html_url"] as? String).orEmpty()

                @Suppress("UNCHECKED_CAST")
                val assets = (json["assets"] as? List<Any?>).orEmpty()
                    .mapNotNull { it as? Map<String, Any?> }

                val wanted = assets.firstOrNull { (it["name"] as? String).orEmpty().endsWith(".msi", true) }
                    ?: assets.firstOrNull { (it["name"] as? String).orEmpty().endsWith(".exe", true) }
                    ?: assets.firstOrNull { (it["name"] as? String).orEmpty().endsWith(".deb", true) }
                    ?: assets.firstOrNull()

                val name = (wanted?.get("name") as? String).orEmpty()
                val url = (wanted?.get("browser_download_url") as? String).orEmpty()
                val size = ((wanted?.get("size") as? Number)?.toLong()) ?: 0L

                UpdateInfo(
                    available = tag.isNotBlank() && compare(tag, current) > 0,
                    currentVersion = current,
                    latestVersion = tag.ifBlank { current },
                    notes = notes,
                    htmlUrl = html,
                    assetName = name,
                    assetUrl = url,
                    assetSize = size
                )
            }
        }
    }

    suspend fun download(
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        require(info.assetUrl.isNotBlank()) { "La release no tiene instalador adjunto" }

        val downloads = File(System.getProperty("user.home"), "Downloads").let {
            if (it.isDirectory || it.mkdirs()) it else File(System.getProperty("user.home"))
        }
        val target = File(downloads, info.assetName.ifBlank { "AGTech-${info.latestVersion}.msi" })
        val part = File(target.parentFile, target.name + ".part")

        val req = Request.Builder()
            .url(info.assetUrl)
            .header("User-Agent", "AGTech-Desktop/${info.currentVersion}")
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Descarga fallida: HTTP ${resp.code}")
            val body = resp.body ?: error("Respuesta vacía")
            val total = body.contentLength().takeIf { it > 0 } ?: info.assetSize
            var done = 0L
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toDouble() / total).toFloat().coerceIn(0f, 1f))
                    }
                }
            }
        }
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
        onProgress(1f)
        target
    }
}
