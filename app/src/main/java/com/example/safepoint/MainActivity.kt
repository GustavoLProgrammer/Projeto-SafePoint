package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// ================= CORES =================
val CorAzulEscuro = Color(0xFF0F3E8A)
val CorAzulMedio = Color(0xFF1976D2)
val CorVerdeBotao = Color(0xFF1E8C48)

val FundoGradiente = Brush.linearGradient(
    colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)),
    start = Offset(0f, 0f),
    end = Offset(1200f, 2200f)
)

val BotaoGradiente = Brush.horizontalGradient(
    colors = listOf(CorAzulEscuro, CorVerdeBotao)
)

// ================= DATA CLASSES =================
data class AlertaRota(
    val id: String,
    val titulo: String,
    val dataHora: String,
    val localizacao: String,
    val gravidade: String
)

// ================= SCREENS =================
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Cadastro : Screen("cadastro")
    object RecuperarSenha : Screen("recuperar_senha")
    object Home : Screen("home")
    object Mapa : Screen("mapa")
    object Historico : Screen("historico")
    object Perfil : Screen("perfil")
    object SOS : Screen("sos")
    object Ajuda : Screen("ajuda")
}

// ================= MAIN ACTIVITY =================
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val startDest = if (auth.currentUser != null) Screen.Home.route else Screen.Login.route

                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Login.route) { TelaLogin(navController, auth) }
                    composable(Screen.Cadastro.route) { TelaCadastro(navController, auth) }
                    composable(Screen.RecuperarSenha.route) { TelaRecuperarSenha(navController, auth) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { TelaMapa(navController) }
                    composable(Screen.Historico.route) { TelaHistorico(navController) }
                    composable(Screen.Perfil.route) { TelaPerfil(navController, auth) }
                    composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Ajuda.route) { TelaAjuda(onVoltar = { navController.popBackStack() }) }
                }
            }
        }
    }
}

// ================= LOGOS =================
@Composable
fun LogoPin() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
                    .border(2.dp, Color(0xFF1EA84C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(CorAzulEscuro),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "SAFEPOINT", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CorAzulEscuro, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Sempre te guiando a um ponto seguro", fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
fun LogoEscudo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(75.dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 35.dp, bottomEnd = 35.dp))) {
                Column {
                    Row {
                        Box(modifier = Modifier.size(37.5.dp).background(CorAzulEscuro))
                        Box(modifier = Modifier.size(37.5.dp).background(Color.White))
                    }
                    Row {
                        Box(modifier = Modifier.size(37.5.dp).background(Color.White))
                        Box(modifier = Modifier.size(37.5.dp).background(CorAzulEscuro))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "SAFEPOINT", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CorAzulEscuro, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Sempre te guiando a um ponto seguro", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "SISTEMA INTELIGENTE DE SEGURANÇA E EVACUAÇÃO", fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.sp)
    }
}

// ================= BOTÃO GRADIENTE =================
@Composable
fun BotaoGradiente(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BotaoGradiente, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(texto, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ================= BOTTOM NAVIGATION =================
@Composable
fun BarraNavegacaoInferior(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = {
                if (currentRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Mapa.route,
            onClick = {
                if (currentRoute != Screen.Mapa.route) {
                    navController.navigate(Screen.Mapa.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            },
            icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
            label = { Text("Mapa") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Historico.route,
            onClick = {
                if (currentRoute != Screen.Historico.route) {
                    navController.navigate(Screen.Historico.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Histórico") },
            label = { Text("Histórico") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Perfil.route,
            onClick = {
                if (currentRoute != Screen.Perfil.route) {
                    navController.navigate(Screen.Perfil.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") }
        )
    }
}

// ================= TELA LOGIN =================
@Composable
fun TelaLogin(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var lembrarMe by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(FundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(vertical = 60.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoPin()
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = "Faça seu login", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("E-mail", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CorAzulEscuro,
                        unfocusedBorderColor = Color(0xFFD6DFE8)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    placeholder = { Text("Senha", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) },
                    trailingIcon = {
                        IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                            Icon(
                                if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CorAzulEscuro,
                        unfocusedBorderColor = Color(0xFFD6DFE8)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = lembrarMe,
                            onCheckedChange = { lembrarMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = CorAzulEscuro)
                        )
                        Text("Lembrar-me", fontSize = 13.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { navController.navigate(Screen.RecuperarSenha.route) }) {
                        Text("Esqueci minha senha", color = CorAzulMedio, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                BotaoGradiente(texto = "Entrar") {
                    if (email.isNotBlank() && senha.isNotBlank()) {
                        auth.signInWithEmailAndPassword(email, senha)
                            .addOnSuccessListener {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("Não tem uma conta? ", color = Color.Gray, fontSize = 14.sp)
                    TextButton(onClick = { navController.navigate(Screen.Cadastro.route) }) {
                        Text("Criar conta", color = CorAzulMedio, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= TELA CADASTRO =================
@Composable
fun TelaCadastro(navController: NavController, auth: FirebaseAuth) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var confirmarSenhaVisivel by remember { mutableStateOf(false) }
    var aceitouTermos by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorAzulEscuro)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Voltar", color = CorAzulEscuro, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoEscudo()
            Spacer(modifier = Modifier.height(28.dp))

            Text(text = "Crie sua conta", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Preencha os dados abaixo para se cadastrar", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                placeholder = { Text("Nome completo", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorAzulEscuro,
                    unfocusedBorderColor = Color(0xFFD6DFE8)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("E-mail", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorAzulEscuro,
                    unfocusedBorderColor = Color(0xFFD6DFE8)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it },
                placeholder = { Text("Telefone / Celular", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorAzulEscuro,
                    unfocusedBorderColor = Color(0xFFD6DFE8)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                placeholder = { Text("Senha", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) },
                trailingIcon = {
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorAzulEscuro,
                    unfocusedBorderColor = Color(0xFFD6DFE8)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmarSenha,
                onValueChange = { confirmarSenha = it },
                placeholder = { Text("Confirmar senha", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) },
                trailingIcon = {
                    IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) {
                        Icon(
                            if (confirmarSenhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorAzulEscuro,
                    unfocusedBorderColor = Color(0xFFD6DFE8)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dataNascimento,
                onValueChange = { dataNascimento = it },
                placeholder = { Text("Data de nascimento", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = CorAzulEscuro) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorAzulEscuro,
                    unfocusedBorderColor = Color(0xFFD6DFE8)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = aceitouTermos,
                    onCheckedChange = { aceitouTermos = it },
                    colors = CheckboxDefaults.colors(checkedColor = CorAzulEscuro)
                )
                Text(
                    text = "Eu li e concordo com os Termos de Uso e a Política de Privacidade.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            BotaoGradiente(texto = "Cadastrar") {
                if (!aceitouTermos) {
                    Toast.makeText(context, "Aceite os termos de uso", Toast.LENGTH_SHORT).show()
                    return@BotaoGradiente
                }
                if (email.isNotBlank() && senha.isNotBlank() && senha == confirmarSenha) {
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Verifique os campos e senhas", Toast.LENGTH_SHORT).show()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("Já tem uma conta? ", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Fazer login", color = CorAzulMedio, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ================= TELA RECUPERAR SENHA =================
@Composable
fun TelaRecuperarSenha(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(FundoGradiente)) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(28.dp))
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(vertical = 100.dp)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoPin()
                Spacer(modifier = Modifier.height(28.dp))

                Text(text = "Recuperar senha", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Digite seu e-mail cadastrado para receber as instruções de redefinição",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("E-mail", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CorAzulEscuro,
                        unfocusedBorderColor = Color(0xFFD6DFE8)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                BotaoGradiente(texto = "Enviar Instruções") {
                    if (email.isNotBlank()) {
                        auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(context, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }
        }
    }
}

// ================= TELA HOME =================
@Composable
fun TelaHome(navController: NavController) {
    var nomeUsuario by remember { mutableStateOf("Usuário") }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val nome = user.email?.substringBefore("@")
            if (nome != null) {
                nomeUsuario = nome
            }
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FundoGradiente)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Olá, $nomeUsuario!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sua segurança é nossa prioridade",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.SOS.route) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SOS - Emergência", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("Acione ajuda imediata", fontSize = 13.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFFC62828))
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.Mapa.route) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(CorAzulMedio),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Mapa", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mapa de Evacuação", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                            Text("Veja rotas seguras próximas", fontSize = 13.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CorAzulEscuro)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.Ajuda.route) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(CorVerdeBotao),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Help, contentDescription = "Ajuda", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dúvidas e Ajuda", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorVerdeBotao)
                            Text("Tire suas dúvidas sobre o app", fontSize = 13.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CorVerdeBotao)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Status de Segurança", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF1EA84C)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sistema ativo - Monitorando sua região", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ================= TELA MAPA =================
@Composable
fun TelaMapa(navController: NavController) {
    val context = LocalContext.current
    var localizacaoAtual by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            obterLocalizacao(context) { lat, lng ->
                localizacaoAtual = Pair(lat, lng)
            }
        } else {
            Toast.makeText(context, "Permissão de localização necessária", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE3F2FD))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Map, contentDescription = "Mapa", tint = CorAzulEscuro, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mapa Interativo de Evacuação", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusText = if (localizacaoAtual != null) "GPS Ativo: Rota Segura Calculada" else "Aguardando localização..."
                    Text(statusText, fontSize = 14.sp, color = Color.Gray)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(factory = { ctx ->
                    val mapView = MapView(ctx)
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    mapView.setMultiTouchControls(true)
                    mapView.controller.setZoom(15.0)

                    val lat = localizacaoAtual?.first ?: -23.5505
                    val lng = localizacaoAtual?.second ?: -46.6333
                    val startPoint = GeoPoint(lat, lng)
                    mapView.controller.setCenter(startPoint)

                    val markerAtual = Marker(mapView)
                    markerAtual.position = startPoint
                    markerAtual.title = "Sua localização"
                    markerAtual.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(markerAtual)

                    val pontosEvacuacao = listOf(
                        GeoPoint(lat + 0.005, lng + 0.005),
                        GeoPoint(lat - 0.003, lng + 0.008),
                        GeoPoint(lat + 0.008, lng - 0.004)
                    )

                    pontosEvacuacao.forEachIndexed { index, ponto ->
                        val marker = Marker(mapView)
                        marker.position = ponto
                        marker.title = "Ponto Seguro ${index + 1}"
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(marker)

                        val routeLine = Polyline()
                        routeLine.setPoints(listOf(startPoint, ponto))
                        routeLine.color = Color(0xFF1E8C48).hashCode()
                        routeLine.width = 5f
                        mapView.overlays.add(routeLine)
                    }

                    mapView
                }, modifier = Modifier.fillMaxSize())

                FloatingActionButton(
                    onClick = {
                        val lat = localizacaoAtual?.first
                        val lng = localizacaoAtual?.second
                        if (lat != null && lng != null) {
                            Toast.makeText(context, "Lat: $lat, Lng: $lng", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = CorAzulMedio
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Centralizar", tint = Color.White)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                BotaoGradiente(texto = "Iniciar Rota de Fuga") {
                    if (localizacaoAtual != null) {
                        Toast.makeText(context, "Rota de fuga iniciada! Siga as indicações.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Aguardando localização...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fineGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return

    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    if (location != null) {
        onLocationReceived(location.latitude, location.longitude)
    } else {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f) { loc ->
            onLocationReceived(loc.latitude, loc.longitude)
        }
    }
}

// ================= TELA HISTÓRICO =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistorico(navController: NavController) {
    var textoPesquisa by remember { mutableStateOf("") }

    val listaAlertas = remember {
        listOf(
            AlertaRota("1", "Enchente Severa - Rio Doce", "15/08/2024, 14:30", "Rua Frei Caneca, 340 - Centro, São Paulo - SP", "Crítica"),
            AlertaRota("2", "Deslizamento de Terra - Morro da Cruz", "10/08/2024, 09:15", "Rua Pedro Álvares Cabral, 12 - Morro da Cruz, Porto Alegre - RS", "Alta"),
            AlertaRota("3", "Tempestade Tropical - Litoral Norte", "05/08/2024, 18:00", "Praia Grande - Ubatuba - SP", "Média"),
            AlertaRota("4", "Alerta de Inundação - Rio Tietê", "01/08/2024, 11:00", "Marginal Tietê - Ponte das Bandeiras, São Paulo - SP", "Alta")
        )
    }

    val alertasFiltrados = listaAlertas.filter { alerta ->
        alerta.titulo.contains(textoPesquisa, ignoreCase = true) || alerta.localizacao.contains(textoPesquisa, ignoreCase = true)
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FundoGradiente)
                    .padding(16.dp)
            ) {
                Text("Histórico de Alertas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedTextField(
                value = textoPesquisa,
                onValueChange = { textoPesquisa = it },
                placeholder = { Text("Pesquisar alertas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alertasFiltrados) { alerta ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    alerta.titulo,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CorAzulEscuro,
                                    modifier = Modifier.weight(1f)
                                )
                                val corGravidade = when (alerta.gravidade) {
                                    "Crítica" -> Color(0xFFE53935)
                                    "Alta" -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(corGravidade)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        alerta.gravidade,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(alerta.localizacao, fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alerta.dataHora, fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ================= TELA PERFIL =================
@Composable
fun TelaPerfil(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            email = user.email ?: ""
            val displayName = user.displayName
            if (displayName != null) {
                nome = displayName
            }
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FundoGradiente)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = CorAzulEscuro, modifier = Modifier.size(64.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val nomeExibido = if (nome.isEmpty()) "Usuário" else nome
                    Text(nomeExibido, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Column(
                    Text(nomeExibido, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
            if (displayName != null) {
                .fillMaxSize()
                                        color = Color.White,
                            }
                    .padding(24.dp),
            Box(
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = FontWeight.Bold,
                            .clip(CircleShape)
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                    Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                .padding(paddingValues)
                                        fontWeight = FontWeight.Bold
                            Spacer(modifier = Modifier.height(8.dp))
                modifier = Modifier
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                            .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(16.dp)
                }
                .background(Color(0xFFF6F8FA))
                            Text(alerta.localizacao, fontSize = 13.sp, color = Color.Gray)
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                        contentAlignment = Alignment.Center
            ) {
                shape = RoundedCornerShape(12.dp),
                    enabled = false
                    ) {
                Text("Editar Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                singleLine = true
                )
                        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = CorAzulEscuro, modifier = Modifier.size(64.dp))
            )

                    }

                OutlinedTextField(
            LazyColumn(
                    value = telefone,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    onValueChange = { telefone = it },
                verticalArrangement = Arrangement.spacedBy(12.dp)
                    label = { Text("Telefone") },
            ) {
                    modifier = Modifier.fillMaxWidth(),
                items(alertasFiltrados) { alerta ->
                    shape = RoundedCornerShape(12.dp),
                    Card(
                    singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                )
                    ) {

                        Column(modifier = Modifier.padding(16.dp)) {
                BotaoGradiente(texto = "Salvar Alterações") {
                            Row(
                    Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Sair", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

            
