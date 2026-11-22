package com.lepramim.leitura

import org.json.JSONArray
import org.json.JSONObject
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

    /** Versão curta para o aviso flutuante, que o sistema corta em duas linhas. */
    fun mensagemCurta(codigo: Int) = when (codigo) {
        401 -> "Chave recusada."
        403 -> "Cota ou recurso da Azure."
        429 -> "Muitas chamadas."
        0 -> "Sem internet."
        else -> "Erro da Azure."
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

    fun mensagemMonitor(codigo: Int) = when (codigo) {
        400, 401 -> "ID ou segredo recusado. Confira os três campos."
        403 -> "Sem permissão. Falta a função Leitor do Monitoramento."
        404 -> "Recurso ou assinatura não encontrados. Confira o nome do recurso."
        0 -> "Sem internet ou Azure fora do ar."
        else -> "Erro da Azure ($codigo)."
    }

    private fun json(c: HttpURLConnection): JSONObject {
        val corpo = (if (c.responseCode in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().decodeToString() }.orEmpty()
        if (c.responseCode !in 200..299) throw ErroAzure(c.responseCode)
        return JSONObject(corpo)
    }

    /**
     * Total de caracteres sintetizados no mês, lido do Azure Monitor (o mesmo número do portal).
     * Devolve o total e o ID da assinatura usada. O recurso e o grupo têm o mesmo nome.
     */
    fun consumoDoMes(tenant: String, app: String, segredo: String, assinatura: String, recurso: String): Pair<Long, String> = chamar {
        val t = URL("https://login.microsoftonline.com/${tenant.trim()}/oauth2/v2.0/token").openConnection() as HttpURLConnection
        t.connectTimeout = 10000; t.readTimeout = 20000
        t.requestMethod = "POST"; t.doOutput = true
        val enc = { s: String -> java.net.URLEncoder.encode(s.trim(), "UTF-8") }
        t.outputStream.use {
            it.write("grant_type=client_credentials&client_id=${enc(app)}&client_secret=${enc(segredo)}&scope=${enc("https://management.azure.com/.default")}".toByteArray())
        }
        val token = json(t).getString("access_token")

        fun get(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000; readTimeout = 20000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val sub = assinatura.trim().ifEmpty {
            val l = json(get("https://management.azure.com/subscriptions?api-version=2022-12-01")).getJSONArray("value")
            if (l.length() == 0) throw ErroAzure(404)
            l.getJSONObject(0).getString("subscriptionId")
        }
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ROOT).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0)
        }
        val inicio = fmt.format(cal.time)
        val fim = fmt.format(java.util.Date())
        val r = recurso.trim()
        val url = "https://management.azure.com/subscriptions/$sub/resourceGroups/$r/providers/Microsoft.CognitiveServices/accounts/$r" +
            "/providers/microsoft.insights/metrics?api-version=2018-01-01&metricnames=SynthesizedCharacters&aggregation=Total&interval=P1D&timespan=" +
            enc("$inicio/$fim")
        val series = json(get(url)).getJSONArray("value").getJSONObject(0).getJSONArray("timeseries")
        var total = 0.0
        for (i in 0 until series.length()) {
            val d = series.getJSONObject(i).optJSONArray("data") ?: continue
            for (j in 0 until d.length()) total += d.getJSONObject(j).optDouble("total", 0.0)
        }
        total.toLong() to sub
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
