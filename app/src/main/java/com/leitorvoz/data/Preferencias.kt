package com.leitorvoz.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
)

private val Context.loja by preferencesDataStore("leitor")

private object Chaves {
    val motor = stringPreferencesKey("motor")
    val voz = stringPreferencesKey("voz")
    val velocidade = floatPreferencesKey("velocidade")
    val destacar = booleanPreferencesKey("destacar")
}

class Repositorio(private val contexto: Context) {
    val preferencias: Flow<Preferencias> = contexto.loja.data.map {
        Preferencias(
            motor = it[Chaves.motor] ?: "",
            voz = it[Chaves.voz] ?: "",
            velocidade = it[Chaves.velocidade] ?: 1f,
            destacar = it[Chaves.destacar] ?: true,
        )
    }

    suspend fun definirMotor(motor: String) = contexto.loja.edit { it[Chaves.motor] = motor; it[Chaves.voz] = "" }
    suspend fun definirVoz(voz: String) = contexto.loja.edit { it[Chaves.voz] = voz }
    suspend fun definirVelocidade(v: Float) = contexto.loja.edit { it[Chaves.velocidade] = v }
    suspend fun definirDestacar(d: Boolean) = contexto.loja.edit { it[Chaves.destacar] = d }
}
