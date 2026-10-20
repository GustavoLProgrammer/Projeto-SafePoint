package com.example.safepoint

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.safepoint.ui.screens.EditarPerfilScreen

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
                    composable(Screen.Historico.route) { TelaHistoricoSafepoint(navController) }
                    composable(Screen.Perfil.route) {
                        EditarPerfilScreen(
                            onVoltar = { navController.popBackStack() },
                            onSalvar = { nome, email, telefone, dataNascimento ->
                                Toast.makeText(applicationContext, "Perfil salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

val AzulFundoGradiente = Brush.verticalGradient(
    colors = listOf(Color(0xFF0C2B63), Color(0xFF13509E), Color(0xFF1B8C48))
)
val CorAzulEscuro = Color(0xFF1B3B6F)
val CorVerdeBotao = Color(0xFF1E8C48)

@Composable
fun LogoSafepoint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Logo",
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "SAFEPOINT", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro, letterSpacing = 2.sp)
        Text(text = "Sempre te guiando a um ponto seguro", fontSize = 12.sp, color = Color.Gray)
    }
}

// ================= TELA DE LOGIN =================
@Composable
fun TelaLogin(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    val context = navController.context

    Box(modifier = Modifier.fillMaxSize().background(AzulFundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoSafepoint()
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Faça seu login", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(20.dp))

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
                            Icon(if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { navController.navigate(Screen.RecuperarSenha.route) }) {
                        Text("Esqueci minha senha", color = Color(0xFF1976D2), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CorVerdeBotao)
                ) {
                    Text("Entrar", fontSize = 16.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { navController.navigate(Screen.Cadastro.route) }) {
                    Text("Não tem uma conta? Criar conta", color = CorAzulEscuro, fontSize = 14.sp)
                }
            }
        }
    }
}

// ================= TELA DE CADASTRO =================
@Composable
fun TelaCadastro(navController: NavController, auth: FirebaseAuth) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    val context = navController.context

    Box(modifier = Modifier.fillMaxSize().background(AzulFundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorAzulEscuro)
                }
                LogoSafepoint()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Crie sua conta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome completo") }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, placeholder = { Text("Telefone / Celular") }, leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha") }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = confirmarSenha, onValueChange = { confirmarSenha = it }, placeholder = { Text("Confirmar senha") }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && senha.isNotBlank() && senha == confirmarSenha) {
                            auth.createUserWithEmailAndPassword(email, senha)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(context, "Verifique os campos e senhas", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CorVerdeBotao)
                ) {
                    Text("Cadastrar", fontSize = 16.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Já tem uma conta? Fazer login", color = Color(0xFF1976D2), fontSize = 14.sp)
                }
            }
        }
    }
}

// ================= TELA RECUPERAR SENHA =================
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
                LogoSafepoint()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Recuperar senha", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Digite seu e-mail cadastrado para receber as instruções", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CorVerdeBotao)
                ) {
                    Text("Enviar Instruções", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

// ================= TELA HOME =================
@Composable
fun TelaHome(navController: NavController) {
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController, abaAtual = "Início") }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Bem-vindo ao Safepoint!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Sua plataforma de monitoramento e rotas seguras.", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

// ================= TELA HISTÓRICO =================
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
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = "Logo", tint = Color(0xFF1E88E5), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Safepoint", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                }
                IconButton(onClick = {}) { Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notificações", tint = CorAzulEscuro) }
            }

            Text(text = "Histórico de Rotas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro, modifier = Modifier.align
