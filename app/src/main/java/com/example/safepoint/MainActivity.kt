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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
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
            Configuration.getInstance().userAgentValue = "SafePoint/1.2 (Android app; https://github.com/GustavoLProgrammer/Projeto-SafePoint; contato: gu45363@gmail.com)"
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
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(24.dp)); Text("Permitir localização?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(12.dp)); Text("Precisamos da sua localização exata para mostrar o mapa real, rotas seguras e abrigos próximos.", fontSize = 14.sp, color = cinzaTexto, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(32.dp)); Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable { onAceitar() }) { Text("Permitir Localização", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = onRecusar) { Text("Agora não", color = cinzaTexto) }
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
                Text("1. OBJETIVO E ACEITAÇÃO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O SafePoint inclui ferramentas de demonstração de mapa, alertas e rotas. Nesta versão, o mapa e o histórico usam dados simulados e um desenho geográfico offline; não são um serviço oficial de alerta nem de evacuação. Ao criar uma conta e utilizar o aplicativo, você declara ter lido, compreendido e concordado com estes Termos de Uso e Política de Privacidade.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("2. CADASTRO E RESPONSABILIDADE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Você se compromete a fornecer dados verídicos, completos e atualizados no cadastro (nome, e-mail, telefone, data de nascimento). É responsável por manter a confidencialidade de sua senha e por todas as atividades realizadas em sua conta.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("3. LOCALIZAÇÃO EXATA - CONSENTIMENTO EXPRESSO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("A localização do aparelho é opcional nesta demonstração e só é consultada após a autorização do sistema e quando você solicita a função de localização. O mapa e o histórico simulados funcionam sem GPS e não enviam coordenadas a serviços externos. Recursos de conta e perfil podem continuar usando os serviços Firebase descritos nesta política.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("4. MAPA REAL PROFISSIONAL - TECNOLOGIA SEM 403", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O mapa desta versão é um desenho vetorial simplificado gerado localmente no aparelho. Não baixa tiles, não usa chave de mapa nem consulta serviços externos de geocodificação ou rotas. As estradas desenhadas são esquemáticas e não representam a malha viária completa.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("5. FIREBASE, ARMAZENAMENTO E SEGURANÇA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Usamos Firebase Authentication para login seguro, Firestore para perfil e histórico, e Firebase Storage para fotos de perfil. As fotos são armazenadas em fotos_perfil/{userId}/profile.jpg com regras que permitem apenas o dono ler/escrever. Caso o bucket não exista ou haja erro 'Object does not exist at location', o app salva apenas dados de texto e informa o usuário.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("6. BOTÃO SOS E EMERGÊNCIA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O botão SOS facilita ligação rápida para 190 (Polícia), 192 (SAMU), 193 (Bombeiros) e compartilhamento de localização via WhatsApp. O SafePoint é ferramenta de apoio e NÃO substitui serviços oficiais de emergência.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("7. HISTÓRICO DE ALERTAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Os alertas e os abrigos são gerados localmente para demonstração e são fictícios. As cidades de referência correspondem a locais brasileiros conhecidos, mas os marcadores não representam ocorrências reais nem abrigos confirmados. Não use o mapa ou o histórico para tomar decisões de emergência.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
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
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).statusBarsPadding().padding(24.dp)) { Column(modifier = Modifier.padding(top = 8.dp)) { Text("Olá, $nomeUsuario !", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Sua segurança é nossa prioridade", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f)) } }
            Spacer(modifier = Modifier.height(24.dp)); Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Mapa.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(CorAzulMedio), contentAlignment = Alignment.Center) { Icon(Icons.Default.Map, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Mapa de Evacuação", fontWeight = FontWeight.Bold, color = CorAzulEscuro); Text("Veja rotas seguras e abrigos próximos", fontSize = 13.sp, color = Color.Gray) } } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOS.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE53935)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Sos, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Emergência SOS", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C)); Text("Ligue 190/192/193 rapidamente", fontSize = 13.sp, color = Color.Gray) } } }
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Ajuda.route) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), elevation = CardDefaults.cardElevation(4.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(SafePointGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Help, null, tint = Color.White) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text("Ajuda e Dúvidas", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)); Text("Como usar o SafePoint", fontSize = 13.sp, color = Color.Gray) } } }
            }
        }
    }
}

val NOMES_DESASTRES = mapOf("EQ" to "Terremoto", "TC" to "Ciclone", "FL" to "Enchente", "VO" to "Vulcão", "DR" to "Seca", "WF" to "Incêndio")
val NOMES_SEGURANCA = mapOf("hospital" to "Hospital", "fire_station" to "Bombeiros", "police" to "Polícia", "shelter" to "Abrigo", "pharmacy" to "Farmácia")
val CORES_SEGURANCA = mapOf("hospital" to Color(0xFFD32F2F), "fire_station" to Color(0xFFF57C00), "police" to Color(0xFF1565C0), "shelter" to Color(0xFF2E7D32), "pharmacy" to Color(0xFF7B1FA2))



// MAPA PROFISSIONAL GPS REAL - VERSÃO 3.0 PROFISSIONAL COM ROTA AZUL, TEMPO E ALERTAS COLORIDOS
data class RotaInfo(val distanciaKm: Double, val duracaoMin: Int, val duracaoTexto: String, val distanciaTexto: String)
enum class ModoRota(val titulo: String, val valhallaCosting: String, val googleMode: String) {
    CARRO("Carro", "auto", "driving"), A_PE("A pé", "pedestrian", "walking"), BICICLETA("Bicicleta", "bicycle", "bicycling")
}
data class DesastreDetalhado(val ocorrencia: Ocorrencia, val alertLevel: String, val gravidade: String, val tipoCode: String, val nomeTipo: String, val fonte: String = "GDACS")

// Dados geográficos locais de cidades brasileiras conhecidas. Alertas e abrigos são simulados.
data class CidadeDemo(val nome: String, val uf: String, val lat: Double, val lng: Double, val aliases: List<String> = emptyList())
data class AlertaDemo(val id: String, val cidade: String, val tipo: String, val gravidade: String, val lat: Double, val lng: Double, val descricao: String, val dataHora: String)
data class AbrigoDemo(val id: String, val cidade: String, val nome: String, val lat: Double, val lng: Double)
object SafePointDemoData {
    val cidades = listOf(
        CidadeDemo("Brasil", "", -14.2350, -51.9253, listOf("brazil", "país", "pais")),
        CidadeDemo("São Paulo", "SP", -23.5505, -46.6333, listOf("sao paulo", "capital paulista", "avenida paulista", "paulista", "rua augusta", "rua da consolacao", "rua oscar freire", "marginal tietê", "marginal tiete", "pinheiros", "moema", "tatuape", "itaim bibi")),
        CidadeDemo("Rio de Janeiro", "RJ", -22.9068, -43.1729, listOf("rio de janeiro", "copacabana", "ipanema", "barra da tijuca", "botafogo", "centro do rio", "lapa rio")),
        CidadeDemo("Brasília", "DF", -15.7939, -47.8828, listOf("brasilia", "distrito federal", "plano piloto", "asa sul", "asa norte", "taguatinga")),
        CidadeDemo("Salvador", "BA", -12.9777, -38.5016, listOf("salvador", "pelourinho", "barra salvador", "rio vermelho")),
        CidadeDemo("Fortaleza", "CE", -3.7319, -38.5267, listOf("fortaleza", "aldeota", "meireles fortaleza")),
        CidadeDemo("Belo Horizonte", "MG", -19.9167, -43.9345, listOf("bh", "belo horizonte", "savassi", "pampulha")),
        CidadeDemo("Manaus", "AM", -3.1190, -60.0217, listOf("manaus", "ponta negra manaus")),
        CidadeDemo("Curitiba", "PR", -25.4284, -49.2733, listOf("curitiba", "batel", "centro civico curitiba")),
        CidadeDemo("Recife", "PE", -8.0476, -34.8770, listOf("recife", "boa viagem recife", "recife antigo")),
        CidadeDemo("Porto Alegre", "RS", -30.0346, -51.2177, listOf("porto alegre", "moinhos de vento", "cidade baixa porto alegre")),
        CidadeDemo("Belém", "PA", -1.4558, -48.5039, listOf("belem")),
        CidadeDemo("Goiânia", "GO", -16.6869, -49.2648, listOf("goiania")),
        CidadeDemo("Guarulhos", "SP", -23.4543, -46.5337, listOf("guarulhos")),
        CidadeDemo("Campinas", "SP", -22.9056, -47.0608, listOf("campinas")),
        CidadeDemo("São Luís", "MA", -2.5307, -44.3068, listOf("sao luis")),
        CidadeDemo("Maceió", "AL", -9.6498, -35.7089, listOf("maceio")),
        CidadeDemo("Natal", "RN", -5.7945, -35.2110, listOf("natal")),
        CidadeDemo("João Pessoa", "PB", -7.1195, -34.8450, listOf("joao pessoa")),
        CidadeDemo("Teresina", "PI", -5.0892, -42.8016, listOf("teresina")),
        CidadeDemo("Aracaju", "SE", -10.9472, -37.0731, listOf("aracaju")),
        CidadeDemo("Campo Grande", "MS", -20.4697, -54.6201, listOf("campo grande")),
        CidadeDemo("Cuiabá", "MT", -15.6014, -56.0979, listOf("cuiaba")),
        CidadeDemo("Porto Velho", "RO", -8.7612, -63.9004, listOf("porto velho")),
        CidadeDemo("Rio Branco", "AC", -9.9754, -67.8249, listOf("rio branco")),
        CidadeDemo("Boa Vista", "RR", 2.8235, -60.6758, listOf("boa vista")),
        CidadeDemo("Macapá", "AP", 0.0349, -51.0694, listOf("macapa")),
        CidadeDemo("Palmas", "TO", -10.2491, -48.3243, listOf("palmas")),
        CidadeDemo("Vitória", "ES", -20.3155, -40.3128, listOf("vitoria")),
        CidadeDemo("Florianópolis", "SC", -27.5954, -48.5480, listOf("florianopolis")),
        CidadeDemo("Santos", "SP", -23.9608, -46.3336, listOf("santos")),
        CidadeDemo("São José dos Campos", "SP", -23.1896, -45.8841, listOf("sao jose dos campos")),
        CidadeDemo("Ribeirão Preto", "SP", -21.1704, -47.8103, listOf("ribeirao preto")),
        CidadeDemo("Sorocaba", "SP", -23.5015, -47.4526, listOf("sorocaba")),
        CidadeDemo("Niterói", "RJ", -22.8832, -43.1034, listOf("niteroi")),
        CidadeDemo("Duque de Caxias", "RJ", -22.7858, -43.3117, listOf("duque de caxias")),
        CidadeDemo("Uberlândia", "MG", -18.9186, -48.2772, listOf("uberlandia")),
        CidadeDemo("Juiz de Fora", "MG", -21.7595, -43.3398, listOf("juiz de fora")),
        CidadeDemo("Londrina", "PR", -23.3045, -51.1696, listOf("londrina")),
        CidadeDemo("Maringá", "PR", -23.4205, -51.9333, listOf("maringa")),
        CidadeDemo("Joinville", "SC", -26.3044, -48.8487, listOf("joinville")),
        CidadeDemo("Blumenau", "SC", -26.9194, -49.0661, listOf("blumenau")),
        CidadeDemo("Caxias do Sul", "RS", -29.1634, -51.1797, listOf("caxias do sul")),
        CidadeDemo("Pelotas", "RS", -31.7654, -52.3376, listOf("pelotas")),
        CidadeDemo("Foz do Iguaçu", "PR", -25.5163, -54.5854, listOf("foz do iguacu")),
        CidadeDemo("Cascavel", "PR", -24.9578, -53.4595, listOf("cascavel")),
        CidadeDemo("Feira de Santana", "BA", -12.2664, -38.9663, listOf("feira de santana")),
        CidadeDemo("Ilhéus", "BA", -14.7935, -39.0460, listOf("ilheus")),
        CidadeDemo("Vitória da Conquista", "BA", -14.8619, -40.8444, listOf("vitoria da conquista")),
        CidadeDemo("Caruaru", "PE", -8.2833, -35.9761, listOf("caruaru")),
        CidadeDemo("Petrolina", "PE", -9.3891, -40.5027, listOf("petrolina")),
        CidadeDemo("Juazeiro do Norte", "CE", -7.2131, -39.3153, listOf("juazeiro do norte")),
        CidadeDemo("Marabá", "PA", -5.3686, -49.1178, listOf("maraba")),
        CidadeDemo("Santarém", "PA", -2.4385, -54.6996, listOf("santarem")),
        CidadeDemo("Anápolis", "GO", -16.3281, -48.9530, listOf("anapolis")),
        CidadeDemo("Rio Verde", "GO", -17.7923, -50.9192, listOf("rio verde")),
        CidadeDemo("Dourados", "MS", -22.2211, -54.8056, listOf("dourados")),
        CidadeDemo("Sinop", "MT", -11.8604, -55.5091, listOf("sinop")),
        CidadeDemo("Imperatriz", "MA", -5.5264, -47.4798, listOf("imperatriz")),
        CidadeDemo("Parnaíba", "PI", -2.9050, -41.7754, listOf("parnaiba")),
        CidadeDemo("Cabo Frio", "RJ", -22.8894, -42.0286, listOf("cabo frio")),
        CidadeDemo("Bauru", "SP", -22.3145, -49.0587, listOf("bauru")),
        CidadeDemo("Piracicaba", "SP", -22.7253, -47.6492, listOf("piracicaba")),
        CidadeDemo("Taubaté", "SP", -23.0264, -45.5553, listOf("taubate")),
        CidadeDemo("Volta Redonda", "RJ", -22.5202, -44.0996, listOf("volta redonda")),
        CidadeDemo("Barreiras", "BA", -12.1439, -44.9968, listOf("barreiras")),
        CidadeDemo("Aparecida de Goiânia", "GO", -16.8198, -49.2469, listOf("aparecida de goiania")),
        CidadeDemo("Canoas", "RS", -29.9178, -51.1836, listOf("canoas")),
        CidadeDemo("Ponta Grossa", "PR", -25.0916, -50.1668, listOf("ponta grossa")),
        CidadeDemo("Chapecó", "SC", -27.1004, -52.6152, listOf("chapeco")),
        CidadeDemo("Rio Grande", "RS", -32.0350, -52.0986, listOf("rio grande"))
    )
    val localInicial get() = cidades.first { it.nome == "São Paulo" }
    private val tipos = listOf("Enchente", "Incêndio", "Tempestade", "Deslizamento", "Seca", "Ciclone")
    private val niveis = listOf("Baixa", "Média", "Alta", "Crítica")
    val alertas: List<AlertaDemo> by lazy {
        cidades.filter { it.nome != "Brasil" }.flatMapIndexed { index, city ->
            (0 until 5).map { n ->
                val dLat = (((index * 7 + n * 11) % 19) - 9) * 0.008
                val dLng = (((index * 13 + n * 5) % 21) - 10) * 0.009
                val tipo = tipos[(index + n * 2) % tipos.size]
                val nivel = niveis[(index * 3 + n) % niveis.size]
                AlertaDemo("demo-${index}-${n}", city.nome, tipo, nivel, (city.lat + dLat).coerceIn(-33.7, 5.3), (city.lng + dLng).coerceIn(-73.9, -34.7), when (tipo) {
                    "Enchente" -> "Alagamento simulado em área urbana"; "Incêndio" -> "Foco de incêndio demonstrativo"; "Tempestade" -> "Tempestade severa simulada"; "Deslizamento" -> "Risco de encosta simulado"; "Seca" -> "Condição de seca demonstrativa"; else -> "Ventos fortes simulados"
                }, "Demonstração • hoje")
            }
        }
    }
    val abrigos: List<AbrigoDemo> by lazy {
        cidades.filter { it.nome != "Brasil" }.flatMapIndexed { index, city ->
            listOf(
                AbrigoDemo("abrigo-${index}-1", city.nome, "Ponto de apoio simulado • ${city.nome}", city.lat + 0.018, city.lng + 0.021),
                AbrigoDemo("abrigo-${index}-2", city.nome, "Abrigo demonstrativo • ${city.nome}", city.lat - 0.022, city.lng - 0.016)
            )
        }
    }
    fun normalizar(valor: String): String = java.text.Normalizer.normalize(valor.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()
    fun buscarLocal(query: String): CidadeDemo? {
        val q = normalizar(query)
        if (q.isBlank()) return null
        if (q in listOf("brasil", "brazil", "pais", "país", "todo brasil", "brasil inteiro")) return cidades.first()
        val candidatos = cidades.filter { it.nome != "Brasil" }
        return candidatos.firstOrNull { city ->
            val nomes = listOf(city.nome, city.uf) + city.aliases
            nomes.any { nome -> val n = normalizar(nome); n.isNotBlank() && (q == n || q.contains(n) || n.contains(q)) }
        } ?: candidatos.firstOrNull { city -> normalizar(city.uf) == q || q.endsWith(", ${normalizar(city.uf)}") }
    }
    fun alertasPerto(lat: Double, lng: Double, raioKm: Double = 500.0): List<AlertaDemo> = alertas.mapNotNull { a ->
        val d = calcularDistancia(lat, lng, a.lat, a.lng)
        if (d <= raioKm) a else null
    }.sortedBy { calcularDistancia(lat, lng, it.lat, it.lng) }
}

/** Mapa vetorial simplificado desenhado no próprio aparelho. Não baixa tiles nem usa chave. */
class OfflineBrasilOverlay(private val cidades: List<CidadeDemo>) : Overlay() {
    private val terra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(22, 57, 64); style = Paint.Style.FILL }
    private val borda = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(70, 151, 134); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val estrada = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(80, 115, 126); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val rio = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(35, 124, 164); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(206, 226, 222); textSize = 25f; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL) }
    private val contorno = listOf(
        GeoPoint(5.2,-60.7),GeoPoint(4.4,-51.6),GeoPoint(2.2,-50.3),GeoPoint(0.7,-49.0),GeoPoint(-1.5,-46.5),GeoPoint(-5.2,-35.2),GeoPoint(-10.5,-36.4),GeoPoint(-15.8,-39.0),GeoPoint(-18.2,-39.7),GeoPoint(-23.0,-42.0),GeoPoint(-25.5,-48.5),GeoPoint(-29.3,-49.8),GeoPoint(-33.7,-53.2),GeoPoint(-30.5,-57.6),GeoPoint(-27.2,-58.2),GeoPoint(-22.0,-57.8),GeoPoint(-17.0,-58.2),GeoPoint(-10.0,-65.0),GeoPoint(-7.0,-73.8),GeoPoint(-2.0,-70.0),GeoPoint(1.0,-67.0),GeoPoint(5.2,-60.7)
    )
    private fun point(proj: org.osmdroid.views.Projection, geo: GeoPoint): Point = proj.toPixels(geo, Point())
    private fun path(proj: org.osmdroid.views.Projection, pts: List<GeoPoint>): AndroidPath {
        val p = AndroidPath()
        pts.forEachIndexed { i, geo -> val xy = point(proj, geo); if (i == 0) p.moveTo(xy.x.toFloat(), xy.y.toFloat()) else p.lineTo(xy.x.toFloat(), xy.y.toFloat()) }
        return p
    }
    override fun draw(canvas: AndroidCanvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mapView.projection
        canvas.drawColor(android.graphics.Color.rgb(10, 24, 39))
        canvas.drawPath(path(proj, contorno), terra)
        canvas.drawPath(path(proj, contorno), borda)
        // Grade geográfica discreta
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(45, 103, 173, 173); strokeWidth = 1f }
        for (lat in -35..5 step 5) { val a = point(proj, GeoPoint(lat.toDouble(), -75.0)); val b = point(proj, GeoPoint(lat.toDouble(), -33.0)); canvas.drawLine(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), grid) }
        for (lng in -75..-35 step 5) { val a = point(proj, GeoPoint(-35.0, lng.toDouble())); val b = point(proj, GeoPoint(6.0, lng.toDouble())); canvas.drawLine(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), grid) }
        // Eixos rodoviários esquemáticos ligando capitais e grandes centros.
        val rotas = listOf(listOf("Manaus","Belém","São Luís","Fortaleza","Natal","João Pessoa","Recife","Maceió","Aracaju","Salvador"), listOf("Brasília","Goiânia","Belo Horizonte","Vitória","Rio de Janeiro","São Paulo","Curitiba","Florianópolis","Porto Alegre"), listOf("Brasília","Cuiabá","Campo Grande","São Paulo"), listOf("Brasília","Palmas","Belém"), listOf("Manaus","Porto Velho","Rio Branco"), listOf("Brasília","Teresina","Fortaleza"))
        rotas.forEach { nomes ->
            val pts = nomes.mapNotNull { n -> cidades.firstOrNull { it.nome == n }?.let { GeoPoint(it.lat, it.lng) } }
            if (pts.size > 1) canvas.drawPath(path(proj, pts), estrada)
        }
        val rioPts = listOf(GeoPoint(-3.1,-67.0),GeoPoint(-3.0,-62.0),GeoPoint(-3.4,-58.0),GeoPoint(-2.5,-54.7),GeoPoint(-1.5,-51.0),GeoPoint(-1.4,-48.5))
        canvas.drawPath(path(proj, rioPts), rio)
        val label = point(proj, GeoPoint(-11.5,-53.0)); canvas.drawText("BRASIL • MAPA OFFLINE", label.x.toFloat(), label.y.toFloat(), texto)
        if (mapView.zoomLevelDouble >= 5.5) {
            val nomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(226, 240, 236); textSize = 19f; setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK) }
            val area = canvas.clipBounds
            cidades.filter { it.nome != "Brasil" }.forEach { city ->
                val xy = point(proj, GeoPoint(city.lat, city.lng))
                if (area.contains(xy.x, xy.y)) {
                    canvas.drawCircle(xy.x.toFloat(), xy.y.toFloat(), 4.5f, borda)
                    canvas.drawText(city.nome, xy.x + 7f, xy.y - 6f, nomePaint)
                }
            }
        }
    }
}

// MODO DEMONSTRAÇÃO OFFLINE: sem chaves, tiles ou APIs externas no mapa/histórico.
@Composable
fun MapaScreenProfissional(navController: NavController, onVoltar: () -> Unit) {
    val context = LocalContext.current
    var busca by remember { mutableStateOf(SafePointDemoSession.local.nome) }
    var cidadeSelecionada by remember { mutableStateOf(SafePointDemoSession.local) }
    var filtroGravidade by remember { mutableStateOf(setOf("Crítica", "Alta", "Média", "Baixa")) }
    var filtroTipo by remember { mutableStateOf(setOf("Enchente", "Incêndio", "Tempestade", "Deslizamento", "Seca", "Ciclone")) }
    var mostrarAbrigos by remember { mutableStateOf(true) }
    var selecionado by remember { mutableStateOf<AlertaDemo?>(null) }
    var abrigoSelecionado by remember { mutableStateOf<AbrigoDemo?>(null) }
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember { mutableStateOf<RotaInfo?>(null) }
    var modoRota by remember { mutableStateOf(ModoRota.CARRO) }
    var origem by remember { mutableStateOf(GeoPoint(cidadeSelecionada.lat, cidadeSelecionada.lng)) }
    var gpsAtivo by remember { mutableStateOf(false) }
    var avisoBusca by remember { mutableStateOf("") }
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        temPermissao = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (temPermissao) obterLocalizacao(context) { lat, lng -> origem = GeoPoint(lat, lng); gpsAtivo = true }
        else Toast.makeText(context, "GPS não autorizado. O mapa continua funcionando em modo de demonstração.", Toast.LENGTH_LONG).show()
    }
    val mapView = remember {
        MapView(context.applicationContext).apply {
            setUseDataConnection(false)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setBackgroundColor(android.graphics.Color.rgb(10, 24, 39))
            controller.setZoom(4.3)
            controller.setCenter(GeoPoint(cidadeSelecionada.lat, cidadeSelecionada.lng))
            overlays.add(OfflineBrasilOverlay(SafePointDemoData.cidades))
        }
    }
    val alertasVisiveis = remember(filtroGravidade, filtroTipo) {
        SafePointDemoData.alertas.filter { it.gravidade in filtroGravidade && it.tipo in filtroTipo }
    }
    val abrigosVisiveis = remember(mostrarAbrigos) { if (mostrarAbrigos) SafePointDemoData.abrigos else emptyList() }

    fun pesquisarLocal() {
        val local = SafePointDemoData.buscarLocal(busca)
        if (local == null) {
            avisoBusca = "Local não encontrado no índice offline. Tente uma cidade brasileira, UF ou ‘Brasil’ — sem internet não há geocodificação de toda rua existente."
            Toast.makeText(context, avisoBusca, Toast.LENGTH_LONG).show()
            return
        }
        cidadeSelecionada = local
        SafePointDemoSession.local = local
        selecionado = null; abrigoSelecionado = null; rotaPontos = emptyList(); rotaInfo = null
        origem = GeoPoint(local.lat, local.lng)
        mapView.controller.animateTo(GeoPoint(local.lat, local.lng))
        mapView.controller.setZoom(if (local.nome == "Brasil") 4.3 else 10.5)
        mapView.invalidate()
        avisoBusca = "${local.nome}${if (local.uf.isNotBlank()) ", ${local.uf}" else ""} • alertas demonstrativos locais"
    }

    fun calcularRota(destinoLat: Double, destinoLng: Double, nome: String) {
        val lat1 = origem.latitude; val lng1 = origem.longitude
        val linhaReta = calcularDistancia(lat1, lng1, destinoLat, destinoLng)
        val fator = when (modoRota) { ModoRota.CARRO -> 1.28; ModoRota.A_PE -> 1.15; ModoRota.BICICLETA -> 1.20 }
        val km = linhaReta * fator
        val velocidade = when (modoRota) { ModoRota.CARRO -> 32.0; ModoRota.A_PE -> 4.5; ModoRota.BICICLETA -> 14.0 }
        val minutos = (km / velocidade * 60).toInt().coerceAtLeast(if (km < 0.15) 1 else 2)
        val duracao = if (minutos < 60) "$minutos min" else "${minutos / 60}h ${minutos % 60}min"
        val dist = if (km < 1) "${(km * 1000).toInt()} m" else String.format(Locale("pt", "BR"), "%.1f km", km)
        rotaInfo = RotaInfo(km, minutos, duracao, dist)
        // Traçado ilustrativo por interpolação local: não é uma rota viária nem navegação curva-a-curva.
        rotaPontos = (0..12).map { i ->
            val t = i / 12.0
            val curva = kotlin.math.sin(t * Math.PI) * 0.012
            GeoPoint(lat1 + (destinoLat - lat1) * t + curva, lng1 + (destinoLng - lng1) * t - curva * 0.65)
        }
        selecionado = null; abrigoSelecionado = null
        mapView.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(rotaPontos), true, 80)
        mapView.invalidate()
    }

    LaunchedEffect(Unit) {
        // Localização GPS é opcional; o modo demonstrativo não depende dela.
        if (temPermissao) obterLocalizacao(context) { lat, lng -> origem = GeoPoint(lat, lng); gpsAtivo = true }
    }
    DisposableEffect(temPermissao) {
        if (!temPermissao) return@DisposableEffect onDispose { }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                origem = GeoPoint(location.latitude, location.longitude)
                gpsAtivo = true
            }
            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                    try { if (locationManager.isProviderEnabled(provider)) locationManager.requestLocationUpdates(provider, 4000L, 8f, listener, android.os.Looper.getMainLooper()) } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
        onDispose { try { locationManager.removeUpdates(listener) } catch (_: Exception) { } }
    }
    DisposableEffect(Unit) { onDispose { try { mapView.onPause(); mapView.onDetach() } catch (_: Exception) {} } }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF3F6F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF75E0A2), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SafePoint • Mapa", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Demonstração offline • dados simulados", color = Color(0xFFBCE8D7), fontSize = 11.sp)
                    }
                    Surface(color = Color(0xFF6D2636), shape = RoundedCornerShape(8.dp)) { Text("SIMULAÇÃO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = busca, onValueChange = { busca = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Cidade, UF ou Brasil", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                    Spacer(Modifier.width(7.dp))
                    Button(onClick = { pesquisarLocal() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9A72))) { Icon(Icons.Default.Search, null); Text("Ir") }
                }
                Text(avisoBusca.ifBlank { "${cidadeSelecionada.nome}${if (cidadeSelecionada.uf.isNotBlank()) ", ${cidadeSelecionada.uf}" else ""} • ${alertasVisiveis.size} alertas no conjunto nacional" }, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
            // Filtros independentes e interativos
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = mostrarAbrigos, onClick = { mostrarAbrigos = !mostrarAbrigos }, label = { Text("🟢 Abrigos", fontSize = 11.sp) }) }
                items(listOf("Crítica", "Alta", "Média", "Baixa")) { nivel -> FilterChip(selected = nivel in filtroGravidade, onClick = { filtroGravidade = if (nivel in filtroGravidade) filtroGravidade - nivel else filtroGravidade + nivel }, label = { Text(nivel, fontSize = 11.sp) }) }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Enchente", "Incêndio", "Tempestade", "Deslizamento", "Seca", "Ciclone")) { tipo -> FilterChip(selected = tipo in filtroTipo, onClick = { filtroTipo = if (tipo in filtroTipo) filtroTipo - tipo else filtroTipo + tipo }, label = { Text(tipo, fontSize = 10.sp) }) }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
                    view.overlays.removeAll { it is Marker || it is Polyline }
                    if (view.overlays.none { it is OfflineBrasilOverlay }) view.overlays.add(0, OfflineBrasilOverlay(SafePointDemoData.cidades))
                    if (rotaPontos.isNotEmpty()) {
                        val linha = Polyline().apply { setPoints(rotaPontos); outlinePaint.color = android.graphics.Color.rgb(65, 160, 255); outlinePaint.strokeWidth = 9f; outlinePaint.strokeCap = Paint.Cap.ROUND; outlinePaint.strokeJoin = Paint.Join.ROUND }
                        view.overlays.add(linha)
                    }
                    alertasVisiveis.forEach { a ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(a.lat, a.lng); title = "${a.tipo} • ${a.gravidade}"; snippet = "SIMULAÇÃO • ${a.cidade} — ${a.descricao}"
                            icon = criarPinoDesastreNivel(context, when (a.gravidade) { "Crítica" -> "Red"; "Alta" -> "Orange"; "Média" -> "Yellow"; else -> "Green" }, 48)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ -> selecionado = a; abrigoSelecionado = null; true }
                        }; view.overlays.add(marker)
                    }
                    abrigosVisiveis.forEach { a ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(a.lat, a.lng); title = "🟢 ${a.nome}"; snippet = "SIMULAÇÃO — ponto demonstrativo, não é abrigo confirmado"
                            icon = criarPinoSeguranca(context, "shelter", Color(0xFF20A464), 44)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ -> abrigoSelecionado = a; selecionado = null; true }
                        }; view.overlays.add(marker)
                    }
                    if (gpsAtivo) {
                        val m = Marker(view).apply { position = origem; title = "Sua posição GPS"; snippet = "Localização fornecida pelo aparelho"; icon = criarPinoSeguranca(context, null, Color(0xFF1976D2), 42); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }
                        view.overlays.add(m)
                    }
                    view.invalidate()
                }
                Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallFloatingActionButton(onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble + 1); mapView.invalidate() }, containerColor = Color.White) { Icon(Icons.Default.Add, "Aumentar zoom", tint = Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble - 1); mapView.invalidate() }, containerColor = Color.White) { Icon(Icons.Default.Remove, "Diminuir zoom", tint = Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick = { if (!temPermissao) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) else obterLocalizacao(context) { lat, lng -> origem = GeoPoint(lat, lng); gpsAtivo = true; mapView.controller.animateTo(origem); mapView.controller.setZoom(14.0); mapView.invalidate() } }, containerColor = Color.White) { Icon(Icons.Default.MyLocation, "Minha localização", tint = Color(0xFF1976D2)) }
                    SmallFloatingActionButton(onClick = { mapView.controller.animateTo(GeoPoint(cidadeSelecionada.lat, cidadeSelecionada.lng)); mapView.controller.setZoom(if (cidadeSelecionada.nome == "Brasil") 4.3 else 11.0); mapView.invalidate() }, containerColor = Color.White) { Icon(Icons.Default.LocationOn, "Centralizar pesquisa", tint = Color(0xFF0B2545)) }
                }
                Surface(Modifier.align(Alignment.BottomStart).padding(8.dp), color = Color(0xE60B2545), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(Color(0xFFE53935), CircleShape)); Spacer(Modifier.width(5.dp)); Text("Crítica", color = Color.White, fontSize = 9.sp); Spacer(Modifier.width(8.dp)); Box(Modifier.size(8.dp).background(Color(0xFFFF9800), CircleShape)); Spacer(Modifier.width(4.dp)); Text("Alta", color = Color.White, fontSize = 9.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(Color(0xFFD5A51B), CircleShape)); Spacer(Modifier.width(5.dp)); Text("Média", color = Color.White, fontSize = 9.sp); Spacer(Modifier.width(8.dp)); Box(Modifier.size(8.dp).background(Color(0xFF43A047), CircleShape)); Spacer(Modifier.width(4.dp)); Text("Baixa", color = Color.White, fontSize = 9.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(Color(0xFF20A464), CircleShape)); Spacer(Modifier.width(5.dp)); Text("Abrigo simulado", color = Color.White, fontSize = 9.sp) }
                    }
                }
            }
            // Cartão selecionado e cálculo de deslocamento local
            val aSel = selecionado; val abrSel = abrigoSelecionado
            if (aSel != null || abrSel != null) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (aSel != null) "${aSel.tipo} • ${aSel.cidade}" else abrSel!!.nome, fontWeight = FontWeight.Bold, color = Color(0xFF0B2545), fontSize = 14.sp)
                        Text(if (aSel != null) "SIMULAÇÃO • ${aSel.gravidade} • ${aSel.descricao}" else "SIMULAÇÃO • este ponto não confirma um abrigo real aberto", fontSize = 11.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 7.dp)) {
                            listOf(ModoRota.CARRO, ModoRota.A_PE, ModoRota.BICICLETA).forEach { modo -> FilterChip(selected = modoRota == modo, onClick = { modoRota = modo; if (aSel != null) calcularRota(aSel.lat, aSel.lng, aSel.cidade) else calcularRota(abrSel!!.lat, abrSel.lng, abrSel.nome) }, label = { Text(modo.titulo, fontSize = 10.sp) }) }
                        }
                        Button(onClick = { if (aSel != null) calcularRota(aSel.lat, aSel.lng, aSel.cidade) else calcularRota(abrSel!!.lat, abrSel.lng, abrSel.nome) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1267A5))) { Icon(Icons.Default.Route, null); Spacer(Modifier.width(7.dp)); Text("Calcular deslocamento") }
                    }
                }
            }
            rotaInfo?.let { info ->
                Surface(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFE5F3FF)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, null, tint = Color(0xFF1267A5)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("Rota demonstrativa • ${modoRota.titulo}", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${info.distanciaTexto} • ${info.duracaoTexto}", fontSize = 12.sp, color = Color(0xFF1267A5)); Text("Estimativa em linha aproximada; não usa ruas/trânsito reais.", fontSize = 9.sp, color = Color.Gray) }; IconButton(onClick = { rotaInfo = null; rotaPontos = emptyList(); mapView.invalidate() }) { Icon(Icons.Default.Close, "Fechar rota") }
                    }
                }
            }
            Text("Pontos e perigos são fictícios para demonstração. Não use para decisões reais de emergência.", modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 9.sp, color = Color(0xFF8A3D28), textAlign = TextAlign.Center)
        }
    }
}

fun criarPinoDesastreNivel(context: Context, nivelAlerta: String, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val h = (w * 1.35f).toInt(); val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val canvas = AndroidCanvas(bmp)
    val cor = when (nivelAlerta) { "Red" -> android.graphics.Color.rgb(211,47,47); "Orange" -> android.graphics.Color.rgb(245,124,0); "Yellow" -> android.graphics.Color.rgb(213,165,27); else -> android.graphics.Color.rgb(56,142,60) }
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
    val w = tamanhoPx; val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888); val canvas = AndroidCanvas(bmp); val cx = w / 2f; val cy = w / 2f; val raio = w / 2f - 4f; val sombra = android.graphics.Paint().apply { color = android.graphics.Color.argb(80, 0, 0, 0); isAntiAlias = true }; canvas.drawCircle(cx, cy + 3f, raio, sombra)
    val circ = android.graphics.Paint().apply { color = android.graphics.Color.argb((corFundo.alpha * 255).toInt(), (corFundo.red * 255).toInt(), (corFundo.green * 255).toInt(), (corFundo.blue * 255).toInt()); isAntiAlias = true }; canvas.drawCircle(cx, cy, raio, circ)
    val borda = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = w * 0.07f; isAntiAlias = true }; canvas.drawCircle(cx, cy, raio - (w * 0.035f), borda)
    val texto = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = w * 0.55f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; isFakeBoldText = true }; val letra = when (categoria) { "hospital" -> "H"; "fire_station" -> "B"; "police" -> "P"; "shelter" -> "A"; "pharmacy" -> "F"; "place_of_worship" -> "R"; else -> "S" }; canvas.drawText(letra, cx, cy - (texto.descent() + texto.ascent()) / 2f, texto); return BitmapDrawable(context.resources, bmp)
}

@Composable fun ItemNavegacao(label: String, icon: ImageVector, ativo: Boolean, onClick: () -> Unit) { val cor = if (ativo) SafePointBlue else Color.Gray; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(12.dp, 4.dp)) { Icon(icon, null, tint = cor, modifier = Modifier.size(24.dp)); Text(label, fontSize = 11.sp, color = cor) } }

data class GdacsResponse(val features: List<GdacsFeature>); data class GdacsFeature(val properties: GdacsProperties, val geometry: GdacsGeometry); data class GdacsProperties(val eventtype: String, val name: String, val alertlevel: String, val pubDate: String? = null, val eventid: String? = null); data class GdacsGeometry(val coordinates: List<Double>)
interface GdacsApi { @retrofit2.http.GET("gdacsapi/api/events/geteventlist/SEARCH") suspend fun getDesastres(@retrofit2.http.Query("eventlist") tipos: String = "EQ;TC;FL;VO;DR;WF", @retrofit2.http.Query("alertlevel") nivel: String = "Green;Orange;Red", @retrofit2.http.Query("limit") limite: Int = 500): GdacsResponse }
object GdacsRepository { private val api = retrofit2.Retrofit.Builder().baseUrl("https://www.gdacs.org/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(GdacsApi::class.java); suspend fun buscarDesastres(tipos: String = "EQ;TC;FL;VO;DR;WF"): GdacsResponse = api.getDesastres(tipos = tipos) }

// Terremotos reais globais do USGS: últimos 30 dias, magnitude mínima 2,5.
data class TerremotoGlobal(val nome: String, val latitude: Double, val longitude: Double, val magnitude: Double, val timestamp: Long)
interface UsgsApi {
    @retrofit2.http.GET("fdsnws/event/1/query")
    suspend fun buscar(
        @retrofit2.http.Query("format") format: String = "geojson",
        @retrofit2.http.Query("starttime") starttime: String,
        @retrofit2.http.Query("minmagnitude") minmagnitude: Double = 2.5,
        @retrofit2.http.Query("orderby") orderby: String = "time",
        @retrofit2.http.Query("limit") limit: Int = 200
    ): okhttp3.ResponseBody
}
object UsgsRepository {
    private val client = okhttp3.OkHttpClient.Builder()
        .callTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", "SafePoint/1.2 (global earthquake alerts)").build()) }
        .build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://earthquake.usgs.gov/").client(client).build().create(UsgsApi::class.java)
    suspend fun buscarUltimos30Dias(): List<TerremotoGlobal> {
        val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val inicio = utc.format(Date(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L))
        val raiz = org.json.JSONObject(api.buscar(starttime = inicio).string())
        val features = raiz.optJSONArray("features") ?: return emptyList()
        val resultado = mutableListOf<TerremotoGlobal>()
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: continue
            val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val lon = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            val mag = props.optDouble("mag", Double.NaN)
            val time = props.optLong("time", 0L)
            if (lat.isFinite() && lon.isFinite() && mag.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0 && time > 0L) {
                resultado.add(TerremotoGlobal(props.optString("place", "Local não informado"), lat, lon, mag, time))
            }
        }
        return resultado
    }
}

data class OverpassResponse(val elements: List<OverpassElement>); data class OverpassElement(val type: String, val id: Long, val lat: Double? = null, val lon: Double? = null, val tags: Map<String, String>? = null, val center: OverpassCenter? = null); data class OverpassCenter(val lat: Double, val lon: Double)
interface OverpassApi { @retrofit2.http.GET("api/interpreter") suspend fun buscarPontos(@retrofit2.http.Query("data") query: String): OverpassResponse }
object OverpassRepository {
    private val httpClient = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent", "SafePoint/1.0 (contact: gu45363@gmail.com)").header("Accept-Language", "pt-BR,pt;q=0.9").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://overpass-api.de/").client(httpClient).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OverpassApi::class.java)
    suspend fun buscarPontosSeguranca(lat: Double, lng: Double, categoria: String, raioMetros: Int = 5000): OverpassResponse { val query = """[out:json][timeout:25];(node["amenity"="$categoria"](around:$raioMetros,$lat,$lng);way["amenity"="$categoria"](around:$raioMetros,$lat,$lng););out center;""".trimIndent(); return api.buscarPontos(query) }
}
data class NominatimResponse(val display_name: String, val address: Map<String, String>?); interface NominatimApi { @retrofit2.http.GET("reverse") suspend fun reverse(@retrofit2.http.Query("lat") lat: Double, @retrofit2.http.Query("lon") lon: Double, @retrofit2.http.Query("format") format: String = "jsonv2"): NominatimResponse }

data class NominatimSearchResult(val lat: String, val lon: String, val display_name: String)
interface NominatimSearchApi {
    @retrofit2.http.GET("search")
    suspend fun search(@retrofit2.http.Query("q") query: String, @retrofit2.http.Query("format") format: String = "json", @retrofit2.http.Query("limit") limit: Int = 5): List<NominatimSearchResult>
}
object NominatimRepository {
    private val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent", "SafePoint/1.0 (contact: gu45363@gmail.com)").header("Accept-Language", "pt-BR,pt;q=0.9").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://nominatim.openstreetmap.org/").client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(NominatimApi::class.java)
    private val searchApi = retrofit2.Retrofit.Builder().baseUrl("https://nominatim.openstreetmap.org/").client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(NominatimSearchApi::class.java)
    suspend fun getEndereco(lat: Double, lng: Double): String { return try { api.reverse(lat, lng).display_name } catch (e: Exception) { "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}" } }
    suspend fun buscarPorNome(query: String): List<NominatimSearchResult> { return try { searchApi.search(query) } catch (e: Exception) { emptyList() } }
}

/** Roteamento para carro, bicicleta e caminhada. Usa Valhalla público; em indisponibilidade, a UI abre Google Maps. */
interface ValhallaApi { @retrofit2.http.GET("route") suspend fun route(@retrofit2.http.Query("json") requestJson: String): okhttp3.ResponseBody }
object ValhallaRepository {
    private val client = okhttp3.OkHttpClient.Builder().callTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://valhalla1.openstreetmap.de/").client(client).build().create(ValhallaApi::class.java)
    suspend fun buscarRotaCompleta(lon1: Double, lat1: Double, lon2: Double, lat2: Double, costing: String): RotaCompleta {
        val json = org.json.JSONObject().apply {
            put("locations", org.json.JSONArray().put(org.json.JSONObject().put("lat", lat1).put("lon", lon1).put("type", "break")).put(org.json.JSONObject().put("lat", lat2).put("lon", lon2).put("type", "break")))
            put("costing", costing); put("units", "kilometers")
            put("directions_options", org.json.JSONObject().put("units", "kilometers"))
        }
        val root = org.json.JSONObject(api.route(json.toString()).string())
        val trip = root.optJSONObject("trip") ?: throw IllegalStateException("Resposta de rota inválida")
        val legs = trip.optJSONArray("legs") ?: throw IllegalStateException("Rota sem trechos")
        val shape = legs.optJSONObject(0)?.optString("shape").orEmpty()
        val summary = trip.optJSONObject("summary") ?: throw IllegalStateException("Rota sem resumo")
        val points = decodePolyline6(shape)
        return RotaCompleta(points, summary.optDouble("length", 0.0) * 1000.0, summary.optDouble("time", 0.0))
    }
    private fun decodePolyline6(encoded: String): List<GeoPoint> {
        val result = ArrayList<GeoPoint>(); var index = 0; var lat = 0; var lon = 0
        while (index < encoded.length) {
            fun readValue(): Int {
                var shift = 0; var value = 0; var b: Int
                do { if (index >= encoded.length) throw IllegalArgumentException("Polyline incompleta"); b = encoded[index++].code - 63; value = value or ((b and 0x1f) shl shift); shift += 5 } while (b >= 0x20)
                return if ((value and 1) != 0) (value shr 1).inv() else value shr 1
            }
            lat += readValue(); lon += readValue()
            result.add(GeoPoint(lat / 1_000_000.0, lon / 1_000_000.0))
        }
        return result
    }
}

data class OsrmResponse(val routes: List<OsrmRoute>); data class OsrmRoute(val geometry: OsrmGeometry, val distance: Double, val duration: Double); data class OsrmGeometry(val coordinates: List<List<Double>>)
data class RotaCompleta(val pontos: List<GeoPoint>, val distanciaMetros: Double, val duracaoSeg: Double)
interface OsrmApi { @retrofit2.http.GET("route/v1/driving/{coords}") suspend fun getRoute(@retrofit2.http.Path("coords", encoded = true) coords: String, @retrofit2.http.Query("overview") overview: String = "full", @retrofit2.http.Query("geometries") geometries: String = "geojson"): OsrmResponse }
object OsrmRepository {
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://router.project-osrm.org/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OsrmApi::class.java)
    suspend fun buscarRota(lon1: Double, lat1: Double, lon2: Double, lat2: Double): List<GeoPoint> {
        return buscarRotaCompleta(lon1, lat1, lon2, lat2).pontos
    }
    suspend fun buscarRotaCompleta(lon1: Double, lat1: Double, lon2: Double, lat2: Double): RotaCompleta {
        val coords = "$lon1,$lat1;$lon2,$lat2"
        val resp = api.getRoute(coords)
        val route = resp.routes.firstOrNull() ?: return RotaCompleta(emptyList(), 0.0, 0.0)
        val pontos = route.geometry.coordinates.map { GeoPoint(it[1], it[0]) }
        return RotaCompleta(pontos, route.distance, route.duration)
    }
}



// AdicionarDesastreDialog removido conforme solicitado - mapa agora é GPS profissional



// HISTÓRICO REAL - 100% LOCALIZAÇÃO REAL + GDACS REAL - NADA FICTÍCIO

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TelaHistoricoReal(navController: NavController) {
    val context = LocalContext.current
    var buscaCidade by remember { mutableStateOf("São Paulo") }
    var cidadeAtual by remember { mutableStateOf(SafePointDemoData.localInicial) }
    var filtroGravidade by remember { mutableStateOf("Todos") }
    var filtroTipo by remember { mutableStateOf("Todos") }
    var mostrarAbrigos by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("Alertas de demonstração • dados simulados") }
    val opcoesGravidade = listOf("Todos", "Crítica", "Alta", "Média", "Baixa")
    val opcoesTipo = listOf("Todos", "Enchente", "Incêndio", "Tempestade", "Deslizamento", "Seca", "Ciclone")
    val lista = remember(cidadeAtual, filtroGravidade, filtroTipo, mostrarAbrigos) {
        val base = if (cidadeAtual.nome == "Brasil") SafePointDemoData.alertas else SafePointDemoData.alertasPerto(cidadeAtual.lat, cidadeAtual.lng, 500.0)
        base.filter { (filtroGravidade == "Todos" || it.gravidade == filtroGravidade) && (filtroTipo == "Todos" || it.tipo == filtroTipo) }
    }
    fun pesquisar() {
        val local = SafePointDemoData.buscarLocal(buscaCidade)
        if (local == null) {
            mensagem = "Local não encontrado no índice offline. Tente cidade brasileira, UF ou Brasil; ruas são reconhecidas quando o nome da cidade está na pesquisa."
            Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show()
        } else {
            cidadeAtual = local
            mensagem = "${local.nome}${if (local.uf.isNotBlank()) ", ${local.uf}" else ""} • ${if (local.nome == "Brasil") SafePointDemoData.alertas.size else SafePointDemoData.alertasPerto(local.lat, local.lng).size} alertas simulados no raio de 500 km"
        }
    }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF2F5F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).statusBarsPadding().padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = Color(0xFF83E3B1), modifier = Modifier.size(30.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text("Histórico de Alertas", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Pesquisa nacional • funciona offline", color = Color(0xFFC9E8DF), fontSize = 11.sp) }
                    Surface(color = Color(0xFF6D2636), shape = RoundedCornerShape(8.dp)) { Text("SIMULAÇÃO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 5.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = buscaCidade, onValueChange = { buscaCidade = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Cidade, rua + cidade, UF ou Brasil", fontSize = 11.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.width(7.dp)); Button(onClick = { pesquisar() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16845F)), shape = RoundedCornerShape(12.dp)) { Text("Buscar") }
            }
            Text(mensagem, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = Color(0xFF4D6475), fontSize = 10.sp)
            Text("Gravidade", modifier = Modifier.padding(start = 16.dp, top = 6.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(opcoesGravidade) { f -> FilterChip(selected = filtroGravidade == f, onClick = { filtroGravidade = f }, label = { Text(f, fontSize = 10.sp) }) } }
            Text("Tipo de ocorrência", modifier = Modifier.padding(start = 16.dp, top = 2.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(opcoesTipo) { f -> FilterChip(selected = filtroTipo == f, onClick = { filtroTipo = f }, label = { Text(f, fontSize = 10.sp) }) } }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${lista.size} registros simulados", color = Color(0xFF0B2545), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { filtroGravidade = "Todos"; filtroTipo = "Todos" }) { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Limpar filtros", fontSize = 10.sp) }
            }
            if (lista.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FilterList, null, tint = Color.Gray, modifier = Modifier.size(42.dp)); Text("Nenhum registro com esses filtros", color = Color.DarkGray, fontWeight = FontWeight.Bold); Text("Altere o tipo ou nível de gravidade.", color = Color.Gray, fontSize = 11.sp) } }
            } else LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(lista, key = { it.id }) { alerta ->
                    val cor = when (alerta.gravidade) { "Crítica" -> Color(0xFFD92D36); "Alta" -> Color(0xFFED8A19); "Média" -> Color(0xFFD5A51B); else -> Color(0xFF269C69) }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable { SafePointDemoSession.local = SafePointDemoData.buscarLocal(alerta.cidade) ?: cidadeAtual; navController.navigate(Screen.Mapa.route) }) {
                        Column(Modifier.padding(13.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(10.dp).padding(top = 2.dp).background(cor, CircleShape)); Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) { Text("${alerta.tipo} • ${alerta.cidade}", color = Color(0xFF0B2545), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(alerta.descricao, color = Color(0xFF5F6F7D), fontSize = 11.sp) }
                                Surface(color = cor, shape = RoundedCornerShape(8.dp)) { Text(alerta.gravidade, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) }
                            }
                            Spacer(Modifier.height(7.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF16845F), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("${alerta.cidade} • coordenada demonstrativa", color = Color(0xFF16845F), fontSize = 10.sp); Spacer(Modifier.weight(1f)); Text(alerta.dataHora, color = Color.Gray, fontSize = 9.sp) }
                            Text("SIMULAÇÃO — toque para abrir o mapa nesta região", color = Color(0xFF9B4A35), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Text("Os alertas e os abrigos desta tela são fictícios. Não representam perigos atuais nem locais de acolhimento confirmados.", modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), textAlign = TextAlign.Center, color = Color(0xFF8A3D28), fontSize = 9.sp)
        }
    }
}

object SafePointDemoSession { var local: CidadeDemo = SafePointDemoData.localInicial }

fun traduzirNomeEvento(nomeFonte: String, tipoPortuguese: String): String {
    var nome = nomeFonte.trim()
    val substituicoes = listOf(
        Regex("(?i)^earthquake(?:s)?\\s+in\\s+") to "Terremoto em ",
        Regex("(?i)^flood(?:ing)?\\s+in\\s+") to "Enchente em ",
        Regex("(?i)^wildfire(?:s)?\\s+in\\s+") to "Incêndio florestal em ",
        Regex("(?i)^drought\\s+in\\s+") to "Seca em ",
        Regex("(?i)^volcanic eruption\\s+in\\s+") to "Erupção vulcânica em ",
        Regex("(?i)^volcano(?:es)?\\s+in\\s+") to "Vulcão em ",
        Regex("(?i)^tropical cyclone\\s*") to "Ciclone tropical ",
        Regex("(?i)^tsunami\\s+in\\s+") to "Tsunami em ",
        Regex("(?i)^severe weather\\s+in\\s+") to "Tempestade severa em "
    )
    for ((regex, substituto) in substituicoes) {
        if (regex.containsMatchIn(nome)) { nome = regex.replaceFirst(nome, substituto); break }
    }
    return nome.ifBlank { tipoPortuguese }
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

fun criarIconeLocalizacao(tamanhoPx: Int): android.graphics.Bitmap {
    val size = tamanhoPx.coerceAtLeast(24)
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val centro = size / 2f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.FILL }
    canvas.drawCircle(centro, centro, size * 0.43f, paint)
    paint.color = android.graphics.Color.rgb(25, 118, 210)
    canvas.drawCircle(centro, centro, size * 0.34f, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centro, centro, size * 0.10f, paint)
    return bitmap
}

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double { val R = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1); val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2); val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); return R * c }



fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return
    try {
        val gps = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val net = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val best = listOfNotNull(gps, net).maxByOrNull { it.time }
        if (best != null) {
            onLocationReceived(best.latitude, best.longitude)
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter {
            try { lm.isProviderEnabled(it) } catch (_: Exception) { false }
        }
        if (providers.isEmpty()) return
        var entregue = false
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                if (!entregue) {
                    entregue = true
                    onLocationReceived(location.latitude, location.longitude)
                    try { lm.removeUpdates(this) } catch (_: Exception) { }
                }
            }
            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }
        providers.forEach { provider ->
            try { lm.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper()) } catch (_: Exception) { }
        }
    } catch (_: Exception) { /* sem localização disponível; não inventar coordenadas */ }
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
        "Como ver meu histórico de alertas?" to "Toque em “Histórico” na barra inferior. Pesquise uma cidade brasileira ou Brasil, depois use os filtros por gravidade e tipo. Os registros são demonstrativos e funcionam offline; não são alertas reais.",
        "Como editar meu perfil?" to "Toque em 'Perfil' na barra inferior. Toque na foto para trocar (escolha da galeria). Edite nome, e-mail, telefone e data de nascimento. Toque em 'Salvar Alterações'. Para sair da conta, toque em 'Sair'. Seus dados ficam salvos no Firebase com segurança.",
        "Preciso permitir localização?" to "Sim, é essencial. Sem localização, o mapa não mostra onde você está, não calcula rotas seguras e não encontra abrigos próximos. Na primeira vez, o app pede permissão. Se negar, vá em Configurações do celular > Apps > SafePoint > Permissões > Localização > Permitir sempre.",
        "O mapa está cinza ou não carrega?" to "Verifique: 1) Internet ativa, 2) Localização permitida, 3) GPS ligado. O mapa usa OSM HOT gratuito, sem API Key, então não deve dar 'API KEY REQUIRED'. Se ainda falhar, feche e abra o app. O app salva cache local para funcionar offline por um tempo.",
        "Como funciona o sistema de alertas?" to "O mapa e o histórico atuais funcionam em modo de demonstração offline. Os pontos de perigo e abrigos são simulados e identificados como SIMULAÇÃO; não representam ocorrências ou abrigos reais. As rotas são estimativas geométricas e não substituem navegação viária ou orientações da Defesa Civil."
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
