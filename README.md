# AGTech NAS — Cliente de escritorio (Windows 11 / Linux)

Aplicación nativa de escritorio que replica la app Android **AGTech NAS** (Kotlin + Jetpack Compose)
usando **Compose Multiplatform / Compose for Desktop**. Comparte el mismo lenguaje, tema y API,
por lo que la apariencia y el comportamiento son equivalentes a la APK.

Probada en vivo contra el NAS real (`https://nas.agtech.dpdns.org`): inicio de sesión, panel,
archivos, Docker, detalle de contenedores con estadísticas y logs, accesos y ajustes.

---

## 1. Funcionalidades

| Pantalla | Descripción |
|---|---|
| **Login** | URL del NAS, emparejamiento por código e inicio de sesión usuario/contraseña. |
| **Inicio** | CPU, RAM, temperatura, uptime, memoria/swap, servicios (latencia real), avisos del NAS y discos con SMART. |
| **Archivos** | Navegador de carpetas del NAS (raíz + ruta), tamaños y reintento ante errores. |
| **Docker** | Lista de contenedores con estado; detalle con CPU/RAM/red, logs en vivo y Start/Stop/Restart. |
| **Accesos** | Apps/servicios publicados por el NAS; se abren en el navegador del sistema. |
| **Ajustes** | Cambio de URL, refresco y cierre de sesión. |

La sesión se guarda **cifrada** con AES-256-GCM:

- Linux: `~/.agtech/` (`master.key` con permisos `600` + `config.properties`).
- Windows: `%USERPROFILE%\.agtech\` (clave y configuración cifrada).

Al reabrir la app, si hay sesión válida entra directamente al panel.

---

## 2. Requisitos

### Para ejecutar (desarrollo)

- **JDK 21** (con soporte gráfico; en Linux debe ser el JDK completo, no `headless`).
- Gradle se descarga solo con el wrapper (`gradle-8.13`).
- Linux: X11/Wayland. En servidores sin pantalla se puede usar Xvfb (ver §6).

### Para generar el instalador de Windows

- **Windows 10/11 x64**.
- **JDK 21** (Microsoft OpenJDK, Temurin, etc.).
- **WiX Toolset 3.x** en el `PATH` (necesario para `packageMsi`/`packageExe`).
  Descarga: <https://github.com/wixtoolset/wix3/releases> (v3.14.x).
- Ejecutar los comandos desde **PowerShell**.

> El instalador nativo **no se puede compilar desde Linux**: `jpackage` solo genera paquetes
> para el sistema operativo anfitrión. Para Windows hay que ejecutar el build en Windows
> (o en un runner `windows-latest` de GitHub Actions).

---

## 3. Ejecutar en desarrollo

```bash
# Linux / macOS
./gradlew run

# Windows (PowerShell)
.\gradlew.bat run
```

La app abre una ventana de 1180×820. Introduce la URL del NAS y tus credenciales, o usa el
código de emparejamiento.

> **Nota sobre credenciales:** no se incluyen contraseñas en el repositorio. Usa la cuenta del
> NAS o el código de emparejamiento generado por el servidor.

---

## 4. Generar el instalador para Windows 11

### Opción A — En un PC Windows (recomendado)

Requisitos: **JDK 21** + **WiX Toolset 3.x** en el `PATH`.

La forma más rápida es el script incluido:

```powershell
# MSI
powershell -ExecutionPolicy Bypass -File .\build-windows.ps1

# EXE
powershell -ExecutionPolicy Bypass -File .\build-windows.ps1 -Exe
```

O manualmente:

```powershell
# Instalador MSI (recomendado para despliegue)
.\gradlew.bat packageMsi

# Instalador EXE
.\gradlew.bat packageExe
```

Resultado:

```
build\compose\binaries\main\msi\AGTech-1.0.0.msi
build\compose\binaries\main\exe\AGTech-1.0.0.exe
```

También se puede crear una **distribución portable** (carpeta con runtime propio, sin instalar):

```powershell
.\gradlew.bat createDistributable
# Resultado: build\compose\binaries\main\app\AGTech\AGTech.exe
```

### Opción B — Sin PC Windows: GitHub Actions

El repositorio incluye `.github/workflows/windows-installer.yml`. GitHub compila el MSI/EXE
en un runner `windows-latest` (con JDK 21 y WiX) y deja los instaladores como artefacto
descargable:

1. Crea un repositorio en GitHub y sube **el contenido de `agtech-desktop/`** a la raíz.
2. Ve a **Actions → Windows Installer → Run workflow**.
3. Al terminar, descarga el artefacto **AGTech-Windows** (contiene `.msi` y `.exe`).

Comandos para subirlo:

```bash
cd agtech-desktop
git init
git add .
git commit -m "AGTech NAS Desktop"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/agtech-desktop.git
git push -u origin main
```

> El workflow también se dispara al publicar un tag `v*` (por ejemplo `git tag v1.0.0 && git push --tags`).

### Firma de código (opcional, recomendado)

Para evitar el aviso de SmartScreen, firma el MSI/EXE con `signtool` y un certificado de firma
de código:

```powershell
signtool sign /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 /a AGTech-1.0.0.msi
```

---

## 5. Empaquetado en Linux (referencia)

```bash
./gradlew packageDeb      # genera build/compose/binaries/main/deb/agtech_1.0.0_amd64.deb
./gradlew createDistributable
```

Requiere `dpkg-deb`, `fakeroot` y `binutils` (`objcopy`). En Debian/Ubuntu:

```bash
sudo apt-get install -y dpkg-dev fakeroot binutils
```

---

## 6. Solución de problemas

### `java.awt.HeadlessException`

El JDK instalado es *headless*. Instala el JDK completo:

```bash
sudo apt-get install -y openjdk-21-jdk
```

### `Cannot create Linux GL context` (Skiko)

Es un aviso: la app cae automáticamente a render por software. Para forzarlo:

```bash
./gradlew run -Dorg.gradle.jvmargs="-Dskiko.renderApi=SOFTWARE"
```

En el instalable, añade el argumento JVM en `build.gradle.kts`:

```kotlin
compose.desktop.application {
    jvmArgs += listOf("-Dskiko.renderApi=SOFTWARE")
}
```

### `jlink ... Cannot run program "objcopy"`

Instala `binutils`:

```bash
sudo apt-get install -y binutils
```

### La pantalla **Archivos** devuelve HTTP 503

No es un fallo de la app: el disco raíz configurado (p. ej. `Karen`) está **OFFLINE** en el NAS.
La UI muestra el error y el botón **Reintentar**. Cuando el disco vuelve, la lista carga sola.

### `packageMsi` falla en Windows

- Comprueba que WiX 3.x está en el `PATH` (`candle.exe`, `light.exe`).
- Usa JDK 21 x64.
- Ejecuta PowerShell como usuario normal (no es necesario administrador para compilar).

---

## 7. Estructura del proyecto

```
agtech-desktop/
├── build.gradle.kts                     # Kotlin 2.2.20, Compose 1.9.3, JDK 21, targets Msi/Exe/Deb
├── settings.gradle.kts
├── gradle/wrapper/                      # Gradle 8.13
├── icons/
│   ├── icon.ico                         # Icono Windows (16–256 px)
│   └── icon.png                         # Icono Linux / ventana
└── src/main/
    ├── resources/icon.png               # Icono cargado en la ventana
    └── kotlin/com/agtech/nas/
        ├── Main.kt                      # Ventana, scaffold, navegación inferior
        ├── Ui.kt                        # Vistas: login, inicio, archivos, docker, apps, ajustes, detalle
        ├── AppState.kt                  # Estado y lógica (corrutinas en Dispatchers.Swing/IO)
        ├── Api.kt                       # Retrofit + OkHttp + Moshi
        ├── Models.kt                    # DTOs de la API
        ├── SecureConfig.kt              # AES/GCM + ~/.agtech
        └── ui/theme/Theme.kt            # Paleta y tipografía equivalentes a la APK
```

### Stack

- Kotlin 2.2.20 · Compose Multiplatform 1.9.3 · Material 3
- Retrofit 2.11 + OkHttp 4.12 + Moshi 1.15
- kotlinx-coroutines-swing 1.9
- Gradle 8.13 · JDK 21

---

## 8. API utilizada

| Endpoint | Uso |
|---|---|
| `POST /auth`, `POST /pair` | Inicio de sesión / emparejamiento |
| `GET /system` | CPU, RAM, swap, temperatura, red, uptime |
| `GET /disks` | Discos, uso y SMART |
| `GET /apps`, `GET /apps/status` | Accesos publicados y estado de servicios |
| `GET /events` | Avisos/eventos del NAS |
| `GET /files`, `/files/download`, `POST /files/mkdir`, `DELETE /files` | Navegación y operaciones de archivos |
| `GET /docker/containers` | Lista de contenedores |
| `GET /docker/containers/{id}/stats`, `/logs` | Estadísticas y logs |
| `POST /docker/containers/{id}/{start\|stop\|restart}` | Control de contenedores |
| `POST /system/power` | Acciones de energía |

Los DTOs viven en `Models.kt` y coinciden con los de la app Android.

---

## 9. Estado de verificación

Verificado con el NAS real (no simulado):

- Login HTTP 200 con JWT y `service_token`.
- Panel con CPU, RAM, temperatura, uptime, **11/11 servicios** y discos.
- **49 contenedores** Docker listados.
- Detalle de contenedor con logs reales (servidor Minecraft Bedrock) y acciones.
- Sesión persistida cifrada y arranque directo en el panel.
- Distribución portable generada y ejecutada (`build/compose/binaries/main/app/AGTech/bin/AGTech`).
- Proyecto Android original intacto: `settings.gradle.kts` incluye solo `:app`.

---

## 10. Notas de diseño

- Se reemplazó `EncryptedSharedPreferences` (Android) por AES/GCM + archivo de clave local.
- No se usa `org.json` (no viene en la JVM estándar): los mensajes `detail` se extraen con regex.
- El estado se actualiza en `Dispatchers.Swing` y las llamadas de red en `Dispatchers.IO`.
- El módulo `compose-desktop` que se había añadido por error dentro de `agtech-nas/` fue eliminado;
  este proyecto es **standalone** para no contaminar el classpath de Android.
