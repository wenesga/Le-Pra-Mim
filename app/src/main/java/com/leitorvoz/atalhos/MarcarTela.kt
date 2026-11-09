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

/**
 * Camada escura com o retângulo marcado pelo dedo.
 * Depois de marcar, dá para ajustar pelas alças dos cantos e das laterais, ou arrastar o retângulo.
 * Perto da borda da tela, o retângulo encosta nela sozinho, porque começar o gesto na borda é difícil.
 */
@SuppressLint("ViewConstructor")
private class AreaMarcada(contexto: Context) : View(contexto) {
    private val d = contexto.resources.displayMetrics.density
    private var r: RectF? = null
    private var modo = 0 // 0 novo, 1 mover, ou soma de bordas: 2 esquerda, 4 topo, 8 direita, 16 base
    private var ultimo = 0f to 0f
    private val toque = 28 * d
    private val ima = 24 * d
    private val escuro = Paint().apply { color = Color.parseColor("#99000000") }
    private val borda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * d
        color = Color.parseColor("#B9C8FF")
    }
    private val alca = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B9C8FF") }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Pede ao sistema para não tratar o arrasto nas laterais como gesto de voltar.
        systemGestureExclusionRects = listOf(
            android.graphics.Rect(0, 0, (40 * d).toInt(), height),
            android.graphics.Rect(width - (40 * d).toInt(), 0, width, height),
        )
    }

    /** Retângulo em coordenadas da tela, ou null para a tela inteira. */
    fun retangulo(): RectF? {
        val a = r ?: return null
        if (a.width() < 40 || a.height() < 24) return null
        val pos = IntArray(2).also { getLocationOnScreen(it) }
        return RectF(a).apply { offset(pos[0].toFloat(), pos[1].toFloat()) }
    }

    private fun bordasTocadas(x: Float, y: Float, a: RectF): Int {
        if (x < a.left - toque || x > a.right + toque || y < a.top - toque || y > a.bottom + toque) return 0
        var m = 0
        if (kotlin.math.abs(x - a.left) < toque) m = m or 2
        if (kotlin.math.abs(x - a.right) < toque) m = m or 8
        if (kotlin.math.abs(y - a.top) < toque) m = m or 4
        if (kotlin.math.abs(y - a.bottom) < toque) m = m or 16
        if (m == 0 && a.contains(x, y)) m = 1
        return m
    }

    private fun encostar(a: RectF) {
        if (a.left < ima) a.left = 0f
        if (a.top < ima) a.top = 0f
        if (width - a.right < ima) a.right = width.toFloat()
        if (height - a.bottom < ima) a.bottom = height.toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val a = r
                modo = if (a != null && a.width() > 4) bordasTocadas(x, y, a) else 0
                if (modo == 0) r = RectF(x, y, x, y)
                ultimo = x to y
                ancora = x to y
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val a = r ?: return true
                when (modo) {
                    0 -> a.set(min(ancora.first, x), min(ancora.second, y), max(ancora.first, x), max(ancora.second, y))
                    1 -> a.offset(x - ultimo.first, y - ultimo.second)
                    else -> {
                        if (modo and 2 != 0) a.left = min(x, a.right - toque)
                        if (modo and 8 != 0) a.right = max(x, a.left + toque)
                        if (modo and 4 != 0) a.top = min(y, a.bottom - toque)
                        if (modo and 16 != 0) a.bottom = max(y, a.top + toque)
                    }
                }
                encostar(a)
                ultimo = x to y
            }
        }
        invalidate()
        return true
    }

    private var ancora = 0f to 0f

    override fun onDraw(canvas: Canvas) {
        val a = r
        if (a == null || a.width() < 4) { canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), escuro); return }
        canvas.drawRect(0f, 0f, width.toFloat(), a.top, escuro)
        canvas.drawRect(0f, a.bottom, width.toFloat(), height.toFloat(), escuro)
        canvas.drawRect(0f, a.top, a.left, a.bottom, escuro)
        canvas.drawRect(a.right, a.top, width.toFloat(), a.bottom, escuro)
        canvas.drawRoundRect(a, 8 * d, 8 * d, borda)
        val raio = 7 * d
        listOf(a.left to a.top, a.right to a.top, a.left to a.bottom, a.right to a.bottom,
            a.centerX() to a.top, a.centerX() to a.bottom, a.left to a.centerY(), a.right to a.centerY())
            .forEach { (cx, cy) -> canvas.drawCircle(cx, cy, raio, alca) }
    }
}
