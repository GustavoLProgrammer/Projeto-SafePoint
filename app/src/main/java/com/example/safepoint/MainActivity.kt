
package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

// CORES - identidade preservada
val azulEscuro = Color(0xFF0F3E8A)
val azulLinhas = Color(0xFF1267A5)
val verdeGradiente = Color(0xFF1EA84C)
val cinzaTexto = Color(0xFF6B7A8F)
val SafePointBlue = Color(0xFF174A86)
val SafePointGreen = Color(0xFF16845F)
val gradientFundoOriginal = Brush.verticalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52), Color(0xFF0F3E8A)))
val gradientBotaoOriginal = Brush.horizontalGradient(colors = listOf(azulEscuro, verdeGradiente))
val BotaoGradiente = gradientBotaoOriginal
val FundoGradiente = Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))

// DADOS REAIS - SEM FICTÍCIOS
data class CidadeDemo(val nome: String, val uf: String, val lat: Double, val lng: Double)
data class AlertaReal(val id: String, val titulo: String, val cidade: String, val tipo: String, val gravidade: String, val lat: Double, val lng: Double, val descricao: String, val dataHora: String, val fonte: String)
data class AbrigoReal(val id: String, val categoria: String, val nome: String, val lat: Double, val lng: Double, val endereco: String, val tags: Map<String,String> = emptyMap())

object SessionAtual { var ultimoLocal: CidadeDemo? = null }

data class RotaInfo(val distanciaKm: Double, val duracaoMin: Int, val duracaoTexto: String, val distanciaTexto: String, val chegadaTexto: String)
enum class ModoRota(val titulo: String, val valhallaCosting: String) { CARRO("Carro","auto"), A_PE("A pé","pedestrian"), BICICLETA("Bicicleta","bicycle"), ONIBUS("Ônibus","transit") }

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
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth; private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply { isAppearanceLightStatusBars = false }
        auth = FirebaseAuth.getInstance(); db = FirebaseFirestore.getInstance()
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val startDestination = if (PrefsHelper.isTermosAceitos(context)) {
                if (PrefsHelper.getUserId(context) != null) Screen.Home.route else Screen.Login.route
            } else Screen.Termos.route
            NavHost(navController = navController, startDestination = startDestination) {
                composable(Screen.Termos.route) { TelaTermosUso(onAceitar = { PrefsHelper.setTermosAceitos(context,true); navController.navigate(Screen.Login.route){ popUpTo(Screen.Termos.route){inclusive=true}} }) }
                composable(Screen.Login.route) { TelaLoginOriginal(navController, auth) }
                composable(Screen.Cadastro.route) { TelaCadastroOriginal(navController, auth, db) }
                composable(Screen.RecuperarSenha.route) { TelaRecuperarSenhaOriginal(navController, auth) }
                composable(Screen.Home.route) { TelaHome(navController) }
                composable(Screen.Mapa.route) { MapaScreenReal(navController = navController, onVoltar = { navController.popBackStack() }) }
                composable(Screen.Historico.route) { TelaHistoricoReal(navController) }
                composable(Screen.Perfil.route) { TelaPerfil(navController, auth, db) }
                composable(Screen.SOS.route) { TelaSOS(onVoltar = { navController.popBackStack() }) }
                composable(Screen.Ajuda.route) { TelaAjudaComoUsar(onVoltar = { navController.popBackStack() }) }
            }
        }
    }
}

// LOGO E ICONES - preservados
@Composable fun LogoSafePoint(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val verdeEscudo = Color(0xFF1EA84C)
        val escudoGradient = Brush.verticalGradient(colors = listOf(Color(0xFF0C3875), Color(0xFF138A4B)), startY = 0f, endY = h)
        val pathEscudoExterno = Path().apply {
            moveTo(w * 0.5f, h * 0.05f); cubicTo(w * 0.82f, h * 0.05f, w * 0.94f, h * 0.18f, w * 0.94f, h * 0.42f)
            cubicTo(w * 0.94f, h * 0.72f, w * 0.52f, h * 0.96f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.48f, h * 0.96f, w * 0.06f, h * 0.72f, w * 0.06f, h * 0.42f)
            cubicTo(w * 0.06f, h * 0.18f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.05f); close()
        }
        drawPath(path = pathEscudoExterno, brush = escudoGradient, style = Stroke(width = 4.dp.toPx()))
    }
}
@Composable fun IconeUsuario(cor: Color, modifier: Modifier = Modifier) { Canvas(modifier = modifier) { val w = size.width; val h = size.height; val stroke = Stroke(width = 1.8.dp.toPx()); drawCircle(color = cor, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.32f), style = stroke); val corpoPath = Path().apply { moveTo(w * 0.15f, h * 0.92f); cubicTo(w * 0.15f, h * 0.68f, w * 0.85f, h * 0.68f, w * 0.85f, h * 0.92f) }; drawPath(path = corpoPath, color = cor, style = stroke) } }
@Composable fun IconeCadeado(cor: Color, modifier: Modifier = Modifier) { Canvas(modifier = modifier) { val w = size.width; val h = size.height; val stroke = Stroke(width = 1.8.dp.toPx()); drawRoundRect(color = cor, topLeft = Offset(w * 0.18f, h * 0.42f), size = Size(w * 0.64f, h * 0.52f), cornerRadius = CornerRadius(4.dp.toPx()), style = stroke); val arcoPath = Path().apply { moveTo(w * 0.30f, h * 0.42f); lineTo(w * 0.30f, h * 0.24f); cubicTo(w * 0.30f, h * 0.08f, w * 0.70f, h * 0.08f, w * 0.70f, h * 0.24f); lineTo(w * 0.70f, h * 0.42f) }; drawPath(path = arcoPath, color = cor, style = stroke) } }
@Composable fun IconeOlho(aberto: Boolean, cor: Color, modifier: Modifier = Modifier) { Canvas(modifier = modifier) { val w = size.width; val h = size.height; val stroke = Stroke(width = 1.6.dp.toPx()); val olhoPath = Path().apply { moveTo(w * 0.1f, h * 0.5f); cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.9f, h * 0.5f); cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.1f, h * 0.5f); close() }; drawPath(path = olhoPath, color = cor, style = stroke); if (aberto) drawCircle(color = cor, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.5f)) } }

@Composable fun BotaoGradiente(texto: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(BotaoGradiente, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text(texto, fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold) }
    }
}
@Composable fun BarraNavegacaoInferior(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected = currentRoute == Screen.Home.route, onClick = { if (currentRoute != Screen.Home.route) navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
        NavigationBarItem(selected = currentRoute == Screen.Mapa.route, onClick = { if (currentRoute != Screen.Mapa.route) navController.navigate(Screen.Mapa.route) }, icon = { Icon(Icons.Default.Map, null) }, label = { Text("Mapa") })
        NavigationBarItem(selected = currentRoute == Screen.Historico.route, onClick = { if (currentRoute != Screen.Historico.route) navController.navigate(Screen.Historico.route) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Histórico") })
        NavigationBarItem(selected = currentRoute == Screen.Perfil.route, onClick = { if (currentRoute != Screen.Perfil.route) navController.navigate(Screen.Perfil.route) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") })
    }
}

// TELAS ORIGINAIS PRESERVADAS
@Composable fun TelaLoginOriginal(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var senhaVisivel by remember { mutableStateOf(false) }; val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal)) {
        Card(shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(22.dp).align(Alignment.Center)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                LogoSafePoint(Modifier.size(92.dp)); Text("SAFEPOINT", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = azulEscuro); Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha") }, leadingIcon = { IconeCadeado(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, trailingIcon = { IconButton(onClick = { senhaVisivel = !senhaVisivel }) { IconeOlho(aberto = senhaVisivel, cor = cinzaTexto, modifier = Modifier.size(22.dp)) } }, visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(Modifier.height(20.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable {
                    if (email.isBlank() || senha.isBlank()) { Toast.makeText(context,"Preencha e-mail e senha",Toast.LENGTH_SHORT).show(); return@clickable }
                    auth.signInWithEmailAndPassword(email, senha).addOnSuccessListener { PrefsHelper.setUserId(context, it.user?.uid?:""); navController.navigate(Screen.Home.route){ popUpTo(Screen.Login.route){inclusive=true}} }.addOnFailureListener { Toast.makeText(context,"Erro: ${it.message}",Toast.LENGTH_LONG).show() }
                }) { Text("Entrar", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(12.dp))
                Text("Esqueci minha senha", color = azulLinhas, modifier = Modifier.clickable { navController.navigate(Screen.RecuperarSenha.route) })
                Spacer(Modifier.height(12.dp))
                Row { Text("Não tem conta? "); Text("Criar conta", color = azulLinhas, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navController.navigate(Screen.Cadastro.route) }) }
            }
        }
    }
}
@Composable fun TelaCadastroOriginal(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    var nome by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var aceitou by remember { mutableStateOf(false) }; val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal).verticalScroll(rememberScrollState())) {
        Card(shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(22.dp)) {
            Column(Modifier.padding(24.dp)) {
                LogoSafePoint(Modifier.size(72.dp)); Text("Criar conta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro); Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, placeholder = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { aceitou = !aceitou }) { Checkbox(checked = aceitou, onCheckedChange = { aceitou = it }); Text("Aceito Termos", fontSize = 12.sp) }
                Spacer(Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(gradientBotaoOriginal).clickable {
                    if (!aceitou) { Toast.makeText(context,"Aceite os termos",Toast.LENGTH_SHORT).show(); return@clickable }
                    auth.createUserWithEmailAndPassword(email, senha).addOnSuccessListener { result -> val userId = result.user?.uid?:""; val perfil = hashMapOf("nome" to nome, "email" to email); db.collection("usuarios").document(userId).set(perfil).addOnSuccessListener { PrefsHelper.setUserId(context,userId); navController.navigate(Screen.Home.route){ popUpTo(Screen.Cadastro.route){inclusive=true}} } }.addOnFailureListener { Toast.makeText(context,"Erro: ${it.message}",Toast.LENGTH_LONG).show() }
                }) { Text("Cadastrar", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
@Composable fun TelaRecuperarSenhaOriginal(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(gradientFundoOriginal)) {
        Card(shape = RoundedCornerShape(36.dp), modifier = Modifier.padding(22.dp).align(Alignment.Center)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Recuperar senha", fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(48.dp).background(gradientBotaoOriginal, RoundedCornerShape(10.dp)).clickable {
                    auth.sendPasswordResetEmail(email).addOnSuccessListener { Toast.makeText(context,"E-mail enviado",Toast.LENGTH_LONG).show(); navController.popBackStack() }
                }, contentAlignment = Alignment.Center) { Text("Enviar", color = Color.White) }
            }
        }
    }
}
@Composable fun TelaTermosUso(onAceitar: () -> Unit) {
    var aceitou by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp)) {
        LogoSafePoint(Modifier.size(72.dp)); Text("Termos de Uso", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = azulEscuro)
        Spacer(Modifier.height(12.dp)); Text("O SafePoint usa mapas OpenStreetMap, NASA GIBS, alertas GDACS/USGS e locais Overpass. Dados reais sujeitos à disponibilidade. Não substitui serviços oficiais.", fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.clickable { aceitou = !aceitou }) { Checkbox(checked = aceitou, onCheckedChange = { aceitou = it }); Text("Li e concordo", fontSize = 12.sp) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (aceitou) onAceitar() }, enabled = aceitou, modifier = Modifier.fillMaxWidth()) { Text("Aceitar e Continuar") }
    }
}
@Composable fun TelaHome(navController: NavController) {
    val context = LocalContext.current; var nomeUsuario by remember { mutableStateOf("Usuário") }; val db = FirebaseFirestore.getInstance()
    LaunchedEffect(Unit) { val userId = PrefsHelper.getUserId(context); if (userId != null) db.collection("usuarios").document(userId).get().addOnSuccessListener { doc -> doc.getString("nome")?.let { if (it.isNotBlank()) nomeUsuario = it } } }
    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF7F8FA)) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(22.dp)) {
            Text("SafePoint", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SafePointBlue); Spacer(Modifier.height(12.dp))
            Text("Olá, $nomeUsuario!", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = SafePointBlue); Text("Bem-vindo ao SafePoint.\nEstamos aqui para te proteger.", modifier = Modifier.padding(top = 10.dp, bottom = 30.dp), fontSize = 17.sp, color = Color(0xFF355777))
            Card(Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOS.route) }, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8EC))) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Text("SOS", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB83238)) }
                    Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("Emergência", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB83238)); Text("Envie alerta imediato.", fontSize = 14.sp) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Mapa.route) }, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F0FF))) {
                Column(Modifier.padding(22.dp)) { Icon(Icons.Default.Map, null, tint = Color(0xFFD94848), modifier = Modifier.size(42.dp)); Text("Mapa real", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1760A7)); Text("Alertas GDACS/USGS e pontos OSM", fontSize = 14.sp) }
            }
            Spacer(Modifier.height(18.dp))
            Card(Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Historico.route) }, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7EF))) {
                Column(Modifier.padding(22.dp)) { Icon(Icons.Default.History, null, tint = Color(0xFFD94848), modifier = Modifier.size(38.dp)); Text("Histórico de alertas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF25804D)); Text("Consulte eventos reais próximos", fontSize = 14.sp) }
            }
        }
    }
}

// MAPA REAL 100% SEM DADOS FICTÍCIOS
@Composable fun MapaScreenReal(navController: NavController, onVoltar: () -> Unit) {
    val context = LocalContext.current
    var busca by remember { mutableStateOf("") }
    var filtroGravidade by remember { mutableStateOf(setOf("Crítica","Alta","Média","Baixa")) }
    var filtroTipo by remember { mutableStateOf(setOf("Enchente","Incêndio","Tempestade","Deslizamento","Seca","Ciclone","Terremoto","Erupção vulcânica")) }
    var filtroApoio by remember { mutableStateOf(setOf("Hospital","Polícia","Bombeiros","Abrigo cadastrado","Escola","Igreja/templo")) }
    var mostrarApoio by remember { mutableStateOf(true) }
    var selecionado by remember { mutableStateOf<AlertaReal?>(null) }
    var abrigoSelecionado by remember { mutableStateOf<AbrigoReal?>(null) }
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember { mutableStateOf<RotaInfo?>(null) }
    var modoRota by remember { mutableStateOf(ModoRota.CARRO) }
    var origem by remember { mutableStateOf<GeoPoint?>(null) }
    var gpsAtivo by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("Permita GPS ou pesquise local (rua, bairro, cidade, país).") }
    var alertas by remember { mutableStateOf<List<AlertaReal>>(emptyList()) }
    var apoios by remember { mutableStateOf<List<AbrigoReal>>(emptyList()) }
    var resultadosBusca by remember { mutableStateOf<List<NominatimSearchResult>>(emptyList()) }
    var mostrarResultados by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ perms -> temPermissao = perms[Manifest.permission.ACCESS_FINE_LOCATION]==true; if (temPermissao) obterLocalizacao(context){ lat,lng -> origem = GeoPoint(lat,lng); gpsAtivo=true } }
    Configuration.getInstance().userAgentValue = "SafePoint/1.4"
    var satelite by remember { mutableStateOf(false) }
    val tileRua = remember { XYTileSource("OSM",0,19,256,".png", arrayOf("https://a.tile.openstreetmap.org/","https://b.tile.openstreetmap.org/","https://c.tile.openstreetmap.org/"), "© OSM") }
    val tileSat = remember {
        object : XYTileSource("NASA GIBS",0,9,256,".jpg", arrayOf("https://gibs.earthdata.nasa.gov/"), "NASA GIBS") {
            override fun getTileURLString(pMapTile: Long): String {
                val z = org.osmdroid.util.MapTileIndex.getZoom(pMapTile); val x = org.osmdroid.util.MapTileIndex.getX(pMapTile); val y = org.osmdroid.util.MapTileIndex.getY(pMapTile)
                val data = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(System.currentTimeMillis()-2*24*60*60*1000L))
                return "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_SNPP_CorrectedReflectance_TrueColor/default/$data/GoogleMapsCompatible_Level9/$z/$y/$x.jpg"
            }
        }
    }
    val mapView = remember { MapView(context.applicationContext).apply { setTileSource(tileRua); setUseDataConnection(true); setMultiTouchControls(true); controller.setZoom(2.5); controller.setCenter(GeoPoint(0.0,0.0)) } }
    LaunchedEffect(satelite){ mapView.setTileSource(if (satelite) tileSat else tileRua); mapView.invalidate() }
    val alertasVisiveis = remember(filtroGravidade,filtroTipo,alertas){ alertas.filter { it.gravidade in filtroGravidade && it.tipo in filtroTipo } }
    val apoiosVisiveis = remember(mostrarApoio,filtroApoio,apoios){ if (!mostrarApoio) emptyList() else apoios.filter { it.categoria in filtroApoio } }

    fun carregarApoios(lat: Double, lng: Double){
        scope.launch {
            carregando=true
            try {
                apoios = OverpassRepository.buscarApoios(lat,lng,5000)
                aviso = if (apoios.isEmpty()) "Nenhum serviço OSM aqui." else "${apoios.size} pontos: ${apoios.groupBy { it.categoria }.map { "${it.value.size} ${it.key}" }.joinToString(", ")}"
            } catch(_:Exception){ aviso="Falha ao buscar apoios." }
            carregando=false; mapView.invalidate()
        }
    }
    fun carregarAlertas(lat: Double, lng: Double, global:Boolean=false){
        scope.launch {
            try {
                val lista = mutableListOf<AlertaReal>()
                try {
                    val gdacs = GdacsRepository.buscar()
                    gdacs.features.forEach { f ->
                        val c = f.geometry.coordinates; if (c.size>=2){ val lon=c[0]; val la=c[1]; if (la in -90.0..90.0 && lon in -180.0..180.0){
                            val p=f.properties; val tipo=when(p.eventtype.uppercase()){"EQ"->"Terremoto";"TC"->"Ciclone";"FL"->"Enchente";"VO"->"Erupção vulcânica";"DR"->"Seca";"WF"->"Incêndio";else->"Tempestade"}
                            val grav=when(p.alertlevel.lowercase()){"red"->"Crítica";"orange"->"Alta";"green"->"Baixa";else->"Média"}
                            lista.add(AlertaReal("gdacs-${p.eventid}", traduzirNomeEvento(p.name,tipo), p.name, tipo, grav, la, lon, p.description?:"Sem descrição", p.pubDate?:"", "GDACS"))
                        }}
                    }
                } catch(_:Exception){}
                try {
                    UsgsRepository.buscarUltimos30Dias().forEach { q ->
                        val grav=when{ q.magnitude>=6.5->"Crítica"; q.magnitude>=5.0->"Alta"; q.magnitude>=3.5->"Média"; else->"Baixa" }
                        lista.add(AlertaReal("usgs-${q.timestamp}", traduzirNomeEvento(q.nome,"Terremoto"), q.nome, "Terremoto", grav, q.latitude, q.longitude, "Mag ${"%.1f".format(q.magnitude)} • USGS", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt","BR")).format(Date(q.timestamp)), "USGS"))
                    }
                } catch(_:Exception){}
                val unicos = lista.distinctBy { it.id }
                alertas = if (global) unicos else unicos.filter { calcularDistancia(lat,lng,it.lat,it.lng)<=1000.0 }
                aviso = if (alertas.isEmpty()) "Nenhum alerta GDACS/USGS nesta área." else "${alertas.size} alertas reais"
                mapView.invalidate()
            } catch(_:Exception){ aviso="Erro ao carregar alertas." }
        }
    }
    fun aplicarLocal(lat: Double, lng: Double, nome: String){
        val ponto = CidadeDemo(nome,"",lat,lng); SessionAtual.ultimoLocal = ponto
        selecionado=null; abrigoSelecionado=null; rotaPontos=emptyList(); rotaInfo=null
        mapView.controller.animateTo(GeoPoint(lat,lng)); mapView.controller.setZoom(13.0); mapView.invalidate()
        carregarAlertas(lat,lng, global = nome=="Mundo"); if (nome!="Mundo") carregarApoios(lat,lng) else apoios=emptyList()
        mostrarResultados=false
    }
    fun pesquisar(){
        val q = busca.trim(); if (q.isBlank()){ aviso="Digite rua, bairro, cidade ou país."; return }
        aviso="Buscando em Nominatim..."
        scope.launch {
            try {
                val res = NominatimRepository.buscarPorNome(q)
                if (res.isEmpty()){ aviso="Local não encontrado. Tente cidade + país."; mostrarResultados=false }
                else {
                    resultadosBusca=res; mostrarResultados=true; aviso="${res.size} resultados • toque para ir"
                    if (res.size==1){ val r=res[0]; val la=r.lat.toDoubleOrNull(); val lo=r.lon.toDoubleOrNull(); if (la!=null && lo!=null) aplicarLocal(la,lo,r.display_name.substringBefore(",")) }
                }
            } catch(_:Exception){ aviso="Busca indisponível." }
        }
    }
    fun calcularRota(destLat: Double, destLng: Double){
        if (modoRota==ModoRota.ONIBUS){ aviso="Ônibus: sem GTFS integrado. Mostrando paradas próximas OSM e opção abrir Google Maps."; scope.launch {
            try { val paradas = OverpassRepository.buscarParadasOnibus(origem?.latitude?:destLat, origem?.longitude?:destLng, 1000); aviso = if (paradas.isEmpty()) "Nenhuma parada OSM aqui. Use transporte local." else "${paradas.size} paradas OSM próximas. GTFS real depende da cidade." } catch(_:Exception){}
        }; return }
        val ini = origem; if (ini==null){ aviso="Ative GPS ou pesquise origem primeiro."; return }
        aviso="Calculando rota Valhalla..."
        scope.launch {
            try {
                val rota = ValhallaRepository.buscarRotaCompleta(ini.longitude, ini.latitude, destLng, destLat, modoRota.valhallaCosting)
                if (rota.pontos.size<2) throw Exception("Rota inválida")
                rotaPontos=rota.pontos
                val km=rota.distanciaMetros/1000.0; val min=(rota.duracaoSeg/60.0).toInt().coerceAtLeast(1)
                val dur=if (min<60) "$min min" else "${min/60}h ${min%60}min"
                val dist=if (km<1.0) "${rota.distanciaMetros.toInt()} m" else "%.1f km".format(km)
                val chegada=SimpleDateFormat("HH:mm", Locale("pt","BR")).format(Date(System.currentTimeMillis()+rota.duracaoSeg.toLong()*1000L))
                rotaInfo=RotaInfo(km,min,dur,dist,chegada); aviso="Rota $dist • $dur • Valhalla"
                mapView.controller.animateTo(GeoPoint((rotaPontos.minOf { it.latitude }+rotaPontos.maxOf { it.latitude })/2.0, (rotaPontos.minOf { it.longitude }+rotaPontos.maxOf { it.longitude })/2.0))
                mapView.controller.setZoom(when{km<2->15.0; km<10->13.0; km<50->10.0; else->7.0}); mapView.invalidate()
            } catch(_:Exception){ aviso="Não foi possível calcular rota real. Nenhuma rota fictícia desenhada."; rotaPontos=emptyList(); rotaInfo=null }
        }
    }

    LaunchedEffect(Unit){
        if (temPermissao){ obterLocalizacao(context){ la,lo -> origem=GeoPoint(la,lo); gpsAtivo=true; mapView.controller.animateTo(origem!!); mapView.controller.setZoom(14.0); carregarApoios(la,lo); carregarAlertas(la,lo); aviso="GPS ativo" } }
        else carregarAlertas(0.0,0.0, global=true)
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }){ padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically){ Icon(Icons.Default.Shield,null,tint=Color(0xFF75E0A2), modifier=Modifier.size(28.dp)); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)){ Text("SafePoint • Mapa", color=Color.White, fontWeight=FontWeight.Bold, fontSize=18.sp); Text(if (satelite) "NASA GIBS • zoom máx 9 • baixa res" else "OSM • GDACS/USGS • Overpass • Valhalla", color=Color(0xFFBCE8D7), fontSize=10.sp) } }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment=Alignment.CenterVertically){ OutlinedTextField(value=busca, onValueChange={busca=it}, modifier=Modifier.weight(1f), placeholder={Text("Rua, bairro, cidade, país...", fontSize=12.sp)}, singleLine=true, shape=RoundedCornerShape(12.dp), colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=Color.White, unfocusedContainerColor=Color.White)); Spacer(Modifier.width(7.dp)); Button(onClick={pesquisar()}, shape=RoundedCornerShape(12.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1D9A72))){ Text("Ir") } }
                Text(aviso, color=Color.White.copy(alpha=0.9f), fontSize=10.sp, modifier=Modifier.padding(top=5.dp))
                if (mostrarResultados && resultadosBusca.isNotEmpty()){
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color.White)){
                        LazyColumn(Modifier.heightIn(max=180.dp)){
                            items(resultadosBusca){ res ->
                                Row(Modifier.fillMaxWidth().clickable {
                                    val la=res.lat.toDoubleOrNull(); val lo=res.lon.toDoubleOrNull(); if (la!=null && lo!=null) aplicarLocal(la,lo,res.display_name.substringBefore(","))
                                }.padding(10.dp)){ Icon(Icons.Default.Place,null,tint=Color(0xFF0B2545), modifier=Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(res.display_name, fontSize=11.sp, maxLines=2) }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
            LazyRow(Modifier.fillMaxWidth().height(46.dp).background(Color.White), contentPadding=PaddingValues(12.dp,5.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){
                item{ FilterChip(selected=mostrarApoio, onClick={mostrarApoio=!mostrarApoio}, label={Text("🟢 Apoio", fontSize=11.sp)}) }
                items(listOf("Crítica","Alta","Média","Baixa")){ n -> FilterChip(selected=n in filtroGravidade, onClick={ filtroGravidade = if (n in filtroGravidade) filtroGravidade-n else filtroGravidade+n }, label={Text(n, fontSize=11.sp)}) }
            }
            LazyRow(Modifier.fillMaxWidth().height(42.dp).background(Color.White), contentPadding=PaddingValues(12.dp,3.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){
                items(listOf("Enchente","Incêndio","Tempestade","Deslizamento","Seca","Ciclone","Terremoto","Erupção vulcânica")){ t -> FilterChip(selected=t in filtroTipo, onClick={ filtroTipo = if (t in filtroTipo) filtroTipo-t else filtroTipo+t }, label={Text(t, fontSize=10.sp)}) }
            }
            LazyRow(Modifier.fillMaxWidth().height(42.dp).background(Color(0xFFF0F4F8)), contentPadding=PaddingValues(12.dp,3.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){
                items(listOf("Hospital","Polícia","Bombeiros","Abrigo cadastrado","Escola","Igreja/templo")){ c -> FilterChip(selected=c in filtroApoio, onClick={ filtroApoio = if (c in filtroApoio) filtroApoio-c else filtroApoio+c }, label={Text(c, fontSize=10.sp)}) }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(8.dp).clipToBounds()){
                AndroidView(factory={mapView}, modifier=Modifier.fillMaxSize().clipToBounds()){ view ->
                    view.overlays.removeAll { it is Marker || it is Polyline }
                    if (rotaPontos.isNotEmpty()){ val linha=Polyline().apply { setPoints(rotaPontos); outlinePaint.color=android.graphics.Color.rgb(65,160,255); outlinePaint.strokeWidth=9f }; view.overlays.add(linha) }
                    alertasVisiveis.forEach { a ->
                        val m=Marker(view).apply {
                            position=GeoPoint(a.lat,a.lng); title="${a.tipo} • ${a.gravidade}"; snippet="${a.titulo} • ${a.fonte} • ${formatarDistanciaKm(origem?.let{ calcularDistancia(it.latitude,it.longitude,a.lat,a.lng)}?:0.0)}"
                            icon=criarPinoDesastreNivel(context, when(a.gravidade){"Crítica"->"Red";"Alta"->"Orange";"Média"->"Yellow";else->"Green"},48)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); setOnMarkerClickListener{ _,_ -> selecionado=a; abrigoSelecionado=null; true }
                        }; view.overlays.add(m)
                    }
                    apoiosVisiveis.forEach { a ->
                        val m=Marker(view).apply {
                            position=GeoPoint(a.lat,a.lng); title="${a.nome} (${a.categoria})"; snippet="${a.categoria} • ${a.endereco} • OSM"
                            icon=criarPinoSeguranca(context, when(a.categoria){"Hospital"->"hospital";"Polícia"->"police";"Bombeiros"->"fire_station";"Igreja/templo"->"place_of_worship";"Escola"->"school";else->"shelter"}, Color(0xFF168CBA),44)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); setOnMarkerClickListener{ _,_ -> abrigoSelecionado=a; selecionado=null; true }
                        }; view.overlays.add(m)
                    }
                    origem?.let{ o -> val m=Marker(view).apply{ position=o; title="Você"; icon=criarPinoSeguranca(context,null, Color(0xFF1976D2),42); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)}; view.overlays.add(m) }
                    view.invalidate()
                }
                Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement=Arrangement.spacedBy(6.dp)){
                    SmallFloatingActionButton(onClick={satelite=!satelite}, containerColor=Color.White){ Icon(Icons.Default.Map, if (satelite) "Ruas" else "Satélite", tint=Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick={mapView.controller.setZoom(mapView.zoomLevelDouble+1); mapView.invalidate()}, containerColor=Color.White){ Icon(Icons.Default.Add, null, tint=Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick={mapView.controller.setZoom(mapView.zoomLevelDouble-1); mapView.invalidate()}, containerColor=Color.White){ Icon(Icons.Default.Remove, null, tint=Color(0xFF0B2545)) }
                    SmallFloatingActionButton(onClick={ if (!temPermissao) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) else obterLocalizacao(context){ la,lo -> origem=GeoPoint(la,lo); gpsAtivo=true; mapView.controller.animateTo(origem!!); mapView.controller.setZoom(14.0); carregarApoios(la,lo); carregarAlertas(la,lo) } }, containerColor=Color.White){ Icon(Icons.Default.MyLocation, null, tint=Color(0xFF1976D2)) }
                }
                Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp), color=Color(0xE6FFFFFF), shape=RoundedCornerShape(8.dp)){ Text(if (satelite) "NASA GIBS • máx 9" else "© OSM • GDACS/USGS", fontSize=9.sp, modifier=Modifier.padding(7.dp,4.dp)) }
            }
            val aSel=selecionado; val bSel=abrigoSelecionado
            if (aSel!=null || bSel!=null){
                Card(Modifier.fillMaxWidth().padding(10.dp,6.dp), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(4.dp)){
                    Column(Modifier.padding(12.dp)){
                        Text(if (aSel!=null) "${aSel.titulo}" else "${bSel!!.nome} • ${bSel.categoria}", fontWeight=FontWeight.Bold, color=Color(0xFF0B2545), fontSize=14.sp)
                        Text(if (aSel!=null) "${aSel.gravidade} • ${aSel.tipo} • ${aSel.fonte} • ${aSel.dataHora}\n${aSel.descricao}" else "Categoria: ${bSel!!.categoria}\n${bSel.endereco}\nOSM: ${bSel.tags.entries.take(3).joinToString()}\nConfirme funcionamento antes de ir", fontSize=11.sp, color=Color.Gray)
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.padding(top=7.dp)){
                            listOf(ModoRota.CARRO, ModoRota.A_PE, ModoRota.BICICLETA).forEach { modo -> FilterChip(selected=modoRota==modo, onClick={ modoRota=modo; if (aSel!=null) calcularRota(aSel.lat,aSel.lng) else calcularRota(bSel!!.lat,bSel.lng) }, label={Text(modo.titulo, fontSize=10.sp)}) }
                            FilterChip(selected=false, onClick={ aviso="Ônibus: requer GTFS real. Use app local ou Google Maps transit." }, label={Text("Ônibus (GTFS)", fontSize=10.sp)})
                        }
                        Button(onClick={ if (aSel!=null) calcularRota(aSel.lat,aSel.lng) else calcularRota(bSel!!.lat,bSel.lng) }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1267A5))){ Icon(Icons.Default.Route,null); Spacer(Modifier.width(7.dp)); Text("Rota real Valhalla") }
                        if (modoRota==ModoRota.ONIBUS){
                            Spacer(Modifier.height(6.dp))
                            Button(onClick={
                                val dLat = aSel?.lat ?: bSel!!.lat; val dLng = aSel?.lng ?: bSel!!.lng
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${origem?.latitude},${origem?.longitude}&destination=$dLat,$dLng&travelmode=transit")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF388E3C))){ Icon(Icons.Default.DirectionsBus,null); Spacer(Modifier.width(6.dp)); Text("Abrir Google Maps Ônibus") }
                        }
                    }
                }
            }
            rotaInfo?.let{ info ->
                Surface(Modifier.fillMaxWidth().padding(10.dp,3.dp), shape=RoundedCornerShape(12.dp), color=Color(0xFFE5F3FF)){
                    Row(Modifier.padding(10.dp), verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.Route,null,tint=Color(0xFF1267A5)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)){ Text("Rota • ${modoRota.titulo}", fontWeight=FontWeight.Bold, fontSize=12.sp); Text("${info.distanciaTexto} • ${info.duracaoTexto}", fontSize=12.sp, color=Color(0xFF1267A5)); Text("Chegada: ${info.chegadaTexto}", fontSize=9.sp, color=Color.Gray) }; IconButton(onClick={rotaInfo=null; rotaPontos=emptyList(); mapView.invalidate()}){ Icon(Icons.Default.Close,null) }
                    }
                }
            }
        }
    }
}

// REPOSITORIOS REAIS - SEM FICTICIOS
data class GdacsResponse(val features: List<GdacsFeature>); data class GdacsFeature(val properties: GdacsProperties, val geometry: GdacsGeometry); data class GdacsProperties(val eventtype: String, val name: String, val alertlevel: String, val pubDate: String? = null, val eventid: String? = null, val description: String? = null); data class GdacsGeometry(val coordinates: List<Double>)
interface GdacsApi { @retrofit2.http.GET("gdacsapi/api/events/geteventlist/SEARCH") suspend fun getDesastres(@retrofit2.http.Query("eventlist") tipos: String = "EQ;TC;FL;VO;DR;WF", @retrofit2.http.Query("alertlevel") nivel: String = "Green;Orange;Red", @retrofit2.http.Query("limit") limite: Int = 500): GdacsResponse }
object GdacsRepository { private val api = retrofit2.Retrofit.Builder().baseUrl("https://www.gdacs.org/").addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(GdacsApi::class.java); suspend fun buscar(): GdacsResponse = api.getDesastres() }

data class TerremotoGlobal(val nome: String, val latitude: Double, val longitude: Double, val magnitude: Double, val timestamp: Long)
interface UsgsApi { @retrofit2.http.GET("fdsnws/event/1/query") suspend fun buscar(@retrofit2.http.Query("format") format: String = "geojson", @retrofit2.http.Query("starttime") starttime: String, @retrofit2.http.Query("minmagnitude") minmagnitude: Double = 2.5, @retrofit2.http.Query("orderby") orderby: String = "time", @retrofit2.http.Query("limit") limit: Int = 200): okhttp3.ResponseBody }
object UsgsRepository {
    private val client = okhttp3.OkHttpClient.Builder().callTimeout(25, java.util.concurrent.TimeUnit.SECONDS).build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://earthquake.usgs.gov/").client(client).build().create(UsgsApi::class.java)
    suspend fun buscarUltimos30Dias(): List<TerremotoGlobal> {
        val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val inicio = utc.format(Date(System.currentTimeMillis() - 30L*24*60*60*1000L))
        val raiz = org.json.JSONObject(api.buscar(starttime=inicio).string())
        val features = raiz.optJSONArray("features") ?: return emptyList()
        val resultado = mutableListOf<TerremotoGlobal>()
        for (i in 0 until features.length()){
            val feature = features.optJSONObject(i) ?: continue; val props = feature.optJSONObject("properties") ?: continue; val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
            if (coords.length()<2) continue
            val lon=coords.optDouble(0,Double.NaN); val lat=coords.optDouble(1,Double.NaN); val mag=props.optDouble("mag",Double.NaN); val time=props.optLong("time",0L)
            if (lat.isFinite() && lon.isFinite() && mag.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0 && time>0L) resultado.add(TerremotoGlobal(props.optString("place","Local não informado"), lat, lon, mag, time))
        }
        return resultado
    }
}

data class OverpassResponse(val elements: List<OverpassElement>); data class OverpassElement(val type: String, val id: Long, val lat: Double? = null, val lon: Double? = null, val tags: Map<String, String>? = null, val center: OverpassCenter? = null); data class OverpassCenter(val lat: Double, val lon: Double)
interface OverpassApi { @retrofit2.http.GET("api/interpreter") suspend fun buscarPontos(@retrofit2.http.Query("data") query: String): OverpassResponse }
object OverpassRepository {
    private val httpClient = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent","SafePoint/1.0").header("Accept-Language","pt-BR").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://overpass-api.de/").client(httpClient).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(OverpassApi::class.java)
    suspend fun buscarApoios(lat: Double, lng: Double, raio: Int = 5000): List<AbrigoReal> {
        val query = """[out:json][timeout:25];(node["amenity"~"^(hospital|police|fire_station|place_of_worship|school|shelter)$"](around:$raio,$lat,$lng);way["amenity"~"^(hospital|police|fire_station|place_of_worship|school|shelter)$"](around:$raio,$lat,$lng););out center tags;"""
        val resp = api.buscarPontos(query)
        return resp.elements.mapNotNull { el ->
            val la = el.lat ?: el.center?.lat ?: return@mapNotNull null; val lo = el.lon ?: el.center?.lon ?: return@mapNotNull null
            val tags=el.tags.orEmpty(); val amenity=tags["amenity"].orEmpty()
            val nome=tags["name"]?:when(amenity){"hospital"->"Hospital";"police"->"Delegacia";"fire_station"->"Bombeiros";"place_of_worship"->"Local religioso";"school"->"Escola";"shelter"->"Abrigo de emergência";else->"Ponto de apoio"}
            val tipo=when(amenity){"hospital"->"Hospital";"police"->"Polícia";"fire_station"->"Bombeiros";"place_of_worship"->"Igreja/templo";"school"->"Escola";"shelter"->"Abrigo cadastrado";else->"Apoio"}
            val end=listOfNotNull(tags["addr:street"],tags["addr:city"]).joinToString(", ")
            AbrigoReal("osm-${el.type}-${el.id}", tipo, nome, la, lo, end, tags)
        }.distinctBy { it.id }.take(100)
    }
    suspend fun buscarParadasOnibus(lat: Double, lng: Double, raio: Int = 1000): List<AbrigoReal> {
        val query = """[out:json][timeout:25];(node["highway"="bus_stop"](around:$raio,$lat,$lng);node["public_transport"="platform"](around:$raio,$lat,$lng););out tags;"""
        val resp = api.buscarPontos(query)
        return resp.elements.mapNotNull { el ->
            val la=el.lat?:el.center?.lat?:return@mapNotNull null; val lo=el.lon?:el.center?.lon?:return@mapNotNull null; val tags=el.tags.orEmpty()
            AbrigoReal("bus-${el.type}-${el.id}", "Parada de ônibus", tags["name"]?:"Parada de ônibus", la, lo, "", tags)
        }.take(50)
    }
}

data class NominatimSearchResult(val lat: String, val lon: String, val display_name: String)
interface NominatimSearchApi { @retrofit2.http.GET("search") suspend fun search(@retrofit2.http.Query("q") query: String, @retrofit2.http.Query("format") format: String = "json", @retrofit2.http.Query("limit") limit: Int = 8): List<NominatimSearchResult> }
object NominatimRepository {
    private val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain -> val req = chain.request().newBuilder().header("User-Agent","SafePoint/1.0").header("Accept-Language","pt-BR").build(); chain.proceed(req) }.build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://nominatim.openstreetmap.org/").client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(NominatimSearchApi::class.java)
    suspend fun buscarPorNome(query: String): List<NominatimSearchResult> { return try { api.search(query) } catch(_:Exception){ emptyList() } }
}

interface ValhallaApi { @retrofit2.http.GET("route") suspend fun route(@retrofit2.http.Query("json") requestJson: String): okhttp3.ResponseBody }
data class RotaCompleta(val pontos: List<GeoPoint>, val distanciaMetros: Double, val duracaoSeg: Double)
object ValhallaRepository {
    private val client = okhttp3.OkHttpClient.Builder().callTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build()
    private val api = retrofit2.Retrofit.Builder().baseUrl("https://valhalla1.openstreetmap.de/").client(client).build().create(ValhallaApi::class.java)
    suspend fun buscarRotaCompleta(lon1: Double, lat1: Double, lon2: Double, lat2: Double, costing: String): RotaCompleta {
        val json = org.json.JSONObject().apply {
            put("locations", org.json.JSONArray().put(org.json.JSONObject().put("lat",lat1).put("lon",lon1).put("type","break")).put(org.json.JSONObject().put("lat",lat2).put("lon",lon2).put("type","break")))
            put("costing",costing); put("units","kilometers")
        }
        val root = org.json.JSONObject(api.route(json.toString()).string())
        val trip = root.optJSONObject("trip") ?: throw IllegalStateException("Rota inválida")
        val legs = trip.optJSONArray("legs") ?: throw IllegalStateException("Sem trechos")
        val shape = legs.optJSONObject(0)?.optString("shape").orEmpty()
        val summary = trip.optJSONObject("summary") ?: throw IllegalStateException("Sem resumo")
        val points = decodePolyline6(shape)
        return RotaCompleta(points, summary.optDouble("length",0.0)*1000.0, summary.optDouble("time",0.0))
    }
    private fun decodePolyline6(encoded: String): List<GeoPoint> {
        val result = ArrayList<GeoPoint>(); var index=0; var lat=0; var lon=0
        while (index<encoded.length){
            fun readValue(): Int { var shift=0; var value=0; var b:Int; do { if (index>=encoded.length) throw IllegalArgumentException("Polyline incompleta"); b=encoded[index++].code-63; value=value or ((b and 0x1f) shl shift); shift+=5 } while (b>=0x20); return if ((value and 1)!=0) (value shr 1).inv() else value shr 1 }
            lat+=readValue(); lon+=readValue(); result.add(GeoPoint(lat/1_000_000.0, lon/1_000_000.0))
        }
        return result
    }
}

// HELPERS
fun criarPinoDesastreNivel(context: Context, nivel: String, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w=tamanhoPx; val h=(w*1.35f).toInt(); val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); val canvas=AndroidCanvas(bmp)
    val cor=when(nivel){"Red"->android.graphics.Color.rgb(211,47,47);"Orange"->android.graphics.Color.rgb(245,124,0);"Yellow"->android.graphics.Color.rgb(213,165,27);else->android.graphics.Color.rgb(56,142,60)}
    val paint=Paint().apply{ color=cor; isAntiAlias=true; style=Paint.Style.FILL }
    val path=android.graphics.Path().apply{ moveTo(w/2f,0f); cubicTo(w*0.95f,0f,w.toFloat(),h*0.25f,w.toFloat(),h*0.38f); cubicTo(w.toFloat(),h*0.62f,w*0.75f,h*0.78f,w/2f,h.toFloat()); cubicTo(w*0.25f,h*0.78f,0f,h*0.62f,0f,h*0.38f); cubicTo(0f,h*0.25f,w*0.05f,0f,w/2f,0f); close() }
    canvas.drawPath(path,paint); val texto=Paint().apply{ color=android.graphics.Color.WHITE; textSize=w*0.55f; textAlign=Paint.Align.CENTER; isFakeBoldText=true; isAntiAlias=true }; canvas.drawText("!",w/2f,h*0.55f,texto)
    return BitmapDrawable(context.resources,bmp)
}
fun criarPinoSeguranca(context: Context, categoria: String?, corFundo: Color, tamanhoPx: Int): android.graphics.drawable.Drawable {
    val w=tamanhoPx; val bmp=Bitmap.createBitmap(w,w,Bitmap.Config.ARGB_8888); val canvas=AndroidCanvas(bmp); val cx=w/2f; val cy=w/2f; val raio=w/2f-4f
    val circ=Paint().apply{ color=android.graphics.Color.argb((corFundo.alpha*255).toInt(), (corFundo.red*255).toInt(), (corFundo.green*255).toInt(), (corFundo.blue*255).toInt()); isAntiAlias=true }; canvas.drawCircle(cx,cy,raio,circ)
    val borda=Paint().apply{ color=android.graphics.Color.WHITE; style=Paint.Style.STROKE; strokeWidth=w*0.07f; isAntiAlias=true }; canvas.drawCircle(cx,cy,raio-(w*0.035f),borda)
    val texto=Paint().apply{ color=android.graphics.Color.WHITE; textSize=w*0.55f; textAlign=Paint.Align.CENTER; isFakeBoldText=true }; val letra=when(categoria){"hospital"->"H";"fire_station"->"B";"police"->"P";"shelter"->"A";"place_of_worship"->"R";else->"S"}; canvas.drawText(letra,cx,cy-(texto.descent()+texto.ascent())/2f,texto)
    return BitmapDrawable(context.resources,bmp)
}
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double { val R=6371.0; val dLat=Math.toRadians(lat2-lat1); val dLon=Math.toRadians(lon2-lon1); val a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2); val c=2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a)); return R*c }
fun formatarDistanciaKm(km: Double): String = if (km<1.0) "${(km*1000).toInt()} m" else "%.1f km".format(km)
fun obterLocalizacao(context: Context, onLocationReceived: (Double, Double)->Unit){
    val lm=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) return
    try {
        val gps=try{ lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)}catch(_:Exception){null}
        val net=try{ lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)}catch(_:Exception){null}
        val best=listOfNotNull(gps,net).maxByOrNull{it.time}
        if (best!=null){ onLocationReceived(best.latitude,best.longitude); return }
    } catch(_:Exception){}
}
fun traduzirNomeEvento(nomeFonte: String, tipoPT: String): String {
    var nome=nomeFonte.trim()
    val subs=listOf(Regex("(?i)^earthquake(?:s)?\\s+in\\s+") to "Terremoto em ", Regex("(?i)^flood(?:ing)?\\s+in\\s+") to "Enchente em ", Regex("(?i)^wildfire(?:s)?\\s+in\\s+") to "Incêndio em ", Regex("(?i)^drought\\s+in\\s+") to "Seca em ", Regex("(?i)^volcanic eruption\\s+in\\s+") to "Erupção em ", Regex("(?i)^tropical cyclone\\s*") to "Ciclone ")
    for ((r,s) in subs) if (r.containsMatchIn(nome)){ nome=r.replaceFirst(nome,s); break }
    return nome.ifBlank { tipoPT }
}
fun formatarDataAlerta(dataFonte: String?): String {
    if (dataFonte.isNullOrBlank()) return "Data não informada"
    val patterns=listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX","yyyy-MM-dd'T'HH:mm:ssX","yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'")
    for (p in patterns){ try{ val parser=SimpleDateFormat(p, Locale.US).apply{ timeZone=TimeZone.getTimeZone("UTC"); isLenient=false }; val parsed=parser.parse(dataFonte)?:continue; return SimpleDateFormat("dd/MM/yyyy HH:mm 'UTC'", Locale("pt","BR")).apply{ timeZone=TimeZone.getTimeZone("UTC")}.format(parsed) } catch(_:Exception){} }
    return dataFonte
}

// HISTORICO REAL - sem cidade padrão
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TelaHistoricoReal(navController: NavController){
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var buscaLocal by remember { mutableStateOf("") }; var localNome by remember { mutableStateOf("Sem localização") }
    var latBase by remember { mutableStateOf<Double?>(null) }; var lonBase by remember { mutableStateOf<Double?>(null) }
    var filtroGrav by remember { mutableStateOf("Todos") }; var filtroTipo by remember { mutableStateOf("Todos") }
    var alertas by remember { mutableStateOf<List<AlertaReal>>(emptyList()) }; var carregando by remember { mutableStateOf(false) }; var erro by remember { mutableStateOf<String?>(null) }; var atualizacao by remember { mutableStateOf("") }
    var selecionado by remember { mutableStateOf<AlertaReal?>(null) }
    val temPermissao = ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    fun carregarPorCoords(lat: Double, lon: Double){
        if (carregando) return; latBase=lat; lonBase=lon; carregando=true; erro=null; alertas=emptyList()
        scope.launch {
            try {
                val resp=GdacsRepository.buscar()
                val lista=resp.features.mapNotNull { f ->
                    val c=f.geometry.coordinates; if (c.size<2) return@mapNotNull null; val lo=c[0]; val la=c[1]; if (!la.isFinite()||!lo.isFinite()||la !in -90.0..90.0) return@mapNotNull null
                    val dist=calcularDistancia(lat,lon,la,lo); if (dist>300.0) return@mapNotNull null
                    val nivel=when(f.properties.alertlevel.lowercase()){"red"->"Crítica";"orange"->"Alta";"green"->"Baixa";else->"Moderada"}
                    val tipo=when(f.properties.eventtype.uppercase()){"FL"->"Enchente";"WF"->"Incêndio";"TC"->"Ciclone";"DR"->"Seca";"EQ"->"Terremoto";"VO"->"Erupção vulcânica";else->"Evento"}
                    AlertaReal(f.properties.eventid?:"gdacs-$la-$lo", traduzirNomeEvento(f.properties.name,tipo), f.properties.name, tipo, nivel, la, lo, f.properties.description?.replace(Regex("<[^>]*>")," ")?.take(300)?:"Sem descrição", f.properties.pubDate?:"", "GDACS")
                }.sortedBy{ calcularDistancia(lat,lon,it.lat,it.lng) }
                alertas=lista; if (lista.isEmpty()) erro="GDACS não retornou eventos em 300km. Não significa ausência de risco."; atualizacao=SimpleDateFormat("dd/MM HH:mm", Locale("pt","BR")).format(Date())
            } catch(_:Exception){ erro="Falha ao consultar GDACS." } finally{ carregando=false }
        }
    }
    fun pesquisarLocal(){
        val termo=buscaLocal.trim(); if (termo.isBlank()) return; scope.launch {
            carregando=true; erro=null
            try{
                val res=NominatimRepository.buscarPorNome(termo); val primeiro=res.firstOrNull(); if (primeiro==null){ erro="Local não encontrado."; carregando=false; return@launch }
                val la=primeiro.lat.toDoubleOrNull(); val lo=primeiro.lon.toDoubleOrNull(); if (la==null||lo==null){ erro="Coordenadas inválidas."; carregando=false; return@launch }
                localNome=primeiro.display_name.substringBefore(","); latBase=la; lonBase=lo; carregando=false; carregarPorCoords(la,lo)
            } catch(_:Exception){ erro="Falha na pesquisa."; carregando=false }
        }
    }
    LaunchedEffect(Unit){ if (temPermissao) obterLocalizacao(context){ la,lo -> localNome="Localização atual"; carregarPorCoords(la,lo) } }
    val filtrados=alertas.filter{ (filtroGrav=="Todos"||it.gravidade==filtroGrav)&&(filtroTipo=="Todos"||it.tipo==filtroTipo) }
    val corNivel: (String)->Color = { n -> when(n){"Crítica"->Color(0xFFD32F2F);"Alta"->Color(0xFFEF6C00);"Moderada"->Color(0xFFF9A825); else->Color(0xFF2E7D32)} }
    Scaffold(bottomBar={ BarraNavegacaoInferior(navController) }){ padding ->
        Column(Modifier.fillMaxSize().padding(padding)){
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B2545), Color(0xFF0D5B52)))).padding(14.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.History,null,tint=Color(0xFF83E3B1), modifier=Modifier.size(28.dp)); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)){ Text("Histórico de alertas", color=Color.White, fontWeight=FontWeight.Bold, fontSize=21.sp); Text("GDACS • raio 300km • real", color=Color(0xFFC9E8DF), fontSize=11.sp) } }
                if (atualizacao.isNotBlank()) Text("Atualizado $atualizacao", color=Color.White.copy(alpha=.75f), fontSize=10.sp)
            }
            Row(Modifier.fillMaxWidth().padding(12.dp,8.dp), verticalAlignment=Alignment.CenterVertically){
                OutlinedTextField(value=buscaLocal, onValueChange={buscaLocal=it}, modifier=Modifier.weight(1f), placeholder={Text("Buscar cidade, bairro, endereço", fontSize=11.sp)}, singleLine=true, shape=RoundedCornerShape(12.dp))
                Spacer(Modifier.width(7.dp)); Button(onClick={pesquisarLocal()}, enabled=!carregando, colors=ButtonDefaults.buttonColors(containerColor=SafePointGreen), shape=RoundedCornerShape(12.dp)){ Text("Buscar") }
            }
            Row(Modifier.fillMaxWidth().padding(14.dp,0.dp), verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.Place,null,tint=SafePointGreen, modifier=Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Área: $localNome", fontSize=11.sp, color=Color(0xFF34495E)) }
            Text("Gravidade", Modifier.padding(start=16.dp, top=5.dp), fontWeight=FontWeight.Bold, fontSize=11.sp); LazyRow(contentPadding=PaddingValues(12.dp,4.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){ items(listOf("Todos","Crítica","Alta","Moderada","Baixa")){ f -> FilterChip(selected=filtroGrav==f, onClick={filtroGrav=f}, label={Text(f, fontSize=10.sp)}) } }
            Text("Tipo", Modifier.padding(start=16.dp, top=2.dp), fontWeight=FontWeight.Bold, fontSize=11.sp); LazyRow(contentPadding=PaddingValues(12.dp,4.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){ items(listOf("Todos","Enchente","Incêndio","Ciclone","Seca","Terremoto","Erupção vulcânica")){ f -> FilterChip(selected=filtroTipo==f, onClick={filtroTipo=f}, label={Text(f, fontSize=10.sp)}) } }
            when{
                carregando && alertas.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment=Alignment.Center){ Column(horizontalAlignment=Alignment.CenterHorizontally){ CircularProgressIndicator(); Spacer(Modifier.height(9.dp)); Text("Consultando GDACS...", fontSize=12.sp) } }
                erro!=null && alertas.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment=Alignment.Center){ Column(horizontalAlignment=Alignment.CenterHorizontally){ Text(erro!!, fontSize=12.sp, textAlign=TextAlign.Center); Spacer(Modifier.height(8.dp)); Button(onClick={ val la=latBase; val lo=lonBase; if (la!=null && lo!=null) carregarPorCoords(la,lo) }){ Text("Tentar novamente") } } }
                filtrados.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment=Alignment.Center){ Text("Nenhum evento real com estes filtros.", fontSize=12.sp, textAlign=TextAlign.Center) }
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding=PaddingValues(12.dp,8.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){
                    items(filtrados, key={it.id}){ alerta ->
                        val cor=corNivel(alerta.gravidade)
                        Card(Modifier.fillMaxWidth().clickable{selecionado=alerta}, shape=RoundedCornerShape(13.dp), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(2.dp)){
                            Row(Modifier.fillMaxWidth()){
                                Box(Modifier.width(6.dp).fillMaxHeight().background(cor))
                                Column(Modifier.weight(1f).padding(12.dp), verticalArrangement=Arrangement.spacedBy(5.dp)){
                                    Row(verticalAlignment=Alignment.Top){ Column(Modifier.weight(1f)){ Text(alerta.titulo, color=Color(0xFF0B2545), fontWeight=FontWeight.Bold, fontSize=13.sp); Text(alerta.tipo, color=Color(0xFF64748B), fontSize=11.sp) }; Surface(color=cor, shape=RoundedCornerShape(7.dp)){ Text(alerta.gravidade, color=Color.White, fontSize=9.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(7.dp,5.dp)) } }
                                    Text(alerta.cidade, color=Color(0xFF475569), fontSize=11.sp); Text("Publicado: ${alerta.dataHora}", color=Color.Gray, fontSize=10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    selecionado?.let{ a ->
        AlertDialog(onDismissRequest={selecionado=null}, title={Text(a.titulo, fontWeight=FontWeight.Bold)}, text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Surface(color=corNivel(a.gravidade), shape=RoundedCornerShape(7.dp)){ Text("${a.gravidade} • ${a.tipo}", color=Color.White, fontWeight=FontWeight.Bold, modifier=Modifier.padding(9.dp,6.dp)) }
                Text(a.descricao, fontSize=13.sp); Text("Fonte: ${a.fonte} • ${a.dataHora}", fontSize=10.sp, color=Color.Gray)
            }
        }, confirmButton={ TextButton(onClick={selecionado=null}){ Text("Fechar") } })
    }
}

@Composable fun TelaPerfil(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore){
    val context=LocalContext.current; var nome by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var carregando by remember { mutableStateOf(true) }
    LaunchedEffect(Unit){ val userId=PrefsHelper.getUserId(context)?:auth.currentUser?.uid; if (userId!=null) db.collection("usuarios").document(userId).get().addOnSuccessListener{ doc -> nome=doc.getString("nome")?:""; email=doc.getString("email")?:""; carregando=false }.addOnFailureListener{ carregando=false } else carregando=false }
    Scaffold(bottomBar={ BarraNavegacaoInferior(navController) }){ padding ->
        if (carregando) Box(Modifier.fillMaxSize().padding(padding), contentAlignment=Alignment.Center){ CircularProgressIndicator() }
        else Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)){ Text("Perfil", fontSize=22.sp, fontWeight=FontWeight.Bold, color=azulEscuro); Spacer(Modifier.height(16.dp)); Text("Nome: $nome"); Text("E-mail: $email"); Spacer(Modifier.height(24.dp)); Button(onClick={ auth.signOut(); PrefsHelper.clearUserId(context); navController.navigate(Screen.Login.route){ popUpTo(0){inclusive=true}} }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE53935))){ Text("Sair", color=Color.White) } }
    }
}
@Composable fun TelaSOS(onVoltar: () -> Unit){
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().background(Color.White)){
        Box(Modifier.fillMaxWidth().background(Color(0xFFB71C1C)).padding(20.dp)){ Row(verticalAlignment=Alignment.CenterVertically){ IconButton(onClick=onVoltar){ Icon(Icons.Default.ArrowBack,null,tint=Color.White) }; Text("Emergência SOS", color=Color.White, fontWeight=FontWeight.Bold, fontSize=22.sp) } }
        Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)){
            listOf(Triple("190","Polícia Militar",Color(0xFF1565C0)), Triple("192","SAMU",Color(0xFF2E7D32)), Triple("193","Bombeiros",Color(0xFFD32F2F)), Triple("199","Defesa Civil",Color(0xFFF57C00))).forEach{ (num,nome,cor) ->
                Card(Modifier.fillMaxWidth().clickable{ val intent=Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")); context.startActivity(intent) }, shape=RoundedCornerShape(14.dp), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(3.dp)){
                    Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(48.dp).clip(CircleShape).background(cor), contentAlignment=Alignment.Center){ Icon(Icons.Default.Call,null,tint=Color.White) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)){ Text(nome, fontWeight=FontWeight.Bold); Text("Ligar $num", color=cor, fontWeight=FontWeight.Bold, fontSize=12.sp) } }
                }
            }
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFE3F2FD)), shape=RoundedCornerShape(16.dp), modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(16.dp)){ Text("Compartilhar localização", fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Button(onClick={ obterLocalizacao(context){ la,lo -> val uri=Uri.parse("https://wa.me/?text=🚨 SOS SafePoint - Preciso de ajuda! https://maps.google.com/?q=$la,$lo"); context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF25D366)), modifier=Modifier.fillMaxWidth()){ Text("WhatsApp") } }
            }
        }
    }
}
@Composable fun TelaAjudaComoUsar(onVoltar: () -> Unit){
    val duvidas=listOf("Como criar conta?" to "Tela inicial > Criar conta. Preencha nome, e-mail, senha.", "Mapa real?" to "OSM + NASA GIBS + GDACS/USGS + Overpass. Sem dados fictícios.", "Filtros?" to "No topo do mapa, toque chips gravidade/tipo/categoria apoio.", "Rota?" to "Toque marcador > Carro/A pé/Bicicleta > Calcular rota real Valhalla.", "Ônibus?" to "Requer GTFS real da cidade. Mostramos paradas OSM e botão Google Maps transit.", "Sem internet?" to "Mapa precisa de conexão. GPS funciona offline.")
    var aberta by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())){
        Box(Modifier.fillMaxWidth().background(SafePointGreen).padding(20.dp)){ Row(verticalAlignment=Alignment.CenterVertically){ IconButton(onClick=onVoltar){ Icon(Icons.Default.ArrowBack,null,tint=Color.White) }; Text("Ajuda", color=Color.White, fontWeight=FontWeight.Bold, fontSize=22.sp) } }
        Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)){
            duvidas.forEachIndexed{ i,(p,r) ->
                Card(Modifier.fillMaxWidth().clickable{ aberta = if (aberta==i) null else i }, colors=CardDefaults.cardColors(containerColor=Color(0xFFF7F9FC))){
                    Column(Modifier.padding(16.dp)){ Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){ Text("${i+1}. $p", fontWeight=FontWeight.Bold, fontSize=13.sp, modifier=Modifier.weight(1f)); Icon(if (aberta==i) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }; if (aberta==i){ Spacer(Modifier.height(10.dp)); Text(r, fontSize=12.sp) } }
                }
            }
        }
    }
}
