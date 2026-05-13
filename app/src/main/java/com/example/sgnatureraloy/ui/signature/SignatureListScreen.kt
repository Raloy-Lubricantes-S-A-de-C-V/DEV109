package com.example.sgnatureraloy.ui.signature

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sgnatureraloy.core.events.RefreshEventBus
import com.example.sgnatureraloy.data.model.SignatureProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureListScreen(
    viewModel: SignatureViewModel,
    userEmail: String,
    onSignatureClick: (SignatureProcess) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val signatures by viewModel.filteredSignatures.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Solo cargar datos si el email es válido (después de un login manual)
    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty() && userEmail != "null") {
            viewModel.loadSignatures(userEmail)
        }
    }

    // Escuchar eventos de actualización en tiempo real (FCM)
    LaunchedEffect(Unit) {
        RefreshEventBus.refreshEvent.collect {
            if (userEmail.isNotEmpty() && userEmail != "null") {
                viewModel.loadSignatures(userEmail)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Firmas Digitales",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.loadSignatures(userEmail) },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menú",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                Log.d("FIRMA", "SignatureListScreen: Click en Cerrar Sesión")
                                Toast.makeText(context, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Fila de Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterButton(
                    text = "PENDIENTES",
                    isActive = selectedFilter == FilterType.PENDIENTE,
                    activeColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilter(FilterType.PENDIENTE) }
                )
                FilterButton(
                    text = "TERMINADOS",
                    isActive = selectedFilter == FilterType.TERMINADO,
                    activeColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilter(FilterType.TERMINADO) }
                )
                FilterButton(
                    text = "CANCELADOS",
                    isActive = selectedFilter == FilterType.CANCELADO,
                    activeColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilter(FilterType.CANCELADO) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (signatures.isEmpty()) {
                    Text(
                        text = "No hay firmas en esta categoría",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(signatures) { signature ->
                            SignatureItem(signature, onSignatureClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else Color.White,
            contentColor = if (isActive) Color.White else Color(0xFF64748B)
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = if (isActive) ButtonDefaults.buttonElevation(defaultElevation = 4.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun SignatureItem(
    signature: SignatureProcess,
    onClick: (SignatureProcess) -> Unit
) {
    val contentColor = when (signature.status.uppercase()) {
        "COMPLETED", "CERRADO" -> Color(0xFF15803D)
        "PROCESSING", "PROCESO" -> Color(0xFF0369A1)
        "CANCELLED", "CANCELADO" -> Color(0xFFB91C1C)
        else -> Color(0xFF1E293B)
    }

    // Efecto de pulso para firmas que requieren mi acción
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by if (signature.requiresMySignature) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(signature) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (signature.requiresMySignature) 3.dp else 1.dp,
            color = contentColor.copy(alpha = alpha)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = signature.referenceId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )
                StatusBadge(signature.status, contentColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fecha: ${signature.createdAt}",
                fontSize = 14.sp,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatusBadge(status: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
