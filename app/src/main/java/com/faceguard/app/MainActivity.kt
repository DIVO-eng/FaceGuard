package com.faceguard.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.faceguard.app.ui.*
import com.faceguard.app.ui.theme.FaceGuardTheme
import com.faceguard.app.util.SecurePrefs

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FaceGuardTheme {
                val prefs = SecurePrefs.get(this)
                var unlocked by remember { mutableStateOf(!prefs.getBoolean(SecurePrefs.KEY_APP_LOCK_ENABLED, true)) }

                if (!unlocked) {
                    AppLockGate(
                        activity = this,
                        onUnlocked = { unlocked = true }
                    )
                } else {
                    val navController = rememberNavController()
                    val onboardingDone = prefs.getBoolean(SecurePrefs.KEY_ONBOARDING_COMPLETE, false)

                    NavHost(
                        navController = navController,
                        startDestination = if (onboardingDone) "dashboard" else "consent"
                    ) {
                        composable("consent") {
                            ConsentScreen(onAccepted = {
                                prefs.edit().putBoolean(SecurePrefs.KEY_ONBOARDING_COMPLETE, true).apply()
                                navController.navigate("dashboard") { popUpTo("consent") { inclusive = true } }
                            })
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenGallery = { navController.navigate("gallery") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("gallery") {
                            GalleryScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
