package com.burhanettin.altinhali

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class Sonuc(
    val kazandi: Boolean,
    val sureli: Boolean,
    val seviye: Int,
    val toplanan: Int,
    val bonus: Int,
    val yildiz: Int,
    val gecenSure: Float,
)

class OyunView(
    context: Context,
    private val sureli: Boolean,
    private val bitince: (Sonuc) -> Unit,
) : View(context) {

    private enum class Durum { HAZIR, OYNUYOR, KUTLAMA, BITTI }

    private val kayit = Kayit(context)
    private val seviye = kayit.seviye
    private val deterjan = Katalog.deterjan(kayit.deterjan)
    private val karakter = Katalog.karakter(kayit.karakter)
    private val d = resources.displayMetrics.density
    private val rnd = Random(System.nanoTime())

    private val SUTUN = 60
    private val SATIR = 90
    private val tahta = Tahta(SUTUN, SATIR, Katalog.katman(seviye), rnd).apply {
        val adet = ((8 + seviye * 1.5f).coerceAtMost(26f) * (if (karakter.sansli) 1.4f else 1f)).toInt()
        hazineleriYerlestir(adet, karakter.sansli)
    }

    private var durum = Durum.HAZIR
    private val toplamSure = (Katalog.sure(seviye) + karakter.ekstraSure).toFloat()
    private var kalan = toplamSure
    private var gecen = 0f
    private var toplanan = 0
    private var carpanSure = 0f
    private var gozlukSure = 0f
    private var bombaHazir = false
    private var kutlamaSure = 0f
    private var sonZaman = 0L

    // Yerleşim
    private val hali = RectF()
    private var hucre = 1f
    private var haliBmp: Bitmap? = null
    private val kirBmp = Bitmap.createBitmap(SUTUN, SATIR, Bitmap.Config.ARGB_8888)
    private val kirRengi = HaliCizici.kirRengi(seviye)
    private val ustPanel = RectF()
    private val altPanel = RectF()
    private val dugmeler = Katalog.guclendiriciler.map { it to RectF() }

    // Dokunma
    private var dokunuyor = false
    private var sonX = 0f
    private var sonY = 0f
    private var dugmeyeBasildi = false
    private val firca get() = 24f * d * deterjan.yaricap * karakter.yaricapCarpani

    // Robot
    private var robotX = SUTUN / 2f
    private var robotY = SATIR / 2f
    private var robotVx = 7f
    private var robotVy = 5f

    // Efektler
    private class Parcacik(var x: Float, var y: Float, var vx: Float, var vy: Float, var r: Float, var omur: Float, val renk: Int)
    private class Yazi(var x: Float, var y: Float, val metin: String, val renk: Int, var omur: Float)
    private val parcaciklar = ArrayList<Parcacik>()
    private val yazilar = ArrayList<Yazi>()

    // Boyalar
    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpBoya = Paint(Paint.FILTER_BITMAP_FLAG)
    private val yaziBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val emojiBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    init {
        tahta.degisenler { kirPikseli(it) }
    }

    // ---------------------------------------------------------------- yerleşim

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val kenar = 14f * d
        ustPanel.set(kenar, kenar, w - kenar, kenar + 74f * d)
        altPanel.set(kenar, h - kenar - 78f * d, w - kenar, h - kenar)
        val ustSinir = ustPanel.bottom + 26f * d
        val altSinir = altPanel.top - 26f * d
        val musaitW = w - 2 * kenar
        val musaitH = altSinir - ustSinir
        var hw = musaitW
        var hh = hw * SATIR / SUTUN
        if (hh > musaitH) {
            hh = musaitH
            hw = hh * SUTUN / SATIR
        }
        val left = (w - hw) / 2
        val top = ustSinir + (musaitH - hh) / 2
        hali.set(left, top, left + hw, top + hh)
        hucre = hw / SUTUN
        haliBmp?.recycle()
        haliBmp = HaliCizici.ciz(hw.toInt().coerceAtLeast(1), hh.toInt().coerceAtLeast(1), seviye)

        val bosluk = 10f * d
        val dw = (altPanel.width() - bosluk * (dugmeler.size + 1)) / dugmeler.size
        dugmeler.forEachIndexed { i, (_, r) ->
            val x = altPanel.left + bosluk + i * (dw + bosluk)
            r.set(x, altPanel.top + bosluk, x + dw, altPanel.bottom - bosluk)
        }
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    fun duraklat() {
        if (durum == Durum.OYNUYOR) durum = Durum.HAZIR
        dokunuyor = false
    }

    fun geriBasildi(): Boolean {
        if (durum == Durum.OYNUYOR) {
            durum = Durum.HAZIR
            invalidate()
            return true
        }
        return false
    }

    // ---------------------------------------------------------------- güncelleme

    private fun guncelle(dt: Float) {
        if (durum == Durum.OYNUYOR) {
            gecen += dt
            if (sureli) {
                kalan -= dt
                if (kalan <= 0f) {
                    kalan = 0f
                    bitir(false)
                }
            }
            if (carpanSure > 0) carpanSure -= dt
            if (gozlukSure > 0) gozlukSure -= dt
            if (karakter.robot) robotuGezdir(dt)
        }

        if (durum == Durum.KUTLAMA) {
            kutlamaSure -= dt
            if (rnd.nextFloat() < 0.6f) konfeti()
            if (kutlamaSure <= 0f) bitir(true)
        }

        tahta.degisenler { kirPikseli(it) }
        if (durum == Durum.OYNUYOR) {
            hazineleriKontrolEt()
            if (tahta.temizOran >= 0.965f) kazan()
        }

        for (h in tahta.hazineler) if (h.toplandi && h.animasyon < 1f) h.animasyon += dt * 2.2f

        val it1 = parcaciklar.iterator()
        while (it1.hasNext()) {
            val p = it1.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 30f * d * dt
            p.omur -= dt
            if (p.omur <= 0) it1.remove()
        }
        val it2 = yazilar.iterator()
        while (it2.hasNext()) {
            val y = it2.next()
            y.y -= 40f * d * dt
            y.omur -= dt
            if (y.omur <= 0) it2.remove()
        }
    }

    private fun robotuGezdir(dt: Float) {
        robotX += robotVx * dt
        robotY += robotVy * dt
        if (robotX < 3f || robotX > SUTUN - 3f) { robotVx = -robotVx; robotX = robotX.coerceIn(3f, SUTUN - 3f) }
        if (robotY < 3f || robotY > SATIR - 3f) { robotVy = -robotVy; robotY = robotY.coerceIn(3f, SATIR - 3f) }
        if (rnd.nextFloat() < dt * 0.6f) {
            val aci = rnd.nextFloat() * 6.283f
            robotVx = 9f * kotlin.math.cos(aci)
            robotVy = 9f * kotlin.math.sin(aci)
        }
        tahta.fircala(robotX, robotY, 3.2f, 1.6f * dt)
    }

    private fun kirPikseli(i: Int) {
        val k = tahta.kir[i]
        val renk = if (k <= 0f) {
            Color.TRANSPARENT
        } else {
            val oran = (k / tahta.katman).coerceIn(0f, 1f)
            val a = ((0.3f + 0.75f * oran).coerceAtMost(1f) * 255).toInt()
            val t = tahta.ton[i]
            Color.argb(
                a,
                (Color.red(kirRengi) * t).toInt().coerceIn(0, 255),
                (Color.green(kirRengi) * t).toInt().coerceIn(0, 255),
                (Color.blue(kirRengi) * t).toInt().coerceIn(0, 255),
            )
        }
        kirBmp.setPixel(i % SUTUN, i / SUTUN, renk)
    }

    private fun hazineleriKontrolEt() {
        for (h in tahta.hazineler) {
            if (!h.toplandi && tahta.kir[h.hucre] <= 0.15f) topla(h)
        }
    }

    private fun gx(x: Float) = hali.left + x * hucre
    private fun gy(y: Float) = hali.top + y * hucre

    private fun altinVer(miktar: Int, x: Float, y: Float) {
        var m = miktar * karakter.altinCarpani
        if (carpanSure > 0) m *= 2
        if (!sureli) m *= 0.5f
        val n = m.toInt().coerceAtLeast(1)
        toplanan += n
        kayit.altin += n
        yazilar.add(Yazi(x, y, "+$n", 0xFFFFD54F.toInt(), 1.1f))
    }

    private fun topla(h: Hazine, miknatis: Boolean = false) {
        h.toplandi = true
        val x = gx(h.x)
        val y = gy(h.y)
        when (h.tip) {
            Esya.PARA -> altinVer(10, x, y)
            Esya.KESE -> altinVer(45, x, y)
            Esya.ELMAS -> { altinVer(120, x, y); yazilar.add(Yazi(x, y - 26 * d, "ELMAS!", 0xFF80DEEA.toInt(), 1.3f)) }
            Esya.SAAT -> if (sureli) {
                kalan += 5f
                yazilar.add(Yazi(x, y, "+5 sn", 0xFF81D4FA.toInt(), 1.2f))
            } else altinVer(15, x, y)
            Esya.YILDIZ -> {
                carpanSure = 10f
                yazilar.add(Yazi(x, y, "x2 ALTIN!", 0xFFFFF176.toInt(), 1.4f))
            }
            Esya.BOMBA -> {
                tahta.patlat(h.x, h.y, 8f)
                yazilar.add(Yazi(x, y, "KÖPÜK PATLAMASI!", 0xFFFFFFFF.toInt(), 1.3f))
                kopukPatlat(x, y, 40, 8f * hucre)
            }
            Esya.HEDIYE -> {
                if (rnd.nextFloat() < 0.6f) {
                    altinVer(40 + rnd.nextInt(110), x, y)
                } else {
                    val g = Katalog.guclendiriciler[rnd.nextInt(Katalog.guclendiriciler.size)]
                    kayit.adetEkle(g.id, 1)
                    yazilar.add(Yazi(x, y, "Hediye: +1 ${g.emoji}", 0xFFF48FB1.toInt(), 1.5f))
                }
            }
            Esya.HAYALET -> if (miknatis) {
                h.toplandi = true
            } else if (sureli) {
                kalan = (kalan - 4f).coerceAtLeast(0.5f)
                yazilar.add(Yazi(x, y, "Böö! -4 sn", 0xFFEF5350.toInt(), 1.3f))
            } else {
                yazilar.add(Yazi(x, y, "Böö!", 0xFFEF5350.toInt(), 1.1f))
            }
        }
        if (h.tip != Esya.HAYALET) yildizSac(x, y, 10)
    }

    private fun kazan() {
        tahta.hepsiniTemizle()
        for (h in tahta.hazineler) if (!h.toplandi && h.tip != Esya.HAYALET) topla(h)
        durum = Durum.KUTLAMA
        kutlamaSure = 1.6f
        yazilar.add(Yazi(width / 2f, hali.centerY(), "TERTEMİZ!", 0xFFFFFFFF.toInt(), 1.6f))
    }

    private fun bitir(kazandi: Boolean) {
        if (durum == Durum.BITTI) return
        durum = Durum.BITTI
        dokunuyor = false
        var bonus = 0
        var yildiz = 0
        if (kazandi && sureli) {
            bonus = (kalan * 3f * karakter.altinCarpani).toInt()
            kayit.altin += bonus
            val oran = kalan / toplamSure
            yildiz = when {
                oran >= 0.45f -> 3
                oran >= 0.2f -> 2
                else -> 1
            }
            kayit.yildizKaydet(seviye, yildiz)
            if (kayit.seviye == seviye) kayit.seviye = seviye + 1
        }
        val sonuc = Sonuc(kazandi, sureli, seviye, toplanan, bonus, yildiz, gecen)
        post { bitince(sonuc) }
    }

    // ---------------------------------------------------------------- güçlendiriciler

    private fun guclendiriciKullan(g: Guclendirici) {
        if (kayit.adet(g.id) <= 0) {
            yazilar.add(Yazi(width / 2f, altPanel.top - 10 * d, "Mağazadan ${g.ad} al!", 0xFFFFFFFF.toInt(), 1.2f))
            return
        }
        if (g.id == "sure" && !sureli) return
        if (g.id == "bomba" && bombaHazir) return
        kayit.adetEkle(g.id, -1)
        when (g.id) {
            "sure" -> {
                kalan += 15f
                yazilar.add(Yazi(width / 2f, ustPanel.bottom + 30 * d, "+15 sn", 0xFF81D4FA.toInt(), 1.2f))
            }
            "gozluk" -> gozlukSure = 4f
            "bomba" -> {
                bombaHazir = true
                yazilar.add(Yazi(width / 2f, hali.centerY(), "Halıda bir yere dokun!", 0xFFFFFFFF.toInt(), 1.4f))
            }
            "miknatis" -> {
                val hedefler = tahta.hazineler
                    .filter { !it.toplandi && it.tip != Esya.HAYALET }
                    .sortedByDescending { if (it.tip == Esya.ELMAS) 3 else if (it.tip == Esya.KESE) 2 else 1 }
                    .take(3)
                if (hedefler.isEmpty()) {
                    yazilar.add(Yazi(width / 2f, hali.centerY(), "Çekilecek bir şey kalmadı", 0xFFFFFFFF.toInt(), 1.2f))
                    kayit.adetEkle(g.id, 1)
                }
                for (h in hedefler) topla(h, miknatis = true)
            }
        }
    }

    // ---------------------------------------------------------------- dokunma

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (durum == Durum.BITTI || durum == Durum.KUTLAMA) return true
                val x = e.x
                val y = e.y
                dugmeyeBasildi = false
                if (durum == Durum.OYNUYOR) {
                    for ((g, r) in dugmeler) {
                        if (r.contains(x, y)) {
                            guclendiriciKullan(g)
                            dugmeyeBasildi = true
                            return true
                        }
                    }
                }
                if (durum == Durum.HAZIR) durum = Durum.OYNUYOR
                if (bombaHazir && hali.contains(x, y)) {
                    bombaHazir = false
                    val bx = (x - hali.left) / hucre
                    val by = (y - hali.top) / hucre
                    tahta.patlat(bx, by, 11f)
                    kopukPatlat(x, y, 60, 11f * hucre)
                    dugmeyeBasildi = true
                    return true
                }
                dokunuyor = true
                sonX = x
                sonY = y
                fircala(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dokunuyor || dugmeyeBasildi || durum != Durum.OYNUYOR) return true
                for (h in 0 until e.historySize) cizgi(e.getHistoricalX(h), e.getHistoricalY(h))
                cizgi(e.x, e.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dokunuyor = false
        }
        return true
    }

    private fun cizgi(x: Float, y: Float) {
        val mesafe = hypot(x - sonX, y - sonY)
        val adim = firca * 0.35f
        val n = (mesafe / adim).toInt()
        for (s in 1..n) {
            val t = s / n.toFloat()
            fircala(sonX + (x - sonX) * t, sonY + (y - sonY) * t)
        }
        if (n > 0) {
            sonX = x
            sonY = y
        }
    }

    private fun fircala(x: Float, y: Float) {
        val bx = (x - hali.left) / hucre
        val by = (y - hali.top) / hucre
        tahta.fircala(bx, by, firca / hucre, 0.3f * deterjan.guc)
        if (parcaciklar.size < 220 && hali.contains(x, y)) {
            val r = firca * 0.8f
            parcaciklar.add(
                Parcacik(
                    x + (rnd.nextFloat() - 0.5f) * r, y + (rnd.nextFloat() - 0.5f) * r,
                    (rnd.nextFloat() - 0.5f) * 30f * d, -rnd.nextFloat() * 30f * d,
                    (2f + rnd.nextFloat() * 6f) * d, 0.5f + rnd.nextFloat() * 0.5f, deterjan.kopukRengi,
                ),
            )
        }
    }

    private fun kopukPatlat(x: Float, y: Float, adet: Int, r: Float) {
        repeat(adet) {
            val a = rnd.nextFloat() * 6.283f
            val hiz = rnd.nextFloat() * r * 2f
            parcaciklar.add(
                Parcacik(x, y, kotlin.math.cos(a) * hiz, kotlin.math.sin(a) * hiz,
                    (3f + rnd.nextFloat() * 8f) * d, 0.6f + rnd.nextFloat() * 0.6f, deterjan.kopukRengi),
            )
        }
    }

    private fun yildizSac(x: Float, y: Float, adet: Int) {
        repeat(adet) {
            val a = rnd.nextFloat() * 6.283f
            val hiz = (40f + rnd.nextFloat() * 80f) * d
            parcaciklar.add(
                Parcacik(x, y, kotlin.math.cos(a) * hiz, kotlin.math.sin(a) * hiz,
                    (2f + rnd.nextFloat() * 3f) * d, 0.5f + rnd.nextFloat() * 0.4f, 0xFFFFD54F.toInt()),
            )
        }
    }

    private fun konfeti() {
        val renkler = intArrayOf(0xFFFFD54F.toInt(), 0xFFEF5350.toInt(), 0xFF66BB6A.toInt(), 0xFF42A5F5.toInt(), 0xFFAB47BC.toInt())
        parcaciklar.add(
            Parcacik(rnd.nextFloat() * width, hali.top, (rnd.nextFloat() - 0.5f) * 60f * d, rnd.nextFloat() * 60f * d,
                (3f + rnd.nextFloat() * 3f) * d, 1.5f, renkler[rnd.nextInt(renkler.size)]),
        )
    }

    // ---------------------------------------------------------------- çizim

    override fun onDraw(c: Canvas) {
        val simdi = System.nanoTime()
        val dt = if (sonZaman == 0L) 0f else ((simdi - sonZaman) / 1e9f).coerceAtMost(0.05f)
        sonZaman = simdi
        guncelle(dt)

        zeminCiz(c)
        sacakCiz(c)
        haliBmp?.let { c.drawBitmap(it, hali.left, hali.top, null) }

        for (h in tahta.hazineler) if (!h.toplandi) hazineCiz(c, h, 255, 1f)
        c.drawBitmap(kirBmp, null, hali, bmpBoya)

        if (gozlukSure > 0) {
            val a = (150 + 100 * sin(gecen * 10f)).toInt().coerceIn(0, 255)
            for (h in tahta.hazineler) if (!h.toplandi) {
                boya.style = Paint.Style.STROKE
                boya.strokeWidth = 2f * d
                boya.color = Color.argb(a, 255, 255, 120)
                c.drawCircle(gx(h.x), gy(h.y), hucre * 2.2f, boya)
                boya.style = Paint.Style.FILL
                hazineCiz(c, h, a, 1f)
            }
        }
        for (h in tahta.hazineler) if (h.toplandi && h.animasyon < 1f) {
            hazineCiz(c, h, ((1f - h.animasyon) * 255).toInt(), 1f + h.animasyon * 0.8f)
        }

        if (karakter.robot) {
            emojiBoya.textSize = hucre * 5f
            emojiBoya.alpha = 255
            emojiCiz(c, "🤖", gx(robotX), gy(robotY))
        }

        for (p in parcaciklar) {
            boya.color = p.renk
            boya.alpha = (p.omur.coerceIn(0f, 1f) * 220).toInt()
            c.drawCircle(p.x, p.y, p.r, boya)
        }
        boya.alpha = 255

        if (dokunuyor && durum == Durum.OYNUYOR) {
            boya.style = Paint.Style.STROKE
            boya.strokeWidth = 2f * d
            boya.color = Color.argb(120, 255, 255, 255)
            c.drawCircle(sonX, sonY, firca, boya)
            boya.style = Paint.Style.FILL
            emojiBoya.textSize = 34f * d
            emojiBoya.alpha = 255
            emojiCiz(c, "🧽", sonX, sonY)
        }

        for (y in yazilar) {
            yaziBoya.textSize = 22f * d
            yaziBoya.color = Color.BLACK
            yaziBoya.alpha = (y.omur.coerceIn(0f, 1f) * 200).toInt()
            c.drawText(y.metin, y.x + 1.5f * d, y.y + 1.5f * d, yaziBoya)
            yaziBoya.color = y.renk
            yaziBoya.alpha = (y.omur.coerceIn(0f, 1f) * 255).toInt()
            c.drawText(y.metin, y.x, y.y, yaziBoya)
        }
        yaziBoya.alpha = 255

        ustPanelCiz(c)
        altPanelCiz(c)

        if (durum == Durum.HAZIR) hazirEkrani(c)

        if (durum != Durum.BITTI) postInvalidateOnAnimation() else sonZaman = 0L
    }

    private fun zeminCiz(c: Canvas) {
        c.drawColor(0xFF4E342E.toInt())
        boya.color = 0xFF3E2723.toInt()
        boya.strokeWidth = 2f * d
        val tahtaGen = 46f * d
        var x = 0f
        var i = 0
        while (x < width) {
            c.drawLine(x, 0f, x, height.toFloat(), boya)
            val ek = if (i % 2 == 0) 0.3f else 0.7f
            c.drawLine(x, height * ek, x + tahtaGen, height * ek, boya)
            x += tahtaGen
            i++
        }
    }

    private fun sacakCiz(c: Canvas) {
        boya.color = 0xFFF5ECD7.toInt()
        boya.strokeWidth = 2f * d
        val uzun = 14f * d
        var x = hali.left + 3f * d
        while (x < hali.right) {
            c.drawLine(x, hali.top, x, hali.top - uzun, boya)
            c.drawLine(x, hali.bottom, x, hali.bottom + uzun, boya)
            x += 6f * d
        }
    }

    private fun hazineCiz(c: Canvas, h: Hazine, alfa: Int, olcek: Float) {
        val x = gx(h.x)
        val y = gy(h.y)
        val r = hucre * 1.7f * olcek
        if (h.tip == Esya.PARA) {
            boya.color = 0xFFB8860B.toInt(); boya.alpha = alfa
            c.drawCircle(x, y, r, boya)
            boya.color = 0xFFFFC83D.toInt(); boya.alpha = alfa
            c.drawCircle(x, y, r * 0.86f, boya)
            boya.color = 0xFFFFE082.toInt(); boya.alpha = alfa
            c.drawCircle(x - r * 0.25f, y - r * 0.25f, r * 0.3f, boya)
            yaziBoya.color = 0xFFB8860B.toInt(); yaziBoya.alpha = alfa
            yaziBoya.textSize = r * 1.1f
            c.drawText("₺", x, y + r * 0.38f, yaziBoya)
            yaziBoya.alpha = 255
            boya.alpha = 255
        } else {
            emojiBoya.textSize = r * 2.1f
            emojiBoya.alpha = alfa
            emojiCiz(c, h.tip.emoji, x, y)
            emojiBoya.alpha = 255
        }
    }

    private fun emojiCiz(c: Canvas, e: String, x: Float, y: Float) {
        val fm = emojiBoya.fontMetrics
        c.drawText(e, x, y - (fm.ascent + fm.descent) / 2, emojiBoya)
    }

    private fun panel(c: Canvas, r: RectF) {
        boya.color = Color.argb(215, 20, 18, 30)
        c.drawRoundRect(r, 18f * d, 18f * d, boya)
    }

    private fun ustPanelCiz(c: Canvas) {
        panel(c, ustPanel)
        val ortaY = ustPanel.top + 30f * d
        yaziBoya.textAlign = Paint.Align.LEFT
        yaziBoya.textSize = 22f * d

        // Süre
        if (sureli) {
            val az = kalan < 10f
            yaziBoya.color = if (az && (gecen * 4).toInt() % 2 == 0) 0xFFEF5350.toInt() else Color.WHITE
            c.drawText("⏱ ${kalan.toInt()}", ustPanel.left + 14 * d, ortaY + 8 * d, yaziBoya)
        } else {
            yaziBoya.color = Color.WHITE
            c.drawText("🧘 Rahat", ustPanel.left + 14 * d, ortaY + 8 * d, yaziBoya)
        }

        // Altın
        yaziBoya.textAlign = Paint.Align.RIGHT
        yaziBoya.color = 0xFFFFD54F.toInt()
        val altinYazi = if (carpanSure > 0) "x2 💰 $toplanan" else "💰 $toplanan"
        c.drawText(altinYazi, ustPanel.right - 14 * d, ortaY + 8 * d, yaziBoya)

        // İlerleme çubuğu
        val bx0 = ustPanel.left + 110 * d
        val bx1 = ustPanel.right - 120 * d
        val by0 = ortaY - 8 * d
        val by1 = ortaY + 8 * d
        boya.color = 0xFF37474F.toInt()
        c.drawRoundRect(bx0, by0, bx1, by1, 8 * d, 8 * d, boya)
        val oran = tahta.temizOran / 0.965f
        boya.color = 0xFF4FC3F7.toInt()
        c.drawRoundRect(bx0, by0, bx0 + (bx1 - bx0) * oran.coerceIn(0f, 1f), by1, 8 * d, 8 * d, boya)
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.textSize = 13f * d
        yaziBoya.color = Color.WHITE
        c.drawText("%${(oran.coerceAtMost(1f) * 100).toInt()} temiz", (bx0 + bx1) / 2, by1 + 15 * d, yaziBoya)

        // Alt satır
        yaziBoya.textSize = 13f * d
        yaziBoya.color = 0xFFB0BEC5.toInt()
        c.drawText(
            "Bölüm $seviye  ·  ${deterjan.emoji} ${deterjan.ad}  ·  ${karakter.emoji} ${karakter.ad}",
            ustPanel.centerX(), ustPanel.bottom - 8 * d, yaziBoya,
        )
    }

    private fun altPanelCiz(c: Canvas) {
        panel(c, altPanel)
        for ((g, r) in dugmeler) {
            val adet = kayit.adet(g.id)
            val aktif = adet > 0 && !(g.id == "sure" && !sureli)
            val secili = (g.id == "bomba" && bombaHazir) || (g.id == "gozluk" && gozlukSure > 0)
            boya.color = if (secili) 0xFF1E88E5.toInt() else if (aktif) 0xFF455A64.toInt() else 0xFF263238.toInt()
            c.drawRoundRect(r, 14 * d, 14 * d, boya)
            emojiBoya.textSize = 26f * d
            emojiBoya.alpha = if (aktif) 255 else 90
            emojiCiz(c, g.emoji, r.centerX(), r.centerY() - 6 * d)
            emojiBoya.alpha = 255
            yaziBoya.textAlign = Paint.Align.CENTER
            yaziBoya.textSize = 12f * d
            yaziBoya.color = if (aktif) Color.WHITE else 0xFF78909C.toInt()
            c.drawText("x$adet", r.centerX(), r.bottom - 6 * d, yaziBoya)
        }
    }

    private fun hazirEkrani(c: Canvas) {
        boya.color = Color.argb(150, 0, 0, 0)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boya)
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.color = Color.WHITE
        yaziBoya.textSize = 28f * d
        val cy = height / 2f
        c.drawText(if (gecen > 0f) "Duraklatıldı" else "Bölüm $seviye", width / 2f, cy - 50 * d, yaziBoya)
        yaziBoya.textSize = 18f * d
        c.drawText("Parmağınla halıyı ovala,", width / 2f, cy, yaziBoya)
        c.drawText("kirin altındaki altınları bul!", width / 2f, cy + 26 * d, yaziBoya)
        yaziBoya.color = 0xFFFFD54F.toInt()
        val a = (180 + 75 * sin(System.nanoTime() / 2e8)).toInt().coerceIn(0, 255)
        yaziBoya.alpha = a
        c.drawText(if (gecen > 0f) "Devam etmek için dokun" else "Başlamak için dokun", width / 2f, cy + 80 * d, yaziBoya)
        yaziBoya.alpha = 255
        if (sureli && gecen == 0f) {
            yaziBoya.color = 0xFFB0BEC5.toInt()
            yaziBoya.textSize = 15f * d
            c.drawText("Süre: ${toplamSure.toInt()} sn — ne kadar hızlı, o kadar çok bonus", width / 2f, cy + 120 * d, yaziBoya)
        }
    }
}
