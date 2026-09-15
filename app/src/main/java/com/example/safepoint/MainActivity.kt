package com.example.safepoint

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepoint.ui.theme.SafepointTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafepointTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FluxoAutenticacao(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun FluxoAutenticacao(modifier: Modifier = Modifier) {
    var telaAtual by remember { mutableStateOf("LOGIN") } // "LOGIN", "CADASTRO", "HOME"

    // Banco de dados em memória com conta de teste padrão
    val usuariosCadastrados = remember {
        mutableStateMapOf(
            "teste@safepoint.com" to "123456"
        )
    }

    when (telaAtual) {
        "LOGIN" -> {
            TelaLogin(
                modifier = modifier,
                onIrParaCadastro = { telaAtual = "CADASTRO" },
                onLoginSucesso = { telaAtual = "HOME" },
                usuarios = usuariosCadastrados
            )
        }
        "CADASTRO" -> {
            TelaCadastro(
                modifier = modifier,
                onVoltarParaLogin = { telaAtual = "LOGIN" },
                onCadastroSucesso = { email, senha ->
                    usuariosCadastrados[email] = senha
                    telaAtual = "LOGIN"
                }
            )
        }
        "HOME" -> {
            TelaHomeSucesso(
                modifier = modifier,
                onSair = { telaAtual = "LOGIN" }
            )
        }
    }
}

@Composable
fun TelaLogin(
    modifier: Modifier = Modifier,
    onIrParaCadastro: () -> Unit,
    onLoginSucesso: () -> Unit,
    usuarios: Map<String, String>
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var lembrarMe by remember { mutableStateOf(false) }
    var senhaVisivel by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val azulEscuro = Color(0xFF0F3E8A)
    val azulLinhas = Color(0xFF1976D2)
    val verdeGradiente = Color(0xFF1E9B44)
    val cinzaTexto = Color(0xFF7A8B9E)
    val cinzaBorda = Color(0xFFD6DFE8)

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
                .padding(top = 76.dp, bottom = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 30.dp)
            ) {
                LogoSafePoint(modifier = Modifier.size(92.dp))

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SAFEPOINT",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = azulEscuro
                )

                Text(
                    text = "Sempre te guiando a um ponto seguro",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = azulEscuro
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "Faça seu login",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulEscuro
                )

                Text(
                    text = "Conta teste: teste@safepoint.com | 123456",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = verdeGradiente,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = { Text("E-mail", color = cinzaTexto, fontSize = 14.sp) },
                    leadingIcon = { IconeUsuario(cor = azulEscuro, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = azulEscuro,
                        unfocusedBorderColor = cinzaBorda
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it.trim() },
                    placeholder = { Text("Senha", color = cinzaTexto, fontSize = 14.sp) },
                    leadingIcon = { IconeCadeado(cor = azulEscuro, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                            IconeOlho(aberto = senhaVisivel, cor = cinzaTexto, modifier = Modifier.size(22.dp))
                        }
                    },
                    visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = azulEscuro,
                        unfocusedBorderColor = cinzaBorda
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { lembrarMe = !lembrarMe }
                    ) {
                        Checkbox(
                            checked = lembrarMe,
                            onCheckedChange = { lembrarMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = verdeGradiente,
                                uncheckedColor = cinzaBorda
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lembrar-me", color = azulEscuro, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Text(
                        text = "Esqueci minha senha",
                        color = azulLinhas,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Use: teste@safepoint.com / 123456", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botão Entrar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(gradientBotao)
                        .clickable {
                            if (email.isBlank() || senha.isBlank()) {
                                Toast.makeText(context, "Preencha e-mail e senha!", Toast.LENGTH_SHORT).show()
                            } else if (usuarios[email] == senha) {
                                Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                                onLoginSucesso()
                            } else {
                                Toast.makeText(context, "E-mail ou senha incorretos!", Toast.LENGTH_LONG).show()
                            }
                        }
                ) {
                    Text("Entrar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Não tem uma conta? ", color = cinzaTexto, fontSize = 13.sp)
                    Text(
                        text = "Criar conta",
                        color = azulLinhas,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onIrParaCadastro() }
                    )
                }
            }
        }
    }
}

@Composable
fun TelaCadastro(
    modifier: Modifier = Modifier,
    onVoltarParaLogin: () -> Unit,
    onCadastroSucesso: (String, String) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmaSenha by remember { mutableStateOf("") }
    val context = LocalContext.current

    val azulEscuro = Color(0xFF0F3E8A)
    val azulLinhas = Color(0xFF1976D2)
    val verdeGradiente = Color(0xFF1E9B44)
    val cinzaTexto = Color(0xFF7A8B9E)
    val cinzaBorda = Color(0xFFD6DFE8)

    val gradientFundo = Brush.linearGradient(
        colors = listOf(Color(0xFF072B66), Color(0xFF0D52A0), Color(0xFF138A4B))
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
                .padding(top = 50.dp, bottom = 30.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Text("Criar conta", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = azulEscuro)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    placeholder = { Text("Nome completo", color = cinzaTexto) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = { Text("E-mail", color = cinzaTexto) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it.trim() },
                    placeholder = { Text("Senha", color = cinzaTexto) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmaSenha,
                    onValueChange = { confirmaSenha = it.trim() },
                    placeholder = { Text("Confirmar Senha", color = cinzaTexto) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(gradientBotao)
                        .clickable {
                            when {
                                nome.isBlank() || email.isBlank() || senha.isBlank() -> {
                                    Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                                }
                                senha != confirmaSenha -> {
                                    Toast.makeText(context, "As senhas não conferem!", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(context, "Conta criada! Faça seu login.", Toast.LENGTH_LONG).show()
                                    onCadastroSucesso(email, senha)
                                }
                            }
                        }
                ) {
                    Text("Cadastrar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row {
                    Text("Já possui uma conta? ", color = cinzaTexto, fontSize = 13.sp)
                    Text(
                        text = "Entrar",
                        color = azulLinhas,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onVoltarParaLogin() }
                    )
                }
            }
        }
    }
}

// Tela exibida após o login com sucesso
@Composable
fun TelaHomeSucesso(modifier: Modifier = Modifier, onSair: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F3E8A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoSafePoint(modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Bem-vindo ao SafePoint!", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Login efetuado com sucesso.", color = Color(0xFF1EA84C), fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSair,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EA84C))
        ) {
            Text("Sair da conta", color = Color.White)
        }
    }
}

// ==========================================
// DESENHO DA LOGO SAFEPOINT
// ==========================================

@Composable
fun LogoSafePoint(modifier: Modifier = Modifier) {
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

        drawPath(
            path = pathEscudoExterno,
            brush = escudoGradient,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

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
            cubicTo(
                pinCentroX - pinRaio * 0.95f, pinPontaY - h * 0.14f,
                pinCentroX - pinRaio, pinTopoY + pinRaio * 1.1f,
                pinCentroX - pinRaio, pinTopoY + pinRaio
            )
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = pinCentroX - pinRaio,
                    top = pinTopoY,
                    right = pinCentroX + pinRaio,
                    bottom = pinTopoY + pinRaio * 2f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            cubicTo(
                pinCentroX + pinRaio, pinTopoY + pinRaio * 1.1f,
                pinCentroX + pinRaio * 0.95f, pinPontaY - h * 0.14f,
                pinCentroX, pinPontaY
            )
            close()
        }

        drawPath(path = pathPin, color = azulEscuro, style = Fill)
        drawCircle(color = Color.White, radius = pinRaio * 0.40f, center = Offset(pinCentroX, pinTopoY + pinRaio))
        drawCircle(color = verdeEscudo, radius = pinRaio * 0.22f, center = Offset(pinCentroX, pinTopoY + pinRaio))
    }
}

// Ícones complementares
@Composable
fun IconeUsuario(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx())
        drawCircle(color = cor, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
        val corpoPath = Path().apply {
            moveTo(w * 0.15f, h * 0.92f)
            cubicTo(w * 0.15f, h * 0.68f, w * 0.85f, h * 0.68f, w * 0.85f, h * 0.92f)
        }
        drawPath(path = corpoPath, color = cor, style = stroke)
    }
}

@Composable
fun IconeCadeado(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx())
        drawRoundRect(
            color = cor,
            topLeft = Offset(w * 0.18f, h * 0.42f),
            size = Size(w * 0.64f, h * 0.52f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = stroke
        )
        val arcoPath = Path().apply {
            moveTo(w * 0.30f, h * 0.42f)
            lineTo(w * 0.30f, h * 0.24f)
            cubicTo(w * 0.30f, h * 0.08f, w * 0.70f, h * 0.08f, w * 0.70f, h * 0.24f)
            lineTo(w * 0.70f, h * 0.42f)
        }
        drawPath(path = arcoPath, color = cor, style = stroke)
        drawCircle(color = cor, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.65f))
    }
}

@Composable
fun IconeOlho(aberto: Boolean, cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val olhoPath = Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.9f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(path = olhoPath, color = cor, style = stroke)
        if (aberto) {
            drawCircle(color = cor, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.5f))
        } else {
            drawCircle(color = cor, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.5f))
            drawLine(color = cor, start = Offset(w * 0.2f, h * 0.2f), end = Offset(w * 0.8f, h * 0.8f), strokeWidth = stroke.width)
        }
    }
}