package com.lipapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            MainScreen()
        }
    }
}
