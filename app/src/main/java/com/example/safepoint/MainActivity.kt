
package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ================= MODELOS =================
data class CidadeDemo(val nome: String, val uf: String, val lat: Double, val lng: Double)
data class AlertaDemo(val id: String, val cidade: String, val tipo: String, val gravidade: String, val lat: Double, val lng: Double, val descricao: String, val dataHora: String)
data class AbrigoDemo(val id: String, val cidade: String, val nome: String, val lat: Double, val lng: Double, val categoria: String="", val endereco: String="", val descricao: String="")
data class RotaInfo(val km: Double, val min: Int, val chegada: String)
enum class ModoRota(val titulo: String, val vel: Double){ CARRO("Carro",40.0), A_PE("A pé",5.0), BICICLETA("Bike",15.0), ONIBUS("Ônibus",30.0) }

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    try{
        val r=6371.0; val dLat=Math.toRadians(lat2-lat1); val dLon=Math.toRadians(lon2-lon1)
        val a=sin(dLat/2)*sin(dLat/2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2)*sin(dLon/2)
        return r*2*atan2(sqrt(a), sqrt(1-a))
    }catch(_:Exception){ return 0.0 }
}

fun obterLocalizacaoSegura(context: Context, cb:(Double,Double)->Unit){
    try{
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            try{
                val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                if(loc!=null){ cb(loc.latitude, loc.longitude); return }
            }catch(_:Exception){}
        }
    }catch(_:Exception){}
    cb(-23.5505, -46.6333)
}

// ================= DADOS INVENTADOS - NUNCA VAZIO =================
fun gerarAbrigor(): List<AbrigoDemo> {
    try{
        val cidades = listOf(
            Triple("São Paulo",-23.5505,-46.6333), Triple("Rio",-22.9068,-43.1729),
            Triple("Brasília",-15.7975,-47.8919), Triple("Salvador",-12.9777,-38.5016),
            Triple("Fortaleza",-3.7319,-38.5267), Triple("Belo Horizonte",-19.9167,-43.9345),
            Triple("Curitiba",-25.4284,-49.2733), Triple("Porto Alegre",-30.0346,-51.2177),
            Triple("Buenos Aires",-34.6037,-58.3816), Triple("Lima",-12.0464,-77.0428),
            Triple("Tóquio",35.6762,139.6503), Triple("Londres",51.5074,-0.1278), Triple("Paris",48.8566,2.3522)
        )
        val lista = mutableListOf<AbrigoDemo>()
        var c=0
        cidades.forEach{ (nome, latB, lngB) ->
            repeat(50){ i ->
                c++; val la=latB + (Random().nextDouble()-0.5)*0.2; val lo=lngB + (Random().nextDouble()-0.5)*0.2
                lista.add(AbrigoDemo("abr-$c", nome, "Abrigo Seguro $nome ${i+1}", la, lo, categoria=listOf("Hospital","Escola","Bombeiros","Abrigo","Polícia").random(), endereco="$nome - verde", descricao="Abrigo verde"))
            }
        }
        repeat(400){ i -> val la=-23.55 + (Random().nextDouble()-0.5)*2.0; val lo=-46.63 + (Random().nextDouble()-0.5)*2.0; c++; lista.add(AbrigoDemo("abr-sp-$i", "São Paulo", "Abrigo SP $i", la, lo, categoria="Abrigo")) }
        return lista
    }catch(_:Exception){ return listOf(AbrigoDemo("abr-1","São Paulo","Abrigo Seguro Central",-23.5505,-46.6333,categoria="Abrigo")) }
}

fun gerarAlertas(): List<AlertaDemo> {
    try{
        val tipos=listOf("Terremoto","Enchente","Incêndio","Ciclone","Deslizamento","Tempestade","Seca","Erupção vulcânica")
        val gravs=listOf("Crítica","Alta","Média","Baixa","Evento")
        val cidades=listOf(Triple("São Paulo",-23.55,-46.63), Triple("Rio",-22.90,-43.17), Triple("Brasília",-15.79,-47.89), Triple("Buenos Aires",-34.60,-58.38), Triple("Tóquio",35.67,139.65), Triple("Londres",51.50,-0.12))
        val lista=mutableListOf<AlertaDemo>(); var id=0
        cidades.forEach{ (cid, latB, lngB) ->
            tipos.forEach{ tipo ->
                gravs.forEach{ grav ->
                    if(lista.size<200){ id++; val la=latB + (Random().nextDouble()-0.5)*0.5; val lo=lngB + (Random().nextDouble()-0.5)*0.5; lista.add(AlertaDemo("al-$id","$tipo em $cid - $grav",tipo,grav,la,lo,"$tipo em $cid - gravidade $grav - inventado para teste", "2024-01-15 12:00")) }
                }
            }
        }
        return lista
    }catch(_:Exception){ return listOf(AlertaDemo("al-1","Terremoto em São Paulo - Crítica","Terremoto","Crítica",-23.55,-46.63,"Demo","2024-01-15")) }
}

fun corTipo(tipo: String): Int {
    return try{
        when{
            tipo.contains("Terremoto")-> android.graphics.Color.rgb(211,47,47)
            tipo.contains("Ciclone")-> android.graphics.Color.rgb(123,31,162)
            tipo.contains("Incêndio")-> android.graphics.Color.rgb(230,74,25)
            tipo.contains("Enchente")-> android.graphics.Color.rgb(21,101,192)
            tipo.contains("Tempestade")-> android.graphics.Color.rgb(0,131,143)
            tipo.contains("Desliz")-> android.graphics.Color.rgb(78,52,46)
            tipo.contains("Seca")-> android.graphics.Color.rgb(249,168,37)
            tipo.contains("vulc")-> android.graphics.Color.rgb(191,54,12)
            else-> android.graphics.Color.rgb(46,125,50)
        }
    }catch(_:Exception){ android.graphics.Color.RED }
}

fun pinoAlertaSafe(context: Context, tipo: String, tamanho: Int): Drawable? {
    return try{
        val w=tamanho; val h=(w*1.4).toInt()
        val bmp=Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
        val paint=Paint().apply{ color=corTipo(tipo); isAntiAlias=true }
        val borda=Paint().apply{ color=Color.White.toArgb(); style=Paint.Style.STROKE; strokeWidth=3f; isAntiAlias=true }
        val path=Path().apply{ moveTo(w/2f,h.toFloat()); lineTo(0f,h*0.35f); arcTo(RectF(0f,0f,w.toFloat(),w.toFloat()),180f,180f); close() }
        c.drawPath(path,paint); c.drawPath(path,borda)
        BitmapDrawable(context.resources,bmp)
    }catch(_:Exception){ null }
}
fun pinoVerdeSafe(context: Context, tamanho: Int): Drawable? {
    return try{
        val w=tamanho; val h=(w*1.4).toInt()
        val bmp=Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
        val paint=Paint().apply{ color=android.graphics.Color.rgb(46,125,50); isAntiAlias=true }
        val borda=Paint().apply{ color=Color.White.toArgb(); style=Paint.Style.STROKE; strokeWidth=3f; isAntiAlias=true }
        val path=Path().apply{ moveTo(w/2f,h.toFloat()); lineTo(0f,h*0.35f); arcTo(RectF(0f,0f,w.toFloat(),w.toFloat()),180f,180f); close() }
        c.drawPath(path,paint); c.drawPath(path,borda)
        BitmapDrawable(context.resources,bmp)
    }catch(_:Exception){ null }
}
fun setaAzulSafe(context: Context, tamanho: Int): Drawable? {
    return try{
        val w=tamanho; val h=tamanho
        val bmp=Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
        val paint=Paint().apply{ color=android.graphics.Color.rgb(25,118,210); isAntiAlias=true }
        val borda=Paint().apply{ color=Color.White.toArgb(); style=Paint.Style.STROKE; strokeWidth=4f; isAntiAlias=true }
        val path=Path().apply{ moveTo(w/2f,0f); lineTo(w.toFloat(),h*0.75f); lineTo(w*0.5f,h*0.6f); lineTo(0f,h*0.75f); close() }
        c.drawPath(path,paint); c.drawPath(path,borda)
        BitmapDrawable(context.resources,bmp)
    }catch(_:Exception){ null }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid",0))
            Configuration.getInstance().userAgentValue = "SafePoint/1.0"
        }catch(_:Exception){}
        setContent {
            MaterialTheme {
                Surface(modifier=Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
                    AppNavegacao()
                }
            }
        }
    }
}

@Composable
fun AppNavegacao(){
    val nav = androidx.navigation.compose.rememberNavController()
    NavHost(navController=nav, startDestination="mapa"){
        composable("mapa"){ TelaMapaBlindada(nav) }
        composable("historico"){ TelaHistoricoBlindada(nav) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMapaBlindada(nav: androidx.navigation.NavController){
    val context=LocalContext.current
    var busca by remember{ mutableStateOf("") }
    var origem by remember{ mutableStateOf<GeoPoint?>(GeoPoint(-23.5505,-46.6333)) }
    var todosAlertas by remember{ mutableStateOf(gerarAlertas()) }
    var todosAbrigor by remember{ mutableStateOf(gerarAbrigor()) }
    var alertasLocal by remember{ mutableStateOf(todosAlertas.take(100)) }
    var abrigorLocal by remember{ mutableStateOf(todosAbrigor.take(80)) }
    var sel by remember{ mutableStateOf<AlertaDemo?>(null) }
    var abrigoSel by remember{ mutableStateOf<AbrigoDemo?>(null) }
    var rotaPts by remember{ mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember{ mutableStateOf<RotaInfo?>(null) }
    var modo by remember{ mutableStateOf(ModoRota.CARRO) }
    var aviso by remember{ mutableStateOf("Mapa blindado - ${todosAlertas.size} alertas - ${todosAbrigor.size} abrigos verdes - seta azul ativa") }
    var filtroGrav by remember{ mutableStateOf(setOf<String>()) }
    var filtroTipo by remember{ mutableStateOf(setOf<String>()) }

    val mapView = remember{
        try{
            MapView(context).apply{
                setTileSource(XYTileSource("Mapnik",0,19,256,".png", arrayOf("https://a.tile.openstreetmap.org/")))
                controller.setZoom(11.0); controller.setCenter(GeoPoint(-23.5505,-46.6333)); setMultiTouchControls(true)
            }
        }catch(e:Exception){
            Toast.makeText(context, "Erro mapa: ${e.message}", Toast.LENGTH_SHORT).show()
            MapView(context)
        }
    }

    val permLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ perms->
        try{
            if(perms[Manifest.permission.ACCESS_FINE_LOCATION]==true){
                obterLocalizacaoSegura(context){ la,lo ->
                    origem=GeoPoint(la,lo)
                    try{
                        abrigorLocal=todosAbrigor.filter{ calcularDistancia(la,lo,it.lat,it.lng)<=80.0 }.take(80)
                        alertasLocal=todosAlertas.filter{ calcularDistancia(la,lo,it.lat,it.lng)<=500.0 }.take(80)
                        mapView.controller.animateTo(GeoPoint(la,lo)); mapView.controller.setZoom(13.0)
                    }catch(_:Exception){}
                    aviso="GPS ativo • você aqui • ${abrigorLocal.size} abrigos verdes"
                }
            }
        }catch(_:Exception){}
    }

    LaunchedEffect(Unit){
        try{ permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }catch(_:Exception){}
    }

    fun aplicarCidade(nome: String, lat: Double, lng: Double, mundo: Boolean){
        try{
            if(mundo){
                alertasLocal=todosAlertas; abrigorLocal=todosAbrigor.take(200)
                mapView.controller.setZoom(2.5); mapView.controller.setCenter(GeoPoint(0.0,0.0))
            }else{
                val existentes=todosAlertas.filter{ it.cidade.lowercase().contains(nome.lowercase()) }
                alertasLocal=if(existentes.isNotEmpty()) existentes else todosAlertas.filter{ calcularDistancia(lat,lng,it.lat,it.lng)<=300.0 }.take(80)
                abrigorLocal=todosAbrigor.filter{ calcularDistancia(lat,lng,it.lat,it.lng)<=60.0 }.take(80)
                mapView.controller.animateTo(GeoPoint(lat,lng)); mapView.controller.setZoom(11.0)
            }
            mapView.invalidate()
            aviso="${alertasLocal.size} alertas em $nome • ${abrigorLocal.size} abrigos verdes"
        }catch(e:Exception){ aviso="Erro ao aplicar: ${e.message}" }
    }

    fun calcularRota(destLat: Double, destLng: Double){
        try{
            val ori=origem?:GeoPoint(-23.5505,-46.6333)
            val dist=calcularDistancia(ori.latitude, ori.longitude, destLat, destLng)
            val tempo=(dist/modo.vel*60).toInt().coerceAtLeast(1)
            val chegada=SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis()+tempo*60*1000L))
            rotaPts=listOf(ori, GeoPoint(destLat,destLng))
            rotaInfo=RotaInfo(dist,tempo,chegada)
            aviso="Rota ${modo.titulo}: ${String.format("%.1f km",dist)} • $tempo min • chegada $chegada • linha azul"
            mapView.invalidate()
        }catch(e:Exception){ aviso="Erro rota: ${e.message}" }
    }

    val alertasVis=try{ alertasLocal.filter{ (filtroGrav.isEmpty()||filtroGrav.contains(it.gravidade)) && (filtroTipo.isEmpty()||filtroTipo.contains(it.tipo)) } }catch(_:Exception){ alertasLocal }

    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment=Alignment.CenterVertically){
            OutlinedTextField(value=busca, onValueChange={busca=it}, placeholder={Text("Buscar - vazio = mundo", fontSize=11.sp)}, modifier=Modifier.weight(1f), singleLine=true)
            Spacer(Modifier.width(6.dp))
            Button(onClick={
                try{
                    if(busca.isBlank()) aplicarCidade("Mundo",0.0,0.0,true)
                    else {
                        val match=todosAlertas.firstOrNull{ it.cidade.lowercase().contains(busca.lowercase()) }
                        if(match!=null) aplicarCidade(busca, match.lat, match.lng, false)
                        else aplicarCidade(busca, -23.55, -46.63, false)
                    }
                }catch(e:Exception){ aviso="Erro busca: ${e.message}" }
            }){ Text("Buscar") }
        }
        Text(aviso, Modifier.padding(horizontal=8.dp), fontSize=11.sp, color=Color(0xFF0B2545))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp, vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("Crítica","Alta","Média","Baixa","Evento").forEach{ g -> FilterChip(selected=filtroGrav.contains(g), onClick={ filtroGrav = if(filtroGrav.contains(g)) filtroGrav-g else filtroGrav+g }, label={Text(g,fontSize=9.sp)}, modifier=Modifier.height(28.dp)) }
            listOf("Terremoto","Enchente","Incêndio","Ciclone").forEach{ t -> FilterChip(selected=filtroTipo.contains(t), onClick={ filtroTipo = if(filtroTipo.contains(t)) filtroTipo-t else filtroTipo+t }, label={Text(t,fontSize=9.sp)}, modifier=Modifier.height(28.dp)) }
            AssistChip(onClick={ filtroGrav=setOf(); filtroTipo=setOf() }, label={Text("Limpar",fontSize=9.sp)}, modifier=Modifier.height(28.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()){
            AndroidView(factory={ mapView }, modifier=Modifier.fillMaxSize()){ view ->
                try{
                    view.overlays.removeAll{ it is Marker || it is Polyline }
                    if(rotaPts.isNotEmpty()){
                        val linha=Polyline().apply{ setPoints(rotaPts); outlinePaint.color=android.graphics.Color.BLUE; outlinePaint.strokeWidth=9f }
                        view.overlays.add(linha)
                    }
                    alertasVis.forEach{ a ->
                        try{
                            val mk=Marker(view).apply{
                                position=GeoPoint(a.lat,a.lng); title="${a.tipo} • ${a.gravidade}"
                                icon=pinoAlertaSafe(context,a.tipo,50); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener{ _,_ -> sel=a; abrigoSel=null; true }
                            }; view.overlays.add(mk)
                        }catch(_:Exception){}
                    }
                    abrigorLocal.forEach{ ab ->
                        try{
                            val mk=Marker(view).apply{
                                position=GeoPoint(ab.lat,ab.lng); title=ab.nome; snippet="${ab.categoria} • verde"
                                icon=pinoVerdeSafe(context,50); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener{ _,_ -> abrigoSel=ab; sel=null; true }
                            }; view.overlays.add(mk)
                        }catch(_:Exception){}
                    }
                    origem?.let{ ori ->
                        try{
                            val mk=Marker(view).apply{
                                position=ori; title="Você"; snippet="Seta azul GPS"
                                icon=setaAzulSafe(context,56); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }; view.overlays.add(mk)
                        }catch(_:Exception){}
                    }
                    view.invalidate()
                }catch(e:Exception){}
            }
            Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement=Arrangement.spacedBy(6.dp)){
                SmallFloatingActionButton(onClick={ try{ mapView.controller.setZoom(mapView.zoomLevelDouble+1) }catch(_:Exception){} }, containerColor=Color.White){ Icon(Icons.Default.Add,"+") }
                SmallFloatingActionButton(onClick={ try{ mapView.controller.setZoom(mapView.zoomLevelDouble-1) }catch(_:Exception){} }, containerColor=Color.White){ Icon(Icons.Default.Remove,"-") }
                SmallFloatingActionButton(onClick={ try{ origem?.let{ mapView.controller.animateTo(it); mapView.controller.setZoom(14.0) } }catch(_:Exception){} }, containerColor=Color.White){ Icon(Icons.Default.MyLocation,"GPS", tint=Color(0xFF1976D2)) }
            }
        }
        val s=sel; val ab=abrigoSel
        if(s!=null || ab!=null){
            Card(Modifier.fillMaxWidth().padding(10.dp).heightIn(max=320.dp), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(8.dp)){
                Column(Modifier.padding(10.dp).verticalScroll(rememberScrollState())){
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(if(s!=null) "${s.tipo} • ${s.cidade}" else ab!!.nome, fontWeight=FontWeight.Bold, color=Color(0xFF0B2545), fontSize=14.sp, maxLines=2)
                            rotaInfo?.let{ Text("Dist: ${String.format("%.1f km", it.km)} • ${it.min} min • Chegada: ${it.chegada}", fontSize=10.sp, fontWeight=FontWeight.SemiBold) }
                        }
                        IconButton(onClick={ sel=null; abrigoSel=null; rotaPts=emptyList(); rotaInfo=null }, modifier=Modifier.size(32.dp)){ Icon(Icons.Default.Close,"Fechar") }
                    }
                    Row(Modifier.padding(top=6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        ModoRota.values().forEach{ m -> FilterChip(selected=modo==m, onClick={ modo=m; if(s!=null){ val ap=abrigorLocal.minByOrNull{ calcularDistancia(s.lat,s.lng,it.lat,it.lng) } ?: abrigorLocal.first(); calcularRota(ap.lat, ap.lng) } else if(ab!=null){ calcularRota(ab.lat, ab.lng) } }, label={Text(m.titulo,fontSize=10.sp)}) }
                    }
                    Button(onClick={ try{ if(s!=null){ val ap=abrigorLocal.minByOrNull{ calcularDistancia(s.lat,s.lng,it.lat,it.lng) } ?: abrigorLocal.first(); calcularRota(ap.lat, ap.lng) } else if(ab!=null){ calcularRota(ab.lat, ab.lng) } }catch(_:Exception){} }, modifier=Modifier.fillMaxWidth().padding(top=6.dp)){ Text("Rotear - linha azul") }
                }
            }
        }
    }
}

@Composable
fun TelaHistoricoBlindada(nav: androidx.navigation.NavController){
    var busca by remember{ mutableStateOf("") }
    var todos by remember{ mutableStateOf(gerarAlertas()) }
    var filtroGrav by remember{ mutableStateOf(setOf<String>()) }
    var filtroTipo by remember{ mutableStateOf(setOf<String>()) }

    val vis=try{
        todos.filter{ (filtroGrav.isEmpty()||filtroGrav.contains(it.gravidade)) && (filtroTipo.isEmpty()||filtroTipo.contains(it.tipo)) && (busca.isBlank()||it.cidade.lowercase().contains(busca.lowercase())) }
    }catch(_:Exception){ todos }

    Column(Modifier.fillMaxSize().padding(8.dp)){
        Text("Histórico - ${vis.size} alertas", fontWeight=FontWeight.Bold, fontSize=16.sp)
        OutlinedTextField(value=busca, onValueChange={busca=it}, placeholder={Text("Buscar - vazio = todos", fontSize=11.sp)}, modifier=Modifier.fillMaxWidth(), singleLine=true)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)){
            listOf("Crítica","Alta","Média","Baixa","Evento").forEach{ g -> FilterChip(selected=filtroGrav.contains(g), onClick={ filtroGrav=if(filtroGrav.contains(g)) filtroGrav-g else filtroGrav+g }, label={Text(g,fontSize=8.sp)}, modifier=Modifier.height(26.dp)) }
            AssistChip(onClick={ filtroGrav=setOf(); filtroTipo=setOf(); busca="" }, label={Text("Limpar",fontSize=8.sp)}, modifier=Modifier.height(26.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(6.dp)){
            vis.take(150).forEach{ a ->
                Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(10.dp), colors=CardDefaults.cardColors(containerColor=Color.White)){
                    Column(Modifier.padding(8.dp)){
                        Text(a.tipo, fontWeight=FontWeight.Bold, fontSize=12.sp); Text(a.cidade, fontSize=11.sp); Text(a.descricao.take(100), fontSize=9.sp, color=Color.Gray)
                    }
                }
            }
        }
    }
}
