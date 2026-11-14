package com.lepramim.leitura

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Erro da Azure com o código HTTP (0 = sem internet). */
class ErroAzure(val codigo: Int) : Exception("Azure $codigo")

object Azure {
    fun mensagem(codigo: Int) = when (codigo) {
        401 -> "Chave recusada: foi trocada ou a região está errada. Cole outra."
        403 -> "Recurso desativado ou cota acabou. Veja no portal da Azure."
        429 -> "Muitas chamadas. Espere um pouco."
        0 -> "Sem internet ou Azure fora do ar."
        else -> "Erro da Azure ($codigo)."
    }

    private fun abrir(url: String, chave: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000; readTimeout = 20000
            setRequestProperty("Ocp-Apim-Subscription-Key", chave)
        }

    private fun <T> chamar(bloco: () -> T): T = try { bloco() } catch (e: ErroAzure) { throw e } catch (e: Exception) { throw ErroAzure(0) }

    /** Fala o texto e devolve o MP3. Velocidade 1.0 = normal. */
    fun sintetizar(regiao: String, chave: String, voz: String, velocidade: Float, texto: String): ByteArray = chamar {
        val c = abrir("https://$regiao.tts.speech.microsoft.com/cognitiveservices/v1", chave)
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/ssml+xml")
        c.setRequestProperty("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
        c.setRequestProperty("User-Agent", "LePraMim")
        val taxa = Math.round((velocidade - 1f) * 100)
        val limpo = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val ssml = "<speak version='1.0' xml:lang='pt-BR'><voice name='$voz'><prosody rate='${if (taxa >= 0) "+" else ""}$taxa%'>$limpo</prosody></voice></speak>"
        c.outputStream.use { it.write(ssml.toByteArray()) }
        if (c.responseCode != 200) throw ErroAzure(c.responseCode)
        c.inputStream.use { it.readBytes() }
    }

    /** Vozes pt-BR neurais disponíveis na região (nome curto e rótulo). */
    fun vozes(regiao: String, chave: String): List<Pair<String, String>> = chamar {
        val c = abrir("https://$regiao.tts.speech.microsoft.com/cognitiveservices/voices/list", chave)
        if (c.responseCode != 200) throw ErroAzure(c.responseCode)
        val a = JSONArray(c.inputStream.use { it.readBytes().decodeToString() })
        (0 until a.length()).map { a.getJSONObject(it) }
            .filter { it.optString("Locale") == "pt-BR" }
            .filter { Regex("pt-BR-[A-Za-z]+Neural").matches(it.getString("ShortName")) && !it.getString("ShortName").contains("Multilingual") }
            .map { it.getString("ShortName") to it.getString("ShortName").removePrefix("pt-BR-").removeSuffix("Neural") }
            .sortedBy { it.second }
    }
}
