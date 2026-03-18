package com.lipapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.lipapp.data.prefs.AppPreferences
import com.lipapp.ui.nav.LipAppNavigation
import com.lipapp.ui.theme.LipAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by prefs.darkMode.collectAsStateWithLifecycle(initialValue = false)
            LipAppTheme(darkTheme = darkMode) {
                LipAppNavigation()
            }
        }
    }
}
