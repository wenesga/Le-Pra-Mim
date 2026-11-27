package com.lepramim.leitura

import android.content.Context
import android.net.Uri

/** Lê o texto de arquivos TXT e Word (.docx), usado ao importar e ao receber um arquivo compartilhado. */
object Arquivos {
    /** Texto do arquivo, ou null se não der para abrir. UTF-8 e, se não for, Windows-1252. */
    fun texto(contexto: Context, origem: Uri): String? = runCatching {
        val bytes = contexto.contentResolver.openInputStream(origem)!!.use { it.readBytes() }
        // .docx é um zip (começa com "PK"); o texto fica em word/document.xml.
        if (bytes.size > 3 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) return@runCatching doDocx(bytes)
        val utf8 = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        runCatching { utf8.decode(java.nio.ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { String(bytes, charset("windows-1252")) }
            .removePrefix("﻿").trim()
    }.getOrNull()

    /** Texto corrido de um .docx: um parágrafo por linha em branco, sem formatação. */
    private fun doDocx(bytes: ByteArray): String {
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.name != "word/document.xml") continue
                val p = android.util.Xml.newPullParser()
                p.setInput(zip, "UTF-8")
                val sb = StringBuilder()
                var dentro = false
                var ev = p.eventType
                while (ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    when (ev) {
                        org.xmlpull.v1.XmlPullParser.START_TAG -> when (p.name) {
                            "t" -> dentro = true
                            "tab" -> sb.append(' ')
                            "br" -> sb.append('\n')
                        }
                        org.xmlpull.v1.XmlPullParser.TEXT -> if (dentro) sb.append(p.text)
                        org.xmlpull.v1.XmlPullParser.END_TAG -> when (p.name) {
                            "t" -> dentro = false
                            "p" -> sb.append("\n\n")
                        }
                    }
                    ev = p.next()
                }
                return sb.toString().replace(Regex("\n{3,}"), "\n\n").trim()
            }
        }
        error("sem texto")
    }
}
