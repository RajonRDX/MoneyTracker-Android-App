package com.rajon.moneytracker

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajon.moneytracker.ui.navigation.AppNavHost
import com.rajon.moneytracker.ui.screens.auth.LockScreen
import com.rajon.moneytracker.ui.theme.MoneyTrackerTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val app = application as MoneyTrackerApp
        val localPrefs = app.container.localPrefs
        val authRepo = app.container.authRepository

        setContent {
            var isLocked by remember { mutableStateOf(localPrefs.isLocked == true) }
            val userId by authRepo.currentUserIdFlow.collectAsStateWithLifecycle(initialValue = null)
            val isLoggedIn = userId != null || authRepo.isLoggedIn

            // If user is logged in and biometric lock is enabled in settings, trigger prompt on resume/start
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn && localPrefs.isLocked == true) {
                    isLocked = true
                }
            }

            MoneyTrackerTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn && isLocked) {
                        LockScreen(
                            onUnlockSuccess = {
                                isLocked = false
                                localPrefs.isLocked = false
                            },
                            onBiometricRequested = {
                                showBiometricPrompt(
                                    onSuccess = {
                                        isLocked = false
                                        localPrefs.isLocked = false
                                    }
                                )
                            }
                        )
                    } else {
                        AppNavHost(appContainer = app.container)
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MoneyTracker")
            .setSubtitle("Authenticate to access your personal finance data")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}