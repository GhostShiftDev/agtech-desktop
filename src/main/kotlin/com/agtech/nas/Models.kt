package com.agtech.nas

// ------------------------------- auth
data class AuthRequest(val username: String, val password: String)
data class PairRequest(val code: String)
data class AuthResponse(
    val token: String = "",
    val expires_in: Long = 0,
    val service_token: String = "",
    val user: String = ""
)

// ------------------------------- /system
data class CpuInfo(val cores: Int = 0, val percent: Double = 0.0, val loadavg: List<Double> = emptyList())
data class MemInfo(val total: Double = 0.0, val used: Double = 0.0, val available: Double = 0.0, val percent: Double = 0.0)
data class SwapInfo(val total: Double = 0.0, val used: Double = 0.0, val percent: Double = 0.0)
data class TempInfo(val value: Double? = null, val sensors: List<Double> = emptyList())
data class NetInfo(val rx_bytes: Double = 0.0, val tx_bytes: Double = 0.0)
data class SystemInfo(
    val hostname: String = "",
    val os: String = "",
    val name: String = "",
    val uptime_s: Long = 0,
    val cpu: CpuInfo = CpuInfo(),
    val memory: MemInfo = MemInfo(),
    val swap: SwapInfo = SwapInfo(),
    val temperature_c: TempInfo = TempInfo(),
    val network: NetInfo = NetInfo(),
    val version: String = ""
)

// ------------------------------- /disks
data class SmartInfo(
    val available: Boolean = false,
    val healthy: Boolean? = null,
    val model: String? = null,
    val temperature_c: Double? = null,
    val power_on_hours: Double? = null,
    val reason: String? = null
)
data class Disk(
    val name: String = "",
    val mount: String = "",
    val device: String? = null,
    val total: Double = 0.0,
    val used: Double = 0.0,
    val free: Double = 0.0,
    val percent: Double = 0.0,
    val smart: SmartInfo = SmartInfo(),
    val error: String? = null,
    val online: Boolean = true
)
data class DisksResponse(
    val disks: List<Disk> = emptyList(),
    val total: Double = 0.0,
    val used: Double = 0.0
)

// ------------------------------- /apps
data class AppItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val icon: String = "",
    val wan: String? = null,
    val lan: String? = null,
    val port: Int = 0,
    val path: String = ""
)
data class AppsResponse(val apps: List<AppItem> = emptyList())

// ------------------------------- /docker
data class Container(
    val id: String = "",
    val name: String = "",
    val image: String? = null,
    val state: String? = null,
    val status: String? = null,
    val created: Long = 0,
    val ports: List<String> = emptyList()
)
data class ContainersResponse(val containers: List<Container> = emptyList(), val count: Int = 0)
data class StatMem(val usage: Double = 0.0, val limit: Double = 0.0, val percent: Double = 0.0)
data class StatNet(val rx_bytes: Double = 0.0, val tx_bytes: Double = 0.0)
data class ContainerStats(
    val cpu_percent: Double = 0.0,
    val memory: StatMem = StatMem(),
    val network: StatNet = StatNet()
)
data class LogsResponse(val lines: List<String> = emptyList())

// ------------------------------- /events
data class EventItem(
    val id: String = "",
    val level: String = "",
    val source: String = "",
    val message: String = ""
)
data class EventsResponse(
    val events: List<EventItem> = emptyList(),
    val count: Int = 0,
    val healthy: Boolean = true,
    val ntfy_topic: String? = null
)

// ------------------------------- /files
data class FileItem(
    val name: String = "",
    val path: String = "",
    val is_dir: Boolean = false,
    val size: Double = 0.0,
    val modified: Double = 0.0,
    val mime: String = ""
)
data class FilesResponse(
    val root: String = "",
    val path: String = "",
    val items: List<FileItem> = emptyList(),
    val count: Int = 0
)
data class OkResponse(val ok: Boolean = false, val name: String? = null, val size: Double? = null)

// ------------------------------- /apps/status (servicios)
data class ServiceStatus(
    val id: String = "",
    val name: String = "",
    val ok: Boolean = false,
    val code: Int = 0,
    val ms: Int = 0
)
data class ServicesStatusResponse(
    val services: List<ServiceStatus> = emptyList(),
    val online: Int = 0,
    val total: Int = 0
)

// ------------------------------- /system/power
data class PowerRequest(val action: String, val confirm: Boolean = true)
data class PowerResponse(val ok: Boolean = false, val action: String? = null)
