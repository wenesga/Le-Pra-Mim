package com.lepramim.ui.ajustes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.data.Preferencias
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.componentes.Seletor
import java.util.Locale

/** Vozes pt-BR mostradas enquanto a lista da API não carregou. */
val VOZES_AZURE_PADRAO = listOf(
    "pt-BR-FranciscaNeural" to "Francisca",
    "pt-BR-AntonioNeural" to "Antonio",
    "pt-BR-ThalitaNeural" to "Thalita",
)

fun formatarVelocidade(v: Float): String = String.format(Locale("pt", "BR"), "%.1fx", v)

/** Escolha entre a voz do aparelho e a da Azure. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorMotor(vm: LeitorViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val opcoes = listOf("" to "Aparelho", Preferencias.MOTOR_AZURE to "Azure")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opcoes.forEachIndexed { i, (id, nome) ->
            SegmentedButton(
                selected = prefs.motor == id,
                onClick = { vm.definirMotor(id) },
                shape = SegmentedButtonDefaults.itemShape(i, opcoes.size),
            ) { Text(nome) }
        }
    }
}

/** Voz do aparelho (só aparece se o motor listou alguma). */
@Composable
fun SeletorVozAparelho(vm: LeitorViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val vozes by vm.vozes.collectAsStateWithLifecycle()
    if (vozes.isEmpty()) return
    Seletor(
        rotulo = "Voz",
        valor = vozes.find { it.id == prefs.voz }?.nome ?: "Padrão",
        opcoes = listOf("" to "Padrão") + vozes.map { it.id to it.nome },
        escolher = vm::definirVoz,
    )
}

@Composable
fun SeletorVozAzure(vm: LeitorViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val vozes by vm.vozesAzure.collectAsStateWithLifecycle()
    Seletor(
        rotulo = "Voz",
        valor = nomeDaVozAzure(prefs.vozAzure, vozes.map { it.id to it.nome }),
        opcoes = vozes.map { it.id to it.nome }.ifEmpty { VOZES_AZURE_PADRAO },
        escolher = vm::definirVozAzure,
    )
}

/** Nome curto da voz da Azure (Francisca, Antonio...) para chips e resumos. */
fun nomeDaVozAzure(id: String, lista: List<Pair<String, String>> = emptyList()): String =
    lista.find { it.first == id }?.second ?: id.removePrefix("pt-BR-").removeSuffix("Neural")

@Composable
fun ControleVelocidade(vm: LeitorViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    var velocidade by remember(prefs.velocidade) { mutableFloatStateOf(prefs.velocidade) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Velocidade", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(formatarVelocidade(velocidade), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = velocidade, onValueChange = { velocidade = it },
        onValueChangeFinished = { vm.definirVelocidade(velocidade) },
        valueRange = 0.5f..2.5f, steps = 19,
    )
}
