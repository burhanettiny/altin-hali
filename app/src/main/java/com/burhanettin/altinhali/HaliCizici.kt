package com.burhanettin.altinhali

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Kirin altındaki halıyı kodla çizer: kilim motifleri, yün dokusu, düğüm sıraları,
 * ışık, yıpranma. Ayrıca odanın parke zeminini çizer.
 */
object HaliCizici {
    // zemin, kenar, vurgu, motif, koyu
    private val paletler = arrayOf(
        intArrayOf(0xFF8E1B2C.toInt(), 0xFF1E3554.toInt(), 0xFFD9A85B.toInt(), 0xFFEFE3CC.toInt(), 0xFF2A1A16.toInt()),
        intArrayOf(0xFF1D466E.toInt(), 0xFF7E1D38.toInt(), 0xFFE0B04A.toInt(), 0xFFEAE2D0.toInt(), 0xFF1A1A24.toInt()),
        intArrayOf(0xFF2B5E47.toInt(), 0xFF70361C.toInt(), 0xFFD8AC46.toInt(), 0xFFF1E6D4.toInt(), 0xFF1C1A14.toInt()),
        intArrayOf(0xFF572A69.toInt(), 0xFF1C5A6E.toInt(), 0xFFE39B1E.toInt(), 0xFFF6EAD2.toInt(), 0xFF1E1420.toInt()),
        intArrayOf(0xFFA94E1C.toInt(), 0xFF283044.toInt(), 0xFFE8D27A.toInt(), 0xFFFBF3E1.toInt(), 0xFF22180F.toInt()),
        intArrayOf(0xFF6B1F1F.toInt(), 0xFF2E4A3A.toInt(), 0xFFCF9F5A.toInt(), 0xFFE9DCC3.toInt(), 0xFF1B1310.toInt()),
    )

    fun ciz(w: Int, h: Int, seviye: Int): Bitmap {
        val palet = paletler[(seviye - 1) % paletler.size]
        val zemin = palet[0]
        val kenar = palet[1]
        val vurgu = palet[2]
        val motif = palet[3]
        val koyu = palet[4]
        val rnd = Random(seviye * 7919 + 13)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val fw = w.toFloat()
        val fh = h.toFloat()
        val b = fw * 0.11f

        // --- Kenar bordürü
        c.drawColor(kenar)
        p.color = koyu
        p.style = Paint.Style.STROKE
        p.strokeWidth = fw * 0.012f
        c.drawRect(b * 0.08f, b * 0.08f, fw - b * 0.08f, fh - b * 0.08f, p)
        p.style = Paint.Style.FILL

        // Bordürde akan su (zikzak) motifi
        p.color = vurgu
        val zz = Path()
        val adim = b * 0.55f
        fun zikzak(x0: Float, y0: Float, x1: Float, y1: Float, yatay: Boolean) {
            zz.reset()
            val uzun = if (yatay) x1 - x0 else y1 - y0
            val n = (uzun / adim).toInt().coerceAtLeast(1)
            val a = uzun / n
            val genlik = b * 0.18f
            for (k in 0..n * 2) {
                val t = k * a / 2
                val off = if (k % 2 == 0) -genlik else genlik
                val px = if (yatay) x0 + t else x0 + off
                val py = if (yatay) y0 + off else y0 + t
                if (k == 0) zz.moveTo(px, py) else zz.lineTo(px, py)
            }
            p.style = Paint.Style.STROKE
            p.strokeWidth = b * 0.12f
            p.strokeJoin = Paint.Join.MITER
            c.drawPath(zz, p)
            p.style = Paint.Style.FILL
        }
        zikzak(b * 0.5f, b * 0.5f, fw - b * 0.5f, 0f, true)
        zikzak(b * 0.5f, fh - b * 0.5f, fw - b * 0.5f, 0f, true)
        zikzak(b * 0.5f, b * 0.5f, 0f, fh - b * 0.5f, false)
        zikzak(fw - b * 0.5f, b * 0.5f, 0f, fh - b * 0.5f, false)
        // Bordür içindeki küçük noktalar
        p.color = motif
        var t = b * 0.5f
        while (t < fw - b * 0.4f) {
            c.drawCircle(t, b * 0.5f, b * 0.05f, p); c.drawCircle(t, fh - b * 0.5f, b * 0.05f, p); t += adim / 2
        }
        t = b * 0.5f
        while (t < fh - b * 0.4f) {
            c.drawCircle(b * 0.5f, t, b * 0.05f, p); c.drawCircle(fw - b * 0.5f, t, b * 0.05f, p); t += adim / 2
        }

        // Koruma şeritleri
        p.color = koyu
        c.drawRect(b * 0.9f, b * 0.9f, fw - b * 0.9f, fh - b * 0.9f, p)
        p.color = vurgu
        c.drawRect(b * 0.95f, b * 0.95f, fw - b * 0.95f, fh - b * 0.95f, p)
        p.color = motif
        c.drawRect(b, b, fw - b, fh - b, p)
        // Ana zemin
        p.color = zemin
        val ic = b * 1.06f
        c.drawRect(ic, ic, fw - ic, fh - ic, p)

        // --- Zemin: göl (sekizgen) motifleri ızgarası
        c.save()
        c.clipRect(ic, ic, fw - ic, fh - ic)
        val gadim = fw * 0.2f
        var sira = 0
        var gy = ic + gadim * 0.55f
        while (gy < fh) {
            var gx = ic + if (sira % 2 == 0) gadim * 0.5f else gadim
            while (gx < fw) {
                gol(c, gx, gy, gadim * 0.2f, koyu, vurgu, motif, p)
                gx += gadim
            }
            gy += gadim * 0.75f
            sira++
        }
        // Köşe madalyonları
        val R = fw * 0.3f
        for ((kx, ky) in listOf(ic to ic, fw - ic to ic, ic to fh - ic, fw - ic to fh - ic)) {
            kancaliElmas(c, kx, ky, R * 0.55f, R * 0.75f, vurgu, p)
            p.color = kenar; elmas(c, kx, ky, R * 0.4f, R * 0.55f, p)
            p.color = motif; elmas(c, kx, ky, R * 0.2f, R * 0.28f, p)
        }
        c.restore()

        // --- Orta madalyon
        val cx = fw / 2
        val cy = fh / 2
        p.color = koyu; elmas(c, cx, cy, R * 1.08f, R * 1.55f, p)
        kancaliElmas(c, cx, cy, R, R * 1.45f, vurgu, p)
        p.color = kenar; elmas(c, cx, cy, R * 0.8f, R * 1.17f, p)
        p.color = motif; elmas(c, cx, cy, R * 0.62f, R * 0.9f, p)
        p.color = zemin; elmas(c, cx, cy, R * 0.46f, R * 0.67f, p)
        p.color = vurgu; yildiz(c, cx, cy, R * 0.34f, p)
        p.color = koyu; yildiz(c, cx, cy, R * 0.2f, p)
        p.color = motif; c.drawCircle(cx, cy, R * 0.07f, p)
        // Madalyonun uç süsleri
        for (s in listOf(-1f, 1f)) {
            p.color = vurgu; elmas(c, cx, cy + s * R * 1.75f, R * 0.18f, R * 0.22f, p)
            p.color = koyu; elmas(c, cx, cy + s * R * 1.75f, R * 0.08f, R * 0.1f, p)
        }

        dokuVer(bmp, w, h, rnd)
        return bmp
    }

    /** Yün dokusu, düğüm sıraları, aşınma, ışık ve kenar gölgesi. */
    private fun dokuVer(bmp: Bitmap, w: Int, h: Int, rnd: Random) {
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val gs = 10
        val kaba = FloatArray((gs + 1) * (gs + 1)) { rnd.nextFloat() }
        val asinma = FloatArray((gs + 1) * (gs + 1)) { if (rnd.nextFloat() < 0.15f) rnd.nextFloat() else 0f }
        var tohum = rnd.nextInt()
        for (y in 0 until h) {
            val fy = y.toFloat() / h
            val gyy = fy * gs
            val iy = gyy.toInt().coerceAtMost(gs - 1)
            val ty = gyy - iy
            for (x in 0 until w) {
                val i = y * w + x
                val fx = x.toFloat() / w
                val gxx = fx * gs
                val ix = gxx.toInt().coerceAtMost(gs - 1)
                val tx = gxx - ix
                val k0 = iy * (gs + 1) + ix
                val dusuk = (kaba[k0] * (1 - tx) + kaba[k0 + 1] * tx) * (1 - ty) +
                    (kaba[k0 + gs + 1] * (1 - tx) + kaba[k0 + gs + 2] * tx) * ty
                val yipran = (asinma[k0] * (1 - tx) + asinma[k0 + 1] * tx) * (1 - ty) +
                    (asinma[k0 + gs + 1] * (1 - tx) + asinma[k0 + gs + 2] * tx) * ty
                // Hızlı karma gürültü
                tohum = tohum * 1103515245 + 12345
                val ince = ((tohum ushr 16) and 0xFF) / 255f
                // Düğüm sıraları: 3 piksellik hücrelerde hafif kabarıklık
                val dugum = if (((x / 3) + (y / 3)) % 2 == 0) 0.035f else -0.035f
                val sira = if (y % 3 == 0) -0.05f else 0f
                val isik = 1.07f - 0.14f * (fx + fy) / 2f
                val dx = fx - 0.5f
                val dy = fy - 0.5f
                val vinyet = 1f - 0.45f * (dx * dx + dy * dy)
                val f = (0.9f + 0.13f * ince + dugum + sira) * (0.93f + 0.14f * dusuk) * isik * vinyet
                val renk = px[i]
                var r = Color.red(renk) * f
                var g = Color.green(renk) * f
                var bb = Color.blue(renk) * f
                if (yipran > 0.05f) {
                    // Yıpranmış yerler: renk solar, alttaki çözgü ipliği görünür
                    val a = (yipran * 0.6f).coerceAtMost(0.4f)
                    r += (205 - r) * a; g += (190 - g) * a; bb += (165 - bb) * a
                }
                px[i] = Color.rgb(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), bb.toInt().coerceIn(0, 255))
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
    }

    /** Odanın parke zemini ve halının gölgesi. */
    fun zeminCiz(w: Int, h: Int, hali: RectF, d: Float): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(42)
        val tonlar = intArrayOf(0xFF6D4C35.toInt(), 0xFF7A5639.toInt(), 0xFF5F4230.toInt(), 0xFF83603F.toInt())
        val gen = 34f * d
        val uzun = 150f * d
        var x = 0f
        var sutun = 0
        while (x < w) {
            var y = if (sutun % 2 == 0) 0f else -uzun / 2
            while (y < h) {
                p.color = tonlar[rnd.nextInt(tonlar.size)]
                c.drawRect(x, y, x + gen, y + uzun, p)
                // Damarlar
                p.color = Color.argb(35, 0, 0, 0)
                p.strokeWidth = 1f
                repeat(4) {
                    val gx = x + 4 * d + rnd.nextFloat() * (gen - 8 * d)
                    c.drawLine(gx, y + 3 * d, gx + (rnd.nextFloat() - 0.5f) * 6 * d, y + uzun - 3 * d, p)
                }
                // Ek yerleri
                p.color = Color.argb(110, 30, 18, 10)
                p.strokeWidth = 1.5f * d
                c.drawLine(x, y, x + gen, y, p)
                y += uzun
            }
            p.color = Color.argb(110, 30, 18, 10)
            p.strokeWidth = 1.5f * d
            c.drawLine(x, 0f, x, h.toFloat(), p)
            x += gen
            sutun++
        }
        // Halının gölgesi
        val g = Paint(Paint.ANTI_ALIAS_FLAG)
        g.color = Color.argb(150, 0, 0, 0)
        g.maskFilter = BlurMaskFilter(14f * d, BlurMaskFilter.Blur.NORMAL)
        c.drawRect(hali.left + 4 * d, hali.top + 8 * d, hali.right + 6 * d, hali.bottom + 12 * d, g)
        // Hafif vinyet
        p.color = Color.argb(70, 0, 0, 0)
        p.strokeWidth = 0f
        p.style = Paint.Style.STROKE
        for (k in 0 until 6) {
            p.strokeWidth = 18f * d
            p.alpha = 30 - k * 4
            c.drawRect(k * 9f * d, k * 9f * d, w - k * 9f * d, h - k * 9f * d, p)
        }
        return bmp
    }

    private val yol = Path()

    private fun elmas(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, p: Paint) {
        yol.reset()
        yol.moveTo(cx, cy - ry)
        yol.lineTo(cx + rx, cy)
        yol.lineTo(cx, cy + ry)
        yol.lineTo(cx - rx, cy)
        yol.close()
        c.drawPath(yol, p)
    }

    /** Kenarlarında kanca uçları olan baklava (Anadolu kilimlerindeki gibi). */
    private fun kancaliElmas(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, renk: Int, p: Paint) {
        p.color = renk
        elmas(c, cx, cy, rx, ry, p)
        val n = 4
        for (k in 1 until n) {
            val f = k / n.toFloat()
            // Dört kenarın her birinde dışa bakan küçük kancalar
            for ((sx, sy) in listOf(1f to -1f, 1f to 1f, -1f to 1f, -1f to -1f)) {
                val px = cx + sx * rx * f
                val py = cy + sy * ry * (1 - f)
                val k2 = rx * 0.12f
                yol.reset()
                yol.moveTo(px, py)
                yol.lineTo(px + sx * k2, py + sy * k2 * 0.3f)
                yol.lineTo(px + sx * k2, py + sy * k2)
                yol.close()
                c.drawPath(yol, p)
            }
        }
    }

    /** Göl motifi: sekizgen içinde çapraz ve noktalar. */
    private fun gol(c: Canvas, cx: Float, cy: Float, r: Float, koyu: Int, vurgu: Int, motif: Int, p: Paint) {
        yol.reset()
        for (k in 0 until 8) {
            val a = Math.PI / 8 + k * Math.PI / 4
            val x = cx + (cos(a) * r * 1.25f).toFloat()
            val y = cy + (sin(a) * r).toFloat()
            if (k == 0) yol.moveTo(x, y) else yol.lineTo(x, y)
        }
        yol.close()
        p.color = koyu
        c.drawPath(yol, p)
        p.color = vurgu
        elmas(c, cx, cy, r * 0.85f, r * 0.7f, p)
        p.color = motif
        c.drawRect(cx - r * 0.12f, cy - r * 0.55f, cx + r * 0.12f, cy + r * 0.55f, p)
        c.drawRect(cx - r * 0.7f, cy - r * 0.1f, cx + r * 0.7f, cy + r * 0.1f, p)
        p.color = koyu
        c.drawCircle(cx, cy, r * 0.12f, p)
    }

    private fun yildiz(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        yol.reset()
        for (i in 0 until 16) {
            val a = Math.PI * i / 8 - Math.PI / 2
            val rr = if (i % 2 == 0) r else r * 0.5f
            val px = cx + (cos(a) * rr).toFloat()
            val py = cy + (sin(a) * rr).toFloat()
            if (i == 0) yol.moveTo(px, py) else yol.lineTo(px, py)
        }
        yol.close()
        c.drawPath(yol, p)
    }
}
