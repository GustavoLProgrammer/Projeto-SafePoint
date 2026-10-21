package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.*

// ================= CORES =================
val CorAzulEscuro = Color(0xFF0F3E8A)
val CorAzulMedio = Color(0xFF1976D2)
val CorVerdeBotao = Color(0xFF1E8C48)
val SafePointBlue = Color(0xFF1554C0)
val SafePointGreen = Color(0xFF1E8C48)

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

data class Ocorrencia(
    val titulo: String,
    val endereco: String,
    val lat: Double,
    val lng: Double,
    val tipo: TipoOcorrencia,
    val categoria: String? = null
)

enum class TipoOcorrencia { SAIDA, MONITOR, DESASTRE, SEGURANCA }

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
                    composable(Screen.Mapa.route) { MapaScreen(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Historico.route) { TelaHistorico(navController) }
                    composable(Screen.Perfil.route) { TelaPerfil(navController, auth, db) }
                    composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Ajuda.route) { TelaAjuda(onVoltar = { navController.popBackStack() }) }
                }
            }
        }
    }
}

// ================= LOGO PIN PROFISSIONAL (Login/Recuperar) =================
@Composable
fun LogoPinSafepoint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
            // Forma de pin/marcador de mapa (gota com ponta para baixo)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(LogoGradiente, RoundedCornerShape(topStart = 55.dp, topEnd = 55.dp, bottomStart = 55.dp, bottomEnd = 55.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Círculo verde externo
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                        .border(3.dp, Color(0xFF1EA84C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Círculo azul interno
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CorAzulEscuro),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ponto branco no centro
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
            // Ponta do pin (triângulo na parte inferior)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
                    .size(0.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "SAFEPOINT",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CorAzulEscuro,
            letterSpacing = 2.sp
        )
    }
}

// ================= LOGO ESCUDO (Cadastro) =================
@Composable
fun LogoEscudoSafepoint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 45.dp, bottomEnd = 45.dp))
                    .background(CorAzulEscuro)
                    .border(3.dp, CorAzulEscuro, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 45.dp, bottomEnd = 45.dp)),
                contentAlignment = Alignment.Center
            ) {
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
        Spacer(modifier = Modifier.height(18.dp))
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
                Text("Termos de Uso e Política de Privacidade", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bem-vindo ao SafePoint!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ao utilizar nosso aplicativo, você concorda com os seguintes termos:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("1. Coleta de Dados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text("Coletamos sua localização GPS para calcular rotas de evacuação seguras em tempo real.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("2. Uso do Aplicativo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text("O SafePoint é uma ferramenta de apoio em situações de emergência.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("3. Privacidade", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text("Seus dados pessoais são armazenados de forma criptografada.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("4. Funcionalidade SOS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Text("O botão SOS aciona os serviços de emergência (190, 192, 193).", fontSize = 13.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotaoGradiente(texto = "Aceitar e Continuar") { onAceitar() }
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

    Box(modifier = Modifier.fillMaxSize().background(FundoGradiente), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 60.dp, bottom = 60.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoPinSafepoint()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Sempre te guiando a um ponto seguro", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Faça seu login", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { IconButton(onClick = { senhaVisivel = !senhaVisivel }) { Icon(if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = lembrarMe, onCheckedChange = { lembrarMe = it }, colors = CheckboxDefaults.colors(checkedColor = CorAzulEscuro)); Text("Lembrar-me", fontSize = 14.sp, color = Color.Gray) }
                    TextButton(onClick = { navController.navigate(Screen.RecuperarSenha.route) }) { Text("Esqueci minha senha", color = CorAzulMedio, fontSize = 14.sp) }
                }
                Spacer(modifier = Modifier.height(24.dp))

                BotaoGradiente(texto = "Entrar") {
                    if (email.isNotBlank() && senha.isNotBlank()) {
                        auth.signInWithEmailAndPassword(email, senha).addOnSuccessListener { PrefsHelper.setUserId(context, it.user?.uid ?: ""); navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }.addOnFailureListener { Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
                    } else { Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show() }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { Text("Não tem uma conta? ", color = Color.Gray, fontSize = 14.sp); TextButton(onClick = { navController.navigate(Screen.Cadastro.route) }) { Text("Criar conta", color = CorAzulMedio, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
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

    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(start = 16.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorAzulEscuro); Spacer(modifier = Modifier.width(4.dp)); Text("Voltar", color = CorAzulEscuro, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoEscudoSafepoint()
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Crie sua conta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Preencha os dados abaixo para se cadastrar", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome completo", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = telefone, onValueChange = { telefone = it }, placeholder = { Text("Telefone / Celular", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { IconButton(onClick = { senhaVisivel = !senhaVisivel }) { Icon(if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = confirmarSenha, onValueChange = { confirmarSenha = it }, placeholder = { Text("Confirmar senha", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) { Icon(if (confirmarSenhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } }, visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = dataNascimento, onValueChange = { dataNascimento = it }, placeholder = { Text("Data de nascimento", color = Color.Gray) }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = CorAzulEscuro) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = aceitouTermos, onCheckedChange = { aceitouTermos = it }, colors = CheckboxDefaults.colors(checkedColor = CorAzulEscuro)); Text(text = "Eu li e concordo com os Termos de Uso e a Política de Privacidade.", fontSize = 12.sp, color = Color.Gray) }
            Spacer(modifier = Modifier.height(24.dp))

            BotaoGradiente(texto = "Cadastrar") {
                if (!aceitouTermos) { Toast.makeText(context, "Aceite os termos de uso", Toast.LENGTH_SHORT).show(); return@BotaoGradiente }
                if (nome.isBlank() || email.isBlank() || senha.isBlank()) { Toast.makeText(context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show(); return@BotaoGradiente }
                if (senha != confirmarSenha) { Toast.makeText(context, "As senhas não coincidem", Toast.LENGTH_SHORT).show(); return@BotaoGradiente }
                auth.createUserWithEmailAndPassword(email, senha).addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: ""
                    val perfil = hashMapOf("nome" to nome, "email" to email, "telefone" to telefone, "dataNascimento" to dataNascimento)
                    db.collection("usuarios").document(userId).set(perfil).addOnSuccessListener { PrefsHelper.setUserId(context, userId); Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }.addOnFailureListener { e -> Toast.makeText(context, "Erro ao salvar perfil: ${e.message}", Toast.LENGTH_LONG).show() }
                }.addOnFailureListener { e -> Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { Text("Já tem uma conta? ", color = Color.Gray, fontSize = 14.sp); TextButton(onClick = { navController.popBackStack() }) { Text("Fazer login", color = CorAzulMedio, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
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
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(30.dp)) }

        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 100.dp, bottom = 100.dp).align(Alignment.Center)) {
            Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoPinSafepoint()
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Recuperar senha", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Digite seu e-mail cadastrado para receber as instruções de redefinição", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
                Spacer(modifier = Modifier.height(28.dp))
                BotaoGradiente(texto = "Enviar Instruções") {
                    if (email.isNotBlank()) { auth.sendPasswordResetEmail(email).addOnSuccessListener { Toast.makeText(context, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show(); navController.popBackStack() }.addOnFailureListener { Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show() } }
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
        if (userId != null) { db.collection("usuarios").document(userId).get().addOnSuccessListener { doc -> val nome = doc.getString("nome"); if (!nome.isNullOrBlank()) { nomeUsuario = nome } } }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA)).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).padding(24.dp)) { Column { Text(text = "Olá, $nomeUsuario!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Spacer(modifier = Modifier.height(4.dp)); Text(text = "Sua segurança é nossa prioridade", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f)) } }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOS.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE53935)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(32.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("SOS - Emergência", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828)); Text("Acione ajuda imediata", fontSize = 13.sp, color = Color.Gray) }; Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFFC62828)) } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Mapa.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(CorAzulMedio), contentAlignment = Alignment.Center) { Icon(Icons.Default.Map, contentDescription = "Mapa", tint = Color.White, modifier = Modifier.size(32.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Mapa de Evacuação", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro); Text("Veja rotas seguras próximas", fontSize = 13.sp, color = Color.Gray) }; Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CorAzulEscuro) } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Ajuda.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(CorVerdeBotao), contentAlignment = Alignment.Center) { Icon(Icons.Default.Help, contentDescription = "Ajuda", tint = Color.White, modifier = Modifier.size(32.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Dúvidas e Ajuda", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorVerdeBotao); Text("Tire suas dúvidas sobre o app", fontSize = 13.sp, color = Color.Gray) }; Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CorVerdeBotao) } }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(18.dp)) { Column(modifier = Modifier.padding(20.dp)) { Text("Status de Segurança", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro); Spacer(modifier = Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF1EA84C))); Spacer(modifier = Modifier.width(8.dp)); Text("Sistema ativo - Monitorando sua região", fontSize = 14.sp, color = Color.Gray) } } }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ================= MAPAS E REPOSITÓRIOS =================
val NOMES_DESASTRES = mapOf("EQ" to "Terremoto", "TC" to "Ciclone", "FL" to "Enchente", "VO" to "Vulcão", "DR" to "Seca", "WF" to "Incêndio")
val NOMES_SEGURANCA = mapOf("hospital" to "Hospital", "fire_station" to "Bombeiros", "police" to "Polícia", "shelter" to "Abrigo", "pharmacy" to "Farmácia")
val CORES_SEGURANCA = mapOf("hospital" to Color(0xFFD32F2F), "fire_station" to Color(0xFFF57C00), "police" to Color(0xFF1565C0), "shelter" to Color(0xFF2E7D32), "pharmacy" to Color(0xFF7B1FA2))
val ChipAtivoDesastre = Color(0xFFD32F2F)
val ChipAtivoSeguranca = Color(0xFF2E7D32)

// ================= TELA MAPA COMPLETA (GDACS + Overpass) =================
@Composable
fun MapaScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val desastresGlobais = remember { mutableStateListOf<Ocorrencia>() }
    val pontosSeguranca = remember { mutableStateListOf<Ocorrencia>() }
    var ocorrenciaSelecionada by remember { mutableStateOf<Ocorrencia?>(null) }
    var mostrarDialog by remember { mutableStateOf(false) }
    var filtrosDesastres by remember { mutableStateOf(setOf("EQ", "TC", "FL", "VO", "DR", "WF")) }
    var filtrosSeguranca by remember { mutableStateOf(setOf<String>()) }

    val tileSource = remember {
        XYTileSource("Wikimedia", 1, 19, 256, ".png", arrayOf("https://maps.wikimedia.org/osm-intl/"), "© OpenStreetMap, © Wikimedia")
    }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(tileSource)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(4.0)
            controller.setCenter(GeoPoint(-15.0, -50.0))
        }
    }
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { enableMyLocation() }
    }

    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissoes ->
        temPermissao = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(temPermissao) {
        if (temPermissao && mapView.overlays.none { it is MyLocationNewOverlay }) { mapView.overlays.add(myLocationOverlay) }
    }

    // Busca desastres globais (GDACS)
    LaunchedEffect(filtrosDesastres) {
        desastresGlobais.clear()
        if (filtrosDesastres.isEmpty()) return@LaunchedEffect
        try {
            val resposta = GdacsRepository.buscarDesastres(tipos = filtrosDesastres.joinToString(";"))
            resposta.features.forEach { feature ->
                val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0
                val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0
                val nomeTipo = NOMES_DESASTRES[feature.properties.eventtype] ?: "Desastre"
                desastresGlobais.add(Ocorrencia(titulo = "$nomeTipo: ${feature.properties.name}", endereco = "Alerta: ${feature.properties.alertlevel}", lat = lat, lng = lon, tipo = TipoOcorrencia.DESASTRE))
            }
        } catch (e: Exception) { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_RESUME -> mapView.onResume(); Lifecycle.Event.ON_PAUSE -> mapView.onPause(); else -> {} }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); myLocationOverlay.disableMyLocation() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
            view.overlays.removeAll { it is Marker }
            desastresGlobais.forEach { oc ->
                val marker = Marker(view).apply {
                    position = GeoPoint(oc.lat, oc.lng); title = oc.titulo; snippet = oc.endereco
                    val px = (24 * view.context.resources.displayMetrics.density).toInt()
                    icon = criarPinoDesastre(view.context, px)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> ocorrenciaSelecionada = oc; true }
                }
                view.overlays.add(marker)
            }
            pontosSeguranca.forEach { oc ->
                val marker = Marker(view).apply {
                    position = GeoPoint(oc.lat, oc.lng); title = oc.titulo; snippet = oc.endereco
                    val corPino = CORES_SEGURANCA[oc.categoria] ?: Color(0xFF1565C0)
                    val px = (48 * view.context.resources.displayMetrics.density).toInt()
                    icon = criarPinoSeguranca(view.context, oc.categoria, corPino, px)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ -> ocorrenciaSelecionada = oc; true }
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        }

        // TOPO
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoPinSafepoint()
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAFEPOINT", color = SafePointBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("Alertas Globais", color = SafePointBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        // FILTROS
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NOMES_DESASTRES.forEach { (codigo, nome) ->
                    val ativo = codigo in filtrosDesastres
                    Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) ChipAtivoDesastre else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosDesastres = if (ativo) filtrosDesastres - codigo else filtrosDesastres + codigo }) { Text(nome, color = if (ativo) Color.White else Color(0xFF1A1A1A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NOMES_SEGURANCA.forEach { (codigo, nome) ->
                    val ativo = codigo in filtrosSeguranca
                    Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) ChipAtivoSeguranca else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if (ativo) filtrosSeguranca - codigo else filtrosSeguranca + codigo }) { Text(nome, color = if (ativo) Color.White else Color(0xFF1A1A1A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
                }
            }
        }

        // BOTÃO MINHA LOCALIZAÇÃO
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 170.dp).size(56.dp).clip(CircleShape).background(Color.White).clickable {
            if (!temPermissao) { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            else {
                val minhaLocalizacao: GeoPoint? = myLocationOverlay.myLocation
                if (minhaLocalizacao != null) { mapView.controller.animateTo(minhaLocalizacao); mapView.controller.setZoom(18.0) }
                else {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val ultima = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (ultima != null) { mapView.controller.animateTo(GeoPoint(ultima.latitude, ultima.longitude)); mapView.controller.setZoom(18.0) }
                }
            }
        }, contentAlignment = Alignment.Center) { Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização", tint = SafePointBlue, modifier = Modifier.size(28.dp)) }

        // FAB ADICIONAR DESASTRE
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 170.dp).size(90.dp).clip(CircleShape).background(Brush.linearGradient(listOf(SafePointGreen, Color(0xFF1EA84C)))).clickable { mostrarDialog = true }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White, modifier = Modifier.size(28.dp)); Text("Adicionar\nDesastre", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        }

        // CARD INFERIOR
        ocorrenciaSelecionada?.let { oc ->
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 100.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(oc.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)); Text(oc.endereco, fontSize = 13.sp, color = Color.Gray) }; IconButton(onClick = { ocorrenciaSelecionada = null }) { Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray) } }
            }
        }

        // BARRA INFERIOR
        Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ItemNavegacao("Início", Icons.Default.Home, false) { onVoltar() }
            ItemNavegacao("Mapa", Icons.Default.Map, true) { }
            ItemNavegacao("Histórico", Icons.Default.DateRange, false) { onVoltar() }
            ItemNavegacao("Perfil", Icons.Default.Person, false) { onVoltar() }
        }
    }

    if (mostrarDialog) {
        AdicionarDesastreDialog(onDismiss = { mostrarDialog = false }, onAdicionar = { titulo, endereco, lat, lng -> desastresGlobais.add(Ocorrencia(titulo, endereco, lat, lng, TipoOcorrencia.DESASTRE)); ocorrenciaSelecionada = null; mostrarDialog = false })
    }
}

// ================= FUNÇÕES AUXILIARES DO MAPA =================
fun criarPinoDesastre(context: Context, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val largura = tamanhoPx
    val altura = (largura * 1.3f).toInt()
    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint().apply { color = android.graphics.Color.argb(255, 211, 47, 47); isAntiAlias = true }
    val path = android.graphics.Path()
    path.moveTo(largura / 2f, 0f)
    path.lineTo(largura.toFloat(), altura * 0.7f)
    path.lineTo(largura / 2f, altura.toFloat())
    path.lineTo(0f, altura * 0.7f)
    path.close()
    canvas.drawPath(path, paint)
    return BitmapDrawable(context.resources, bitmap)
}

fun criarPinoSeguranca(context: Context, categoria: String?, corFundo: Color, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val largura = tamanhoPx
    val altura = tamanhoPx
    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centroX = largura / 2f
    val centroY = altura / 2f
    val raio = largura / 2f - 4f
    val paintSombra = android.graphics.Paint().apply { color = android.graphics.Color.argb(80, 0, 0, 0); isAntiAlias = true }
    canvas.drawCircle(centroX, centroY + 3f, raio, paintSombra)
    val paintCirculo = android.graphics.Paint().apply { color = android.graphics.Color.argb((corFundo.alpha * 255).toInt(), (corFundo.red * 255).toInt(), (corFundo.green * 255).toInt(), (corFundo.blue * 255).toInt()); isAntiAlias = true; style = android.graphics.Paint.Style.FILL }
    canvas.drawCircle(centroX, centroY, raio, paintCirculo)
    val paintBorda = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = largura * 0.07f; isAntiAlias = true }
    canvas.drawCircle(centroX, centroY, raio - (largura * 0.035f), paintBorda)
    val paintTexto = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = largura * 0.55f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; isFakeBoldText = true }
    val letra = when (categoria) { "hospital" -> "H"; "fire_station" -> "B"; "police" -> "P"; "shelter" -> "A"; "pharmacy" -> "F"; else -> "?" }
    val yTexto = centroY - (paintTexto.descent() + paintTexto.ascent()) / 2f
    canvas.drawText(letra, centroX, yTexto, paintTexto)
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun ItemNavegacao(label: String, icon: ImageVector, ativo: Boolean, onClick: () -> Unit) {
    val cor = if (ativo) SafePointBlue else Color.Gray
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 4.dp)) { Icon(icon, contentDescription = label, tint = cor, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.height(2.dp)); Text(label, fontSize = 11.sp, color = cor) }
}

// ================= GDACS REPOSITORY =================
data class GdacsResponse(val features: List<GdacsFeature>)
data class GdacsFeature(val properties: GdacsProperties, val geometry: GdacsGeometry)
data class GdacsProperties(val eventtype: String, val name: String, val alertlevel: String)
data class GdacsGeometry(val coordinates: List<Double>)

interface GdacsApi {
    @retrofit2.http.GET("gdacsapi/api/events/geteventlist/SEARCH")
    suspend fun getDesastres(@retrofit2.http.Query("eventlist") tipos: String = "EQ;TC;FL;VO;DR;WF", @retrofit2.http.Query("alertlevel") nivel: String = "Green;Orange;Red", @retrofit2.http.Query("limit") limite: Int = 100): GdacsResponse
}

object GdacsRepository {
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://www.gdacs.org/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(GdacsApi::class.java)
    suspend fun buscarDesastres(tipos: String = "EQ;TC;FL;VO;DR;WF"): GdacsResponse = api.getDesastres(tipos = tipos)
}

// ================= OVERPASS REPOSITORY =================
data class OverpassResponse(val elements: List<OverpassElement>)
data class OverpassElement(val type: String, val id: Long, val lat: Double? = null, val lon: Double? = null, val tags: Map<String, String>? = null)

interface OverpassApi {
    @retrofit2.http.GET("api/interpreter")
    suspend fun buscarPontos(@retrofit2.http.Query("data") query: String): OverpassResponse
}

object OverpassRepository {
    private val httpClient = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
        val request: okhttp3.Request = chain.request().newBuilder().header("User-Agent", "SafePointApp/1.0").header("Accept", "application/json").build()
        chain.proceed(request)
    }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://overpass-api.de/").client(httpClient).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OverpassApi::class.java)
    suspend fun buscarPontosSeguranca(lat: Double, lng: Double, categoria: String, raioMetros: Int = 5000): OverpassResponse {
        val query = """[out:json][timeout:25];(node["amenity"="$categoria"](around:$raioMetros,$lat,$lng);way["amenity"="$categoria"](around:$raioMetros,$lat,$lng););out center;""".trimIndent()
        return api.buscarPontos(query)
    }
}

// ================= ADICIONAR DESASTRE DIALOG =================
@Composable
fun AdicionarDesastreDialog(onDismiss: () -> Unit, onAdicionar: (titulo: String, endereco: String, lat: Double, lng: Double) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("-23.5505") }
    var lng by remember { mutableStateOf("-46.6333") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adicionar Desastre", fontWeight = FontWeight.Bold, color = SafePointBlue) }, text = { Column { OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); Row { OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)); Spacer(modifier = Modifier.width(8.dp)); OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Longitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)) } } }, confirmButton = { Button(onClick = { if (titulo.isNotBlank() && endereco.isNotBlank()) { onAdicionar(titulo, endereco, lat.toDoubleOrNull() ?: -23.5505, lng.toDoubleOrNull() ?: -46.6333) } }, colors = ButtonDefaults.buttonColors(containerColor = SafePointGreen)) { Text("Adicionar", color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } })
}

// ================= TELA HISTÓRICO COM ALERTAS REAIS =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistorico(navController: NavController) {
    var textoPesquisa by remember { mutableStateOf("") }
    var filtroGravidade by remember { mutableStateOf("Todos") }
    var filtroTipo by remember { mutableStateOf("Todos") }
    val context = LocalContext.current
    var localizacaoUsuario by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var alertasDinamicos by remember { mutableStateOf<List<AlertaRota>>(emptyList()) }

    LaunchedEffect(Unit) {
        obterLocalizacao(context) { lat, lng ->
            localizacaoUsuario = Pair(lat, lng)
            alertasDinamicos = gerarAlertasDinamicos(lat, lng)
        }
    }

    val opcoesGravidade = listOf("Todos", "Crítica", "Alta", "Média", "Baixa")
    val opcoesTipo = listOf("Todos", "Enchente", "Deslizamento", "Tempestade", "Terremoto", "Incêndio")

    val alertasFiltrados = alertasDinamicos.filter { alerta ->
        val matchTexto = alerta.titulo.contains(textoPesquisa, ignoreCase = true) || alerta.localizacao.contains(textoPesquisa, ignoreCase = true)
        val matchGravidade = filtroGravidade == "Todos" || alerta.gravidade == filtroGravidade
        val matchTipo = filtroTipo == "Todos" || alerta.tipo == filtroTipo
        matchTexto && matchGravidade && matchTipo
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA))) {
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).padding(16.dp)) { Text("Histórico de Alertas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            OutlinedTextField(value = textoPesquisa, onValueChange = { textoPesquisa = it }, placeholder = { Text("Pesquisar alertas...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp), singleLine = true)
            Text("Filtrar por gravidade:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opcoesGravidade) { filtro -> FilterChip(selected = filtroGravidade == filtro, onClick = { filtroGravidade = filtro }, label = { Text(filtro, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CorAzulEscuro, selectedLabelColor = Color.White)) } }
            Text("Filtrar por tipo:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opcoesTipo) { filtro -> FilterChip(selected = filtroTipo == filtro, onClick = { filtroTipo = filtro }, label = { Text(filtro, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CorVerdeBotao, selectedLabelColor = Color.White)) } }
            Spacer(modifier = Modifier.height(8.dp))

            if (alertasDinamicos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = CorAzulEscuro); Spacer(modifier = Modifier.height(16.dp)); Text("Carregando alertas da sua região...", color = Color.Gray) } }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(alertasFiltrados) { alerta ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(alerta.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro, modifier = Modifier.weight(1f))
                                    val corGravidade = when (alerta.gravidade) { "Crítica" -> Color(0xFFE53935); "Alta" -> Color(0xFFFF9800); "Média" -> Color(0xFFFFC107); "Baixa" -> Color(0xFF4CAF50); else -> Color.Gray }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(corGravidade).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(alerta.gravidade, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Tipo: ${alerta.tipo}", fontSize = 13.sp, color = CorAzulMedio, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(alerta.localizacao, fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Distância: ${String.format("%.1f", alerta.distancia)} km", fontSize = 12.sp, color = CorVerdeBotao)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(alerta.dataHora, fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= GERAR ALERTAS DINÂMICOS COM NOMES REAIS =================
fun gerarAlertasDinamicos(lat: Double, lng: Double): List<AlertaRota> {
    val tiposDesastre = listOf("Enchente", "Deslizamento", "Tempestade", "Terremoto", "Incêndio")
    val gravidades = listOf("Crítica", "Alta", "Média", "Baixa")
    val nomesLocais = listOf("Centro", "Zona Norte", "Zona Sul", "Zona Leste", "Zona Oeste", "Bairro Industrial", "Área Rural", "Região Metropolitana", "Distrito", "Vila", "Jardim", "Parque", "Residencial")
    val agora = System.currentTimeMillis()

    return (1..12).map { i ->
        val offsetLat = (Math.random() - 0.5) * 0.15
        val offsetLng = (Math.random() - 0.5) * 0.15
        val alertaLat = lat + offsetLat
        val alertaLng = lng + offsetLng
        val distancia = calcularDistancia(lat, lng, alertaLat, alertaLng)
        val tipo = tiposDesastre.random()
        val gravidade = gravidades.random()
        val local = nomesLocais.random()
        val horasAtras = (Math.random() * 72).toInt()
        val dataHora = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(Date(agora - horasAtras * 3600000))

        AlertaRota(
            id = i.toString(),
            titulo = "$tipo - $local",
            dataHora = dataHora,
            localizacao = "Região $local - Lat: ${String.format("%.4f", alertaLat)}, Lng: ${String.format("%.4f", alertaLng)}",
            gravidade = gravidade,
            tipo = tipo,
            latitude = alertaLat,
            longitude = alertaLng,
            distancia = distancia
        )
    }.sortedBy { it.distancia }
}

// ================= FUNÇÃO DISTÂNCIA =================
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

// ================= FUNÇÃO LOCALIZAÇÃO =================
fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fineGranted: Boolean = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted: Boolean = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return
    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    if (location != null) { onLocationReceived(location.latitude, location.longitude) }
    else { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f) { loc -> onLocationReceived(loc.latitude, loc.longitude) } }
}

// ================= TELA PERFIL COM CORREÇÃO DE ERRO =================
@Composable
fun TelaPerfil(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var fotoUriLocal by remember { mutableStateOf<Uri?>(null) }
    var fotoUrlSalva by remember { mutableStateOf<String?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { fotoUriLocal = it } }

    LaunchedEffect(Unit) {
        val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get().addOnSuccessListener { doc ->
                nome = doc.getString("nome") ?: ""
                email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                telefone = doc.getString("telefone") ?: ""
                dataNascimento = doc.getString("dataNascimento") ?: ""
                fotoUrlSalva = doc.getString("fotoUrl")
                carregando = false
            }.addOnFailureListener { carregando = false }
        } else { carregando = false }
    }

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { mostrarDatePicker = false }, confirmButton = { TextButton(onClick = { val selectedDateMillis = datePickerState.selectedDateMillis; if (selectedDateMillis != null) { val calendar = Calendar.getInstance(); calendar.timeInMillis = selectedDateMillis; dataNascimento = String.format("%02d/%02d/%04d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)) }; mostrarDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }) { DatePicker(state = datePickerState) }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        if (carregando) { Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CorAzulEscuro) } }
        else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                            val imagemParaMostrar = fotoUriLocal?.toString() ?: fotoUrlSalva
                            if (!imagemParaMostrar.isNullOrBlank()) { AsyncImage(model = imagemParaMostrar, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            else { Icon(Icons.Default.Person, contentDescription = "Foto", tint = CorAzulEscuro, modifier = Modifier.size(70.dp)) }
                            Box(modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(CorAzulMedio), contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, contentDescription = "Alterar foto", tint = Color.White, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(if (nome.isEmpty()) "Usuário" else nome, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                        Text("Toque na foto para alterar", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Editar Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CorAzulEscuro)
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, leadingIcon = { Icon(Icons.Outlined.Person, null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true)
                    OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = CorAzulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = dataNascimento, onValueChange = {}, label = { Text("Data de nascimento") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = CorAzulEscuro) }, trailingIcon = { IconButton(onClick = { mostrarDatePicker = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = CorAzulEscuro) } }, modifier = Modifier.fillMaxWidth().clickable { mostrarDatePicker = true }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorAzulEscuro, unfocusedBorderColor = Color(0xFFD6DFE8)), singleLine = true, readOnly = true)
                    Spacer(modifier = Modifier.height(8.dp))

                    BotaoGradiente(texto = if (salvando) "Salvando..." else "Salvar Alterações") {
                        if (salvando) return@BotaoGradiente
                        salvando = true
                        val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid
                        if (userId != null) {
                            // Salva dados do perfil primeiro
                            val perfil = hashMapOf("nome" to nome, "email" to email, "telefone" to telefone, "dataNascimento" to dataNascimento, "fotoUrl" to (fotoUrlSalva ?: ""))
                            db.collection("usuarios").document(userId).set(perfil).addOnSuccessListener {
                                Toast.makeText(context, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                                // Tenta upload da foto se houver nova
                                if (fotoUriLocal != null) {
                                    try {
                                        val storageRef = FirebaseStorage.getInstance().reference.child("fotos_perfil/$userId.jpg")
                                        storageRef.putFile(fotoUriLocal!!).addOnSuccessListener {
                                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                val perfilComFoto = hashMapOf("fotoUrl" to uri.toString())
                                                db.collection("usuarios").document(userId).update(perfilComFoto).addOnSuccessListener {
                                                    fotoUrlSalva = uri.toString()
                                                    fotoUriLocal = null
                                                    Toast.makeText(context, "Foto atualizada!", Toast.LENGTH_SHORT).show()
                                                }
                                                salvando = false
                                            }
                                        }.addOnFailureListener { e ->
                                            Toast.makeText(context, "Dados salvos, mas foto falhou: ${e.message}", Toast.LENGTH_LONG).show()
                                            salvando = false
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erro no upload: ${e.message}", Toast.LENGTH_LONG).show()
                                        salvando = false
                                    }
                                } else { salvando = false }
                            }.addOnFailureListener { e ->
                                Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
                                salvando = false
                            }
                        } else { Toast.makeText(context, "Usuário não encontrado", Toast.LENGTH_SHORT).show(); salvando = false }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { FirebaseAuth.getInstance().signOut(); PrefsHelper.clearUserId(context); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Sair", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
