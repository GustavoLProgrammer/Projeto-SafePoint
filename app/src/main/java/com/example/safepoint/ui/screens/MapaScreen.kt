package com.example.safepoint.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.safepoint.LogoSafePoint
import com.example.safepoint.R
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

val PinoAzul = Color(0xFF1976D2)
val PinoLaranja = Color(0xFFF57C00)
val PinoVermelho = Color(0xFFD32F2F)
val ChipAtivoDesastre = Color(0xFFD32F2F)
val ChipAtivoSeguranca = Color(0xFF2E7D32)

data class Ocorrencia(
    val titulo: String,
    val endereco: String,
    val lat: Double,
    val lng: Double,
    val tipo: TipoOcorrencia,
    val categoria: String? = null
)

enum class TipoOcorrencia { SAIDA, MONITOR, DESASTRE, SEGURANCA }

val NOMES_DESASTRES = mapOf(
    "EQ" to "Terremoto",
    "TC" to "Ciclone",
    "FL" to "Enchente",
    "VO" to "Vulcão",
    "DR" to "Seca",
    "WF" to "Incêndio"
)

val NOMES_SEGURANCA = mapOf(
    "hospital" to "Hospital",
    "fire_station" to "Bombeiros",
    "police" to "Polícia",
    "shelter" to "Abrigo",
    "pharmacy" to "Farmácia"
)

val CORES_SEGURANCA = mapOf(
    "hospital" to Color(0xFFD32F2F),
    "fire_station" to Color(0xFFF57C00),
    "police" to Color(0xFF1565C0),
    "shelter" to Color(0xFF2E7D32),
    "pharmacy" to Color(0xFF7B1FA2)
)

// ============ PONTOS DE SEGURANÇA ESPALHADOS ============
val PONTOS_SEGURANCA_MOCK = listOf(
    // ============ SÃO PAULO - SP ============
    Ocorrencia("Hospital das Clínicas", "Hospital", -23.5575, -46.6700, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital São Paulo", "Hospital", -23.5990, -46.6410, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros SP - Centro", "Bombeiros", -23.5450, -46.6390, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Militar SP - Zona Sul", "Polícia", -23.6520, -46.7120, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Zona Leste", "Abrigo", -23.5420, -46.4700, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Zona Norte", "Farmácia", -23.4780, -46.6280, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ RIO DE JANEIRO - RJ ============
    Ocorrencia("Hospital Copa D'Or", "Hospital", -22.9670, -43.1790, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Miguel Couto", "Hospital", -22.9880, -43.2260, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros RJ - Centro", "Bombeiros", -22.9060, -43.1780, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil RJ - Zona Norte", "Polícia", -22.8500, -43.3200, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Zona Oeste", "Abrigo", -22.9100, -43.5600, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Niterói", "Farmácia", -22.8800, -43.1000, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ BRASÍLIA - DF ============
    Ocorrencia("Hospital de Base do DF", "Hospital", -15.8000, -47.8850, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Santa Lúcia", "Hospital", -15.8090, -47.8880, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros DF - Asa Norte", "Bombeiros", -15.7880, -47.8820, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Federal DF", "Polícia", -15.7970, -47.8790, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Taguatinga", "Abrigo", -15.8300, -48.0500, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Gama", "Farmácia", -16.0200, -48.0600, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ BELO HORIZONTE - MG ============
    Ocorrencia("Hospital de Clínicas UFMG", "Hospital", -19.9240, -43.9400, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Mater Dei", "Hospital", -19.9340, -43.9420, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros MG - Centro", "Bombeiros", -19.9240, -43.9380, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Militar MG - Pampulha", "Polícia", -19.8500, -43.9600, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Betim", "Abrigo", -19.9700, -44.2000, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Contagem", "Farmácia", -19.9300, -44.0500, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ SALVADOR - BA ============
    Ocorrencia("Hospital Geral do Estado", "Hospital", -12.9830, -38.5010, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Santo Amaro", "Hospital", -12.9730, -38.5120, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros BA - Centro", "Bombeiros", -12.9770, -38.5040, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil BA - Itapuã", "Polícia", -12.9500, -38.3700, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Camaçari", "Abrigo", -12.7000, -38.3200, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Lauro de Freitas", "Farmácia", -12.8900, -38.3300, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ FORTALEZA - CE ============
    Ocorrencia("Hospital Geral de Fortaleza", "Hospital", -3.7430, -38.5010, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital São José", "Hospital", -3.7350, -38.5050, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros CE - Centro", "Bombeiros", -3.7280, -38.5100, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Militar CE - Messejana", "Polícia", -3.8300, -38.4900, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Caucaia", "Abrigo", -3.7300, -38.6600, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Maracanaú", "Farmácia", -3.8800, -38.6200, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ MANAUS - AM ============
    Ocorrencia("Hospital 28 de Agosto", "Hospital", -3.0920, -60.0120, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Tropical", "Hospital", -3.0880, -60.0160, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros AM - Centro", "Bombeiros", -3.1340, -60.0230, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil AM - Zona Leste", "Polícia", -3.1000, -59.9500, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Iranduba", "Abrigo", -3.2800, -60.1800, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Manacapuru", "Farmácia", -3.2900, -60.6200, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ PORTO ALEGRE - RS ============
    Ocorrencia("Hospital de Clínicas RS", "Hospital", -30.0350, -51.2200, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Moinhos de Vento", "Hospital", -30.0280, -51.2100, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros RS - Centro", "Bombeiros", -30.0320, -51.2280, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil RS - Zona Sul", "Polícia", -30.1000, -51.2300, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Canoas", "Abrigo", -29.9200, -51.1800, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Viamão", "Farmácia", -30.0800, -51.0200, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ RECIFE - PE ============
    Ocorrencia("Hospital da Restauração", "Hospital", -8.0480, -34.8950, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Português", "Hospital", -8.0560, -34.8990, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros PE - Centro", "Bombeiros", -8.0520, -34.8920, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil PE - Boa Viagem", "Polícia", -8.1200, -34.9000, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Olinda", "Abrigo", -7.9900, -34.8400, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Jaboatão", "Farmácia", -8.1800, -35.0000, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ CURITIBA - PR ============
    Ocorrencia("Hospital de Clínicas PR", "Hospital", -25.4280, -49.2640, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Cajuru", "Hospital", -25.4250, -49.2400, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros PR - Centro", "Bombeiros", -25.4300, -49.2700, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil PR - Boqueirão", "Polícia", -25.5000, -49.2400, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo São José dos Pinhais", "Abrigo", -25.5300, -49.2000, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Colombo", "Farmácia", -25.2900, -49.2200, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ GOIÂNIA - GO ============
    Ocorrencia("Hospital Santa Helena", "Hospital", -16.6870, -49.2650, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital de Urgências de Goiânia", "Hospital", -16.6810, -49.2570, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros GO - Centro", "Bombeiros", -16.6850, -49.2600, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil GO - Aparecida", "Polícia", -16.8200, -49.2400, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Trindade", "Abrigo", -16.6500, -49.4900, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Senador Canedo", "Farmácia", -16.7100, -49.0900, TipoOcorrencia.SEGURANCA, "pharmacy"),

    // ============ FLORIANÓPOLIS - SC ============
    Ocorrencia("Hospital Universitário UFSC", "Hospital", -27.6010, -48.5200, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Hospital Celso Ramos", "Hospital", -27.5920, -48.5520, TipoOcorrencia.SEGURANCA, "hospital"),
    Ocorrencia("Bombeiros SC - Centro", "Bombeiros", -27.5950, -48.5480, TipoOcorrencia.SEGURANCA, "fire_station"),
    Ocorrencia("Polícia Civil SC - São José", "Polícia", -27.6100, -48.6300, TipoOcorrencia.SEGURANCA, "police"),
    Ocorrencia("Abrigo Palhoça", "Abrigo", -27.6400, -48.6700, TipoOcorrencia.SEGURANCA, "shelter"),
    Ocorrencia("Farmácia Biguaçu", "Farmácia", -27.4900, -48.6500, TipoOcorrencia.SEGURANCA, "pharmacy")
)

@Composable
fun MapaScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current

    val desastresGlobais = remember { mutableStateListOf<Ocorrencia>() }
    val pontosSeguranca = remember { mutableStateListOf<Ocorrencia>() }

    var ocorrenciaSelecionada by remember { mutableStateOf<Ocorrencia?>(null) }
    var mostrarDialog by remember { mutableStateOf(false) }
    var abaAtual by remember { mutableStateOf("MAPA") }

    var filtrosDesastres by remember { mutableStateOf(setOf("EQ", "TC", "FL", "VO", "DR", "WF")) }
    var filtrosSeguranca by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "SafePoint/1.0 (Android)"
    }

    // Atualiza lista de pontos baseado nos filtros
    LaunchedEffect(filtrosSeguranca) {
        pontosSeguranca.clear()
        if (filtrosSeguranca.isEmpty()) return@LaunchedEffect
        PONTOS_SEGURANCA_MOCK.forEach { ponto ->
            if (ponto.categoria in filtrosSeguranca) {
                pontosSeguranca.add(ponto)
            }
        }
    }

    val tileSource = remember {
        XYTileSource(
            "Wikimedia", 1, 19, 256, ".png",
            arrayOf("https://maps.wikimedia.org/osm-intl/"),
            "© OpenStreetMap, © Wikimedia"
        )
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
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }

    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        temPermissao = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(temPermissao) {
        if (temPermissao && mapView.overlays.none { it is MyLocationNewOverlay }) {
            mapView.overlays.add(myLocationOverlay)
        }
    }

    // Busca desastres globais (GDACS)
    LaunchedEffect(filtrosDesastres) {
        desastresGlobais.clear()
        if (filtrosDesastres.isEmpty()) return@LaunchedEffect
        try {
            val resposta = GdacsRepository.buscarDesastres(
                tipos = filtrosDesastres.joinToString(";")
            )
            resposta.features.forEach { feature ->
                val lon = feature.geometry.coordinates.getOrNull(0) ?: 0.0
                val lat = feature.geometry.coordinates.getOrNull(1) ?: 0.0
                val nomeTipo = NOMES_DESASTRES[feature.properties.eventtype] ?: "Desastre"
                desastresGlobais.add(
                    Ocorrencia(
                        titulo = "$nomeTipo: ${feature.properties.name}",
                        endereco = "Alerta: ${feature.properties.alertlevel}",
                        lat = lat,
                        lng = lon,
                        tipo = TipoOcorrencia.DESASTRE
                    )
                )
            }
        } catch (e: Exception) { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocationOverlay.disableMyLocation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            view.overlays.removeAll { it is Marker }

            // Desastres (pinos vermelhos)
            desastresGlobais.forEach { oc ->
                val marker = Marker(view).apply {
                    position = GeoPoint(oc.lat, oc.lng)
                    title = oc.titulo
                    snippet = oc.endereco
                    val d = ContextCompat.getDrawable(view.context, R.drawable.pino_vermelho)
                    val px = (24 * view.context.resources.displayMetrics.density).toInt()
                    d?.setBounds(0, 0, px, (px * 1.3f).toInt())
                    icon = d
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        ocorrenciaSelecionada = oc
                        true
                    }
                }
                view.overlays.add(marker)
            }

            // Pontos de segurança
            pontosSeguranca.forEach { oc ->
                val marker = Marker(view).apply {
                    position = GeoPoint(oc.lat, oc.lng)
                    title = oc.titulo
                    snippet = oc.endereco

                    val corPino = CORES_SEGURANCA[oc.categoria] ?: Color(0xFF1565C0)
                    val px = (48 * view.context.resources.displayMetrics.density).toInt()
                    icon = criarPinoSeguranca(view.context, oc.categoria, corPino, px)

                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        ocorrenciaSelecionada = oc
                        true
                    }
                }
                view.overlays.add(marker)
            }

            view.invalidate()
        }

        // TOPO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoSafePoint(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAFEPOINT", color = SafePointBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("Alertas Globais", color = SafePointBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        // FILTROS
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NOMES_DESASTRES.forEach { (codigo, nome) ->
                    val ativo = codigo in filtrosDesastres
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (ativo) ChipAtivoDesastre else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.clickable {
                            filtrosDesastres = if (ativo) filtrosDesastres - codigo else filtrosDesastres + codigo
                        }
                    ) {
                        Text(
                            nome,
                            color = if (ativo) Color.White else Color(0xFF1A1A1A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NOMES_SEGURANCA.forEach { (codigo, nome) ->
                    val ativo = codigo in filtrosSeguranca
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (ativo) ChipAtivoSeguranca else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.clickable {
                            filtrosSeguranca = if (ativo) filtrosSeguranca - codigo else filtrosSeguranca + codigo
                        }
                    ) {
                        Text(
                            nome,
                            color = if (ativo) Color.White else Color(0xFF1A1A1A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // BOTÃO MINHA LOCALIZAÇÃO
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 170.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    if (!temPermissao) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        val minhaLocalizacao: GeoPoint? = myLocationOverlay.myLocation
                        if (minhaLocalizacao != null) {
                            mapView.controller.animateTo(minhaLocalizacao)
                            mapView.controller.setZoom(18.0)
                        } else {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val ultima = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (ultima != null) {
                                mapView.controller.animateTo(GeoPoint(ultima.latitude, ultima.longitude))
                                mapView.controller.setZoom(18.0)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização", tint = SafePointBlue, modifier = Modifier.size(28.dp))
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 170.dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SafePointGreen, Color(0xFF1EA84C))))
                .clickable { mostrarDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White, modifier = Modifier.size(28.dp))
                Text("Adicionar\nDesastre", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        // CARD INFERIOR
        ocorrenciaSelecionada?.let { oc ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(oc.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                        Text(oc.endereco, fontSize = 13.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { ocorrenciaSelecionada = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }
            }
        }

        // BARRA INFERIOR
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ItemNavegacao("Início", Icons.Default.Home, abaAtual == "INICIO") { onVoltar() }
            ItemNavegacao("Mapa", Icons.Default.Map, abaAtual == "MAPA") { abaAtual = "MAPA" }
            ItemNavegacao("Histórico", Icons.Default.History, abaAtual == "HISTORICO") { abaAtual = "HISTORICO" }
            ItemNavegacao("Perfil", Icons.Default.Person, abaAtual == "PERFIL") { abaAtual = "PERFIL" }
        }
    }

    if (mostrarDialog) {
        AdicionarDesastreDialog(
            onDismiss = { mostrarDialog = false },
            onAdicionar = { titulo, endereco, lat, lng ->
                desastresGlobais.add(Ocorrencia(titulo, endereco, lat, lng, TipoOcorrencia.DESASTRE))
                ocorrenciaSelecionada = null
                mostrarDialog = false
            }
        )
    }
}

fun criarPinoSeguranca(
    context: Context,
    categoria: String?,
    corFundo: Color,
    tamanhoPx: Int
): android.graphics.drawable.Drawable {
    val largura = tamanhoPx
    val altura = tamanhoPx

    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centroX = largura / 2f
    val centroY = altura / 2f
    val raio = largura / 2f - 4f

    val paintSombra = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(80, 0, 0, 0)
        isAntiAlias = true
    }
    canvas.drawCircle(centroX, centroY + 3f, raio, paintSombra)

    val paintCirculo = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(
            (corFundo.alpha * 255).toInt(),
            (corFundo.red * 255).toInt(),
            (corFundo.green * 255).toInt(),
            (corFundo.blue * 255).toInt()
        )
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(centroX, centroY, raio, paintCirculo)

    val paintBorda = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = largura * 0.07f
        isAntiAlias = true
    }
    canvas.drawCircle(centroX, centroY, raio - (largura * 0.035f), paintBorda)

    val paintTexto = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = largura * 0.55f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    val letra = when (categoria) {
        "hospital" -> "H"
        "fire_station" -> "B"
        "police" -> "P"
        "shelter" -> "A"
        "pharmacy" -> "F"
        else -> "?"
    }
    val yTexto = centroY - (paintTexto.descent() + paintTexto.ascent()) / 2f
    canvas.drawText(letra, centroX, yTexto, paintTexto)

    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun ItemNavegacao(
    label: String,
    icon: ImageVector,
    ativo: Boolean,
    onClick: () -> Unit
) {
    val cor = if (ativo) SafePointBlue else Color.Gray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = cor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = cor)
    }
}