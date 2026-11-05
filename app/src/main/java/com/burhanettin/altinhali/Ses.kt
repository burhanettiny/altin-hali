package com.burhanettin.altinhali

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Oyunun ses efektleri ve titreşimi. Sesler ses dosyası yerine kodla üretilir (sentezlenir),
 * ilk açılışta önbelleğe WAV olarak yazılıp SoundPool'a yüklenir.
 */
object Ses {
    private const val HZ = 22050

    private var havuz: SoundPool? = null
    private val idler = HashMap<String, Int>()
    private val hazir = HashSet<Int>()
    private var acik = true
    private var titresim = true
    private var vib: Vibrator? = null

    fun baslat(ctx: Context) {
        val k = Kayit(ctx)
        acik = k.sesAcik
        titresim = k.titresimAcik
        if (havuz != null) return
        val app = ctx.applicationContext
        @Suppress("DEPRECATION")
        vib = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val h = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        h.setOnLoadCompleteListener { _, id, durum -> if (durum == 0) synchronized(hazir) { hazir.add(id) } }
        havuz = h
        Thread {
            try {
                val dizin = File(app.cacheDir, "ses_v2")
                dizin.mkdirs()
                for ((ad, ornek) in uret()) {
                    val f = File(dizin, "$ad.wav")
                    if (!f.exists()) wavYaz(f, ornek)
                    val id = h.load(f.path, 1)
                    synchronized(idler) { idler[ad] = id }
                }
            } catch (_: Exception) {
                // Ses üretilemezse oyun sessiz devam eder.
            }
        }.start()
    }

    fun ayarla(ses: Boolean, tit: Boolean) {
        acik = ses
        titresim = tit
    }

    fun cal(ad: String, hacim: Float = 1f, hiz: Float = 1f) {
        if (!acik) return
        val h = havuz ?: return
        val id = synchronized(idler) { idler[ad] } ?: return
        if (synchronized(hazir) { id !in hazir }) return
        val v = hacim.coerceIn(0f, 1f)
        h.play(id, v, v, 1, 0, hiz.coerceIn(0.5f, 2f))
    }

    fun titret(ms: Long) {
        if (!titresim) return
        val v = vib ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------ sentez

    private fun uret(): List<Pair<String, ShortArray>> {
        val r = Random(7)
        return listOf(
            "para" to ses(0.32f) { nota(it, 0f, 0.08f, 1568f, 0.5f); nota(it, 0.06f, 0.25f, 2093f, 0.5f) },
            "kese" to ses(0.45f) {
                floatArrayOf(1760f, 2349f, 1976f, 2637f, 2093f).forEachIndexed { i, f -> nota(it, i * 0.045f, 0.16f, f, 0.32f) }
            },
            "elmas" to ses(0.75f) {
                floatArrayOf(1047f, 1319f, 1568f, 2093f, 2637f).forEachIndexed { i, f -> nota(it, i * 0.06f, 0.4f, f, 0.32f) }
            },
            "guc" to ses(0.55f) { suzul(it, 0f, 0.32f, 500f, 1500f, 0.45f); nota(it, 0.28f, 0.25f, 1568f, 0.4f) },
            "bomba" to ses(0.7f) { gurultu(it, 0f, 0.65f, 0.9f, 0.88f, r); suzul(it, 0f, 0.45f, 150f, 40f, 0.8f) },
            "hayalet" to ses(0.7f) { suzul(it, 0f, 0.65f, 700f, 200f, 0.45f, vibrato = 9f) },
            "ovala" to ses(0.1f) { gurultu(it, 0f, 0.09f, 0.6f, 0.55f, r) },
            "kazan" to ses(1.0f) {
                floatArrayOf(523f, 659f, 784f).forEachIndexed { i, f -> nota(it, i * 0.12f, 0.2f, f, 0.45f) }
                nota(it, 0.36f, 0.6f, 1047f, 0.5f)
                nota(it, 0.36f, 0.6f, 784f, 0.25f)
            },
            "kaybet" to ses(1.1f) {
                floatArrayOf(784f, 659f, 523f).forEachIndexed { i, f -> nota(it, i * 0.16f, 0.25f, f, 0.45f) }
                nota(it, 0.48f, 0.6f, 392f, 0.5f)
            },
            "tik" to ses(0.06f) { nota(it, 0f, 0.04f, 2000f, 0.4f, harmonik = false) },
            "dugme" to ses(0.09f) { nota(it, 0f, 0.07f, 880f, 0.35f) },
            "yildiz" to ses(0.45f) { nota(it, 0f, 0.35f, 1319f, 0.4f); nota(it, 0.05f, 0.35f, 1976f, 0.3f) },
            "kombo" to ses(0.2f) { suzul(it, 0f, 0.16f, 800f, 1600f, 0.4f) },
            "basarim" to ses(0.7f) {
                floatArrayOf(784f, 988f, 1175f, 1568f).forEachIndexed { i, f -> nota(it, i * 0.08f, 0.35f, f, 0.38f) }
            },
            "satin" to ses(0.35f) { nota(it, 0f, 0.1f, 1175f, 0.4f); nota(it, 0.08f, 0.25f, 1760f, 0.45f) },
            "cop" to ses(0.4f) { suzul(it, 0f, 0.12f, 300f, 120f, 0.7f); gurultu(it, 0.05f, 0.25f, 0.5f, 0.6f, r) },
            "cimbiz" to ses(0.08f) { nota(it, 0f, 0.05f, 2600f, 0.3f, harmonik = false) },
            "su" to ses(0.18f) { gurultu(it, 0f, 0.17f, 0.45f, 0.2f, r) },
            "supurge" to ses(0.15f) { gurultu(it, 0f, 0.15f, 0.35f, 0.75f, r); nota(it, 0f, 0.15f, 220f, 0.15f) },
            "buz" to ses(0.15f) {
                for (k in 0 until 5) nota(it, k * 0.025f, 0.03f, 3000f + k * 300f, 0.25f, harmonik = false)
            },
            "silkele" to ses(0.4f) { gurultu(it, 0f, 0.35f, 0.9f, 0.9f, r); suzul(it, 0f, 0.2f, 120f, 60f, 0.6f) },
            "alet" to ses(0.1f) { nota(it, 0f, 0.05f, 660f, 0.3f); nota(it, 0.04f, 0.05f, 990f, 0.3f) },
            "semi" to ses(0.5f) { suzul(it, 0f, 0.2f, 500f, 900f, 0.35f); suzul(it, 0.22f, 0.25f, 900f, 400f, 0.35f) },
            "nils" to ses(0.4f) { nota(it, 0f, 0.12f, 1319f, 0.3f); nota(it, 0.13f, 0.12f, 1047f, 0.3f); nota(it, 0.26f, 0.12f, 1319f, 0.3f) },
        )
    }

    private fun ses(sure: Float, yap: (FloatArray) -> Unit): ShortArray {
        val b = FloatArray((sure * HZ).toInt())
        yap(b)
        var tepe = 0f
        for (v in b) tepe = maxOf(tepe, abs(v))
        val olcek = if (tepe > 0.9f) 0.9f / tepe else 1f
        return ShortArray(b.size) { (b[it] * olcek * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun nota(b: FloatArray, bas: Float, sure: Float, f: Float, hacim: Float, harmonik: Boolean = true) {
        val s0 = (bas * HZ).toInt()
        val n = (sure * HZ).toInt()
        var faz = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= b.size) break
            val t = i.toFloat() / HZ
            val zarf = min(1f, t / 0.004f) * exp(-t * 5f / sure)
            faz += 2 * PI * f / HZ
            var v = sin(faz).toFloat()
            if (harmonik) v = v * 0.75f + 0.25f * sin(2 * faz).toFloat()
            b[idx] += v * zarf * hacim
        }
    }

    private fun suzul(b: FloatArray, bas: Float, sure: Float, f0: Float, f1: Float, hacim: Float, vibrato: Float = 0f) {
        val s0 = (bas * HZ).toInt()
        val n = (sure * HZ).toInt()
        var faz = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= b.size) break
            val t = i.toFloat() / HZ
            val oran = i.toFloat() / n
            var f = f0 + (f1 - f0) * oran
            if (vibrato > 0f) f *= 1f + 0.06f * sin(2 * PI * vibrato * t).toFloat()
            val zarf = min(1f, t / 0.01f) * (1f - oran)
            faz += 2 * PI * f / HZ
            b[idx] += sin(faz).toFloat() * zarf * hacim
        }
    }

    private fun gurultu(b: FloatArray, bas: Float, sure: Float, hacim: Float, yumusak: Float, r: Random) {
        val s0 = (bas * HZ).toInt()
        val n = (sure * HZ).toInt()
        var y = 0f
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= b.size) break
            val t = i.toFloat() / HZ
            val zarf = min(1f, t / 0.005f) * exp(-t * 5f / sure)
            val x = r.nextFloat() * 2f - 1f
            y += (1f - yumusak) * (x - y)
            b[idx] += y * zarf * hacim * 2f
        }
    }

    private fun wavYaz(f: File, ornek: ShortArray) {
        val veri = ornek.size * 2
        val bb = ByteBuffer.allocate(44 + veri).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()).putInt(36 + veri).put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(HZ).putInt(HZ * 2).putShort(2).putShort(16)
        bb.put("data".toByteArray()).putInt(veri)
        for (s in ornek) bb.putShort(s)
        FileOutputStream(f).use { it.write(bb.array()) }
    }
}
