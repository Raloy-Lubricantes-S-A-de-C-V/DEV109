package com.example.sgnatureraloy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sgnatureraloy.core.network.RetrofitClient
import com.example.sgnatureraloy.core.notification.SignaturePollingManager
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("FIRMA", "Permiso POST_NOTIFICATIONS concedido: $isGranted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val apiService = RetrofitClient.getApiService(context)
    val pollingManager = remember { SignaturePollingManager(context, apiService, sessionManager) }

    var loggedInState by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    val startDestination = if (loggedInState) "signature_list" else "login"

    DisposableEffect(loggedInState) {
        if (loggedInState) {
            pollingManager.startPolling()
        }

        onDispose {
            pollingManager.stopPolling()
        }
    }

    val authRepository = AuthRepository(apiService)
    val signatureRepository = SignatureRepository(apiService)

    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(authRepository, sessionManager)
    )
    val signatureViewModel: SignatureViewModel = viewModel(
        factory = SignatureViewModelFactory(signatureRepository)
    )

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            Log.d("FIRMA", "NavHost: MOSTRANDO LOGIN")
            Log.d("FIRMA", "FCM Token actual: ${sessionManager.getFcmToken()}")

            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    Log.d("FIRMA", "NavHost: LOGIN EXITOSO -> PASANDO A HOME")
                    loginViewModel.resetLoginState()
                    loggedInState = true
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
                    Log.d("FIRMA", "MainNavigation: Ejecutando logout")
                    sessionManager.clearSession()
                    loggedInState = false
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "signature_detail/{referenceId}",
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
