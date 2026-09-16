package com.example.safepoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Cores do SafePoint
val SafePointBlue = Color(0xFF0D47A1)
val SafePointGreen = Color(0xFF2E7D32)
val GradientStart = Color(0xFF0D47A1)
val GradientEnd = Color(0xFF2E7D32)

// Cor do texto digitado nos campos
val TextColorDark = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroScreen(
    onVoltarParaLogin: () -> Unit,
    onCadastroSucesso: (String, String) -> Unit,
    viewModel: CadastroViewModel = viewModel()
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }

    var senhaVisivel by remember { mutableStateOf(false) }
    var confirmarSenhaVisivel by remember { mutableStateOf(false) }
    var aceitouTermos by remember { mutableStateOf(false) }

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var erroLocal by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sucesso by viewModel.cadastroSucesso.collectAsState()

    LaunchedEffect(sucesso) {
        if (sucesso) {
            onCadastroSucesso(email, senha)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botão Voltar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onVoltarParaLogin) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = SafePointBlue)
            }
            Text(
                text = "Voltar",
                color = SafePointBlue,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logo
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Logo",
            tint = SafePointBlue,
            modifier = Modifier.size(80.dp)
        )

        Text(
            text = "SAFEPOINT",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = SafePointBlue
        )
        Text(
            text = "Sempre te guiando a um ponto seguro",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = "SISTEMA INTELIGENTE DE SEGURANÇA E EVACUAÇÃO",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Crie sua conta",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SafePointBlue
        )
        Text(
            text = "Preencha os dados abaixo para se cadastrar",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Campo Nome
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            placeholder = { Text("Nome completo", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextColorDark,
                unfocusedTextColor = TextColorDark,
                focusedBorderColor = SafePointBlue,
                unfocusedBorderColor = Color(0xFFD6DFE8),
                focusedLeadingIconColor = SafePointBlue,
                unfocusedLeadingIconColor = SafePointBlue
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo E-mail
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("E-mail", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextColorDark,
                unfocusedTextColor = TextColorDark,
                focusedBorderColor = SafePointBlue,
                unfocusedBorderColor = Color(0xFFD6DFE8),
                focusedLeadingIconColor = SafePointBlue,
                unfocusedLeadingIconColor = SafePointBlue
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Telefone
        OutlinedTextField(
            value = telefone,
            onValueChange = { telefone = it },
            placeholder = { Text("Telefone / Celular", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextColorDark,
                unfocusedTextColor = TextColorDark,
                focusedBorderColor = SafePointBlue,
                unfocusedBorderColor = Color(0xFFD6DFE8),
                focusedLeadingIconColor = SafePointBlue,
                unfocusedLeadingIconColor = SafePointBlue
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Senha
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            placeholder = { Text("Senha", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                    Icon(
                        imageVector = if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextColorDark,
                unfocusedTextColor = TextColorDark,
                focusedBorderColor = SafePointBlue,
                unfocusedBorderColor = Color(0xFFD6DFE8),
                focusedLeadingIconColor = SafePointBlue,
                unfocusedLeadingIconColor = SafePointBlue,
                focusedTrailingIconColor = SafePointBlue,
                unfocusedTrailingIconColor = SafePointBlue
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Confirmar Senha
        OutlinedTextField(
            value = confirmarSenha,
            onValueChange = { confirmarSenha = it },
            placeholder = { Text("Confirmar senha", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) {
                    Icon(
                        imageVector = if (confirmarSenhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextColorDark,
                unfocusedTextColor = TextColorDark,
                focusedBorderColor = SafePointBlue,
                unfocusedBorderColor = Color(0xFFD6DFE8),
                focusedLeadingIconColor = SafePointBlue,
                unfocusedLeadingIconColor = SafePointBlue,
                focusedTrailingIconColor = SafePointBlue,
                unfocusedTrailingIconColor = SafePointBlue
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Data de Nascimento (com calendário)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dataNascimento,
                onValueChange = { },
                placeholder = { Text("Data de nascimento", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFD6DFE8),
                    disabledTextColor = TextColorDark,
                    disabledPlaceholderColor = Color.Gray,
                    disabledLeadingIconColor = SafePointBlue,
                    disabledTrailingIconColor = SafePointBlue
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { mostrarDatePicker = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checkbox Termos
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = aceitouTermos,
                onCheckedChange = { aceitouTermos = it },
                colors = CheckboxDefaults.colors(checkedColor = SafePointBlue)
            )
            Text(
                text = "Eu li e concordo com os Termos de Uso e a Política de Privacidade.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botão Cadastrar
        Button(
            onClick = {
                erroLocal = when {
                    nome.isBlank() -> "Preencha o nome completo."
                    email.isBlank() -> "Preencha o e-mail."
                    telefone.isBlank() -> "Preencha o telefone."
                    senha.isBlank() -> "Preencha a senha."
                    senha != confirmarSenha -> "As senhas não coincidem."
                    dataNascimento.isBlank() -> "Selecione a data de nascimento."
                    !aceitouTermos -> "Você precisa aceitar os Termos de Uso."
                    else -> null
                }

                if (erroLocal == null) {
                    viewModel.cadastrarUsuario(
                        nome = nome,
                        email = email,
                        telefone = telefone,
                        senha = senha,
                        dataNascimento = dataNascimento
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Cadastrar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Mensagens de erro
        val mensagemErro = erroLocal ?: errorMessage
        if (mensagemErro != null) {
            Text(
                text = mensagemErro,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link Fazer Login
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Já tem uma conta? ", color = Color.Gray)
            Text(
                "Fazer login",
                color = SafePointBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onVoltarParaLogin() }
            )
        }
    }

    // Diálogo do calendário
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
                            calendar.add(Calendar.HOUR_OF_DAY, 12)

                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                            formatter.timeZone = TimeZone.getTimeZone("UTC")
                            dataNascimento = formatter.format(calendar.time)
                        }
                        mostrarDatePicker = false
                    }
                ) {
                    Text("OK", color = SafePointBlue)
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