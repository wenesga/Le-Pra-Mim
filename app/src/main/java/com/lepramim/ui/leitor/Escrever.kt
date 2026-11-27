package com.lepramim.ui.leitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.theme.Espaco
import com.lepramim.ui.theme.corBotao

/**
 * Janela de escrever um texto: título e nota. O botão guarda na biblioteca e deixa o texto
 * pronto na aba Leitura (sem tocar sozinho).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEscrever(vm: LeitorViewModel, aoVoltar: () -> Unit, aoContinuar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var nota by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Escrever texto") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { margens ->
        Column(
            Modifier.fillMaxSize().padding(margens).padding(horizontal = Espaco.gg).padding(bottom = Espaco.gg),
            verticalArrangement = Arrangement.spacedBy(Espaco.g),
        ) {
            OutlinedTextField(
                value = titulo, onValueChange = { titulo = it },
                placeholder = { Text("Título (opcional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            OutlinedTextField(
                value = nota, onValueChange = { nota = it },
                placeholder = { Text("Digite ou cole o texto") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Button(
                onClick = { vm.guardarEAbrir(titulo, nota); aoContinuar() },
                enabled = nota.isNotBlank(), colors = corBotao(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continuar") }
        }
    }
}
