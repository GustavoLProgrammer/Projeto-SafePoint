package com.example.safepoint

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class AlertaRota(
    val id: String,
    val titulo: String,
    val dataHora: String,
    val localizacao: String,
    val gravidade: String
)

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Cadastro : Screen("cadastro")
    object RecuperarSenha : Screen("recuperar_senha")
    object Home : Screen("home")
    object Mapa : Screen("mapa")
    object Historico : Screen("historico")
    object Perfil : Screen("perfil")
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val currentUser = auth.currentUser
                val startDest = if (currentUser != null) Screen.Home.route else Screen.Login.route

                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Login.route) { TelaLogin(navController, auth) }
                    composable(Screen.Cadastro.route) { TelaCadastro(navController, auth) }
                    composable(Screen.RecuperarSenha.route) { TelaRecuperarSenha(navController, auth) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { TelaMapaSafepoint(navController) }
                    composable(Screen.Historico.route) { TelaHistoricoSafepoint(navController) }
                    composable(Screen.Perfil.route) { TelaEditarPerfil(navController, auth) }
                }
            }
        }
    }
}

val AzulFundoGradiente = Brush.verticalGradient(
    colors = listOf(Color(0xFF0C2B63), Color(0xFF13509E), Color(0xFF1B8C48))
)
val CorAzulEscuro = Color(0xFF1B3B6F)
val CorVerdeBotao = Brush.horizontalGradient(listOf(Color(0xFF13509E), Color(0xFF1B8C48)))

@Composable
fun LogoSafepointMockupExato() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Logo vetorizada idêntica ao design original (Escudo com marcador e tom verde)
        Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(68.dp)) {
                val width = size.width
                val height = size.height
                // Contorno externo do escudo em verde/azul
                drawCircle(
                    color = Color(0xFF1B8C48).copy(alpha = 0.2f),
                    radius = width / 2f,
                    center = Offset(width / 2f, height / 2f)
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Logo",
                    tint = CorAzulEscuro,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "SAFEPOINT", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro, letterSpacing = 2.sp)
        Text(text = "Sempre te guiando a um ponto seguro", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun BarraNavegacaoInferior(navController: NavController, abaAtual: String) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = abaAtual == "Início",
            onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") }
        )
        NavigationBarItem(
            selected = abaAtual == "Mapa",
            onClick = { navController.navigate(Screen.Mapa.route) },
            icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
            label = { Text("Mapa") }
        )
        NavigationBarItem(
            selected = abaAtual == "Histórico",
            onClick = { navController.navigate(Screen.Historico.route) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Histórico") },
            label = { Text("Histórico") }
        )
        NavigationBarItem(
            selected = abaAtual == "Perfil",
            onClick = { navController.navigate(Screen.Perfil.route) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") }
        )
    }
}

@Composable
fun TelaLogin(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var lembrarMe by remember { mutableStateOf(false) }
    var senhaVisivel by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(AzulFundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoSafepointMockupExato()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Faça seu login", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("E-mail") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    placeholder = { Text("Senha") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) },
                    trailingIcon = {
                        IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                            Icon(if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = lembrarMe, onCheckedChange = { lembrarMe = it })
                        Text(text = "Lembrar-me", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { navController.navigate(Screen.RecuperarSenha.route) }) {
                        Text("Esqueci minha senha", color = Color(0xFF1976D2), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && senha.isNotBlank()) {
                            auth.signInWithEmailAndPassword(email, senha)
                                .addOnSuccessListener {
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Erro ao entrar: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(CorVerdeBotao), contentAlignment = Alignment.Center) {
                        Text("Entrar", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { navController.navigate(Screen.Cadastro.route) }) {
                    Text("Não tem uma conta? ", color = Color.Gray, fontSize = 13.sp)
                    Text("Criar conta", color = CorAzulEscuro, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TelaCadastro(navController: NavController, auth: FirebaseAuth) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var termosAceitos by remember { mutableStateOf(false) }
    var senhaVisivel by remember { mutableStateOf(false) }
    val context = navController.context

    Box(modifier = Modifier.fillMaxSize().background(AzulFundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.Start)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorAzulEscuro)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voltar", color = CorAzulEscuro, fontWeight = FontWeight.Bold)
                    }
                }
                LogoSafepointMockupExato()
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Crie sua conta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text(text = "Preencha os dados abaixo para se cadastrar", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome completo") }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, placeholder = { Text("Telefone / Celular") }, leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha") }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { IconButton(onClick = { senhaVisivel = !senhaVisivel }) { Icon(if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = confirmarSenha, onValueChange = { confirmarSenha = it }, placeholder = { Text("Confirmar senha") }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = dataNascimento, onValueChange = { dataNascimento = it }, placeholder = { Text("Data de nascimento") }, leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), singleLine = true)

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = termosAceitos, onCheckedChange = { termosAceitos = it })
                    Text(text = "Eu li e concordo com os Termos de Uso e a Política de Privacidade.", fontSize = 9.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && senha.isNotBlank() && senha == confirmarSenha && termosAceitos) {
                            auth.createUserWithEmailAndPassword(email, senha)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(context, "Preencha todos os campos e aceite os termos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(CorVerdeBotao), contentAlignment = Alignment.Center) {
                        Text("Cadastrar", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Já tem uma conta? ", color = Color.Gray, fontSize = 12.sp)
                    Text("Fazer login", color = Color(0xFF1976D2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TelaRecuperarSenha(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    val context = navController.context

    Box(modifier = Modifier.fillMaxSize().background(AzulFundoGradiente), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorAzulEscuro)
                }
                LogoSafepointMockupExato()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Recuperar senha", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Digite seu e-mail cadastrado para receber as instruções de redefinição", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (email.isNotBlank()) {
                            auth.sendPasswordResetEmail(email).addOnSuccessListener {
                                Toast.makeText(context, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }.addOnFailureListener {
                                Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(CorVerdeBotao), contentAlignment = Alignment.Center) {
                        Text("Enviar Instruções", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TelaHome(navController: NavController) {
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController, abaAtual = "Início") }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CorAzulEscuro, modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Bem-vindo ao Safepoint!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Sua plataforma de monitoramento e rotas seguras.", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TelaMapaSafepoint(navController: NavController) {
    val context = LocalContext.current
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissao = concedida
        if (concedida) {
            Toast.makeText(context, "GPS Conectado com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController, abaAtual = "Mapa") }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!temPermissao) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6F9)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = CorAzulEscuro, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Acesso à Localização Necessário", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Para carregar o mapa interativo de evacuação em tempo real, permita o acesso à sua localização.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CorAzulEscuro)
                    ) {
                        Text("Permitir Localização", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Mapa interativo com grade, simulação de marcadores de pontos seguros e rota
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD))) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Linhas de grade do mapa simulado
                        for (i in 1..10) {
                            drawLine(Color(0xFFBBDEFB), Offset(0f, i * (h / 11)), Offset(w, i * (h / 11)), 1.5f)
                            drawLine(Color(0xFFBBDEFB), Offset(i * (w / 11), 0f), Offset(i * (w / 11), h), 1.5f)
                        }
                    }

                    // Marcadores interativos de pontos seguros no mapa
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF1B8C48))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("GPS Ativo • Rota Segura Calculada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CorAzulEscuro)
                                    Text("2 Pontos de Evacuação abertos na região", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            // Ponto Seguro 1 (Marcador visível)
                            Card(
                                shape = RoundedCornerShape(50.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B8C48)),
                                modifier = Modifier.clickable { Toast.makeText(context, "Ponto Seguro: Ginásio Municipal (350m)", Toast.LENGTH_SHORT).show() }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ponto Seguro: Ginásio Municipal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Navegação em Andamento", fontWeight = FontWeight.Bold, color = CorAzulEscuro, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Siga em direção à Praça Central para evacuação rápida.", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { Toast.makeText(context, "Rota de fuga encerrada com segurança.", Toast.LENGTH_SHORT).show() },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                                    ) {
                                        Text("Encerrar Navegação", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoSafepoint(navController: NavController) {
    var textoPesquisa by remember { mutableStateOf("") }
    var filtroSelecionado by remember { mutableStateOf("Todas") }

    val listaAlertas = remember {
        listOf(
            AlertaRota("1", "Enchente Severa - Rio Doce", "15/08/2024, 14:30", "Rua Frei Caneca, 340 - Centro, São Paulo - SP", "Crítica"),
            AlertaRota("2", "Deslizamento de Terra - Morro da Cruz", "10/08/2024, 09:15", "Rua Pedro Álvares Cabral, 12 - Morro da Cruz, Porto Alegre - RS", "Alta"),
            AlertaRota("3", "Tempestade Tropical - Litoral Norte", "05/08/2024, 18:00", "Praia Grande - Ubatuba - SP", "Média"),
            AlertaRota("4", "Alerta de Inundação - Rio Tietê", "01/08/2024, 11:00", "Marginal Tietê - Ponte das Bandeiras, São Paulo - SP", "Alta")
        )
    }

    val alertasFiltrados = listaAlertas.filter {
        it.titulo.contains(textoPesquisa, ignoreCase = true) || it.localizacao.contains(textoPesquisa, ignoreCase = true)
    }

    val opcoesFiltro = listOf("Todas", "Hoje", "Esta semana", "Este mês", "Período")

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController, abaAtual = "Histórico") }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA)).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = CorAzulEscuro)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Histórico de Alertas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                }
            }

            OutlinedTextField(
                value = textoPesquisa,
                onValueChange = { textoPesquisa = it },
                placeholder = { Text("Pesquisar alertas") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(opcoesFiltro) { opcao ->
                    FilterChip(
                        selected = filtroSelecionado == opcao,
                        onClick = { filtroSelecionado = opcao },
                        label = { Text(opcao) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(alertasFiltrados) { alertaItem ->
                    val corGravidade = when (alertaItem.gravidade) {
                        "Crítica" -> Color(0xFFD32F2F)
                        "Alta" -> Color(0xFFF57C00)
                        else -> Color(0xFFFBC02D)
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(alertaItem.titulo, fontWeight = FontWeight.Bold, color = CorAzulEscuro, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alertaItem.dataHora, color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alertaItem.localizacao, color = Color.DarkGray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(corGravidade, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(alertaItem.gravidade, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaEditarPerfil(navController: NavController, auth: FirebaseAuth) {
    val context = navController.context
    val currentUser = auth.currentUser
    val db = FirebaseFirestore.getInstance()

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var telefone by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("01/01/2000") }
    var fotoUrl by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            fotoUrl = it.toString()
            Toast.makeText(context, "Foto selecionada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        nome = document.getString("nome") ?: ""
                        telefone = document.getString("telefone") ?: ""
                        dataNascimento = document.getString("dataNascimento") ?: "01/01/2000"
                        fotoUrl = document.getString("fotoUrl") ?: ""
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AzulFundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.Start)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorAzulEscuro)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voltar", color = CorAzulEscuro)
                    }
                }

                Text(text = "Editar Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoUrl.isNotBlank()) {
                        AsyncImage(model = fotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CorAzulEscuro, modifier = Modifier.size(40.dp))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .background(CorAzulEscuro, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome Sobrenome") }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, enabled = false, placeholder = { Text("exemplo@email.com") }, leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, placeholder = { Text("(00) 00000-0000") }, leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = dataNascimento, onValueChange = { dataNascimento = it }, placeholder = { Text("01/01/2000") }, leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            val dadosUsuario = hashMapOf(
                                "nome" to nome,
                                "email" to email,
                                "telefone" to telefone,
                                "dataNascimento" to dataNascimento,
                                "fotoUrl" to fotoUrl
                            )
                            db.collection("usuarios").document(uid).set(dadosUsuario)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        } ?: Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(CorVerdeBotao), contentAlignment = Alignment.Center) {
                        Text("Salvar Alterações", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

