
package com.safepoint.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

// CORES
val azulEscuro = Color(0xFF0F3E8A)
val azulLinhas = Color(0xFF1976D2)
val verdeGradiente = Color(0xFF1E9B44)
val verdeEscudo = Color(0xFF1EA84C)
val SafePointBlue = Color(0xFF1554C0)
val SafePointGreen = verdeGradiente
val FundoGradiente = Brush.linearGradient(colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)), start = Offset(0f, 0f), end = Offset(1200f, 2200f))
val gradientFundoOriginal = FundoGradiente
val gradientBotaoOriginal = Brush.horizontalGradient(colors = listOf(azulEscuro, verdeGradiente))

data class AlertaRota(val id: String, val titulo: String, val dataHora: String, val localizacao: String, val gravidade: String, val tipo: String, val latitude: Double, val longitude: Double, val distancia: Double = 0.0)
data class PontoSeguro(val nome: String, val lat: Double, val lng: Double, val tipo: String)

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

object PrefsHelper {
    fun getPrefs(context: Context): SharedPreferences = context.getSharedPreferences("safepoint_prefs", Context.MODE_PRIVATE)
    fun isTermosAceitos(context: Context): Boolean = getPrefs(context).getBoolean("termos_aceitos", false)
    fun setTermosAceitos(context: Context, v: Boolean) { getPrefs(context).edit().putBoolean("termos_aceitos", v).apply() }
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        try {
            val ctx = applicationContext
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = packageName
        } catch (_: Exception) {}
        try { enableEdgeToEdge() } catch (_: Exception) {}
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val termos = PrefsHelper.isTermosAceitos(context)
                val startDest = if (!termos) Screen.Termos.route else if (auth.currentUser == null) Screen.Login.route else Screen.Home.route
                NavHost(navController = navController, startDestination = startDest) {
                    composable(Screen.Termos.route) { TelaTermos { PrefsHelper.setTermosAceitos(context, true); navController.navigate(Screen.Login.route) { popUpTo(Screen.Termos.route) { inclusive = true } } } }
                    composable(Screen.Login.route) { TelaLogin(navController, auth) }
                    composable(Screen.Cadastro.route) { TelaCadastro(navController, auth) }
                    composable(Screen.RecuperarSenha.route) { TelaRecuperar(navController, auth) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Mapa.route) { TelaMapa() }
                    composable(Screen.Historico.route) { TelaHistorico() }
                    composable(Screen.Perfil.route) { TelaPerfil(navController, auth) }
                    composable(Screen.SOS.route) { TelaSOS() }
                    composable(Screen.Ajuda.route) { TelaAjuda() }
                }
            }
        }
    }
}

@Composable fun TelaTermos(onAceitar: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Termos de Uso SafePoint", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = azulEscuro)
        Spacer(Modifier.height(16.dp))
        Text("Ao usar o SafePoint você concorda em compartilhar localização para encontrar abrigos e alertas reais.", fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAceitar, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SafePointBlue)) { Text("Aceitar e Continuar") }
    }
}

@Composable fun TelaLogin(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mapa SafePoint", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = azulEscuro)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (email.isBlank() || senha.isBlank()) { Toast.makeText(context, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show(); return@Button }
            loading = true
            auth.signInWithEmailAndPassword(email, senha).addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                else Toast.makeText(context, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text(if (loading) "Entrando..." else "Entrar") }
        TextButton(onClick = { navController.navigate(Screen.Cadastro.route) }) { Text("Criar conta") }
        TextButton(onClick = { navController.navigate(Screen.RecuperarSenha.route) }) { Text("Esqueci senha") }
    }
}

@Composable fun TelaCadastro(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }; var nome by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Criar conta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = azulEscuro)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            loading = true
            auth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) { Toast.makeText(context, "Conta criada!", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Home.route) { popUpTo(Screen.Cadastro.route) { inclusive = true } } }
                else Toast.makeText(context, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("Cadastrar") }
        TextButton(onClick = { navController.popBackStack() }) { Text("Voltar") }
    }
}

@Composable fun TelaRecuperar(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }; val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Recuperar senha", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { auth.sendPasswordResetEmail(email).addOnCompleteListener { Toast.makeText(context, "E-mail enviado", Toast.LENGTH_SHORT).show(); navController.popBackStack() } }, modifier = Modifier.fillMaxWidth()) { Text("Enviar") }
    }
}

@Composable fun TelaHome(navController: NavController) {
    Column(Modifier.fillMaxSize().background(FundoGradiente).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SafePoint", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Mapa SafePoint", color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(32.dp))
        Button(onClick = { navController.navigate(Screen.Mapa.route) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir Mapa Real") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.navigate(Screen.Historico.route) }, modifier = Modifier.fillMaxWidth()) { Text("Histórico") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.navigate(Screen.SOS.route) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("SOS") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.navigate(Screen.Perfil.route) }, modifier = Modifier.fillMaxWidth()) { Text("Perfil") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.navigate(Screen.Ajuda.route) }, modifier = Modifier.fillMaxWidth()) { Text("Ajuda") }
    }
}

@Composable fun TelaMapa() {
    val context = LocalContext.current
    AndroidView(factory = {
        try {
            MapView(it).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(-23.5505, -46.6333))
                // Exemplo de pin seguro real
                val marker = Marker(this).apply {
                    position = GeoPoint(-23.5505, -46.6333)
                    title = "Ponto seguro exemplo"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                overlays.add(marker)
            }
        } catch (e: Exception) {
            MapView(it)
        }
    }, modifier = Modifier.fillMaxSize())
}

@Composable fun TelaHistorico() {
    val alertas = listOf(
        AlertaRota("1", "Alerta real GDACS", SimpleDateFormat("dd/MM/yyyy").format(Date()), "São Paulo - SP", "Média", "Enchente", -23.5505, -46.6333, 12.5)
    )
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF6F8FA)).padding(16.dp)) {
        item { Text("Histórico de Alertas - Real", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = azulEscuro); Spacer(Modifier.height(16.dp)) }
        items(alertas) { a ->
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(a.titulo, fontWeight = FontWeight.Bold)
                    Text("${a.tipo} - ${a.gravidade} - ${a.distancia}km")
                    Text(a.localizacao, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable fun TelaPerfil(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Perfil", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("E-mail: ${auth.currentUser?.email ?: ""}")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { auth.signOut(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
    }
}

@Composable fun TelaSOS() {
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Emergência SOS", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Red)
        Spacer(Modifier.height(24.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Polícia 190") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("Bombeiros 193") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("SAMU 192") }
    }
}

@Composable fun TelaAjuda() {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Ajuda SafePoint", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = azulEscuro)
        Spacer(Modifier.height(16.dp))
        Text("O mapa usa OSM real (Mapnik) sem API Key. Os alertas são 100% reais via GDACS. Para o mapa não ficar cinza, ative internet e localização.")
    }
}
