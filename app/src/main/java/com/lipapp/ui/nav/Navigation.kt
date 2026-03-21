package com.lipapp.ui.nav

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lipapp.SseService
import com.lipapp.ui.login.LoginScreen
import com.lipapp.ui.main.MainScreen

@Composable
fun LipAppNavigation(startLoggedIn: Boolean = false) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = if (startLoggedIn) "main" else "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startService(Intent(context, SseService::class.java))
            }
            MainScreen()
        }
    }
}
