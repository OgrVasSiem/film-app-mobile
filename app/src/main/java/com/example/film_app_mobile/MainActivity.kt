package com.example.film_app_mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.film_app_mobile.core.dataStore.AppThemeMode
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.session.SessionMode
import com.example.film_app_mobile.navigation.MovieApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var account: AccountSession

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.observeAccountWhileVisible() }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val dark = viewModel.themeMode.collectAsStateWithLifecycle()

            val isDark = dark.value == AppThemeMode.DARK
            AppTheme(darkTheme = isDark) {
                val transparent = AppTheme.colors.transparent.toArgb()
                val fallback = AppTheme.colors.systemBarFallback.toArgb()
                SideEffect {
                    val style =
                        if (isDark) SystemBarStyle.dark(transparent)
                        else SystemBarStyle.light(transparent, fallback)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
                val connected by viewModel.internetAvailable.collectAsStateWithLifecycle()
                val session by account.session.collectAsStateWithLifecycle()
                when (connected) {
                    false -> com.example.film_app_mobile.components.OfflineBanner()
                    null -> com.example.film_app_mobile.components.InitialLoading()
                    true ->
                        when (session) {
                            SessionMode.Restoring ->
                                com.example.film_app_mobile.components.InitialLoading()
                            SessionMode.Retry ->
                                androidx.compose.foundation.layout.Box(
                                    androidx.compose.ui.Modifier.fillMaxSize().safeDrawingPadding(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    com.example.film_app_mobile.core.designsystem.component.Action(
                                        getString(R.string.connection_error) +
                                            " · " +
                                            getString(R.string.retry),
                                        true,
                                        viewModel::restore,
                                    )
                                }
                            SessionMode.Server -> MovieApp(true)
                            SessionMode.Guest -> MovieApp(false)
                        }
                }
            }
        }
    }
}
