package com.example.sgnatureraloy.ui.signature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sgnatureraloy.data.model.SignatureProcess
import com.example.sgnatureraloy.data.model.Signer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureDetailScreen(
    signature: SignatureProcess,
    onBack: () -> Unit,
    onSignClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Firma", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF1F5F9))
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Referencia: ${signature.referenceId}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Estado: ${signature.status}", color = Color(0xFF2563EB))
                    Text("Creado: ${signature.createdAt}", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Firmantes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(signature.firmantes ?: emptyList()) { signer ->
                    SignerItem(signer)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (signature.status != "COMPLETED" && signature.status != "CERRADO") {
                Button(
                    onClick = {
                        val url = "https://kiosko.raloy.com.mx/firmadigital/proceso/${signature.tokenAcceso}"
                        onSignClick(url)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("IR A FIRMAR DOCUMENTO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SignerItem(signer: Signer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(signer.name, fontWeight = FontWeight.SemiBold)
                Text(signer.email, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = if (signer.fechaFirma != null) "FIRMADO" else "PENDIENTE",
                color = if (signer.fechaFirma != null) Color(0xFF10B981) else Color(0xFFF59E0B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
