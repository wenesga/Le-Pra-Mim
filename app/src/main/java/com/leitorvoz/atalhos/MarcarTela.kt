package com.leitorvoz.atalhos

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.leitorvoz.leitura.Leitura
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Botão de acessibilidade: abre uma camada onde se marca um trecho da tela.
 * Ao tocar em Ler, tira um print, recorta o trecho e reconhece o texto (OCR no aparelho).
 * Um toque sem arrastar vale para a tela inteira.
 */
class MarcarTela : AccessibilityService() {
    private var camada: View? = null
    private val janelas by lazy { getSystemService(WindowManager::class.java) }
    private val reconhecedor by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val retorno = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) = alternar()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityButtonController.registerAccessibilityButtonCallback(retorno)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(retorno)
        fechar()
        super.onDestroy()
    }

    private fun alternar() {
        if (camada != null) { fechar(); return }
        val area = AreaMarcada(this)
        val raiz = FrameLayout(this).apply {
            addView(area, FrameLayout.LayoutParams(-1, -1))
            addView(barra(area), FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(96)
            })
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS }
        janelas.addView(raiz, p)
        camada = raiz
    }

    private fun fechar() {
        camada?.let { runCatching { janelas.removeView(it) } }
        camada = null
    }

    private fun barra(area: AreaMarcada) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(botao("Cancelar", preenchido = false) { fechar() })
        addView(View(context), LinearLayout.LayoutParams(dp(12), 1))
        addView(botao("Ler", preenchido = true) {
            val r = area.retangulo()
            fechar()
            // Espera a camada sumir antes do print.
            Handler(Looper.getMainLooper()).postDelayed({ capturar(r) }, 250)
        })
    }

    private fun botao(texto: String, preenchido: Boolean, aoTocar: () -> Unit) = TextView(this).apply {
        text = texto
        textSize = 15f
        gravity = Gravity.CENTER
        minHeight = dp(48)
        minWidth = dp(112)
        setPadding(dp(24), 0, dp(24), 0)
        setTextColor(if (preenchido) Color.parseColor("#0F1D4A") else Color.WHITE)
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            if (preenchido) setColor(Color.parseColor("#B9C8FF"))
            else { setColor(Color.parseColor("#CC1F232A")); setStroke(dp(1), Color.parseColor("#8E9099")) }
        }
        setOnClickListener { aoTocar() }
    }

    private fun capturar(r: RectF?) {
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(resultado: ScreenshotResult) {
                val buffer = resultado.hardwareBuffer
                val inteira = Bitmap.wrapHardwareBuffer(buffer, resultado.colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                buffer.close()
                if (inteira == null) { avisar("Não foi possível ler a tela"); return }
                val recorte = if (r == null) inteira else {
                    val x = r.left.roundToInt().coerceIn(0, inteira.width - 1)
                    val y = r.top.roundToInt().coerceIn(0, inteira.height - 1)
                    val w = r.width().roundToInt().coerceIn(1, inteira.width - x)
                    val h = r.height().roundToInt().coerceIn(1, inteira.height - y)
                    Bitmap.createBitmap(inteira, x, y, w, h)
                }
                reconhecer(recorte)
            }

            override fun onFailure(codigo: Int) = avisar("Não foi possível ler a tela")
        })
    }

    private fun reconhecer(imagem: Bitmap) {
        reconhecedor.process(InputImage.fromBitmap(imagem, 0))
            .addOnSuccessListener { t ->
                // Junta as linhas de cada bloco numa frase só, para a leitura não picotar.
                val texto = t.textBlocks.joinToString("\n") { b -> b.lines.joinToString(" ") { it.text } }
                if (texto.isBlank()) avisar("Nenhum texto encontrado") else Leitura.ler(this, texto)
            }
            .addOnFailureListener { avisar("Não foi possível reconhecer o texto") }
    }

    private fun avisar(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}

/** Camada escura com o retângulo marcado pelo dedo. */
@SuppressLint("ViewConstructor")
private class AreaMarcada(contexto: Context) : View(contexto) {
    private var ini: Pair<Float, Float>? = null
    private var fim: Pair<Float, Float>? = null
    private val escuro = Paint().apply { color = Color.parseColor("#99000000") }
    private val borda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * contexto.resources.displayMetrics.density
        color = Color.parseColor("#B9C8FF")
    }

    /** Retângulo em coordenadas da tela, ou null para a tela inteira. */
    fun retangulo(): RectF? {
        val r = local() ?: return null
        if (r.width() < 40 || r.height() < 24) return null
        val pos = IntArray(2).also { getLocationOnScreen(it) }
        r.offset(pos[0].toFloat(), pos[1].toFloat())
        return r
    }

    private fun local(): RectF? {
        val a = ini ?: return null
        val b = fim ?: return null
        return RectF(min(a.first, b.first), min(a.second, b.second), max(a.first, b.first), max(a.second, b.second))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { ini = e.x to e.y; fim = ini }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> fim = e.x to e.y
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val r = local()
        if (r == null || r.width() < 4) { canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), escuro); return }
        canvas.drawRect(0f, 0f, width.toFloat(), r.top, escuro)
        canvas.drawRect(0f, r.bottom, width.toFloat(), height.toFloat(), escuro)
        canvas.drawRect(0f, r.top, r.left, r.bottom, escuro)
        canvas.drawRect(r.right, r.top, width.toFloat(), r.bottom, escuro)
        val raio = 8 * resources.displayMetrics.density
        canvas.drawRoundRect(r, raio, raio, borda)
    }
}
