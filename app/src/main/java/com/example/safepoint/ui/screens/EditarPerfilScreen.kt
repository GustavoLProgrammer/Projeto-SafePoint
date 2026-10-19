package com.example.safepoint.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    onVoltar: () -> Unit,
    onSalvar: (String, String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf("Nome Sobrenome") }
    var email by remember { mutableStateOf("exemplo@email.com") }
    var telefone by remember { mutableStateOf("(00) 00000-0000") }
    var dataNascimento by remember { mutableStateOf("01/01/2000") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }

    var mostrarDatePicker by remember { mutableStateOf(false) }

    // Launcher para selecionar imagem da galeria
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoUri = uri
        }
    }

    val azulEscuro = Color(0xFF0F3E8A)
    val verdeGradiente = Color(0xFF1E9B44)
    val cinzaTexto = Color(0xFF7A8B9E)
    val cinzaBorda = Color(0xFFD6DFE8)
    val textoDigitado = Color(0xFF1A1A1A)

    val gradientFundo = Brush.linearGradient(
        colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B))
    )

    val gradientBotao = Brush.horizontalGradient(
        colors = listOf(azulEscuro, verdeGradiente)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientFundo)
    ) {
        Card(
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 50.dp, bottom = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Cabeçalho / Botão Voltar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = azulEscuro)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Editar Perfil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar com suporte a seleção de imagem via Coil
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(cinzaBorda)
                        .clickable { launcher.launch("image/*") }
                ) {
                    if (fotoUri != null) {
                        AsyncImage(
                            model = fotoUri,
                            contentDescription = "Foto de Perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = azulEscuro,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    // Ícone da câmara sobreposto
                    Surface(
                        shape = CircleShape,
                        color = azulEscuro,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Alterar Foto",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Campo Nome
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    placeholder = { Text("Nome completo", color = cinzaTexto) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = azulEscuro) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textoDigitado,
                        unfocusedTextColor = textoDigitado,
                        focusedBorderColor = azulEscuro,
                        unfocusedBorderColor = cinzaBorda
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Campo E-mail
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("E-mail", color = cinzaTexto) },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = azulEscuro) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textoDigitado,
                        unfocusedTextColor = textoDigitado,
                        focusedBorderColor = azulEscuro,
                        unfocusedBorderColor = cinzaBorda
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Telefone
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it },
                    placeholder = { Text("Telefone", color = cinzaTexto) },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = azulEscuro) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textoDigitado,
                        unfocusedTextColor = textoDigitado,
                        focusedBorderColor = azulEscuro,
                        unfocusedBorderColor = cinzaBorda
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Data de Nascimento
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dataNascimento,
                        onValueChange = { },
                        placeholder = { Text("Data de nascimento", color = cinzaTexto) },
                        leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null, tint = azulEscuro) },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = azulEscuro) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = cinzaBorda,
                            disabledTextColor = textoDigitado,
                            disabledPlaceholderColor = cinzaTexto,
                            disabledLeadingIconColor = azulEscuro,
                            disabledTrailingIconColor = azulEscuro
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botão Salvar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradientBotao)
                        .clickable {
                            onSalvar(nome, email, telefone, dataNascimento)
                        }
                ) {
                    Text(
                        text = "Salvar Alterações",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Diálogo do DatePicker
    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.timeInMillis = selectedMillis
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                            formatter.timeZone = TimeZone.getTimeZone("UTC")
                            dataNascimento = formatter.format(calendar.time)
                        }
                        mostrarDatePicker = false
                    }
                ) {
                    Text("OK", color = azulEscuro)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
