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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
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
val CorAzulEscuro = azulEscuro
val CorAzulMedio = azulLinhas
val CorVerdeBotao = verdeGradiente
val SafePointBlue = Color(0xFF1554C0)
val SafePointGreen = verdeGradiente
val FundoGradiente = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientFundoOriginal = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientBotaoOriginal = Brush.horizontalGradient(colors = listOf(azulEscuro, verdeGradiente))
val BotaoGradiente = gradientBotaoOriginal

data class AlertaRota(val id: String, val titulo: String, val dataHora: String, val localizacao: String, val gravidade: String, val tipo: String, val latitude: Double, val longitude: Double, val distancia: Double = 0.0, val bairro: String = "", val cidade: String = "", val regiao: String = "", val descricao: String = "", val fonte: String = "SafePoint Global")
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
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = "SafePoint/1.4 (Android; \${ctx.packageName}; https://github.com/GustavoLProgrammer/Projeto-SafePoint)"
            Configuration.getInstance().tileDownloadThreads = 2
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

// LOGO ORIGINAL PIN - LOGIN
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

// LOGO ESCUDO AZUL PERFEITO - CADASTRO
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

// TELAS LOGIN/CADASTRO ORIGINAIS
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
            Spacer(modifier = Modifier.height(24.dp)); Text("Crie sua conta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(4.dp)); Text("Preencha os dados abaixo para se cadastrar", fontSize = 12.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome completo", color = cinzaTexto) }, leadingIcon = { IconeUsuario(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
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
                auth.createUserWithEmailAndPassword(email, senha).addOnSuccessListener { result -> val userId = result.user?.uid ?: ""; val perfil = hashMapOf("nome" to nome, "email" to email, "telefone" to telefone, "dataNascimento" to dataNascimento); db.collection("usuarios").document(userId).set(perfil).addOnSuccessListener { PrefsHelper.setUserId(context, userId); Toast.makeText(context, "Conta criada!", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Home.route) { popUpTo(Screen.Cadastro.route) { inclusive = true } } }.addOnFailureListener { Toast.makeText(context, "Erro Firestore: \${it.message}", Toast.LENGTH_LONG).show() } }.addOnFailureListener { Toast.makeText(context, "Erro: \${it.localizedMessage}", Toast.LENGTH_LONG).show() }
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
                    if (email.isNotBlank()) auth.sendPasswordResetEmail(email).addOnSuccessListener { Toast.makeText(context, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show(); navController.popBackStack() }.addOnFailureListener { Toast.makeText(context, "Erro: \${it.message}", Toast.LENGTH_LONG).show() } else Toast.makeText(context, "Digite o e-mail", Toast.LENGTH_SHORT).show()
                }) { Text("Enviar Instruções", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp)); TextButton(onClick = { navController.popBackStack() }) { Text("Voltar", color = azulEscuro) }
            }
        }
    }
}

@Composable fun TelaPermissaoLocalizacao(onAceitar: () -> Unit, onRecusar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(24.dp)); Text("Permitir localização?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(modifier = Modifier.height(12.dp)); Text("Precisamos da sua localização exata para mostrar o mapa, rotas seguras e abrigos próximos. Esta permissão será solicitada apenas uma vez.", fontSize = 14.sp, color = cinzaTexto, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(32.dp)); Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable { onAceitar() }) { Text("Permitir Localização", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = onRecusar) { Text("Agora não", color = cinzaTexto) }
    }
}

@Composable
fun TelaTermosUso(onAceitar: () -> Unit) {
    var aceitou by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.Default.ArrowBack, null); Spacer(modifier = Modifier.width(4.dp)); Text("Voltar", color = azulEscuro) }
        Spacer(modifier = Modifier.height(8.dp)); LogoSafePoint(modifier = Modifier.size(92.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("Sempre te guiando a um ponto seguro", fontSize = 12.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(4.dp)); Text("SISTEMA INTELIGENTE DE SEGURANÇA E EVACUAÇÃO", fontSize = 10.sp, color = cinzaTexto, letterSpacing = 0.8.sp); Spacer(modifier = Modifier.height(24.dp))
        Text("Termos de Uso e Política de Privacidade", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = azulEscuro, textAlign = TextAlign.Center); Text("Última atualização: Março de 2026", fontSize = 11.sp, color = cinzaTexto); Spacer(modifier = Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("1. OBJETIVO E ACEITAÇÃO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("O SafePoint oferece um mapa completo, pesquisa de localidades, abrigos e alertas de emergência em todas as regiões. Ao criar uma conta e utilizar o aplicativo, você concorda com estes Termos de Uso.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
                Text("2. LOCALIZAÇÃO E PERMISSÃO ÚNICA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro); Text("A permissão de localização é solicitada apenas uma vez para garantir o funcionamento correto do GPS, abrigos e rotas em tempo real.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F4FF)).clickable { aceitou = !aceitou }.padding(14.dp)) { Checkbox(checked = aceitou, onCheckedChange = { aceitou = it }, modifier = Modifier.padding(top = 2.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Eu li, compreendi e concordo com os Termos de Uso e Política de Privacidade.", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { if (aceitou) onAceitar() }, enabled = aceitou, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (aceitou) azulEscuro else Color.Gray)) { Text("Aceitar e Continuar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

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
            Text("Bem-vindo ao SafePoint. Sempre te guiando a um ponto seguro.", modifier = Modifier.padding(top = 10.dp, bottom = 30.dp), fontSize = 17.sp, lineHeight = 22.sp, color = Color(0xFF355777))
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
                    Text("Responda algumas perguntas e envie sua localização com segurança.", modifier = Modifier.padding(top = 8.dp), fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF355777))
                }
            }
            Card(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 22.dp).clickable { navController.navigate(Screen.Historico.route) }, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7EF)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD94848), modifier = Modifier.size(38.dp))
                    Text("Histórico de eventos e alertas", modifier = Modifier.padding(top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF25804D))
                    Text("Consulte o histórico completo de eventos passados e alertas de todas as gravidades.", modifier = Modifier.padding(top = 8.dp), fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF355777))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// DADOS FICTÍCIOS UNIFORMES E VARIADOS PARA TODAS AS CIDADES DO MUNDO
data class CidadeGlobal(val nome: String, val pais: String, val lat: Double, val lng: Double)
data class AlertaMapa(val id: String, val cidade: String, val tipo: String, val gravidade: String, val lat: Double, val lng: Double, val descricao: String, val dataHora: String)
data class AbrigoMapa(val id: String, val cidade: String, val nome: String, val lat: Double, val lng: Double)

object SafePointDatabaseGlobal {
    val cidadesMundo = listOf(
        // Brasil
        CidadeGlobal("São Paulo", "Brasil", -23.5505, -46.6333),
        CidadeGlobal("Rio de Janeiro", "Brasil", -22.9068, -43.1729),
        CidadeGlobal("Belo Horizonte", "Brasil", -19.9167, -43.9345),
        CidadeGlobal("Brasília", "Brasil", -15.7939, -47.8828),
        CidadeGlobal("Salvador", "Brasil", -12.9777, -38.5016),
        CidadeGlobal("Fortaleza", "Brasil", -3.7319, -38.5267),
        CidadeGlobal("Manaus", "Brasil", -3.1190, -60.0217),
        CidadeGlobal("Curitiba", "Brasil", -25.4284, -49.2733),
        CidadeGlobal("Recife", "Brasil", -8.0476, -34.8770),
        CidadeGlobal("Porto Alegre", "Brasil", -30.0346, -51.2177),
        // Américas
        CidadeGlobal("Nova York", "Estados Unidos", 40.7128, -74.0060),
        CidadeGlobal("Los Angeles", "Estados Unidos", 34.0522, -118.2437),
        CidadeGlobal("Toronto", "Canadá", 43.6532, -79.3832),
        CidadeGlobal("Cidade do México", "México", 19.4326, -99.1332),
        CidadeGlobal("Buenos Aires", "Argentina", -34.6037, -58.3816),
        CidadeGlobal("Santiago", "Chile", -33.4489, -70.6693),
        CidadeGlobal("Bogotá", "Colômbia", 4.7110, -74.0721),
        CidadeGlobal("Lima", "Peru", -12.0464, -77.0428),
        // Europa
        CidadeGlobal("Londres", "Reino Unido", 51.5072, -0.1276),
        CidadeGlobal("Paris", "França", 48.8566, 2.3522),
        CidadeGlobal("Berlim", "Alemanha", 52.5200, 13.4050),
        CidadeGlobal("Roma", "Itália", 41.9028, 12.4964),
        CidadeGlobal("Madri", "Espanha", 40.4168, -3.7038),
        CidadeGlobal("Lisboa", "Portugal", 38.7223, -9.1393),
        CidadeGlobal("Moscou", "Rússia", 55.7558, 37.6173),
        // Ásia e Oceania
        CidadeGlobal("Tóquio", "Japão", 35.6762, 139.6503),
        CidadeGlobal("Pequim", "China", 39.9042, 116.4074),
        CidadeGlobal("Nova Delhi", "Índia", 28.6139, 77.2090),
        CidadeGlobal("Seul", "Coreia do Sul", 37.5665, 126.9780),
        CidadeGlobal("Singapura", "Singapura", 1.3521, 103.8198),
        CidadeGlobal("Sydney", "Austrália", -33.8688, 151.2093),
        CidadeGlobal("Dubai", "Emirados Árabes", 25.2048, 55.2708),
        // África e Oriente Médio
        CidadeGlobal("Cairo", "Egito", 30.0444, 31.2357),
        CidadeGlobal("Cidade do Cabo", "África do Sul", -33.9249, 18.4241),
        CidadeGlobal("Lagos", "Nigéria", 6.5244, 3.3792),
        CidadeGlobal("Istambul", "Turquia", 41.0082, 28.9784)
    )

    val tiposAlerta = listOf("Terremoto", "Enchente", "Incêndio", "Tempestade", "Deslizamento", "Ciclone", "Seca", "Erupção")
    val niveisGravidade = listOf("Crítica", "Alta", "Média", "Baixa")

    val listaAlertasMundo: List<AlertaMapa> by lazy {
        cidadesMundo.mapIndexed { index, cidade ->
            val tipo = tiposAlerta[index % tiposAlerta.size]
            val gravidade = niveisGravidade[index % niveisGravidade.size]
            val offsetLat = (index % 3 - 1) * 0.035
            val offsetLng = ((index * 7) % 5 - 2) * 0.035
            val lat = cidade.lat + offsetLat
            val lng = cidade.lng + offsetLng
            val descricao = when (tipo) {
                "Terremoto" -> "Abalo sísmico detectado de intensidade moderada a forte na região de ${cidade.nome}. Mantenha-se afastado de estruturas elevadas."
                "Enchente" -> "Nível de água elevado após fortes chuvas em ${cidade.nome}. Evite transitar por vias alagadas."
                "Incêndio" -> "Foco de incêndio registrado em área urbana/periférica de ${cidade.nome}. Equipes atuam no controle."
                "Tempestade" -> "Temporal severo com rajadas fortes e descargas elétricas em ${cidade.nome}. Busque abrigo imediato."
                "Deslizamento" -> "Risco geológico de encosta na zona de ${cidade.nome}. Moradores orientados a evacuar o setor."
                "Ciclone" -> "Ventos fortes associados a frente tempestuosa em ${cidade.nome}. Proteja janelas e objetos soltos."
                "Seca" -> "Estiagem prolongada e baixa umidade relativa do ar em ${cidade.nome}. Atenção à hidratação."
                else -> "Atividade vulcânica monitorada próxima a ${cidade.nome}. Siga as instruções da Defesa Civil."
            }
            val dia = (index % 28) + 1
            val dataHora = String.format("2026-03-%02d 14:%02d", dia, (index * 17) % 60)
            AlertaMapa(
                id = "alerta-$index",
                cidade = cidade.nome,
                tipo = tipo,
                gravidade = gravidade,
                lat = lat,
                lng = lng,
                descricao = descricao,
                dataHora = dataHora
            )
        }
    }

    val listaAbrigosMundo: List<AbrigoMapa> by lazy {
        cidadesMundo.mapIndexed { index, cidade ->
            AbrigoMapa(
                id = "abrigo-$index",
                cidade = cidade.nome,
                nome = "Abrigo Seguro • ${cidade.nome}",
                lat = cidade.lat + 0.025,
                lng = cidade.lng + 0.025
            )
        }
    }
}

// ROTA E INFORMAÇÕES DE DESLOCAMENTO
data class RotaInfoCompleta(val distanciaKm: Double, val duracaoMin: Int, val duracaoTexto: String, val distanciaTexto: String, val chegadaTexto: String)
enum class ModoTransporte(val titulo: String, val valhallaCosting: String) {
    CARRO("Carro", "auto"), A_PE("A pé", "pedestrian"), BICICLETA("Bicicleta", "bicycle")
}

@Composable
fun MapaScreenProfissional(navController: NavController, onVoltar: () -> Unit) {
    val context = LocalContext.current
    var busca by remember { mutableStateOf("") }
    var cidadeSelecionada by remember { mutableStateOf(SafePointDatabaseGlobal.cidadesMundo.first { it.nome == "São Paulo" }) }
    var filtroGravidade by remember { mutableStateOf(setOf("Crítica", "Alta", "Média", "Baixa")) }
    var filtroTipo by remember { mutableStateOf(SafePointDatabaseGlobal.tiposAlerta.toSet()) }
    var mostrarAbrigos by remember { mutableStateOf(true) }
    var alertaSelecionado by remember { mutableStateOf<AlertaMapa?>(null) }
    var abrigoSelecionado by remember { mutableStateOf<AbrigoMapa?>(null) }
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember { mutableStateOf<RotaInfoCompleta?>(null) }
    var modoTransporte by remember { mutableStateOf(ModoTransporte.CARRO) }
    var origem by remember { mutableStateOf(GeoPoint(cidadeSelecionada.lat, cidadeSelecionada.lng)) }
    var gpsAtivo by remember { mutableStateOf(false) }
    var mapaSatelite by remember { mutableStateOf(false) }
    var avisoStatus by remember { mutableStateOf("Mapa mundial carregado com alertas e abrigos uniformes.") }
    val scope = rememberCoroutineScope()

    // Permissão pedida apenas uma vez
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        temPermissao = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (temPermissao) obterLocalizacao(context) { lat, lng -> origem = GeoPoint(lat, lng); gpsAtivo = true }
    }

    val tileSourceRua = remember {
        XYTileSource("OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://a.tile.openstreetmap.org/", "https://b.tile.openstreetmap.org/", "https://c.tile.openstreetmap.org/"), "© OpenStreetMap contributors")
    }
    val tileSourceSatelite = remember {
        object : XYTileSource("NASA GIBS", 0, 9, 256, ".jpg", arrayOf("https://gibs.earthdata.nasa.gov/"), "NASA GIBS") {
            override fun getTileURLString(pMapTile: Long): String {
                val z = org.osmdroid.util.MapTileIndex.getZoom(pMapTile)
                val x = org.osmdroid.util.MapTileIndex.getX(pMapTile)
                val y = org.osmdroid.util.MapTileIndex.getY(pMapTile)
                return "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_SNPP_CorrectedReflectance_TrueColor/default/2026-03-01/GoogleMapsCompatible_Level9/$z/$y/$x.jpg"
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
            controller.setZoom(4.0)
            controller.setCenter(GeoPoint(cidadeSelecionada.lat, cidadeSelecionada.lng))
        }
    }

    LaunchedEffect(mapaSatelite) { mapView.setTileSource(tileSource); mapView.invalidate() }

    val alertasVisiveis = remember(filtroGravidade, filtroTipo) {
        SafePointDatabaseGlobal.listaAlertasMundo.filter { it.gravidade in filtroGravidade && it.tipo in filtroTipo }
    }
    val abrigosVisiveis = remember(mostrarAbrigos) {
        if (mostrarAbrigos) SafePointDatabaseGlobal.listaAbrigosMundo else emptyList()
    }

    fun pesquisarLocal() {
        val q = busca.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return
        val encontrada = SafePointDatabaseGlobal.cidadesMundo.firstOrNull { it.nome.lowercase(Locale.ROOT).contains(q) || it.pais.lowercase(Locale.ROOT).contains(q) }
        if (encontrada != null) {
            cidadeSelecionada = encontrada
            origem = GeoPoint(encontrada.lat, encontrada.lng)
            mapView.controller.animateTo(GeoPoint(encontrada.lat, encontrada.lng))
            mapView.controller.setZoom(11.0)
            avisoStatus = "Local encontrado: ${encontrada.nome} (${encontrada.pais})"
            mapView.invalidate()
        } else {
            avisoStatus = "Local não encontrado na base mundial fictícia."
        }
    }

    fun calcularRota(destinoLat: Double, destinoLng: Double) {
        scope.launch {
            avisoStatus = "Calculando rota de deslocamento..."
            try {
                val rota = ValhallaRepository.buscarRotaCompleta(origem.longitude, origem.latitude, destinoLng, destinoLat, modoTransporte.valhallaCosting)
                rotaPontos = rota.pontos
                val km = rota.distanciaMetros / 1000.0
                val minutos = (rota.duracaoSeg / 60.0).toInt().coerceAtLeast(1)
                val duracao = if (minutos < 60) "$minutos min" else "\${minutos / 60}h \${minutos % 60}min"
                val distancia = if (km < 1.0) "\${rota.distanciaMetros.toInt()} m" else String.format(Locale("pt", "BR"), "%.1f km", km)
                val chegada = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(System.currentTimeMillis() + rota.duracaoSeg.toLong() * 1000L))
                rotaInfo = RotaInfoCompleta(km, minutos, duracao, distancia, chegada)
                avisoStatus = "Rota traçada com sucesso!"
                mapView.invalidate()
            } catch (_: Exception) {
                // Rota simulada robusta caso o serviço externo falhe
                val km = calcularDistancia(origem.latitude, origem.longitude, destinoLat, destinoLng)
                val minutos = (km * 3.5).toInt().coerceAtLeast(2)
                rotaPontos = listOf(origem, GeoPoint((origem.latitude + destinoLat)/2, (origem.longitude + destinoLng)/2), GeoPoint(destinoLat, destinoLng))
                rotaInfo = RotaInfoCompleta(km, minutos, "$minutos min", String.format(Locale("pt", "BR"), "%.1f km", km), SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(System.currentTimeMillis() + minutos * 60000L)))
                avisoStatus = "Rota simulada calculada com sucesso!"
                mapView.invalidate()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!temPermissao) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            obterLocalizacao(context) { lat, lng -> origem = GeoPoint(lat, lng); gpsAtivo = true }
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF3F6F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF75E0A2), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SafePoint • Mapa Mundial", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if (mapaSatelite) "Modo Satélite Ativo" else "Alertas e Abrigos Uniformes em Todas as Cidades", color = Color(0xFFBCE8D7), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = busca, onValueChange = { busca = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Pesquisar cidade ou país...", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                    Spacer(Modifier.width(7.dp))
                    Button(onClick = { pesquisarLocal() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9A72))) { Icon(Icons.Default.Search, null); Text("Ir") }
                }
                Text(avisoStatus, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }

            // Filtros de Gravidade e Tipos
            LazyRow(modifier = Modifier.fillMaxWidth().height(46.dp).background(Color.White), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = mostrarAbrigos, onClick = { mostrarAbrigos = !mostrarAbrigos }, label = { Text("🟢 Abrigos (+)", fontSize = 11.sp) }) }
                items(listOf("Crítica", "Alta", "Média", "Baixa")) { nivel -> FilterChip(selected = nivel in filtroGravidade, onClick = { filtroGravidade = if (nivel in filtroGravidade) filtroGravidade - nivel else filtroGravidade + nivel }, label = { Text(nivel, fontSize = 11.sp) }) }
            }
            LazyRow(modifier = Modifier.fillMaxWidth().height(42.dp).background(Color.White), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SafePointDatabaseGlobal.tiposAlerta) { tipo -> FilterChip(selected = tipo in filtroTipo, onClick = { filtroTipo = if (tipo in filtroTipo) filtroTipo - tipo else filtroTipo + tipo }, label = { Text(tipo, fontSize = 10.sp) }) }
            }

            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 4.dp).clipToBounds()) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().clipToBounds()) { view ->
                    view.overlays.removeAll { it is Marker || it is Polyline }
                    if (rotaPontos.isNotEmpty()) {
                        val linha = Polyline().apply { setPoints(rotaPontos); outlinePaint.color = android.graphics.Color.rgb(21, 84, 192); outlinePaint.strokeWidth = 10f; outlinePaint.strokeCap = Paint.Cap.ROUND }
                        view.overlays.add(linha)
                    }
                    // Alertas no mapa com ícones menores e profissionais
                    alertasVisiveis.forEach { a ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(a.lat, a.lng)
                            title = "\${a.tipo} • \${a.gravidade}"
                            snippet = "\${a.cidade} — \${a.descricao}"
                            icon = criarPinoAlertaProfissional(context, a.gravidade, 36)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ -> alertaSelecionado = a; abrigoSelecionado = null; true }
                        }
                        view.overlays.add(marker)
                    }
                    // Abrigos verdes com símbolo de +
                    abrigosVisiveis.forEach { a ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(a.lat, a.lng)
                            title = a.nome
                            snippet = "Abrigo seguro em \${a.cidade}."
                            icon = criarPinoAbrigoVerde(context, 36)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ -> abrigoSelecionado = a; alertaSelecionado = null; true }
                        }
                        view.overlays.add(marker)
                    }
                    // Ícone de localização igual ao do GPS (não seta azul)
                    val markerGPS = Marker(view).apply {
                        position = origem
                        title = "Sua Localização GPS"
                        snippet = "Você está aqui"
                        icon = criarPinoGpsReal(context, 36)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    view.overlays.add(markerGPS)
                    view.invalidate()
                }

                // Botões flutuantes do mapa
                Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallFloatingActionButton(onClick = { mapaSatelite = !mapaSatelite }, containerColor = Color.White) { Icon(Icons.Default.Map, if (mapaSatelite) "Modo Rua" else "Modo Satélite", tint = Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble + 1); mapView.invalidate() }, containerColor = Color.White) { Icon(Icons.Default.Add, null, tint = Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble - 1); mapView.invalidate() }, containerColor = Color.White) { Icon(Icons.Default.Remove, null, tint = Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick = { obterLocalizacao(context) { lat, lng -> origem = GeoPoint(lat, lng); gpsAtivo = true; mapView.controller.animateTo(origem); mapView.controller.setZoom(13.0); mapView.invalidate() } }, containerColor = Color.White) { Icon(Icons.Default.MyLocation, null, tint = Color(0xFF1976D2)) }
                }
            }

            // MODAL / CARD DE DESCRIÇÃO DO ALERTA OU ABRIGO COM BOTÃO X
            val alertaAtual = alertaSelecionado
            val abrigoAtual = abrigoSelecionado
            if (alertaAtual != null || abrigoAtual != null) {
                Card(Modifier.fillMaxWidth().padding(10.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(if (alertaAtual != null) "\${alertaAtual.tipo} (\${alertaAtual.gravidade})" else abrigoAtual!!.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0B2545))
                                Text(if (alertaAtual != null) "\${alertaAtual.cidade} • \${alertaAtual.dataHora}" else abrigoAtual!!.cidade, fontSize = 12.sp, color = Color.Gray)
                            }
                            // Botão de fechar (X)
                            IconButton(onClick = { alertaSelecionado = null; abrigoSelecionado = null; rotaPontos = emptyList(); rotaInfo = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.DarkGray)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(if (alertaAtual != null) alertaAtual.descricao else "Abrigo verificado com capacidade de acolhimento e suprimentos essenciais.", fontSize = 13.sp, color = Color(0xFF334155))
                        Spacer(Modifier.height(10.dp))

                        // Seleção de transporte
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            listOf(ModoTransporte.CARRO, ModoTransporte.A_PE, ModoTransporte.BICICLETA).forEach { modo ->
                                FilterChip(selected = modoTransporte == modo, onClick = { modoTransporte = modo }, label = { Text(modo.titulo, fontSize = 11.sp) })
                            }
                        }

                        Button(onClick = {
                            if (alertaAtual != null) calcularRota(alertaAtual.lat, alertaAtual.lng)
                            else if (abrigoAtual != null) calcularRota(abrigoAtual.lat, abrigoAtual.lng)
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1267A5))) {
                            Icon(Icons.Default.Route, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Traçar Rota até o Local / Abrigo")
                        }
                    }
                }
            }

            // INFORMAÇÕES DE ROTA (TEMPO, CHEGADA, KM)
            rotaInfo?.let { info ->
                Surface(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFE5F3FF)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = Color(0xFF1267A5))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Deslocamento • \${modoTransporte.titulo}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0B2545))
                            Text("Distância: \${info.distanciaTexto} | Duração: \${info.duracaoTexto}", fontSize = 12.sp, color = Color(0xFF1267A5))
                            Text("Chegada estimada às \${info.chegadaTexto}", fontSize = 11.sp, color = Color.DarkGray)
                        }
                        IconButton(onClick = { rotaInfo = null; rotaPontos = emptyList(); mapView.invalidate() }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }
    }
}

// ÍCONES PERSONALIZADOS PROFISSIONAIS E MENORES
fun criarPinoAlertaProfissional(context: Context, gravidade: String, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val h = (w * 1.2f).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val canvas = AndroidCanvas(bmp)
    val cor = when (gravidade) {
        "Crítica" -> android.graphics.Color.rgb(211, 47, 47)
        "Alta" -> android.graphics.Color.rgb(245, 124, 0)
        "Média" -> android.graphics.Color.rgb(213, 165, 27)
        else -> android.graphics.Color.rgb(56, 142, 60)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor; style = Paint.Style.FILL }
    val paintBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
    val path = AndroidPath().apply {
        moveTo(w / 2f, 0f)
        cubicTo(w.toFloat(), 0f, w.toFloat(), h * 0.6f, w / 2f, h.toFloat())
        cubicTo(0f, h * 0.6f, 0f, 0f, w / 2f, 0f)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawPath(path, paintBorda)
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = w * 0.45f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    canvas.drawText("!", w / 2f, h * 0.55f, texto)
    return BitmapDrawable(context.resources, bmp)
}

fun criarPinoAbrigoVerde(context: Context, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx; val h = (w * 1.2f).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val canvas = AndroidCanvas(bmp)
    val cor = android.graphics.Color.rgb(30, 155, 68)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor; style = Paint.Style.FILL }
    val paintBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
    val path = AndroidPath().apply {
        moveTo(w / 2f, 0f)
        cubicTo(w.toFloat(), 0f, w.toFloat(), h * 0.6f, w / 2f, h.toFloat())
        cubicTo(0f, h * 0.6f, 0f, 0f, w / 2f, 0f)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawPath(path, paintBorda)
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = w * 0.55f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    canvas.drawText("+", w / 2f, h * 0.6f, texto)
    return BitmapDrawable(context.resources, bmp)
}

fun criarPinoGpsReal(context: Context, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w = tamanhoPx
    val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888); val canvas = AndroidCanvas(bmp)
    val cx = w / 2f; val cy = w / 2f; val raio = w / 2f - 3f
    val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(70, 0, 0, 0) }
    canvas.drawCircle(cx, cy + 2f, raio, sombra)
    val fundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(25, 118, 210); style = Paint.Style.FILL }
    canvas.drawCircle(cx, cy, raio, fundo)
    val borda = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
    canvas.drawCircle(cx, cy, raio - 1.5f, borda)
    val centro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL }
    canvas.drawCircle(cx, cy, raio * 0.35f, centro)
    return BitmapDrawable(context.resources, bmp)
}

// HISTÓRICO COMPLETO DE EVENTOS PASSADOS SEM LIMITES E COM TODOS OS NÍVEIS
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TelaHistoricoReal(navController: NavController) {
    var filtroGravidade by remember { mutableStateOf("Todos") }
    var filtroTipo by remember { mutableStateOf("Todos") }
    var buscaTermo by remember { mutableStateOf("") }
    var alertaSelecionado by remember { mutableStateOf<AlertaMapa?>(null) }

    // Lista completa, sem limites, com todos os tipos de alerta e todos os níveis de gravidade
    val historicoEventos = remember {
        SafePointDatabaseGlobal.cidadesMundo.flatMapIndexed { i, cidade ->
            listOf(
                AlertaMapa("hist-$i-1", cidade.nome, SafePointDatabaseGlobal.tiposAlerta[i % SafePointDatabaseGlobal.tiposAlerta.size], SafePointDatabaseGlobal.niveisGravidade[i % SafePointDatabaseGlobal.niveisGravidade.size], cidade.lat, cidade.lng, "Evento registrado pelo sistema em ${cidade.nome} (${cidade.pais}). O usuário passou por esta região durante ocorrência monitorada.", "2026-02-15 10:30"),
                AlertaMapa("hist-$i-2", cidade.nome, SafePointDatabaseGlobal.tiposAlerta[(i + 2) % SafePointDatabaseGlobal.tiposAlerta.size], SafePointDatabaseGlobal.niveisGravidade[(i + 1) % SafePointDatabaseGlobal.niveisGravidade.size], cidade.lat + 0.01, cidade.lng + 0.01, "Ocorrência anterior registrada no trajeto do usuário em ${cidade.nome}. Condição normalizada após atuação.", "2026-01-20 16:45")
            )
        }
    }

    val filtrados = historicoEventos.filter { ev ->
        val matchGravidade = filtroGravidade == "Todos" || ev.gravidade == filtroGravidade
        val matchTipo = filtroTipo == "Todos" || ev.tipo == filtroTipo
        val matchBusca = buscaTermo.isBlank() || ev.cidade.lowercase(Locale.ROOT).contains(buscaTermo.lowercase(Locale.ROOT)) || ev.descricao.lowercase(Locale.ROOT).contains(buscaTermo.lowercase(Locale.ROOT))
        matchGravidade && matchTipo && matchBusca
    }

    val corNivel: (String) -> Color = { grav ->
        when (grav) {
            "Crítica" -> Color(0xFFD32F2F)
            "Alta" -> Color(0xFFEF6C00)
            "Média" -> Color(0xFFF9A825)
            else -> Color(0xFF2E7D32)
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF2F5F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = Color(0xFF83E3B1), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Histórico de Eventos", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Registro completo de eventos e locais por onde passou", color = Color(0xFFC9E8DF), fontSize = 11.sp)
                    }
                }
            }

            // Campo de busca no histórico
            OutlinedTextField(
                value = buscaTermo,
                onValueChange = { buscaTermo = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar no histórico por cidade ou evento...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filtros de Gravidade
            Text("Nível de Gravidade", Modifier.padding(start = 16.dp, top = 4.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Todos", "Crítica", "Alta", "Média", "Baixa")) { f ->
                    FilterChip(selected = filtroGravidade == f, onClick = { filtroGravidade = f }, label = { Text(f, fontSize = 11.sp) })
                }
            }

            // Filtros de Tipo de Alerta
            Text("Tipo de Evento", Modifier.padding(start = 16.dp, top = 2.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Todos") + SafePointDatabaseGlobal.tiposAlerta) { f ->
                    FilterChip(selected = filtroTipo == f, onClick = { filtroTipo = f }, label = { Text(f, fontSize = 11.sp) })
                }
            }

            if (filtrados.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum evento encontrado com os filtros selecionados.", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtrados, key = { it.id }) { evento ->
                        val cor = corNivel(evento.gravidade)
                        Card(Modifier.fillMaxWidth().clickable { alertaSelecionado = evento }, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Box(Modifier.width(6.dp).fillMaxHeight().background(cor))
                                Column(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Column(Modifier.weight(1f)) {
                                            Text(evento.tipo, color = Color(0xFF0B2545), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(evento.cidade, color = Color(0xFF64748B), fontSize = 12.sp)
                                        }
                                        Surface(color = cor, shape = RoundedCornerShape(7.dp)) {
                                            Text(evento.gravidade, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                        }
                                    }
                                    Text(evento.descricao, color = Color(0xFF475569), fontSize = 12.sp, maxLines = 2)
                                    Text("Data: \${evento.dataHora}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    alertaSelecionado?.let { ev ->
        AlertDialog(
            onDismissRequest = { alertaSelecionado = null },
            title = { Text("\${ev.tipo} em \${ev.cidade}", color = Color(0xFF0B2545), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = corNivel(ev.gravidade), shape = RoundedCornerShape(7.dp)) {
                        Text("Gravidade: \${ev.gravidade}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Text(ev.descricao, color = Color(0xFF334155), fontSize = 14.sp)
                    Text("Data e Hora: \${ev.dataHora}", color = Color(0xFF475569), fontSize = 12.sp)
                    Text("Localização: \${ev.lat}, \${ev.lng}", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { alertaSelecionado = null }) { Text("Fechar") }
            }
        )
    }
}

// RESTANTE DAS TELAS (PERFIL, SOS, AJUDA)
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
                        Spacer(modifier = Modifier.height(14.dp)); Text(if (nome.isEmpty()) "Usuário" else nome, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
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
                                Toast.makeText(context, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show(); salvando = false
                            }.addOnFailureListener { e -> Toast.makeText(context, "Erro ao salvar: \${e.message}", Toast.LENGTH_LONG).show(); salvando = false }
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
        }
    }
}

@Composable fun BotaoEmergencia(numero: String, nome: String, cor: Color, icon: ImageVector, context: Context) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth().clickable { val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:\$numero")); context.startActivity(intent) }) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(cor), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }; Spacer(modifier = Modifier.width(14.dp)); Column(modifier = Modifier.weight(1f)) { Text(nome, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Ligar \$numero", fontSize = 12.sp, color = cor, fontWeight = FontWeight.Bold) }; Icon(Icons.Default.Call, null, tint = cor) }
    }
}

@Composable fun TelaAjudaComoUsar(onVoltar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, null) }
            Text("Ajuda e Suporte", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = azulEscuro)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("O SafePoint mantém todas as interfaces originais, mapa mundial completo com abrigos verdes e alertas de todos os tipos distribuídos uniformemente.", color = Color.DarkGray)
    }
}

// Utilitários de cálculo e repositórios
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (gps != null) {
            onLocationReceived(gps.latitude, gps.longitude)
            return
        }
    } catch (_: Exception) {}
    onLocationReceived(-23.5505, -46.6333) // Padrão São Paulo
}

interface ValhallaApi { @retrofit2.http.GET("route") suspend fun route(@retrofit2.http.Query("json") requestJson: String): okhttp3.ResponseBody }
object ValhallaRepository {
    private val client = okhttp3.OkHttpClient.Builder().callTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://valhalla1.openstreetmap.de/").client(client).build().create(ValhallaApi::class.java)
    suspend fun buscarRotaCompleta(lon1: Double, lat1: Double, lon2: Double, lat2: Double, costing: String): RotaCompleta {
        val json = org.json.JSONObject().apply {
            put("locations", org.json.JSONArray().put(org.json.JSONObject().put("lat", lat1).put("lon", lon1)).put(org.json.JSONObject().put("lat", lat2).put("lon", lon2)))
            put("costing", costing); put("units", "kilometers")
        }
        val root = org.json.JSONObject(api.route(json.toString()).string())
        val trip = root.getJSONObject("trip")
        val legs = trip.getJSONArray("legs")
        val shape = legs.getJSONObject(0).getString("shape")
        val summary = trip.getJSONObject("summary")
        val points = decodePolyline6(shape)
        return RotaCompleta(points, summary.getDouble("length") * 1000.0, summary.getDouble("time"))
    }
    private fun decodePolyline6(encoded: String): List<GeoPoint> {
        val result = ArrayList<GeoPoint>(); var index = 0; var lat = 0; var lon = 0
        while (index < encoded.length) {
            fun readValue(): Int {
                var shift = 0; var value = 0; var b: Int
                do { b = encoded[index++].code - 63; value = value or ((b and 0x1f) shl shift); shift += 5 } while (b >= 0x20)
                return if ((value and 1) != 0) (value shr 1).inv() else value shr 1
            }
            lat += readValue(); lon += readValue()
            result.add(GeoPoint(lat / 1_000_000.0, lon / 1_000_000.0))
        }
        return result
    }
}
data class RotaCompleta(val pontos: List<GeoPoint>, val distanciaMetros: Double, val duracaoSeg: Double)
