package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

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

val LogoGradiente = Brush.radialGradient(
    colors = listOf(Color(0xFF1EA84C), Color(0xFF1976D2), Color(0xFF0F3E8A)),
    center = Offset(0.5f, 0.5f),
    radius = 0.7f
)

// ================= DATA CLASSES =================
data class AlertaRota(
    val id: String,
    val titulo: String,
    val dataHora: String,
    val localizacao: String,
    val gravidade: String,
    val tipo: String,
    val latitude: Double,
    val longitude: Double,
    val distancia: Double = 0.0
)

// ================= SCREENS =================
sealed class Screen(val route: String) {
    object Termos : Screen("termos")
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

// ================= PREFS HELPER =================
object PrefsHelper {
    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("safepoint_prefs", Context.MODE_PRIVATE)

    fun getUserId(context: Context): String? =
        getPrefs(context).getString("user_id", null)

    fun setUserId(context: Context, id: String) {
        getPrefs(context).edit().putString("user_id", id).apply()
    }

    fun clearUserId(context: Context) {
        getPrefs(context).edit().remove("user_id").apply()
    }

    fun isTermosAceitos(context: Context): Boolean =
        getPrefs(context).getBoolean("termos_aceitos", false)

    fun setTermosAceitos(context: Context, aceito: Boolean) {
        getPrefs(context).edit().putBoolean("termos_aceitos", aceito).apply()
    }
}

// ================= MAIN ACTIVITY =================
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // CRÍTICO: Configurar User-Agent ANTES de qualquer coisa do OSMDroid
        Configuration.getInstance().userAgentValue = "${packageName}/1.0"
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val termosAceitos = PrefsHelper.isTermosAceitos(context)
                val currentUser = auth.currentUser

                val startDest = when {
                    !termosAceitos -> Screen.Termos.route
                    currentUser == null -> Screen.Login.route
                    else -> Screen.Home.route
                }

                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Termos.route) {
                        TelaTermosUso(
                            onAceitar = {
                                PrefsHelper.setTermosAceitos(context, true)
                                if (currentUser != null) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Termos.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Termos.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable(Screen.Login.route) { TelaLogin(navController, auth) }
                    composable(Screen.Cadastro.route) { TelaCadastro(navController, auth) }
                    composable(Screen.RecuperarSenha.route) { TelaRecuperarSenha(navController, auth) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { TelaMapa(navController) }
                    composable(Screen.Historico.route) { TelaHistorico(navController) }
                    composable(Screen.Perfil.route) { TelaPerfil(navController, auth, db) }
                    composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Ajuda.route) { TelaAjuda(onVoltar = { navController.popBackStack() }) }
                }
            }
        }
    }
}

// ================= LOGO PIN COM GRADIENTE (Login/Recuperar) =================
@Composable
fun LogoPinSafepoint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            // Forma de pin/marcador de mapa
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 50.dp,
                            topEnd = 50.dp,
                            bottomStart = 50.dp,
                            bottomEnd = 50.dp
                        )
                    )
                    .background(LogoGradiente),
                contentAlignment = Alignment.Center
            ) {
                // Círculo externo verde
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                        .border(3.dp, Color(0xFF1EA84C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Círculo azul médio
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CorAzulEscuro),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ponto branco no centro
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
            // Ponta do pin (triângulo)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .size(0.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SAFEPOINT",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CorAzulEscuro,
            letterSpacing = 2.sp
        )
    }
}

// ================= LOGO ESCUDO PROFISSIONAL (Cadastro) =================
@Composable
fun LogoEscudoSafepoint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            // Escudo azul sólido com borda
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 45.dp,
                            bottomEnd = 45.dp
                        )
                    )
                    .background(CorAzulEscuro)
                    .border(3.dp, CorAzulEscuro, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 45.dp, bottomEnd = 45.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Cruz branca dentro do escudo
                Column {
                    Row {
                        Box(modifier = Modifier.size(45.dp).background(CorAzulEscuro))
                        Box(modifier = Modifier.size(45.dp).background(Color.White))
                    }
                    Row {
                        Box(modifier = Modifier.size(45.dp).background(Color.White))
                        Box(modifier = Modifier.size(45.dp).background(CorAzulEscuro))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SAFEPOINT",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CorAzulEscuro,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sempre te guiando a um ponto seguro",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SISTEMA INTELIGENTE DE SEGURANÇA E EVACUAÇÃO",
            fontSize = 10.sp,
            color = Color.Gray,
            letterSpacing = 1.5.sp
        )
    }
}

// ================= BOTÃO GRADIENTE =================
@Composable
fun BotaoGradiente(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(BotaoGradiente, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(texto, fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

// ================= TELA TERMOS DE USO =================
@Composable
fun TelaTermosUso(onAceitar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoGradiente)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        LogoPinSafepoint()
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Termos de Uso e Política de Privacidade",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CorAzulEscuro
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Bem-vindo ao SafePoint!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ao utilizar nosso aplicativo, você concorda com os seguintes termos:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("1. Coleta de Dados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text(
                    "Coletamos sua localização GPS para calcular rotas de evacuação seguras em tempo real. Seus dados de localização são processados de forma segura e não são compartilhados com terceiros.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("2. Uso do Aplicativo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text(
                    "O SafePoint é uma ferramenta de apoio em situações de emergência. Em caso de risco iminente, priorize sempre as orientações das autoridades locais.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("3. Privacidade", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text(
                    "Seus dados pessoais (nome, e-mail, telefone) são armazenados de forma criptografada e utilizados apenas para melhorar sua experiência no aplicativo.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("4. Funcionalidade SOS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text(
                    "O botão SOS aciona os serviços de emergência (190, 192, 193) e compartilha sua localização com contatos de confiança previamente cadastrados.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("5. Cancelamento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text(
                    "Você pode solicitar a exclusão de seus dados a qualquer momento através da tela de Perfil ou entrando em contato com suporte@safepoint.com.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BotaoGradiente(texto = "Aceitar e Continuar") {
            onAceitar()
        }

        Spacer(modifier = Modifier.height(40.dp))
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

    Box(
        modifier = Modifier.fillMaxSize().background(FundoGradiente),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 60.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoPinSafepoint()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sempre te guiando a um ponto seguro",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Faça seu login",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = CorAzulEscuro
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("E-mail", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CorAzulEscuro,
                        unfocusedBorderColor = Color(0xFFD6DFE8)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

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
                    shape = RoundedCornerShape(14.dp),
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
                        Text("Lembrar-me", fontSize = 14.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { navController.navigate(Screen.RecuperarSenha.route) }) {
                        Text("Esqueci minha senha", color = CorAzulMedio, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                BotaoGradiente(texto = "Entrar") {
                    if (email.isNotBlank() && senha.isNotBlank()) {
                        auth.signInWithEmailAndPassword(email, senha)
                            .addOnSuccessListener {
                                PrefsHelper.setUserId(context, it.user?.uid ?: "")
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
                Spacer(modifier = Modifier.height(24.dp))

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
    val db = FirebaseFirestore.getInstance()

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
            LogoEscudoSafepoint()
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Crie sua conta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Preencha os dados abaixo para se cadastrar", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                placeholder = { Text("Nome completo", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("E-mail", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it },
                placeholder = { Text("Telefone / Celular", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))

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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = dataNascimento,
                onValueChange = { dataNascimento = it },
                placeholder = { Text("Data de nascimento", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = CorAzulEscuro) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CorAzulEscuro) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(20.dp))

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
            Spacer(modifier = Modifier.height(24.dp))

            BotaoGradiente(texto = "Cadastrar") {
                if (!aceitouTermos) {
                    Toast.makeText(context, "Aceite os termos de uso", Toast.LENGTH_SHORT).show()
                    return@BotaoGradiente
                }
                if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
                    Toast.makeText(context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                    return@BotaoGradiente
                }
                if (senha != confirmarSenha) {
                    Toast.makeText(context, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                    return@BotaoGradiente
                }

                auth.createUserWithEmailAndPassword(email, senha)
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid ?: ""
                        val perfil = hashMapOf(
                            "nome" to nome,
                            "email" to email,
                            "telefone" to telefone,
                            "dataNascimento" to dataNascimento
                        )
                        db.collection("usuarios").document(userId).set(perfil)
                            .addOnSuccessListener {
                                PrefsHelper.setUserId(context, userId)
                                Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Erro ao salvar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("Já tem uma conta? ", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Fazer login", color = CorAzulMedio, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
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
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(30.dp))
        }

        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp, bottom = 100.dp)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoPinSafepoint()
                Spacer(modifier = Modifier.height(32.dp))

                Text(text = "Recuperar senha", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Digite seu e-mail cadastrado para receber as instruções de redefinição",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("E-mail", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(28.dp))

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
    val context = LocalContext.current
    var nomeUsuario by remember { mutableStateOf("Usuário") }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val userId = PrefsHelper.getUserId(context)
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { doc ->
                    val nome = doc.getString("nome")
                    if (!nome.isNullOrBlank()) {
                        nomeUsuario = nome
                    }
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sua segurança é nossa prioridade",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
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
                    shape = RoundedCornerShape(18.dp),
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
                    shape = RoundedCornerShape(18.dp),
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
                    shape = RoundedCornerShape(18.dp),
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
                shape = RoundedCornerShape(18.dp)
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

// ================= TELA MAPA (CORRIGIDA - SEM ERRO 403) =================
@Composable
fun TelaMapa(navController: NavController) {
    val context = LocalContext.current
    var localizacaoAtual by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mapaCarregado by remember { mutableStateOf(false) }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted: Boolean = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted: Boolean = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
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
                AndroidView(
                    factory = { ctx ->
                        // CRÍTICO: Reconfigurar User-Agent antes de criar o MapView
                        Configuration.getInstance().userAgentValue = "${ctx.packageName}/1.0"
                        
                        val mapView = MapView(ctx)
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                        mapView.setMultiTouchControls(true)
                        mapView.setBuiltInZoomControls(true)
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

                        // Pontos de evacuação próximos à localização real
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

                        mapaCarregado = true
                        mapView
                    },
                    modifier = Modifier.fillMaxSize()
                )

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

// ================= FUNÇÃO LOCALIZAÇÃO =================
fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fineGranted: Boolean = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted: Boolean = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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

// ================= FUNÇÃO DISTÂNCIA =================
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

// ================= TELA PERFIL COM UPLOAD REAL =================
@Composable
fun TelaPerfil(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var fotoUriLocal by remember { mutableStateOf<android.net.Uri?>(null) }
    var fotoUrlSalva by remember { mutableStateOf<String?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { fotoUriLocal = it }
    }

    LaunchedEffect(Unit) {
        val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { doc ->
                    nome = doc.getString("nome") ?: ""
                    email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                    telefone = doc.getString("telefone") ?: ""
                    dataNascimento = doc.getString("dataNascimento") ?: ""
                    fotoUrlSalva = doc.getString("fotoUrl")
                    carregando = false
                }
                .addOnFailureListener { carregando = false }
        } else {
            carregando = false
        }
    }

    // DatePicker do Material 3 (Corrigido)
    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = selectedDateMillis
                        dataNascimento = String.format(
                            "%02d/%02d/%04d",
                            calendar.get(java.util.Calendar.DAY_OF_MONTH),
                            calendar.get(java.util.Calendar.MONTH) + 1,
                            calendar.get(java.util.Calendar.YEAR)
                        )
                    }
                    mostrarDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        if (carregando) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CorAzulEscuro)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
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
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val imagemParaMostrar = fotoUriLocal?.toString() ?: fotoUrlSalva
                            if (!imagemParaMostrar.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = imagemParaMostrar,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Foto",
                                    tint = CorAzulEscuro,
                                    modifier = Modifier.size(70.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CorAzulMedio),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Alterar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            if (nome.isEmpty()) "Usuário" else nome,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                        Text(
                            "Toque na foto para alterar",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Editar Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome completo") },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, tint = CorAzulEscuro) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = CorAzulEscuro) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = telefone,
                        onValueChange = { telefone = it },
                        label = { Text("Telefone") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = CorAzulEscuro) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = dataNascimento,
                        onValueChange = {},
                        label = { Text("Data de nascimento") },
                        leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = CorAzulEscuro) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarDatePicker = true }) {
                                Icon(Icons.Default.ArrowDropDown, null, tint = CorAzulEscuro)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)),
                        singleLine = true,
                        readOnly = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BotaoGradiente(texto = if (salvando) "Salvando..." else "Salvar Alterações") {
                        if (salvando) return@BotaoGradiente
                        salvando = true

                        val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid
                        if (userId != null) {
                            if (fotoUriLocal != null) {
                                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("fotos_perfil/$userId.jpg")
                                storageRef.putFile(fotoUriLocal!!)
                                    .addOnSuccessListener {
                                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                                            salvarPerfil(db, userId, nome, email, telefone, dataNascimento, uri.toString(), context) { sucesso ->
                                                if (sucesso) {
                                                    fotoUrlSalva = uri.toString()
                                                    fotoUriLocal = null
                                                }
                                                salvando = false
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Erro ao enviar foto: ${e.message}", Toast.LENGTH_LONG).show()
                                        salvando = false
                                    }
                            } else {
                                salvarPerfil(db, userId, nome, email, telefone, dataNascimento, fotoUrlSalva, context) {
                                    salvando = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                            salvando = false
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            PrefsHelper.clearUserId(context)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Sair", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= FUNÇÃO AUXILIAR SALVAR PERFIL =================
fun salvarPerfil(
    db: FirebaseFirestore,
    userId: String,
    nome: String,
    email: String,
    telefone: String,
    dataNascimento: String,
    fotoUrl: String?,
    context: Context,
    onFim: (Boolean) -> Unit
) {
    val perfil = hashMapOf(
        "nome" to nome,
        "email" to email,
        "telefone" to telefone,
        "dataNascimento" to dataNascimento,
        "fotoUrl" to (fotoUrl ?: "")
    )
    db.collection("usuarios").document(userId).set(perfil)
        .addOnSuccessListener {
            Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            onFim(true)
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            onFim(false)
        }
}
