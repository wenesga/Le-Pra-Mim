package com.lepramim.leitura

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Reconhece o texto de fotos e imagens, no aparelho (o mesmo ML Kit do "Marcar na tela"). */
object Imagens {
    private val reconhecedor by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Texto de cada imagem, juntos com uma linha em branco entre páginas; null se nada foi reconhecido. */
    fun texto(contexto: Context, imagens: List<Uri>, pronto: (String?) -> Unit) {
        val partes = mutableListOf<String>()
        fun proxima(i: Int) {
            if (i >= imagens.size) {
                pronto(partes.joinToString("\n\n").trim().ifEmpty { null })
                return
            }
            val imagem = runCatching { InputImage.fromFilePath(contexto, imagens[i]) }.getOrNull()
            if (imagem == null) { proxima(i + 1); return }
            reconhecedor.process(imagem)
                .addOnSuccessListener { partes += it.text.trim(); proxima(i + 1) }
                .addOnFailureListener { proxima(i + 1) }
        }
        proxima(0)
    }
}
