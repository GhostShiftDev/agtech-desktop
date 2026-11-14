package com.agtech.nas

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Configuración persistente cifrada para escritorio.
 * Sustituye a EncryptedSharedPreferences (Android) por:
 *  - clave AES-256 en ~/.agtech/master.key (permisos 600)
 *  - valores cifrados con AES/GCM en ~/.agtech/config.properties
 */
class SecureConfig {

    private val baseDir: File = File(System.getProperty("user.home"), ".agtech").apply { mkdirs() }
    private val keyFile = File(baseDir, "master.key")
    private val propsFile = File(baseDir, "config.properties")
    private val key: SecretKey = loadOrCreateKey()
    private val props = Properties().apply {
        if (propsFile.exists()) propsFile.inputStream().use { load(it) }
    }

    private fun loadOrCreateKey(): SecretKey {
        return if (keyFile.exists()) {
            SecretKeySpec(keyFile.readBytes(), "AES")
        } else {
            val k = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            keyFile.writeBytes(k.encoded)
            runCatching {
                Files.setPosixFilePermissions(
                    keyFile.toPath(),
                    setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
                )
            }
            k
        }
    }

    private fun encrypt(plain: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = iv + ct
        return Base64.getEncoder().encodeToString(out)
    }

    private fun decrypt(encoded: String): String? = runCatching {
        val all = Base64.getDecoder().decode(encoded)
        val iv = all.copyOfRange(0, 12)
        val ct = all.copyOfRange(12, all.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        String(cipher.doFinal(ct), Charsets.UTF_8)
    }.getOrNull()

    private fun putEnc(key: String, value: String) {
        props.setProperty(key, encrypt(value)); save()
    }

    private fun getEnc(key: String): String? = props.getProperty(key)?.let { decrypt(it) }

    private fun save() {
        propsFile.outputStream().use { props.store(it, "AGTech Desktop") }
        runCatching {
            Files.setPosixFilePermissions(
                propsFile.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            )
        }
    }

    // -------- API tipo ConfigSnapshot --------
    fun baseUrl(): String = props.getProperty("base_url", DEFAULT_URL) ?: DEFAULT_URL
    fun token(): String? = getEnc("token")
    fun serviceToken(): String? = getEnc("service_token")
    fun user(): String = getEnc("user") ?: ""
    fun isPaired(): Boolean = !token().isNullOrBlank()

    fun saveBaseUrl(url: String) {
        props.setProperty("base_url", url.trim().trimEnd('/')); save()
    }

    fun saveSession(token: String, serviceToken: String, user: String) {
        putEnc("token", token)
        putEnc("service_token", serviceToken)
        putEnc("user", user)
    }

    fun clearSession() {
        props.remove("token"); props.remove("service_token"); props.remove("user"); save()
    }

    fun clearAll() {
        props.clear(); save()
    }

    companion object {
        const val DEFAULT_URL = "https://nas.agtech.dpdns.org"
    }
}
