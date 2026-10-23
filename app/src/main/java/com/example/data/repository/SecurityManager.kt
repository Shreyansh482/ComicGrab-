package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("comicgrab_security", Context.MODE_PRIVATE)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var failedAttempts = 0
    private var lockoutUntilElapsedRealtime: Long = 0L

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LOCK_TIMEOUT_SECONDS = "lock_timeout_seconds" // 0 = Immediately, 60 = 1 min, 300 = 5 min, -1 = Never
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        private const val HASH_ITERATIONS = 5000
    }

    init {
        // If no PIN is configured, allow unlocked state directly until user sets up a PIN
        if (!hasPinConfigured()) {
            _isUnlocked.value = true
        }
    }

    fun hasPinConfigured(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLockTimeoutSeconds(): Int {
        return prefs.getInt(KEY_LOCK_TIMEOUT_SECONDS, 0) // Default: Immediately
    }

    fun setLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_LOCK_TIMEOUT_SECONDS, seconds).apply()
    }

    fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isLockedOut(): Boolean {
        val remaining = lockoutUntilElapsedRealtime - SystemClock.elapsedRealtime()
        return remaining > 0
    }

    fun getRemainingLockoutSeconds(): Int {
        val remainingMs = lockoutUntilElapsedRealtime - SystemClock.elapsedRealtime()
        return if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt() else 0
    }

    fun setupPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashPinWithSalt(pin, salt)

        val saltHex = bytesToHex(salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, saltHex)
            .apply()

        _isUnlocked.value = true
        failedAttempts = 0
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (isLockedOut()) return false

        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = hexToBytes(storedSaltHex)

        val computedHash = hashPinWithSalt(pin, salt)
        val matches = MessageDigest.isEqual(computedHash.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))

        if (matches) {
            failedAttempts = 0
            _isUnlocked.value = true
            return true
        } else {
            failedAttempts++
            if (failedAttempts >= 5) {
                lockoutUntilElapsedRealtime = SystemClock.elapsedRealtime() + 60_000 // 60s lockout
            } else if (failedAttempts >= 3) {
                lockoutUntilElapsedRealtime = SystemClock.elapsedRealtime() + 30_000 // 30s lockout
            }
            return false
        }
    }

    fun unlockWithBiometrics() {
        failedAttempts = 0
        _isUnlocked.value = true
    }

    fun lock() {
        if (hasPinConfigured()) {
            _isUnlocked.value = false
        }
    }

    fun onAppBackgrounded() {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
    }

    fun onAppForegrounded() {
        if (!hasPinConfigured()) return

        val timeoutSeconds = getLockTimeoutSeconds()
        if (timeoutSeconds < 0) return // Never auto lock

        val lastBg = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBg > 0) {
            val elapsedSeconds = (System.currentTimeMillis() - lastBg) / 1000
            if (elapsedSeconds >= timeoutSeconds) {
                lock()
            }
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockWithBiometrics()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Fingerprint not recognized. Try again or use PIN.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ComicGrab")
            .setSubtitle("Use your fingerprint to access your private library")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        var result = md.digest(pin.toByteArray(Charsets.UTF_8))
        for (i in 1 until HASH_ITERATIONS) {
            md.reset()
            result = md.digest(result)
        }
        return bytesToHex(result)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
