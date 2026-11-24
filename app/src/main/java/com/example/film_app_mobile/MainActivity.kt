package com.example.film_app_mobile

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.session.SessionMode
import com.example.film_app_mobile.data.sync.DecisionSync
import com.example.film_app_mobile.navigation.MovieApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var account: AccountSession
    @Inject lateinit var sync: DecisionSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sync.scheduleSync()
        val dark = getSharedPreferences("appearance", MODE_PRIVATE).getBoolean("dark", true)
        val style =
            if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.DKGRAY)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            AppContent(account.session.collectAsStateWithLifecycle().value == SessionMode.Server)
        }
    }

    @Composable
    internal fun AppContent(signedIn: Boolean) {
        val settings = remember { getSharedPreferences("appearance", MODE_PRIVATE) }
        var dark by remember { mutableStateOf(settings.getBoolean("dark", true)) }
        SideEffect {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        AppTheme(darkTheme = dark) {
            MovieApp(
                signedIn,
                dark,
                { enabled ->
                    dark = enabled
                    settings.edit().putBoolean("dark", enabled).apply()
                },
            )
        }
    }
}
