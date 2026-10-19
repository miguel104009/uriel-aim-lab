package com.urielgaming.trainer

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

val FundoNeon = Color(0xFF040814)
val CartaoNeon = Color(0xFF0B162C)
val BordaNeon = Color(0xFF143560)
val AzulNeon = Color(0xFF0099FF)
val CianoNeon = Color(0xFF26DCFF)
val RosaNeon = Color(0xFFFF2A85)
val TextoClaro = Color(0xFFEEF5FF)
val TextoCinza = Color(0xFF8BA2C4)

class MainActivity : ComponentActivity() {
    override fun onCreate(estadoSalvo: Bundle?) {
        super.onCreate(estadoSalvo)
        val preferencias = getSharedPreferences("preferencias_uriel", Context.MODE_PRIVATE)

        setContent {
            var telaAtual by remember { mutableStateOf("inicio") }
            var recordeAtual by remember { mutableIntStateOf(preferencias.getInt("recorde", 0)) }
            var sensibilidade by remember { mutableFloatStateOf(preferencias.getFloat("sensibilidade", 50f)) }
            var vibracaoAtiva by remember { mutableStateOf(preferencias.getBoolean("vibracao", true)) }

            Scaffold(
                containerColor = FundoNeon,
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CartaoNeon)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BotaoAba("INÍCIO", telaAtual == "inicio") { telaAtual = "inicio" }
                        BotaoAba("TREINO", telaAtual == "treino") { telaAtual = "treino" }
                        BotaoAba("AJUSTES", telaAtual == "ajustes") { telaAtual = "ajustes" }
                    }
                }
            ) { preenchimento ->
                Box(modifier = Modifier.fillMaxSize().padding(preenchimento)) {
                    when (telaAtual) {
                        "inicio" -> TelaInicial(recordeAtual) { telaAtual = "treino" }
                        "treino" -> TelaTreino(sensibilidade, vibracaoAtiva) { novaPontuacao ->
                            if (novaPontuacao > recordeAtual) {
                                recordeAtual = novaPontuacao
                                preferencias.edit().putInt("recorde", novaPontuacao).apply()
                            }
                        }
                        "ajustes" -> TelaAjustes(
                            sensibilidadeAtual = sensibilidade,
                            vibracaoHabilitada = vibracaoAtiva,
                            aoMudarSensibilidade = {
                                sensibilidade = it
                                preferencias.edit().putFloat("sensibilidade", it).apply()
                            },
                            aoMudarVibracao = {
                                vibracaoAtiva = it
                                preferencias.edit().putBoolean("vibracao", it).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BotaoAba(texto: String, ativo: Boolean, aoTocar: () -> Unit) {
    Text(
        text = texto,
        color = if (ativo) CianoNeon else TextoCinza,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.clickable { aoTocar() }.padding(8.dp)
    )
}

@Composable
fun TelaInicial(recorde: Int, aoIniciar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("⚡ URIEL AIM LAB", color = CianoNeon, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("LABORATÓRIO DE REFLEXOS E PRECISÃO", color = TextoCinza, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF092042), Color(0xFF061124))))
                .border(1.dp, AzulNeon, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text("TREINO COMPETITIVO", color = CianoNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Melhore seu tempo de reação.", color = TextoClaro, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Treino motor isolado de 30 segundos. Totalmente seguro e sem vínculo a outros jogos.", color = TextoCinza, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = aoIniciar,
                    colors = ButtonDefaults.buttonColors(containerColor = AzulNeon),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JOGAR AGORA", color = TextoClaro, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("RECORDE LOCAL: $recorde PONTOS", color = CianoNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun TelaTreino(sensibilidade: Float, vibracao: Boolean, aoSalvarPontos: (Int) -> Unit) {
    val contexto = LocalContext.current
    val vibrador = remember { contexto.getSystemService(Vibrator::class.java) }
    var treinoEmAndamento by remember { mutableStateOf(false) }
    var pontos by remember { mutableIntStateOf(0) }
    var tempoRestante by remember { mutableIntStateOf(30) }
    var tamanhoArena by remember { mutableStateOf(IntSize.Zero) }
    var posicaoAlvo by remember { mutableStateOf(IntOffset(120, 120)) }
    var exibirResultado by remember { mutableStateOf(false) }

    val tamanhoAlvo = (68 - (sensibilidade / 100f * 26)).coerceIn(38f, 68f)

    fun acionarVibracao() {
        if (vibracao && vibrador != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(35)
            }
        }
    }

    fun moverAlvo() {
        if (tamanhoArena.width > 160 && tamanhoArena.height > 160) {
            val margem = 60
            val limiteX = (tamanhoArena.width - margem - tamanhoAlvo.toInt()).coerceAtLeast(margem + 1)
            val limiteY = (tamanhoArena.height - margem - tamanhoAlvo.toInt()).coerceAtLeast(margem + 1)
            val novoX = Random.nextInt(margem, limiteX)
            val novoY = Random.nextInt(margem, limiteY)
            posicaoAlvo = IntOffset(novoX, novoY)
        }
    }

    LaunchedEffect(treinoEmAndamento) {
        if (treinoEmAndamento) {
            pontos = 0
            tempoRestante = 30
            moverAlvo()
            while (tempoRestante > 0 && treinoEmAndamento) {
                delay(1000)
                tempoRestante--
            }
            if (treinoEmAndamento) {
                treinoEmAndamento = false
                exibirResultado = true
                aoSalvarPontos(pontos)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CartaoNeon)
                .border(1.dp, BordaNeon, RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PONTOS: $pontos", color = CianoNeon, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("TEMPO: ${tempoRestante}s", color = if (tempoRestante <= 5) RosaNeon else TextoClaro, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF020712))
                .border(1.dp, BordaNeon, RoundedCornerShape(18.dp))
                .onSizeChanged { tamanhoArena = it }
        ) {
            if (!treinoEmAndamento) {
                Text(
                    text = "Toque em INICIAR para treinar",
                    color = TextoCinza,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Box(
                    modifier = Modifier
                        .offset { posicaoAlvo }
                        .size(tamanhoAlvo.dp)
                        .clip(CircleShape)
                        .background(CianoNeon.copy(alpha = 0.3f))
                        .border(2.dp, CianoNeon, CircleShape)
                        .clickable {
                            pontos += 10
                            acionarVibracao()
                            moverAlvo()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((tamanhoAlvo * 0.45f).dp)
                            .clip(CircleShape)
                            .background(AzulNeon)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { treinoEmAndamento = !treinoEmAndamento },
            colors = ButtonDefaults.buttonColors(containerColor = if (treinoEmAndamento) RosaNeon else AzulNeon),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (treinoEmAndamento) "PARAR" else "INICIAR (30s)", color = TextoClaro, fontWeight = FontWeight.Bold)
        }
    }

    if (exibirResultado) {
        AlertDialog(
            onDismissRequest = { exibirResultado = false },
            containerColor = CartaoNeon,
            title = { Text("FIM DA SESSÃO", color = CianoNeon, fontWeight = FontWeight.Black) },
            text = { Text("Sua pontuação: $pontos pontos!", color = TextoClaro) },
            confirmButton = {
                TextButton(onClick = { exibirResultado = false; treinoEmAndamento = true }) {
                    Text("JOGAR DE NOVO", color = CianoNeon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exibirResultado = false }) {
                    Text("FECHAR", color = TextoCinza)
                }
            }
        )
    }
}

@Composable
fun TelaAjustes(
    sensibilidadeAtual: Float,
    vibracaoHabilitada: Boolean,
    aoMudarSensibilidade: (Float) -> Unit,
    aoMudarVibracao: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AJUSTES", color = TextoClaro, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CartaoNeon)
                .border(1.dp, BordaNeon, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sensibilidade: ${sensibilidadeAtual.toInt()}", color = TextoClaro, fontWeight = FontWeight.Bold)
                Text("Alvo Menor", color = CianoNeon, fontSize = 12.sp)
            }
            Slider(
                value = sensibilidadeAtual,
                onValueChange = aoMudarSensibilidade,
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(thumbColor = CianoNeon, activeTrackColor = AzulNeon)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CartaoNeon)
                .border(1.dp, BordaNeon, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vibração ao acertar", color = TextoClaro, fontWeight = FontWeight.Bold)
            Switch(checked = vibracaoHabilitada, onCheckedChange = aoMudarVibracao)
        }
    }
}
