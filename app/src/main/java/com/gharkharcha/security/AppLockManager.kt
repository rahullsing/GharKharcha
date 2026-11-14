package com.gharkharcha.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.gharkharcha.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
) {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val enabled = prefs.preferences.first().appLockEnabled
            if (enabled && isDeviceSecure()) {
                _isLocked.value = true
            }
        }
    }

    fun isDeviceSecure(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }

    fun lockIfEnabled() {
        CoroutineScope(Dispatchers.Default).launch {
            val enabled = prefs.preferences.first().appLockEnabled
            if (enabled && isDeviceSecure()) {
                _isLocked.value = true
            }
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        if (!isDeviceSecure()) {
            _isLocked.value = false
            onResult(true)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    _isLocked.value = false
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    // Biometric didn't match, user can retry
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock GharKharcha")
            .setSubtitle("Confirm your biometric or device lock")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
