package com.lepramim.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** Backup automático: grava a biblioteca num arquivo, numa pasta escolhida pela pessoa (pode ser do Google Drive). */
object Backup {
    private const val ATUAL = "LePraMim-biblioteca.json"

    /** Atualiza sempre o mesmo arquivo na pasta escolhida. Devolve false se a pasta não estiver mais acessível. Chamar fora da thread principal. */
    fun gravarNaPasta(contexto: Context, pasta: Uri, json: String): Boolean = runCatching {
        val raiz = DocumentFile.fromTreeUri(contexto, pasta)
        if (raiz == null || !raiz.canWrite()) return@runCatching false
        escrever(contexto, raiz, ATUAL, json)
        true
    }.getOrDefault(false)

    private fun escrever(contexto: Context, raiz: DocumentFile, nome: String, json: String) {
        val arquivo = raiz.findFile(nome) ?: raiz.createFile("application/json", nome) ?: error("sem arquivo")
        contexto.contentResolver.openOutputStream(arquivo.uri, "wt")!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }
}
