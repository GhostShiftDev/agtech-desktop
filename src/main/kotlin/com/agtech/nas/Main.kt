package com.agtech.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.agtech.nas.ui.theme.AGTechTheme
import com.agtech.nas.ui.theme.AppBackground
import com.agtech.nas.ui.theme.Cyan400
import com.agtech.nas.ui.theme.Navy900
import com.agtech.nas.ui.theme.TextMuted

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.HOME, "Inicio", Icons.Filled.Home),
    NavItem(Screen.FILES, "Archivos", Icons.Filled.Folder),
    NavItem(Screen.CONTAINERS, "Docker", Icons.Filled.Storage),
    NavItem(Screen.APPS, "Accesos", Icons.Filled.Apps),
    NavItem(Screen.SETTINGS, "Ajustes", Icons.Filled.Settings)
)

fun main() = application {
    val state = remember { AppState() }
    Window(
        onCloseRequest = ::exitApplication,
        title = "AGTech NAS",
        state = rememberWindowState(width = 1180.dp, height = 820.dp)
    ) {
        // Icono de la ventana/barra de tareas (recurso empaquetado en el jar).
        val appIcon = remember {
            runCatching {
                Thread.currentThread().contextClassLoader
                    ?.getResourceAsStream("icon.png")
                    ?.use { javax.imageio.ImageIO.read(it) }
            }.getOrNull()
        }
        LaunchedEffect(appIcon) {
            if (appIcon != null) runCatching { window.iconImage = appIcon }
        }
        AGTechTheme {
            Root(state)
        }
    }
}

@Composable
fun Root(state: AppState) {
    if (state.screen == Screen.LOGIN || !state.config.isPaired()) {
        LoginView(state)
        return
    }

    val showNav = state.screen != Screen.DETAIL

    Scaffold(
        bottomBar = {
            if (showNav) {
                NavigationBar(containerColor = Navy900.copy(alpha = 0.96f), tonalElevation = 10.dp) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = state.screen == item.screen,
                            onClick = { state.navigate(item.screen) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Navy900,
                                selectedTextColor = Cyan400,
                                indicatorColor = Cyan400.copy(alpha = 0.22f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        },
        containerColor = Navy900
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(AppBackground)) {
            when (state.screen) {
                Screen.HOME -> HomeView(state, onNavigate = { state.navigate(it) })
                Screen.FILES -> FilesView(state)
                Screen.CONTAINERS -> ContainersView(state)
                Screen.APPS -> AppsView(state)
                Screen.SETTINGS -> SettingsView(state)
                Screen.DETAIL -> DetailView(state)
                Screen.MEDIA -> HomeView(state, onNavigate = { state.navigate(it) })
                Screen.LOGIN -> LoginView(state)
            }

            state.notice?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { state.clearMessages() }) { Text("OK") }
                    }
                ) { Text(msg) }
            }
        }
    }
}
