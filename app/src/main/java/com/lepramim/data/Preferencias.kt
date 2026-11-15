package com.lepramim.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cor de destaque do app (as mesmas do Escala de Trabalho, sem o roxo). */
enum class CorDestaque { AZUL, TURQUESA, LARANJA, VERMELHO, VERDE, CINZA }

/** Tema do app: segue o sistema ou fica fixo. */
enum class TemaApp { SISTEMA, CLARO, ESCURO }

private fun mesAtual() = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.ROOT).format(java.util.Date())

/** Escolhas de voz. Motor vazio = motor padrão do Android. */
data class Preferencias(
    val motor: String = "",
    val voz: String = "",
    val velocidade: Float = 1f,
    val destacar: Boolean = true,
    val regiao: String = "canadacentral",
    val chave: String = "",
    val chaveEm: Long = 0L,
    val vozAzure: String = "pt-BR-FranciscaNeural",
    val caracteres: Long = 0L,
    val mes: String = "",
    val cor: CorDestaque = CorDestaque.VERDE,
    val tema: TemaApp = TemaApp.SISTEMA,
    val markdown: Boolean = true,
    val filtros: List<String> = emptyList(),
) {
    val azure get() = motor == MOTOR_AZURE

    /** Filtros do usuário já compilados; os inválidos são ignorados. */
    val regexes: List<Regex> by lazy { filtros.mapNotNull { runCatching { Regex(it) }.getOrNull() } }

    /** Caracteres enviados à Azure neste mês (a contagem zera quando o mês vira). */
    val usadosNoMes get() = if (mes == mesAtual()) caracteres else 0L

    /** Chave colada há mais de 10 meses: hora de trocar no portal. */
    val chaveVelha get() = chave.isNotEmpty() && chaveEm > 0 && System.currentTimeMillis() - chaveEm > DEZ_MESES

    companion object {
        const val MOTOR_AZURE = "azure"
        /** Cota mensal do plano gratuito (valor aproximado; conferir no portal). */
        const val COTA_AZURE = 500_000L
        private const val DEZ_MESES = 300L * 24 * 3600 * 1000
    }
}

private val Context.loja by preferencesDataStore("leitor")

private object Chaves {
    val motor = stringPreferencesKey("motor")
    val voz = stringPreferencesKey("voz")
    val velocidade = floatPreferencesKey("velocidade")
    val destacar = booleanPreferencesKey("destacar")
    val regiao = stringPreferencesKey("regiao")
    val chave = stringPreferencesKey("chave")
    val chaveEm = longPreferencesKey("chave_em")
    val vozAzure = stringPreferencesKey("voz_azure")
    val caracteres = longPreferencesKey("caracteres")
    val mes = stringPreferencesKey("mes")
    val cor = stringPreferencesKey("cor")
    val tema = stringPreferencesKey("tema")
    val markdown = booleanPreferencesKey("markdown")
    val filtros = stringPreferencesKey("filtros")
}

class Repositorio(private val contexto: Context) {
    val preferencias: Flow<Preferencias> = contexto.loja.data.map {
        Preferencias(
            motor = it[Chaves.motor] ?: "",
            voz = it[Chaves.voz] ?: "",
            velocidade = it[Chaves.velocidade] ?: 1f,
            destacar = it[Chaves.destacar] ?: true,
            regiao = it[Chaves.regiao] ?: "canadacentral",
            chave = it[Chaves.chave] ?: "",
            chaveEm = it[Chaves.chaveEm] ?: 0L,
            vozAzure = it[Chaves.vozAzure] ?: "pt-BR-FranciscaNeural",
            caracteres = it[Chaves.caracteres] ?: 0L,
            mes = it[Chaves.mes] ?: "",
            cor = runCatching { CorDestaque.valueOf(it[Chaves.cor] ?: "") }.getOrDefault(CorDestaque.VERDE),
            tema = runCatching { TemaApp.valueOf(it[Chaves.tema] ?: "") }.getOrDefault(TemaApp.SISTEMA),
            markdown = it[Chaves.markdown] ?: true,
            filtros = it[Chaves.filtros].orEmpty().split('\n').filter { f -> f.isNotEmpty() },
        )
    }

    suspend fun definirMotor(motor: String) = contexto.loja.edit { it[Chaves.motor] = motor; it[Chaves.voz] = "" }
    suspend fun definirVoz(voz: String) = contexto.loja.edit { it[Chaves.voz] = voz }
    suspend fun definirVelocidade(v: Float) = contexto.loja.edit { it[Chaves.velocidade] = v }
    suspend fun definirDestacar(d: Boolean) = contexto.loja.edit { it[Chaves.destacar] = d }
    suspend fun definirCor(c: CorDestaque) = contexto.loja.edit { it[Chaves.cor] = c.name }
    suspend fun definirTema(t: TemaApp) = contexto.loja.edit { it[Chaves.tema] = t.name }
    suspend fun definirMarkdown(m: Boolean) = contexto.loja.edit { it[Chaves.markdown] = m }
    suspend fun definirFiltros(f: List<String>) = contexto.loja.edit { it[Chaves.filtros] = f.joinToString("\n") }
    suspend fun definirRegiao(r: String) = contexto.loja.edit { it[Chaves.regiao] = r.trim().lowercase() }
    suspend fun definirChave(c: String) = contexto.loja.edit {
        it[Chaves.chave] = c.trim()
        it[Chaves.chaveEm] = if (c.isBlank()) 0L else System.currentTimeMillis()
    }
    suspend fun definirVozAzure(v: String) = contexto.loja.edit { it[Chaves.vozAzure] = v }

    /** Soma os caracteres enviados à Azure no mês (zera ao virar o mês). */
    suspend fun somarCaracteres(n: Int) = contexto.loja.edit {
        val mes = mesAtual()
        if (it[Chaves.mes] != mes) { it[Chaves.mes] = mes; it[Chaves.caracteres] = 0L }
        it[Chaves.caracteres] = (it[Chaves.caracteres] ?: 0L) + n
    }
}
