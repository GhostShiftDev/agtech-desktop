package com.agtech.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agtech.nas.ui.theme.*
import java.awt.Desktop
import java.net.URI
import java.util.Locale

// ------------------------------------------------------------------ formato
fun fmtBytes(v: Double): String {
    val gb = v / 1_000_000_000.0
    return when {
        gb >= 1000.0 -> String.format(Locale.US, "%.2f TB", gb / 1000.0)
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        else -> String.format(Locale.US, "%.0f MB", v / 1_000_000.0)
    }
}

fun fmtPercent(v: Double) = String.format(Locale.US, "%.1f%%", v)

fun fmtUptime(sec: Long): String {
    val d = sec / 86400
    val h = (sec % 86400) / 3600
    val m = (sec % 3600) / 60
    return if (d > 0) "${d}d ${h}h ${m}m" else "${h}h ${m}m"
}

fun barColor(p: Double): Color = when {
    p >= 90.0 -> Danger
    p >= 75.0 -> Cyan300
    else -> Mint300
}

fun openInBrowser(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}

// ------------------------------------------------------------------ componentes
@Composable
fun ProgressBar(fraction: Float, color: Color) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Navy700)
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(color)
        )
    }
}

@Composable
fun StatCard(label: String, value: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, AccentGradient, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(34.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(AccentGradient))
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
fun AppHeader(title: String, subtitle: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .border(1.dp, AccentGradient, RoundedCornerShape(24.dp))
            .background(HeaderGradient).padding(18.dp)
    ) {
        Box(
            Modifier.size(90.dp).align(Alignment.TopEnd)
                .background(Brush.radialGradient(listOf(Cyan400.copy(alpha = 0.20f), Color.Transparent)))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(GlowMintDot, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Cyan300)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            trailing()
        }
    }
}

private val GlowMintDot = Color(0xFF8CFBD0)

@Composable
fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
        Row(
            Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, null, tint = Danger, modifier = Modifier.size(22.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Cyan400)) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Cyan400, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

// ------------------------------------------------------------------ Login
@Composable
fun LoginView(state: AppState) {
    var url by remember { mutableStateOf(if (state.config.baseUrl() == SecureConfig.DEFAULT_URL) SecureConfig.DEFAULT_URL else state.config.baseUrl()) }
    var code by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 460.dp).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppHeader("AGTech NAS", "Cliente de escritorio · Windows 11")
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(url, { url = it }, label = { Text("URL del NAS") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))

            Text("Emparejar con código", style = MaterialTheme.typography.titleSmall, color = Cyan400,
                modifier = Modifier.align(Alignment.Start))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(code, { code = it }, label = { Text("Código") },
                    singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    state.saveBaseUrl(url)
                    state.pair(code)
                }, enabled = !state.loading) { Text("Vincular") }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Navy700)
            Spacer(Modifier.height(16.dp))

            Text("Iniciar sesión", style = MaterialTheme.typography.titleSmall, color = Cyan400,
                modifier = Modifier.align(Alignment.Start))
            OutlinedTextField(user, { user = it }, label = { Text("Usuario") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(pass, { pass = it }, label = { Text("Contraseña") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { state.saveBaseUrl(url); state.login(user, pass) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Navy900)
                else Text("Entrar")
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Danger, style = MaterialTheme.typography.bodySmall)
            }
            state.notice?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Mint300, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Text("Por defecto: ${SecureConfig.DEFAULT_URL}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

// ------------------------------------------------------------------ Home
@Composable
fun HomeView(state: AppState, onNavigate: (Screen) -> Unit) {
    val sys = state.system
    val disks = state.disks
    val events = state.events
    val svc = state.services

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 1100.dp).align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppHeader(
                "AGTech NAS", sys?.hostname ?: "Conectando…",
                trailing = {
                    IconButton(onClick = { state.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Refrescar", tint = Cyan400)
                    }
                }
            )

            state.error?.let { ErrorBanner(it) { state.refresh() } }

            if (sys != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("CPU", fmtPercent(sys.cpu.percent), "${sys.cpu.cores} núcleos", barColor(sys.cpu.percent), Modifier.weight(1f))
                    StatCard("RAM", fmtPercent(sys.memory.percent), "${fmtBytes(sys.memory.used)} / ${fmtBytes(sys.memory.total)}", barColor(sys.memory.percent), Modifier.weight(1f))
                    StatCard("Temp", sys.temperature_c.value?.let { "%.1f °C".format(it) } ?: "—", "NAS", Cyan300, Modifier.weight(1f))
                    StatCard("Uptime", fmtUptime(sys.uptime_s), sys.os, Mint300, Modifier.weight(1f))
                }

                Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Memory, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                            Text("Memoria", style = MaterialTheme.typography.titleSmall, color = Cyan400)
                        }
                        ProgressBar((sys.memory.percent / 100.0).toFloat(), barColor(sys.memory.percent))
                        Text("${fmtBytes(sys.memory.used)} usados de ${fmtBytes(sys.memory.total)}  ·  swap ${fmtPercent(sys.swap.percent)}",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }

            if (svc != null && svc.services.isNotEmpty()) {
                SectionTitle("Servicios · ${svc.online}/${svc.total} en línea")
                Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        svc.services.chunked(3).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth()) {
                                rowItems.forEach { s ->
                                    Row(
                                        Modifier.weight(1f).clickable { onNavigate(Screen.APPS) }.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(Modifier.size(8.dp).background(if (s.ok) Mint300 else Danger, CircleShape))
                                        Text(s.name, style = MaterialTheme.typography.labelSmall,
                                            color = if (s.ok) TextPrimary else Danger, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text("${s.ms}ms", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            if (events != null && events.events.isNotEmpty()) {
                val accent = if (events.healthy) Cyan400 else Danger
                SectionTitle("Estado del NAS · ${events.count} aviso(s)")
                Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        events.events.forEach { e ->
                            val color = when (e.level) {
                                "critical" -> Danger
                                "warning" -> Cyan300
                                else -> TextMuted
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = color, style = MaterialTheme.typography.bodySmall)
                                Text("${e.source}: ${e.message}", color = color, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else if (events != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Mint300, modifier = Modifier.size(18.dp))
                        Text("Todo correcto · sin avisos", style = MaterialTheme.typography.bodySmall, color = Mint300)
                    }
                }
            }

            if (disks != null && disks.disks.isNotEmpty()) {
                SectionTitle("Discos")
                Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        disks.disks.forEach { d ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(d.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                                    val smart = when (d.smart.healthy) {
                                        true -> "SMART OK" to Mint300
                                        false -> "REVISAR" to Danger
                                        null -> "SMART —" to TextMuted
                                    }
                                    Text(if (d.online) smart.first else "OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (d.online) smart.second else Danger)
                                }
                                ProgressBar((d.percent / 100.0).toFloat(), barColor(d.percent))
                                Text("${fmtBytes(d.used)} / ${fmtBytes(d.total)}  ·  ${fmtPercent(d.percent)}",
                                    style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            }

            if (state.loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Cyan400)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Files
@Composable
fun FilesView(state: AppState) {
    val f = state.filesState
    LaunchedEffect(Unit) { if (f.items.isEmpty() && !f.loading) state.loadFiles() }

    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp)) {
        AppHeader(
            "Archivos", "${f.root}${f.path}",
            trailing = {
                IconButton(onClick = { state.filesUp() }) { Icon(Icons.Filled.ArrowUpward, "Subir", tint = Cyan400) }
                IconButton(onClick = { state.loadFiles() }) { Icon(Icons.Filled.Refresh, "Refrescar", tint = Cyan400) }
            }
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Origen:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            FILE_ROOTS.forEach { root ->
                val selected = f.root == root
                FilterChip(
                    selected = selected,
                    onClick = { if (!selected) state.setFilesRoot(root) },
                    label = { Text(root) },
                    leadingIcon = {
                        Icon(
                            if (root == "Karen") Icons.Filled.Storage else Icons.Filled.VideoLibrary,
                            null, modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (f.error != null) { ErrorBanner(f.error) { state.loadFiles() }; Spacer(Modifier.height(8.dp)) }
        if (f.loading) { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan400); Spacer(Modifier.height(8.dp)) }

        Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = Navy800)) {
            if (f.items.isEmpty() && !f.loading) EmptyState("Carpeta vacía")
            else LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                items(f.items) { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (item.is_dir) state.loadFiles(f.root, item.path)
                        }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (item.is_dir) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                            null, tint = if (item.is_dir) Cyan300 else TextMuted, modifier = Modifier.size(22.dp)
                        )
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!item.is_dir) Text(fmtBytes(item.size), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    HorizontalDivider(color = Navy700.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Containers
@Composable
fun ContainersView(state: AppState) {
    val list = state.containers?.containers ?: emptyList()
    LaunchedEffect(Unit) { if (state.containers == null) state.refresh() }
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp)) {
        AppHeader("Docker", "${list.size} contenedores", trailing = {
            IconButton(onClick = { state.refresh() }) { Icon(Icons.Filled.Refresh, "Refrescar", tint = Cyan400) }
        })
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = Navy800)) {
            if (list.isEmpty()) EmptyState("Sin contenedores")
            else LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                items(list) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { state.openContainer(c) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.size(9.dp).background(if (c.state == "running") Mint300 else Danger, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(c.image ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(c.status ?: c.state ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    HorizontalDivider(color = Navy700.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Apps
@Composable
fun AppsView(state: AppState) {
    val list = state.apps?.apps ?: emptyList()
    LaunchedEffect(Unit) { if (state.apps == null) state.refresh() }
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp)) {
        AppHeader("Accesos", "${list.size} aplicaciones · se abren dentro de la app")
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) EmptyState("Sin accesos configurados")
        else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { app ->
                Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { state.openApp(app) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(GlassCard), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Apps, null, tint = Cyan400, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(app.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(app.category, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        if (state.hasCredential(app.id)) {
                            Icon(Icons.Filled.Lock, "Acceso automático", tint = Mint300, modifier = Modifier.size(16.dp))
                        }
                        Icon(Icons.Filled.OpenInBrowser, "Abrir en la app", tint = Cyan400, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Settings
@Composable
fun SettingsView(state: AppState) {
    var url by remember { mutableStateOf(state.config.baseUrl()) }
    var credApp by remember { mutableStateOf<AppItem?>(null) }
    var nasPass by remember { mutableStateOf("") }
    var showNasPass by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state.updateInfo == null && !state.updateChecking) state.checkUpdates()
    }

    Column(
        Modifier.fillMaxSize().background(AppBackground)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppHeader("Ajustes", state.config.user().ifBlank { "NAS" })

        // ---------------------------------------------------------- actualizaciones
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Navy800)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Actualizaciones", style = MaterialTheme.typography.titleSmall, color = Cyan400, modifier = Modifier.weight(1f))
                    Text("v$APP_VERSION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }

                val info = state.updateInfo
                when {
                    state.updateChecking -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Cyan400)
                        Text("Comprobando…", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }

                    info == null -> Text(
                        "Pulsa «Buscar actualización» para consultar GitHub Releases.",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )

                    info.available -> {
                        Text(
                            "Nueva versión disponible: v${info.latestVersion}",
                            style = MaterialTheme.typography.bodyMedium, color = Mint300
                        )
                        if (info.notes.isNotBlank()) {
                            Text(
                                info.notes.take(1200),
                                style = MaterialTheme.typography.labelSmall, color = TextMuted
                            )
                        }
                        if (info.assetName.isNotBlank()) {
                            Text(
                                "Instalador: ${info.assetName}" +
                                    if (info.assetSize > 0) " (${fmtBytes(info.assetSize.toDouble())})" else "",
                                style = MaterialTheme.typography.labelSmall, color = TextMuted
                            )
                        }
                    }

                    else -> Text(
                        "Tienes la última versión (v${info.latestVersion.ifBlank { APP_VERSION }}).",
                        style = MaterialTheme.typography.bodySmall, color = Mint300
                    )
                }

                if (state.downloadingUpdate) {
                    ProgressBar(state.downloadProgress, Cyan400)
                    Text(
                        "Descargando… ${(state.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted
                    )
                }

                state.updateError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Danger)
                }

                state.downloadedUpdate?.let { file ->
                    Text("Descargado: ${file.absolutePath}", style = MaterialTheme.typography.labelSmall, color = Mint300)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { state.checkUpdates() },
                        enabled = !state.updateChecking && !state.downloadingUpdate
                    ) { Text("Buscar actualización") }

                    if (info?.available == true && state.downloadedUpdate == null) {
                        Button(
                            onClick = { state.downloadUpdate() },
                            enabled = !state.downloadingUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900)
                        ) { Text(if (state.downloadingUpdate) "Descargando…" else "Descargar e instalar") }
                    }

                    if (state.downloadedUpdate != null) {
                        Button(
                            onClick = { state.openDownloadedUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = Mint300, contentColor = Navy900)
                        ) { Text("Instalar ahora") }
                        OutlinedButton(onClick = { state.openDownloadFolder() }) { Text("Abrir carpeta") }
                    }
                }
            }
        }

        // ---------------------------------------------------------- conexión
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Navy800)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Conexión", style = MaterialTheme.typography.titleSmall, color = Cyan400)
                OutlinedTextField(url, { url = it }, label = { Text("URL del NAS") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { state.saveBaseUrl(url); state.refresh() }) { Text("Guardar y conectar") }
                    OutlinedButton(onClick = { state.refresh() }) { Text("Refrescar") }
                }
                HorizontalDivider(color = Navy700)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Usuario:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text(state.config.user().ifBlank { "—" }, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ---------------------------------------------------------- accesos / credenciales
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Navy800)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Accesos y contraseñas", style = MaterialTheme.typography.titleSmall, color = Cyan400)
                Text(
                    "Los accesos se abren dentro de la app y rellenan el formulario automáticamente. " +
                        "Si un servicio usa otras credenciales, cámbialas aquí.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = state.useNasCredentialsForAll,
                        onCheckedChange = { state.useNasCredentialsForAll = it }
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Usar mis credenciales del NAS en todos", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text(
                            "Se usan en los accesos sin credenciales propias.",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted
                        )
                    }
                }

                val apps = state.apps?.apps.orEmpty().filter { !it.wan.isNullOrBlank() }
                if (apps.isEmpty()) {
                    Text("Sin accesos cargados todavía.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    HorizontalDivider(color = Navy700)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            nasPass, { nasPass = it },
                            label = {
                                Text(
                                    if (state.nasPasswordSet) "Contraseña del NAS (guardada)"
                                    else "Contraseña del NAS para autorrelleno"
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (showNasPass) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showNasPass = !showNasPass }) {
                            Icon(
                                if (showNasPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                "Mostrar/ocultar", tint = TextMuted, modifier = Modifier.size(18.dp)
                            )
                        }
                        Button(
                            onClick = { state.saveNasPassword(nasPass); nasPass = "" },
                            enabled = nasPass.isNotBlank()
                        ) { Text("Guardar") }
                    }
                    HorizontalDivider(color = Navy700)
                    apps.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (state.hasCredential(app.id)) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                null,
                                tint = if (state.hasCredential(app.id)) Mint300 else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(app.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                val explicit = state.config.appCredentials()[app.id]
                                Text(
                                    when {
                                        explicit != null -> "Credencial propia: ${explicit.login}"
                                        state.credentials[app.id] != null -> "Credencial del NAS: ${state.credentials[app.id]?.login}"
                                        state.useNasCredentialsForAll && state.config.loginPassword().isNotBlank() ->
                                            "Usa tu usuario del NAS"
                                        else -> "Sin credenciales"
                                    },
                                    style = MaterialTheme.typography.labelSmall, color = TextMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { credApp = app }) { Text("Editar") }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------- sesión
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Navy800)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sesión", style = MaterialTheme.typography.titleSmall, color = Cyan400)
                Button(
                    onClick = { state.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Danger)
                ) {
                    Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cerrar sesión")
                }
            }
        }
    }

    credApp?.let { app ->
        CredentialDialog(app, state) { credApp = null }
    }
}

@Composable
private fun CredentialDialog(app: AppItem, state: AppState, onClose: () -> Unit) {
    val existing = state.config.appCredentials()[app.id] ?: state.credentials[app.id] ?: AppCredential()
    var login by remember(app.id) { mutableStateOf(existing.login) }
    var pass by remember(app.id) { mutableStateOf(existing.password) }
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Credenciales · ${app.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Se guardan cifradas en este equipo. Si el servicio no las tiene configuradas, el formulario se rellenará igualmente.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
                OutlinedTextField(login, { login = it }, label = { Text("Usuario o correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    pass, { pass = it }, label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = if (showPass) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "Ocultar contraseña" else "Mostrar contraseña")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.saveAppCredential(app.id, login, pass)
                    onClose()
                },
                enabled = login.isNotBlank() && pass.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (state.config.appCredentials().containsKey(app.id)) {
                    TextButton(onClick = { state.removeAppCredential(app.id); onClose() }) { Text("Borrar", color = Danger) }
                }
                TextButton(onClick = onClose) { Text("Cancelar") }
            }
        }
    )
}

// ------------------------------------------------------------------ Detail
@Composable
fun DetailView(state: AppState) {
    val d = state.detail
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp)) {
        AppHeader(d.name.ifBlank { "Contenedor" }, d.id.take(12), trailing = {
            IconButton(onClick = { state.navigate(Screen.CONTAINERS) }) { Icon(Icons.Filled.ArrowBack, "Volver", tint = Cyan400) }
        })
        Spacer(Modifier.height(12.dp))

        d.error?.let { ErrorBanner(it) { state.openContainer(Container(id = d.id, name = d.name)) }; Spacer(Modifier.height(8.dp)) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { state.containerAction(d.id, "start") }, enabled = !d.working, colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Mint300)) { Text("Start") }
            Button(onClick = { state.containerAction(d.id, "stop") }, enabled = !d.working, colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Danger)) { Text("Stop") }
            Button(onClick = { state.containerAction(d.id, "restart") }, enabled = !d.working, colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Cyan400)) { Text("Restart") }
        }
        Spacer(Modifier.height(12.dp))

        d.stats?.let { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("CPU", fmtPercent(s.cpu_percent), "contenedor", barColor(s.cpu_percent), Modifier.weight(1f))
                StatCard("RAM", fmtPercent(s.memory.percent), "${fmtBytes(s.memory.usage)} / ${fmtBytes(s.memory.limit)}", barColor(s.memory.percent), Modifier.weight(1f))
                StatCard("Red RX", fmtBytes(s.network.rx_bytes), "total", Cyan300, Modifier.weight(1f))
                StatCard("Red TX", fmtBytes(s.network.tx_bytes), "total", Mint300, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        SectionTitle("Logs")
        Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1523))) {
            if (d.working) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Cyan400) }
            else LazyColumn(Modifier.fillMaxSize().padding(10.dp)) {
                items(d.logs) { line ->
                    Text(line, style = MaterialTheme.typography.labelSmall, color = Cyan200, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}
