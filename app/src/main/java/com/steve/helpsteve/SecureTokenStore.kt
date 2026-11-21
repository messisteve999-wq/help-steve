package com.steve.helpsteve

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stocke le jeton chiffré avec une clé conservée dans Android Keystore.
 * Le jeton n'est jamais écrit en clair dans les préférences.
 */
object SecureTokenStore {
    private const val KEY_ALIAS = "help_steve_server_token_key"
    private const val PREFS = "help_steve_secure_settings"
    private const val TOKEN_KEY = "encrypted_server_token"

    private fun keyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val store = keyStore()
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    @Synchronized
    fun save(context: Context, token: String): Boolean {
        val clean = token.trim()
        if (clean.isEmpty()) return false

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + encrypted

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(TOKEN_KEY, Base64.encodeToString(combined, Base64.NO_WRAP))
                .commit()
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun read(context: Context): String? {
        return try {
            val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(TOKEN_KEY, null) ?: return null

            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= 12) return null

            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun isConfigured(context: Context): Boolean =
        !read(context).isNullOrBlank()

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(TOKEN_KEY)
            .apply()
    }
}
