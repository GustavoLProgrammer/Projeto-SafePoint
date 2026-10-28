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
                    composable(Screen.Mapa.route) { MapaScreenProfissional(onVoltar = { navController.popBackStack() }) }
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
                Text("1. OBJETIVO E ACEITAÇÃO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O SafePoint é um sistema inteligente de segurança, evacuação e resposta a desastres naturais, que utiliza mapas reais, dados de localização exata e integração com serviços de emergência. Ao criar uma conta e utilizar o aplicativo, você declara ter lido, compreendido e concordado integralmente com estes Termos de Uso e Política de Privacidade. Se não concordar, não utilize o aplicativo.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("2. CADASTRO E RESPONSABILIDADE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Você se compromete a fornecer dados verídicos, completos e atualizados no cadastro (nome, e-mail, telefone, data de nascimento). É responsável por manter a confidencialidade de sua senha e por todas as atividades realizadas em sua conta.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("3. LOCALIZAÇÃO EXATA - CONSENTIMENTO EXPRESSO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("Para funcionamento essencial, coletamos e processamos sua localização exata (GPS fino e grosso), mesmo em segundo plano quando autorizado, para: (a) exibir sua posição no mapa real; (b) calcular rotas seguras; (c) identificar abrigos, hospitais, bombeiros, polícia e farmácias próximos via Overpass; (d) gerar histórico de alertas regionais com distância em km; (e) melhorar alertas de desastres com base em sua região. Dados são criptografados, armazenados no Firebase Firestore e nunca vendidos.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("4. MAPA REAL PROFISSIONAL - TECNOLOGIA SEM 403", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O mapa utiliza as ruas do OpenStreetMap Standard, com cache local e identificação do aplicativo no acesso aos mapas. Não depende de uma chave da API do Google Maps. As informações de desastres e pontos de segurança vêm de serviços públicos, cuja disponibilidade depende da conexão e das fontes de dados.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
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

@Composable
fun MapaScreenProfissional(onVoltar: () -> Unit) {
    val context = LocalContext.current
    var filtrosDesastres by remember { mutableStateOf(setOf("EQ","TC","FL","VO","DR","WF")) }
    var filtrosSeguranca by remember { mutableStateOf(setOf("hospital","fire_station","police","shelter","pharmacy","place_of_worship")) }
    val pontosSeguranca = remember { mutableStateListOf<Ocorrencia>() }
    val desastresGlobais = remember { mutableStateListOf<DesastreDetalhado>() }
    var buscaCidade by remember { mutableStateOf("") }
    var localizacaoAtual by remember { mutableStateOf<GeoPoint?>(null) }
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember { mutableStateOf<RotaInfo?>(null) }
    var ocorrenciaSelecionada by remember { mutableStateOf<Ocorrencia?>(null) }
    var desastreSelecionado by remember { mutableStateOf<DesastreDetalhado?>(null) }
    var carregandoRota by remember { mutableStateOf(false) }
    var modoRota by remember { mutableStateOf(ModoRota.CARRO) }
    var enderecoAtual by remember { mutableStateOf("") }
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var permissaoJaPedida by remember { mutableStateOf(PrefsHelper.isLocalizacaoAceita(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms -> 
        temPermissao = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (temPermissao) PrefsHelper.setLocalizacaoAceita(context, true)
    }

    // Mapa sem chave de API. Usa o endpoint oficial HTTPS do OpenStreetMap,
    // com User-Agent identificável definido em MainActivity.onCreate e cache local do osmdroid.
    // Atribuição visível obrigatória: © OpenStreetMap contributors.
    val tileSource = remember { XYTileSource("SafePoint OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors") }
    val mapView = remember { try { MapView(context.applicationContext).apply { setTileSource(tileSource); setMultiTouchControls(true); isTilesScaledToDpi = true; setUseDataConnection(true); controller.setZoom(4.5); controller.setCenter(GeoPoint(-14.2350, -51.9253)) } } catch (e: Exception) { MapView(context.applicationContext) } }
    val myLocationOverlay = remember { try { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { enableMyLocation() } } catch (e: Exception) { null } }

    fun buscarPontosProximos(lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val encontrados = mutableListOf<Ocorrencia>()
            val categorias = listOf("hospital", "fire_station", "police", "pharmacy", "shelter", "place_of_worship")
            for (cat in categorias) {
                try {
                    val res = OverpassRepository.buscarPontosSeguranca(lat, lng, cat, 8000)
                    res.elements.forEach { el ->
                        val plat = el.lat ?: el.center?.lat ?: 0.0
                        val plon = el.lon ?: el.center?.lon ?: 0.0
                        if (plat != 0.0 && plon != 0.0) {
                            val nome = el.tags?.get("name") ?: "${NOMES_SEGURANCA[cat] ?: cat} próximo"
                            encontrados.add(Ocorrencia(nome, "${NOMES_SEGURANCA[cat] ?: cat} - cadastro do OpenStreetMap", plat, plon, TipoOcorrencia.SEGURANCA, cat))
                        }
                    }
                } catch (_: Exception) { /* uma categoria indisponível não impede as outras */ }
            }
            withContext(Dispatchers.Main) {
                pontosSeguranca.clear()
                pontosSeguranca.addAll(encontrados.distinctBy { "${it.categoria}:${it.lat}:${it.lng}" })
            }
        }
    }

    fun buscarEnderecoDigitado(query: String) {
        if (query.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultado = NominatimRepository.buscarPorNome(query)
                val queryNormalizada = query.trim().lowercase(Locale.ROOT).replace("ã", "a").replace("á", "a")
                val primeiro = if (queryNormalizada.startsWith("sao paulo")) resultado.firstOrNull { it.display_name.contains("Brasil", ignoreCase = true) && it.display_name.contains("São Paulo", ignoreCase = true) } ?: resultado.firstOrNull() else resultado.firstOrNull()
                if (primeiro != null) {
                    withContext(Dispatchers.Main) {
                        val ponto = GeoPoint(primeiro.lat.toDouble(), primeiro.lon.toDouble())
                        try { mapView.controller.animateTo(ponto); mapView.controller.setZoom(16.0) } catch (_: Exception) {}
                        localizacaoAtual = ponto
                        buscarPontosProximos(primeiro.lat.toDouble(), primeiro.lon.toDouble())
                        try {
                            val resp = GdacsRepository.buscarDesastres(tipos = if (filtrosDesastres.isEmpty()) "EQ;TC;FL;VO;DR;WF" else filtrosDesastres.joinToString(";"))
                            desastresGlobais.clear()
                            resp.features.forEach { feature ->
                                val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0
                                val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0
                                val dist = calcularDistancia(ponto.latitude, ponto.longitude, lat, lon)
                                if (dist <= 500.0) {
                                    val grav = when (feature.properties.alertlevel) { "Red" -> "Crítica"; "Orange" -> "Alta"; "Green" -> "Baixa"; else -> "Baixa" }
                                    val tipoNome = NOMES_DESASTRES[feature.properties.eventtype] ?: feature.properties.eventtype
                                    val oc = Ocorrencia("${tipoNome}: ${feature.properties.name}", "Alerta ${feature.properties.alertlevel} - ${feature.properties.name} | ${String.format("%.0f km de %s", dist, query)}", lat, lon, TipoOcorrencia.DESASTRE, feature.properties.eventtype)
                                    desastresGlobais.add(DesastreDetalhado(oc, feature.properties.alertlevel, grav, feature.properties.eventtype, tipoNome, "GDACS"))
                                }
                            }
                            if ("EQ" in filtrosDesastres) {
                                try {
                                    UsgsRepository.buscarUltimos30Dias().filter { it.magnitude >= 4.5 }.forEach { quake ->
                                        val dist = calcularDistancia(ponto.latitude, ponto.longitude, quake.latitude, quake.longitude)
                                        if (dist <= 500.0) {
                                            val nivel = when { quake.magnitude >= 6.5 -> "Red"; quake.magnitude >= 5.0 -> "Orange"; else -> "Green" }
                                            val gravidade = when (nivel) { "Red" -> "Crítica"; "Orange" -> "Alta"; else -> "Média" }
                                            val nome = "Terremoto M${String.format(Locale.US, "%.1f", quake.magnitude)}: ${quake.nome}"
                                            val oc = Ocorrencia(nome, "USGS • magnitude ${String.format(Locale.US, "%.1f", quake.magnitude)} • ${String.format(Locale("pt", "BR"), "%.0f km", dist)} da busca", quake.latitude, quake.longitude, TipoOcorrencia.DESASTRE, "EQ")
                                            desastresGlobais.add(DesastreDetalhado(oc, nivel, gravidade, "EQ", "Terremoto", "USGS"))
                                        }
                                    }
                                } catch (_: Exception) { /* mantém os eventos GDACS se o USGS estiver indisponível */ }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun tracarRotaAte(destino: GeoPoint, nomeDestino: String = "") {
        val origem = localizacaoAtual ?: return
        carregandoRota = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rotaCompleta = ValhallaRepository.buscarRotaCompleta(
                    origem.longitude, origem.latitude, destino.longitude, destino.latitude, modoRota.valhallaCosting
                )
                if (rotaCompleta.pontos.isEmpty()) throw IllegalStateException("A fonte de rotas não retornou um caminho")
                withContext(Dispatchers.Main) {
                    rotaPontos = rotaCompleta.pontos
                    val distKm = rotaCompleta.distanciaMetros / 1000.0
                    val durMin = (rotaCompleta.duracaoSeg / 60).toInt().coerceAtLeast(1)
                    val durTexto = if (durMin < 60) "${durMin} min" else "${durMin/60}h ${durMin%60}min"
                    val distTexto = if (distKm < 1) "${(distKm*1000).toInt()} m" else String.format(Locale("pt", "BR"), "%.1f km", distKm)
                    rotaInfo = RotaInfo(distKm, durMin, durTexto, distTexto)
                    carregandoRota = false
                    try { mapView.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(rotaPontos), true, 120) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    carregandoRota = false
                    Toast.makeText(context, "Não consegui desenhar a rota no mapa. Abrindo navegação externa.", Toast.LENGTH_LONG).show()
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${origem.latitude},${origem.longitude}&destination=${destino.latitude},${destino.longitude}&travelmode=${modoRota.googleMode}")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { Toast.makeText(context, "Não foi possível abrir o app de mapas.", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    fun irParaAbrigoMaisProximo() {
        val origem = localizacaoAtual
        if (origem == null) {
            Toast.makeText(context, "Aguardando sua localização GPS. Verifique se o GPS está ligado.", Toast.LENGTH_LONG).show()
            return
        }
        val existente = pontosSeguranca.filter { it.categoria == "shelter" }
            .minByOrNull { calcularDistancia(origem.latitude, origem.longitude, it.lat, it.lng) }
        if (existente != null) {
            ocorrenciaSelecionada = existente
            desastreSelecionado = null
            tracarRotaAte(GeoPoint(existente.lat, existente.lng), existente.titulo)
            return
        }
        Toast.makeText(context, "Buscando abrigos mapeados em um raio maior…", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            val encontrados = try { OverpassRepository.buscarPontosSeguranca(origem.latitude, origem.longitude, "shelter", 20000).elements.mapNotNull { el ->
                val lat = el.lat ?: el.center?.lat
                val lon = el.lon ?: el.center?.lon
                if (lat == null || lon == null || lat == 0.0 || lon == 0.0) null
                else Ocorrencia(el.tags?.get("name") ?: "Abrigo mapeado", "Cadastro do OpenStreetMap; confirme se está aberto e operacional", lat, lon, TipoOcorrencia.SEGURANCA, "shelter")
            } } catch (_: Exception) { emptyList() }
            withContext(Dispatchers.Main) {
                pontosSeguranca.addAll(encontrados)
                val candidato = encontrados.minByOrNull { calcularDistancia(origem.latitude, origem.longitude, it.lat, it.lng) }
                if (candidato != null) {
                    ocorrenciaSelecionada = candidato
                    desastreSelecionado = null
                    tracarRotaAte(GeoPoint(candidato.lat, candidato.lng), candidato.titulo)
                } else {
                    Toast.makeText(context, "Nenhum abrigo formal foi encontrado nos dados mapeados até 20 km. Consulte a Defesa Civil local; não vou inventar um abrigo.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(temPermissao) {
        if (temPermissao) {
            try {
                if (mapView.overlays.none { it is MyLocationNewOverlay } && myLocationOverlay != null) mapView.overlays.add(myLocationOverlay)
                obterLocalizacao(context) { lat, lng ->
                    val ponto = GeoPoint(lat, lng)
                    localizacaoAtual = ponto
                    CoroutineScope(Dispatchers.IO).launch { try { enderecoAtual = NominatimRepository.getEndereco(lat, lng) } catch (_: Exception) {} }
                    try { mapView.controller.animateTo(ponto); mapView.controller.setZoom(16.0) } catch (e: Exception) {}
                    buscarPontosProximos(lat, lng)
                }
            } catch (e: Exception) {}
        }
    }
    DisposableEffect(temPermissao) {
        if (!temPermissao) return@DisposableEffect onDispose { }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                // Atualiza a posição usada nos cálculos sem mover o mapa à força enquanto a pessoa navega.
                localizacaoAtual = GeoPoint(location.latitude, location.longitude)
            }
            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                    try { if (lm.isProviderEnabled(provider)) lm.requestLocationUpdates(provider, 3000L, 5f, listener, android.os.Looper.getMainLooper()) } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
        onDispose { try { lm.removeUpdates(listener) } catch (_: Exception) { } }
    }
    LaunchedEffect(filtrosDesastres) {
        if (filtrosDesastres.isEmpty()) { desastresGlobais.clear(); return@LaunchedEffect }
        try {
            val resposta = GdacsRepository.buscarDesastres(tipos = filtrosDesastres.joinToString(";"))
            desastresGlobais.clear()
            resposta.features.forEach { feature ->
                val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0
                val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0
                val grav = when (feature.properties.alertlevel) { "Red" -> "Crítica"; "Orange" -> "Alta"; "Green" -> "Baixa"; else -> "Baixa" }
                val tipoNome = NOMES_DESASTRES[feature.properties.eventtype] ?: feature.properties.eventtype
                val oc = Ocorrencia("$tipoNome: ${feature.properties.name}", "Alerta: ${feature.properties.alertlevel} - Evento real GDACS global", lat, lon, TipoOcorrencia.DESASTRE, feature.properties.eventtype)
                desastresGlobais.add(DesastreDetalhado(oc, feature.properties.alertlevel, grav, feature.properties.eventtype, tipoNome, "GDACS"))
            }
            if ("EQ" in filtrosDesastres) {
                try {
                    UsgsRepository.buscarUltimos30Dias().filter { it.magnitude >= 4.5 }.forEach { quake ->
                        val nivel = when { quake.magnitude >= 6.5 -> "Red"; quake.magnitude >= 5.0 -> "Orange"; else -> "Green" }
                        val gravidade = when (nivel) { "Red" -> "Crítica"; "Orange" -> "Alta"; else -> "Média" }
                        val nome = "Terremoto M${String.format(Locale.US, "%.1f", quake.magnitude)}: ${quake.nome}"
                        val oc = Ocorrencia(nome, "USGS • magnitude ${String.format(Locale.US, "%.1f", quake.magnitude)} • últimos 30 dias", quake.latitude, quake.longitude, TipoOcorrencia.DESASTRE, "EQ")
                        desastresGlobais.add(DesastreDetalhado(oc, nivel, gravidade, "EQ", "Terremoto", "USGS"))
                    }
                } catch (_: Exception) { /* se USGS falhar, os alertas GDACS continuam disponíveis */ }
            }
        } catch (_: Exception) {}
    }
    DisposableEffect(Unit) { try { mapView.onResume() } catch (_: Exception) {}; onDispose { try { mapView.onPause() } catch (_: Exception) {}; try { myLocationOverlay?.disableMyLocation() } catch (_: Exception) {} } }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
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
                val desastresFiltrados = desastresGlobais.filter { detalhe -> detalhe.tipoCode in filtrosDesastres && localizacaoAtual?.let { calcularDistancia(it.latitude, it.longitude, detalhe.ocorrencia.lat, detalhe.ocorrencia.lng) <= 250.0 } == true }
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
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) { Text("Mapa de segurança", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = azulEscuro); Text("Mapa Voyager • navegação GPS", fontSize = 10.sp, color = Color(0xFF64748B)) }
                Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = onVoltar, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, null, tint = azulEscuro) }
            }
            OutlinedTextField(value = buscaCidade, onValueChange = { buscaCidade = it }, placeholder = { Text("Buscar cidade, endereço ou local", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = azulEscuro, modifier = Modifier.size(20.dp)) }, trailingIcon = { if (buscaCidade.isNotBlank()) IconButton(onClick = { buscarEnderecoDigitado(buscaCidade) }) { Icon(Icons.Default.ArrowForward, null, tint = SafePointGreen) } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ModoRota.values().forEach { modo ->
                    FilterChip(selected = modoRota == modo, onClick = { modoRota = modo }, label = { Text(modo.titulo, fontSize = 11.sp) }, leadingIcon = { Icon(when (modo) { ModoRota.CARRO -> Icons.Default.DirectionsCar; ModoRota.A_PE -> Icons.Default.DirectionsWalk; ModoRota.BICICLETA -> Icons.Default.DirectionsBike }, null, modifier = Modifier.size(16.dp)) })
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { irParaAbrigoMaisProximo() }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF167C43))) {
                    Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Abrigo", fontSize = 11.sp)
                }
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                item { Surface(shape = RoundedCornerShape(20.dp), color = if (filtrosDesastres.size == 6) Color(0xFFE3F2FD) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosDesastres = if (filtrosDesastres.size == 6) emptySet() else setOf("EQ","TC","FL","VO","DR","WF") }) { Text("Todos perigos", fontSize = 11.sp, modifier = Modifier.padding(10.dp, 5.dp), fontWeight = FontWeight.Bold) } } 
                NOMES_DESASTRES.forEach { (codigo, nome) -> val ativo = codigo in filtrosDesastres; item { Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) Color(0xFFD32F2F) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosDesastres = if (ativo) filtrosDesastres - codigo else filtrosDesastres + codigo }) { Text(nome, color = if (ativo) Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp), fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal) } } } 
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                item { Surface(shape = RoundedCornerShape(20.dp), color = if (filtrosSeguranca.size == 6) Color(0xFFE8F5E9) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if (filtrosSeguranca.size == 6) emptySet() else setOf("hospital","fire_station","police","shelter","pharmacy","place_of_worship") }) { Text("Todos seguros", fontSize = 11.sp, modifier = Modifier.padding(10.dp, 5.dp), fontWeight = FontWeight.Bold) } }
                NOMES_SEGURANCA.forEach { (codigo, nome) -> val ativo = codigo in filtrosSeguranca; item { Surface(shape = RoundedCornerShape(20.dp), color = if (ativo) Color(0xFF2E7D32) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if (ativo) filtrosSeguranca - codigo else filtrosSeguranca + codigo }) { Text(nome, color = if (ativo) Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp)) } } } 
                item { Surface(shape = RoundedCornerShape(20.dp), color = if ("place_of_worship" in filtrosSeguranca) Color(0xFF5D4037) else Color.White, shadowElevation = 2.dp, modifier = Modifier.clickable { filtrosSeguranca = if ("place_of_worship" in filtrosSeguranca) filtrosSeguranca - "place_of_worship" else filtrosSeguranca + "place_of_worship" }) { Text("Igrejas/Religião", color = if ("place_of_worship" in filtrosSeguranca) Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp)) } } 
            }
            if (rotaInfo != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(12.dp, 6.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Rota GPS Ativa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${rotaInfo!!.distanciaTexto} • ${rotaInfo!!.duracaoTexto}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Navigation, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("GPS", color = Color.White, fontSize = 11.sp) }
                    }
                }
            }
        }
        if (!temPermissao) {
            Card(modifier = Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, null, tint = Color.Red, modifier = Modifier.size(48.dp)); Spacer(modifier = Modifier.height(12.dp))
                    Text("Mapa precisa de localização", fontWeight = FontWeight.Bold); Text("Para usar o mapa como GPS profissional com rotas, tempo de deslocamento e alertas reais globais, permita a localização uma única vez. Seu consentimento será lembrado.", fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp)); Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Permitir Localização (1x)") }; Spacer(modifier = Modifier.height(8.dp)); TextButton(onClick = onVoltar) { Text("Voltar") }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 120.dp).size(56.dp).clip(CircleShape).background(Color.White).clickable { if (!temPermissao) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) else { val minha = myLocationOverlay?.myLocation; if (minha != null) { mapView.controller.animateTo(minha); mapView.controller.setZoom(18.0) } else { obterLocalizacao(context) { lat, lng -> val p = GeoPoint(lat, lng); localizacaoAtual = p; mapView.controller.animateTo(p); mapView.controller.setZoom(18.0) } } } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.MyLocation, null, tint = SafePointBlue, modifier = Modifier.size(28.dp)) }
        if (rotaPontos.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 190.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).clickable { rotaPontos = emptyList(); rotaInfo = null }.padding(12.dp, 8.dp)) { Text("Limpar rota", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold) }
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
                            if (distanciaUser > 0) Text(String.format(Locale("pt", "BR"), "%.1f km de você", distanciaUser), fontSize = 11.sp, color = SafePointBlue, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { ocorrenciaSelecionada = null; desastreSelecionado = null }) { Icon(Icons.Default.Close, null) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isDesastre && desastreSelecionado != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("⚠️ O que está acontecendo agora:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE65100))
                                Text("Fonte: ${desastreSelecionado!!.fonte}. Nível exibido: ${desastreSelecionado!!.gravidade}. Local: ${oc.titulo}. Distância aproximada: ${String.format(Locale("pt", "BR"), "%.1f km", distanciaUser)}. No USGS, o nível é uma classificação aproximada baseada na magnitude, não um alerta oficial. A cobertura varia; confirme com as autoridades locais.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Text("Ponto de interesse encontrado no OpenStreetMap. O cadastro não confirma funcionamento nem horário de abertura. Toque em Traçar Rota para calcular o percurso.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { tracarRotaAte(GeoPoint(oc.lat, oc.lng), oc.titulo) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SafePointBlue)) {
                            if (carregandoRota) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else { Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Traçar Rota GPS") }
                        }
                        OutlinedButton(onClick = { val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${oc.lat},${oc.lng}"); context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }) { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) { ItemNavegacao("Início", Icons.Default.Home, false) { onVoltar() }; ItemNavegacao("Mapa", Icons.Default.Map, true) {}; ItemNavegacao("Histórico", Icons.Default.DateRange, false) { onVoltar() }; ItemNavegacao("Perfil", Icons.Default.Person, false) { onVoltar() } }
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
    var buscaCidade by remember { mutableStateOf("") }
    var filtroGravidade by remember { mutableStateOf("Todos") }
    var filtroTipo by remember { mutableStateOf("Todos") }
    val context = LocalContext.current
    var alertasReais by remember { mutableStateOf<List<AlertaRota>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var cidadeAtual by remember { mutableStateOf("Carregando...") }
    var enderecoAtualReal by remember { mutableStateOf("") }
    var localizacaoAtual by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var buscaEmAndamento by remember { mutableStateOf(false) }
    var erroFonte by remember { mutableStateOf<String?>(null) }

    fun carregarAlertas(lat: Double, lng: Double, cidadeNome: String? = null) {
        carregando = true
        erroFonte = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enderecoReal = try { NominatimRepository.getEndereco(lat, lng) } catch (_: Exception) { "" }
                val partes = enderecoReal.split(",")
                val cidade = cidadeNome ?: partes.getOrNull(partes.size - 3)?.trim() ?: partes.getOrNull(partes.size - 4)?.trim() ?: "Local pesquisado"
                val gdacs = try { GdacsRepository.buscarDesastres() } catch (_: Exception) { null }
                val terremotos = try { UsgsRepository.buscarUltimos30Dias() } catch (_: Exception) { null }
                if (gdacs == null && terremotos == null) {
                    withContext(Dispatchers.Main) {
                        erroFonte = "As fontes globais de alertas estão temporariamente indisponíveis. Verifique a internet e tente novamente."
                        alertasReais = emptyList(); cidadeAtual = cidade; enderecoAtualReal = enderecoReal
                        carregando = false; buscaEmAndamento = false
                    }
                    return@launch
                }
                val listaReal = mutableListOf<AlertaRota>()
                gdacs?.features.orEmpty().forEach { feature ->
                    val fLon = feature.geometry.coordinates.getOrNull(0) ?: Double.NaN
                    val fLat = feature.geometry.coordinates.getOrNull(1) ?: Double.NaN
                    if (fLat !in -90.0..90.0 || fLon !in -180.0..180.0) return@forEach
                    val dist = calcularDistancia(lat, lng, fLat, fLon)
                    if (dist <= 500.0) {
                        val grav = when (feature.properties.alertlevel.lowercase(Locale.ROOT)) { "red" -> "Crítica"; "orange" -> "Alta"; "green" -> "Baixa"; else -> "Não classificada" }
                        val tipo = NOMES_DESASTRES[feature.properties.eventtype] ?: "Evento de desastre"
                        val nomeFonte = feature.properties.name.ifBlank { tipo }
                        val endRealFeature = traduzirNomeEvento(nomeFonte, tipo)
                        listaReal.add(AlertaRota(
                            id = "gdacs-${feature.properties.eventid ?: "${feature.properties.eventtype}-$fLat-$fLon"}",
                            titulo = "$tipo próximo de $cidade", dataHora = formatarDataAlerta(feature.properties.pubDate),
                            localizacao = endRealFeature, gravidade = grav, tipo = tipo, latitude = fLat, longitude = fLon,
                            distancia = dist, bairro = "GDACS • evento publicado", cidade = cidade, regiao = endRealFeature
                        ))
                    }
                }
                terremotos.orEmpty().forEach { quake ->
                    val dist = calcularDistancia(lat, lng, quake.latitude, quake.longitude)
                    if (dist <= 500.0) {
                        val grav = when { quake.magnitude >= 6.5 -> "Crítica"; quake.magnitude >= 5.0 -> "Alta"; quake.magnitude >= 4.0 -> "Média"; else -> "Baixa" }
                        val mag = String.format(Locale("pt", "BR"), "%.1f", quake.magnitude)
                        val data = SimpleDateFormat("dd/MM/yyyy HH:mm 'UTC'", Locale("pt", "BR")).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(quake.timestamp))
                        listaReal.add(AlertaRota(
                            id = "usgs-${quake.timestamp}-${quake.latitude}-${quake.longitude}",
                            titulo = "Terremoto M$mag próximo de $cidade", dataHora = "$data • USGS",
                            localizacao = quake.nome, gravidade = grav, tipo = "Terremoto", latitude = quake.latitude,
                            longitude = quake.longitude, distancia = dist, bairro = "USGS • magnitude $mag",
                            cidade = cidade, regiao = quake.nome
                        ))
                    }
                }
                listaReal.sortBy { it.distancia }
                withContext(Dispatchers.Main) {
                    alertasReais = listaReal.distinctBy { it.id }.take(200)
                    cidadeAtual = cidade; enderecoAtualReal = enderecoReal
                    carregando = false; buscaEmAndamento = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    erroFonte = "Não foi possível carregar os alertas globais. Verifique a conexão e tente novamente."
                    carregando = false; buscaEmAndamento = false
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
                val queryNormalizada = query.trim().lowercase(Locale.ROOT).replace("ã", "a").replace("á", "a")
                val primeiro = if (queryNormalizada.startsWith("sao paulo")) resultados.firstOrNull { it.display_name.contains("Brasil", ignoreCase = true) && it.display_name.contains("São Paulo", ignoreCase = true) } ?: resultados.firstOrNull() else resultados.firstOrNull()
                if (primeiro != null) {
                    withContext(Dispatchers.Main) {
                        val lat = primeiro.lat.toDoubleOrNull()
                        val lng = primeiro.lon.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            localizacaoAtual = Pair(lat, lng)
                            carregarAlertas(lat, lng, primeiro.display_name.split(",").firstOrNull() ?: query)
                        } else {
                            erroFonte = "O local encontrado não possui coordenadas válidas."
                            carregando = false
                            buscaEmAndamento = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { erroFonte = "Não encontramos esse local. Tente informar cidade e país."; carregando = false; buscaEmAndamento = false }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { erroFonte = "Falha ao pesquisar o local. Verifique a conexão e tente novamente."; carregando = false; buscaEmAndamento = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        obterLocalizacao(context) { lat, lng ->
            localizacaoAtual = Pair(lat, lng)
            carregarAlertas(lat, lng)
        }
        kotlinx.coroutines.delay(8000)
        if (carregando && !buscaEmAndamento && localizacaoAtual == null) {
            carregando = false
            erroFonte = "Não foi possível obter a localização atual. Ative o GPS ou pesquise uma cidade."
        }
    }
    val opcoesGravidade = listOf("Todos", "Crítica", "Alta", "Média", "Baixa"); val opcoesTipo = listOf("Todos", "Terremoto", "Ciclone", "Enchente", "Vulcão", "Seca", "Incêndio")
    val alertasFiltrados = alertasReais.filter { a -> val matchGrav = filtroGravidade == "Todos" || a.gravidade == filtroGravidade; val matchTipo = filtroTipo == "Todos" || a.tipo.contains(filtroTipo, ignoreCase = true); matchGrav && matchTipo }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF6F8FA))) {
            Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Histórico de Alertas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Eventos publicados perto de $cidadeAtual • raio de 500 km", fontSize = 12.sp, color = Color.White.copy(alpha = 0.88f))
                    if (enderecoAtualReal.isNotBlank()) Text(enderecoAtualReal.take(70) + "...", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
            OutlinedTextField(value = buscaCidade, onValueChange = { buscaCidade = it }, placeholder = { Text("Pesquisar rua, bairro, cidade ou país", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = azulEscuro) }, trailingIcon = { if (buscaCidade.isNotBlank()) IconButton(onClick = { buscarPorCidadeDigitada(buscaCidade) }) { Icon(Icons.Default.ArrowForward, null, tint = SafePointGreen) } }, modifier = Modifier.fillMaxWidth().padding(12.dp, 6.dp), shape = RoundedCornerShape(12.dp), singleLine = true)
            Text("Filtrar por gravidade:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)); LazyRow(contentPadding = PaddingValues(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opcoesGravidade) { filtro -> FilterChip(selected = filtroGravidade == filtro, onClick = { filtroGravidade = filtro }, label = { Text(filtro, fontSize = 11.sp) }) } }
            Text("Filtrar por tipo:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)); LazyRow(contentPadding = PaddingValues(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opcoesTipo) { filtro -> FilterChip(selected = filtroTipo == filtro, onClick = { filtroTipo = filtro }, label = { Text(filtro, fontSize = 11.sp) }) } }
            if (carregando) Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(color = azulEscuro); Spacer(modifier = Modifier.height(12.dp)); Text("Consultando alertas publicados para a região selecionada…", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)) } }
            else if (erroFonte != null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Alertas indisponíveis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = azulEscuro)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(erroFonte.orEmpty(), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val local = localizacaoAtual
                            if (local != null) {
                                carregarAlertas(local.first, local.second)
                            } else if (buscaCidade.isNotBlank()) {
                                buscarPorCidadeDigitada(buscaCidade)
                            }
                        }, enabled = !buscaEmAndamento) { Text("Tentar novamente") }
                    }
                }
            }
            else if (alertasReais.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                        Icon(Icons.Default.CheckCircle, null, tint = SafePointGreen, modifier = Modifier.size(64.dp)); Spacer(modifier = Modifier.height(16.dp)); 
                        Text("Nenhum alerta real próximo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = azulEscuro); 
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("GDACS e USGS não retornaram eventos dentro de 500 km de $cidadeAtual nos dados consultados. Isso não significa ausência de perigo local: nem todos os incidentes são publicados nessas fontes. Consulte também a Defesa Civil e as autoridades locais.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center) 
                    } 
                }
            }
            else if (alertasFiltrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterList, null, tint = Color.Gray, modifier = Modifier.size(48.dp)); Spacer(modifier = Modifier.height(12.dp))
                        Text("Nenhum alerta com esses filtros", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = azulEscuro)
                        Text("Tente mudar os filtros de gravidade ou tipo para ver outros alertas reais próximos.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
            else LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alertasFiltrados) { alerta ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) { 
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                                Text(alerta.titulo, fontWeight = FontWeight.Bold, color = azulEscuro, fontSize = 13.sp, modifier = Modifier.weight(1f)); 
                                val cor = when (alerta.gravidade) { "Crítica" -> Color(0xFFE53935); "Alta" -> Color(0xFFFF9800); "Média" -> Color(0xFFFFC107); else -> Color(0xFF4CAF50) }; 
                                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(cor).padding(8.dp, 4.dp)) { Text(alerta.gravidade, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) } 
                            }; 
                            Spacer(modifier = Modifier.height(4.dp)); Text(alerta.localizacao, fontSize = 11.sp, color = Color.Gray); Spacer(modifier = Modifier.height(3.dp)); 
                            Text("${alerta.cidade} | ${String.format(Locale("pt", "BR"), "%.1f km da região", alerta.distancia)} | ${alerta.tipo}", fontSize = 10.sp, color = CorVerdeBotao, fontWeight = FontWeight.Bold); 
                            Spacer(modifier = Modifier.height(2.dp)); Text(alerta.bairro, fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp)); Text(alerta.dataHora, fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

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
        "Como ver meu histórico de alertas?" to "Toque em 'Histórico' na barra inferior. O app busca alertas reais da sua região usando sua localização exata + API GDACS (desastres globais). Você vê distância em km, gravidade e localização geográfica via Nominatim. As fontes incluem GDACS e terremotos publicados pelo USGS; a cobertura varia por tipo de ocorrência. Use a busca e filtros por gravidade e tipo.",
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
