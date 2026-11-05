package com.burhanettin.altinhali

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/** Kirin altındaki desenli halıyı kodla çizer; her bölümde renkler değişir. */
object HaliCizici {
    // zemin, kenar, vurgu, motif
    private val paletler = arrayOf(
        intArrayOf(0xFF9B1B30.toInt(), 0xFF1F3A5F.toInt(), 0xFFE8C170.toInt(), 0xFFF4E9D8.toInt()),
        intArrayOf(0xFF1F4E79.toInt(), 0xFF8B1E3F.toInt(), 0xFFF2C14E.toInt(), 0xFFEDE6D6.toInt()),
        intArrayOf(0xFF2E6B4F.toInt(), 0xFF7A3B1D.toInt(), 0xFFE9B949.toInt(), 0xFFF5EBDD.toInt()),
        intArrayOf(0xFF5B2A6E.toInt(), 0xFF1E5F74.toInt(), 0xFFF0A202.toInt(), 0xFFFCEFD4.toInt()),
        intArrayOf(0xFFB5541C.toInt(), 0xFF2B2D42.toInt(), 0xFFF6E27F.toInt(), 0xFFFFF8E7.toInt()),
    )

    // Kir renkleri: çamur, toz, is, yosun
    private val kirRenkleri = intArrayOf(
        0xFF6D4C2F.toInt(), 0xFF8D8270.toInt(), 0xFF3B3632.toInt(), 0xFF5E6B3A.toInt(),
    )

    fun kirRengi(seviye: Int): Int = kirRenkleri[(seviye - 1) % kirRenkleri.size]

    fun ciz(w: Int, h: Int, seviye: Int): Bitmap {
        val palet = paletler[(seviye - 1) % paletler.size]
        val zemin = palet[0]
        val kenar = palet[1]
        val vurgu = palet[2]
        val motif = palet[3]

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val fw = w.toFloat()
        val fh = h.toFloat()
        val b = fw * 0.09f

        c.drawColor(kenar)
        p.color = zemin
        c.drawRect(b, b, fw - b, fh - b, p)

        // Kenar çizgileri
        p.style = Paint.Style.STROKE
        p.strokeWidth = fw * 0.012f
        p.color = vurgu
        c.drawRect(b * 0.2f, b * 0.2f, fw - b * 0.2f, fh - b * 0.2f, p)
        c.drawRect(b * 0.92f, b * 0.92f, fw - b * 0.92f, fh - b * 0.92f, p)
        p.strokeWidth = fw * 0.005f
        p.color = motif
        c.drawRect(b * 1.15f, b * 1.15f, fw - b * 1.15f, fh - b * 1.15f, p)
        p.style = Paint.Style.FILL

        // Kenar bandındaki küçük baklavalar
        val adim = b * 0.8f
        val s = b * 0.2f
        var x = b * 0.9f
        while (x < fw - b * 0.6f) {
            p.color = if (((x / adim).toInt()) % 2 == 0) vurgu else motif
            elmas(c, x, b * 0.56f, s, s * 1.3f, p)
            elmas(c, x, fh - b * 0.56f, s, s * 1.3f, p)
            x += adim
        }
        var y = b * 0.9f
        while (y < fh - b * 0.6f) {
            p.color = if (((y / adim).toInt()) % 2 == 0) vurgu else motif
            elmas(c, b * 0.56f, y, s * 1.3f, s, p)
            elmas(c, fw - b * 0.56f, y, s * 1.3f, s, p)
            y += adim
        }

        // Zemin motifleri
        c.save()
        c.clipRect(b * 1.2f, b * 1.2f, fw - b * 1.2f, fh - b * 1.2f)
        val gadim = fw * 0.1f
        p.color = Color.argb(90, Color.red(motif), Color.green(motif), Color.blue(motif))
        var sira = 0
        y = b * 1.2f + gadim * 0.5f
        while (y < fh) {
            x = b * 1.2f + if (sira % 2 == 0) gadim * 0.5f else gadim
            while (x < fw) {
                elmas(c, x, y, gadim * 0.14f, gadim * 0.2f, p)
                x += gadim
            }
            y += gadim * 0.7f
            sira++
        }

        // Köşe yarım madalyonları
        val R = fw * 0.3f
        for ((kx, ky) in listOf(b to b, fw - b to b, b to fh - b, fw - b to fh - b)) {
            p.color = vurgu; elmas(c, kx, ky, R * 0.55f, R * 0.7f, p)
            p.color = kenar; elmas(c, kx, ky, R * 0.4f, R * 0.52f, p)
            p.color = motif; elmas(c, kx, ky, R * 0.22f, R * 0.3f, p)
        }
        c.restore()

        // Orta madalyon
        val cx = fw / 2
        val cy = fh / 2
        p.color = vurgu; elmas(c, cx, cy, R, R * 1.45f, p)
        p.color = kenar; elmas(c, cx, cy, R * 0.82f, R * 1.2f, p)
        p.color = motif; elmas(c, cx, cy, R * 0.62f, R * 0.92f, p)
        p.color = zemin; elmas(c, cx, cy, R * 0.45f, R * 0.68f, p)
        p.color = vurgu; yildiz(c, cx, cy, R * 0.34f, p)
        p.color = kenar; c.drawCircle(cx, cy, R * 0.12f, p)
        p.color = motif; c.drawCircle(cx, cy, R * 0.05f, p)

        // Dokuma hissi: ince yatay çizgiler
        p.color = Color.argb(22, 0, 0, 0)
        p.strokeWidth = 1f
        var dy = 0f
        while (dy < fh) {
            c.drawLine(0f, dy, fw, dy, p)
            dy += 3f
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
