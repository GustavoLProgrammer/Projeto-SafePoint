package com.example.safepoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modelo de Dados dos Alertas
data class AlertaRota(
    val id: String,
    val titulo: String,
    val dataHora: String,
    val localizacao: String,
    val gravidade: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TelaHistoricoSafepoint()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoSafepoint() {
    var textoPesquisa by remember { mutableStateOf("") }
    var filtroSelecionado by remember { mutableStateOf("Todas") }

    // Dados de exemplo baseados no mockup
    val listaAlertas = remember {
        listOf(
            AlertaRota("1", "Enchente Severa - Rio Doce", "15/08/2024, 14:30", "Rua Frei Caneca, 340 - Centro, São Paulo - SP", "Crítica"),
            AlertaRota("2", "Deslizamento de Terra - Morro da Cruz", "10/08/2024, 09:15", "Rua Pedro Álvares Cabral, 12 - Morro da Cruz, Porto Alegre - RS", "Alta"),
            AlertaRota("3", "Tempestade Tropical - Litoral Norte", "05/08/2024, 18:00", "Praia Grande - Ubatuba - SP", "Média"),
            AlertaRota("4", "Alerta de Inundação - Rio Tietê", "01/08/2024, 11:00", "Marginal Tietê - Ponte das Bandeiras, São Paulo - SP", "Alta")
        )
    }

    // Filtragem em tempo real pelo texto de busca
    val alertasFiltrados = listaAlertas.filter {
        it.titulo.contains(textoPesquisa, ignoreCase = true) ||
                it.localizacao.contains(textoPesquisa, ignoreCase = true)
    }

    val opcoesFiltro = listOf("Todas", "Hoje", "Esta semana", "Este mês", "Período")

    Scaffold(
        bottomBar = { BarraNavegacaoInferior() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
                .padding(horizontal = 16.dp)
        ) {
            // Cabeçalho: Logo + Nome + Notificação
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Logo",
                        tint = Color(0xFF1E88E5),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Safepoint",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B3B6F)
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificações",
                        tint = Color(0xFF1B3B6F)
                    )
                }
            }

            // Título principal da página
            Text(
                text = "Histórico de Rotas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B3B6F),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )

            // Campo de Pesquisa
            OutlinedTextField(
                value = textoPesquisa,
                onValueChange = { textoPesquisa = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

            // Seção de Filtros (Filtro por chips)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Filtrar por:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B3B6F),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

            // Lista de Cards de Ocorrências
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone da categoria em um círculo azul claro
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE1F5FE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Informações do Alerta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alerta.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1B3B6F)
                )

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

                Text(
                    text = alerta.gravidade,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = corGravidade
                )
            }

            // Ação 'visualizar >'
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "visualizar",
                    color = Color(0xFF1976D2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Visualizar",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BarraNavegacaoInferior() {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
            label = { Text("Mapa") }
        )
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Histórico") },
            label = { Text("Histórico") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") }
        )
    }
}