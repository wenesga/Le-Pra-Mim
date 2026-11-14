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
) {
    val azure get() = motor == MOTOR_AZURE
    companion object { const val MOTOR_AZURE = "azure" }
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
        )
    }

    suspend fun definirMotor(motor: String) = contexto.loja.edit { it[Chaves.motor] = motor; it[Chaves.voz] = "" }
    suspend fun definirVoz(voz: String) = contexto.loja.edit { it[Chaves.voz] = voz }
    suspend fun definirVelocidade(v: Float) = contexto.loja.edit { it[Chaves.velocidade] = v }
    suspend fun definirDestacar(d: Boolean) = contexto.loja.edit { it[Chaves.destacar] = d }
    suspend fun definirRegiao(r: String) = contexto.loja.edit { it[Chaves.regiao] = r.trim().lowercase() }
    suspend fun definirChave(c: String) = contexto.loja.edit {
        it[Chaves.chave] = c.trim()
        it[Chaves.chaveEm] = if (c.isBlank()) 0L else System.currentTimeMillis()
    }
    suspend fun definirVozAzure(v: String) = contexto.loja.edit { it[Chaves.vozAzure] = v }

    /** Soma os caracteres enviados à Azure no mês (zera ao virar o mês). */
    suspend fun somarCaracteres(n: Int) = contexto.loja.edit {
        val mes = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.ROOT).format(java.util.Date())
        if (it[Chaves.mes] != mes) { it[Chaves.mes] = mes; it[Chaves.caracteres] = 0L }
        it[Chaves.caracteres] = (it[Chaves.caracteres] ?: 0L) + n
    }
}
