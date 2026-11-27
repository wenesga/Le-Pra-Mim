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

    /**
     * Texto de um PDF. Com texto de verdade, sai direto; PDF de imagem (escaneado) é desenhado página a página e lido
     * pelo ML Kit, o que demora. Chamar fora da thread principal. Null se não abrir ou não achar texto.
     */
    fun pdf(contexto: Context, origem: Uri): String? = textoDoPdf(contexto, origem) ?: pdfPorImagem(contexto, origem)

    /** PDF com texto de verdade: tira o texto direto, na hora. Null se for PDF de imagem (quase sem texto) ou não abrir. */
    private fun textoDoPdf(contexto: Context, origem: Uri): String? = runCatching {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(contexto.applicationContext)
        contexto.contentResolver.openInputStream(origem)!!.use { entrada ->
            com.tom_roush.pdfbox.pdmodel.PDDocument.load(entrada).use { doc ->
                val texto = com.tom_roush.pdfbox.text.PDFTextStripper().getText(doc).trim()
                // Poucas letras por página: é PDF de imagem, que precisa do reconhecimento.
                if (texto.count { !it.isWhitespace() } >= 20 * doc.numberOfPages.coerceAtLeast(1)) texto else null
            }
        }
    }.getOrNull()

    private fun pdfPorImagem(contexto: Context, origem: Uri): String? = runCatching {
        contexto.contentResolver.openFileDescriptor(origem, "r")!!.use { fd ->
            android.graphics.pdf.PdfRenderer(fd).use { leitor ->
                val partes = mutableListOf<String>()
                for (i in 0 until leitor.pageCount) {
                    leitor.openPage(i).use { pagina ->
                        val escala = (1800f / pagina.width).coerceIn(1f, 3f)
                        val imagem = android.graphics.Bitmap.createBitmap(
                            (pagina.width * escala).toInt(), (pagina.height * escala).toInt(), android.graphics.Bitmap.Config.ARGB_8888,
                        )
                        imagem.eraseColor(android.graphics.Color.WHITE)
                        pagina.render(imagem, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val texto = com.google.android.gms.tasks.Tasks.await(reconhecedor.process(InputImage.fromBitmap(imagem, 0))).text.trim()
                        imagem.recycle()
                        if (texto.isNotEmpty()) partes += texto
                    }
                }
                partes.joinToString("\n\n").ifEmpty { null }
            }
        }
    }.getOrNull()
}
