package com.agtech.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agtech.nas.ui.theme.*
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebContent
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import dev.datlag.kcef.KCEFBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

/**
 * Navegador embebido (Chromium/CEF). Abre los accesos DENTRO de la app,
 * rellena el formulario de login automáticamente y vuelve a «Accesos».
 */
@Composable
fun WebViewScreen(state: AppState) {
    val target = state.webTarget
    if (target == null) {
        LaunchedEffect(Unit) { state.navigate(Screen.APPS) }
        return
    }
    key(target.appId, target.url) {
        WebContent(state, target)
    }
}

@Composable
private fun WebContent(state: AppState, target: WebTarget) {
    val engine by BrowserEngine.status.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { BrowserEngine.ensureReady(scope) }

    val ws = rememberWebViewState(target.url) {
        isJavaScriptEnabled = true
        supportZoom = true
        desktopWebSettings.disablePopupWindows = false
    }
    val navigator = rememberWebViewNavigator()

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        // ------------------------------------------------------------ barra
        Surface(color = Navy800, tonalElevation = 4.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = { state.closeWeb() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, "Volver a Accesos", tint = Cyan400, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        ws.pageTitle?.takeIf { it.isNotBlank() } ?: target.name,
                        style = MaterialTheme.typography.titleSmall, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        ws.lastLoadedUrl ?: target.url,
                        style = MaterialTheme.typography.labelSmall, color = TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (engine.ready && ws.loadingState !is LoadingState.Finished) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Cyan400)
                    Spacer(Modifier.width(6.dp))
                }
                if (state.hasCredential(target.appId)) {
                    Icon(Icons.Filled.Lock, "Acceso automático activo", tint = Mint300, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = { if (engine.ready) navigator.reload() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Refresh, "Recargar", tint = Cyan400, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { runCatching { Desktop.getDesktop().browse(URI(ws.lastLoadedUrl ?: target.url)) } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.OpenInNew, "Abrir en navegador externo", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ------------------------------------------------------------ contenido
        Box(Modifier.fillMaxSize()) {
            when {
                engine.error != null -> BrowserError(engine.error!!, onRetry = {
                    BrowserEngine.ensureReady(scope)
                }, onExternal = {
                    runCatching { Desktop.getDesktop().browse(URI(target.url)) }
                })

                !engine.ready -> BrowserLoading(engine)

                else -> EmbeddedBrowser(state, target, ws, navigator)
            }
        }
    }
}

@Composable
private fun EmbeddedBrowser(
    state: AppState,
    target: WebTarget,
    ws: WebViewState,
    navigator: WebViewNavigator
) {
    // Auto-login: al terminar cada carga, inyecta las credenciales (si hay).
    LaunchedEffect(ws, target.appId) {
        var lastInjected: String? = null
        snapshotFlow { ws.loadingState to ws.lastLoadedUrl }
            .map { it }
            .distinctUntilChanged()
            .collect { (loading, url) ->
                if (loading is LoadingState.Finished && !url.isNullOrBlank() && url != lastInjected) {
                    val cred = state.credentialFor(target.appId)
                    if (cred != null && state.autoLoginEnabled) {
                        withContext(Dispatchers.Main) {
                            navigator.evaluateJavaScript(buildAutoLoginJs(cred.login, cred.password), null)
                        }
                        lastInjected = url
                    }
                }
            }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        WebView(
            state = ws,
            modifier = Modifier.fillMaxSize(),
            navigator = navigator,
            factory = { param ->
                // Fábrica propia: crea el navegador sin contexto de petición
                // modificado. Así evitamos el NPE de `createContext` cuando el
                // contexto global de CEF todavía no está disponible.
                val url = (param.state.content as? WebContent.Url)?.url ?: KCEFBrowser.BLANK_URI
                param.client.createBrowser(url, param.rendering, param.transparent)
            }
        )
    }
}

@Composable
private fun BrowserLoading(engine: BrowserStatus) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Cyan400)
        Spacer(Modifier.height(16.dp))
        Text(
            engine.stage.ifBlank { "Iniciando navegador…" },
            style = MaterialTheme.typography.bodyMedium, color = TextPrimary
        )
        if (engine.progress >= 0f) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(280.dp)) { ProgressBar(engine.progress, Cyan400) }
            Spacer(Modifier.height(6.dp))
            Text(
                "La primera vez se descarga Chromium (~150 MB). Solo ocurre una vez.",
                style = MaterialTheme.typography.labelSmall, color = TextMuted
            )
        }
    }
}

@Composable
private fun BrowserError(message: String, onRetry: () -> Unit, onExternal: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No se pudo abrir el navegador integrado", style = MaterialTheme.typography.titleMedium, color = Danger)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRetry) { Text("Reintentar") }
            OutlinedButton(onClick = onExternal) { Text("Abrir en navegador externo") }
        }
    }
}

// ------------------------------------------------------------------ auto-login JS
private fun jsQuote(value: String): String = buildString {
    append('"')
    for (c in value) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}

/**
 * Rellena el formulario de login de la SPA y lo envía. Es genérico:
 *  - busca el input de contraseña visible y el primer usuario/email visible,
 *  - usa el setter nativo para que React/Vue detecten el cambio,
 *  - hace clic en el botón de envío,
 *  - si la página ya está autenticada (no hay password) no hace nada.
 */
fun buildAutoLoginJs(login: String, password: String): String {
    val l = jsQuote(login)
    val p = jsQuote(password)
    return """
        (function(){
          var L=$l, P=$p, tries=0;
          if (window.__agtechAutoLogin) return 'already';
          function visible(el){
            if(!el) return false;
            var r=el.getBoundingClientRect();
            return el.offsetParent!==null && r.width>0 && r.height>0;
          }
          function set(el,v){
            try {
              var proto = el.tagName === 'TEXTAREA'
                ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
              var d = Object.getOwnPropertyDescriptor(proto,'value');
              if(d && d.set) d.set.call(el,v); else el.value=v;
              el.dispatchEvent(new Event('input',{bubbles:true}));
              el.dispatchEvent(new Event('change',{bubbles:true}));
              el.dispatchEvent(new Event('blur',{bubbles:true}));
              return true;
            } catch(e){ return false; }
          }
          function findButton(pass){
            var re=/^(login|log ?in|sign ?in|entrar|iniciar( sesi[oó]n)?|acceder|continuar|continue|submit|next|siguiente|ok)$/i;
            var all=[].slice.call(document.querySelectorAll('button,input[type=submit],[role=button]')).filter(visible);
            var byText=all.filter(function(b){
              var t=(b.innerText||b.value||b.getAttribute('aria-label')||'').trim();
              return re.test(t);
            });
            if(byText.length) return byText[0];
            var form=pass.closest('form');
            if(form){
              var inForm=all.filter(function(b){ return form.contains(b); });
              if(inForm.length) return inForm[inForm.length-1];
            }
            // El botón visible más cercano por debajo del campo de contraseña.
            var pr=pass.getBoundingClientRect(), best=null, bd=1e18;
            all.forEach(function(b){
              var r=b.getBoundingClientRect();
              if(r.top < pr.bottom - 4) return;
              var d=(r.top-pr.bottom)+Math.abs((r.left+r.width/2)-(pr.left+pr.width/2));
              if(d<bd){bd=d;best=b;}
            });
            return best;
          }
          var timer=setInterval(function(){
            var pass=[].slice.call(document.querySelectorAll('input[type=password]')).filter(visible)[0];
            if(pass){
              var cands=[].slice.call(document.querySelectorAll('input')).filter(function(i){
                if(i===pass || !visible(i)) return false;
                var t=(i.type||'text').toLowerCase();
                return t==='email'||t==='text'||t==='tel'||
                  (i.autocomplete||'').toLowerCase()==='username'||
                  /user|email|login|account|usuario|correo/i.test(i.name||i.id||'');
              });
              if(cands.length){
                set(cands[0],L); set(pass,P); clearInterval(timer);
                window.__agtechAutoLogin=true;
                setTimeout(function(){
                  var form=pass.closest('form');
                  var btn=findButton(pass);
                  if(btn) btn.click();
                  else if(form && form.requestSubmit) form.requestSubmit();
                  else if(pass.form) pass.form.submit();
                }, 800);
              }
            }
            if(++tries>25) clearInterval(timer);
          }, 400);
        })();
    """.trimIndent()
}
