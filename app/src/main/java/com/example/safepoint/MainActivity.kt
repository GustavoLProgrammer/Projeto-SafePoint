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
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
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
val CorAzulEscuro = azulEscuro
val CorAzulMedio = azulLinhas
val CorVerdeBotao = verdeGradiente
val SafePointBlue = Color(0xFF1554C0)
val SafePointGreen = verdeGradiente
val FundoGradiente = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientFundoOriginal = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientBotaoOriginal = Brush.horizontalGradient(colors = listOf(azulEscuro, verdeGradiente))
val BotaoGradiente = gradientBotaoOriginal

data class AlertaRota(val id: String, val titulo: String, val dataHora: String, val localizacao: String, val gravidade: String, val tipo: String, val latitude: Double, val longitude: Double, val distancia: Double = 0.0, val bairro: String = "", val cidade: String = "", val regiao: String = "")
data class Ocorrencia(val titulo: String, val endereco: String, val lat: Double, val lng: Double, val tipo: TipoOcorrencia, val categoria: String? = null)
enum class TipoOcorrencia { SAIDA, MONITOR, DESASTRE, SEGURANCA }
sealed class Screen(val route: String) {
    object Termos : Screen("termos"); object PermissaoLocalizacao : Screen("permissao_localizacao"); object Login : Screen("login"); object Cadastro : Screen("cadastro"); object RecuperarSenha : Screen("recuperar_senha"); object Home : Screen("home"); object Mapa : Screen("mapa"); object Historico : Screen("historico"); object Perfil : Screen("perfil"); object SOS : Screen("sos"); object Ajuda : Screen("ajuda")
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
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var firebaseConfigured: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        firebaseConfigured = try {
            val firebaseApp = FirebaseApp.initializeApp(this)
            if (firebaseApp != null) {
                auth = FirebaseAuth.getInstance(firebaseApp)
                db = FirebaseFirestore.getInstance(firebaseApp)
                true
            } else false
        } catch (_: Exception) { false }
        try {
            val ctx = applicationContext
            Configuration.getInstance().userAgentValue = "SafePoint/1.0 (https://github.com/GustavoLProgrammer/Projeto-SafePoint; contato: gu45363@gmail.com)"
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            Configuration.getInstance().osmdroidBasePath = ctx.getExternalFilesDir(null)
            Configuration.getInstance().osmdroidTileCache = ctx.getExternalFilesDir("osmdroid_tiles")
        } catch (_: Exception) {}
        setContent {
            MaterialTheme {
                val navController = rememberNavController(); val context = LocalContext.current
                val termosAceitos = PrefsHelper.isTermosAceitos(context); val locAceita = PrefsHelper.isLocalizacaoAceita(context); val currentUser = if (firebaseConfigured) auth.currentUser else null
                val startDest = when {
                    !termosAceitos -> Screen.Termos.route
                    !locAceita -> Screen.PermissaoLocalizacao.route
                    !firebaseConfigured -> Screen.Home.route
                    currentUser == null -> Screen.Login.route
                    else -> Screen.Home.route
                }
                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Termos.route) { TelaTermosUso(onAceitar = { PrefsHelper.setTermosAceitos(context, true); navController.navigate(Screen.PermissaoLocalizacao.route) { popUpTo(Screen.Termos.route) { inclusive = true } } }) }
                    composable(Screen.PermissaoLocalizacao.route) { TelaPermissaoLocalizacao(onAceitar = {
                        PrefsHelper.setLocalizacaoAceita(context, true)
                        val destino = if (firebaseConfigured && auth.currentUser == null) Screen.Login.route else Screen.Home.route
                        navController.navigate(destino) { popUpTo(0) { inclusive = true } }
                    }, onRecusar = { Toast.makeText(context, "Você pode pesquisar locais manualmente sem compartilhar sua localização.", Toast.LENGTH_LONG).show(); navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } }) }
                    composable(Screen.Login.route) { if (firebaseConfigured) TelaLoginOriginal(navController, auth) else TelaFirebaseIndisponivel(navController, "Entrar") }
                    composable(Screen.Cadastro.route) { if (firebaseConfigured) TelaCadastroEscudoOriginal(navController, auth) else TelaFirebaseIndisponivel(navController, "Criar conta") }
                    composable(Screen.RecuperarSenha.route) { if (firebaseConfigured) TelaRecuperarSenhaOriginal(navController, auth) else TelaFirebaseIndisponivel(navController, "Recuperar senha") }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { MapaScreenProfissional(navController = navController, onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Historico.route) { TelaHistoricoReal(navController) }
                    composable(Screen.Perfil.route) { if (firebaseConfigured) TelaPerfil(navController, auth, db) else TelaFirebaseIndisponivel(navController, "Perfil") }
                    composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Ajuda.route) { TelaAjudaComoUsar(onVoltar = { navController.popBackStack() }) }
                }
            }
        }
    }
}

@Composable
fun TelaFirebaseIndisponivel(navController: NavController, titulo: String) {
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoSafePoint(modifier = Modifier.size(84.dp))
            Spacer(Modifier.height(18.dp))
            Text("$titulo indisponível", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro)
            Spacer(Modifier.height(10.dp))
            Text(
                "O mapa e os alertas públicos continuam disponíveis. Para ativar login, cadastro e sincronização de perfil, configure o Firebase deste aplicativo.",
                fontSize = 14.sp, color = cinzaTexto, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = { navController.navigate(Screen.Mapa.route) { launchSingleTop = true } }) { Text("Abrir mapa") }
            TextButton(onClick = { navController.navigate(Screen.Historico.route) { launchSingleTop = true } }) { Text("Ver histórico de alertas") }
            TextButton(onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } }) { Text("Voltar ao início") }
        }
    }
}

// LOGO ORIGINAL PIN - LOGIN - EXATO SEU ARQUIVO .TXT
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

// LOGO ESCUDO AZUL PERFEITO SEM FALHAS - CADASTRO - IGUAL ORIGINAL
@Composable
fun LogoEscudoSafePoint(modifier: Modifier = Modifier) {
    // Escudo 🛡️ azul perfeito, pontudo, bonito, sem falhas - igual emoji
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val azul = Color(0xFF0F3E8A)
        val azulClaro = Color(0xFF1A5DC0)
        // Path escudo clássico 🛡️ - topo reto levemente curvo, laterais retas, ponta no meio
        val escudo = Path().apply {
            moveTo(w * 0.5f, h * 0.02f)
            // topo arredondado
            cubicTo(w * 0.78f, h * 0.02f, w * 0.92f, h * 0.08f, w * 0.92f, h * 0.22f)
            // lateral direita descendo
            lineTo(w * 0.92f, h * 0.48f)
            cubicTo(w * 0.92f, h * 0.70f, w * 0.70f, h * 0.88f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.30f, h * 0.88f, w * 0.08f, h * 0.70f, w * 0.08f, h * 0.48f)
            lineTo(w * 0.08f, h * 0.22f)
            cubicTo(w * 0.08f, h * 0.08f, w * 0.22f, h * 0.02f, w * 0.5f, h * 0.02f)
            close()
        }
        // Sombra suave
        drawPath(path = escudo, color = azul.copy(alpha = 0.25f), style = Stroke(width = 8.dp.toPx()))
        // Preenchimento degradê azul
        drawPath(path = escudo, brush = Brush.verticalGradient(colors = listOf(azulClaro, azul), startY = 0f, endY = h), style = Fill)
        // Borda branca interna
        drawPath(path = escudo, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 2.5.dp.toPx()))
        // Brilho topo esquerdo
        val brilho = Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            cubicTo(w * 0.30f, h * 0.08f, w * 0.42f, h * 0.06f, w * 0.48f, h * 0.10f)
            lineTo(w * 0.38f, h * 0.18f)
            cubicTo(w * 0.30f, h * 0.16f, w * 0.24f, h * 0.14f, w * 0.22f, h * 0.12f)
            close()
        }
        drawPath(path = brilho, color = Color.White.copy(alpha = 0.35f), style = Fill)
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

// TELAS LOGIN/CADASTRO ORIGINAIS COM CENTRALIZAÇÃO
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
            Text(text = "SISTEMA INTELIGENTE DE SEGURANÇA E EVACUAÇÃO", fontSize = 9.sp, color = cinzaTexto, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp)); Text("Crie sua conta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(4.dp)); Text("Preencha os dados abaixo para se cadastrar", fontSize = 12.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(20.dp))
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

@Composable fun TelaPermissaoLocalizacao(onAceitar: () -> Unit, onRecusar: () -> Unit) {
    val context = LocalContext.current
    val jaAutorizada = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var pedidoPermissaoJaFeito by remember { mutableStateOf(PrefsHelper.getPrefs(context).getBoolean("location_permission_prompted", false)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val concedida = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedida) onAceitar() else { Toast.makeText(context, "Sem localização, você ainda pode pesquisar cidades manualmente.", Toast.LENGTH_LONG).show(); onRecusar() }
    }
    LaunchedEffect(jaAutorizada) { if (jaAutorizada) onAceitar() }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(24.dp)); Text("Permitir localização?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(12.dp)); Text("Usaremos a permissão do Android para centralizar o mapa e calcular distâncias. O pedido do sistema será feito nesta etapa; a localização não será substituída por coordenadas fictícias.", fontSize = 14.sp, color = cinzaTexto, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(32.dp)); Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable {
            if (!pedidoPermissaoJaFeito) {
                PrefsHelper.getPrefs(context).edit().putBoolean("location_permission_prompted", true).apply()
                pedidoPermissaoJaFeito = true
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }
        }) { Text(if (pedidoPermissaoJaFeito) "Abrir configurações" else "Permitir localização", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = onRecusar) { Text("Agora não", color = cinzaTexto) }
    }
}

@Composable
fun TelaTermosUso(onAceitar: () -> Unit) {
    var aceitou by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.Default.ArrowBack, null); Spacer(modifier = Modifier.width(4.dp)); Text("Voltar", color = azulEscuro) }
        Spacer(modifier = Modifier.height(8.dp)); LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("Sempre te guiando a um ponto seguro", fontSize = 12.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(4.dp)); Text("SISTEMA INTELIGENTE DE SEGURANÇA E EVACUAÇÃO", fontSize = 10.sp, color = cinzaTexto, letterSpacing = 0.8.sp); Spacer(modifier = Modifier.height(24.dp))
        Text("Termos de Uso e Política de Privacidade", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = azulEscuro, textAlign = TextAlign.Center); Text("Última atualização: Outubro de 2026 - Versão 2.0 Extensa", fontSize = 11.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("1. OBJETIVO E ACEITAÇÃO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O SafePoint é um sistema inteligente de segurança, evacuação e resposta a desastres naturais, que utiliza mapas reais, dados de localização exata e integração com serviços de emergência. Ao criar uma conta e utilizar o aplicativo, você declara ter lido, compreendido e concordado integralmente com estes Termos de Uso e Política de Privacidade. Se não concordar, não utilize o aplicativo.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("2. CADASTRO E RESPONSABILIDADE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Você se compromete a fornecer dados verídicos, completos e atualizados no cadastro (nome, e-mail, telefone, data de nascimento). É responsável por manter a confidencialidade de sua senha e por todas as atividades realizadas em sua conta.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("3. LOCALIZAÇÃO EXATA - CONSENTIMENTO EXPRESSO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Para funcionamento essencial, coletamos e processamos sua localização exata (GPS fino e grosso), quando o app está em uso, para: (a) exibir sua posição no mapa real; (b) calcular rotas estimadas; (c) identificar abrigos, hospitais, bombeiros, polícia e farmácias próximos via Overpass; (d) gerar histórico de alertas regionais com distância em km; (e) melhorar alertas de desastres com base em sua região. Dados são criptografados, armazenados no Firebase Firestore e nunca vendidos.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("4. MAPA REAL PROFISSIONAL - TECNOLOGIA SEM 403", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O mapa utiliza tecnologia OpenStreetMap via CartoDB Voyager (tile server profissional que segue osm.wiki/Blocked e não bloqueia com 403), com cache local, User-Agent identificado como SafePoint/1.0 com contato, e integração com APIs públicas: GDACS para desastres globais, Overpass para pontos de segurança reais, e Nominatim para geocodificação reversa. Não utilizamos Google Maps para garantir funcionamento sem API Key.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("5. FIREBASE, ARMAZENAMENTO E SEGURANÇA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Usamos Firebase Authentication para login seguro, Firestore para perfil e histórico, e Firebase Storage para fotos de perfil. As fotos são armazenadas em fotos_perfil/{userId}/profile.jpg com regras que permitem apenas o dono ler/escrever. Caso o bucket não exista ou haja erro 'Object does not exist at location', o app salva apenas dados de texto e informa o usuário.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("6. BOTÃO SOS E EMERGÊNCIA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O botão SOS facilita ligação rápida para 190 (Polícia), 192 (SAMU), 193 (Bombeiros) e compartilhamento de localização via WhatsApp. O SafePoint é ferramenta de apoio e NÃO substitui serviços oficiais de emergência.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("7. HISTÓRICO DE ALERTAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O histórico consulta eventos publicados pela GDACS nos últimos seis meses e calcula a distância aproximada a partir da localização ou local pesquisado. A cobertura depende da fonte e não substitui avisos da Defesa Civil.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("8. PRIVACIDADE E LGPD", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Conforme LGPD (Lei 13.709/2018), você tem direito a acessar, corrigir, excluir e portar seus dados. Coletamos apenas dados essenciais. Não compartilhamos dados com anunciantes.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F4FF)).clickable { aceitou = !aceitou }.padding(14.dp)) { Checkbox(checked = aceitou, onCheckedChange = { aceitou = it }, modifier = Modifier.padding(top = 2.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Eu li, compreendi e concordo integralmente com os Termos de Uso extensos e a Política de Privacidade, e autorizo expressamente o uso da minha localização exata.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { if (aceitou) onAceitar() }, enabled = aceitou, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (aceitou) azulEscuro else Color.Gray)) { Text("Aceitar e Continuar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable fun TelaHome(navController: NavController) {
    val context = LocalContext.current
    var nomeUsuario by remember { mutableStateOf("Usuário") }
    val db = remember { try { FirebaseFirestore.getInstance() } catch (_: Exception) { null } }
    LaunchedEffect(Unit) {
        val userId = PrefsHelper.getUserId(context)
        val store = db
        if (userId != null && store != null) store.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc -> doc.getString("nome")?.let { if (it.isNotBlank()) nomeUsuario = it } }
    }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA)).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).statusBarsPadding().padding(24.dp)) { Column(modifier = Modifier.padding(top = 8.dp)) { Text("Olá, $nomeUsuario !", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Sua segurança é nossa prioridade", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f)) } }
            Spacer(modifier = Modifier.height(24.dp)); Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Mapa.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(CorAzulMedio), contentAlignment = Alignment.Center) { Icon(Icons.Default.Map, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Mapa de Evacuação", fontWeight = FontWeight.Bold, color = CorAzulEscuro); Text("Veja rotas estimadas, alertas e abrigos cadastrados", fontSize = 13.sp, color = Color.Gray) } } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOS.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE53935)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Sos, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Emergência SOS", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C)); Text("Ligue 190/192/193 rapidamente", fontSize = 13.sp, color = Color.Gray) } } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Ajuda.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(SafePointGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Help, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Ajuda e Dúvidas", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)); Text("Como usar o SafePoint", fontSize = 13.sp, color = Color.Gray) } } }
            }
        }
    }
}

val NOMES_DESASTRES = mapOf("EQ" to "Terremoto", "TC" to "Ciclone", "FL" to "Enchente", "VO" to "Vulcão", "DR" to "Seca", "WF" to "Incêndio florestal", "TS" to "Tsunami")
val NOMES_SEGURANCA = mapOf("hospital" to "Hospital", "fire_station" to "Bombeiros", "police" to "Polícia", "shelter" to "Abrigo", "pharmacy" to "Farmácia")
val CORES_SEGURANCA = mapOf("hospital" to Color(0xFFD32F2F), "fire_station" to Color(0xFFF57C00), "police" to Color(0xFF1565C0), "shelter" to Color(0xFF2E7D32), "pharmacy" to Color(0xFF7B1FA2))



// MAPA PROFISSIONAL GPS REAL - VERSÃO 3.0 PROFISSIONAL COM ROTA AZUL, TEMPO E ALERTAS COLORIDOS
data class RotaInfo(val distanciaKm: Double, val duracaoMin: Int, val duracaoTexto: String, val distanciaTexto: String)
data class DesastreDetalhado(val ocorrencia: Ocorrencia, val alertLevel: String, val gravidade: String, val tipoCode: String, val nomeTipo: String)

@Composable
fun MapaScreenProfissional(navController: NavController, onVoltar: () -> Unit) {
    val context = LocalContext.current
    var filtrosDesastres by remember { mutableStateOf(NOMES_DESASTRES.keys) }
    var filtrosSeguranca by remember { mutableStateOf(setOf("hospital","fire_station","police","shelter","pharmacy","place_of_worship")) }
    val pontosSeguranca = remember { mutableStateListOf<Ocorrencia>() }
    val desastresGlobais = remember { mutableStateListOf<DesastreDetalhado>() }
    var buscaCidade by remember { mutableStateOf("") }
    var localizacaoAtual by remember { mutableStateOf<GeoPoint?>(null) }
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotasDisponiveis by remember { mutableStateOf<List<RotaCompleta>>(emptyList()) }
    var rotaSelecionadaIndex by remember { mutableIntStateOf(0) }
    var rotaInfo by remember { mutableStateOf<RotaInfo?>(null) }
    var modoTransporte by remember { mutableStateOf("carro") }
    var destinoRota by remember { mutableStateOf<Pair<GeoPoint, String>?>(null) }
    var ocorrenciaSelecionada by remember { mutableStateOf<Ocorrencia?>(null) }
    var desastreSelecionado by remember { mutableStateOf<DesastreDetalhado?>(null) }
    var carregandoRota by remember { mutableStateOf(false) }
    var buscandoAbrigo by remember { mutableStateOf(false) }
    var carregandoPontosSeguranca by remember { mutableStateOf(false) }
    var erroPontosSeguranca by remember { mutableStateOf<String?>(null) }
    var erroDesastres by remember { mutableStateOf<String?>(null) }
    var enderecoAtual by remember { mutableStateOf("") }
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) }

    val tileSource = remember { XYTileSource("CartoDB Voyager", 0, 19, 256, ".png", arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager/", "https://b.basemaps.cartocdn.com/rastertiles/voyager/", "https://c.basemaps.cartocdn.com/rastertiles/voyager/", "https://d.basemaps.cartocdn.com/rastertiles/voyager/"), "© OpenStreetMap contributors © CARTO") }
    val mapView = remember { try { MapView(context.applicationContext).apply { setTileSource(tileSource); setMultiTouchControls(true); isTilesScaledToDpi = true; setUseDataConnection(true); controller.setZoom(3.0); controller.setCenter(GeoPoint(-14.2350, -51.9253)) } } catch (e: Exception) { MapView(context.applicationContext) } }
    val myLocationOverlay = remember { try { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) } catch (e: Exception) { null } }

    fun buscarPontosProximos(lat: Double, lng: Double) {
        if (carregandoPontosSeguranca) return
        carregandoPontosSeguranca = true
        erroPontosSeguranca = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resposta = OverpassRepository.buscarPontosSeguranca(lat, lng, "all", 8000)
                val pontosNovos = resposta.elements.mapNotNull { elemento ->
                    val plat = elemento.lat ?: elemento.center?.lat
                    val plon = elemento.lon ?: elemento.center?.lon
                    if (plat == null || plon == null || !plat.isFinite() || !plon.isFinite() || plat !in -90.0..90.0 || plon !in -180.0..180.0) return@mapNotNull null
                    val tags = elemento.tags.orEmpty()
                    val categoria = when {
                        tags["amenity"]?.let { it in setOf("hospital", "fire_station", "police", "pharmacy", "place_of_worship") } == true -> tags["amenity"]
                        tags["amenity"] == "shelter" || tags["emergency"] == "shelter" || tags["social_facility"] == "shelter" -> "shelter"
                        else -> null
                    } ?: return@mapNotNull null
                    val tipo = NOMES_SEGURANCA[categoria] ?: categoria
                    val nome = tags["name"]?.takeIf { it.isNotBlank() } ?: "$tipo cadastrado"
                    Ocorrencia(nome, "$tipo cadastrado no OpenStreetMap; funcionamento não confirmado", plat, plon, TipoOcorrencia.SEGURANCA, categoria)
                }.distinctBy { "${it.categoria}:${it.lat}:${it.lng}" }
                withContext(Dispatchers.Main) {
                    pontosSeguranca.clear()
                    pontosSeguranca.addAll(pontosNovos)
                    carregandoPontosSeguranca = false
                    erroPontosSeguranca = if (pontosNovos.isEmpty()) "Nenhum serviço foi retornado pelo OpenStreetMap nesta área. Isso não significa que não existam serviços locais." else null
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    carregandoPontosSeguranca = false
                    erroPontosSeguranca = "Não foi possível carregar abrigos e serviços do mapa agora. Verifique a conexão e tente novamente."
                }
            }
        }
    }

    fun buscarEnderecoDigitado(query: String) {
        if (query.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultado = NominatimRepository.buscarPorNome(query)
                if (resultado.isNotEmpty()) {
                    val primeiro = resultado[0]
                    withContext(Dispatchers.Main) {
                        val ponto = GeoPoint(primeiro.lat.toDouble(), primeiro.lon.toDouble())
                        try { mapView.controller.animateTo(ponto); mapView.controller.setZoom(16.0) } catch (_: Exception) {}
                        buscarPontosProximos(primeiro.lat.toDouble(), primeiro.lon.toDouble())
                        try {
                            val resp = GdacsRepository.buscarDesastres(tipos = if (filtrosDesastres.isEmpty()) NOMES_DESASTRES.keys.joinToString(";") else filtrosDesastres.joinToString(";"))
                            desastresGlobais.clear()
                            resp.features.forEach { feature ->
                                val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0
                                val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0
                                val dist = calcularDistancia(ponto.latitude, ponto.longitude, lat, lon)
                                if (dist < 20000) {
                                    val grav = when (feature.properties.alertlevel.orEmpty()) { "Red" -> "Crítica"; "Orange" -> "Alta"; "Green" -> "Média"; else -> "Baixa" }
                                    val tipoNome = NOMES_DESASTRES[feature.properties.eventtype.orEmpty()] ?: feature.properties.eventtype.orEmpty()
                                    val nomeEvento = feature.properties.name.orEmpty().ifBlank { feature.properties.eventname.orEmpty().ifBlank { "Evento reportado" } }
                                    val detalheEvento = listOfNotNull(feature.properties.country?.takeIf { it.isNotBlank() }, feature.properties.description?.takeIf { it.isNotBlank() }, feature.properties.fromdate?.takeIf { it.isNotBlank() }?.let { "Início: $it" }).joinToString(" • ")
                                    val oc = Ocorrencia("${tipoNome}: $nomeEvento", listOf("Alerta ${feature.properties.alertlevel}", detalheEvento, String.format("%.0f km de %s", dist, query)).filter { it.isNotBlank() }.joinToString(" • "), lat, lon, TipoOcorrencia.DESASTRE, feature.properties.eventtype.orEmpty())
                                    desastresGlobais.add(DesastreDetalhado(oc, feature.properties.alertlevel.orEmpty(), grav, feature.properties.eventtype.orEmpty(), tipoNome))
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun tracarRotaAte(destino: GeoPoint, nomeDestino: String = "") {
        val origem = myLocationOverlay?.myLocation ?: localizacaoAtual
        if (origem == null) { Toast.makeText(context, "Aguardando uma localização válida para calcular a rota.", Toast.LENGTH_LONG).show(); return }
        destinoRota = destino to nomeDestino
        carregandoRota = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alternativas = OsrmRepository.buscarRotasCompletas(origem.longitude, origem.latitude, destino.longitude, destino.latitude, modoTransporte)
                val rotaCompleta = alternativas.firstOrNull() ?: throw IllegalStateException("Nenhuma rota disponível para esse modo de deslocamento.")
                withContext(Dispatchers.Main) {
                    rotasDisponiveis = alternativas
                    rotaSelecionadaIndex = 0
                    rotaPontos = rotaCompleta.pontos
                    val distKm = rotaCompleta.distanciaMetros / 1000.0
                    val durMin = (rotaCompleta.duracaoSeg / 60).toInt().coerceAtLeast(1)
                    val durTexto = if (durMin < 60) "${durMin} min" else "${durMin/60}h ${durMin%60}min"
                    val distTexto = if (distKm < 1) "${(distKm*1000).toInt()} m" else String.format("%.1f km", distKm)
                    rotaInfo = RotaInfo(distKm, durMin, durTexto, distTexto)
                    carregandoRota = false
                    try {
                        val bounding = org.osmdroid.util.BoundingBox.fromGeoPoints(rotaPontos)
                        mapView.zoomToBoundingBox(bounding, true, 120)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    carregandoRota = false
                    Toast.makeText(context, "Não foi possível desenhar a rota neste momento. Verifique a conexão ou use Iniciar navegação.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun buscarAbrigoMaisProximo() {
        val origem = myLocationOverlay?.myLocation ?: localizacaoAtual
        if (origem == null) {
            Toast.makeText(context, "Aguardando uma localização GPS válida para procurar abrigos.", Toast.LENGTH_LONG).show()
            return
        }
        if (buscandoAbrigo) return
        buscandoAbrigo = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Consulta ao vivo numa área mais ampla; o cadastro OSM não confirma disponibilidade do abrigo.
                val resposta = OverpassRepository.buscarPontosSeguranca(origem.latitude, origem.longitude, "shelter", 15000)
                val abrigo = resposta.elements.mapNotNull { elemento ->
                    val lat = elemento.lat ?: elemento.center?.lat
                    val lon = elemento.lon ?: elemento.center?.lon
                    if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) null
                    else Ocorrencia(
                        elemento.tags?.get("name") ?: "Abrigo cadastrado no OpenStreetMap",
                        "Abrigo cadastrado no OpenStreetMap; disponibilidade não confirmada",
                        lat, lon, TipoOcorrencia.SEGURANCA, "shelter"
                    )
                }.minByOrNull { calcularDistancia(origem.latitude, origem.longitude, it.lat, it.lng) }
                withContext(Dispatchers.Main) {
                    buscandoAbrigo = false
                    if (abrigo == null) {
                        Toast.makeText(context, "Nenhum abrigo cadastrado foi encontrado em até 15 km. Não será traçada rota até o desastre.", Toast.LENGTH_LONG).show()
                    } else {
                        if (pontosSeguranca.none { it.categoria == "shelter" && it.lat == abrigo.lat && it.lng == abrigo.lng }) pontosSeguranca.add(abrigo)
                        tracarRotaAte(GeoPoint(abrigo.lat, abrigo.lng), abrigo.titulo)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    buscandoAbrigo = false
                    Toast.makeText(context, "Não foi possível consultar abrigos agora. Confira a conexão e siga as orientações da Defesa Civil.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(temPermissao) {
        if (temPermissao) {
            try {
                myLocationOverlay?.enableMyLocation()
                if (mapView.overlays.none { it is MyLocationNewOverlay } && myLocationOverlay != null) mapView.overlays.add(myLocationOverlay)
                obterLocalizacao(context) { lat, lng ->
                    val ponto = GeoPoint(lat, lng)
                    localizacaoAtual = ponto
                    CoroutineScope(Dispatchers.IO).launch { try { enderecoAtual = NominatimRepository.getEndereco(lat, lng) } catch (_: Exception) {} }
                    try {
                        myLocationOverlay?.enableFollowLocation()
                        mapView.controller.animateTo(ponto)
                        mapView.controller.setZoom(16.0)
                    } catch (_: Exception) {}
                    buscarPontosProximos(lat, lng)
                }
            } catch (e: Exception) {}
        }
    }
    LaunchedEffect(filtrosDesastres) {
        erroDesastres = null
        if (filtrosDesastres.isEmpty()) { desastresGlobais.clear(); return@LaunchedEffect }
        try {
            val resposta = GdacsRepository.buscarDesastres(tipos = filtrosDesastres.joinToString(";"))
            desastresGlobais.clear()
            resposta.features.forEach { feature ->
                val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0
                val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0
                val grav = when (feature.properties.alertlevel.orEmpty()) { "Red" -> "Crítica"; "Orange" -> "Alta"; "Green" -> "Média"; else -> "Baixa" }
                val tipoNome = NOMES_DESASTRES[feature.properties.eventtype.orEmpty()] ?: feature.properties.eventtype.orEmpty()
                val nomeEvento = feature.properties.name.orEmpty().ifBlank { feature.properties.eventname.orEmpty().ifBlank { "Evento reportado" } }
                val detalheEvento = listOfNotNull(feature.properties.country?.takeIf { it.isNotBlank() }, feature.properties.description?.takeIf { it.isNotBlank() }, feature.properties.fromdate?.takeIf { it.isNotBlank() }?.let { "Início: $it" }).joinToString(" • ")
                val oc = Ocorrencia("$tipoNome: $nomeEvento", listOf("Alerta ${feature.properties.alertlevel}", detalheEvento, "Fonte: GDACS").filter { it.isNotBlank() }.joinToString(" • "), lat, lon, TipoOcorrencia.DESASTRE, feature.properties.eventtype.orEmpty())
                desastresGlobais.add(DesastreDetalhado(oc, feature.properties.alertlevel.orEmpty(), grav, feature.properties.eventtype.orEmpty(), tipoNome))
            }
        } catch (_: Exception) {
            erroDesastres = "Alertas GDACS indisponíveis no momento. Confira a conexão; a ausência de pontos não significa ausência de perigo."
        }
    }
    DisposableEffect(Unit) { try { mapView.onResume() } catch (_: Exception) {}; onDispose { try { mapView.onPause() } catch (_: Exception) {}; try { myLocationOverlay?.disableMyLocation() } catch (_: Exception) {} } }
    Box(modifier = Modifier.fillMaxSize()) {
        // O mapa ocupa apenas a área abaixo dos filtros. Assim, os gestos do OSMDroid
        // não competem com os controles Compose e o mapa não fica escondido sob as abas.
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().padding(
                top = if (rotaInfo != null) 390.dp
                    else if (erroDesastres != null || erroPontosSeguranca != null || carregandoPontosSeguranca) 315.dp
                    else 275.dp,
                bottom = 58.dp
            )
        ) { view ->
            try {
                view.overlays.removeAll { it is Marker || it is org.osmdroid.views.overlay.Polyline }
                if (view.overlays.none { it is MyLocationNewOverlay } && myLocationOverlay != null && temPermissao) view.overlays.add(myLocationOverlay)
                if (rotaPontos.isNotEmpty()) {
                    val bordaBranca = org.osmdroid.views.overlay.Polyline().apply {
                        setPoints(rotaPontos)
                        outlinePaint.color = android.graphics.Color.WHITE
                        outlinePaint.strokeWidth = 24f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    view.overlays.add(bordaBranca)
                    val linhaAzul = org.osmdroid.views.overlay.Polyline().apply {
                        setPoints(rotaPontos)
                        outlinePaint.color = android.graphics.Color.rgb(66, 133, 244)
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    view.overlays.add(linhaAzul)
                }
                val desastresFiltrados = desastresGlobais.filter { it.tipoCode in filtrosDesastres }
                desastresFiltrados.forEach { detalhe ->
                    val oc = detalhe.ocorrencia
                    val marker = Marker(view).apply {
                        position = GeoPoint(oc.lat, oc.lng); title = oc.titulo; snippet = oc.endereco
                        val px = (38 * view.context.resources.displayMetrics.density).toInt(); icon = criarPinoDesastreNivel(view.context, detalhe.alertLevel, px); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ -> ocorrenciaSelecionada = oc; desastreSelecionado = detalhe; true }
                    }; view.overlays.add(marker)
                }
                val pontosFiltrados = if (filtrosSeguranca.isEmpty()) emptyList() else pontosSeguranca.filter { it.categoria in filtrosSeguranca }
                pontosFiltrados.forEach { oc ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(oc.lat, oc.lng); title = oc.titulo; snippet = oc.endereco
                        val px = (32 * view.context.resources.displayMetrics.density).toInt()
                        val cor = when (oc.categoria) { "hospital" -> Color(0xFFE53935); "fire_station" -> Color(0xFFFB8C00); "police" -> Color(0xFF1E88E5); "pharmacy" -> Color(0xFF43A047); "shelter" -> Color(0xFF8E24AA); "place_of_worship" -> Color(0xFF5D4037); else -> SafePointGreen }
                        icon = criarPinoSeguranca(view.context, oc.categoria, cor, px)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ -> ocorrenciaSelecionada = oc; desastreSelecionado = null; true }
                    }; view.overlays.add(marker)
                }
                view.invalidate()
            } catch (_: Exception) {}
        }
        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color.White).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { LogoSafePoint(modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("SAFEPOINT GPS", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = azulEscuro) }
                Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = onVoltar, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, null, tint = azulEscuro) }
            }
            OutlinedTextField(value = buscaCidade, onValueChange = { buscaCidade = it }, placeholder = { Text("Digite cidade, bairro ou rua... (global)", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = azulEscuro, modifier = Modifier.size(20.dp)) }, trailingIcon = { if (buscaCidade.isNotBlank()) IconButton(onClick = { buscarEnderecoDigitado(buscaCidade) }) { Icon(Icons.Default.ArrowForward, null, tint = SafePointGreen) } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
            if (erroDesastres != null || erroPontosSeguranca != null || carregandoPontosSeguranca) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
                    if (carregandoPontosSeguranca) Text("Carregando abrigos e serviços próximos…", fontSize = 10.sp, color = Color(0xFF475569))
                    if (erroDesastres != null) Text(erroDesastres.orEmpty(), fontSize = 10.sp, color = Color(0xFFB45309), maxLines = 2)
                    if (erroPontosSeguranca != null) Text(erroPontosSeguranca.orEmpty(), fontSize = 10.sp, color = Color(0xFFB45309), maxLines = 2)
                }
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                item { Surface(shape = RoundedCornerShape(20.dp), color = if (filtrosDesastres.size == NOMES_DESASTRES.size) Color(0xFFE3F2FD) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosDesastres = if (filtrosDesastres.size == NOMES_DESASTRES.size) emptySet() else NOMES_DESASTRES.keys }) { Text("Todos perigos", fontSize = 11.sp, modifier = Modifier.padding(10.dp, 5.dp), fontWeight = FontWeight.Bold) } } 
                NOMES_DESASTRES.forEach { (codigo, nome) -> val ativo = codigo in filtrosDesastres; item { Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) Color(0xFFD32F2F) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosDesastres = if (ativo) filtrosDesastres - codigo else filtrosDesastres + codigo }) { Text(nome, color = if (ativo) Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp), fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal) } } } 
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                item { Surface(shape = RoundedCornerShape(20.dp), color = if (filtrosSeguranca.size == 6) Color(0xFFE8F5E9) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if (filtrosSeguranca.size == 6) emptySet() else setOf("hospital","fire_station","police","shelter","pharmacy","place_of_worship") }) { Text("Todos seguros", fontSize = 11.sp, modifier = Modifier.padding(10.dp, 5.dp), fontWeight = FontWeight.Bold) } }
                NOMES_SEGURANCA.forEach { (codigo, nome) -> val ativo = codigo in filtrosSeguranca; item { Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) Color(0xFF2E7D32) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if (ativo) filtrosSeguranca - codigo else filtrosSeguranca + codigo }) { Text(nome, color = if (ativo) Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp)) } } } 
                item { Surface(shape = RoundedCornerShape(20.dp), color = if ("place_of_worship" in filtrosSeguranca) Color(0xFF5D4037) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if ("place_of_worship" in filtrosSeguranca) filtrosSeguranca - "place_of_worship" else filtrosSeguranca + "place_of_worship" }) { Text("Igrejas/Religião", color = if ("place_of_worship" in filtrosSeguranca) Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp)) } } 
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = modoTransporte == "carro", onClick = { modoTransporte = "carro"; destinoRota?.let { tracarRotaAte(it.first, it.second) } }, label = { Text("🚗 Carro", fontSize = 11.sp) }) }
                item { FilterChip(selected = modoTransporte == "bicicleta", onClick = { modoTransporte = "bicicleta"; destinoRota?.let { tracarRotaAte(it.first, it.second) } }, label = { Text("🚲 Bicicleta", fontSize = 11.sp) }) }
                item { FilterChip(selected = modoTransporte == "a_pe", onClick = { modoTransporte = "a_pe"; destinoRota?.let { tracarRotaAte(it.first, it.second) } }, label = { Text("🚶 A pé", fontSize = 11.sp) }) }
            }
            if (rotaInfo != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(12.dp, 6.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${when (modoTransporte) { "a_pe" -> "A pé"; "bicicleta" -> "Bicicleta"; else -> "Carro" }} • rota estimada", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${rotaInfo!!.distanciaTexto} • ${rotaInfo!!.duracaoTexto}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Não confirma vias livres ou segurança do abrigo", color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp) }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Navigation, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("GPS", color = Color.White, fontSize = 11.sp) }
                            TextButton(onClick = {
                                val origem = myLocationOverlay?.myLocation ?: localizacaoAtual
                                val destino = destinoRota?.first
                                if (origem != null && destino != null) {
                                    val modoGoogle = when (modoTransporte) { "a_pe" -> "walking"; "bicicleta" -> "bicycling"; else -> "driving" }
                                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${origem.latitude},${origem.longitude}&destination=${destino.latitude},${destino.longitude}&travelmode=$modoGoogle&dir_action=navigate")
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { Toast.makeText(context, "Não foi possível abrir o aplicativo de navegação.", Toast.LENGTH_LONG).show() }
                                } else Toast.makeText(context, "Origem ou destino indisponível.", Toast.LENGTH_SHORT).show()
                            }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("Iniciar navegação", color = Color.White, fontSize = 10.sp) }
                        }
                    }
                }
                if (rotasDisponiveis.size > 1) {
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rotasDisponiveis.size) { index ->
                            val alternativa = rotasDisponiveis[index]
                            val distancia = alternativa.distanciaMetros / 1000.0
                            val minutos = (alternativa.duracaoSeg / 60).toInt().coerceAtLeast(1)
                            FilterChip(
                                selected = rotaSelecionadaIndex == index,
                                onClick = {
                                    rotaSelecionadaIndex = index
                                    rotaPontos = alternativa.pontos
                                    val textoDuracao = if (minutos < 60) "$minutos min" else "${minutos / 60}h ${minutos % 60}min"
                                    val textoDistancia = if (distancia < 1.0) "${(distancia * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", distancia)
                                    rotaInfo = RotaInfo(distancia, minutos, textoDuracao, textoDistancia)
                                },
                                label = { Text("Rota ${index + 1} • ${if (distancia < 1.0) "${(distancia * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", distancia)}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
        if (!temPermissao) {
            Card(modifier = Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, null, tint = Color.Red, modifier = Modifier.size(48.dp)); Spacer(modifier = Modifier.height(12.dp))
                    Text("Mapa precisa de localização", fontWeight = FontWeight.Bold); Text("Para usar o mapa como GPS profissional com rotas, tempo de deslocamento e alertas reais globais, permita a localização uma única vez. Seu consentimento será lembrado.", fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp)); Button(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Abrir configurações") }; Spacer(modifier = Modifier.height(8.dp)); TextButton(onClick = onVoltar) { Text("Voltar") }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 120.dp).size(56.dp).clip(CircleShape).background(Color.White).clickable { if (!temPermissao) { context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } else { val minha = myLocationOverlay?.myLocation; if (minha != null) { myLocationOverlay?.enableFollowLocation(); mapView.controller.animateTo(minha); mapView.controller.setZoom(18.0) } else { obterLocalizacao(context) { lat, lng -> val p = GeoPoint(lat, lng); localizacaoAtual = p; mapView.controller.animateTo(p); mapView.controller.setZoom(18.0) } } } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.MyLocation, null, tint = SafePointBlue, modifier = Modifier.size(28.dp)) }
        if (rotaPontos.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 190.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).clickable { rotaPontos = emptyList(); rotaInfo = null; destinoRota = null; rotasDisponiveis = emptyList(); rotaSelecionadaIndex = 0 }.padding(12.dp, 8.dp)) { Text("Limpar rota", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold) }
        }
        ocorrenciaSelecionada?.let { oc ->
            val isDesastre = oc.tipo == TipoOcorrencia.DESASTRE
            val distanciaUser = localizacaoAtual?.let { calcularDistancia(it.latitude, it.longitude, oc.lat, oc.lng) } ?: 0.0
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp, 90.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(oc.titulo, fontWeight = FontWeight.Bold, color = azulEscuro, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (isDesastre && desastreSelecionado != null) {
                                    val corNivel = when (desastreSelecionado!!.alertLevel) { "Red" -> Color(0xFFD32F2F); "Orange" -> Color(0xFFF57C00); else -> Color(0xFF388E3C) }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(corNivel).padding(6.dp, 3.dp)) { Text(desastreSelecionado!!.gravidade, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Text(oc.endereco, fontSize = 12.sp, color = Color.Gray)
                            if (distanciaUser > 0) Text(String.format(Locale("pt", "BR"), "Distância em linha reta: %.1f km", distanciaUser), fontSize = 11.sp, color = SafePointBlue, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { ocorrenciaSelecionada = null; desastreSelecionado = null }) { Icon(Icons.Default.Close, null) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isDesastre && desastreSelecionado != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("⚠️ Evento reportado pela fonte:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE65100))
                                Text("Evento reportado pela GDACS. Local: ${oc.titulo}. Distância aproximada: ${String.format(Locale("pt", "BR"), "%.1f km", distanciaUser)}. Não navegue até o ponto do desastre. O mapa não confirma bloqueios ou condições atuais das vias; consulte a Defesa Civil e selecione um abrigo cadastrado quando disponível.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Text("Ponto cadastrado no OpenStreetMap. O cadastro não confirma que o local está aberto, operacional ou seguro neste momento. O trajeto calculado não verifica automaticamente vias bloqueadas.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (isDesastre) {
                                buscarAbrigoMaisProximo()
                            } else tracarRotaAte(GeoPoint(oc.lat, oc.lng), oc.titulo)
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SafePointBlue)) {
                            if (carregandoRota || (isDesastre && buscandoAbrigo)) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else { Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(if (isDesastre) "Buscar abrigo próximo" else "Traçar trajeto") }
                        }
                        OutlinedButton(onClick = { val uri = Uri.parse("https://www.openstreetmap.org/?mlat=${oc.lat}&mlon=${oc.lng}#map=12/${oc.lat}/${oc.lng}"); context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }) { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ItemNavegacao("Início", Icons.Default.Home, false) { navController.navigate(Screen.Home.route) { launchSingleTop = true } }
            ItemNavegacao("Mapa", Icons.Default.Map, true) { }
            ItemNavegacao("Histórico", Icons.Default.DateRange, false) { navController.navigate(Screen.Historico.route) { launchSingleTop = true } }
            ItemNavegacao("Perfil", Icons.Default.Person, false) { navController.navigate(Screen.Perfil.route) { launchSingleTop = true } }
        }
    }
}



fun criarPinoDesastreNivel(context: Context, nivelAlerta: String, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val h = (w * 1.35f).toInt(); val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val canvas = Canvas(bmp)
    val cor = when (nivelAlerta) { "Red" -> android.graphics.Color.rgb(211,47,47); "Orange" -> android.graphics.Color.rgb(245,124,0); else -> android.graphics.Color.rgb(56,142,60) }
    val paint = android.graphics.Paint().apply { color = cor; isAntiAlias = true; style = android.graphics.Paint.Style.FILL }
    val paintBorda = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = w*0.08f }
    val path = android.graphics.Path()
    path.moveTo(w/2f, 0f)
    path.cubicTo(w*0.95f, 0f, w.toFloat(), h*0.25f, w.toFloat(), h*0.38f)
    path.cubicTo(w.toFloat(), h*0.62f, w*0.75f, h*0.78f, w/2f, h.toFloat())
    path.cubicTo(w*0.25f, h*0.78f, 0f, h*0.62f, 0f, h*0.38f)
    path.cubicTo(0f, h*0.25f, w*0.05f, 0f, w/2f, 0f)
    path.close()
    canvas.drawPath(path, paint)
    canvas.drawPath(path, paintBorda)
    val texto = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = w*0.55f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    canvas.drawText("!", w/2f, h*0.55f, texto)
    return BitmapDrawable(context.resources, bmp)
}

fun criarPinoDesastre(context: Context, tamanhoPx: Int): android.graphics.drawable.Drawable {
    return criarPinoDesastreNivel(context, "Red", tamanhoPx)
}
fun criarPinoSeguranca(context: Context, categoria: String?, corFundo: Color, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888); val canvas = Canvas(bmp); val cx = w / 2f; val cy = w / 2f; val raio = w / 2f - 4f; val sombra = android.graphics.Paint().apply { color = android.graphics.Color.argb(80, 0, 0, 0); isAntiAlias = true }; canvas.drawCircle(cx, cy + 3f, raio, sombra)
    val circ = android.graphics.Paint().apply { color = android.graphics.Color.argb((corFundo.alpha * 255).toInt(), (corFundo.red * 255).toInt(), (corFundo.green * 255).toInt(), (corFundo.blue * 255).toInt()); isAntiAlias = true }; canvas.drawCircle(cx, cy, raio, circ)
    val borda = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = w * 0.07f; isAntiAlias = true }; canvas.drawCircle(cx, cy, raio - (w * 0.035f), borda)
    val texto = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = w * 0.55f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; isFakeBoldText = true }; val letra = when (categoria) { "hospital" -> "H"; "fire_station" -> "B"; "police" -> "P"; "shelter" -> "A"; "pharmacy" -> "F"; "place_of_worship" -> "R"; else -> "S" }; canvas.drawText(letra, cx, cy - (texto.descent() + texto.ascent()) / 2f, texto); return BitmapDrawable(context.resources, bmp)
}

@Composable fun ItemNavegacao(label: String, icon: ImageVector, ativo: Boolean, onClick: () -> Unit) { val cor = if (ativo) SafePointBlue else Color.Gray; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(12.dp, 4.dp)) { Icon(icon, null, tint = cor, modifier = Modifier.size(24.dp)); Text(label, fontSize = 11.sp, color = cor) } }

data class GdacsResponse(val features: List<GdacsFeature>); data class GdacsFeature(val properties: GdacsProperties, val geometry: GdacsGeometry); data class GdacsProperties(val eventtype: String? = null, val name: String? = null, val alertlevel: String? = null, val pubDate: String? = null, val eventid: String? = null, val fromdate: String? = null, val todate: String? = null, val datemodified: String? = null, val country: String? = null, val description: String? = null, val eventname: String? = null); data class GdacsGeometry(val coordinates: List<Double>)
interface GdacsApi { @retrofit2.http.GET("gdacsapi/api/events/geteventlist/SEARCH") suspend fun getDesastres(@retrofit2.http.Query("eventlist") tipos: String = "EQ;TC;FL;VO;DR;WF;TS", @retrofit2.http.Query("alertlevel") nivel: String = "green;orange;red", @retrofit2.http.Query("fromdate") fromdate: String? = null, @retrofit2.http.Query("todate") todate: String? = null, @retrofit2.http.Query("pageSize") pageSize: Int = 100, @retrofit2.http.Query("pageNumber") pageNumber: Int = 1): GdacsResponse }
object GdacsRepository { private val api = retrofit2.Retrofit.Builder().baseUrl("https://www.gdacs.org/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(GdacsApi::class.java); suspend fun buscarDesastres(tipos: String = "EQ;TC;FL;VO;DR;WF;TS", fromdate: String? = null, todate: String? = null): GdacsResponse = api.getDesastres(tipos = tipos, fromdate = fromdate, todate = todate) }
data class OverpassResponse(val elements: List<OverpassElement>); data class OverpassElement(val type: String, val id: Long, val lat: Double? = null, val lon: Double? = null, val tags: Map<String, String>? = null, val center: OverpassCenter? = null); data class OverpassCenter(val lat: Double, val lon: Double)
interface OverpassApi { @retrofit2.http.GET("api/interpreter") suspend fun buscarPontos(@retrofit2.http.Query("data") query: String): OverpassResponse }
object OverpassRepository {
    private val httpClient = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent", "SafePoint/1.0 (contact: gu45363@gmail.com)").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://overpass-api.de/").client(httpClient).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OverpassApi::class.java)
    suspend fun buscarPontosSeguranca(lat: Double, lng: Double, categoria: String, raioMetros: Int = 5000): OverpassResponse {
        val query = if (categoria == "all") {
            """[out:json][timeout:25];(node["amenity"~"^(hospital|fire_station|police|pharmacy|place_of_worship|shelter)$"](around:$raioMetros,$lat,$lng);way["amenity"~"^(hospital|fire_station|police|pharmacy|place_of_worship|shelter)$"](around:$raioMetros,$lat,$lng);node["emergency"="shelter"](around:$raioMetros,$lat,$lng);way["emergency"="shelter"](around:$raioMetros,$lat,$lng);node["social_facility"="shelter"](around:$raioMetros,$lat,$lng);way["social_facility"="shelter"](around:$raioMetros,$lat,$lng););out center;""".trimIndent()
        } else if (categoria == "shelter") {
            """[out:json][timeout:25];(node["amenity"="shelter"](around:$raioMetros,$lat,$lng);way["amenity"="shelter"](around:$raioMetros,$lat,$lng);node["emergency"="shelter"](around:$raioMetros,$lat,$lng);way["emergency"="shelter"](around:$raioMetros,$lat,$lng);node["social_facility"="shelter"](around:$raioMetros,$lat,$lng);way["social_facility"="shelter"](around:$raioMetros,$lat,$lng););out center;""".trimIndent()
        } else {
            """[out:json][timeout:25];(node["amenity"="$categoria"](around:$raioMetros,$lat,$lng);way["amenity"="$categoria"](around:$raioMetros,$lat,$lng););out center;""".trimIndent()
        }
        return api.buscarPontos(query)
    }
}
data class NominatimResponse(val display_name: String, val address: Map<String, String>?); interface NominatimApi { @retrofit2.http.GET("reverse") suspend fun reverse(@retrofit2.http.Query("lat") lat: Double, @retrofit2.http.Query("lon") lon: Double, @retrofit2.http.Query("format") format: String = "jsonv2"): NominatimResponse }

data class NominatimSearchResult(val lat: String, val lon: String, val display_name: String)
interface NominatimSearchApi {
    @retrofit2.http.GET("search")
    suspend fun search(@retrofit2.http.Query("q") query: String, @retrofit2.http.Query("format") format: String = "json", @retrofit2.http.Query("limit") limit: Int = 5): List<NominatimSearchResult>
}
object NominatimRepository {
    private val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent", "SafePoint/1.0 (contact: gu45363@gmail.com)").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://nominatim.openstreetmap.org/").client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(NominatimApi::class.java)
    private val searchApi = retrofit2.Retrofit.Builder().baseUrl("https://nominatim.openstreetmap.org/").client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(NominatimSearchApi::class.java)
    suspend fun getEndereco(lat: Double, lng: Double): String { return try { api.reverse(lat, lng).display_name } catch (e: Exception) { "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}" } }
    suspend fun buscarPorNome(query: String): List<NominatimSearchResult> { return try { searchApi.search(query) } catch (e: Exception) { emptyList() } }
}

data class OsrmResponse(val routes: List<OsrmRoute>); data class OsrmRoute(val geometry: OsrmGeometry, val distance: Double, val duration: Double); data class OsrmGeometry(val coordinates: List<List<Double>>)
data class RotaCompleta(val pontos: List<GeoPoint>, val distanciaMetros: Double, val duracaoSeg: Double)
interface OsrmApi { @retrofit2.http.GET("{service}/route/v1/driving/{coords}") suspend fun getRoute(@retrofit2.http.Path("service", encoded = true) service: String, @retrofit2.http.Path("coords", encoded = true) coords: String, @retrofit2.http.Query("overview") overview: String = "full", @retrofit2.http.Query("geometries") geometries: String = "geojson", @retrofit2.http.Query("alternatives") alternatives: Boolean = true, @retrofit2.http.Query("steps") steps: Boolean = true): OsrmResponse }
object OsrmRepository {
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://routing.openstreetmap.de/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OsrmApi::class.java)
    suspend fun buscarRota(lon1: Double, lat1: Double, lon2: Double, lat2: Double): List<GeoPoint> {
        return buscarRotaCompleta(lon1, lat1, lon2, lat2).pontos
    }
    suspend fun buscarRotasCompletas(lon1: Double, lat1: Double, lon2: Double, lat2: Double, modo: String = "carro"): List<RotaCompleta> {
        require(lon1 in -180.0..180.0 && lon2 in -180.0..180.0 && lat1 in -90.0..90.0 && lat2 in -90.0..90.0) { "Coordenadas inválidas" }
        val coords = "$lon1,$lat1;$lon2,$lat2"
        val service = when (modo) { "bicicleta" -> "routed-bike"; "a_pe" -> "routed-foot"; else -> "routed-car" }
        val resp = api.getRoute(service, coords)
        return resp.routes.mapNotNull { route ->
            val pontos = route.geometry.coordinates.mapNotNull { coordenada ->
                if (coordenada.size >= 2 && coordenada[0].isFinite() && coordenada[1].isFinite() && coordenada[0] in -180.0..180.0 && coordenada[1] in -90.0..90.0) GeoPoint(coordenada[1], coordenada[0]) else null
            }
            if (pontos.size >= 2) RotaCompleta(pontos, route.distance, route.duration) else null
        }
    }
    suspend fun buscarRotaCompleta(lon1: Double, lat1: Double, lon2: Double, lat2: Double, modo: String = "carro"): RotaCompleta {
        return buscarRotasCompletas(lon1, lat1, lon2, lat2, modo).firstOrNull() ?: RotaCompleta(emptyList(), 0.0, 0.0)
    }
}



// AdicionarDesastreDialog removido conforme solicitado - mapa agora é GPS profissional



// HISTÓRICO REAL - 100% LOCALIZAÇÃO REAL + GDACS REAL - NADA FICTÍCIO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartaoResumoHistorico(titulo: String, valor: Int, cor: Color, carregandoSemDados: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(titulo, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
            Text(if (carregandoSemDados) "—" else valor.toString(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = cor)
            Text("dados recebidos", fontSize = 9.sp, color = Color(0xFF94A3B8), maxLines = 1)
        }
    }
}

@Composable fun TelaHistoricoReal(navController: NavController) {
    val context = LocalContext.current
    var buscaCidade by remember { mutableStateOf("") }
    var filtroGravidade by remember { mutableStateOf("Todos") }
    var filtroTipo by remember { mutableStateOf("Todos") }
    var alertasReais by remember { mutableStateOf<List<AlertaRota>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erroFonte by remember { mutableStateOf<String?>(null) }
    var cidadeAtual by remember { mutableStateOf("Sua localização") }
    var enderecoAtualReal by remember { mutableStateOf("") }
    var localizacaoAtual by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var buscaEmAndamento by remember { mutableStateOf(false) }
    var ultimaAtualizacao by remember { mutableStateOf("") }

    fun carregarAlertas(lat: Double, lng: Double, cidadeNome: String? = null) {
        carregando = true
        erroFonte = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enderecoReal = try { NominatimRepository.getEndereco(lat, lng) } catch (_: Exception) { "" }
                val partes = enderecoReal.split(",")
                val cidade = cidadeNome ?: partes.getOrNull(partes.size - 3)?.trim()?.takeIf { it.isNotBlank() }
                    ?: partes.getOrNull(partes.size - 4)?.trim()?.takeIf { it.isNotBlank() } ?: "Região consultada"
                val formatoApi = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val hojeUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val dataFim = formatoApi.format(hojeUtc.time)
                hojeUtc.add(Calendar.MONTH, -6)
                val dataInicio = formatoApi.format(hojeUtc.time)
                val gdacs = GdacsRepository.buscarDesastres(fromdate = dataInicio, todate = dataFim)
                val listaReal = mutableListOf<AlertaRota>()
                gdacs.features.forEach { feature ->
                    val fLon = feature.geometry.coordinates.getOrNull(0) ?: return@forEach
                    val fLat = feature.geometry.coordinates.getOrNull(1) ?: return@forEach
                    if (!fLat.isFinite() || !fLon.isFinite() || fLat !in -90.0..90.0 || fLon !in -180.0..180.0) return@forEach
                    val dist = calcularDistancia(lat, lng, fLat, fLon)
                    val grav = when (feature.properties.alertlevel.orEmpty()) { "Red" -> "Crítica"; "Orange" -> "Alta"; "Green" -> "Média"; else -> "Não classificada" }
                    val tipo = NOMES_DESASTRES[feature.properties.eventtype.orEmpty()] ?: feature.properties.eventtype.orEmpty()
                    val nomeEvento = feature.properties.name.orEmpty().ifBlank { feature.properties.eventname.orEmpty().ifBlank { "Evento reportado" } }
                    val local = listOfNotNull(nomeEvento, feature.properties.country?.takeIf { it.isNotBlank() }, feature.properties.description?.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "Coordenadas ${String.format(Locale.US, "%.4f, %.4f", fLat, fLon)}" }
                    val dataFonte = formatarDataAlerta(feature.properties.fromdate ?: feature.properties.pubDate)
                    listaReal.add(AlertaRota(
                        id = "gdacs-${feature.properties.eventid ?: "${feature.properties.eventtype.orEmpty()}-${fLat}-${fLon}"}",
                        titulo = "$tipo: $nomeEvento",
                        dataHora = dataFonte,
                        localizacao = local,
                        gravidade = grav,
                        tipo = tipo,
                        latitude = fLat,
                        longitude = fLon,
                        distancia = dist,
                        bairro = feature.properties.name.orEmpty(),
                        cidade = cidade,
                        regiao = local
                    ))
                }
                listaReal.sortBy { it.distancia }
                withContext(Dispatchers.Main) {
                    alertasReais = listaReal.take(100)
                    cidadeAtual = cidade
                    enderecoAtualReal = enderecoReal
                    ultimaAtualizacao = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
                    carregando = false
                    buscaEmAndamento = false
                    erroFonte = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    carregando = false
                    buscaEmAndamento = false
                    erroFonte = "Não foi possível consultar a fonte GDACS agora. Verifique a conexão e tente atualizar. Nenhum alerta foi inventado."
                }
            }
        }
    }

    fun buscarPorCidadeDigitada(query: String) {
        if (query.isBlank()) return
        buscaEmAndamento = true
        carregando = true
        erroFonte = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultados = NominatimRepository.buscarPorNome(query)
                val primeiro = resultados.firstOrNull()
                if (primeiro != null) {
                    val lat = primeiro.lat.toDoubleOrNull()
                    val lng = primeiro.lon.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        withContext(Dispatchers.Main) {
                            localizacaoAtual = Pair(lat, lng)
                            carregarAlertas(lat, lng, primeiro.display_name.split(",").firstOrNull() ?: query)
                        }
                    } else withContext(Dispatchers.Main) { carregando = false; buscaEmAndamento = false; erroFonte = "A localização encontrada não possui coordenadas válidas." }
                } else withContext(Dispatchers.Main) { carregando = false; buscaEmAndamento = false; erroFonte = "Não encontramos esse local. Tente informar cidade e país." }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { carregando = false; buscaEmAndamento = false; erroFonte = "Falha ao pesquisar o local. Tente novamente." }
            }
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            obterLocalizacao(context) { lat, lng ->
                localizacaoAtual = Pair(lat, lng)
                carregarAlertas(lat, lng)
            }
            delay(2500)
            if (localizacaoAtual == null && carregando) {
                carregando = false
                erroFonte = "Não há uma localização recente disponível. Ative a localização do aparelho ou pesquise uma cidade. O aplicativo não usará uma coordenada fictícia."
            }
        } else {
            carregando = false
            erroFonte = "A permissão de localização não está ativa. Você pode pesquisar uma cidade manualmente sem conceder acesso ao GPS."
        }
    }

    val opcoesGravidade = listOf("Todos", "Crítica", "Alta", "Média", "Não classificada")
    val opcoesTipo = listOf("Todos", "Terremoto", "Ciclone", "Enchente", "Vulcão", "Seca", "Incêndio", "Tsunami")
    val alertasFiltrados = alertasReais.filter { a ->
        (filtroGravidade == "Todos" || a.gravidade == filtroGravidade) &&
            (filtroTipo == "Todos" || a.tipo.contains(filtroTipo, ignoreCase = true))
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF5F7FA))) {
            Column(modifier = Modifier.fillMaxWidth().background(FundoGradiente).statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Histórico de alertas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Eventos publicados pela GDACS • últimos 6 meses", fontSize = 12.sp, color = Color.White.copy(alpha = 0.88f))
                    }
                    IconButton(onClick = { val loc = localizacaoAtual; if (loc != null) carregarAlertas(loc.first, loc.second) else if (buscaCidade.isNotBlank()) buscarPorCidadeDigitada(buscaCidade) }) {
                        Icon(Icons.Default.Refresh, "Atualizar alertas", tint = Color.White)
                    }
                }
                if (enderecoAtualReal.isNotBlank()) Text(enderecoAtualReal.take(100), fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f), maxLines = 2)
                if (ultimaAtualizacao.isNotBlank()) Text("Consulta realizada: $ultimaAtualizacao", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
            }
            // Resumo calculado exclusivamente a partir dos eventos recebidos da fonte real.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalEventos = alertasReais.size
                val totalCriticos = alertasReais.count { it.gravidade == "Crítica" }
                val totalAltos = alertasReais.count { it.gravidade == "Alta" }
                CartaoResumoHistorico("Eventos", totalEventos, azulEscuro, carregando && alertasReais.isEmpty(), Modifier.weight(1f))
                CartaoResumoHistorico("Críticos", totalCriticos, Color(0xFFD32F2F), carregando && alertasReais.isEmpty(), Modifier.weight(1f))
                CartaoResumoHistorico("Altos", totalAltos, Color(0xFFEF6C00), carregando && alertasReais.isEmpty(), Modifier.weight(1f))
            }
            OutlinedTextField(
                value = buscaCidade, onValueChange = { buscaCidade = it },
                placeholder = { Text("Pesquisar cidade ou região", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = azulEscuro) },
                trailingIcon = { IconButton(onClick = { buscarPorCidadeDigitada(buscaCidade) }, enabled = buscaCidade.isNotBlank() && !carregando) { Icon(Icons.Default.ArrowForward, null, tint = SafePointGreen) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Text("Gravidade", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(opcoesGravidade) { filtro -> FilterChip(selected = filtroGravidade == filtro, onClick = { filtroGravidade = filtro }, label = { Text(filtro, fontSize = 11.sp) }) }
            }
            Text("Tipo de evento", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(opcoesTipo) { filtro -> FilterChip(selected = filtroTipo == filtro, onClick = { filtroTipo = filtro }, label = { Text(filtro, fontSize = 11.sp) }) }
            }
            when {
                carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                        CircularProgressIndicator(color = azulEscuro)
                        Spacer(Modifier.height(12.dp))
                        Text("Consultando alertas publicados pela GDACS…", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
                erroFonte != null && alertasReais.isEmpty() -> Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Dados indisponíveis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = azulEscuro)
                        Spacer(Modifier.height(8.dp))
                        Text(erroFonte.orEmpty(), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { val loc = localizacaoAtual; if (loc != null) carregarAlertas(loc.first, loc.second) else if (buscaCidade.isNotBlank()) buscarPorCidadeDigitada(buscaCidade) }, enabled = !buscaEmAndamento) { Text("Tentar novamente") }
                    }
                }
                alertasFiltrados.isEmpty() -> Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterList, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (alertasReais.isEmpty()) "Nenhum evento retornado pela fonte" else "Nenhum evento corresponde aos filtros", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = azulEscuro, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(if (alertasReais.isEmpty()) "A consulta foi concluída sem eventos disponíveis para exibir. Isso não garante ausência de perigo local. Consulte também a Defesa Civil." else "Altere os filtros para ver outros eventos disponíveis.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(alertasFiltrados, key = { it.id }) { alerta ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable {
                            val uri = Uri.parse("https://www.openstreetmap.org/?mlat=${alerta.latitude}&mlon=${alerta.longitude}#map=12/${alerta.latitude}/${alerta.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = when (alerta.gravidade) { "Crítica" -> Color(0xFFD32F2F); "Alta" -> Color(0xFFEF6C00); "Média" -> Color(0xFFF9A825); else -> Color(0xFF64748B) }, modifier = Modifier.size(20.dp))
                                    Text(alerta.titulo, fontWeight = FontWeight.Bold, color = azulEscuro, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    val cor = when (alerta.gravidade) { "Crítica" -> Color(0xFFD32F2F); "Alta" -> Color(0xFFEF6C00); "Média" -> Color(0xFFF9A825); else -> Color(0xFF64748B) }
                                    Surface(shape = RoundedCornerShape(8.dp), color = cor) { Text(alerta.gravidade, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                                }
                                Text(alerta.localizacao, fontSize = 12.sp, color = Color(0xFF475569))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NearMe, null, tint = SafePointBlue, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(String.format(Locale("pt", "BR"), "%.1f km da localização consultada", alerta.distancia), fontSize = 11.sp, color = SafePointBlue, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Data do evento: ${alerta.dataHora}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Text("Fonte: GDACS • consulta global limitada a até 100 resultados; não é um arquivo completo de todos os eventos", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatarDataAlerta(dataFonte: String?): String {
    if (dataFonte.isNullOrBlank()) return "Data de publicação não informada pela fonte"
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
    for (pattern in patterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
            val parsed = parser.parse(dataFonte) ?: continue
            return SimpleDateFormat("dd/MM/yyyy HH:mm 'UTC'", Locale("pt", "BR")).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(parsed)
        } catch (_: Exception) { }
    }
    return dataFonte
}

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double { val R = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1); val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2); val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); return R * c }



fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager; val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return
    try {
        val gps = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val net = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val best = listOfNotNull(gps, net).maxByOrNull { it.time }
        if (best != null) {
            onLocationReceived(best.latitude, best.longitude)
        } else {
            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider != null) {
                lm.requestSingleUpdate(provider, object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        onLocationReceived(location.latitude, location.longitude)
                    }
                    @Deprecated("Deprecated by Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
                    override fun onProviderEnabled(provider: String) { }
                    override fun onProviderDisabled(provider: String) { }
                }, Looper.getMainLooper())
            }
        }
    } catch (_: Exception) {
        // Sem coordenadas disponíveis: não inventar uma posição de fallback.
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TelaPerfil(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    val context = LocalContext.current; var nome by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var telefone by remember { mutableStateOf("") }; var dataNascimento by remember { mutableStateOf("") }; var fotoUriLocal by remember { mutableStateOf<Uri?>(null) }; var fotoUrlSalva by remember { mutableStateOf<String?>(null) }; var mostrarDatePicker by remember { mutableStateOf(false) }; var salvando by remember { mutableStateOf(false) }; var carregando by remember { mutableStateOf(true) }
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { fotoUriLocal = it } }
    LaunchedEffect(Unit) { val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid; if (userId != null) db.collection("usuarios").document(userId).get().addOnSuccessListener { doc -> nome = doc.getString("nome") ?: ""; email = doc.getString("email") ?: auth.currentUser?.email ?: ""; telefone = doc.getString("telefone") ?: ""; dataNascimento = doc.getString("dataNascimento") ?: ""; fotoUrlSalva = doc.getString("fotoUrl"); carregando = false }.addOnFailureListener { carregando = false } else carregando = false }
    if (mostrarDatePicker) { val datePickerState = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { mostrarDatePicker = false }, confirmButton = { TextButton(onClick = { val millis = datePickerState.selectedDateMillis; if (millis != null) { val cal = Calendar.getInstance(); cal.timeInMillis = millis; dataNascimento = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)) }; mostrarDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }) { DatePicker(state = datePickerState) } }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        if (carregando) Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = azulEscuro) }
        else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).statusBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                            val img = fotoUriLocal?.toString() ?: fotoUrlSalva; if (!img.isNullOrBlank()) AsyncImage(model = img, contentDescription = "Foto", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Icon(Icons.Default.Person, null, tint = azulEscuro, modifier = Modifier.size(70.dp))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(azulLinhas), contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(modifier = Modifier.height(14.dp)); Text(if (nome.isEmpty()) "Usuário" else nome, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f)); Text("Toque na foto para alterar", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Editar Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro)
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, leadingIcon = { IconeUsuario(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = dataNascimento, onValueChange = {}, label = { Text("Data de nascimento") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = azulEscuro) }, trailingIcon = { IconButton(onClick = { mostrarDatePicker = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth().clickable { mostrarDatePicker = true }, shape = RoundedCornerShape(12.dp), singleLine = true, readOnly = true)
                    BotaoGradiente(texto = if (salvando) "Salvando..." else "Salvar Alterações") {
                        if (salvando) return@BotaoGradiente; salvando = true; val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid
                        if (userId != null) {
                            val perfilMap = hashMapOf<String, Any>("nome" to nome, "email" to email, "telefone" to telefone, "dataNascimento" to dataNascimento)
                            db.collection("usuarios").document(userId).set(perfilMap, SetOptions.merge()).addOnSuccessListener {
                                if (fotoUriLocal != null) {
                                    try {
                                        val storage = FirebaseStorage.getInstance(); val storageRef = storage.reference.child("fotos_perfil/$userId/profile.jpg")
                                        storageRef.putFile(fotoUriLocal!!).continueWithTask { task -> if (!task.isSuccessful) throw task.exception!!; storageRef.downloadUrl }.addOnCompleteListener { task ->
                                            if (task.isSuccessful) { val url = task.result.toString(); db.collection("usuarios").document(userId).set(mapOf("fotoUrl" to url), SetOptions.merge()).addOnSuccessListener { fotoUrlSalva = url; fotoUriLocal = null; Toast.makeText(context, "Perfil e foto atualizados!", Toast.LENGTH_SHORT).show(); salvando = false }.addOnFailureListener { Toast.makeText(context, "Foto enviada mas erro ao salvar URL.", Toast.LENGTH_LONG).show(); salvando = false } }
                                            else { Toast.makeText(context, "Storage não configurado. Salvando só dados de texto.", Toast.LENGTH_LONG).show(); salvando = false }
                                        }
                                    } catch (e: Exception) { Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show(); salvando = false }
                                } else { Toast.makeText(context, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show(); salvando = false }
                            }.addOnFailureListener { e -> Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show(); salvando = false }
                        } else { Toast.makeText(context, "Usuário não encontrado", Toast.LENGTH_SHORT).show(); salvando = false }
                    }
                    Button(onClick = { FirebaseAuth.getInstance().signOut(); PrefsHelper.clearUserId(context); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Sair", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable fun TelaSOS(onVoltar: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFB71C1C)).statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)) { 
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) { 
                IconButton(onClick = onVoltar, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp)); Column { Text("Emergência SOS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Ação rápida para salvar vidas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f)) } 
            } 
        }
        Column(modifier = Modifier.padding(20.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BotaoEmergencia(numero = "190", nome = "Polícia Militar", cor = Color(0xFF1565C0), icon = Icons.Default.LocalPolice, context = context)
            BotaoEmergencia(numero = "192", nome = "SAMU - Ambulância", cor = Color(0xFF2E7D32), icon = Icons.Default.MedicalServices, context = context)
            BotaoEmergencia(numero = "193", nome = "Corpo de Bombeiros", cor = Color(0xFFD32F2F), icon = Icons.Default.FireTruck, context = context)
            BotaoEmergencia(numero = "199", nome = "Defesa Civil", cor = Color(0xFFF57C00), icon = Icons.Default.Shield, context = context)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compartilhar minha localização", fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(8.dp)); Text("Envie sua localização exata para contatos de emergência via WhatsApp", fontSize = 12.sp, color = Color.Gray); Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { obterLocalizacao(context) { lat, lng -> val uri = Uri.parse("https://wa.me/?text=🚨 SOS SafePoint - Preciso de ajuda! Minha localização: https://maps.google.com/?q=$lat,$lng"); val intent = Intent(Intent.ACTION_VIEW, uri); context.startActivity(intent) } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) { Icon(Icons.Default.Share, null); Spacer(modifier = Modifier.width(8.dp)); Text("Compartilhar no WhatsApp", color = Color.White) }
                }
            }
        }
    }
}
@Composable fun BotaoEmergencia(numero: String, nome: String, cor: Color, icon: ImageVector, context: Context) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth().clickable { val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")); context.startActivity(intent) }) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(cor), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }; Spacer(modifier = Modifier.width(14.dp)); Column(modifier = Modifier.weight(1f)) { Text(nome, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Ligar $numero", fontSize = 12.sp, color = cor, fontWeight = FontWeight.Bold) }; Icon(Icons.Default.Call, null, tint = cor) }
    }
}

// AJUDA - 12 DÚVIDAS REAIS DE COMO USAR O APP - CENTRALIZADO
@Composable fun TelaAjudaComoUsar(onVoltar: () -> Unit) {
    val duvidas = listOf(
        "Como criar minha conta?" to "Na tela inicial, toque em 'Criar conta'. Preencha nome completo, e-mail válido, telefone, senha segura, confirme a senha e data de nascimento. Aceite os termos e toque em Cadastrar. Você receberá um e-mail de confirmação.",
        "Como faço login?" to "Digite seu e-mail e senha cadastrados na tela 'Faça seu login'. Se quiser, marque 'Lembrar-me' para não precisar digitar toda vez. Toque em Entrar. Se esquecer a senha, use 'Esqueci minha senha'.",
        "Como recuperar minha senha?" to "Na tela de login, toque em 'Esqueci minha senha'. Digite seu e-mail cadastrado e toque em 'Enviar Instruções'. Você receberá um e-mail com link para redefinir a senha.",
        "Como funciona o mapa?" to "O mapa mostra sua localização exata (ponto azul), abrigos, hospitais, bombeiros, polícia e farmácias próximos (pins coloridos). Toque nos filtros no topo para mostrar apenas o que precisa. Toque em um pin para ver detalhes e rota.",
        "Como filtrar hospitais e abrigos no mapa?" to "No topo do mapa há dois filtros: desastres (vermelho) e segurança (verde). Toque em 'Hospital', 'Bombeiros', 'Polícia', 'Abrigo' ou 'Farmácia' para mostrar apenas aquele tipo. Toque novamente para desfiltrar. Use o botão azul de localização para voltar para sua posição.",
        "Como usar o botão SOS?" to "Na tela inicial ou no menu, toque em 'Emergência SOS'. Escolha Polícia (190), SAMU (192), Bombeiros (193) ou Defesa Civil (199). Toque no card e o app abrirá o discador do celular com o número. Confirme a ligação. Você também pode compartilhar sua localização via WhatsApp.",
        "Como compartilhar minha localização?" to "Na tela SOS, toque em 'Compartilhar no WhatsApp'. O app pega sua localização exata via GPS e cria uma mensagem com link do Google Maps. Escolha o contato para enviar. Funciona mesmo sem internet no mapa, se o GPS estiver ativo.",
        "Como ver meu histórico de alertas?" to "Toque em 'Histórico' na barra inferior. O app busca alertas reais da sua região usando sua localização exata + API GDACS (desastres globais). Você vê distância em km, gravidade (Crítica, Alta, Média) e endereço real via Nominatim. Use a busca e filtros por gravidade e tipo.",
        "Como editar meu perfil?" to "Toque em 'Perfil' na barra inferior. Toque na foto para trocar (escolha da galeria). Edite nome, e-mail, telefone e data de nascimento. Toque em 'Salvar Alterações'. Para sair da conta, toque em 'Sair'. Seus dados ficam salvos no Firebase com segurança.",
        "Preciso permitir localização?" to "Sim, é essencial. Sem localização, o mapa não mostra onde você está, não calcula rotas seguras e não encontra abrigos próximos. Na primeira vez, o app pede permissão. Se negar, vá em Configurações do celular > Apps > SafePoint > Permissões > Localização > Permitir sempre.",
        "O mapa está cinza ou não carrega?" to "Verifique: 1) Internet ativa, 2) Localização permitida, 3) GPS ligado. O mapa usa OSM HOT gratuito, sem API Key, então não deve dar 'API KEY REQUIRED'. Se ainda falhar, feche e abra o app. O app salva cache local para funcionar offline por um tempo.",
        "Como funciona o sistema de alertas?" to "Os alertas no histórico são 100% reais, baseados na sua localização real + API GDACS da ONU. O app calcula distância via fórmula Haversine e mostra apenas desastres num raio de 10.000 km. Não há alertas fictícios. Se não houver nada próximo, mostra mensagem de segurança."
    )
    var aberta by remember { mutableStateOf<Int?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(SafePointGreen).statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)) { 
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) { 
                IconButton(onClick = onVoltar, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp)); Column { Text("Ajuda e Dúvidas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Como usar o SafePoint", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f)) } 
            } 
        }
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Perguntas Frequentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = azulEscuro, modifier = Modifier.padding(bottom = 8.dp))
            duvidas.forEachIndexed { index, (pergunta, resposta) ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), elevation = CardDefaults.cardElevation(if (aberta == index) 3.dp else 1.dp), modifier = Modifier.fillMaxWidth().clickable { aberta = if (aberta == index) null else index }) {
                    Column(modifier = Modifier.padding(16.dp)) { 
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                            Text("${index+1}. $pergunta", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro, modifier = Modifier.weight(1f).padding(end = 8.dp)); 
                            Icon(if (aberta == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = azulEscuro) 
                        }
                        if (aberta == index) { Spacer(modifier = Modifier.height(10.dp)); HorizontalDivider(color = Color(0xFFE5EAF0)); Spacer(modifier = Modifier.height(10.dp)); Text(resposta, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Text("ⓘ", color = azulLinhas, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column { Text("Ainda com dúvida?", fontWeight = FontWeight.Bold, color = azulEscuro, fontSize = 14.sp); Text("Entre em contato: suporte@safepoint.com ou use o SOS para emergências.", fontSize = 11.sp, color = Color.Gray) }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
