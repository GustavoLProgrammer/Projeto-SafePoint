package com.example.safepoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safepoint.ui.screens.EditarPerfilScreen

// Modelo de Dados dos Alertas
data class AlertaRota(
    val id: String,
    val titulo: String,
    val dataHora: String,
    val localizacao: String,
    val gravidade: String
)

// Rotas do Aplicativo
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Cadastro : Screen("cadastro")
    object Home : Screen("home")
    object Historico : Screen("historico")
    object EditarPerfil : Screen("editar_perfil")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.Login.route) {
                    composable(Screen.Login.route) { TelaLogin(navController) }
                    composable(Screen.Cadastro.route) { TelaCadastro(navController) }
                    composable(Screen.Home.route) { TelaHome(navController) }
                    composable(Screen.Historico.route) { TelaHistoricoSafepoint(navController) }
                    composable(Screen.EditarPerfil.route) {
                        EditarPerfilScreen(
                            onVoltar = { navController.popBackStack() },
                            onSalvar = { _, _, _, _ ->
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ================= TELA DE LOGIN =================
@Composable
fun TelaLogin(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Logo",
            tint = Color(0xFF1E88E5),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Safepoint", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B3B6F))
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
        ) {
            Text("Entrar", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate(Screen.Cadastro.route) }) {
            Text("Não tem uma conta? Cadastre-se", color = Color(0xFF1976D2))
        }
    }
}

// ================= TELA DE CADASTRO =================
@Composable
fun TelaCadastro(navController: NavController) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Criar Conta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B3B6F))
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome Completo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text("Cadastrar", fontSize = 16.sp, color = Color.White)
        }

        TextButton(onClick = { navController.popBackStack() }) {
            Text("Já tem uma conta? Faça login", color = Color(0xFF1976D2))
        }
    }
}

// ================= TELA HOME =================
@Composable
fun TelaHome(navController: NavController) {
    Scaffold(
        bottomBar = { BarraNavegacaoInferior(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Bem-vindo ao Safepoint!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B3B6F))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Sua plataforma de monitoramento de rotas e alertas.", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { navController.navigate(Screen.EditarPerfil.route) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3E8A))
            ) {
                Text("Editar Perfil", color = Color.White)
            }
        }
    }
}

// ================= TELA HISTÓRICO =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoSafepoint(navController: NavController) {
    var textoPesquisa by remember { mutableStateOf("") }
    var filtroSelecionado by remember { mutableStateOf("Todas") }

    val listaAlertas = remember {
        listOf(
            AlertaRota("1", "Enchente Severa - Rio Doce", "15/08/2024, 14:30", "Rua Frei Caneca, 340 - Centro, São Paulo - SP", "Crítica"),
            AlertaRota("2", "Deslizamento de Terra - Morro da Cruz", "10/08/2024, 09:15", "Rua Pedro Álvares Cabral, 12 - Morro da Cruz, Porto Alegre - RS", "Alta"),
            AlertaRota("3", "Tempestade Tropical - Litoral Norte", "05/08/2024, 18:00", "Praia Grande - Ubatuba - SP", "Média"),
            AlertaRota("4", "Alerta de Inundação - Rio Tietê", "01/08/2024, 11:00", "Marginal Tietê - Ponte das Bandeiras, São Paulo - SP", "Alta")
        )
    }

    val alertasFiltrados = listaAlertas.filter {
        it.titulo.contains(textoPesquisa, ignoreCase = true) ||
                it.localizacao.contains(textoPesquisa, ignoreCase = true)
    }

    val opcoesFiltro = listOf("Todas", "Hoje", "Esta semana", "Este mês", "Período")

    Scaffold(
        bottomBar = { BarraNavegacaoInferior(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = "Logo", tint = Color(0xFF1E88E5), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Safepoint", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B3B6F))
                }
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notificações", tint = Color(0xFF1B3B6F))
                }
            }

            Text(
                text = "Histórico de Rotas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B3B6F),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = textoPesquisa,
                onValueChange = { textoPesquisa = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Pesquisar rotas...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFDCDCDC)
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(text = "Filtrar por:", fontWeight = FontWeight.Bold, color = Color(0xFF1B3B6F), fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(opcoesFiltro) { opcao ->
                        val selecionado = (opcao == filtroSelecionado)
                        FilterChip(
                            selected = selecionado,
                            onClick = { filtroSelecionado = opcao },
                            label = { Text(opcao) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E7D32),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color.DarkGray
                            )
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)
            ) {
                items(alertasFiltrados) { alerta ->
                    CardAlertaItem(alerta = alerta)
                }
            }
        }
    }
}

@Composable
fun CardAlertaItem(alerta: AlertaRota) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFE1F5FE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = alerta.titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1B3B6F))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = alerta.dataHora, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = alerta.localizacao, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(6.dp))
                val corGravidade = when (alerta.gravidade) {
                    "Crítica" -> Color.Red
                    "Alta" -> Color(0xFFFFA000)
                    else -> Color(0xFFFBC02D)
                }
                Text(text = alerta.gravidade, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = corGravidade)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Text(text = "visualizar", color = Color(0xFF1976D2), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Visualizar", tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ================= BARRA DE NAVEGAÇÃO INFERIOR =================
@Composable
fun BarraNavegacaoInferior(navController: NavController) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(Screen.Home.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Mapa */ },
            icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
            label = { Text("Mapa") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(Screen.Historico.route) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Histórico") },
            label = { Text("Histórico") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(Screen.EditarPerfil.route) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") }
        )
    }
}
