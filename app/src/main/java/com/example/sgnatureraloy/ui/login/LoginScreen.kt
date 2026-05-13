package com.example.sgnatureraloy.ui.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    Log.d("FIRMA", "LoginScreen: Componiendo pantalla de Login")
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    val otpState by viewModel.otpState.collectAsState()

    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Success) {
            onLoginSuccess()
        } else if (loginState is LoginUiState.Error) {
            Toast.makeText(context, (loginState as LoginUiState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(otpState) {
        otpState?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.resetOtpState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 60.dp, horizontal = 32.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Portal de Documentos",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0056B3),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ingresa tu correo y PIN para ver el estado de los documentos que has enviado.",
                fontSize = 14.5.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Correo Electrónico:",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                placeholder = { Text("ejemplo@raloy.com.mx", color = Color(0xFFBBBBBB)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0056B3),
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF333333),
                    unfocusedTextColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PIN (Fijo o Temporal):",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                ),
                placeholder = { Text("****") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0056B3),
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF333333),
                    unfocusedTextColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { viewModel.login(email, pin) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056B3)),
                shape = RoundedCornerShape(8.dp),
                enabled = loginState !is LoginUiState.Loading
            ) {
                if (loginState is LoginUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Ingresar al Portal", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            HorizontalDivider(color = Color(0xFFF1F1F1))
            Spacer(modifier = Modifier.height(25.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "¿No tienes tu PIN a la mano?",
                    fontSize = 13.sp,
                    color = Color(0xFF777777)
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            OutlinedButton(
                onClick = { viewModel.requestOtp(email) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF666666))
            ) {
                Text("Enviar PIN Temporal por Correo", fontSize = 14.sp)
            }
        }
    }
}
