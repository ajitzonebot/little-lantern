package org.littlelantern.gate

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Second step of the parent gate: fingerprint via BiometricPrompt.
 * Falls back to the PIN when no biometric is enrolled, when the parent taps "Use PIN",
 * or after [MAX_FAILURES] failed fingerprint attempts.
 */
class ParentGate(
    private val activity: FragmentActivity,
    private val onUnlocked: () -> Unit,
    private val onNeedPin: () -> Unit,
) {
    fun open() {
        val canUseBiometric = BiometricManager.from(activity)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (!canUseBiometric) {
            onNeedPin()
            return
        }

        var failures = 0
        var switchedToPin = false
        lateinit var prompt: BiometricPrompt
        prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationFailed() {
                    failures++
                    if (failures >= MAX_FAILURES && !switchedToPin) {
                        switchedToPin = true
                        prompt.cancelAuthentication()
                        onNeedPin()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (switchedToPin) return
                    when (errorCode) {
                        // Dismissed (often the child): stay in Child mode.
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> Unit
                        // "Use PIN" button, lockout, hardware problems: use the PIN.
                        else -> {
                            switchedToPin = true
                            onNeedPin()
                        }
                    }
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parent check")
            .setSubtitle("Touch the fingerprint sensor")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        const val MAX_FAILURES = 3
    }
}
