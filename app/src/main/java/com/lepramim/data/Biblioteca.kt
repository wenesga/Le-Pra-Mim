package com.lepramim.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Um texto guardado na biblioteca. */
data class ItemBiblioteca(val id: String, val titulo: String, val texto: String, val criado: Long)

/**
 * Biblioteca de textos: um arquivo JSON por item, na pasta do app. Sem banco de dados,
 * para não depender de reflexão (o R8 do release já derrubou o app por isso em outros projetos).
 */
class Biblioteca(contexto: Context) {
    private val pasta = File(contexto.filesDir, "biblioteca").apply { mkdirs() }

    /** Itens do mais novo para o mais antigo. */
    fun listar(): List<ItemBiblioteca> =
        pasta.listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull(::ler).sortedByDescending { it.criado }

    fun salvar(titulo: String, texto: String): ItemBiblioteca {
        val item = ItemBiblioteca(UUID.randomUUID().toString(), titulo, texto, System.currentTimeMillis())
        gravar(item)
        return item
    }

    fun renomear(id: String, titulo: String) {
        ler(arquivo(id))?.let { gravar(it.copy(titulo = titulo)) }
    }

    fun excluir(id: String) {
        arquivo(id).delete()
    }

    private fun arquivo(id: String) = File(pasta, "$id.json")

    private fun ler(f: File): ItemBiblioteca? = runCatching {
        val j = JSONObject(f.readText())
        ItemBiblioteca(j.getString("id"), j.getString("titulo"), j.getString("texto"), j.getLong("criado"))
    }.getOrNull()

    private fun gravar(i: ItemBiblioteca) {
        arquivo(i.id).writeText(
            JSONObject().put("id", i.id).put("titulo", i.titulo).put("texto", i.texto).put("criado", i.criado).toString(),
        )
    }
}
