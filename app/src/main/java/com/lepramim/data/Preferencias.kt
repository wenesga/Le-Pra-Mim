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

/** Tons de uma cor num tema: botão (e texto/ícone destacado), texto sobre o botão, fundo suave e texto sobre o suave. */
class Tom(val botao: Long, val noBotao: Long, val suave: Long, val noSuave: Long)

/** Cor de destaque do app (padrão dos apps do Wenes): botão e suave, no claro e no escuro. */
enum class CorDestaque(val nome: String, val claro: Tom, val escuro: Tom) {
    /** Android 12+: cor do papel de parede; os valores são só reserva. */
    AUTOMATICA("Automática", Tom(0xFF50545E, 0xFFFFFFFF, 0xFFE3E5E8, 0xFF1B1C1E), Tom(0xFFC2C5CB, 0xFF1B1C1E, 0xFF3D4148, 0xFFE3E5E8)),
    AZUL("Azul", Tom(0xFF1D56A5, 0xFFFFFFFF, 0xFFD1E2FA, 0xFF0B203D), Tom(0xFF9DC0F1, 0xFF0C1C31, 0xFF243D60, 0xFFD6E3F5)),
    AZUL_ACO("Azul-aço", Tom(0xFF3F6383, 0xFFFFFFFF, 0xFFDEE6ED, 0xFF0B253D), Tom(0xFFB0C8DD, 0xFF0C2031, 0xFF344351, 0xFFD6E7F5)),
    VERDE("Verde", Tom(0xFF257E5E, 0xFFFFFFFF, 0xFFE0F0EA, 0xFF0B3D2A), Tom(0xFF80C6B1, 0xFF0C3126, 0xFF283E37, 0xFFD6F5EC)),
    TURQUESA("Turquesa", Tom(0xFF2D7670, 0xFFFFFFFF, 0xFFDCEAE9, 0xFF0B3D39), Tom(0xFF81CFC9, 0xFF0C312E, 0xFF2A4644, 0xFFD6F5F2)),
    LARANJA("Laranja", Tom(0xFF946E4C, 0xFFFFFFFF, 0xFFF0EAE5, 0xFF3D220B), Tom(0xFFF1C09D, 0xFF311C0C, 0xFF603D24, 0xFFF5E3D6)),
    CINZA("Cinza", Tom(0xFF50545E, 0xFFFFFFFF, 0xFFE3E5E8, 0xFF0B1B3D), Tom(0xFFC2C5CB, 0xFF0C1831, 0xFF3D4148, 0xFFD6E0F5)),
}

/** Fundo do tema escuro (as variações do Ciclo Treino; independe da cor de destaque). */
enum class FundoEscuro(val nome: String) {
    PRETO("Preto"), PETROLEO("Petróleo"), MARINHO("Marinho"),
    VINHO_PROFUNDO("Vinho profundo"), GRAFITE("Grafite"), VERDE_PROFUNDO("Verde profundo"), ROXO_PROFUNDO("Roxo profundo"),
}

/** Tema do app: segue o sistema ou fica fixo. */
enum class TemaApp { SISTEMA, CLARO, ESCURO }

/** Tamanho do texto da tela principal, em sp. */
enum class TamanhoTexto(val sp: Int, val nome: String) {
    PEQUENO(17, "Pequeno"), MEDIO(20, "Médio"), GRANDE(24, "Grande"), MAIOR(28, "Maior")
}

/** Fonte do texto da tela principal. As de dislexia têm letras mais pesadas embaixo; as outras são só legíveis. */
enum class FonteTexto(val nome: String, val dislexia: Boolean) {
    PADRAO("Padrão", false), ATKINSON("Atkinson", false), LEXEND("Lexend", false), ANDIKA("Andika", false),
    COMICNEUE("Comic Neue", false), MULISH("Mulish", false), LATO("Lato", false), FIRASANS("Fira Sans", false),
    NOTOSANS("Noto Sans", false), PTSANS("PT Sans", false),
    OPENDYSLEXIC3("OpenDyslexic 3", true), OPENDYSLEXIC("OpenDyslexic", true), OPENDYSLEXICALTA("OpenDyslexic Alta", true),
    OPENDYSLEXICMONO("OpenDyslexic Mono", true), OPENDYSLEXIC092("OpenDyslexic 0.92", true), OPENDYSLEXIC091("OpenDyslexic 0.91", true)
}

private fun mesAtual() = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.ROOT).format(java.util.Date())

/** Com que frequência o backup automático grava a cópia. */
enum class FrequenciaBackup(val nome: String, val periodoMs: Long) {
    DESLIGADO("Desligado", 0L),
    DIARIO("Diário", 24L * 60 * 60 * 1000),
    SEMANAL("Semanal", 7L * 24 * 60 * 60 * 1000),
}

/** Escolhas de voz. Motor vazio = motor padrão do Android. */
data class Preferencias(
    val motor: String = "",
    val voz: String = "",
    val velocidade: Float = 1f,
    val destacar: Boolean = true,
    val pastaBackup: String = "",
    val frequenciaBackup: FrequenciaBackup = FrequenciaBackup.DESLIGADO,
    val ultimoBackup: Long = 0L,
    val regiao: String = "canadacentral",
    val chave: String = "",
    val chaveEm: Long = 0L,
    val vozAzure: String = "pt-BR-FranciscaNeural",
    val favoritas: List<String> = emptyList(),
    val caracteres: Long = 0L,
    val mes: String = "",
    val cor: CorDestaque = CorDestaque.VERDE,
    val tema: TemaApp = TemaApp.SISTEMA,
    val fundo: FundoEscuro = FundoEscuro.MARINHO,
    val markdown: Boolean = true,
    val tamanho: TamanhoTexto = TamanhoTexto.MEDIO,
    val fonte: FonteTexto = FonteTexto.PADRAO,
    val filtros: List<String> = emptyList(),
    val monApp: String = "",
    val monTenant: String = "",
    val monSegredo: String = "",
    val monAssinatura: String = "",
    val monRecurso: String = "lepramim-fala",
    val portalCaracteres: Long = 0L,
    val portalMes: String = "",
) {
    /** Credenciais do Azure Monitor preenchidas: dá para ler o consumo do portal. */
    val monitorPronto get() = monApp.isNotBlank() && monTenant.isNotBlank() && monSegredo.isNotBlank() && monRecurso.isNotBlank()

    /** Total do portal neste mês, ou null se ainda não foi lido. */
    val portalNoMes get() = if (portalMes == mesAtual()) portalCaracteres else null

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
    val pastaBackup = stringPreferencesKey("pasta_backup")
    val frequenciaBackup = stringPreferencesKey("frequencia_backup")
    val ultimoBackup = longPreferencesKey("ultimo_backup")
    val regiao = stringPreferencesKey("regiao")
    val chave = stringPreferencesKey("chave")
    val chaveEm = longPreferencesKey("chave_em")
    val vozAzure = stringPreferencesKey("voz_azure")
    val favoritas = stringPreferencesKey("favoritas")
    val caracteres = longPreferencesKey("caracteres")
    val mes = stringPreferencesKey("mes")
    val cor = stringPreferencesKey("cor")
    val tema = stringPreferencesKey("tema")
    val fundo = stringPreferencesKey("fundo")
    val markdown = booleanPreferencesKey("markdown")
    val tamanho = stringPreferencesKey("tamanho")
    val fonte = stringPreferencesKey("fonte")
    val filtros = stringPreferencesKey("filtros")
    val monApp = stringPreferencesKey("mon_app")
    val monTenant = stringPreferencesKey("mon_tenant")
    val monSegredo = stringPreferencesKey("mon_segredo")
    val monAssinatura = stringPreferencesKey("mon_assinatura")
    val monRecurso = stringPreferencesKey("mon_recurso")
    val portalCaracteres = longPreferencesKey("portal_caracteres")
    val portalMes = stringPreferencesKey("portal_mes")
}

class Repositorio(private val contexto: Context) {
    val preferencias: Flow<Preferencias> = contexto.loja.data.map {
        Preferencias(
            motor = it[Chaves.motor] ?: "",
            voz = it[Chaves.voz] ?: "",
            velocidade = it[Chaves.velocidade] ?: 1f,
            destacar = it[Chaves.destacar] ?: true,
            pastaBackup = it[Chaves.pastaBackup] ?: "",
            frequenciaBackup = runCatching { FrequenciaBackup.valueOf(it[Chaves.frequenciaBackup] ?: "") }.getOrDefault(FrequenciaBackup.DESLIGADO),
            ultimoBackup = it[Chaves.ultimoBackup] ?: 0L,
            regiao = it[Chaves.regiao] ?: "canadacentral",
            chave = it[Chaves.chave] ?: "",
            chaveEm = it[Chaves.chaveEm] ?: 0L,
            vozAzure = it[Chaves.vozAzure] ?: "pt-BR-FranciscaNeural",
            favoritas = it[Chaves.favoritas].orEmpty().split('\n').filter { f -> f.isNotEmpty() },
            caracteres = it[Chaves.caracteres] ?: 0L,
            mes = it[Chaves.mes] ?: "",
            cor = runCatching { CorDestaque.valueOf(it[Chaves.cor] ?: "") }.getOrDefault(CorDestaque.VERDE),
            tema = runCatching { TemaApp.valueOf(it[Chaves.tema] ?: "") }.getOrDefault(if (it[Chaves.tema] == "PRETO") TemaApp.ESCURO else TemaApp.SISTEMA),
            fundo = runCatching { FundoEscuro.valueOf(it[Chaves.fundo] ?: "") }.getOrDefault(FundoEscuro.MARINHO),
            markdown = it[Chaves.markdown] ?: true,
            tamanho = runCatching { TamanhoTexto.valueOf(it[Chaves.tamanho] ?: "") }.getOrDefault(TamanhoTexto.MEDIO),
            fonte = runCatching { FonteTexto.valueOf(it[Chaves.fonte] ?: "") }.getOrDefault(FonteTexto.PADRAO),
            filtros = it[Chaves.filtros].orEmpty().split('\n').filter { f -> f.isNotEmpty() },
            monApp = it[Chaves.monApp] ?: "",
            monTenant = it[Chaves.monTenant] ?: "",
            monSegredo = it[Chaves.monSegredo] ?: "",
            monAssinatura = it[Chaves.monAssinatura] ?: "",
            monRecurso = it[Chaves.monRecurso] ?: "lepramim-fala",
            portalCaracteres = it[Chaves.portalCaracteres] ?: 0L,
            portalMes = it[Chaves.portalMes] ?: "",
        )
    }

    suspend fun definirMotor(motor: String) = contexto.loja.edit { it[Chaves.motor] = motor; it[Chaves.voz] = "" }
    suspend fun definirVoz(voz: String) = contexto.loja.edit { it[Chaves.voz] = voz }
    suspend fun definirVelocidade(v: Float) = contexto.loja.edit { it[Chaves.velocidade] = v }
    suspend fun definirDestacar(d: Boolean) = contexto.loja.edit { it[Chaves.destacar] = d }
    suspend fun definirPastaBackup(p: String) = contexto.loja.edit { it[Chaves.pastaBackup] = p }
    suspend fun definirFrequenciaBackup(f: FrequenciaBackup) = contexto.loja.edit { it[Chaves.frequenciaBackup] = f.name }
    suspend fun definirUltimoBackup(quando: Long) = contexto.loja.edit { it[Chaves.ultimoBackup] = quando }
    suspend fun definirCor(c: CorDestaque) = contexto.loja.edit { it[Chaves.cor] = c.name }
    suspend fun definirFundo(f: FundoEscuro) = contexto.loja.edit { it[Chaves.fundo] = f.name }
    suspend fun definirTema(t: TemaApp) = contexto.loja.edit { it[Chaves.tema] = t.name }
    suspend fun definirTamanho(t: TamanhoTexto) = contexto.loja.edit { it[Chaves.tamanho] = t.name }
    suspend fun definirFonte(f: FonteTexto) = contexto.loja.edit { it[Chaves.fonte] = f.name }
    suspend fun definirMarkdown(m: Boolean) = contexto.loja.edit { it[Chaves.markdown] = m }
    suspend fun definirFiltros(f: List<String>) = contexto.loja.edit { it[Chaves.filtros] = f.joinToString("\n") }
    suspend fun definirRegiao(r: String) = contexto.loja.edit { it[Chaves.regiao] = r.trim().lowercase() }
    suspend fun definirChave(c: String) = contexto.loja.edit {
        it[Chaves.chave] = c.trim()
        it[Chaves.chaveEm] = if (c.isBlank()) 0L else System.currentTimeMillis()
    }
    suspend fun definirMonApp(v: String) = contexto.loja.edit { it[Chaves.monApp] = v.trim() }
    suspend fun definirMonTenant(v: String) = contexto.loja.edit { it[Chaves.monTenant] = v.trim() }
    suspend fun definirMonSegredo(v: String) = contexto.loja.edit { it[Chaves.monSegredo] = v.trim() }
    suspend fun definirMonAssinatura(v: String) = contexto.loja.edit { it[Chaves.monAssinatura] = v.trim() }
    suspend fun definirMonRecurso(v: String) = contexto.loja.edit { it[Chaves.monRecurso] = v.trim() }
    suspend fun guardarPortal(total: Long, assinatura: String) = contexto.loja.edit {
        it[Chaves.portalCaracteres] = total
        it[Chaves.portalMes] = mesAtual()
        if (it[Chaves.monAssinatura].isNullOrBlank()) it[Chaves.monAssinatura] = assinatura
    }
    suspend fun limparMonitor() = contexto.loja.edit {
        it[Chaves.monApp] = ""; it[Chaves.monTenant] = ""; it[Chaves.monSegredo] = ""
        it[Chaves.monAssinatura] = ""; it[Chaves.portalCaracteres] = 0L; it[Chaves.portalMes] = ""
    }
    suspend fun definirVozAzure(v: String) = contexto.loja.edit { it[Chaves.vozAzure] = v }
    suspend fun definirFavoritas(f: List<String>) = contexto.loja.edit { it[Chaves.favoritas] = f.joinToString("\n") }

    /** Soma os caracteres enviados à Azure no mês (zera ao virar o mês). */
    suspend fun somarCaracteres(n: Int) = contexto.loja.edit {
        val mes = mesAtual()
        if (it[Chaves.mes] != mes) { it[Chaves.mes] = mes; it[Chaves.caracteres] = 0L }
        it[Chaves.caracteres] = (it[Chaves.caracteres] ?: 0L) + n
    }
}
