package com.agtech.nas

import dev.datlag.kcef.KCEF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Estado de arranque del motor Chromium (CEF). La primera vez descarga el
 * paquete (~150 MB) en la carpeta de datos del usuario; después arranca en
 * caliente. Se comparte entre todas las pestañas web.
 */
data class BrowserStatus(
    val ready: Boolean = false,
    val initializing: Boolean = false,
    val progress: Float = -1f,
    val stage: String = "",
    val error: String? = null
)

object BrowserEngine {

    private val _status = MutableStateFlow(BrowserStatus())
    val status: StateFlow<BrowserStatus> = _status.asStateFlow()

    @Volatile
    private var started = false

    /** Carpeta escribible para CEF (funciona también si la app está en Program Files). */
    fun dataDir(): File {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val base = if (os.contains("win")) {
            File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "AGTech")
        } else {
            File(System.getProperty("user.home"), ".agtech")
        }
        return base.apply { mkdirs() }
    }

    fun ensureReady(scope: CoroutineScope) {
        val s = _status.value
        if (s.ready || s.initializing || started) return
        started = true
        _status.value = BrowserStatus(initializing = true, stage = "Preparando navegador…")

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    KCEF.init(
                        builder = {
                            installDir(File(dataDir(), "cef-bundle"))
                            progress {
                                onLocating {
                                    _status.update { it.copy(stage = "Localizando paquetes de Chromium…") }
                                }
                                onDownloading { p ->
                                    _status.update {
                                        it.copy(
                                            progress = p,
                                            stage = "Descargando Chromium… ${(p * 100).toInt()}%"
                                        )
                                    }
                                }
                                onExtracting {
                                    _status.update { it.copy(stage = "Extrayendo Chromium…") }
                                }
                                onInstall {
                                    _status.update { it.copy(stage = "Instalando Chromium…") }
                                }
                                onInitializing {
                                    _status.update { it.copy(stage = "Iniciando motor web…") }
                                }
                                onInitialized { }
                            }
                        },
                        onError = { e ->
                            started = false
                            _status.update {
                                it.copy(
                                    ready = false,
                                    initializing = false,
                                    error = e?.message ?: "No se pudo iniciar el navegador"
                                )
                            }
                        },
                        onRestartRequired = {
                            started = false
                            _status.update {
                                it.copy(
                                    ready = false,
                                    initializing = false,
                                    error = "Reinicia la aplicación para activar el navegador"
                                )
                            }
                        }
                    )
                }
                // Dar un margen a CEF para terminar la inicialización nativa antes
                // de crear el primer contexto/browser (evita una carrera en el
                // primer arranque tras la descarga).
                kotlinx.coroutines.delay(900)
                _status.update {
                    it.copy(ready = true, initializing = false, progress = 1f, stage = "Navegador listo", error = null)
                }
            } catch (e: Throwable) {
                started = false
                _status.update {
                    it.copy(
                        ready = false,
                        initializing = false,
                        error = "No se pudo iniciar el navegador: ${e.message}"
                    )
                }
            }
        }
    }
}
