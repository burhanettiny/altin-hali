package com.burhanettin.altinhali

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 3.0 ile zemin ve halı yüzeyini gerçek zamanlı ışıkla çizer.
 * Oyun mantığı ve üst katman (çöpler, karakterler, arayüz) [OyunView]'dadır; bu sınıf yalnızca
 * oradan gelen dokuları ve sahne bilgisini kullanır. Başarısız olursa [hata] çağrılır ve oyun 2B'ye döner.
 */
class HaliRenderer(private val ctx: Context, private val hata: (String) -> Unit) : GLSurfaceView.Renderer {

    // --- Ana iş parçacığından gelen sahne bilgisi
    @Volatile var hali = floatArrayOf(0f, 0f, 1f, 1f)
    @Volatile var kaydirX = 0f
    @Volatile var kaydirY = 0f
    @Volatile var isikX = -0.45f
    @Volatile var isikY = -0.5f
    @Volatile var isikZ = 0.74f
    @Volatile var dalga = 0f
    @Volatile var zaman = 0f
    @Volatile var yogunluk = 2f

    private val kilit = Any()
    private var haliBmp: Bitmap? = null
    private var zeminBmp: Bitmap? = null
    private var esyaBmp: Bitmap? = null
    private var haliKirli = false
    private var zeminKirli = false
    private var esyaKirli = false

    private var izW = 1
    private var izH = 1
    private var kir = IntArray(1)
    private var kopuk = IntArray(1)
    private var islak = IntArray(1)
    private var hasar = IntArray(1)
    private var katmanKirli = false

    // --- GL nesneleri
    private var calisiyor = false
    private var haliProg = 0
    private var zeminProg = 0
    private var izgaraVbo = 0
    private var izgaraSayi = 0
    private var dortgenVbo = 0
    private val tx = IntArray(7) // 0 halı, 1 eşya, 2 kir, 3 köpük, 4 ıslak, 5 hasar, 6 zemin
    private var ekranW = 1
    private var ekranH = 1
    private var bayt: ByteBuffer? = null
    private var katmanBoyutu = 0

    // ---------------------------------------------------------------- ana iş parçacığı

    fun haliVer(b: Bitmap) = synchronized(kilit) { haliBmp = b; haliKirli = true }
    fun zeminVer(b: Bitmap) = synchronized(kilit) { zeminBmp = b; zeminKirli = true }
    fun esyaVer(b: Bitmap) = synchronized(kilit) { esyaBmp = b; esyaKirli = true }

    fun katmanlar(w: Int, h: Int, kirPx: IntArray, kopukPx: IntArray, islakPx: IntArray, hasarPx: IntArray) {
        synchronized(kilit) {
            if (kir.size != w * h) {
                kir = IntArray(w * h); kopuk = IntArray(w * h); islak = IntArray(w * h); hasar = IntArray(w * h)
            }
            izW = w
            izH = h
            System.arraycopy(kirPx, 0, kir, 0, w * h)
            System.arraycopy(kopukPx, 0, kopuk, 0, w * h)
            System.arraycopy(islakPx, 0, islak, 0, w * h)
            System.arraycopy(hasarPx, 0, hasar, 0, w * h)
            katmanKirli = true
        }
    }

    // ---------------------------------------------------------------- GL iş parçacığı

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            haliProg = program("hali.vert", "hali.frag")
            zeminProg = program("zemin.vert", "zemin.frag")
            GLES30.glGenTextures(tx.size, tx, 0)
            val bos = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
            for (t in tx) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t)
                dokuAyarla()
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, bos)
            }
            katmanBoyutu = 0
            izgaraSayi = izgaraOlustur(36, 54)
            dortgenVbo = vbo(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f))
            synchronized(kilit) {
                haliKirli = haliBmp != null
                zeminKirli = zeminBmp != null
                esyaKirli = esyaBmp != null
                katmanKirli = true
            }
            calisiyor = true
        } catch (e: Exception) {
            calisiyor = false
            Log.e("AltinHali", "3B başlatılamadı", e)
            hata(e.message ?: "GL hatası")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        ekranW = w
        ekranH = h
        GLES30.glViewport(0, 0, w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClearColor(0.3f, 0.2f, 0.15f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        if (!calisiyor) return
        try {
            yukle()
            zeminCiz()
            haliCiz()
        } catch (e: Exception) {
            calisiyor = false
            Log.e("AltinHali", "3B çizim hatası", e)
            hata(e.message ?: "GL çizim hatası")
        }
    }

    private fun yukle() {
        var hb: Bitmap? = null
        var zb: Bitmap? = null
        var eb: Bitmap? = null
        var katman = false
        synchronized(kilit) {
            if (haliKirli) { hb = haliBmp; haliKirli = false }
            if (zeminKirli) { zb = zeminBmp; zeminKirli = false }
            if (esyaKirli) { eb = esyaBmp; esyaKirli = false }
            if (katmanKirli) {
                katman = true
                katmanKirli = false
                val n = izW * izH
                if (bayt == null || bayt!!.capacity() < n * 4 * 4) {
                    bayt = ByteBuffer.allocateDirect(n * 4 * 4).order(ByteOrder.nativeOrder())
                }
                val b = bayt!!
                b.clear()
                for (dizi in arrayOf(kir, kopuk, islak, hasar)) {
                    for (i in 0 until n) {
                        val c = dizi[i]
                        b.put((c shr 16 and 0xFF).toByte())
                        b.put((c shr 8 and 0xFF).toByte())
                        b.put((c and 0xFF).toByte())
                        b.put((c ushr 24).toByte())
                    }
                }
            }
        }
        hb?.let { bitmapYukle(tx[0], it) }
        zb?.let { bitmapYukle(tx[6], it) }
        eb?.let { bitmapYukle(tx[1], it) }
        if (katman) {
            val n = izW * izH
            val b = bayt!!
            for (k in 0 until 4) {
                b.position(k * n * 4)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tx[2 + k])
                if (katmanBoyutu != n) {
                    GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, izW, izH, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, b)
                } else {
                    GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, izW, izH, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, b)
                }
            }
            katmanBoyutu = n
            b.position(0)
        }
    }

    private fun bitmapYukle(doku: Int, b: Bitmap) {
        if (b.isRecycled) return
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, doku)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, b, 0)
    }

    private fun zeminCiz() {
        GLES30.glUseProgram(zeminProg)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tx[6])
        GLES30.glUniform1i(GLES30.glGetUniformLocation(zeminProg, "uZeminTex"), 0)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(zeminProg, "uEkran"), ekranW.toFloat(), ekranH.toFloat())
        GLES30.glUniform3f(GLES30.glGetUniformLocation(zeminProg, "uIsik"), isikX, isikY, isikZ)
        ciz(dortgenVbo, 6)
    }

    private fun haliCiz() {
        val p = haliProg
        GLES30.glUseProgram(p)
        val adlar = arrayOf("uHaliTex", "uEsyaTex", "uKirTex", "uKopukTex", "uIslakTex", "uHasarTex")
        for (i in adlar.indices) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tx[i])
            GLES30.glUniform1i(GLES30.glGetUniformLocation(p, adlar[i]), i)
        }
        val h = hali
        fun u(ad: String) = GLES30.glGetUniformLocation(p, ad)
        GLES30.glUniform4f(u("uHali"), h[0], h[1], h[2], h[3])
        GLES30.glUniform2f(u("uEkran"), ekranW.toFloat(), ekranH.toFloat())
        GLES30.glUniform2f(u("uKaydir"), kaydirX, kaydirY)
        GLES30.glUniform2f(u("uHaliPx"), h[2] - h[0], h[3] - h[1])
        GLES30.glUniform2f(u("uIzgara"), izW.toFloat(), izH.toFloat())
        GLES30.glUniform3f(u("uIsik"), isikX, isikY, isikZ)
        GLES30.glUniform1f(u("uZaman"), zaman)
        GLES30.glUniform1f(u("uDalga"), dalga)
        GLES30.glUniform1f(u("uYogunluk"), yogunluk)
        ciz(izgaraVbo, izgaraSayi)
    }

    private fun ciz(tampon: Int, sayi: Int) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tampon)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, sayi)
    }

    private fun dokuAyarla() {
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    private fun izgaraOlustur(nx: Int, ny: Int): Int {
        val v = FloatArray(nx * ny * 12)
        var k = 0
        for (j in 0 until ny) for (i in 0 until nx) {
            val u0 = i / nx.toFloat()
            val u1 = (i + 1) / nx.toFloat()
            val v0 = j / ny.toFloat()
            val v1 = (j + 1) / ny.toFloat()
            for (f in floatArrayOf(u0, v0, u1, v0, u0, v1, u1, v0, u1, v1, u0, v1)) v[k++] = f
        }
        izgaraVbo = vbo(v)
        return v.size / 2
    }

    private fun vbo(veri: FloatArray): Int {
        val id = IntArray(1)
        GLES30.glGenBuffers(1, id, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, id[0])
        val fb: FloatBuffer = ByteBuffer.allocateDirect(veri.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        fb.put(veri).position(0)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, veri.size * 4, fb, GLES30.GL_STATIC_DRAW)
        return id[0]
    }

    private fun kaynak(ad: String): String = ctx.assets.open("shaders/$ad").bufferedReader().use { it.readText() }

    private fun golgelendirici(tur: Int, ad: String): Int {
        val s = GLES30.glCreateShader(tur)
        GLES30.glShaderSource(s, kaynak(ad))
        GLES30.glCompileShader(s)
        val durum = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, durum, 0)
        if (durum[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(s)
            GLES30.glDeleteShader(s)
            throw IllegalStateException("$ad: $log")
        }
        return s
    }

    private fun program(vs: String, fs: String): Int {
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, golgelendirici(GLES30.GL_VERTEX_SHADER, vs))
        GLES30.glAttachShader(p, golgelendirici(GLES30.GL_FRAGMENT_SHADER, fs))
        GLES30.glBindAttribLocation(p, 0, "aUV")
        GLES30.glLinkProgram(p)
        val durum = IntArray(1)
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, durum, 0)
        if (durum[0] == 0) throw IllegalStateException("bağlama: " + GLES30.glGetProgramInfoLog(p))
        return p
    }
}
