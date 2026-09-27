package com.example.safepoint

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.safepoint.ui.theme.SafepointTheme
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            SafepointTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TelaRecuperarSenha(
                        modifier = Modifier.padding(innerPadding),
                        onVoltar = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun TelaRecuperarSenha(modifier: Modifier = Modifier, onVoltar: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    val azulEscuro = Color(0xFF0F3E8A)
    val azulLinhas = Color(0xFF1976D2)
    val verdeGradiente = Color(0xFF1E9B44)
    val cinzaTexto = Color(0xFF7A8B9E)
    val cinzaBorda = Color(0xFFD6DFE8)
    val textoDigitado = Color(0xFF1A1A1A)

    val gradientFundo = Brush.linearGradient(
        colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B)),
        start = Offset(0f, 0f),
        end = Offset(1200f, 2200f)
    )

    val gradientBotao = Brush.horizontalGradient(
        colors = listOf(azulEscuro, verdeGradiente)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientFundo)
    ) {
        Card(
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 56.dp, bottom = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Botão Voltar topo esquerdo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = azulEscuro)
                    }
                }

                LogoSafePointRecuperacao(modifier = Modifier.size(80.dp))

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SAFEPOINT",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = azulEscuro
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recuperar senha",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulEscuro
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Digite seu e-mail cadastrado para receber as instruções de redefinição",
                    fontSize = 13.sp,
                    color = cinzaTexto,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = { Text("E-mail", color = cinzaTexto, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textoDigitado,
                        unfocusedTextColor = textoDigitado,
                        focusedBorderColor = azulEscuro,
                        unfocusedBorderColor = cinzaBorda
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(gradientBotao)
                        .clickable {
                            if (email.isBlank()) {
                                Toast.makeText(context, "Insira o seu e-mail!", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }

                            isLoading = true
                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "E-mail de recuperação enviado com sucesso!", Toast.LENGTH_LONG).show()
                                        onVoltar()
                                    } else {
                                        Toast.makeText(context, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Enviar Instruções", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LogoSafePointRecuperacao(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val azulEscuro = Color(0xFF0F3E8A)
        val verdeEscudo = Color(0xFF1EA84C)
        val escudoGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0C3875), Color(0xFF138A4B)),
            startY = 0f,
            endY = h
        )
        val pathEscudoExterno = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.82f, h * 0.05f, w * 0.94f, h * 0.18f, w * 0.94f, h * 0.42f)
            cubicTo(w * 0.94f, h * 0.72f, w * 0.52f, h * 0.96f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.48f, h * 0.96f, w * 0.06f, h * 0.72f, w * 0.06f, h * 0.42f)
            cubicTo(w * 0.06f, h * 0.18f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(path = pathEscudoExterno, brush = escudoGradient, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        val pathEscudoInterno = Path().apply {
            moveTo(w * 0.5f, h * 0.13f)
            cubicTo(w * 0.75f, h * 0.13f, w * 0.84f, h * 0.24f, w * 0.84f, h * 0.44f)
            cubicTo(w * 0.84f, h * 0.67f, w * 0.52f, h * 0.86f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.48f, h * 0.86f, w * 0.16f, h * 0.67f, w * 0.16f, h * 0.44f)
            cubicTo(w * 0.16f, h * 0.24f, w * 0.25f, h * 0.13f, w * 0.5f, h * 0.13f)
            close()
        }
        drawPath(path = pathEscudoInterno, color = Color(0x181EA84C), style = Fill)
        drawPath(path = pathEscudoInterno, color = verdeEscudo.copy(alpha = 0.5f), style = Stroke(width = 1.2.dp.toPx()))
        val pinCentroX = w * 0.5f
        val pinTopoY = h * 0.28f
        val pinRaio = w * 0.18f
        val pinPontaY = h * 0.70f
        val pathPin = Path().apply {
            moveTo(pinCentroX, pinPontaY)
            cubicTo(pinCentroX - pinRaio * 0.95f, pinPontaY - h * 0.14f, pinCentroX - pinRaio, pinTopoY + pinRaio * 1.1f, pinCentroX - pinRaio, pinTopoY + pinRaio)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(left = pinCentroX - pinRaio, top = pinTopoY, right = pinCentroX + pinRaio, bottom = pinTopoY + pinRaio * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            cubicTo(pinCentroX + pinRaio, pinTopoY + pinRaio * 1.1f, pinCentroX + pinRaio * 0.95f, pinPontaY - h * 0.14f, pinCentroX, pinPontaY)
            close()
        }
        drawPath(path = pathPin, color = azulEscuro, style = Fill)
        drawCircle(color = Color.White, radius = pinRaio * 0.40f, center = Offset(pinCentroX, pinTopoY + pinRaio))
        drawCircle(color = verdeEscudo, radius = pinRaio * 0.22f, center = Offset(pinCentroX, pinTopoY + pinRaio))
    }
}
