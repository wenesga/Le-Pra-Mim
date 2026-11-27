package com.lepramim.data

import android.content.Context
import org.json.JSONArray
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
    private companion object { const val FORMATO = "lepramim-biblioteca" }

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

    /** Toda a biblioteca num único texto JSON (só os textos; nada de chaves nem preferências). */
    fun exportar(): String {
        val itens = JSONArray()
        listar().forEach { itens.put(JSONObject().put("id", it.id).put("titulo", it.titulo).put("texto", it.texto).put("criado", it.criado)) }
        return JSONObject().put("formato", FORMATO).put("versao", 1).put("itens", itens).toString()
    }

    /** Traz de volta os itens de um backup, sem duplicar os que já existem. Devolve quantos entraram; null se o arquivo não for um backup. */
    fun restaurar(json: String): Int? = runCatching {
        val raiz = JSONObject(json)
        if (raiz.optString("formato") != FORMATO) return@runCatching null
        val itens = raiz.getJSONArray("itens")
        var novos = 0
        for (i in 0 until itens.length()) {
            val o = itens.getJSONObject(i)
            // O id vira nome de arquivo: só aceita letras, números e traço.
            val id = o.getString("id").takeIf { Regex("[A-Za-z0-9-]{1,64}").matches(it) } ?: UUID.randomUUID().toString()
            if (arquivo(id).exists()) continue
            gravar(ItemBiblioteca(id, o.getString("titulo"), o.getString("texto"), o.optLong("criado", System.currentTimeMillis())))
            novos++
        }
        novos
    }.getOrNull()

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
