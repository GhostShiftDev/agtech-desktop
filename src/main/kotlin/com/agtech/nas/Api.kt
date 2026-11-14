package com.agtech.nas

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NasApi {
    @POST("auth") suspend fun auth(@Body body: AuthRequest): Response<AuthResponse>
    @POST("pair") suspend fun pair(@Body body: PairRequest): Response<AuthResponse>

    @GET("system") suspend fun system(): Response<SystemInfo>
    @GET("disks") suspend fun disks(): Response<DisksResponse>
    @GET("apps") suspend fun apps(): Response<AppsResponse>
    @GET("apps/status") suspend fun appsStatus(): Response<ServicesStatusResponse>
    @GET("events") suspend fun events(): Response<EventsResponse>

    @GET("files")
    suspend fun files(@Query("root") root: String, @Query("path") path: String): Response<FilesResponse>
    @GET("files/download")
    suspend fun download(@Query("root") root: String, @Query("path") path: String): Response<ResponseBody>
    @POST("files/mkdir") suspend fun mkdir(@Body body: MkdirRequest): Response<OkResponse>
    @DELETE("files")
    suspend fun delete(@Query("root") root: String, @Query("path") path: String): Response<OkResponse>

    @GET("docker/containers") suspend fun containers(): Response<ContainersResponse>
    @GET("docker/containers/{id}/stats") suspend fun stats(@Path("id") id: String): Response<ContainerStats>
    @GET("docker/containers/{id}/logs")
    suspend fun logs(@Path("id") id: String, @Query("tail") tail: Int = 200): Response<LogsResponse>
    @POST("docker/containers/{id}/{action}")
    suspend fun action(@Path("id") id: String, @Path("action") action: String): Response<Void>

    @POST("system/power") suspend fun power(@Body body: PowerRequest): Response<PowerResponse>
}

data class MkdirRequest(val root: String, val path: String, val name: String)

class NasClient(private val config: SecureConfig) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun http(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val b = chain.request().newBuilder()
            config.token()?.takeIf { it.isNotBlank() }?.let { b.header("Authorization", "Bearer $it") }
            config.serviceToken()?.takeIf { it.isNotBlank() }?.let { b.header("X-NAS-Service-Token", it) }
            chain.proceed(b.build())
        }
        .build()

    fun api(baseUrl: String = config.baseUrl()): NasApi {
        val base = baseUrl.trim().trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(http())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NasApi::class.java)
    }
}
