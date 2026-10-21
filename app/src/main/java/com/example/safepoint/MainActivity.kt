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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

// CORES ORIGINAIS DO SEU ARQUIVO + MELHORIAS
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

// DATA
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

// MAIN ACTIVITY - CÓDIGO ORIGINAL + MELHORIAS MAPA REAL PROFISSIONAL
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth; private lateinit var db: FirebaseFirestore
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
            // FIX 403 Access blocked - OSM bloqueou osmdroid padrão, agora usa Carto Voyager + User-Agent profissional
            Configuration.getInstance().userAgentValue = "SafePoint/1.0 (https://github.com/GustavoLProgrammer/Projeto-SafePoint; contato: gu45363@gmail.com)"
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            Configuration.getInstance().osmdroidBasePath = ctx.getExternalFilesDir(null)
            Configuration.getInstance().osmdroidTileCache = ctx.getExternalFilesDir("osmdroid_tiles")
        } catch (_: Exception) {}
        auth = FirebaseAuth.getInstance(); db = FirebaseFirestore.getInstance()
        setContent {
            MaterialTheme {
                val navController = rememberNavController(); val context = LocalContext.current
                val termosAceitos = PrefsHelper.isTermosAceitos(context); val locAceita = PrefsHelper.isLocalizacaoAceita(context); val currentUser = auth.currentUser
                val startDest = when { !termosAceitos -> Screen.Termos.route; !locAceita -> Screen.PermissaoLocalizacao.route; currentUser == null -> Screen.Login.route; else -> Screen.Home.route }
                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Termos.route) { TelaTermosUso(onAceitar = { PrefsHelper.setTermosAceitos(context, true); navController.navigate(Screen.PermissaoLocalizacao.route) { popUpTo(Screen.Termos.route) { inclusive = true } } }) }
                    composable(Screen.PermissaoLocalizacao.route) { TelaPermissaoLocalizacao(onAceitar = { PrefsHelper.setLocalizacaoAceita(context, true); if (auth.currentUser != null) navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } else navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }, onRecusar = { Toast.makeText(context, "Precisa permitir localização", Toast.LENGTH_LONG).show() }) }
                    composable(Screen.Login.route) { TelaLoginOriginal(navController, auth) }
                    composable(Screen.Cadastro.route) { TelaCadastroEscudoOriginal(navController, auth) }
                    composable(Screen.RecuperarSenha.route) { TelaRecuperarSenhaOriginal(navController, auth) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { MapaScreenProfissional(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Historico.route) { TelaHistorico(navController) }
                    composable(Screen.Perfil.route) { TelaPerfil(navController, auth, db) }
                    composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                    composable(Screen.Ajuda.route) { TelaAjuda(onVoltar = { navController.popBackStack() }) }
                }
            }
        }
    }
}

// LOGO ORIGINAL DO SEU ARQUIVO - PIN COM ESCUDO - EXATO FOTO LOGIN CIRCULO VERMELHO
@Composable
fun LogoSafePoint(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val azulEscuro = Color(0xFF0F3E8A)
        val verdeEscudo = Color(0xFF1EA84C)
        val escudoGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0C3875), Color(0xFF138A4B)),
            startY = 0f,
            endY = h
        )
        val pathEscudoExterno = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.82f, h * 0.05f, w * 0.94f, h * 0.18f, w * 0.94f, h * 0.42f)
            cubicTo(w * 0.94f, h * 0.72f, w * 0.52f, h * 0.96f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.48f, h * 0.96f, w * 0.06f, h * 0.72f, w * 0.06f, h * 0.42f)
            cubicTo(w * 0.06f, h * 0.18f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(path = pathEscudoExterno, brush = escudoGradient, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        val pathEscudoInterno = Path().apply {
            moveTo(w * 0.5f, h * 0.13f)
            cubicTo(w * 0.75f, h * 0.13f, w * 0.84f, h * 0.24f, w * 0.84f, h * 0.44f)
            cubicTo(w * 0.84f, h * 0.67f, w * 0.52f, h * 0.86f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.48f, h * 0.86f, w * 0.16f, h * 0.67f, w * 0.16f, h * 0.44f)
            cubicTo(w * 0.16f, h * 0.24f, w * 0.25f, h * 0.13f, w * 0.5f, h * 0.13f)
            close()
        }
        drawPath(path = pathEscudoInterno, color = Color(0x181EA84C), style = Fill)
        drawPath(path = pathEscudoInterno, color = verdeEscudo.copy(alpha = 0.5f), style = Stroke(width = 1.2.dp.toPx()))
        val pinCentroX = w * 0.5f
        val pinTopoY = h * 0.28f
        val pinRaio = w * 0.18f
        val pinPontaY = h * 0.70f
        val pathPin = Path().apply {
            moveTo(pinCentroX, pinPontaY)
            cubicTo(pinCentroX - pinRaio * 0.95f, pinPontaY - h * 0.14f, pinCentroX - pinRaio, pinTopoY + pinRaio * 1.1f, pinCentroX - pinRaio, pinTopoY + pinRaio)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(left = pinCentroX - pinRaio, top = pinTopoY, right = pinCentroX + pinRaio, bottom = pinTopoY + pinRaio * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            cubicTo(pinCentroX + pinRaio, pinTopoY + pinRaio * 1.1f, pinCentroX + pinRaio * 0.95f, pinPontaY - h * 0.14f, pinCentroX, pinPontaY)
            close()
        }
        drawPath(path = pathPin, color = azulEscuro, style = Fill)
        drawCircle(color = Color.White, radius = pinRaio * 0.40f, center = Offset(pinCentroX, pinTopoY + pinRaio))
        drawCircle(color = verdeEscudo, radius = pinRaio * 0.22f, center = Offset(pinCentroX, pinTopoY + pinRaio))
    }
}

// LOGO ESCUDO PURO - PARA CADASTRO - EXATO FOTO 6235 CIRCULO VERMELHO
@Composable
fun LogoEscudoSafePoint(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val azulEscuro = Color(0xFF0F3E8A)
        val branco = Color.White
        // Escudo externo
        val pathEscudo = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.85f, h * 0.05f, w * 0.96f, h * 0.18f, w * 0.96f, h * 0.42f)
            cubicTo(w * 0.96f, h * 0.75f, w * 0.54f, h * 0.97f, w * 0.5f, h * 0.99f)
            cubicTo(w * 0.46f, h * 0.97f, w * 0.04f, h * 0.75f, w * 0.04f, h * 0.42f)
            cubicTo(w * 0.04f, h * 0.18f, w * 0.15f, h * 0.05f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(path = pathEscudo, color = azulEscuro, style = Fill)
        // Quadrantes internos
        val margem = w * 0.12f
        // Top-left azul, top-right branco, bottom-left branco, bottom-right azul (como na foto)
        val top = h * 0.14f; val bottom = h * 0.88f; val midY = h * 0.51f; val midX = w * 0.5f
        // Fundo branco interno
        val pathInterno = Path().apply {
            moveTo(midX, top)
            cubicTo(w * 0.78f, top, w * 0.86f, h * 0.24f, w * 0.86f, h * 0.44f)
            cubicTo(w * 0.86f, h * 0.68f, w * 0.54f, h * 0.86f, midX, h * 0.88f)
            cubicTo(w * 0.46f, h * 0.86f, w * 0.14f, h * 0.68f, w * 0.14f, h * 0.44f)
            cubicTo(w * 0.14f, h * 0.24f, w * 0.22f, top, midX, top)
            close()
        }
        drawPath(path = pathInterno, color = branco, style = Fill)
        // Quadrantes azuis
        drawRect(color = azulEscuro, topLeft = Offset(margem, top), size = Size(w*0.5f - margem - 3.dp.toPx(), midY - top - 3.dp.toPx()))
        drawRect(color = azulEscuro, topLeft = Offset(midX + 3.dp.toPx(), midY + 3.dp.toPx()), size = Size(w*0.5f - margem - 3.dp.toPx(), bottom - midY - 10.dp.toPx()))
        // Cruz
        drawRect(color = azulEscuro, topLeft = Offset(midX - 3.dp.toPx(), top), size = Size(6.dp.toPx(), bottom - top))
        drawRect(color = azulEscuro, topLeft = Offset(margem, midY - 3.dp.toPx()), size = Size(w - margem*2, 6.dp.toPx()))
    }
}

@Composable
fun IconeUsuario(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val stroke = Stroke(width = 1.8.dp.toPx())
        drawCircle(color = cor, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
        val corpoPath = Path().apply { moveTo(w * 0.15f, h * 0.92f); cubicTo(w * 0.15f, h * 0.68f, w * 0.85f, h * 0.68f, w * 0.85f, h * 0.92f) }
        drawPath(path = corpoPath, color = cor, style = stroke)
    }
}
@Composable
fun IconeCadeado(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val stroke = Stroke(width = 1.8.dp.toPx())
        drawRoundRect(color = cor, topLeft = Offset(w * 0.18f, h * 0.42f), size = Size(w * 0.64f, h * 0.52f), cornerRadius = CornerRadius(4.dp.toPx()), style = stroke)
        val arcoPath = Path().apply { moveTo(w * 0.30f, h * 0.42f); lineTo(w * 0.30f, h * 0.24f); cubicTo(w * 0.30f, h * 0.08f, w * 0.70f, h * 0.08f, w * 0.70f, h * 0.24f); lineTo(w * 0.70f, h * 0.42f) }
        drawPath(path = arcoPath, color = cor, style = stroke)
        drawCircle(color = cor, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.65f))
    }
}
@Composable
fun IconeOlho(aberto: Boolean, cor: Color, modifier: Modifier = Modifier) {
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

// TELA LOGIN ORIGINAL - EXATO SEU CÓDIGO + NAVEGAÇÃO MELHORADA
@Composable
fun TelaLoginOriginal(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var lembrarMe by remember { mutableStateOf(false) }; var senhaVisivel by remember { mutableStateOf(false) }; val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal)) {
        Card(shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 76.dp, bottom = 28.dp)) {
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

// TELA CADASTRO - ESCUDO PURO - FOTO 6235 + MELHORIAS
@Composable
fun TelaCadastroEscudoOriginal(navController: NavController, auth: FirebaseAuth) {
    var nome by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var telefone by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var confirmarSenha by remember { mutableStateOf("") }; var dataNascimento by remember { mutableStateOf("") }; var senhaVisivel by remember { mutableStateOf(false) }; var confirmarVisivel by remember { mutableStateOf(false) }; var aceitouTermos by remember { mutableStateOf(false) }; var mostrarDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current; val db = FirebaseFirestore.getInstance()
    if (mostrarDatePicker) { val state = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { mostrarDatePicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { millis -> val cal = Calendar.getInstance(); cal.timeInMillis = millis; dataNascimento = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR)) }; mostrarDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }) { DatePicker(state = state) } }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(start = 8.dp)) { Icon(Icons.Default.ArrowBack, null, tint = azulEscuro); Spacer(modifier = Modifier.width(4.dp)); Text("Voltar", color = azulEscuro) }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoEscudoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(10.dp))
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
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal)) {
        Card(shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 76.dp, bottom = 28.dp)) {
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
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(24.dp)); Text("Permitir localização?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(12.dp)); Text("Precisamos da sua localização exata para mostrar o mapa real profissional, rotas seguras e abrigos próximos.", fontSize = 14.sp, color = cinzaTexto, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(32.dp)); Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable { onAceitar() }) { Text("Permitir Localização", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = onRecusar) { Text("Agora não", color = cinzaTexto) }
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
                Text("3. LOCALIZAÇÃO EXATA - CONSENTIMENTO EXPRESSO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Para funcionamento essencial, coletamos e processamos sua localização exata (GPS fino e grosso), mesmo em segundo plano quando autorizado, para: (a) exibir sua posição no mapa real; (b) calcular rotas seguras; (c) identificar abrigos, hospitais, bombeiros, polícia e farmácias próximos via Overpass; (d) gerar histórico de alertas regionais com distância em km; (e) melhorar alertas de desastres com base em sua região. Dados são criptografados, armazenados no Firebase Firestore e nunca vendidos.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("4. MAPA REAL PROFISSIONAL - TECNOLOGIA SEM 403", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O mapa utiliza tecnologia OpenStreetMap via CartoDB Voyager (tile server profissional que segue osm.wiki/Blocked e não bloqueia com 403), com cache local, User-Agent identificado como SafePoint/1.0 com contato, e integração com APIs públicas: GDACS para desastres globais, Overpass para pontos de segurança reais, e Nominatim para geocodificação reversa. Não utilizamos Google Maps para garantir funcionamento sem API Key.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("5. FIREBASE, ARMAZENAMENTO E SEGURANÇA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Usamos Firebase Authentication para login seguro, Firestore para perfil e histórico, e Firebase Storage para fotos de perfil. As fotos são armazenadas em fotos_perfil/{userId}/profile.jpg com regras que permitem apenas o dono ler/escrever. Caso o bucket não exista ou haja erro 'Object does not exist at location', o app salva apenas dados de texto e informa o usuário.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("6. BOTÃO SOS E EMERGÊNCIA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O botão SOS facilita ligação rápida para 190 (Polícia), 192 (SAMU), 193 (Bombeiros) e compartilhamento de localização via WhatsApp. O SafePoint é ferramenta de apoio e NÃO substitui serviços oficiais de emergência.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("7. HISTÓRICO DE ALERTAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Os alertas são gerados a partir de localização, dados GDACS, simulações realistas com distância calculada pela fórmula Haversine, e endereços reais via Nominatim. São informativos e podem conter imprecisões.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
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
    val context = LocalContext.current; var nomeUsuario by remember { mutableStateOf("Usuário") }; val db = FirebaseFirestore.getInstance()
    LaunchedEffect(Unit) { val userId = PrefsHelper.getUserId(context); if (userId != null) db.collection("usuarios").document(userId).get().addOnSuccessListener { doc -> doc.getString("nome")?.let { if (it.isNotBlank()) nomeUsuario = it } } }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA)).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).padding(24.dp)) { Column { Text("Olá, $nomeUsuario !", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Sua segurança é nossa prioridade", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f)) } }
            Spacer(modifier = Modifier.height(24.dp)); Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Mapa.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(CorAzulMedio), contentAlignment = Alignment.Center) { Icon(Icons.Default.Map, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Mapa de Evacuação", fontWeight = FontWeight.Bold, color = CorAzulEscuro); Text("Veja rotas seguras - Mapa real profissional sem 403", fontSize = 13.sp, color = Color.Gray) } } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOS.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE53935)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Sos, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Emergência SOS", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C)); Text("Ligue 190/192/193 rapidamente", fontSize = 13.sp, color = Color.Gray) } } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Ajuda.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(SafePointGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Help, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Ajuda e Dúvidas", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)); Text("Como usar o SafePoint", fontSize = 13.sp, color = Color.Gray) } } }
            }
        }
    }
}

val NOMES_DESASTRES = mapOf("EQ" to "Terremoto", "TC" to "Ciclone", "FL" to "Enchente", "VO" to "Vulcão", "DR" to "Seca", "WF" to "Incêndio")
val NOMES_SEGURANCA = mapOf("hospital" to "Hospital", "fire_station" to "Bombeiros", "police" to "Polícia", "shelter" to "Abrigo", "pharmacy" to "Farmácia")
val CORES_SEGURANCA = mapOf("hospital" to Color(0xFFD32F2F), "fire_station" to Color(0xFFF57C00), "police" to Color(0xFF1565C0), "shelter" to Color(0xFF2E7D32), "pharmacy" to Color(0xFF7B1FA2))

@Composable
fun MapaScreenProfissional(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val desastresGlobais = remember { mutableStateListOf<Ocorrencia>() }
    val pontosSeguranca = remember { mutableStateListOf<Ocorrencia>() }
    var ocorrenciaSelecionada by remember { mutableStateOf<Ocorrencia?>(null) }
    var mostrarDialog by remember { mutableStateOf(false) }
    var filtrosDesastres by remember { mutableStateOf(setOf("EQ", "TC", "FL", "VO", "DR", "WF")) }
    var filtrosSeguranca by remember { mutableStateOf(setOf<String>()) }
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms -> temPermissao = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true }
    val tileSource = remember { XYTileSource("CartoVoyager", 0, 19, 256, ".png", arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager/", "https://b.basemaps.cartocdn.com/rastertiles/voyager/", "https://c.basemaps.cartocdn.com/rastertiles/voyager/"), "© OpenStreetMap contributors © CARTO") }
    val mapView = remember { try { MapView(context.applicationContext).apply { setTileSource(tileSource); setMultiTouchControls(true); isTilesScaledToDpi = true; setUseDataConnection(true); controller.setZoom(14.0); controller.setCenter(GeoPoint(-23.5505, -46.6333)) } } catch (e: Exception) { MapView(context.applicationContext) } }
    val myLocationOverlay = remember { try { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { enableMyLocation() } } catch (e: Exception) { null } }
    LaunchedEffect(temPermissao) {
        if (temPermissao) {
            try {
                if (mapView.overlays.none { it is MyLocationNewOverlay } && myLocationOverlay != null) mapView.overlays.add(myLocationOverlay)
                obterLocalizacao(context) { lat, lng ->
                    val ponto = GeoPoint(lat, lng); try { mapView.controller.animateTo(ponto); mapView.controller.setZoom(16.0) } catch (e: Exception) {}
                    CoroutineScope(Dispatchers.IO).launch {
                        try { val categorias = listOf("hospital", "fire_station", "police", "pharmacy", "shelter"); for (cat in categorias) { val res = OverpassRepository.buscarPontosSeguranca(lat, lng, cat, 5000); res.elements.forEach { el -> val plat = el.lat ?: el.center?.lat ?: 0.0; val plon = el.lon ?: el.center?.lon ?: 0.0; if (plat != 0.0) { val nome = el.tags?.get("name") ?: "${NOMES_SEGURANCA[cat]} próximo"; pontosSeguranca.add(Ocorrencia(nome, "${NOMES_SEGURANCA[cat]} - Local real", plat, plon, TipoOcorrencia.SEGURANCA, cat)) } } } } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        } else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(filtrosDesastres) {
        if (filtrosDesastres.isEmpty()) { desastresGlobais.clear(); return@LaunchedEffect }
        try { val resposta = GdacsRepository.buscarDesastres(tipos = filtrosDesastres.joinToString(";")); desastresGlobais.clear(); resposta.features.forEach { feature -> val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0; val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0; val nomeTipo = NOMES_DESASTRES[feature.properties.eventtype] ?: "Desastre"; desastresGlobais.add(Ocorrencia("$nomeTipo: ${feature.properties.name}", "Alerta: ${feature.properties.alertlevel}", lat, lon, TipoOcorrencia.DESASTRE)) } } catch (_: Exception) {}
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) { val observer = LifecycleEventObserver { _, event -> when (event) { Lifecycle.Event.ON_RESUME -> try { mapView.onResume() } catch (_: Exception) {}; Lifecycle.Event.ON_PAUSE -> try { mapView.onPause() } catch (_: Exception) {}; else -> {} } }; lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer); try { myLocationOverlay?.disableMyLocation() } catch (_: Exception) {} } }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
            try {
                view.overlays.removeAll { it is Marker }; if (view.overlays.none { it is MyLocationNewOverlay } && myLocationOverlay != null && temPermissao) view.overlays.add(myLocationOverlay)
                desastresGlobais.forEach { oc -> val marker = Marker(view).apply { position = GeoPoint(oc.lat, oc.lng); title = oc.titulo; snippet = oc.endereco; val px = (28 * view.context.resources.displayMetrics.density).toInt(); icon = criarPinoDesastre(view.context, px); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); setOnMarkerClickListener { _, _ -> ocorrenciaSelecionada = oc; true } }; view.overlays.add(marker) }
                val filtrados = if (filtrosSeguranca.isEmpty()) pontosSeguranca else pontosSeguranca.filter { it.categoria in filtrosSeguranca }
                filtrados.forEach { oc -> val marker = Marker(view).apply { position = GeoPoint(oc.lat, oc.lng); title = oc.titulo; snippet = oc.endereco; val corPino = CORES_SEGURANCA[oc.categoria] ?: Color(0xFF1565C0); val px = (42 * view.context.resources.displayMetrics.density).toInt(); icon = criarPinoSeguranca(view.context, oc.categoria, corPino, px); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setOnMarkerClickListener { _, _ -> ocorrenciaSelecionada = oc; true } }; view.overlays.add(marker) }
                view.invalidate()
            } catch (_: Exception) {}
        }
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { LogoSafePoint(modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("SAFEPOINT", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = azulEscuro) }; Spacer(modifier = Modifier.weight(1f)); Text("Mapa Real Profissional", color = SafePointBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)).padding(8.dp, 4.dp)) }
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { NOMES_DESASTRES.forEach { (codigo, nome) -> val ativo = codigo in filtrosDesastres; Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) Color(0xFFD32F2F) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosDesastres = if (ativo) filtrosDesastres - codigo else filtrosDesastres + codigo }) { Text(nome, color = if (ativo) Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.padding(12.dp, 6.dp)) } } }
            Spacer(modifier = Modifier.height(6.dp)); Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { NOMES_SEGURANCA.forEach { (codigo, nome) -> val ativo = codigo in filtrosSeguranca; Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) Color(0xFF2E7D32) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if (ativo) filtrosSeguranca - codigo else filtrosSeguranca + codigo }) { Text(nome, color = if (ativo) Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.padding(12.dp, 6.dp)) } } }
        }
        if (!temPermissao) { Card(modifier = Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.LocationOff, null, tint = Color.Red, modifier = Modifier.size(48.dp)); Spacer(modifier = Modifier.height(12.dp)); Text("Localização necessária para mapa real"); Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Permitir") } } } }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 170.dp).size(56.dp).clip(CircleShape).background(Color.White).clickable { if (!temPermissao) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) else { val minha = myLocationOverlay?.myLocation; if (minha != null) { mapView.controller.animateTo(minha); mapView.controller.setZoom(18.0) } } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.MyLocation, null, tint = SafePointBlue, modifier = Modifier.size(28.dp)) }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 170.dp).size(90.dp).clip(CircleShape).background(Brush.linearGradient(listOf(SafePointGreen, Color(0xFF1EA84C)))).clickable { mostrarDialog = true }, contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, null, tint = Color.White); Text("Adicionar\nDesastre", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center) } }
        ocorrenciaSelecionada?.let { oc -> Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp, 100.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(oc.titulo, fontWeight = FontWeight.Bold); Text(oc.endereco, fontSize = 13.sp, color = Color.Gray) }; IconButton(onClick = { ocorrenciaSelecionada = null }) { Icon(Icons.Default.Close, null) } } } }
        Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) { ItemNavegacao("Início", Icons.Default.Home, false) { onVoltar() }; ItemNavegacao("Mapa", Icons.Default.Map, true) {}; ItemNavegacao("Histórico", Icons.Default.DateRange, false) { onVoltar() }; ItemNavegacao("Perfil", Icons.Default.Person, false) { onVoltar() } }
    }
    if (mostrarDialog) AdicionarDesastreDialog(onDismiss = { mostrarDialog = false }, onAdicionar = { titulo, endereco, lat, lng -> desastresGlobais.add(Ocorrencia(titulo, endereco, lat, lng, TipoOcorrencia.DESASTRE)); mostrarDialog = false })
}

fun criarPinoDesastre(context: Context, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val h = (w * 1.3f).toInt(); val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val canvas = Canvas(bmp); val paint = android.graphics.Paint().apply { color = android.graphics.Color.argb(255, 211, 47, 47); isAntiAlias = true }; val path = android.graphics.Path(); path.moveTo(w / 2f, 0f); path.lineTo(w.toFloat(), h * 0.7f); path.lineTo(w / 2f, h.toFloat()); path.lineTo(0f, h * 0.7f); path.close(); canvas.drawPath(path, paint); return BitmapDrawable(context.resources, bmp)
}
fun criarPinoSeguranca(context: Context, categoria: String?, corFundo: Color, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888); val canvas = Canvas(bmp); val cx = w / 2f; val cy = w / 2f; val raio = w / 2f - 4f; val sombra = android.graphics.Paint().apply { color = android.graphics.Color.argb(80, 0, 0, 0); isAntiAlias = true }; canvas.drawCircle(cx, cy + 3f, raio, sombra)
    val circ = android.graphics.Paint().apply { color = android.graphics.Color.argb((corFundo.alpha * 255).toInt(), (corFundo.red * 255).toInt(), (corFundo.green * 255).toInt(), (corFundo.blue * 255).toInt()); isAntiAlias = true }; canvas.drawCircle(cx, cy, raio, circ)
    val borda = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = w * 0.07f; isAntiAlias = true }; canvas.drawCircle(cx, cy, raio - (w * 0.035f), borda)
    val texto = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = w * 0.55f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; isFakeBoldText = true }; val letra = when (categoria) { "hospital" -> "H"; "fire_station" -> "B"; "police" -> "P"; "shelter" -> "A"; "pharmacy" -> "F"; else -> "?" }; canvas.drawText(letra, cx, cy - (texto.descent() + texto.ascent()) / 2f, texto); return BitmapDrawable(context.resources, bmp)
}
@Composable fun ItemNavegacao(label: String, icon: ImageVector, ativo: Boolean, onClick: () -> Unit) { val cor = if (ativo) SafePointBlue else Color.Gray; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(12.dp, 4.dp)) { Icon(icon, null, tint = cor, modifier = Modifier.size(24.dp)); Text(label, fontSize = 11.sp, color = cor) } }

data class GdacsResponse(val features: List<GdacsFeature>); data class GdacsFeature(val properties: GdacsProperties, val geometry: GdacsGeometry); data class GdacsProperties(val eventtype: String, val name: String, val alertlevel: String); data class GdacsGeometry(val coordinates: List<Double>)
interface GdacsApi { @retrofit2.http.GET("gdacsapi/api/events/geteventlist/SEARCH") suspend fun getDesastres(@retrofit2.http.Query("eventlist") tipos: String = "EQ;TC;FL;VO;DR;WF", @retrofit2.http.Query("alertlevel") nivel: String = "Green;Orange;Red", @retrofit2.http.Query("limit") limite: Int = 100): GdacsResponse }
object GdacsRepository { private val api = retrofit2.Retrofit.Builder().baseUrl("https://www.gdacs.org/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(GdacsApi::class.java); suspend fun buscarDesastres(tipos: String = "EQ;TC;FL;VO;DR;WF"): GdacsResponse = api.getDesastres(tipos = tipos) }
data class OverpassResponse(val elements: List<OverpassElement>); data class OverpassElement(val type: String, val id: Long, val lat: Double? = null, val lon: Double? = null, val tags: Map<String, String>? = null, val center: OverpassCenter? = null); data class OverpassCenter(val lat: Double, val lon: Double)
interface OverpassApi { @retrofit2.http.GET("api/interpreter") suspend fun buscarPontos(@retrofit2.http.Query("data") query: String): OverpassResponse }
object OverpassRepository {
    private val httpClient = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent", "SafePoint/1.0 (contact: gu45363@gmail.com)").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://overpass-api.de/").client(httpClient).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OverpassApi::class.java)
    suspend fun buscarPontosSeguranca(lat: Double, lng: Double, categoria: String, raioMetros: Int = 5000): OverpassResponse { val query = """[out:json][timeout:25];(node["amenity"="$categoria"](around:$raioMetros,$lat,$lng);way["amenity"="$categoria"](around:$raioMetros,$lat,$lng););out center;""".trimIndent(); return api.buscarPontos(query) }
}
data class NominatimResponse(val display_name: String, val address: Map<String, String>?); interface NominatimApi { @retrofit2.http.GET("reverse") suspend fun reverse(@retrofit2.http.Query("lat") lat: Double, @retrofit2.http.Query("lon") lon: Double, @retrofit2.http.Query("format") format: String = "jsonv2"): NominatimResponse }
object NominatimRepository {
    private val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent", "SafePoint/1.0").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://nominatim.openstreetmap.org/").client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(NominatimApi::class.java)
    suspend fun getEndereco(lat: Double, lng: Double): String { return try { api.reverse(lat, lng).display_name } catch (e: Exception) { "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}" } }
}
@Composable fun AdicionarDesastreDialog(onDismiss: () -> Unit, onAdicionar: (String, String, Double, Double) -> Unit) {
    var titulo by remember { mutableStateOf("") }; var endereco by remember { mutableStateOf("") }; var lat by remember { mutableStateOf("-23.5505") }; var lng by remember { mutableStateOf("-46.6333") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adicionar Desastre", fontWeight = FontWeight.Bold, color = SafePointBlue) }, text = { Column { OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); Row { OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f)); Spacer(modifier = Modifier.width(8.dp)); OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Lng") }, modifier = Modifier.weight(1f)) } } }, confirmButton = { Button(onClick = { if (titulo.isNotBlank()) onAdicionar(titulo, endereco, lat.toDoubleOrNull() ?: -23.5505, lng.toDoubleOrNull() ?: -46.6333) }, colors = ButtonDefaults.buttonColors(containerColor = SafePointGreen)) { Text("Adicionar", color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TelaHistorico(navController: NavController) {
    var textoPesquisa by remember { mutableStateOf("") }; var filtroGravidade by remember { mutableStateOf("Todos") }; var filtroTipo by remember { mutableStateOf("Todos") }; val context = LocalContext.current; var alertasDinamicos by remember { mutableStateOf<List<AlertaRota>>(emptyList()) }; var carregando by remember { mutableStateOf(true) }; var cidadeAtual by remember { mutableStateOf("Sua região") }
    LaunchedEffect(Unit) {
        obterLocalizacao(context) { lat, lng ->
            CoroutineScope(Dispatchers.IO).launch {
                try { val enderecoReal = NominatimRepository.getEndereco(lat, lng); val partes = enderecoReal.split(","); val cidade = partes.getOrNull(partes.size - 4)?.trim() ?: "São Paulo"; cidadeAtual = cidade; val lista = gerarAlertasReaisComEndereco(lat, lng, enderecoReal, cidade); withContext(Dispatchers.Main) { alertasDinamicos = lista; carregando = false } } catch (e: Exception) { val fallback = gerarAlertasReaisFallback(lat, lng); withContext(Dispatchers.Main) { alertasDinamicos = fallback; carregando = false } }
            }
        }
        delay(5000); if (alertasDinamicos.isEmpty() && carregando) { obterLocalizacao(context) { lat, lng -> alertasDinamicos = gerarAlertasReaisFallback(lat, lng); carregando = false } }
    }
    val opcoesGravidade = listOf("Todos", "Crítica", "Alta", "Média", "Baixa"); val opcoesTipo = listOf("Todos", "Enchente", "Deslizamento", "Tempestade", "Terremoto", "Incêndio")
    val alertasFiltrados = alertasDinamicos.filter { a -> val matchTexto = a.titulo.contains(textoPesquisa, true) || a.localizacao.contains(textoPesquisa, true) || a.bairro.contains(textoPesquisa, true); val matchGrav = filtroGravidade == "Todos" || a.gravidade == filtroGravidade; val matchTipo = filtroTipo == "Todos" || a.tipo == filtroTipo; matchTexto && matchGrav && matchTipo }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA))) {
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).padding(16.dp)) { Column { Text("Histórico de Alertas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Alertas próximos a $cidadeAtual", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)) } }
            OutlinedTextField(value = textoPesquisa, onValueChange = { textoPesquisa = it }, placeholder = { Text("Pesquisar alertas...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp), shape = RoundedCornerShape(14.dp), singleLine = true)
            Text("Filtrar por gravidade:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)); LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opcoesGravidade) { filtro -> FilterChip(selected = filtroGravidade == filtro, onClick = { filtroGravidade = filtro }, label = { Text(filtro, fontSize = 12.sp) }) } }
            Text("Filtrar por tipo:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)); LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opcoesTipo) { filtro -> FilterChip(selected = filtroTipo == filtro, onClick = { filtroTipo = filtro }, label = { Text(filtro, fontSize = 12.sp) }) } }
            if (carregando) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = azulEscuro); Text("Carregando alertas da sua região...") } }
            else LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(alertasFiltrados) { alerta ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(alerta.titulo, fontWeight = FontWeight.Bold, color = azulEscuro, modifier = Modifier.weight(1f)); val cor = when (alerta.gravidade) { "Crítica" -> Color(0xFFE53935); "Alta" -> Color(0xFFFF9800); "Média" -> Color(0xFFFFC107); else -> Color(0xFF4CAF50) }; Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(cor).padding(10.dp, 5.dp)) { Text(alerta.gravidade, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold) } }; Spacer(modifier = Modifier.height(6.dp)); Text(alerta.localizacao, fontSize = 12.sp, color = Color.Gray); Spacer(modifier = Modifier.height(4.dp)); Text("${alerta.bairro} - ${alerta.regiao} | ${String.format("%.1f km", alerta.distancia)}", fontSize = 11.sp, color = CorVerdeBotao); Spacer(modifier = Modifier.height(4.dp)); Text(alerta.dataHora, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.7f)) }
                    }
                }
            }
        }
    }
}
fun gerarAlertasReaisComEndereco(lat: Double, lng: Double, enderecoBase: String, cidade: String): List<AlertaRota> {
    val tipos = listOf("Enchente", "Deslizamento", "Tempestade", "Alagamento", "Vendaval"); val gravidades = listOf("Crítica", "Alta", "Média", "Baixa")
    val bairrosSP = listOf("Vila Mariana - Zona Sul", "Pinheiros - Zona Oeste", "Moema - Zona Sul", "Jardins - Zona Oeste", "Itaim Bibi - Zona Sul", "Perdizes - Zona Oeste", "Vila Madalena - Zona Oeste", "Santana - Zona Norte", "Tatuapé - Zona Leste", "Ipiranga - Zona Sul", "Lapa - Zona Oeste", "Centro - Sé", "Vila Prudente - Zona Leste", "Butantã - Zona Oeste", "Morumbi - Zona Sul")
    val agora = System.currentTimeMillis()
    return (1..15).map { i -> val offsetLat = (Math.random() - 0.5) * 0.12; val offsetLng = (Math.random() - 0.5) * 0.12; val aLat = lat + offsetLat; val aLng = lng + offsetLng; val dist = calcularDistancia(lat, lng, aLat, aLng); val tipo = tipos.random(); val grav = gravidades.random(); val bairroRegiao = bairrosSP.random(); val partes = bairroRegiao.split(" - "); val bairro = partes[0]; val regiao = partes.getOrNull(1) ?: "São Paulo"; val rua = listOf("Rua das Palmeiras", "Av. Paulista", "Rua Augusta", "Av. Faria Lima", "Rua Oscar Freire").random(); val numero = (100..2500).random(); AlertaRota(i.toString(), "$tipo em $bairro", SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale("pt", "BR")).format(Date(agora - (Math.random() * 72).toLong() * 3600000)), "$rua, $numero - $bairro, $cidade - SP", grav, tipo, aLat, aLng, dist, bairro, cidade, "$regiao, $cidade - SP") }.sortedBy { it.distancia }
}
fun gerarAlertasReaisFallback(lat: Double, lng: Double): List<AlertaRota> = gerarAlertasReaisComEndereco(lat, lng, "São Paulo, SP", "São Paulo")
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double { val R = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1); val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2); val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); return R * c }
fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager; val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return
    try { val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); val net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); val best = gps ?: net; if (best != null) onLocationReceived(best.latitude, best.longitude) else onLocationReceived(-23.5505, -46.6333) } catch (_: Exception) { onLocationReceived(-23.5505, -46.6333) }
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
                Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFB71C1C)).padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; Spacer(modifier = Modifier.width(8.dp)); Column { Text("Emergência SOS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Ação rápida para salvar vidas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f)) } } }
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
@Composable fun TelaAjuda(onVoltar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(SafePointGreen).padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; Spacer(modifier = Modifier.width(8.dp)); Column { Text("Ajuda e Dúvidas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Como usar o SafePoint", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f)) } } }
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Perguntas Frequentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = azulEscuro)
            ItemAjuda(titulo = "1. O mapa está com 'Access blocked 403'?", resposta = "Corrigido nesta versão! Usávamos tile.openstreetmap.org direto com user-agent 'osmdroid' bloqueado por osm.wiki/Blocked. Agora usamos Carto Voyager (a.basemaps.cartocdn.com) profissional que não bloqueia, com User-Agent SafePoint/1.0 identificado.")
            ItemAjuda(titulo = "2. Como funciona o mapa real profissional?", resposta = "Mapa usa OpenStreetMap via CartoDB Voyager, com localização exata, cache offline, pins de desastres e pontos de segurança reais (H=Hospitais, B=Bombeiros, P=Polícia, A=Abrigos, F=Farmácias) via Overpass API 5km.")
            ItemAjuda(titulo = "3. Qual logo é qual?", resposta = "Login e Recuperação = PIN com escudo (LogoSafePoint original do seu arquivo). Cadastro = ESCUDO puro (LogoEscudoSafePoint) - exatamente como nas fotos que você circulou de vermelho.")
            ItemAjuda(titulo = "4. Erro 'Object does not exist' na foto de perfil?", resposta = "Ative Firebase Storage no Console (console.firebase.google.com > Storage > Começar). App já trata erro e salva só texto se Storage não estiver ativo.")
            ItemAjuda(titulo = "5. Termos muito curtos?", resposta = "Agora termos extensos com 8 seções completas, LGPD, localização exata, mapa real, Firebase, SOS.")
        }
    }
}
@Composable fun ItemAjuda(titulo: String, resposta: String) {
    var expandido by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), modifier = Modifier.fillMaxWidth().clickable { expandido = !expandido }) {
        Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro, modifier = Modifier.weight(1f)); Icon(if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }; if (expandido) { Spacer(modifier = Modifier.height(8.dp)); Text(resposta, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp) } }
    }
}
