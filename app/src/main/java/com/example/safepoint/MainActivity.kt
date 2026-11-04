
package com.example.safepoint

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.os.Bundle
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
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

// ================= DADOS =================
data class CidadeDemo(val nome: String, val uf: String, val lat: Double, val lng: Double, val aliases: List<String> = emptyList())
data class AlertaDemo(val id: String, val cidade: String, val tipo: String, val gravidade: String, val lat: Double, val lng: Double, val descricao: String, val dataHora: String)
data class AbrigoDemo(val id: String, val cidade: String, val nome: String, val lat: Double, val lng: Double, val categoria: String = cidade, val endereco: String = "", val descricao: String = "")
data class RotaInfo(val km: Double, val minutos: Int, val chegada: String)

enum class ModoRota(val titulo: String, val vel: Double){
    CARRO("Carro",40.0), A_PE("A pé",5.0), BICICLETA("Bike",15.0), ONIBUS("Ônibus",30.0)
}

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r=6371.0; val dLat=Math.toRadians(lat2-lat1); val dLon=Math.toRadians(lon2-lon1)
    val a=sin(dLat/2)*sin(dLat/2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2)*sin(dLon/2)
    return r*2*atan2(sqrt(a), sqrt(1-a))
}

fun obterLocalizacao(context: Context, cb:(Double,Double)->Unit){
    try{
        val fused=LocationServices.getFusedLocationProviderClient(context)
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            fused.lastLocation.addOnSuccessListener{ loc-> if(loc!=null) cb(loc.latitude, loc.longitude) }
        }
    }catch(_:Exception){}
}

// ================= INVENTADO - MILHARES DE ABRIGOS E TODOS TIPOS =================
fun gerarMilharesAbrigos(): List<AbrigoDemo> {
    val cidades = listOf(
        Triple("São Paulo",-23.5505,-46.6333), Triple("Rio de Janeiro",-22.9068,-43.1729),
        Triple("Brasília",-15.7975,-47.8919), Triple("Salvador",-12.9777,-38.5016),
        Triple("Fortaleza",-3.7319,-38.5267), Triple("Belo Horizonte",-19.9167,-43.9345),
        Triple("Curitiba",-25.4284,-49.2733), Triple("Porto Alegre",-30.0346,-51.2177),
        Triple("Recife",-8.0476,-34.8770), Triple("Manaus",-3.1190,-60.0217),
        Triple("Belém",-1.4558,-48.4902), Triple("Goiânia",-16.6869,-49.2648),
        Triple("Buenos Aires",-34.6037,-58.3816), Triple("Lima",-12.0464,-77.0428),
        Triple("Santiago",-33.4489,-70.6693), Triple("Bogotá",4.7110,-74.0721),
        Triple("Cidade do México",19.4326,-99.1332), Triple("Nova York",40.7128,-74.0060),
        Triple("Los Angeles",34.0522,-118.2437), Triple("Tóquio",35.6762,139.6503),
        Triple("Pequim",39.9042,116.4074), Triple("Londres",51.5074,-0.1278),
        Triple("Paris",48.8566,2.3522), Triple("Berlim",52.5200,13.4050),
        Triple("Roma",41.9028,12.4964), Triple("Madri",40.4168,-3.7038),
        Triple("Lisboa",38.7223,-9.1393), Triple("Moscou",55.7558,37.6173),
        Triple("Cairo",30.0444,31.2357), Triple("Sydney",-33.8688,151.2093)
    )
    val lista = mutableListOf<AbrigoDemo>()
    var c=0
    cidades.forEach { (nome, latB, lngB) ->
        val cats = listOf("Hospital" to "Hospital Municipal", "Escola" to "Escola Municipal", "Bombeiros" to "Corpo de Bombeiros", "Polícia" to "Delegacia", "Igreja/templo" to "Igreja Comunitária", "Abrigo" to "Abrigo Seguro", "Clínica" to "Clínica Saúde", "Ginásio" to "Ginásio")
        repeat(70){ i ->
            val (cat, base) = cats[i % cats.size]
            val la = latB + (Random().nextDouble()-0.5)*0.3
            val lo = lngB + (Random().nextDouble()-0.5)*0.3
            c++; lista.add(AbrigoDemo("abr-$c", nome, "$base - $nome ${i+1}", la, lo, categoria=cat, endereco="$nome - ponto verde", descricao="Abrigo verde inventado #2E7D32 - $cat"))
        }
    }
    // +600 extras em SP para garantir milhares no Brasil
    repeat(600){ i ->
        val la = -23.5505 + (Random().nextDouble()-0.5)*3.0
        val lo = -46.6333 + (Random().nextDouble()-0.5)*3.0
        c++; lista.add(AbrigoDemo("abr-sp-$i", "São Paulo", "Abrigo Seguro SP $i", la, lo, categoria=listOf("Hospital","Escola","Bombeiros","Abrigo").random(), endereco="SP - verde", descricao="Abrigo verde"))
    }
    return lista
}

fun gerarTodosAlertas(): List<AlertaDemo> {
    val tipos = listOf("Terremoto","Enchente","Incêndio","Ciclone","Deslizamento","Tempestade","Seca","Erupção vulcânica")
    val gravs = listOf("Crítica","Alta","Média","Baixa","Evento")
    val cidades = listOf(
        Triple("São Paulo",-23.5505,-46.6333), Triple("Rio de Janeiro",-22.9068,-43.1729),
        Triple("Brasília",-15.7975,-47.8919), Triple("Salvador",-12.9777,-38.5016),
        Triple("Fortaleza",-3.7319,-38.5267), Triple("Recife",-8.0476,-34.8770),
        Triple("Manaus",-3.1190,-60.0217), Triple("Curitiba",-25.4284,-49.2733),
        Triple("Porto Alegre",-30.0346,-51.2177), Triple("Buenos Aires",-34.6037,-58.3816),
        Triple("Lima",-12.0464,-77.0428), Triple("Santiago",-33.4489,-70.6693),
        Triple("Bogotá",4.7110,-74.0721), Triple("Cidade do México",19.4326,-99.1332),
        Triple("Nova York",40.7128,-74.0060), Triple("Tóquio",35.6762,139.6503),
        Triple("Londres",51.5074,-0.1278), Triple("Paris",48.8566,2.3522),
        Triple("Lisboa",38.7223,-9.1393), Triple("Berlim",52.5200,13.4050)
    )
    val lista = mutableListOf<AlertaDemo>()
    var id=0
    cidades.forEach { (cid, latB, lngB) ->
        tipos.forEach { tipo ->
            gravs.forEach { grav ->
                if(lista.size<300){
                    id++; val la=latB + (Random().nextDouble()-0.5)*0.6; val lo=lngB + (Random().nextDouble()-0.5)*0.6
                    val desc = when(tipo){
                        "Terremoto" -> "Terremoto magnitude ${(3..7).random()}.${(0..9).random()} a ${(5..80).random()} km de $cid - risco ${grav.lowercase()} - todos níveis aplicados"
                        "Enchente" -> "Enchente em $cid - nível do rio acima da cota - gravidade $grav - inventado"
                        "Incêndio" -> "Incêndio florestal próximo a $cid - ${(10..500).random()} ha - $grav - inventado"
                        "Ciclone" -> "Ciclone tropical a ${(50..300).random()} km de $cid - ventos ${(60..180).random()} km/h - $grav"
                        "Deslizamento" -> "Deslizamento de terra em encosta em $cid - $grav - evacuação preventiva - inventado"
                        "Tempestade" -> "Tempestade severa em $cid - chuva ${(30..150).random()}mm - $grav - inventado"
                        "Seca" -> "Seca severa em $cid - reservatórios ${(5..40).random()}% - $grav - inventado"
                        else -> "Erupção vulcânica a ${(20..100).random()} km de $cid - alerta $grav - inventado"
                    }
                    lista.add(AlertaDemo("alerta-$id", "$tipo em $cid - $grav - inventado", tipo, grav, la, lo, desc, "2024-01-15 ${(0..23).random().toString().padStart(2,'0')}:00"))
                }
            }
        }
    }
    return lista.shuffled()
}

fun gerarAlertasCidade(nome: String, lat: Double, lng: Double): List<AlertaDemo> {
    val tipos = listOf("Terremoto","Enchente","Incêndio","Ciclone","Deslizamento","Tempestade","Seca","Erupção vulcânica")
    val gravs = listOf("Crítica","Alta","Média","Baixa","Evento")
    return tipos.mapIndexed { idx, tipo ->
        val grav = gravs[idx % gravs.size]
        AlertaDemo("busca-${nome}-$idx", "$tipo em $nome - $grav - inventado para busca", tipo, grav, lat+(Math.random()-0.5)*0.08, lng+(Math.random()-0.5)*0.08, "Alerta fictício de $tipo em $nome criado porque você buscou esta cidade - gravidade $grav", "2024-01-15 12:00")
    }
}

// ================= ICONES =================
fun corTipo(tipo: String): Int {
    val t=tipo.lowercase()
    return when{
        t.contains("terremoto")-> android.graphics.Color.rgb(211,47,47)
        t.contains("ciclone")-> android.graphics.Color.rgb(123,31,162)
        t.contains("incêndio")-> android.graphics.Color.rgb(230,74,25)
        t.contains("enchente")-> android.graphics.Color.rgb(21,101,192)
        t.contains("tempestade")-> android.graphics.Color.rgb(0,131,143)
        t.contains("desliz")-> android.graphics.Color.rgb(78,52,46)
        t.contains("seca")-> android.graphics.Color.rgb(249,168,37)
        t.contains("vulc")-> android.graphics.Color.rgb(191,54,12)
        else-> android.graphics.Color.rgb(46,125,50)
    }
}
fun pinoAlerta(context: Context, tipo: String, tamanho: Int): Drawable {
    val w=tamanho; val h=(w*1.4).toInt()
    val bmp=Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
    val paint=Paint().apply{ color=corTipo(tipo); isAntiAlias=true }
    val borda=Paint().apply{ color=Color.White.toArgb(); style=Paint.Style.STROKE; strokeWidth=3f; isAntiAlias=true }
    val path=Path().apply{ moveTo(w/2f,h.toFloat()); lineTo(0f,h*0.35f); arcTo(RectF(0f,0f,w.toFloat(),w.toFloat()),180f,180f); close() }
    c.drawPath(path,paint); c.drawPath(path,borda)
    val tp=Paint().apply{ color=Color.White.toArgb(); textSize=w*0.35f; isFakeBoldText=true; textAlign=Paint.Align.CENTER; isAntiAlias=true }
    val letra=when{ tipo.lowercase().contains("terremoto")->"T"; tipo.lowercase().contains("ciclone")->"C"; tipo.lowercase().contains("incêndio")->"F"; tipo.lowercase().contains("enchente")->"E"; tipo.lowercase().contains("desliz")->"D"; tipo.lowercase().contains("seca")->"S"; tipo.lowercase().contains("vulc")->"V"; else->"!" }
    c.drawText(letra,w/2f,h*0.45f,tp)
    return BitmapDrawable(context.resources,bmp)
}
fun pinoVerde(context: Context, tamanho: Int): Drawable {
    val w=tamanho; val h=(w*1.4).toInt()
    val bmp=Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
    val paint=Paint().apply{ color=android.graphics.Color.rgb(46,125,50); isAntiAlias=true }
    val borda=Paint().apply{ color=Color.White.toArgb(); style=Paint.Style.STROKE; strokeWidth=3f; isAntiAlias=true }
    val path=Path().apply{ moveTo(w/2f,h.toFloat()); lineTo(0f,h*0.35f); arcTo(RectF(0f,0f,w.toFloat(),w.toFloat()),180f,180f); close() }
    c.drawPath(path,paint); c.drawPath(path,borda)
    val tp=Paint().apply{ color=Color.White.toArgb(); textSize=w*0.4f; isFakeBoldText=true; textAlign=Paint.Align.CENTER; isAntiAlias=true }
    c.drawText("✚",w/2f,h*0.48f,tp)
    return BitmapDrawable(context.resources,bmp)
}
fun setaAzul(context: Context, tamanho: Int): Drawable {
    val w=tamanho; val h=tamanho
    val bmp=Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
    val paint=Paint().apply{ color=android.graphics.Color.rgb(25,118,210); isAntiAlias=true }
    val borda=Paint().apply{ color=Color.White.toArgb(); style=Paint.Style.STROKE; strokeWidth=4f; isAntiAlias=true }
    val path=Path().apply{ moveTo(w/2f,0f); lineTo(w.toFloat(),h*0.75f); lineTo(w*0.5f,h*0.6f); lineTo(0f,h*0.75f); close() }
    c.drawPath(path,paint); c.drawPath(path,borda)
    return BitmapDrawable(context.resources,bmp)
}

// ================= TELA MAPA INVENTADA 100% =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMapaReal(navController: NavController) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busca by remember{ mutableStateOf("") }
    var cidadeSel by remember{ mutableStateOf(CidadeDemo("Mundo","",0.0,0.0)) }
    var origem by remember{ mutableStateOf<GeoPoint?>(GeoPoint(-23.5505,-46.6333)) }
    var gpsAtivo by remember{ mutableStateOf(true) }
    var todosAlertas by remember{ mutableStateOf(gerarTodosAlertas()) }
    var todosAbrigos by remember{ mutableStateOf(gerarMilharesAbrigos()) }
    var alertasLocal by remember{ mutableStateOf(todosAlertas.take(100)) }
    var abrigosLocal by remember{ mutableStateOf(todosAbrigos.filter{ calcularDistancia(-23.5505,-46.6333,it.lat,it.lng)<=80.0 }.take(80)) }
    var sel by remember{ mutableStateOf<AlertaDemo?>(null) }
    var abrigoSel by remember{ mutableStateOf<AbrigoDemo?>(null) }
    var rotaPts by remember{ mutableStateOf<List<GeoPoint>>(emptyList()) }
    var rotaInfo by remember{ mutableStateOf<RotaInfo?>(null) }
    var modo by remember{ mutableStateOf(ModoRota.CARRO) }
    var aviso by remember{ mutableStateOf("${todosAlertas.size} alertas inventados (todos tipos) • ${todosAbrigos.size} abrigos verdes • seta azul ativa") }
    var filtroGrav by remember{ mutableStateOf(setOf<String>()) }
    var filtroTipo by remember{ mutableStateOf(setOf<String>()) }

    val mapView=remember{
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid",0))
        MapView(context).apply{
            setTileSource(object: XYTileSource("Mapnik",0,19,256,".png", arrayOf("https://a.tile.openstreetmap.org/")){ override fun getTileURLString(p:Long)=baseUrl+MapTileIndex.getZoom(p)+"/"+MapTileIndex.getX(p)+"/"+MapTileIndex.getY(p)+mImageFilenameEnding })
            controller.setZoom(11.0); controller.setCenter(GeoPoint(-23.5505,-46.6333)); setMultiTouchControls(true)
        }
    }
    val permLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ perms->
        if(perms[Manifest.permission.ACCESS_FINE_LOCATION]==true){
            obterLocalizacao(context){ la,lo ->
                origem=GeoPoint(la,lo); gpsAtivo=true
                abrigosLocal=todosAbrigos.filter{ calcularDistancia(la,lo,it.lat,it.lng)<=50.0 }.take(80)
                alertasLocal=todosAlertas.filter{ calcularDistancia(la,lo,it.lat,it.lng)<=500.0 }.take(80)
                mapView.controller.animateTo(GeoPoint(la,lo)); mapView.controller.setZoom(13.0); mapView.invalidate()
                aviso="GPS ativo • você está aqui • ${abrigosLocal.size} abrigos verdes"
            }
        }
    }
    LaunchedEffect(Unit){
        permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        // seta azul já ativa com SP
        obterLocalizacao(context){ la,lo -> origem=GeoPoint(la,lo); gpsAtivo=true; mapView.controller.animateTo(GeoPoint(la,lo)); mapView.controller.setZoom(13.0); mapView.invalidate() }
    }

    fun aplicarCidade(nome: String, lat: Double, lng: Double, mundo: Boolean){
        cidadeSel=CidadeDemo(nome,"",lat,lng)
        if(mundo){
            alertasLocal=todosAlertas
            abrigosLocal=todosAbrigos.take(200)
            mapView.controller.setZoom(2.5); mapView.controller.setCenter(GeoPoint(0.0,0.0))
        } else {
            // Gera alertas fictícios da cidade buscada se não existir
            val existentes=todosAlertas.filter{ it.cidade.lowercase().contains(nome.lowercase()) }
            val novos=if(existentes.isEmpty()) gerarAlertasCidade(nome,lat,lng) else emptyList()
            if(novos.isNotEmpty()){ todosAlertas=(novos+todosAlertas).distinctBy{it.id}; alertasLocal=novos+existentes }
            else alertasLocal=todosAlertas.filter{ calcularDistancia(lat,lng,it.lat,it.lng)<=300.0 }.take(80)
            abrigosLocal=todosAbrigos.filter{ calcularDistancia(lat,lng,it.lat,it.lng)<=50.0 }.take(80).ifEmpty { gerarMilharesAbrigos().filter{ calcularDistancia(lat,lng,it.lat,it.lng)<=50.0 }.take(60) }
            mapView.controller.animateTo(GeoPoint(lat,lng)); mapView.controller.setZoom(11.0)
        }
        mapView.invalidate()
        aviso="${alertasLocal.size} alertas em $nome (todos tipos e gravidades) • ${abrigosLocal.size} abrigos verdes"
    }

    fun calcularRota(destLat: Double, destLng: Double){
        val ori=origem?:GeoPoint(-23.5505,-46.6333)
        val dist=calcularDistancia(ori.latitude, ori.longitude, destLat, destLng)
        val tempo=(dist/modo.vel*60).toInt().coerceAtLeast(1)
        val chegada=SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis()+tempo*60*1000L))
        rotaPts=listOf(ori, GeoPoint(destLat,destLng))
        rotaInfo=RotaInfo(dist,tempo,chegada)
        aviso="Rota ${modo.titulo}: ${String.format("%.1f km",dist)} • $tempo min • chegada $chegada • linha azul"
        mapView.invalidate()
    }

    val alertasVis=alertasLocal.filter{ (filtroGrav.isEmpty()||filtroGrav.contains(it.gravidade)) && (filtroTipo.isEmpty()||filtroTipo.contains(it.tipo)) }

    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment=Alignment.CenterVertically){
            OutlinedTextField(value=busca, onValueChange={busca=it}, placeholder={Text("Buscar cidade - vazio = mundo - inventado", fontSize=11.sp)}, modifier=Modifier.weight(1f), singleLine=true)
            Spacer(Modifier.width(6.dp))
            Button(onClick={
                if(busca.isBlank()) aplicarCidade("Mundo todo",0.0,0.0,true)
                else {
                    // busca fictícia: se contém nome, usa coordenadas aproximadas
                    val lower=busca.lowercase()
                    val match=todosAlertas.firstOrNull{ it.cidade.lowercase().contains(lower) }
                    if(match!=null) aplicarCidade(busca, match.lat, match.lng, false)
                    else {
                        // Cidade não existe ainda, cria fictícia perto de SP
                        val la=-23.5505 + (Math.random()-0.5)*2.0; val lo=-46.6333 + (Math.random()-0.5)*2.0
                        val novos=gerarAlertasCidade(busca, la, lo)
                        todosAlertas=(novos+todosAlertas).distinctBy{it.id}
                        aplicarCidade(busca, la, lo, false)
                    }
                }
            }){ Text("Buscar") }
        }
        Text(aviso, Modifier.padding(horizontal=8.dp), fontSize=11.sp, color=Color(0xFF0B2545))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp, vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("Crítica","Alta","Média","Baixa","Evento").forEach{ g -> FilterChip(selected=filtroGrav.contains(g), onClick={ filtroGrav = if(filtroGrav.contains(g)) filtroGrav-g else filtroGrav+g }, label={Text(g,fontSize=9.sp)}, modifier=Modifier.height(28.dp)) }
            listOf("Terremoto","Enchente","Incêndio","Ciclone","Deslizamento","Tempestade","Seca","Erupção vulcânica").forEach{ t -> FilterChip(selected=filtroTipo.contains(t), onClick={ filtroTipo = if(filtroTipo.contains(t)) filtroTipo-t else filtroTipo+t }, label={Text(t,fontSize=9.sp)}, modifier=Modifier.height(28.dp)) }
            AssistChip(onClick={ filtroGrav=setOf(); filtroTipo=setOf() }, label={Text("Limpar",fontSize=9.sp)}, modifier=Modifier.height(28.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()){
            AndroidView(factory={mapView}, modifier=Modifier.fillMaxSize().clipToBounds()){ view ->
                view.overlays.removeAll{ it is Marker || it is Polyline }
                if(rotaPts.isNotEmpty()){
                    val linha=Polyline().apply{ setPoints(rotaPts); outlinePaint.color=android.graphics.Color.rgb(65,160,255); outlinePaint.strokeWidth=9f }
                    view.overlays.add(linha)
                }
                alertasVis.forEach{ a ->
                    val mk=Marker(view).apply{
                        position=GeoPoint(a.lat,a.lng); title="${a.tipo} • ${a.gravidade}"; snippet="${a.cidade} • ${a.descricao.take(80)}"
                        icon=pinoAlerta(context,a.tipo,54); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener{ _,_ -> sel=a; abrigoSel=null; true }
                    }; view.overlays.add(mk)
                }
                abrigosLocal.forEach{ ab ->
                    val mk=Marker(view).apply{
                        position=GeoPoint(ab.lat,ab.lng); title=ab.nome; snippet="${ab.categoria} • ponto verde seguro"
                        icon=pinoVerde(context,56); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener{ _,_ -> abrigoSel=ab; sel=null; true }
                    }; view.overlays.add(mk)
                }
                origem?.let{ ori ->
                    val mk=Marker(view).apply{
                        position=ori; title="Você está aqui"; snippet="Seta azul - sua localização exata - GPS ativo"
                        icon=setaAzul(context,58); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }; view.overlays.add(mk)
                }
                view.invalidate()
            }
            Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement=Arrangement.spacedBy(6.dp)){
                SmallFloatingActionButton(onClick={ mapView.controller.setZoom(mapView.zoomLevelDouble+1) }, containerColor=Color.White){ Icon(Icons.Default.Add,"+") }
                SmallFloatingActionButton(onClick={ mapView.controller.setZoom(mapView.zoomLevelDouble-1) }, containerColor=Color.White){ Icon(Icons.Default.Remove,"-") }
                SmallFloatingActionButton(onClick={ origem?.let{ mapView.controller.animateTo(it); mapView.controller.setZoom(14.0) } }, containerColor=Color.White){ Icon(Icons.Default.MyLocation,"GPS", tint=Color(0xFF1976D2)) }
            }
        }
        val s=sel; val ab=abrigoSel
        if(s!=null || ab!=null){
            Card(Modifier.fillMaxWidth().padding(10.dp).heightIn(max=320.dp), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(8.dp)){
                Column(Modifier.padding(10.dp).verticalScroll(rememberScrollState())){
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(if(s!=null) "${s.tipo} • ${s.cidade}" else ab!!.nome, fontWeight=FontWeight.Bold, color=Color(0xFF0B2545), fontSize=14.sp, maxLines=2)
                            Text(if(s!=null) "${s.gravidade} • ${s.descricao.take(120)}" else "${ab!!.categoria} • ponto verde seguro inventado", fontSize=10.sp, color=Color.Gray, maxLines=3)
                            rotaInfo?.let{ Text("Dist: ${String.format("%.1f km", it.km)} • Tempo: ${it.minutos} min • Chegada: ${it.chegada}", fontSize=10.sp, fontWeight=FontWeight.SemiBold, color=Color(0xFF0B2545)) }
                        }
                        IconButton(onClick={ sel=null; abrigoSel=null; rotaPts=emptyList(); rotaInfo=null }, modifier=Modifier.size(32.dp)){ Icon(Icons.Default.Close,"Fechar") }
                    }
                    Row(Modifier.padding(top=6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        ModoRota.values().forEach{ m -> FilterChip(selected=modo==m, onClick={ modo=m; if(s!=null){ val abProx=abrigosLocal.minByOrNull{ calcularDistancia(s.lat,s.lng,it.lat,it.lng) } ?: gerarMilharesAbrigos().first(); calcularRota(abProx.lat, abProx.lng) } else if(ab!=null){ calcularRota(ab.lat, ab.lng) } }, label={Text(m.titulo,fontSize=10.sp)}) }
                    }
                    Button(onClick={ if(s!=null){ val abProx=abrigosLocal.minByOrNull{ calcularDistancia(s.lat,s.lng,it.lat,it.lng) } ?: gerarMilharesAbrigos().first(); calcularRota(abProx.lat,abProx.lng) } else if(ab!=null){ calcularRota(ab.lat,ab.lng) } }, modifier=Modifier.fillMaxWidth().padding(top=6.dp)){ Text(if(s!=null) "Rotear para abrigo seguro mais próximo - linha azul" else "Rotear para este abrigo verde - linha azul", fontSize=12.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoReal(navController: NavController) {
    val context=LocalContext.current
    var busca by remember{ mutableStateOf("") }
    var todosAlertas by remember{ mutableStateOf(gerarTodosAlertas()) }
    var filtroGrav by remember{ mutableStateOf(setOf<String>()) }
    var filtroTipo by remember{ mutableStateOf(setOf<String>()) }
    var localUsuario by remember{ mutableStateOf("GPS ativo • localização exata") }
    var latUser by remember{ mutableStateOf<Double?>(null) }
    var lngUser by remember{ mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit){
        obterLocalizacao(context){ la, lo ->
            latUser=la; lngUser=lo
            try{
                val geo=Geocoder(context, Locale("pt","BR"))
                val addr=geo.getFromLocation(la,lo,1)?.firstOrNull()
                val cid=addr?.locality ?: "Você"
                localUsuario="$cid • ${String.format("%.5f, %.5f", la, lo)} • GPS ativo • sem botão Usar GPS - área exata"
            }catch(_:Exception){ localUsuario="${String.format("%.5f, %.5f", la, lo)} • GPS ativo • área exata" }
        }
    }

    val visiveis = todosAlertas.filter{ a ->
        val g=filtroGrav.isEmpty()||filtroGrav.contains(a.gravidade)
        val t=filtroTipo.isEmpty()||filtroTipo.contains(a.tipo)
        val b=busca.isBlank()||a.cidade.lowercase().contains(busca.lowercase())||a.tipo.lowercase().contains(busca.lowercase())
        g && t && b
    }.let{ lista ->
        if(busca.isBlank()) lista // vazio = todos do mapa sem exceção
        else {
            val porCidade=lista.filter{ it.cidade.lowercase().contains(busca.lowercase()) }
            if(porCidade.isNotEmpty()) porCidade
            else {
                // busca cidade inexistente -> gera alertas fictícios daquela cidade
                gerarAlertasCidade(busca, latUser ?: -23.5505, lngUser ?: -46.6333)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(8.dp)){
        Text("Histórico - ${visiveis.size} alertas - todos do mapa", fontWeight=FontWeight.Bold, fontSize=16.sp, color=Color(0xFF0B2545))
        Text(localUsuario, fontSize=11.sp, color=Color(0xFF1976D2), modifier=Modifier.padding(vertical=4.dp))
        OutlinedTextField(value=busca, onValueChange={
            busca=it
            if(it.isBlank()){ /* mostra todos */ }
        }, placeholder={Text("Buscar cidade - vazio = todos alertas do mapa - inventado", fontSize=11.sp)}, modifier=Modifier.fillMaxWidth(), singleLine=true)
        Column(Modifier.fillMaxWidth().padding(vertical=4.dp)){
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(4.dp)){
                Text("Grav:", fontSize=9.sp, fontWeight=FontWeight.Bold, modifier=Modifier.align(Alignment.CenterVertically))
                listOf("Crítica","Alta","Média","Baixa","Evento").forEach{ g -> FilterChip(selected=filtroGrav.contains(g), onClick={ filtroGrav=if(filtroGrav.contains(g)) filtroGrav-g else filtroGrav+g }, label={Text(g,fontSize=8.sp)}, modifier=Modifier.height(26.dp)) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=2.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)){
                Text("Tipo:", fontSize=9.sp, fontWeight=FontWeight.Bold, modifier=Modifier.align(Alignment.CenterVertically))
                listOf("Terremoto","Enchente","Incêndio","Ciclone","Deslizamento","Tempestade","Seca","Erupção vulcânica").forEach{ t -> FilterChip(selected=filtroTipo.contains(t), onClick={ filtroTipo=if(filtroTipo.contains(t)) filtroTipo-t else filtroTipo+t }, label={Text(t,fontSize=8.sp)}, modifier=Modifier.height(26.dp)) }
                AssistChip(onClick={ filtroGrav=setOf(); filtroTipo=setOf(); busca="" }, label={Text("Limpar - mostra todos",fontSize=8.sp)}, modifier=Modifier.height(26.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(6.dp)){
            visiveis.take(150).forEach{ a ->
                Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(10.dp), colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(2.dp)){
                    Column(Modifier.padding(8.dp)){
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){
                            Text(a.tipo, fontWeight=FontWeight.Bold, fontSize=12.sp, color=Color(corTipo(a.tipo)))
                            Text(a.gravidade, fontSize=10.sp, color=Color.Gray)
                        }
                        Text(a.cidade, fontSize=11.sp, color=Color(0xFF0B2545), maxLines=2)
                        Text(a.descricao.take(120), fontSize=9.sp, color=Color.Gray, maxLines=2)
                    }
                }
            }
        }
    }
}
