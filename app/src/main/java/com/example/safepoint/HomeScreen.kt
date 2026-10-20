package com.example.safepoint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.filled.Person

@Composable
fun HomeScreen(
    onAbrirSOS: () -> Unit,
    onAbrirAjuda: () -> Unit
) {
    val repository = UserRepository()
    var nomeUsuario by remember { mutableStateOf("Usuário") }
    var itemSelecionado by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        nomeUsuario = repository.buscarNomeUsuario() ?: "Usuário"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) {
        // CONTEÚDO DA TELA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 24.dp,
                    end = 24.dp,
                    bottom = 8.dp
                ),
            verticalArrangement = Arrangement.Top
        ) {
            // CABEÇALHO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SafePoint",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF164A91)
                )

                Text(
                    text = "●",
                    fontSize = 20.sp,
                    color = Color(0xFF35A853)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // SAUDAÇÃO
            Text(
                text = "Olá, $nomeUsuario!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF164A91)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bem-vindo ao SafePoint.",
                fontSize = 17.sp,
                color = Color(0xFF164A91)
            )

            Text(
                text = "Estamos aqui para te proteger.",
                fontSize = 17.sp,
                color = Color(0xFF164A91)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // CARTÃO SOS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clickable { onAbrirSOS() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE8EC)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .height(100.dp)
                            .width(100.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SOS",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Emergência",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Precisando de ajuda urgente?",
                            fontSize = 14.sp,
                            color = Color(0xFF164A91)
                        )

                        Text(
                            text = "Envie um alerta imediato.",
                            fontSize = 14.sp,
                            color = Color(0xFF164A91)
                        )
                    }

                    Text(
                        text = "›",
                        fontSize = 34.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Acesso rápido",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF164A91)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CARTÕES DE ACESSO
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { onAbrirAjuda() },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE6F1FF)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Text(text = "❓", fontSize = 30.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Preciso de ajuda",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1465D8)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Responda algumas perguntas e envie sua localização com segurança.",
                            fontSize = 14.sp,
                            color = Color(0xFF164A91)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F7F0)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Text(text = "📍", fontSize = 30.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Minhas rotas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E9B4B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Acompanhe o status das suas solicitações e veja o histórico.",
                            fontSize = 14.sp,
                            color = Color(0xFF164A91)
                        )
                    }
                }
            }
        }

        // MENU INFERIOR
        NavigationBar(
            containerColor = Color.White,
            contentColor = Color(0xFF52658A)
        ) {
            NavigationBarItem(
                selected = itemSelecionado == 0,
                onClick = { itemSelecionado = 0 },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Início"
                    )
                },
                label = { Text("Início") },
                alwaysShowLabel = true
            )

            NavigationBarItem(
                selected = itemSelecionado == 1,
                onClick = { itemSelecionado = 1 },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Mapa"
                    )
                },
                label = { Text("Mapa") },
                alwaysShowLabel = true
            )

            NavigationBarItem(
                selected = itemSelecionado == 2,
                onClick = { itemSelecionado = 2 },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "Histórico"
                    )
                },
                label = { Text("Histórico") },
                alwaysShowLabel = true
            )

            NavigationBarItem(
                selected = itemSelecionado == 3,
                onClick = { itemSelecionado = 3 },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Perfil"
                    )
                },
                label = { Text("Perfil") },
                alwaysShowLabel = true
            )
        }
    }
}