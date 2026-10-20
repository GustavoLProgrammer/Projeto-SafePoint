
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PerguntaFrequente(
    val pergunta: String,
    val resposta: String
)

@Composable
fun TelaAjuda(
    onVoltar: () -> Unit
) {
    val perguntas = listOf(
        PerguntaFrequente(
            pergunta = "Como faço para entrar no aplicativo?",
            resposta = "Acesse sua conta usando os dados de login cadastrados no SafePoint."
        ),
        PerguntaFrequente(
            pergunta = "Esqueci minha senha. O que faço?",
            resposta = "Na tela de login, utilize a opção de recuperação de senha para receber as instruções de redefinição."
        ),
        PerguntaFrequente(
            pergunta = "Para que serve o botão SOS?",
            resposta = "O botão SOS permite escolher um serviço de emergência: Polícia Militar (190), Corpo de Bombeiros (193) ou SAMU (192). O aplicativo solicita sua confirmação e abre o discador do celular."
        ),
        PerguntaFrequente(
            pergunta = "Como acesso minhas rotas?",
            resposta = "Você pode acessar Minhas rotas pelo atalho da tela inicial ou pela navegação do aplicativo, quando disponível."
        ),
        PerguntaFrequente(
            pergunta = "Como consulto ou altero meus dados?",
            resposta = "Acesse a área Perfil para consultar suas informações e verificar as opções de edição disponíveis."
        ),
        PerguntaFrequente(
            pergunta = "O aplicativo apresentou um problema. O que faço?",
            resposta = "Verifique sua conexão com a internet e abra o aplicativo novamente. Se o problema continuar, entre em contato com a equipe responsável pelo SafePoint."
        )
    )

    var perguntaAberta by remember {
        mutableStateOf<Int?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
            .padding(24.dp)
    ) {
        TextButton(
            onClick = onVoltar
        ) {
            Text(
                text = "‹  Voltar",
                fontSize = 17.sp,
                color = Color(0xFF164A91)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Perguntas frequentes",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF164A91)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Encontre respostas para suas dúvidas sobre o SafePoint.",
            fontSize = 16.sp,
            color = Color(0xFF164A91)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            perguntas.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    perguntaAberta =
                                        if (perguntaAberta == index) {
                                            null
                                        } else {
                                            index
                                        }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.pergunta,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF164A91)
                            )

                            Text(
                                text = if (perguntaAberta == index) "−" else "+",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1465D8)
                            )
                        }

                        if (perguntaAberta == index) {
                            HorizontalDivider(
                                color = Color(0xFFE5EAF0)
                            )

                            Text(
                                text = item.resposta,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 15.sp,
                                color = Color(0xFF43536A),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
