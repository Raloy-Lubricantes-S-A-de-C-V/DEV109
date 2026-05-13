package com.example.sgnatureraloy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sgnatureraloy.core.network.RetrofitClient
import com.example.sgnatureraloy.core.session.SessionManager
import com.example.sgnatureraloy.data.repository.AuthRepository
import com.example.sgnatureraloy.data.repository.SignatureRepository
import com.example.sgnatureraloy.ui.login.LoginScreen
import com.example.sgnatureraloy.ui.login.LoginViewModel
import com.example.sgnatureraloy.ui.login.LoginViewModelFactory
import com.example.sgnatureraloy.ui.signature.SignatureDetailScreen
import com.example.sgnatureraloy.ui.signature.SignatureListScreen
import com.example.sgnatureraloy.ui.signature.SignatureViewModel
import com.example.sgnatureraloy.ui.signature.SignatureViewModelFactory
import com.example.sgnatureraloy.ui.theme.SgnatureraloyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FIRMA", "MainActivity: onCreate")
        
        enableEdgeToEdge()

        setContent {
            SgnatureraloyTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // DETERMINAR DESTINO INICIAL SEGÚN SESIÓN
    val startDestination = if (sessionManager.isLoggedIn()) "signature_list" else "login"

    val apiService = RetrofitClient.getApiService(context)
    val authRepository = AuthRepository(apiService)
    val signatureRepository = SignatureRepository(apiService)
    
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(authRepository, sessionManager))
    val signatureViewModel: SignatureViewModel = viewModel(factory = SignatureViewModelFactory(signatureRepository))

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            Log.d("FIRMA", "NavHost: MOSTRANDO LOGIN (ESTÁTICO)")
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    Log.d("FIRMA", "NavHost: LOGIN EXITOSO -> PASANDO A HOME")
                    navController.navigate("signature_list") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("signature_list") {
            SignatureListScreen(
                viewModel = signatureViewModel,
                userEmail = sessionManager.getUserEmail() ?: "",
                onSignatureClick = { signature ->
                    navController.navigate("signature_detail/${signature.referenceId}")
                },
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate("login") {
                        popUpTo("signature_list") { inclusive = true }
                    }
                }
            )
        }
        composable(
            "signature_detail/{referenceId}",
            arguments = listOf(navArgument("referenceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val referenceId = backStackEntry.arguments?.getString("referenceId")
            val signatures by signatureViewModel.signatures.collectAsState()
            val signature = signatures.find { it.referenceId == referenceId }
            
            signature?.let {
                SignatureDetailScreen(
                    signature = it,
                    onBack = { navController.popBackStack() },
                    onSignClick = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}
