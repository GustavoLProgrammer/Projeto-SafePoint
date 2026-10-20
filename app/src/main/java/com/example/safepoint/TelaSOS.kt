package com.example.safepoint

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon

private data class ServicoEmergencia(
    val nome: String,
    val numero: String,
    val descricao: String,
    val cor: Color,
    val fundo: Color,
    val simbolo: String
)

@Composable
fun TelaSOS(
    onVoltar: () -> Unit
) {
    val context = LocalContext.current

    val azul = Color(0xFF1554C0)
    val azulEscuro = Color(0xFF164A91)
    val fundoTela = Color(0xFFEAF6FF)
    val vermelho = Color(0xFFE52323)
    val verde = Color(0xFF20A447)

    val servicos = listOf(
        ServicoEmergencia(
            nome = "Polícia Militar",
            numero = "190",
            descricao = "Emergências policiais",
            cor = Color(0xFF1554C0),
            fundo = Color(0xFFEEF6FF),
             simbolo = "🛡️"
        ),
        ServicoEmergencia(
            nome = "Corpo de Bombeiros",
            numero = "193",
            descricao = "Incêndios, acidentes e resgates",
            cor = Color(0xFFE52323),
            fundo = Color(0xFFFFF0F0),
            simbolo = "🔥"
        ),
        ServicoEmergencia(
            nome = "Ambulância / SAMU",
            numero = "192",
            descricao = "Emergências médicas",
            cor = verde,
            fundo = Color(0xFFECFAF2),
            simbolo = "+"
        )
    )

    var servicoSelecionado by remember {
        mutableStateOf<ServicoEmergencia?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fundoTela)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        // Cabeçalho sem ícone no canto superior direito
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                modifier = Modifier
                    .clickable { onVoltar() }
                    .width(48.dp),
                color = azul,
                fontSize = 42.sp,
                lineHeight = 42.sp
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Emergência",
                    color = azulEscuro,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cartão de alerta SOS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFE8EB)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SOS",
                        color = vermelho,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Em caso de emergência",
                        color = vermelho,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Estamos aqui para te ajudar.",
                        color = azulEscuro,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Ligue para o serviço necessário.",
                        color = azulEscuro,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "⚠",
                    color = vermelho,
                    fontSize = 34.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Números de emergência",
            color = azulEscuro,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Toque para ligar",
            color = Color(0xFF61779B),
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Cartões dos serviços de emergência
        servicos.forEach { servico ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clickable {
                        servicoSelecionado = servico
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = servico.fundo
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(130.dp)
                            .background(
                                servico.cor,
                                RoundedCornerShape(12.dp)
                            )
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                horizontal = 12.dp,
                                vertical = 16.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(servico.cor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = servico.simbolo,
                                color = Color.White,
                                fontSize = if (servico.numero == "192") 52.sp else 39.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = servico.nome,
                                color = servico.cor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(7.dp))

                            Text(
                                text = "Ligue ${servico.numero}",
                                color = azulEscuro,
                                fontSize = 15.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = servico.descricao,
                                color = azulEscuro,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Botão de telefone dentro de círculo colorido
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(servico.cor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                   imageVector = Icons.Default.Call,
                                   contentDescription = "Telefone",
                                   tint = Color.White,
                                   modifier = Modifier.size(30.dp)
                                )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Cartão de dicas importantes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEEF6FF)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ⓘ",
                        color = azul,
                        fontSize = 35.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Dicas importantes",
                        color = azul,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Informe seu local com clareza e mantenha a calma.",
                        color = azulEscuro,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sua segurança é o mais importante.",
                        color = azulEscuro,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A ligação só será iniciada por você, após confirmar e abrir o discador.",
            fontSize = 12.sp,
            color = Color(0xFF61779B)
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Confirmação antes de abrir o discador
    servicoSelecionado?.let { servico ->
        AlertDialog(
            onDismissRequest = {
                servicoSelecionado = null
            },
            title = {
                Text("Confirmar ligação")
            },
            text = {
                Text(
                    "Deseja ligar para ${servico.nome} — ${servico.numero}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        servicoSelecionado = null

                        val intent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse("tel:${servico.numero}")
                        )

                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828)
                    )
                ) {
                    Text("Abrir discador")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        servicoSelecionado = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
