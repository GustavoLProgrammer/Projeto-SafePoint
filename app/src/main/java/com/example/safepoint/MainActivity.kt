package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

// CORES ORIGINAIS
val azulEscuro = Color(0xFF0F3E8A)
val azulLinhas = Color(0xFF1976D2)
val verdeGradiente = Color(0xFF1E9B44)
val verdeEscudo = Color(0xFF1EA84C)
val cinzaTexto = Color(0xFF7A8B9E)
val cinzaBorda = Color(0xFFD6DFE8)
val textoDigitado = Color(0xFF1A1A1A)
val SafePointBlue = Color(0xFF1554C0)
val SafePointGreen = verdeGradiente
val FundoGradiente = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientFundoOriginal = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientBotaoOriginal = Brush.horizontalGradient(colors = listOf(azulEscuro, verdeGradiente))
val BotaoGradiente = gradientBotaoOriginal

data class AlertaRota(
    val id: String,
    val titulo: String,
    val dataHora: String,
    val localizacao: String,
    val gravidade: String,
    val tipo: String,
    val latitude: Double,
    val longitude: Double,
    val distancia: Double = 0.0,
    val bairro: String = "",
    val cidade: String = "",
    val regiao: String = "",
    val descricao: String = "",
    val fonte: String = "Defesa Civil"
)

sealed class Screen(val route: String) {
    object Termos : Screen("termos")
    object PermissaoLocalizacao : Screen("permissao_localizacao")
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

object PrefsHelper {
    fun getPrefs(context: Context): SharedPreferences = context.getSharedPreferences("safepoint_prefs", Context.MODE_PRIVATE)
    fun getUserId(context: Context): String? = getPrefs(context).getString("user_id", null)
    fun setUserId(context: Context, id: String) { getPrefs(context).edit().putString("user_id", id).apply() }
    fun clearUserId(context: Context) { getPrefs(context).edit().remove("user_id").apply() }
    fun isTermosAceitos(context: Context): Boolean = getPrefs(context).getBoolean("termos_aceitos", false)
    fun setTermosAceitos(context: Context, aceito: Boolean) { getPrefs(context).edit().putBoolean("termos_aceitos", aceito).apply() }
    fun isLocalizacaoAceita(context: Context): Boolean = getPrefs(context).getBoolean("loc_aceita", false)
    fun setLocalizacaoAceita(context: Context, aceito: Boolean) { getPrefs(context).edit().putBoolean("loc_aceita", aceito).apply() }
    fun jaPediuPermissaoGps(context: Context): Boolean = getPrefs(context).getBoolean("gps_permissao_pedida", false)
    fun setJaPediuPermissaoGps(context: Context, pedida: Boolean) { getPrefs(context).edit().putBoolean("gps_permissao_pedida", pedida).apply() }
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        try {
            val ctx = applicationContext
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = "SafePoint/2.0 (Android; ${ctx.packageName})"
            Configuration.getInstance().tileDownloadThreads = 2
            Configuration.getInstance().osmdroidBasePath = ctx.getExternalFilesDir(null)
            Configuration.getInstance().osmdroidTileCache = ctx.getExternalFilesDir("osmdroid_tiles")
        } catch (_: Exception) {}
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val termosAceitos = PrefsHelper.isTermosAceitos(context)
                val locAceita = PrefsHelper.isLocalizacaoAceita(context)
                val currentUser = auth.currentUser
                val startDest = when {
                    !termosAceitos -> Screen.Termos.route
                    !locAceita -> Screen.PermissaoLocalizacao.route
                    currentUser == null -> Screen.Login.route
                    else -> Screen.Home.route
                }
                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Termos.route) { TelaTermosUso(onAceitar = { PrefsHelper.setTermosAceitos(context, true); navController.navigate(Screen.PermissaoLocalizacao.route) { popUpTo(Screen.Termos.route) { inclusive = true } } }) }
                    composable(Screen.PermissaoLocalizacao.route) { TelaPermissaoLocalizacao(onAceitar = { PrefsHelper.setLocalizacaoAceita(context, true); if (auth.currentUser != null) navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } else navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }, onRecusar = { Toast.makeText(context, "Permita a localização nas configurações quando desejar.", Toast.LENGTH_LONG).show(); if (auth.currentUser != null) navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } else navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }) }
                    composable(Screen.Login.route) { TelaLoginOriginal(navController, auth) }
                    composable(Screen.Cadastro.route) { TelaCadastroEscudoOriginal(navController, auth) }
                    composable(Screen.RecuperarSenha.route) { TelaRecuperarSenhaOriginal(navController, auth) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { MapaScreenProfissional(navController = navController, onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Historico.route) { TelaHistoricoReal(navController) }
                    composable(Screen.Perfil.route) { TelaPerfil(navController, auth, db) }
                    composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Ajuda.route) { TelaAjudaComoUsar(onVoltar = { navController.popBackStack() }) }
                }
            }
        }
    }
}

// LOGO ORIGINAL PIN
@Composable
fun LogoSafePoint(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val azulEscuro = Color(0xFF0F3E8A); val verdeEscudo = Color(0xFF1EA84C)
        val escudoGradient = Brush.verticalGradient(colors = listOf(Color(0xFF0C3875), Color(0xFF138A4B)), startY = 0f, endY = h)
        val pathEscudoExterno = Path().apply {
            moveTo(w * 0.5f, h * 0.05f); cubicTo(w * 0.82f, h * 0.05f, w * 0.94f, h * 0.18f, w * 0.94f, h * 0.42f)
            cubicTo(w * 0.94f, h * 0.72f, w * 0.52f, h * 0.96f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.48f, h * 0.96f, w * 0.06f, h * 0.72f, w * 0.06f, h * 0.42f)
            cubicTo(w * 0.06f, h * 0.18f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.05f); close()
        }
        drawPath(path = pathEscudoExterno, brush = escudoGradient, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        val pathEscudoInterno = Path().apply {
            moveTo(w * 0.5f, h * 0.13f); cubicTo(w * 0.75f, h * 0.13f, w * 0.84f, h * 0.24f, w * 0.84f, h * 0.44f)
            cubicTo(w * 0.84f, h * 0.67f, w * 0.52f, h * 0.86f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.48f, h * 0.86f, w * 0.16f, h * 0.67f, w * 0.16f, h * 0.44f)
            cubicTo(w * 0.16f, h * 0.24f, w * 0.25f, h * 0.13f, w * 0.5f, h * 0.13f); close()
        }
        drawPath(path = pathEscudoInterno, color = Color(0x181EA84C), style = Fill)
        drawPath(path = pathEscudoInterno, color = verdeEscudo.copy(alpha = 0.5f), style = Stroke(width = 1.2.dp.toPx()))
        val pinCentroX = w * 0.5f; val pinTopoY = h * 0.28f; val pinRaio = w * 0.18f; val pinPontaY = h * 0.70f
        val pathPin = Path().apply {
            moveTo(pinCentroX, pinPontaY)
            cubicTo(pinCentroX - pinRaio * 0.95f, pinPontaY - h * 0.14f, pinCentroX - pinRaio, pinTopoY + pinRaio * 1.1f, pinCentroX - pinRaio, pinTopoY + pinRaio)
            arcTo(rect = androidx.compose.ui.geometry.Rect(left = pinCentroX - pinRaio, top = pinTopoY, right = pinCentroX + pinRaio, bottom = pinTopoY + pinRaio * 2f), startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false)
            cubicTo(pinCentroX + pinRaio, pinTopoY + pinRaio * 1.1f, pinCentroX + pinRaio * 0.95f, pinPontaY - h * 0.14f, pinCentroX, pinPontaY); close()
        }
        drawPath(path = pathPin, color = azulEscuro, style = Fill)
        drawCircle(color = Color.White, radius = pinRaio * 0.40f, center = Offset(pinCentroX, pinTopoY + pinRaio))
        drawCircle(color = verdeEscudo, radius = pinRaio * 0.22f, center = Offset(pinCentroX, pinTopoY + pinRaio))
    }
}

@Composable
fun LogoEscudoSafePoint(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val azul = Color(0xFF0F3E8A); val azulClaro = Color(0xFF1A5DC0)
        val escudo = Path().apply {
            moveTo(w * 0.5f, h * 0.02f)
            cubicTo(w * 0.78f, h * 0.02f, w * 0.92f, h * 0.08f, w * 0.92f, h * 0.22f)
            lineTo(w * 0.92f, h * 0.48f)
            cubicTo(w * 0.92f, h * 0.70f, w * 0.70f, h * 0.88f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.30f, h * 0.88f, w * 0.08f, h * 0.70f, w * 0.08f, h * 0.48f)
            lineTo(w * 0.08f, h * 0.22f)
            cubicTo(w * 0.08f, h * 0.08f, w * 0.22f, h * 0.02f, w * 0.5f, h * 0.02f)
            close()
        }
        drawPath(path = escudo, color = azul.copy(alpha = 0.25f), style = Stroke(width = 8.dp.toPx()))
        drawPath(path = escudo, brush = Brush.verticalGradient(colors = listOf(azulClaro, azul), startY = 0f, endY = h), style = Fill)
        drawPath(path = escudo, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable fun IconeUsuario(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val stroke = Stroke(width = 1.8.dp.toPx())
        drawCircle(color = cor, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
        val corpoPath = Path().apply { moveTo(w * 0.15f, h * 0.92f); cubicTo(w * 0.15f, h * 0.68f, w * 0.85f, h * 0.68f, w * 0.85f, h * 0.92f) }
        drawPath(path = corpoPath, color = cor, style = stroke)
    }
}

@Composable fun IconeCadeado(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val stroke = Stroke(width = 1.8.dp.toPx())
        drawRoundRect(color = cor, topLeft = Offset(w * 0.18f, h * 0.42f), size = Size(w * 0.64f, h * 0.52f), cornerRadius = CornerRadius(4.dp.toPx()), style = stroke)
        val arcoPath = Path().apply { moveTo(w * 0.30f, h * 0.42f); lineTo(w * 0.30f, h * 0.24f); cubicTo(w * 0.30f, h * 0.08f, w * 0.70f, h * 0.08f, w * 0.70f, h * 0.24f); lineTo(w * 0.70f, h * 0.42f) }
        drawPath(path = arcoPath, color = cor, style = stroke); drawCircle(color = cor, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.65f))
    }
}

@Composable fun IconeOlho(aberto: Boolean, cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val olhoPath = Path().apply { moveTo(w * 0.1f, h * 0.5f); cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.9f, h * 0.5f); cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.1f, h * 0.5f); close() }
        drawPath(path = olhoPath, color = cor, style = stroke)
        if (aberto) drawCircle(color = cor, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.5f))
        else { drawCircle(color = cor, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.5f)); drawLine(color = cor, start = Offset(w * 0.2f, h * 0.2f), end = Offset(w * 0.8f, h * 0.8f), strokeWidth = stroke.width) }
    }
}

@Composable fun BotaoGradiente(texto: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(BotaoGradiente, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text(texto, fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable fun BarraNavegacaoInferior(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected = currentRoute == Screen.Home.route, onClick = { if (currentRoute != Screen.Home.route) navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
        NavigationBarItem(selected = currentRoute == Screen.Mapa.route, onClick = { if (currentRoute != Screen.Mapa.route) navController.navigate(Screen.Mapa.route) { popUpTo(Screen.Home.route) } }, icon = { Icon(Icons.Default.Map, null) }, label = { Text("Mapa") })
        NavigationBarItem(selected = currentRoute == Screen.Historico.route, onClick = { if (currentRoute != Screen.Historico.route) navController.navigate(Screen.Historico.route) { popUpTo(Screen.Home.route) } }, icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Histórico") })
        NavigationBarItem(selected = currentRoute == Screen.Perfil.route, onClick = { if (currentRoute != Screen.Perfil.route) navController.navigate(Screen.Perfil.route) { popUpTo(Screen.Home.route) } }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") })
    }
}

// LOGIN
@Composable
fun TelaLoginOriginal(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var lembrarMe by remember { mutableStateOf(false) }; var senhaVisivel by remember { mutableStateOf(false) }; val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal).statusBarsPadding()) {
        Card(shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 48.dp, bottom = 28.dp).align(Alignment.Center)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 30.dp)) {
                LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(10.dp))
                Text(text = "SAFEPOINT", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = azulEscuro)
                Text(text = "Sempre te guiando a um ponto seguro", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = azulEscuro)
                Spacer(modifier = Modifier.height(30.dp)); Text(text = "Faça seu login", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(22.dp))
                OutlinedTextField(value = email, onValueChange = { email = it.trim() }, placeholder = { Text("E-mail", color = cinzaTexto, fontSize = 14.sp) }, leadingIcon = { IconeUsuario(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textoDigitado, unfocusedTextColor = textoDigitado, focusedBorderColor = azulEscuro, unfocusedBorderColor = cinzaBorda), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it.trim() }, placeholder = { Text("Senha", color = cinzaTexto, fontSize = 14.sp) }, leadingIcon = { IconeCadeado(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, trailingIcon = { IconButton(onClick = { senhaVisivel = !senhaVisivel }) { IconeOlho(aberto = senhaVisivel, cor = cinzaTexto, modifier = Modifier.size(22.dp)) } }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textoDigitado, unfocusedTextColor = textoDigitado, focusedBorderColor = azulEscuro, unfocusedBorderColor = cinzaBorda), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { lembrarMe = !lembrarMe }) {
                        Checkbox(checked = lembrarMe, onCheckedChange = { lembrarMe = it }, colors = CheckboxDefaults.colors(checkedColor = verdeGradiente, uncheckedColor = cinzaBorda), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Lembrar-me", color = azulEscuro, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(text = "Esqueci minha senha", color = azulLinhas, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.navigate(Screen.RecuperarSenha.route) })
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable {
                    if (email.isBlank() || senha.isBlank()) Toast.makeText(context, "Preencha e-mail e senha!", Toast.LENGTH_SHORT).show()
                    else auth.signInWithEmailAndPassword(email, senha).addOnCompleteListener { task ->
                        if (task.isSuccessful) { PrefsHelper.setUserId(context, task.result.user?.uid ?: ""); Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }
                        else Toast.makeText(context, "E-mail ou senha incorretos!", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Entrar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Não tem uma conta? ", color = cinzaTexto, fontSize = 13.sp); Text(text = "Criar conta", color = azulLinhas, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navController.navigate(Screen.Cadastro.route) })
                }
            }
        }
    }
}

// CADASTRO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastroEscudoOriginal(navController: NavController, auth: FirebaseAuth) {
    var nome by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var telefone by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var confirmarSenha by remember { mutableStateOf("") }; var dataNascimento by remember { mutableStateOf("") }; var senhaVisivel by remember { mutableStateOf(false) }; var confirmarVisivel by remember { mutableStateOf(false) }; var aceitouTermos by remember { mutableStateOf(false) }; var mostrarDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current; val db = FirebaseFirestore.getInstance()
    if (mostrarDatePicker) { val state = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { mostrarDatePicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { millis -> val cal = Calendar.getInstance(); cal.timeInMillis = millis; dataNascimento = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR)) }; mostrarDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }) { DatePicker(state = state) } }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(start = 8.dp)) { Icon(Icons.Default.ArrowBack, null, tint = azulEscuro); Spacer(modifier = Modifier.width(4.dp)); Text("Voltar", color = azulEscuro) }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(10.dp))
            Text(text = "SAFEPOINT", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = azulEscuro)
            Text(text = "Sempre te guiando a um ponto seguro", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = azulEscuro)
            Spacer(modifier = Modifier.height(24.dp)); Text("Crie sua conta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome completo", color = cinzaTexto) }, leadingIcon = { IconeUsuario(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = azulEscuro, unfocusedBorderColor = cinzaBorda))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail", color = cinzaTexto) }, leadingIcon = { Icon(Icons.Outlined.Email, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = telefone, onValueChange = { telefone = it }, placeholder = { Text("Telefone / Celular", color = cinzaTexto) }, leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha", color = cinzaTexto) }, leadingIcon = { IconeCadeado(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, trailingIcon = { IconButton(onClick = { senhaVisivel = !senhaVisivel }) { IconeOlho(aberto = senhaVisivel, cor = cinzaTexto, modifier = Modifier.size(22.dp)) } }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = confirmarSenha, onValueChange = { confirmarSenha = it }, placeholder = { Text("Confirmar senha", color = cinzaTexto) }, leadingIcon = { IconeCadeado(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, trailingIcon = { IconButton(onClick = { confirmarVisivel = !confirmarVisivel }) { IconeOlho(aberto = confirmarVisivel, cor = cinzaTexto, modifier = Modifier.size(22.dp)) } }, visualTransformation = if (confirmarVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = dataNascimento, onValueChange = {}, placeholder = { Text("Data de nascimento", color = cinzaTexto) }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = azulEscuro) }, trailingIcon = { IconButton(onClick = { mostrarDatePicker = true }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = azulEscuro) } }, modifier = Modifier.fillMaxWidth().clickable { mostrarDatePicker = true }, shape = RoundedCornerShape(12.dp), singleLine = true, readOnly = true)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().clickable { aceitouTermos = !aceitouTermos }.padding(4.dp)) { Checkbox(checked = aceitouTermos, onCheckedChange = { aceitouTermos = it }, colors = CheckboxDefaults.colors(checkedColor = verdeGradiente), modifier = Modifier.padding(top = 2.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Eu li e concordo com os Termos de Uso e a Política de Privacidade.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 14.sp) }
            Spacer(modifier = Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable {
                if (!aceitouTermos) { Toast.makeText(context, "Aceite os termos", Toast.LENGTH_SHORT).show(); return@clickable }
                if (senha != confirmarSenha) { Toast.makeText(context, "Senhas não conferem", Toast.LENGTH_SHORT).show(); return@clickable }
                if (nome.isBlank() || email.isBlank() || senha.isBlank()) { Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show(); return@clickable }
                auth.createUserWithEmailAndPassword(email, senha).addOnSuccessListener { result -> val userId = result.user?.uid ?: ""; val perfil = hashMapOf("nome" to nome, "email" to email, "telefone" to telefone, "dataNascimento" to dataNascimento); db.collection("usuarios").document(userId).set(perfil).addOnSuccessListener { PrefsHelper.setUserId(context, userId); Toast.makeText(context, "Conta criada!", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Home.route) { popUpTo(Screen.Cadastro.route) { inclusive = true } } }.addOnFailureListener { Toast.makeText(context, "Erro Firestore: ${it.message}", Toast.LENGTH_LONG).show() } }.addOnFailureListener { Toast.makeText(context, "Erro: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
            }) { Text("Cadastrar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { Text("Já tem uma conta? ", color = cinzaTexto, fontSize = 13.sp); Text("Fazer login", color = azulLinhas, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navController.popBackStack() }) }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// RECUPERAR SENHA
@Composable
fun TelaRecuperarSenhaOriginal(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal).statusBarsPadding()) {
        Card(shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 48.dp, bottom = 28.dp).align(Alignment.Center)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 30.dp)) {
                LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(10.dp)); Text("SAFEPOINT", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = azulEscuro); Text("Sempre te guiando a um ponto seguro", fontSize = 13.sp, color = azulEscuro); Spacer(modifier = Modifier.height(24.dp)); Text("Recuperar senha", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Text("Digite seu e-mail", fontSize = 12.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = email, onValueChange = { email = it.trim() }, placeholder = { Text("E-mail", color = cinzaTexto) }, leadingIcon = { Icon(Icons.Outlined.Email, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(modifier = Modifier.height(24.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable {
                    if (email.isNotBlank()) auth.sendPasswordResetEmail(email).addOnSuccessListener { Toast.makeText(context, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show(); navController.popBackStack() }.addOnFailureListener { Toast.makeText(context, "Erro: ${it.message}", Toast.LENGTH_LONG).show() } else Toast.makeText(context, "Digite o e-mail", Toast.LENGTH_SHORT).show()
                }) { Text("Enviar Instruções", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp)); TextButton(onClick = { navController.popBackStack() }) { Text("Voltar", color = azulEscuro) }
            }
        }
    }
}

// PERMISSAO LOCALIZACAO
@Composable fun TelaPermissaoLocalizacao(onAceitar: () -> Unit, onRecusar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(24.dp)); Text("Permitir localização?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(12.dp)); Text("Precisamos da sua localização exata para mostrar rotas de fuga seguras, alertas ao seu redor e os abrigos mais próximos.", fontSize = 14.sp, color = cinzaTexto, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(32.dp)); Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable { onAceitar() }) { Text("Permitir Localização", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = onRecusar) { Text("Agora não", color = cinzaTexto) }
    }
}

// TERMOS
@Composable
fun TelaTermosUso(onAceitar: () -> Unit) {
    var aceitou by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.Default.ArrowBack, null); Spacer(modifier = Modifier.width(4.dp)); Text("Voltar", color = azulEscuro) }
        Spacer(modifier = Modifier.height(8.dp)); LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("Sempre te guiando a um ponto seguro", fontSize = 12.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(24.dp))
        Text("Termos de Uso e Política de Privacidade", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = azulEscuro, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("1. OBJETIVO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O SafePoint oferece um sistema de mapeamento, detecção de áreas de perigo e rotas de evacuação seguras para abrigos e pontos de apoio.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("2. LOCALIZAÇÃO E ROTAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Sua localização é utilizada para calcular o abrigo mais próximo e traçar trajetos seguros. Os dados não são vendidos nem compartilhados para publicidade.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F4FF)).clickable { aceitou = !aceitou }.padding(14.dp)) { Checkbox(checked = aceitou, onCheckedChange = { aceitou = it }, modifier = Modifier.padding(top = 2.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Eu li, compreendi e concordo integralmente com os Termos de Uso e a Política de Privacidade.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { if (aceitou) onAceitar() }, enabled = aceitou, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (aceitou) azulEscuro else Color.Gray)) { Text("Aceitar e Continuar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// HOME
@Composable fun TelaHome(navController: NavController) {
    val context = LocalContext.current
    var nomeUsuario by remember { mutableStateOf("Usuário") }
    val db = FirebaseFirestore.getInstance()
    LaunchedEffect(Unit) {
        val userId = PrefsHelper.getUserId(context)
        if (userId != null) db.collection("usuarios").document(userId).get().addOnSuccessListener { doc ->
            doc.getString("nome")?.let { if (it.isNotBlank()) nomeUsuario = it }
        }
    }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF7F8FA)) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).background(Color(0xFFF7F8FA)).padding(horizontal = 22.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 18.dp, bottom = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("SafePoint", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF174A86))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(11.dp).background(Color(0xFF35A66A), CircleShape))
            }
            Text("Olá, $nomeUsuario!", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174A86))
            Text("Bem-vindo ao SafePoint.\nEstamos aqui para te proteger.", modifier = Modifier.padding(top = 10.dp, bottom = 30.dp), fontSize = 17.sp, lineHeight = 22.sp, color = Color(0xFF355777))
            Card(Modifier.fillMaxWidth().heightIn(min = 190.dp).clickable { navController.navigate(Screen.SOS.route) }, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8EC)), elevation = CardDefaults.cardElevation(5.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Text("SOS", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB83238)) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Emergência", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB83238))
                        Text("Precisando de ajuda urgente? Envie um alerta imediato.", modifier = Modifier.padding(top = 6.dp), fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF355777))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFB83238), modifier = Modifier.size(28.dp))
                }
            }
            Text("Acesso rápido", modifier = Modifier.padding(top = 30.dp, bottom = 14.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174A86))
            Card(Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Ajuda.route) }, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F0FF)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Icon(Icons.Default.HelpOutline, null, tint = Color(0xFFD94848), modifier = Modifier.size(42.dp))
                    Text("Preciso de ajuda", modifier = Modifier.padding(top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1760A7))
                    Text("Dúvidas e orientações completas sobre como se proteger.", modifier = Modifier.padding(top = 8.dp), fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF355777))
                }
            }
            Card(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 22.dp).clickable { navController.navigate(Screen.Historico.route) }, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7EF)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD94848), modifier = Modifier.size(38.dp))
                    Text("Histórico de alertas", modifier = Modifier.padding(top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF25804D))
                    Text("Consulte ocorrências e alertas registrados nas regiões.", modifier = Modifier.padding(top = 8.dp), fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF355777))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// -------------------------------------------------------------
// MODELOS E DADOS PARA MAPA E HISTÓRICO
// -------------------------------------------------------------

data class RotaInfo(val distanciaKm: Double, val duracaoMin: Int, val duracaoTexto: String, val distanciaTexto: String, val chegadaTexto: String)

enum class ModoTransporte(val titulo: String, val velocidadeKmh: Double, val icone: ImageVector) {
    CARRO("Carro", 45.0, Icons.Default.DirectionsCar),
    MOTO("Moto", 52.0, Icons.Default.TwoWheeler),
    ONIBUS("Ônibus", 22.0, Icons.Default.DirectionsBus),
    BICICLETA("Bicicleta", 15.0, Icons.Default.DirectionsBike),
    A_PE("A pé", 4.5, Icons.Default.DirectionsWalk)
}

data class CidadeRef(val nome: String, val uf: String, val lat: Double, val lng: Double, val aliases: List<String> = emptyList())
data class AlertaMapa(val id: String, val cidade: String, val tipo: String, val gravidade: String, val lat: Double, val lng: Double, val descricao: String, val dataHora: String)
data class AbrigoMapa(val id: String, val cidade: String, val nome: String, val lat: Double, val lng: Double, val endereco: String)

// Dados realistas de cidades e bairros terrestres
object SafePointGeoEngine {
    val cidades = listOf(
        CidadeRef("São Paulo", "SP", -23.5505, -46.6333, listOf("sao paulo", "sp", "paulista")),
        CidadeRef("Rio de Janeiro", "RJ", -22.9068, -43.1729, listOf("rio", "rj", "rio de janeiro")),
        CidadeRef("Belo Horizonte", "MG", -19.9167, -43.9345, listOf("bh", "belo horizonte", "mg")),
        CidadeRef("Brasília", "DF", -15.7939, -47.8828, listOf("brasilia", "df")),
        CidadeRef("Curitiba", "PR", -25.4284, -49.2733, listOf("curitiba", "pr")),
        CidadeRef("Porto Alegre", "RS", -30.0346, -51.2177, listOf("porto alegre", "rs")),
        CidadeRef("Salvador", "BA", -12.9777, -38.5016, listOf("salvador", "ba")),
        CidadeRef("Fortaleza", "CE", -3.7319, -38.5267, listOf("fortaleza", "ce")),
        CidadeRef("Recife", "PE", -8.0476, -34.8770, listOf("recife", "pe")),
        CidadeRef("Manaus", "AM", -3.1190, -60.0217, listOf("manaus", "am")),
        CidadeRef("Goiânia", "GO", -16.6869, -49.2648, listOf("goiania", "go")),
        CidadeRef("Campinas", "SP", -22.9056, -47.0608, listOf("campinas")),
        CidadeRef("Florianópolis", "SC", -27.5954, -48.5480, listOf("florianopolis", "floripa")),
        CidadeRef("Vitória", "ES", -20.3155, -40.3128, listOf("vitoria", "es")),
        CidadeRef("Campo Grande", "MS", -20.4697, -54.6201, listOf("campo grande")),
        CidadeRef("Cuiabá", "MT", -15.6014, -56.0979, listOf("cuiaba")),
        CidadeRef("Natal", "RN", -5.7945, -35.2110, listOf("natal")),
        CidadeRef("João Pessoa", "PB", -7.1195, -34.8450, listOf("joao pessoa")),
        CidadeRef("Maceió", "AL", -9.6498, -35.7089, listOf("maceio")),
        CidadeRef("Teresina", "PI", -5.0892, -42.8016, listOf("teresina")),
        CidadeRef("Aracaju", "SE", -10.9472, -37.0731, listOf("aracaju")),
        CidadeRef("São Luís", "MA", -2.5307, -44.3068, listOf("sao luis")),
        CidadeRef("Belém", "PA", -1.4558, -48.5039, listOf("belem")),
        CidadeRef("Porto Velho", "RO", -8.7612, -63.9004, listOf("porto velho")),
        CidadeRef("Rio Branco", "AC", -9.9754, -67.8249, listOf("rio branco")),
        CidadeRef("Palmas", "TO", -10.2491, -48.3243, listOf("palmas")),
        CidadeRef("Macapá", "AP", 0.0349, -51.0694, listOf("macapa")),
        CidadeRef("Boa Vista", "RR", 2.8235, -60.6758, listOf("boa vista")),
        CidadeRef("Londrina", "PR", -23.3045, -51.1696, listOf("londrina")),
        CidadeRef("Ribeirão Preto", "SP", -21.1704, -47.8103, listOf("ribeirao preto")),
        CidadeRef("Santos", "SP", -23.9608, -46.3336, listOf("santos")),
        CidadeRef("Sorocaba", "SP", -23.5015, -47.4526, listOf("sorocaba")),
        CidadeRef("Caxias do Sul", "RS", -29.1634, -51.1797, listOf("caxias do sul"))
    )

    val tiposDisponiveis = listOf("Enchente", "Incêndio", "Tempestade", "Deslizamento", "Terremoto", "Vendaval", "Acidente Químico", "Seca")
    val gravidadesDisponiveis = listOf("Baixa", "Média", "Alta", "Crítica")

    // Cores específicas por tipo de alerta para identificação visual imediata
    fun corDoTipo(tipo: String): Color = when (tipo) {
        "Enchente" -> Color(0xFF0288D1)        // Azul Oceano
        "Incêndio" -> Color(0xFFE64A19)        // Laranja Fogo
        "Tempestade" -> Color(0xFF7B1FA2)      // Roxo
        "Deslizamento" -> Color(0xFF795548)    // Marrom
        "Terremoto" -> Color(0xFFC62828)       // Vermelho Carmim
        "Vendaval" -> Color(0xFF1565C0)        // Azul Escuro
        "Acidente Químico" -> Color(0xFFF57F17)// Âmbar Químico
        "Seca" -> Color(0xFFFB8C00)            // Dourado
        else -> Color(0xFFD32F2F)
    }

    // Gerador de alertas e abrigos terrestres com offsets seguros longe do mar
    val listaAlertas: List<AlertaMapa> by lazy {
        val list = mutableListOf<AlertaMapa>()
        val descricoes = mapOf(
            "Enchente" to "Vias intransitáveis devido ao acúmulo de águas pluviais. Atenção ao transitar nas imediações.",
            "Incêndio" to "Incêndio ativo na vegetação e estruturas próximas. Fumaça densa e risco de propagação.",
            "Tempestade" to "Chuva forte com rajadas de vento intensas e descargas atmosféricas. Busque proteção imediatamente.",
            "Deslizamento" to "Instabilidade de encosta detectada pelo monitoramento geológico. Risco de escorregamento.",
            "Terremoto" to "Abalo sísmico registrado na região. Mantenha distância de estruturas fragilizadas.",
            "Vendaval" to "Ventos fortes superiores a 70 km/h com risco de queda de galhos e interrupção na fiação elétrica.",
            "Acidente Químico" to "Vazamento de produto químico tóxico em transporte rodoviário. Evite inalação e isole a área.",
            "Seca" to "Estiagem severa com baixa umidade do ar e risco elevado de focos de queimada."
        )

        val datas = listOf("Hoje às 09:42", "Hoje às 07:15", "Ontem às 21:30", "Ontem às 18:45", "05/11 às 14:10", "04/11 às 11:20", "03/11 às 16:05")

        // Para cidades litorâneas (Santos, Rio, Salvador, etc.), empurramos os offsets para o interior (oeste/norte) para nunca cair no mar
        cidades.forEachIndexed { cIdx, cidade ->
            val ehLitoral = cidade.nome in listOf("Santos", "Rio de Janeiro", "Salvador", "Fortaleza", "Recife", "Florianópolis", "Vitória", "Natal", "Maceió", "João Pessoa", "Aracaju")
            val sinalLng = if (ehLitoral) -1.0 else 1.0 // Se for litoral no Brasil, ir para o oeste (interior)

            // Gera 3 alertas distintos por cidade com gravidades variadas (garantindo Baixa, Média, Alta e Crítica)
            for (i in 0..2) {
                val tipo = tiposDisponiveis[(cIdx * 3 + i) % tiposDisponiveis.size]
                val gravidade = gravidadesDisponiveis[(cIdx + i) % gravidadesDisponiveis.size]
                val dLat = (if (i == 0) 0.012 else if (i == 1) -0.015 else 0.008) + (cIdx % 3) * 0.002
                val dLng = sinalLng * ((if (i == 0) 0.014 else if (i == 1) 0.018 else -0.012) + (cIdx % 2) * 0.003)
                val dataHora = datas[(cIdx + i) % datas.size]
                val desc = descricoes[tipo] ?: "Ocorrência registrada sob monitoramento preventivo da Defesa Civil."

                list.add(
                    AlertaMapa(
                        id = "alt-${cidade.nome}-$i",
                        cidade = cidade.nome,
                        tipo = tipo,
                        gravidade = gravidade,
                        lat = cidade.lat + dLat,
                        lng = cidade.lng + dLng,
                        descricao = "$desc Região: ${cidade.nome} ($gravidade).",
                        dataHora = dataHora
                    )
                )
            }
        }
        list
    }

    val listaAbrigos: List<AbrigoMapa> by lazy {
        val list = mutableListOf<AbrigoMapa>()
        val nomesAbrigos = listOf("Abrigo Central da Defesa Civil", "Ponto de Apoio e Socorro Municipal", "Centro Comunitário de Acolhimento", "Posto Integrado de Emergência")
        cidades.forEachIndexed { cIdx, cidade ->
            val ehLitoral = cidade.nome in listOf("Santos", "Rio de Janeiro", "Salvador", "Fortaleza", "Recife", "Florianópolis", "Vitória")
            val sinalLng = if (ehLitoral) -1.0 else 1.0

            // 2 abrigos seguros por cidade, bem posicionados
            for (j in 0..1) {
                val dLat = (if (j == 0) -0.018 else 0.022)
                val dLng = sinalLng * (if (j == 0) -0.021 else 0.025)
                val nomeAbrigo = nomesAbrigos[(cIdx + j) % nomesAbrigos.size]
                list.add(
                    AbrigoMapa(
                        id = "abr-${cidade.nome}-$j",
                        cidade = cidade.nome,
                        nome = "$nomeAbrigo • ${cidade.nome}",
                        lat = cidade.lat + dLat,
                        lng = cidade.lng + dLng,
                        endereco = "Av. Principal Segura, ${100 + j * 240} - ${cidade.nome}"
                    )
                )
            }
        }
        list
    }

    fun buscarCidade(termo: String): CidadeRef? {
        val q = java.text.Normalizer.normalize(termo.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()
        if (q.isBlank()) return null
        return cidades.firstOrNull { c ->
            val n = java.text.Normalizer.normalize(c.nome.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            n == q || c.aliases.any { it.equals(q, ignoreCase = true) } || n.contains(q)
        }
    }
}

// -------------------------------------------------------------
// DESENHOS DE PINOS: SETA GPS, ABRIGO COM +, E ALERTA COLORIDO
// -------------------------------------------------------------

// Seta direcional azul GPS moderna com círculo e sombra
fun criarIconeSetaGps(context: Context, tamanhoPx: Int = 48): android.graphics.drawable.Drawable {
    val bmp = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val cx = tamanhoPx / 2f
    val cy = tamanhoPx / 2f
    val raio = tamanhoPx / 2.2f

    // Sombra do halo
    val paintSombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(50, 0, 0, 0)
    }
    canvas.drawCircle(cx, cy + 2f, raio, paintSombra)

    // Halo branco
    val paintHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(cx, cy, raio, paintHalo)

    // Círculo azul externo
    val paintAzul = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(21, 101, 192)
    }
    canvas.drawCircle(cx, cy, raio - 3f, paintAzul)

    // Seta apontando para cima (estilo GPS navigation arrow)
    val pathSeta = AndroidPath().apply {
        moveTo(cx, cy - raio * 0.65f)
        lineTo(cx + raio * 0.52f, cy + raio * 0.52f)
        lineTo(cx, cy + raio * 0.22f)
        lineTo(cx - raio * 0.52f, cy + raio * 0.52f)
        close()
    }
    val paintSeta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawPath(pathSeta, paintSeta)

    return BitmapDrawable(context.resources, bmp)
}

// Ponto de abrigo seguro: círculo verde vivo com ícone "+" médico/resgate branco
fun criarPinoAbrigoVerde(context: Context, tamanhoPx: Int = 46): android.graphics.drawable.Drawable {
    val bmp = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val cx = tamanhoPx / 2f
    val cy = tamanhoPx / 2f
    val raio = tamanhoPx / 2.2f

    val paintSombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(70, 0, 0, 0)
    }
    canvas.drawCircle(cx, cy + 3f, raio, paintSombra)

    // Círculo verde
    val paintVerde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(30, 168, 76) // Verde seguro SafePoint
    }
    canvas.drawCircle(cx, cy, raio, paintVerde)

    // Borda branca interna
    val paintBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = tamanhoPx * 0.07f
    }
    canvas.drawCircle(cx, cy, raio - (tamanhoPx * 0.035f), paintBorda)

    // Ícone de + branco grosso no centro
    val paintMais = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = tamanhoPx * 0.16f
        strokeCap = Paint.Cap.ROUND
    }
    val tamCruz = raio * 0.50f
    canvas.drawLine(cx - tamCruz, cy, cx + tamCruz, cy, paintMais)
    canvas.drawLine(cx, cy - tamCruz, cx, cy + tamCruz, paintMais)

    return BitmapDrawable(context.resources, bmp)
}

// Pino de alerta temático com a cor exata do tipo
fun criarPinoAlertaColorido(context: Context, tipo: String, gravidade: String, tamanhoPx: Int = 48): android.graphics.drawable.Drawable {
    val w = tamanhoPx
    val h = (w * 1.35f).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)

    val cor = SafePointGeoEngine.corDoTipo(tipo)
    val paintCor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.rgb((cor.red * 255).toInt(), (cor.green * 255).toInt(), (cor.blue * 255).toInt())
        style = Paint.Style.FILL
    }
    val paintBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = w * 0.08f
    }

    val path = AndroidPath().apply {
        moveTo(w / 2f, 0f)
        cubicTo(w * 0.95f, 0f, w.toFloat(), h * 0.25f, w.toFloat(), h * 0.38f)
        cubicTo(w.toFloat(), h * 0.62f, w * 0.75f, h * 0.78f, w / 2f, h.toFloat())
        cubicTo(w * 0.25f, h * 0.78f, 0f, h * 0.62f, 0f, h * 0.38f)
        cubicTo(0f, h * 0.25f, w * 0.05f, 0f, w / 2f, 0f)
        close()
    }
    canvas.drawPath(path, paintCor)
    canvas.drawPath(path, paintBorda)

    // Letra ou símbolo de perigo no centro
    val letra = when (tipo) {
        "Enchente" -> "💧"
        "Incêndio" -> "🔥"
        "Tempestade" -> "⚡"
        "Deslizamento" -> "⛰️"
        "Terremoto" -> "⚠️"
        "Vendaval" -> "💨"
        "Acidente Químico" -> "☣️"
        else -> "!"
    }

    val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = w * 0.42f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText(letra, w / 2f, h * 0.50f, paintTexto)

    return BitmapDrawable(context.resources, bmp)
}

// -------------------------------------------------------------
// ALGORITMO DE ROTA DE CONTORNO INTELIGENTE (NÃO É UMA LINHA RETA)
// -------------------------------------------------------------
fun gerarRotaInteligenteContorno(inicio: GeoPoint, destino: GeoPoint, pontoAlerta: GeoPoint?): List<GeoPoint> {
    val pontos = mutableListOf<GeoPoint>()
    pontos.add(inicio)

    val latDist = destino.latitude - inicio.latitude
    val lngDist = destino.longitude - inicio.longitude
    val totalPassos = 14

    // Se houver um ponto de perigo no trajeto, adicionamos uma curva de desvio (offset perpendicular)
    for (i in 1 until totalPassos) {
        val progresso = i / totalPassos.toDouble()
        var latInter = inicio.latitude + latDist * progresso
        var lngInter = inicio.longitude + lngDist * progresso

        // Se tem alerta, gera desvio seguro para contornar
        if (pontoAlerta != null) {
            val distAoAlerta = calcularDistancia(latInter, lngInter, pontoAlerta.latitude, pontoAlerta.longitude)
            if (distAoAlerta < 2.5) { // Se estiver muito perto do perigo, desvia lateralmente
                val desvio = (1.0 - (distAoAlerta / 2.5)) * 0.007
                latInter += desvio * kotlin.math.sin(progresso * Math.PI)
                lngInter += desvio * kotlin.math.cos(progresso * Math.PI)
            }
        } else {
            // Curvatura de malha urbana (imita esquinas e avenidas)
            val jitter = kotlin.math.sin(progresso * Math.PI * 3.0) * 0.0015
            latInter += jitter
            lngInter -= jitter * 0.6
        }
        pontos.add(GeoPoint(latInter, lngInter))
    }

    pontos.add(destino)
    return pontos
}

// -------------------------------------------------------------
// TELA DO MAPA PROFISSIONAL
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreenProfissional(navController: NavController, onVoltar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busca by remember { mutableStateOf("") }
    var cidadeAtual by remember { mutableStateOf(SafePointGeoEngine.cidades.first()) }
    var minhaPosicao by remember { mutableStateOf(GeoPoint(cidadeAtual.lat, cidadeAtual.lng)) }
    var temGps by remember { mutableStateOf(false) }

    // FILTROS: Não começam forçados/ativos; o usuário escolhe
    var filtroGravidade by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filtroTipo by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mostrarAbrigos by remember { mutableStateOf(true) }

    // Estado de seleção
    var alertaSelecionado by remember { mutableStateOf<AlertaMapa?>(null) }
    var abrigoSelecionado by remember { mutableStateOf<AbrigoMapa?>(null) }

    // Rota
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember { mutableStateOf<RotaInfo?>(null) }
    var modoTransporte by remember { mutableStateOf(ModoTransporte.CARRO) }
    var abrigoDestinoRota by remember { mutableStateOf<AbrigoMapa?>(null) }

    var mapaSatelite by remember { mutableStateOf(false) }
    var avisoBusca by remember { mutableStateOf("") }

    // Gerenciamento de Permissão Única
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val concedida = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedida) {
            temGps = true
            obterLocalizacao(context) { lat, lng ->
                minhaPosicao = GeoPoint(lat, lng)
            }
        } else {
            Toast.makeText(context, "Permissão negada. Você pode ativar nas configurações do celular a qualquer momento.", Toast.LENGTH_LONG).show()
        }
    }

    fun solicitarPermissaoGpsUmaVez() {
        val concedida = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (concedida) {
            temGps = true
            obterLocalizacao(context) { lat, lng -> minhaPosicao = GeoPoint(lat, lng) }
        } else {
            if (!PrefsHelper.jaPediuPermissaoGps(context)) {
                PrefsHelper.setJaPediuPermissaoGps(context, true)
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                // Se já pediu antes e negou, convida amigavelmente a ir nas configurações
                Toast.makeText(context, "Abra as configurações do celular para permitir o acesso à sua localização.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

    LaunchedEffect(Unit) {
        solicitarPermissaoGpsUmaVez()
    }

    // Camadas de Tile: OpenStreetMap e NASA GIBS Satélite
    val tileSourceRua = remember {
        XYTileSource("OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://a.tile.openstreetmap.org/", "https://b.tile.openstreetmap.org/", "https://c.tile.openstreetmap.org/"), "© OpenStreetMap contributors")
    }
    val tileSourceSatelite = remember {
        object : XYTileSource("NASA GIBS True Color", 0, 9, 256, ".jpg", arrayOf("https://gibs.earthdata.nasa.gov/"), "NASA Earthdata") {
            override fun getTileURLString(pMapTile: Long): String {
                val z = org.osmdroid.util.MapTileIndex.getZoom(pMapTile)
                val x = org.osmdroid.util.MapTileIndex.getX(pMapTile)
                val y = org.osmdroid.util.MapTileIndex.getY(pMapTile)
                // Pega data recente para garantir tiles disponíveis na NASA
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { add(Calendar.DAY_OF_YEAR, -3) }
                val data = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                return "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_SNPP_CorrectedReflectance_TrueColor/default/$data/GoogleMapsCompatible_Level9/$z/$y/$x.jpg"
            }
        }
    }

    val tileSource = if (mapaSatelite) tileSourceSatelite else tileSourceRua
    val mapView = remember {
        MapView(context.applicationContext).apply {
            setTileSource(tileSource)
            setUseDataConnection(true)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(13.5)
            controller.setCenter(minhaPosicao)
        }
    }

    LaunchedEffect(mapaSatelite) {
        mapView.setTileSource(tileSource)
        mapView.invalidate()
    }

    // Alertas e Abrigos filtrados conforme escolha do usuário
    val alertasVisiveis = remember(filtroGravidade, filtroTipo, cidadeAtual) {
        SafePointGeoEngine.listaAlertas.filter { alerta ->
            val passaGravidade = if (filtroGravidade.isEmpty()) true else alerta.gravidade in filtroGravidade
            val passaTipo = if (filtroTipo.isEmpty()) true else alerta.tipo in filtroTipo
            passaGravidade && passaTipo
        }
    }

    val abrigosVisiveis = remember(mostrarAbrigos) {
        if (mostrarAbrigos) SafePointGeoEngine.listaAbrigos else emptyList()
    }

    // Função para traçar rota até o abrigo seguro mais próximo
    fun iniciarRotaDeFuga(abrigoAlvo: AbrigoMapa, alertaDesvio: AlertaMapa?) {
        abrigoDestinoRota = abrigoAlvo
        val destinoGeo = GeoPoint(abrigoAlvo.lat, abrigoAlvo.lng)
        val perigoGeo = if (alertaDesvio != null) GeoPoint(alertaDesvio.lat, alertaDesvio.lng) else null

        val rota = gerarRotaInteligenteContorno(minhaPosicao, destinoGeo, perigoGeo)
        rotaPontos = rota

        // Cálculo da distância total
        var distTotalKm = 0.0
        for (i in 0 until rota.size - 1) {
            distTotalKm += calcularDistancia(rota[i].latitude, rota[i].longitude, rota[i + 1].latitude, rota[i + 1].longitude)
        }
        if (distTotalKm < 0.5) distTotalKm = 0.8 // margem mínima

        val horas = distTotalKm / modoTransporte.velocidadeKmh
        val minutos = (horas * 60).toInt().coerceAtLeast(2)
        val duracaoTexto = if (minutos < 60) "$minutos min" else "${minutos / 60}h ${minutos % 60}min"
        val distTexto = String.format(Locale("pt", "BR"), "%.1f km", distTotalKm)

        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, minutos) }
        val chegadaTexto = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(cal.time)

        rotaInfo = RotaInfo(distTotalKm, minutos, duracaoTexto, distTexto, chegadaTexto)

        avisoBusca = "Rota de fuga segura calculada até ${abrigoAlvo.nome.take(28)}..."
        mapView.controller.animateTo(GeoPoint((minhaPosicao.latitude + destinoGeo.latitude) / 2, (minhaPosicao.longitude + destinoGeo.longitude) / 2))
        mapView.controller.setZoom(13.8)
        mapView.invalidate()
    }

    fun cancelarRota() {
        rotaPontos = emptyList()
        rotaInfo = null
        abrigoDestinoRota = null
        avisoBusca = "Rota cancelada pelo usuário."
        mapView.invalidate()
    }

    fun buscarLocalDigitado() {
        val achada = SafePointGeoEngine.buscarCidade(busca)
        if (achada != null) {
            cidadeAtual = achada
            minhaPosicao = GeoPoint(achada.lat, achada.lng)
            mapView.controller.animateTo(minhaPosicao)
            mapView.controller.setZoom(13.5)
            avisoBusca = "Localização definida: ${achada.nome} - ${achada.uf}"
        } else {
            avisoBusca = "Região não encontrada. Tente outra cidade brasileira."
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF3F6F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Barra Superior de Busca
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF75E0A2), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SafePoint • Mapa", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if (mapaSatelite) "Observação Terrestre NASA GIBS" else "Monitoramento de Riscos e Abrigos", color = Color(0xFFBCE8D7), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = busca,
                        onValueChange = { busca = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Buscar cidade, bairro ou região", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { buscarLocalDigitado() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9A72))
                    ) {
                        Text("Ir")
                    }
                }
                if (avisoBusca.isNotBlank()) {
                    Text(avisoBusca, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Barra de Filtros: NÃO começam ativos, o usuário escolhe
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = mostrarAbrigos,
                        onClick = { mostrarAbrigos = !mostrarAbrigos },
                        label = { Text("🟢 Abrigos (+)", fontSize = 11.sp) }
                    )
                }
                items(SafePointGeoEngine.gravidadesDisponiveis) { nivel ->
                    val ativo = nivel in filtroGravidade
                    FilterChip(
                        selected = ativo,
                        onClick = {
                            filtroGravidade = if (ativo) filtroGravidade - nivel else filtroGravidade + nivel
                        },
                        label = { Text(nivel, fontSize = 11.sp) }
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 2.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(SafePointGeoEngine.tiposDisponiveis) { tipo ->
                    val ativo = tipo in filtroTipo
                    FilterChip(
                        selected = ativo,
                        onClick = {
                            filtroTipo = if (ativo) filtroTipo - tipo else filtroTipo + tipo
                        },
                        label = { Text(tipo, fontSize = 11.sp) }
                    )
                }
            }

            // Área Central do Mapa
            Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
                    view.overlays.removeAll { it is Marker || it is Polyline }

                    // Linha da Rota
                    if (rotaPontos.isNotEmpty()) {
                        val linha = Polyline().apply {
                            setPoints(rotaPontos)
                            outlinePaint.color = android.graphics.Color.rgb(33, 150, 243)
                            outlinePaint.strokeWidth = 11f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                        }
                        view.overlays.add(linha)
                    }

                    // Marcadores de Alertas
                    alertasVisiveis.forEach { alerta ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(alerta.lat, alerta.lng)
                            title = "${alerta.tipo} • ${alerta.gravidade}"
                            snippet = alerta.descricao
                            icon = criarPinoAlertaColorido(context, alerta.tipo, alerta.gravidade, 48)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                alertaSelecionado = alerta
                                abrigoSelecionado = null
                                true
                            }
                        }
                        view.overlays.add(marker)
                    }

                    // Marcadores de Abrigos Seguros com "+"
                    abrigosVisiveis.forEach { abrigo ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(abrigo.lat, abrigo.lng)
                            title = abrigo.nome
                            snippet = abrigo.endereco
                            icon = criarPinoAbrigoVerde(context, 44)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { _, _ ->
                                abrigoSelecionado = abrigo
                                alertaSelecionado = null
                                true
                            }
                        }
                        view.overlays.add(marker)
                    }

                    // Marcador de Posição do Usuário (Seta Azul de GPS)
                    val pinoUsuario = Marker(view).apply {
                        position = minhaPosicao
                        title = "Sua localização atual"
                        icon = criarIconeSetaGps(context, 48)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    view.overlays.add(pinoUsuario)

                    view.invalidate()
                }

                // Botões de Ação Rápida Flutuantes
                Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { mapaSatelite = !mapaSatelite },
                        containerColor = Color.White
                    ) {
                        Icon(Icons.Default.Layers, if (mapaSatelite) "Modo Ruas" else "Modo Satélite", tint = Color(0xFF0B2545))
                    }
                    SmallFloatingActionButton(
                        onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble + 1); mapView.invalidate() },
                        containerColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, "Mais zoom", tint = Color(0xFF0B2545))
                    }
                    SmallFloatingActionButton(
                        onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble - 1); mapView.invalidate() },
                        containerColor = Color.White
                    ) {
                        Icon(Icons.Default.Remove, "Menos zoom", tint = Color(0xFF0B2545))
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            solicitarPermissaoGpsUmaVez()
                            mapView.controller.animateTo(minhaPosicao)
                            mapView.controller.setZoom(14.5)
                        },
                        containerColor = Color.White
                    ) {
                        Icon(Icons.Default.MyLocation, "Centralizar GPS", tint = Color(0xFF1976D2))
                    }
                }

                // Card Inferior com Detalhes ao Clicar no Alerta (Com Botão "X")
                alertaSelecionado?.let { alerta ->
                    Card(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).background(SafePointGeoEngine.corDoTipo(alerta.tipo), CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${alerta.tipo} • Gravidade ${alerta.gravidade}", fontWeight = FontWeight.Bold, color = Color(0xFF0B2545), fontSize = 16.sp)
                                    Text(alerta.dataHora, color = Color.Gray, fontSize = 11.sp)
                                }
                                // Botão de Fechar com "X"
                                IconButton(onClick = { alertaSelecionado = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar descrição", tint = Color(0xFF0B2545))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(alerta.descricao, color = Color(0xFF334155), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(12.dp))

                            // Seletor de Meios de Transporte
                            Text("Escolha seu meio de transporte para evacuação:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0B2545))
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                ModoTransporte.values().forEach { modo ->
                                    FilterChip(
                                        selected = modoTransporte == modo,
                                        onClick = {
                                            modoTransporte = modo
                                            // Se já tem rota, recalcula com a nova velocidade
                                            abrigoDestinoRota?.let { iniciarRotaDeFuga(it, alerta) }
                                        },
                                        leadingIcon = { Icon(modo.icone, null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(modo.titulo, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        // Encontra o abrigo mais próximo do usuário
                                        val maisProximo = SafePointGeoEngine.listaAbrigos.minByOrNull {
                                            calcularDistancia(minhaPosicao.latitude, minhaPosicao.longitude, it.lat, it.lng)
                                        }
                                        maisProximo?.let { iniciarRotaDeFuga(it, alerta) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Traçar rota ao abrigo", fontSize = 12.sp)
                                }

                                if (rotaPontos.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { cancelarRota() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                                    ) {
                                        Text("Cancelar rota", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Card ao Clicar no Abrigo Verde com "+"
                abrigoSelecionado?.let { abrigo ->
                    Card(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).background(Color(0xFF2E7D32), CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(abrigo.nome, fontWeight = FontWeight.Bold, color = Color(0xFF0B2545), fontSize = 15.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { abrigoSelecionado = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Fechar", tint = Color.Gray)
                                }
                            }
                            Text(abrigo.endereco, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { iniciarRotaDeFuga(abrigo, null) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsRun, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ir para este abrigo")
                                }
                                if (rotaPontos.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { cancelarRota() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                                    ) {
                                        Text("Cancelar")
                                    }
                                }
                            }
                        }
                    }
                }

                // Painel de Informação de Rota Ativa (Com Botão Cancelar)
                rotaInfo?.let { info ->
                    if (alertaSelecionado == null && abrigoSelecionado == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE8F5E9),
                            shadowElevation = 6.dp
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(modoTransporte.icone, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Rota Segura • ${modoTransporte.titulo}", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 14.sp)
                                    Text("${info.distanciaTexto} • Chegada às ${info.chegadaTexto} (${info.duracaoTexto})", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { cancelarRota() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancelar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TELA HISTÓRICO DE ALERTAS
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoReal(navController: NavController) {
    var busca by remember { mutableStateOf("") }
    // Filtros: Não começam travados/ativos, o usuário escolhe
    var filtroGravidade by remember { mutableStateOf<String?>(null) }
    var filtroTipo by remember { mutableStateOf<String?>(null) }
    var alertaDetalhe by remember { mutableStateOf<AlertaMapa?>(null) }

    val todosAlertas = remember { SafePointGeoEngine.listaAlertas }

    val alertasFiltrados = remember(busca, filtroGravidade, filtroTipo) {
        todosAlertas.filter { alerta ->
            val termo = busca.trim().lowercase()
            val passaBusca = if (termo.isBlank()) true else {
                alerta.cidade.lowercase().contains(termo) ||
                        alerta.tipo.lowercase().contains(termo) ||
                        alerta.descricao.lowercase().contains(termo)
            }
            val passaGravidade = if (filtroGravidade == null) true else alerta.gravidade == filtroGravidade
            val passaTipo = if (filtroTipo == null) true else alerta.tipo == filtroTipo
            passaBusca && passaGravidade && passaTipo
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF2F5F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Cabeçalho
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = Color(0xFF83E3B1), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Histórico de Alertas", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Ocorrências registradas por onde você navegou e consultou", color = Color(0xFFC9E8DF), fontSize = 11.sp)
                    }
                }
            }

            // Barra de Busca
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Filtrar por cidade, bairro ou palavra-chave", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Filtros de Gravidade (Escolha livre do usuário)
            Text("Gravidade do evento", Modifier.padding(start = 16.dp, top = 2.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SafePointGeoEngine.gravidadesDisponiveis) { g ->
                    val sel = filtroGravidade == g
                    FilterChip(
                        selected = sel,
                        onClick = { filtroGravidade = if (sel) null else g },
                        label = { Text(g, fontSize = 11.sp) }
                    )
                }
            }

            // Filtros de Tipo (Escolha livre do usuário)
            Text("Tipo de ocorrência", Modifier.padding(start = 16.dp, top = 2.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SafePointGeoEngine.tiposDisponiveis) { t ->
                    val sel = filtroTipo == t
                    FilterChip(
                        selected = sel,
                        onClick = { filtroTipo = if (sel) null else t },
                        label = { Text(t, fontSize = 11.sp) }
                    )
                }
            }

            // Lista de Ocorrências com data, hora e descrição
            if (alertasFiltrados.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum alerta encontrado com os filtros selecionados.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alertasFiltrados, key = { it.id }) { alerta ->
                        val cor = SafePointGeoEngine.corDoTipo(alerta.tipo)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { alertaDetalhe = alerta },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Box(Modifier.width(6.dp).fillMaxHeight().background(cor))
                                Column(Modifier.weight(1f).padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${alerta.tipo} • ${alerta.cidade}", fontWeight = FontWeight.Bold, color = Color(0xFF0B2545), fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Surface(
                                            color = cor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(alerta.gravidade, color = cor, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(