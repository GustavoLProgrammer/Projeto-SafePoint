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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

// CORES ORIGINAIS DO APP
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

// ESTRUTURAS DE NAVEGAÇÃO E DADOS
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

data class AlertaItem(
    val id: String,
    val cidade: String,
    val tipo: String,
    val gravidade: String,
    val lat: Double,
    val lng: Double,
    val descricao: String,
    val dataHora: String
)

data class AbrigoItem(
    val id: String,
    val cidade: String,
    val nome: String,
    val lat: Double,
    val lng: Double,
    val endereco: String
)

data class CidadeRef(val nome: String, val uf: String, val lat: Double, val lng: Double, val aliases: List<String> = emptyList())
data class RotaInfo(val distanciaKm: Double, val duracaoMin: Int, val duracaoTexto: String, val distanciaTexto: String, val chegadaTexto: String)

enum class ModoTransporte(val titulo: String, val velocidadeKmh: Double, val icone: ImageVector) {
    CARRO("Carro", 45.0, Icons.Default.DirectionsCar),
    MOTO("Moto", 52.0, Icons.Default.TwoWheeler),
    ONIBUS("Ônibus", 22.0, Icons.Default.DirectionsBus),
    BICICLETA("Bicicleta", 15.0, Icons.Default.DirectionsBike),
    A_PE("A pé", 4.5, Icons.Default.DirectionsWalk)
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

// CÁLCULOS GEOGRÁFICOS E GPS
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

fun formatarDistanciaKm(km: Double): String = if (km < 1.0) "${(km * 1000).toInt()} m" else String.format(Locale("pt", "BR"), "%.1f km", km)

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
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        providers.forEach { provider ->
            try { lm.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper()) } catch (_: Exception) { }
        }
    } catch (_: Exception) {}
}

// BASE GEOGRÁFICA 100% TERRESTRE E ALGORITMOS
object SafePointGeoEngine {
    val cidades = listOf(
        CidadeRef("São Paulo", "SP", -23.5505, -46.6333, listOf("sao paulo", "sp", "capital paulista")),
        CidadeRef("Rio de Janeiro", "RJ", -22.9068, -43.1729, listOf("rio", "rj", "rio de janeiro")),
        CidadeRef("Belo Horizonte", "MG", -19.9167, -43.9345, listOf("bh", "belo horizonte")),
        CidadeRef("Brasília", "DF", -15.7939, -47.8828, listOf("brasilia", "df")),
        CidadeRef("Curitiba", "PR", -25.4284, -49.2733, listOf("curitiba", "pr")),
        CidadeRef("Porto Alegre", "RS", -30.0346, -51.2177, listOf("porto alegre", "rs")),
        CidadeRef("Salvador", "BA", -12.9777, -38.5016, listOf("salvador", "ba")),
        CidadeRef("Fortaleza", "CE", -3.7319, -38.5267, listOf("fortaleza", "ce")),
        CidadeRef("Recife", "PE", -8.0476, -34.8770, listOf("recife", "pe")),
        CidadeRef("Campinas", "SP", -22.9056, -47.0608, listOf("campinas")),
        CidadeRef("Goiânia", "GO", -16.6869, -49.2648, listOf("goiania")),
        CidadeRef("Manaus", "AM", -3.1190, -60.0217, listOf("manaus")),
        CidadeRef("Florianópolis", "SC", -27.5954, -48.5480, listOf("florianopolis")),
        CidadeRef("Vitória", "ES", -20.3155, -40.3128, listOf("vitoria")),
        CidadeRef("Santos", "SP", -23.9608, -46.3336, listOf("santos")),
        CidadeRef("Ribeirão Preto", "SP", -21.1704, -47.8103, listOf("ribeirao preto"))
    )

    val tiposDisponiveis = listOf("Enchente", "Incêndio", "Tempestade", "Deslizamento", "Terremoto", "Vendaval", "Acidente Químico", "Seca")
    val gravidadesDisponiveis = listOf("Baixa", "Média", "Alta", "Crítica")

    fun corDoTipo(tipo: String): Color = when (tipo) {
        "Enchente" -> Color(0xFF0288D1)
        "Incêndio" -> Color(0xFFE64A19)
        "Tempestade" -> Color(0xFF7B1FA2)
        "Deslizamento" -> Color(0xFF795548)
        "Terremoto" -> Color(0xFFC62828)
        "Vendaval" -> Color(0xFF1565C0)
        "Acidente Químico" -> Color(0xFFF57F17)
        "Seca" -> Color(0xFFFB8C00)
        else -> Color(0xFFD32F2F)
    }

    val listaAlertas: List<AlertaItem> by lazy {
        val list = mutableListOf<AlertaItem>()
        val descricoes = mapOf(
            "Enchente" to "Vias alagadas e risco de transbordamento de canais pluviais. Evite áreas baixas.",
            "Incêndio" to "Foco de incêndio com emissão intensa de fumaça. Risco de propagação rápida.",
            "Tempestade" to "Chuva torrencial com descargas elétricas e rajadas de vento severas.",
            "Deslizamento" to "Instabilidade e saturação de solo em encosta. Monitoramento ativo.",
            "Terremoto" to "Tremor e abalo registrado na região. Afaste-se de estruturas instáveis.",
            "Vendaval" to "Rajadas de vento superiores a 75 km/h. Risco de queda de árvores e postes.",
            "Acidente Químico" to "Vazamento de substância perigosa com isolamento do perímetro.",
            "Seca" to "Período crítico de estiagem severa e baixa umidade relativa do ar."
        )
        val datas = listOf("Hoje às 10:15", "Hoje às 08:30", "Ontem às 22:40", "Ontem às 19:10", "05/11 às 15:20", "04/11 às 11:45")

        cidades.forEachIndexed { cIdx, cidade ->
            val sinalLng = if (cidade.nome in listOf("Santos", "Rio de Janeiro", "Salvador", "Fortaleza", "Recife", "Florianópolis", "Vitória")) -1.0 else 1.0
            for (i in 0..2) {
                val tipo = tiposDisponiveis[(cIdx * 3 + i) % tiposDisponiveis.size]
                val gravidade = gravidadesDisponiveis[(cIdx + i) % gravidadesDisponiveis.size]
                val dLat = (if (i == 0) 0.011 else if (i == 1) -0.013 else 0.007) + (cIdx % 3) * 0.002
                val dLng = sinalLng * ((if (i == 0) 0.013 else if (i == 1) 0.016 else -0.011) + (cIdx % 2) * 0.002)
                val dataHora = datas[(cIdx + i) % datas.size]
                val desc = descricoes[tipo] ?: "Ocorrência sob supervisão da Defesa Civil."
                list.add(
                    AlertaItem(
                        id = "alt-${cidade.nome}-$i",
                        cidade = cidade.nome,
                        tipo = tipo,
                        gravidade = gravidade,
                        lat = cidade.lat + dLat,
                        lng = cidade.lng + dLng,
                        descricao = "$desc Região: ${cidade.nome}.",
                        dataHora = dataHora
                    )
                )
            }
        }
        list
    }

    val listaAbrigos: List<AbrigoItem> by lazy {
        val list = mutableListOf<AbrigoItem>()
        val nomesAbrigos = listOf("Ponto de Apoio Integrado", "Centro de Evacuação Comunitário", "Abrigo Emergencial Municipal", "Posto Central de Resgate")
        cidades.forEachIndexed { cIdx, cidade ->
            val sinalLng = if (cidade.nome in listOf("Santos", "Rio de Janeiro", "Salvador", "Fortaleza", "Recife", "Florianópolis", "Vitória")) -1.0 else 1.0
            for (j in 0..1) {
                val dLat = if (j == 0) -0.017 else 0.019
                val dLng = sinalLng * (if (j == 0) -0.019 else 0.021)
                val nomeAbrigo = nomesAbrigos[(cIdx + j) % nomesAbrigos.size]
                list.add(
                    AbrigoItem(
                        id = "abr-${cidade.nome}-$j",
                        cidade = cidade.nome,
                        nome = "$nomeAbrigo • ${cidade.nome}",
                        lat = cidade.lat + dLat,
                        lng = cidade.lng + dLng,
                        endereco = "Av. Central Segura, ${120 + j * 180} - ${cidade.nome}"
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

// DESENHO DE ÍCONES DO MAPA
fun criarIconeSetaGps(context: Context, tamanhoPx: Int = 48): android.graphics.drawable.Drawable {
    val bmp = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val cx = tamanhoPx / 2f
    val cy = tamanhoPx / 2f
    val raio = tamanhoPx / 2.2f

    val paintSombra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(60, 0, 0, 0) }
    canvas.drawCircle(cx, cy + 2f, raio, paintSombra)

    val paintHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(cx, cy, raio, paintHalo)

    val paintAzul = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(21, 101, 192) }
    canvas.drawCircle(cx, cy, raio - 3f, paintAzul)

    val pathSeta = AndroidPath().apply {
        moveTo(cx, cy - raio * 0.65f)
        lineTo(cx + raio * 0.52f, cy + raio * 0.52f)
        lineTo(cx, cy + raio * 0.22f)
        lineTo(cx - raio * 0.52f, cy + raio * 0.52f)
        close()
    }
    val paintSeta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL }
    canvas.drawPath(pathSeta, paintSeta)
    return BitmapDrawable(context.resources, bmp)
}

fun criarPinoAbrigoVerde(context: Context, tamanhoPx: Int = 46): android.graphics.drawable.Drawable {
    val bmp = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val cx = tamanhoPx / 2f
    val cy = tamanhoPx / 2f
    val raio = tamanhoPx / 2.2f

    val paintSombra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(70, 0, 0, 0) }
    canvas.drawCircle(cx, cy + 3f, raio, paintSombra)

    val paintVerde = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30, 168, 76) }
    canvas.drawCircle(cx, cy, raio, paintVerde)

    val paintBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = tamanhoPx * 0.07f
    }
    canvas.drawCircle(cx, cy, raio - (tamanhoPx * 0.035f), paintBorda)

    val paintMais = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; style = Paint.Style.STROKE
        strokeWidth = tamanhoPx * 0.16f; strokeCap = Paint.Cap.ROUND
    }
    val tamCruz = raio * 0.50f
    canvas.drawLine(cx - tamCruz, cy, cx + tamCruz, cy, paintMais)
    canvas.drawLine(cx, cy - tamCruz, cx, cy + tamCruz, paintMais)
    return BitmapDrawable(context.resources, bmp)
}

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
        this.color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = w * 0.08f
    }
    val path = AndroidPath().apply {
        moveTo(w / 2f, 0f)
        cubicTo(w * 0.95f, 0f, w.toFloat(), h * 0.25f, w.toFloat(), h * 0.38f)
        cubicTo(w.toFloat(), h * 0.62f, w * 0.75f, h * 0.78f, w / 2f, h.toFloat())
        cubicTo(w * 0.25f, h * 0.78f, 0f, h * 0.62f, 0f, h * 0.38f)
        cubicTo(0f, h * 0.25f, w * 0.05f, 0f, w / 2f, 0f); close()
    }
    canvas.drawPath(path, paintCor)
    canvas.drawPath(path, paintBorda)

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
        this.color = android.graphics.Color.WHITE; textSize = w * 0.42f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    canvas.drawText(letra, w / 2f, h * 0.50f, paintTexto)
    return BitmapDrawable(context.resources, bmp)
}

fun gerarRotaInteligenteContorno(inicio: GeoPoint, destino: GeoPoint, pontoAlerta: GeoPoint?): List<GeoPoint> {
    val pontos = mutableListOf<GeoPoint>()
    pontos.add(inicio)
    val latDist = destino.latitude - inicio.latitude
    val lngDist = destino.longitude - inicio.longitude
    val totalPassos = 14

    for (i in 1 until totalPassos) {
        val progresso = i / totalPassos.toDouble()
        var latInter = inicio.latitude + latDist * progresso
        var lngInter = inicio.longitude + lngDist * progresso

        if (pontoAlerta != null) {
            val distAoAlerta = calcularDistancia(latInter, lngInter, pontoAlerta.latitude, pontoAlerta.longitude)
            if (distAoAlerta < 2.5) {
                val desvio = (1.0 - (distAoAlerta / 2.5)) * 0.007
                latInter += desvio * kotlin.math.sin(progresso * Math.PI)
                lngInter += desvio * kotlin.math.cos(progresso * Math.PI)
            }
        } else {
            val jitter = kotlin.math.sin(progresso * Math.PI * 3.0) * 0.0015
            latInter += jitter
            lngInter -= jitter * 0.6
        }
        pontos.add(GeoPoint(latInter, lngInter))
    }
    pontos.add(destino)
    return pontos
}

// TELA DO MAPA PROFISSIONAL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreenProfissional(navController: NavController, onVoltar: () -> Unit) {
    val context = LocalContext.current
    var busca by remember { mutableStateOf("") }
    var cidadeAtual by remember { mutableStateOf(SafePointGeoEngine.cidades.first()) }
    var minhaPosicao by remember { mutableStateOf(GeoPoint(cidadeAtual.lat, cidadeAtual.lng)) }
    var temGps by remember { mutableStateOf(false) }

    // FILTROS: Não começam forçados/ativos; o usuário escolhe
    var filtroGravidade by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filtroTipo by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mostrarAbrigos by remember { mutableStateOf(true) }

    var alertaSelecionado by remember { mutableStateOf<AlertaItem?>(null) }
    var abrigoSelecionado by remember { mutableStateOf<AbrigoItem?>(null) }
    var rotaPontos by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember { mutableStateOf<RotaInfo?>(null) }
    var modoTransporte by remember { mutableStateOf(ModoTransporte.CARRO) }
    var abrigoDestinoRota by remember { mutableStateOf<AbrigoItem?>(null) }
    var mapaSatelite by remember { mutableStateOf(false) }
    var avisoBusca by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val concedida = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedida) {
            temGps = true
            obterLocalizacao(context) { lat, lng -> minhaPosicao = GeoPoint(lat, lng) }
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
                Toast.makeText(context, "Abra as configurações do celular para permitir o acesso à sua localização.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

    LaunchedEffect(Unit) { solicitarPermissaoGpsUmaVez() }

    val tileSourceRua = remember {
        XYTileSource("OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://a.tile.openstreetmap.org/", "https://b.tile.openstreetmap.org/", "https://c.tile.openstreetmap.org/"), "© OpenStreetMap contributors")
    }
    val tileSourceSatelite = remember {
        object : XYTileSource("NASA GIBS True Color", 0, 9, 256, ".jpg", arrayOf("https://gibs.earthdata.nasa.gov/"), "NASA Earthdata") {
            override fun getTileURLString(pMapTile: Long): String {
                val z = org.osmdroid.util.MapTileIndex.getZoom(pMapTile)
                val x = org.osmdroid.util.MapTileIndex.getX(pMapTile)
                val y = org.osmdroid.util.MapTileIndex.getY(pMapTile)
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

    fun iniciarRotaDeFuga(abrigoAlvo: AbrigoItem, alertaDesvio: AlertaItem?) {
        abrigoDestinoRota = abrigoAlvo
        val destinoGeo = GeoPoint(abrigoAlvo.lat, abrigoAlvo.lng)
        val perigoGeo = if (alertaDesvio != null) GeoPoint(alertaDesvio.lat, alertaDesvio.lng) else null
        val rota = gerarRotaInteligenteContorno(minhaPosicao, destinoGeo, perigoGeo)
        rotaPontos = rota

        var distTotalKm = 0.0
        for (i in 0 until rota.size - 1) {
            distTotalKm += calcularDistancia(rota[i].latitude, rota[i].longitude, rota[i + 1].latitude, rota[i + 1].longitude)
        }
        if (distTotalKm < 0.5) distTotalKm = 0.8

        val horas = distTotalKm / modoTransporte.velocidadeKmh
        val minutos = (horas * 60).toInt().coerceAtLeast(2)
        val duracaoTexto = if (minutos < 60) "$minutos min" else "${minutos / 60}h ${minutos % 60}min"
        val distTexto = String.format(Locale("pt", "BR"), "%.1f km", distTotalKm)
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, minutos) }
        val chegadaTexto = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(cal.time)

        rotaInfo = RotaInfo(distTotalKm, minutos, duracaoTexto, distTexto, chegadaTexto)
        avisoBusca = "Rota segura calculada até ${abrigoAlvo.nome.take(28)}..."
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
            avisoBusca = "Região não encontrada. Tente outra cidade."
        }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }, containerColor = Color(0xFFF3F6F8)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Barra de Busca Superior
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
                        placeholder = { Text("Buscar cidade ou região", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { buscarLocalDigitado() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9A72))
                    ) { Text("Ir") }
                }
                if (avisoBusca.isNotBlank()) {
                    Text(avisoBusca, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Filtros de Gravidade (Escolha livre do usuário)
            LazyRow(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 4.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = mostrarAbrigos, onClick = { mostrarAbrigos = !mostrarAbrigos }, label = { Text("🟢 Abrigos (+)", fontSize = 11.sp) })
                }
                items(SafePointGeoEngine.gravidadesDisponiveis) { nivel ->
                    val ativo = nivel in filtroGravidade
                    FilterChip(selected = ativo, onClick = { filtroGravidade = if (ativo) filtroGravidade - nivel else filtroGravidade + nivel }, label = { Text(nivel, fontSize = 11.sp) })
                }
            }

            // Filtros de Tipos (Escolha livre do usuário)
            LazyRow(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 2.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SafePointGeoEngine.tiposDisponiveis) { tipo ->
                    val ativo = tipo in filtroTipo
                    FilterChip(selected = ativo, onClick = { filtroTipo = if (ativo) filtroTipo - tipo else filtroTipo + tipo }, label = { Text(tipo, fontSize = 11.sp) })
                }
            }

            // Área do Mapa
            Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
                    view.overlays.removeAll { it is Marker || it is Polyline }

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

                    val pinoUsuario = Marker(view).apply {
                        position = minhaPosicao
                        title = "Sua localização atual"
                        icon = criarIconeSetaGps(context, 48)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    view.overlays.add(pinoUsuario)
                    view.invalidate()
                }

                // Botões de Ação Rápida
                Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(onClick = { mapaSatelite = !mapaSatelite }, containerColor = Color.White) {
                        Icon(Icons.Default.Layers, if (mapaSatelite) "Modo Ruas" else "Modo Satélite", tint = Color(0xFF0B2545))
                    }
                    SmallFloatingActionButton(onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble + 1); mapView.invalidate() }, containerColor = Color.White) {
                        Icon(Icons.Default.Add, "Mais zoom", tint = Color(0xFF0B2545))
                    }
                    SmallFloatingActionButton(onClick = { mapView.controller.setZoom(mapView.zoomLevelDouble - 1); mapView.invalidate() }, containerColor = Color.White) {
                        Icon(Icons.Default.Remove, "Menos zoom", tint = Color(0xFF0B2545))
                    }
                    SmallFloatingActionButton(onClick = {
                        solicitarPermissaoGpsUmaVez()
                        mapView.controller.animateTo(minhaPosicao)
                        mapView.controller.setZoom(14.5)
                    }, containerColor = Color.White) {
                        Icon(Icons.Default.MyLocation, "Centralizar GPS", tint = Color(0xFF1976D2))
                    }
                }

                // Card de Detalhes ao Clicar no Alerta (Com Botão X para Fechar)
                alertaSelecionado?.let { alerta ->
                    Card(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).background(SafePointGeoEngine.corDoTipo(alerta.tipo), CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${alerta.tipo} • Gravidade ${alerta.gravidade}", fontWeight = FontWeight.Bold, color = Color(0xFF0B2545), fontSize = 16.sp)
                                    Text(alerta.dataHora, color = Color.Gray, fontSize = 11.sp)
                                }
                                IconButton(onClick = { alertaSelecionado = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF0B2545))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(alerta.descricao, color = Color(0xFF334155), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(10.dp))

                            Text("Meio de transporte para evacuação:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0B2545))
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                ModoTransporte.values().forEach { modo ->
                                    FilterChip(
                                        selected = modoTransporte == modo,
                                        onClick = {
                                            modoTransporte = modo
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
                                    ) { Text("Cancelar rota", fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }

                // Card de Detalhes ao Clicar no Abrigo Verde
                abrigoSelecionado?.let { abrigo ->
                    Card(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
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
                                    OutlinedButton(onClick = { cancelarRota() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))) {
                                        Text("Cancelar")
                                    }
                                }
                            }
                        }
                    }
                }

                // Resumo da Rota Ativa
                rotaInfo?.let { info ->
                    if (alertaSelecionado == null && abrigoSelecionado == null) {
                        Surface(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5E9), shadowElevation = 6.dp) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(modoTransporte.icone, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Rota Segura • ${modoTransporte.titulo}", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 14.sp)
                                    Text("${info.distanciaTexto} • Chegada às ${info.chegadaTexto} (${info.duracaoTexto})", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(onClick = { cancelarRota() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(10.dp)) {
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

// TELA DO HISTÓRICO DE ALERTAS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoReal(navController: NavController) {
    var busca by remember { mutableStateOf("") }
    var filtroGravidade by remember { mutableStateOf<String?>(null) }
    var filtroTipo by remember { mutableStateOf<String?>(null) }
    var alertaDetalhe by remember { mutableStateOf<AlertaItem?>(null) }

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
                        Text("Ocorrências registradas nas regiões monitoradas", color = Color(0xFFC9E8DF), fontSize = 11.sp)
                    }
                }
            }

            // Busca
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

            // Filtros de Gravidade (Escolha do usuário)
            Text("Gravidade do evento", Modifier.padding(start = 16.dp, top = 2.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SafePointGeoEngine.gravidadesDisponiveis) { g ->
                    val sel = filtroGravidade == g
                    FilterChip(selected = sel, onClick = { filtroGravidade = if (sel) null else g }, label = { Text(g, fontSize = 11.sp) })
                }
            }

            // Filtros de Tipo (Escolha do usuário)
            Text("Tipo de ocorrência", Modifier.padding(start = 16.dp, top = 2.dp), color = Color(0xFF34495E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SafePointGeoEngine.tiposDisponiveis) { t ->
                    val sel = filtroTipo == t
                    FilterChip(selected = sel, onClick = { filtroTipo = if (sel) null else t }, label = { Text(t, fontSize = 11.sp) })
                }
            }

            // Lista de Ocorrências
            if (alertasFiltrados.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum alerta encontrado com os filtros selecionados.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        Surface(color = cor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                            Text(alerta.gravidade, color = cor, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(alerta.descricao, color = Color(0xFF334155), fontSize = 12.sp, maxLines = 2)
                                    Spacer(Modifier.height(6.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(alerta.dataHora, color = Color.Gray, fontSize = 10.sp)
                                        Text("Ver detalhes", color = Color(0xFF16845F), fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Detalhes do Histórico
    alertaDetalhe?.let { a ->
        val cor = SafePointGeoEngine.corDoTipo(a.tipo)
        AlertDialog(
            onDismissRequest = { alertaDetalhe = null },
            title = { Text("${a.tipo} • ${a.cidade}", color = Color(0xFF0B2545), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = cor, shape = RoundedCornerShape(6.dp)) {
                        Text("${a.gravidade} • ${a.tipo}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
                    }
                    Text(a.descricao, color = Color(0xFF334155), fontSize = 13.sp)
                    Text("Horário de registro: ${a.dataHora}", color = Color(0xFF475569), fontSize = 12.sp)
                    Text("Fonte: Defesa Civil Municipal", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { alertaDetalhe = null }) { Text("Fechar") } }
        )
    }
}

// TELA DE PERFIL
@OptIn(ExperimentalMaterial3Api::class)
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

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fotoUriLocal = it }
    }

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
        } else carregando = false
    }

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        dataNascimento = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                    }
                    mostrarDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(bottomBar = { BarraNavegacaoInferior(navController) }) { paddingValues ->
        if (carregando) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = azulEscuro) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().background(FundoGradiente).statusBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                            val img = fotoUriLocal?.toString() ?: fotoUrlSalva
                            if (!img.isNullOrBlank()) AsyncImage(model = img, contentDescription = "Foto", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.Person, null, tint = azulEscuro, modifier = Modifier.size(70.dp))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(azulLinhas), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(if (nome.isEmpty()) "Usuário" else nome, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Editar Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulEscuro)
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, leadingIcon = { IconeUsuario(cor = azulEscuro, modifier = Modifier.size(20.dp)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Outlined.Email, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = azulEscuro) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = dataNascimento, onValueChange = {}, label = { Text("Data de nascimento") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = azulEscuro) }, trailingIcon = { IconButton(onClick = { mostrarDatePicker = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth().clickable { mostrarDatePicker = true }, shape = RoundedCornerShape(12.dp), singleLine = true, readOnly = true)

                    BotaoGradiente(texto = if (salvando) "Salvando..." else "Salvar Alterações") {
                        if (salvando) return@BotaoGradiente
                        salvando = true
                        val userId = PrefsHelper.getUserId(context) ?: auth.currentUser?.uid
                        if (userId != null) {
                            val perfilMap = hashMapOf<String, Any>("nome" to nome, "email" to email, "telefone" to telefone, "dataNascimento" to dataNascimento)
                            db.collection("usuarios").document(userId).set(perfilMap, SetOptions.merge()).addOnSuccessListener {
                                if (fotoUriLocal != null) {
                                    try {
                                        val storageRef = FirebaseStorage.getInstance().reference.child("fotos_perfil/$userId/profile.jpg")
                                        storageRef.putFile(fotoUriLocal!!).continueWithTask { task ->
                                            if (!task.isSuccessful) throw task.exception!!
                                            storageRef.downloadUrl
                                        }.addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val url = task.result.toString()
                                                db.collection("usuarios").document(userId).set(mapOf("fotoUrl" to url), SetOptions.merge()).addOnSuccessListener {
                                                    fotoUrlSalva = url
                                                    fotoUriLocal = null
                                                    Toast.makeText(context, "Perfil e foto atualizados!", Toast.LENGTH_SHORT).show()
                                                    salvando = false
                                                }.addOnFailureListener { salvando = false }
                                            } else salvando = false
                                        }
                                    } catch (_: Exception) { salvando = false }
                                } else {
                                    Toast.makeText(context, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                                    salvando = false
                                }
                            }.addOnFailureListener { salvando = false }
                        } else salvando = false
                    }

                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            PrefsHelper.clearUserId(context)
                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) { Text("Sair", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// TELA SOS EMERGÊNCIA
@Composable
fun TelaSOS(onVoltar: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFB71C1C)).statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onVoltar, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Emergência SOS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Ação rápida para salvar vidas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
        Column(modifier = Modifier.padding(20.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BotaoEmergencia(numero = "190", nome = "Polícia Militar", cor = Color(0xFF1565C0), icon = Icons.Default.LocalPolice, context = context)
            BotaoEmergencia(numero = "192", nome = "SAMU - Ambulância", cor = Color(0xFF2E7D32), icon = Icons.Default.MedicalServices, context = context)
            BotaoEmergencia(numero = "193", nome = "Corpo de Bombeiros", cor = Color(0xFFD32F2F), icon = Icons.Default.FireTruck, context = context)
            BotaoEmergencia(numero = "199", nome = "Defesa Civil", cor = Color(0xFFF57C00), icon = Icons.Default.Shield, context = context)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compartilhar minha localização", fontWeight = FontWeight.Bold, color = azulEscuro)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Envie sua localização exata para contatos de emergência via WhatsApp", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            obterLocalizacao(context) { lat, lng ->
                                val uri = Uri.parse("https://wa.me/?text=🚨 SOS SafePoint - Preciso de ajuda! Minha localização: https://maps.google.com/?q=$lat,$lng")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartilhar no WhatsApp", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun BotaoEmergencia(numero: String, nome: String, cor: Color, icon: ImageVector, context: Context) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth().clickable {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
        context.startActivity(intent)
    }) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(cor), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Ligar $numero", fontSize = 12.sp, color = cor, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Call, null, tint = cor)
        }
    }
}

// TELA DE AJUDA E DÚVIDAS
@Composable
fun TelaAjudaComoUsar(onVoltar: () -> Unit) {
    val duvidas = listOf(
        "Como funciona o mapa?" to "O mapa monitora ocorrências com cores específicas para cada risco e pontos de apoio verdes com o símbolo '+'. Você pode selecionar filtros para ver apenas o que precisa.",
        "Como traçar uma rota segura?" to "Toque sobre um alerta ou abrigo no mapa e escolha seu meio de transporte (Carro, Moto, Ônibus, Bicicleta ou A pé). Em seguida, toque em 'Traçar rota ao abrigo'.",
        "Como cancelar uma rota?" to "A qualquer momento durante uma evacuação, toque no botão vermelho 'Cancelar rota' para encerrar a navegação.",
        "Como usar o SOS?" to "Acesse o SOS para ligar com um toque para a Polícia (190), SAMU (192), Bombeiros (193) ou Defesa Civil (199), ou enviar sua localização no WhatsApp.",
        "Como consultar o histórico?" to "Acesse a aba 'Histórico' para visualizar ocorrências passadas registradas por onde você navegou com data, hora e descrição completa."
    )
    var aberta by remember { mutableStateOf<Int?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().background(SafePointGreen).statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onVoltar, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Ajuda e Dúvidas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Guia prático de uso do SafePoint", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Perguntas Frequentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = azulEscuro, modifier = Modifier.padding(bottom = 8.dp))
            duvidas.forEachIndexed { index, (pergunta, resposta) ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), elevation = CardDefaults.cardElevation(if (aberta == index) 3.dp else 1.dp), modifier = Modifier.fillMaxWidth().clickable { aberta = if (aberta == index) null else index }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. $pergunta", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = azulEscuro, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            Icon(if (aberta == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = azulEscuro)
                        }
                        if (aberta == index) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFE5EAF0))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(resposta, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}