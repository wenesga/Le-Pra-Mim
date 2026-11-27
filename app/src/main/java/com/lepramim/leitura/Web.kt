package com.lepramim.leitura

import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/** Baixa uma página da web e tira o texto principal (sem biblioteca extra). */
object Web {
    private val opcoes = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    private const val LIMITE = 3_000_000

    /** Título e texto da página, ou null se não abrir. Chamar fora da thread principal. */
    fun ler(entrada: String): Pair<String, String>? = runCatching {
        val endereco = entrada.trim().let { if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it" }
        val con = (URL(endereco).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.5")
        }
        if (con.responseCode !in 200..299) return@runCatching null
        val bytes = con.inputStream.use { entrada ->
            val saida = java.io.ByteArrayOutputStream()
            val bloco = ByteArray(8192)
            while (saida.size() < LIMITE) {
                val n = entrada.read(bloco)
                if (n < 0) break
                saida.write(bloco, 0, n)
            }
            saida.toByteArray()
        }
        val html = decodificar(con.contentType, bytes)
        val titulo = Regex("<title[^>]*>(.*?)</title>", opcoes).find(html)?.groupValues?.get(1)
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
            ?.take(80)?.ifEmpty { null } ?: URL(endereco).host
        titulo to extrair(html)
    }.getOrNull()

    /** Texto da página: usa a codificação declarada; sem declaração, UTF-8 se for válido, senão Windows-1252 (sites antigos). */
    private fun decodificar(tipo: String?, bytes: ByteArray): String {
        val nome = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(tipo.orEmpty())?.groupValues?.get(1)
            ?: Regex("<meta[^>]+charset=[\"']?([\\w-]+)", RegexOption.IGNORE_CASE)
                .find(String(bytes, 0, minOf(bytes.size, 32_768), Charsets.ISO_8859_1))?.groupValues?.get(1)
        val declarada = nome?.let { runCatching { Charset.forName(it) }.getOrNull() }
        if (declarada != null) return String(bytes, declarada)
        val utf8 = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        return runCatching { utf8.decode(java.nio.ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { String(bytes, Charset.forName("windows-1252")) }
    }

    private fun extrair(html: String): String {
        val limpo = html
            .replace(Regex("<!--.*?-->", opcoes), " ")
            .replace(Regex("<(script|style|noscript|nav|header|footer|aside|form|svg|iframe|button)[^>]*>.*?</\\1>", opcoes), " ")
        // Prefere o miolo da matéria (article ou main); sem isso, a página toda.
        val miolo = listOf("article", "main", "body")
            .mapNotNull { tag -> Regex("<$tag[^>]*>(.*?)</$tag>", opcoes).findAll(limpo).map { it.groupValues[1] }.maxByOrNull { it.length } }
            .firstOrNull { it.length > 600 } ?: limpo
        return Html.fromHtml(miolo, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace('￼', ' ')
            .lines().joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
