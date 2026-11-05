package com.burhanettin.altinhali

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Oyun ekranı. Halıyı, kiri, efektleri ve tüm oyun içi arayüzü (üst panel, güçlendiriciler,
 * duraklatma menüsü, sonuç ekranı) kendisi çizer.
 *
 * [eylem] "tekrar" (bölümü yeniden/sonrakini başlat) veya "menu" ile çağrılır.
 */
class OyunView(
    context: Context,
    private val sureli: Boolean,
    private val eylem: (String) -> Unit,
) : View(context) {

    private enum class Durum { HAZIR, OYNUYOR, DURAKLADI, KUTLAMA, BITTI }
    private enum class Tur { NORMAL, HAZINE, INATCI }

    private class Dugme(val r: RectF, var yazi: String, val renk: Int, val yaziRengi: Int) {
        var eylem: () -> Unit = {}
    }

    private val kayit = Kayit(context)
    private val seviye = kayit.seviye
    private val deterjan = Katalog.deterjan(kayit.deterjan)
    private val karakter = Katalog.karakter(kayit.karakter)
    private val d = resources.displayMetrics.density
    private val rnd = Random(System.nanoTime())

    private val tur = when {
        seviye % 5 == 0 -> Tur.HAZINE
        seviye >= 6 && seviye % 3 == 0 -> Tur.INATCI
        else -> Tur.NORMAL
    }

    private val SUTUN = 60
    private val SATIR = 90
    private val tahta = Tahta(SUTUN, SATIR, Katalog.katman(seviye), rnd).apply {
        var adet = (8 + seviye * 1.5f).coerceAtMost(26f)
        if (karakter.sansli) adet *= 1.4f
        if (tur == Tur.HAZINE) adet *= 1.8f
        hazineleriYerlestir(adet.toInt(), karakter.sansli, tur == Tur.HAZINE)
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
    private var animZaman = 0f

    // Kombo, sarsıntı, ses zamanlaması
    private var kombo = 0
    private var komboSure = 0f
    private var enIyiKombo = 0
    private var sarsinti = 0f
    private var ovalaSesZaman = 0f
    private var yayilZaman = 0f

    // Sonuç
    private var kazandi = false
    private var bonus = 0
    private var yildiz = 0
    private var rekor = 0
    private var yeniRekor = false
    private var sonucZaman = 0f
    private var calinanYildiz = 0
    private val kazanilanBasarimlar = ArrayList<Basarim>()

    // Yerleşim
    private val hali = RectF()
    private var hucre = 1f
    private var haliBmp: Bitmap? = null
    private val kirBmp = Bitmap.createBitmap(SUTUN, SATIR, Bitmap.Config.ARGB_8888)
    private val kirRengi = HaliCizici.kirRengi(seviye)
    private val ustPanel = RectF()
    private val duraklatR = RectF()
    private val altPanel = RectF()
    private val gucDugmeleri = Katalog.guclendiriciler.map { it to RectF() }
    private val menuPanel = RectF()
    private val menuDugmeleri = ArrayList<Dugme>()

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
    private class Yazi(var x: Float, var y: Float, val metin: String, val renk: Int, var omur: Float, val boyut: Float = 22f)
    private class Bildirim(val metin: String, var omur: Float)
    private val parcaciklar = ArrayList<Parcacik>()
    private val yazilar = ArrayList<Yazi>()
    private val bildirimler = ArrayList<Bildirim>()

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
        val kenar = 12f * d
        duraklatR.set(w - kenar - 52f * d, kenar, w - kenar, kenar + 52f * d)
        ustPanel.set(kenar, kenar, duraklatR.left - 8f * d, kenar + 96f * d)
        altPanel.set(kenar, h - kenar - 78f * d, w - kenar, h - kenar)
        val ustSinir = ustPanel.bottom + 24f * d
        val altSinir = altPanel.top - 24f * d
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
        val dw = (altPanel.width() - bosluk * (gucDugmeleri.size + 1)) / gucDugmeleri.size
        gucDugmeleri.forEachIndexed { i, (_, r) ->
            val x = altPanel.left + bosluk + i * (dw + bosluk)
            r.set(x, altPanel.top + bosluk, x + dw, altPanel.bottom - bosluk)
        }
        when (durum) {
            Durum.DURAKLADI -> duraklatMenusuKur()
            Durum.BITTI -> sonucDugmeleriniKur()
            else -> {}
        }
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    /** Uygulama arka plana geçince çağrılır. */
    fun duraklat() {
        if (durum == Durum.OYNUYOR || (durum == Durum.HAZIR && gecen > 0f)) duraklatMenusu()
        dokunuyor = false
    }

    /** Geri tuşu: oyundaysa duraklatır; true dönerse olay tüketilmiştir. */
    fun geriBasildi(): Boolean = when (durum) {
        Durum.OYNUYOR, Durum.HAZIR -> { duraklatMenusu(); true }
        Durum.DURAKLADI -> { devamEt(); true }
        Durum.KUTLAMA -> true
        Durum.BITTI -> false
    }

    private fun devamEt() {
        durum = if (gecen > 0f) Durum.OYNUYOR else Durum.HAZIR
        menuDugmeleri.clear()
    }

    // ---------------------------------------------------------------- güncelleme

    private fun guncelle(dt: Float) {
        animZaman += dt
        if (durum == Durum.OYNUYOR) {
            gecen += dt
            if (sureli) {
                val onceki = ceil(kalan).toInt()
                kalan -= dt
                val simdi = ceil(kalan).toInt()
                if (simdi != onceki && simdi in 1..5) Ses.cal("tik", 0.7f)
                if (kalan <= 0f) {
                    kalan = 0f
                    bitir(false)
                }
            }
            if (carpanSure > 0) carpanSure -= dt
            if (gozlukSure > 0) gozlukSure -= dt
            if (komboSure > 0) {
                komboSure -= dt
                if (komboSure <= 0) kombo = 0
            }
            if (ovalaSesZaman > 0) ovalaSesZaman -= dt
            if (karakter.robot) robotuGezdir(dt)
            if (tur == Tur.INATCI) {
                yayilZaman += dt
                while (yayilZaman > 0.25f) {
                    yayilZaman -= 0.25f
                    tahta.yayil(45, tahta.katman * 0.35f)
                }
            }
        }

        if (durum == Durum.KUTLAMA) {
            kutlamaSure -= dt
            if (rnd.nextFloat() < 0.7f) konfeti()
            if (kutlamaSure <= 0f) bitir(true)
        }

        if (durum == Durum.BITTI) {
            sonucZaman += dt
            if (kazandi && sureli && calinanYildiz < yildiz && sonucZaman > 0.45f + calinanYildiz * 0.35f) {
                calinanYildiz++
                Ses.cal("yildiz", 0.8f, 0.9f + 0.15f * calinanYildiz)
                Ses.titret(15)
            }
        }

        if (sarsinti > 0) sarsinti = (sarsinti - dt).coerceAtLeast(0f)

        tahta.degisenler { kirPikseli(it) }
        if (durum == Durum.OYNUYOR) {
            hazineleriKontrolEt()
            if (durum == Durum.OYNUYOR && tahta.temizOran >= 0.965f) kazan()
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
        if (bildirimler.isNotEmpty()) {
            bildirimler[0].omur -= dt
            if (bildirimler[0].omur <= 0) bildirimler.removeAt(0)
        }
    }

    private fun robotuGezdir(dt: Float) {
        robotX += robotVx * dt
        robotY += robotVy * dt
        if (robotX < 3f || robotX > SUTUN - 3f) { robotVx = -robotVx; robotX = robotX.coerceIn(3f, SUTUN - 3f) }
        if (robotY < 3f || robotY > SATIR - 3f) { robotVy = -robotVy; robotY = robotY.coerceIn(3f, SATIR - 3f) }
        if (rnd.nextFloat() < dt * 0.6f) {
            val aci = rnd.nextFloat() * 6.283f
            robotVx = 9f * cos(aci)
            robotVy = 9f * sin(aci)
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

    private val komboCarpani get() = (1f + (kombo - 1) * 0.2f).coerceIn(1f, 2f)

    private fun altinVer(miktar: Int, x: Float, y: Float, kombolu: Boolean = false) {
        var m = miktar * karakter.altinCarpani
        if (carpanSure > 0) m *= 2
        if (kombolu) m *= komboCarpani
        if (!sureli) m *= 0.5f
        val n = m.toInt().coerceAtLeast(1)
        toplanan += n
        kayit.altin += n
        kayit.istatEkle("altin", n)
        yazilar.add(Yazi(x, y, "+$n", 0xFFFFD54F.toInt(), 1.1f))
    }

    private fun komboArtir(x: Float, y: Float) {
        kombo = if (komboSure > 0) kombo + 1 else 1
        komboSure = 2.2f
        if (kombo > enIyiKombo) {
            enIyiKombo = kombo
            kayit.istatEnFazla("kombo", kombo)
        }
        if (kombo >= 2) {
            yazilar.add(Yazi(x, y - 30 * d, "KOMBO x$kombo", 0xFFFF9800.toInt(), 1.0f, 18f))
            Ses.cal("kombo", 0.5f, 1f + minOf(kombo, 8) * 0.06f)
        }
    }

    private fun topla(h: Hazine, miknatis: Boolean = false) {
        h.toplandi = true
        val x = gx(h.x)
        val y = gy(h.y)
        when (h.tip) {
            Esya.PARA -> {
                komboArtir(x, y)
                altinVer(10, x, y, kombolu = true)
                Ses.cal("para", 0.8f, 1f + minOf(kombo, 6) * 0.04f)
                Ses.titret(10)
            }
            Esya.KESE -> {
                komboArtir(x, y)
                altinVer(45, x, y, kombolu = true)
                Ses.cal("kese", 0.8f)
                Ses.titret(20)
            }
            Esya.ELMAS -> {
                komboArtir(x, y)
                altinVer(120, x, y, kombolu = true)
                kayit.istatEkle("elmas")
                yazilar.add(Yazi(x, y - 26 * d, "ELMAS!", 0xFF80DEEA.toInt(), 1.3f))
                Ses.cal("elmas")
                Ses.titret(30)
            }
            Esya.SAAT -> {
                if (sureli) {
                    kalan += 5f
                    yazilar.add(Yazi(x, y, "+5 sn", 0xFF81D4FA.toInt(), 1.2f))
                } else {
                    altinVer(15, x, y)
                }
                Ses.cal("guc", 0.7f)
            }
            Esya.YILDIZ -> {
                carpanSure = 10f
                yazilar.add(Yazi(x, y, "x2 ALTIN!", 0xFFFFF176.toInt(), 1.4f))
                Ses.cal("guc", 0.7f, 1.2f)
            }
            Esya.BOMBA -> {
                tahta.patlat(h.x, h.y, 8f)
                yazilar.add(Yazi(x, y, "KÖPÜK PATLAMASI!", 0xFFFFFFFF.toInt(), 1.3f))
                kopukPatlat(x, y, 40, 8f * hucre)
                sarsinti = 0.3f
                Ses.cal("bomba", 0.9f)
                Ses.titret(60)
            }
            Esya.HEDIYE -> {
                if (rnd.nextFloat() < 0.6f) {
                    altinVer(40 + rnd.nextInt(110), x, y)
                } else {
                    val g = Katalog.guclendiriciler[rnd.nextInt(Katalog.guclendiriciler.size)]
                    kayit.adetEkle(g.id, 1)
                    yazilar.add(Yazi(x, y, "Hediye: +1 ${g.emoji}", 0xFFF48FB1.toInt(), 1.5f))
                }
                Ses.cal("guc", 0.7f, 1.4f)
            }
            Esya.HAYALET -> if (!miknatis) {
                kayit.istatEkle("hayalet")
                if (sureli) {
                    kalan = (kalan - 4f).coerceAtLeast(0.5f)
                    yazilar.add(Yazi(x, y, "Böö! -4 sn", 0xFFEF5350.toInt(), 1.3f))
                } else {
                    yazilar.add(Yazi(x, y, "Böö!", 0xFFEF5350.toInt(), 1.1f))
                }
                kombo = 0
                komboSure = 0f
                sarsinti = 0.2f
                Ses.cal("hayalet", 0.8f)
                Ses.titret(120)
            }
        }
        if (h.tip != Esya.HAYALET) yildizSac(x, y, 10)
        basarimKontrol()
    }

    private fun basarimKontrol() {
        val yeni = Basarimlar.kontrol(kayit)
        for (b in yeni) {
            kazanilanBasarimlar.add(b)
            bildirimler.add(Bildirim("🏆 ${b.ad}   +${b.odul} 💰", 2.8f))
            Ses.cal("basarim", 0.8f)
        }
    }

    private fun kazan() {
        tahta.hepsiniTemizle()
        for (h in tahta.hazineler) if (!h.toplandi && h.tip != Esya.HAYALET) topla(h, miknatis = true)
        durum = Durum.KUTLAMA
        dokunuyor = false
        kutlamaSure = 1.6f
        yazilar.add(Yazi(width / 2f, hali.centerY(), "TERTEMİZ!", 0xFFFFFFFF.toInt(), 1.6f, 34f))
        Ses.cal("kazan")
        Ses.titret(80)
    }

    private fun bitir(kazandiMi: Boolean) {
        if (durum == Durum.BITTI) return
        durum = Durum.BITTI
        dokunuyor = false
        kazandi = kazandiMi
        sonucZaman = 0f
        if (kazandi && sureli) {
            bonus = (kalan * 3f * karakter.altinCarpani).toInt()
            kayit.altin += bonus
            kayit.istatEkle("altin", bonus)
            val oran = kalan / toplamSure
            yildiz = when {
                oran >= 0.45f -> 3
                oran >= 0.2f -> 2
                else -> 1
            }
            kayit.yildizKaydet(seviye, yildiz)
            if (kayit.seviye == seviye) kayit.seviye = seviye + 1
            kayit.istatEkle("bolum")
            val sn = gecen.toInt().coerceAtLeast(1)
            kayit.istatEnAz("hizli", sn)
            val eski = kayit.rekor(seviye)
            if (eski == 0 || sn < eski) {
                yeniRekor = eski != 0
                kayit.rekorKaydet(seviye, sn)
            }
            rekor = kayit.rekor(seviye)
        }
        if (!kazandi) {
            Ses.cal("kaybet")
            Ses.titret(150)
        }
        basarimKontrol()
        sonucDugmeleriniKur()
    }

    // ---------------------------------------------------------------- menüler

    private fun duraklatMenusu() {
        if (durum == Durum.BITTI || durum == Durum.KUTLAMA) return
        durum = Durum.DURAKLADI
        dokunuyor = false
        duraklatMenusuKur()
    }

    private fun sesYazisi() = if (kayit.sesAcik) "🔊  Ses: Açık" else "🔇  Ses: Kapalı"

    private fun duraklatMenusuKur() {
        val pw = minOf(width - 48f * d, 340f * d)
        val ph = 380f * d
        menuPanel.set((width - pw) / 2, (height - ph) / 2, (width + pw) / 2, (height + ph) / 2)
        menuDugmeleri.clear()
        val bh = 54f * d
        val ara = 12f * d
        var y = menuPanel.top + 96f * d
        fun yeni(yazi: String, renk: Int, yaziRengi: Int, ey: () -> Unit): Dugme {
            val b = Dugme(RectF(menuPanel.left + 22 * d, y, menuPanel.right - 22 * d, y + bh), yazi, renk, yaziRengi)
            b.eylem = ey
            menuDugmeleri.add(b)
            y += bh + ara
            return b
        }
        yeni("▶  Devam Et", 0xFFFFB300.toInt(), 0xFF2B1B00.toInt()) { devamEt() }
        yeni("↻  Yeniden Başla", 0xFF4FC3F7.toInt(), 0xFF0D2A3A.toInt()) { eylem("tekrar") }
        val ses = yeni(sesYazisi(), 0xFF455A64.toInt(), Color.WHITE) {}
        ses.eylem = {
            kayit.sesAcik = !kayit.sesAcik
            Ses.ayarla(kayit.sesAcik, kayit.titresimAcik)
            ses.yazi = sesYazisi()
        }
        yeni("🏠  Ana Menü", 0xFF37474F.toInt(), Color.WHITE) { eylem("menu") }
    }

    private fun sonucDugmeleriniKur() {
        val pw = minOf(width - 32f * d, 380f * d)
        val ph = minOf(height - 40f * d, 560f * d)
        menuPanel.set((width - pw) / 2, (height - ph) / 2, (width + pw) / 2, (height + ph) / 2)
        menuDugmeleri.clear()
        val sol = menuPanel.left + 22 * d
        val sag = menuPanel.right - 22 * d
        val ana = Dugme(
            RectF(sol, menuPanel.bottom - 132 * d, sag, menuPanel.bottom - 76 * d),
            if (kazandi && sureli) "Sonraki Bölüm  ▶" else "Tekrar Oyna  ↻",
            0xFFFFB300.toInt(), 0xFF2B1B00.toInt(),
        )
        ana.eylem = { eylem("tekrar") }
        val menu = Dugme(RectF(sol, menuPanel.bottom - 66 * d, sag, menuPanel.bottom - 16 * d), "Ana Menü", 0xFF37474F.toInt(), Color.WHITE)
        menu.eylem = { eylem("menu") }
        menuDugmeleri.add(ana)
        menuDugmeleri.add(menu)
    }

    // ---------------------------------------------------------------- güçlendiriciler

    private fun guclendiriciKullan(g: Guclendirici) {
        if (g.id == "sure" && !sureli) return
        if (kayit.adet(g.id) <= 0) {
            yazilar.add(Yazi(width / 2f, altPanel.top - 10 * d, "Mağazadan ${g.ad} al!", 0xFFFFFFFF.toInt(), 1.2f, 16f))
            return
        }
        if (g.id == "bomba" && bombaHazir) return
        kayit.adetEkle(g.id, -1)
        Ses.cal("guc", 0.7f)
        when (g.id) {
            "sure" -> {
                kalan += 15f
                yazilar.add(Yazi(width / 2f, ustPanel.bottom + 30 * d, "+15 sn", 0xFF81D4FA.toInt(), 1.2f))
            }
            "gozluk" -> gozlukSure = 4f
            "bomba" -> {
                bombaHazir = true
                yazilar.add(Yazi(width / 2f, hali.centerY(), "Halıda bir yere dokun!", 0xFFFFFFFF.toInt(), 1.4f, 18f))
            }
            "miknatis" -> {
                val hedefler = tahta.hazineler
                    .filter { !it.toplandi && it.tip != Esya.HAYALET }
                    .sortedByDescending { if (it.tip == Esya.ELMAS) 3 else if (it.tip == Esya.KESE) 2 else 1 }
                    .take(3)
                if (hedefler.isEmpty()) {
                    yazilar.add(Yazi(width / 2f, hali.centerY(), "Çekilecek bir şey kalmadı", 0xFFFFFFFF.toInt(), 1.2f, 16f))
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
                val x = e.x
                val y = e.y
                if (durum == Durum.DURAKLADI || durum == Durum.BITTI) {
                    if (durum == Durum.BITTI && sonucZaman < 0.6f) return true
                    for (b in menuDugmeleri.toList()) {
                        if (b.r.contains(x, y)) {
                            Ses.cal("dugme", 0.7f)
                            b.eylem()
                            break
                        }
                    }
                    return true
                }
                if (durum == Durum.KUTLAMA) return true
                if (x >= duraklatR.left - 8 * d && y <= duraklatR.bottom + 8 * d) {
                    Ses.cal("dugme", 0.7f)
                    duraklatMenusu()
                    return true
                }
                dugmeyeBasildi = false
                if (durum == Durum.OYNUYOR) {
                    for ((g, r) in gucDugmeleri) {
                        if (r.contains(x, y)) {
                            guclendiriciKullan(g)
                            dugmeyeBasildi = true
                            return true
                        }
                    }
                }
                if (durum == Durum.HAZIR) {
                    durum = Durum.OYNUYOR
                    if (!kayit.egitimGoruldu) kayit.egitimGoruldu = true
                }
                if (bombaHazir && hali.contains(x, y)) {
                    bombaHazir = false
                    val bx = (x - hali.left) / hucre
                    val by = (y - hali.top) / hucre
                    tahta.patlat(bx, by, 11f)
                    kopukPatlat(x, y, 60, 11f * hucre)
                    sarsinti = 0.35f
                    Ses.cal("bomba")
                    Ses.titret(70)
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
        val degisen = tahta.fircala(bx, by, firca / hucre, 0.3f * deterjan.guc)
        if (degisen > 0 && ovalaSesZaman <= 0f) {
            Ses.cal("ovala", 0.22f, 0.85f + rnd.nextFloat() * 0.3f)
            ovalaSesZaman = 0.09f
        }
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
                Parcacik(x, y, cos(a) * hiz, sin(a) * hiz,
                    (3f + rnd.nextFloat() * 8f) * d, 0.6f + rnd.nextFloat() * 0.6f, deterjan.kopukRengi),
            )
        }
    }

    private fun yildizSac(x: Float, y: Float, adet: Int) {
        repeat(adet) {
            val a = rnd.nextFloat() * 6.283f
            val hiz = (40f + rnd.nextFloat() * 80f) * d
            parcaciklar.add(
                Parcacik(x, y, cos(a) * hiz, sin(a) * hiz,
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
        c.save()
        if (sarsinti > 0) {
            val g = sarsinti * 30f * d
            c.translate((rnd.nextFloat() - 0.5f) * g, (rnd.nextFloat() - 0.5f) * g)
        }
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

        yaziBoya.textAlign = Paint.Align.CENTER
        for (y in yazilar) {
            yaziBoya.textSize = y.boyut * d
            yaziBoya.color = Color.BLACK
            yaziBoya.alpha = (y.omur.coerceIn(0f, 1f) * 200).toInt()
            c.drawText(y.metin, y.x + 1.5f * d, y.y + 1.5f * d, yaziBoya)
            yaziBoya.color = y.renk
            yaziBoya.alpha = (y.omur.coerceIn(0f, 1f) * 255).toInt()
            c.drawText(y.metin, y.x, y.y, yaziBoya)
        }
        yaziBoya.alpha = 255
        c.restore()

        ustPanelCiz(c)
        duraklatDugmesiCiz(c)
        altPanelCiz(c)
        bildirimCiz(c)

        when (durum) {
            Durum.HAZIR -> hazirEkrani(c)
            Durum.DURAKLADI -> duraklatEkrani(c)
            Durum.BITTI -> sonucEkrani(c)
            else -> {}
        }

        postInvalidateOnAnimation()
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
            yaziBoya.textAlign = Paint.Align.CENTER
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

    private fun panel(c: Canvas, r: RectF, alfa: Int = 215) {
        boya.style = Paint.Style.FILL
        boya.color = Color.argb(alfa, 20, 18, 30)
        c.drawRoundRect(r, 18f * d, 18f * d, boya)
    }

    private fun turAdi() = when (tur) {
        Tur.HAZINE -> "Hazine Halısı"
        Tur.INATCI -> "İnatçı Kir"
        Tur.NORMAL -> null
    }

    private fun ustPanelCiz(c: Canvas) {
        panel(c, ustPanel)
        val sol = ustPanel.left + 16 * d
        val sag = ustPanel.right - 16 * d
        val y1 = ustPanel.top + 32 * d

        yaziBoya.textSize = 22f * d
        yaziBoya.textAlign = Paint.Align.LEFT
        if (sureli) {
            val az = kalan < 10f && durum == Durum.OYNUYOR
            yaziBoya.color = if (az && (gecen * 4).toInt() % 2 == 0) 0xFFEF5350.toInt() else Color.WHITE
            c.drawText("⏱ ${ceil(kalan).toInt()}", sol, y1, yaziBoya)
        } else {
            yaziBoya.color = Color.WHITE
            c.drawText("🧘 Rahat", sol, y1, yaziBoya)
        }

        yaziBoya.textAlign = Paint.Align.RIGHT
        yaziBoya.color = 0xFFFFD54F.toInt()
        c.drawText("💰 $toplanan", sag, y1, yaziBoya)

        // Ortada etkin durumlar
        val durumlar = ArrayList<String>()
        if (kombo >= 2) durumlar.add("🔥x$kombo")
        if (carpanSure > 0) durumlar.add("⭐x2 ${ceil(carpanSure).toInt()}")
        if (gozlukSure > 0) durumlar.add("🔍 ${ceil(gozlukSure).toInt()}")
        if (durumlar.isNotEmpty()) {
            yaziBoya.textAlign = Paint.Align.CENTER
            yaziBoya.textSize = 15f * d
            yaziBoya.color = 0xFFFFB74D.toInt()
            c.drawText(durumlar.joinToString("  "), ustPanel.centerX(), y1 - 2 * d, yaziBoya)
        }

        // İlerleme çubuğu
        val by0 = ustPanel.top + 44 * d
        val by1 = ustPanel.top + 64 * d
        boya.color = 0xFF37474F.toInt()
        c.drawRoundRect(sol, by0, sag, by1, 10 * d, 10 * d, boya)
        val oran = (tahta.temizOran / 0.965f).coerceIn(0f, 1f)
        if (oran > 0.02f) {
            boya.color = if (oran > 0.8f) 0xFF66BB6A.toInt() else 0xFF4FC3F7.toInt()
            c.drawRoundRect(sol, by0, sol + (sag - sol) * oran, by1, 10 * d, 10 * d, boya)
        }
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.textSize = 13f * d
        yaziBoya.color = Color.WHITE
        c.drawText("%${(oran * 100).toInt()} temiz", (sol + sag) / 2, by1 - 5 * d, yaziBoya)

        yaziBoya.textSize = 12f * d
        yaziBoya.color = 0xFFB0BEC5.toInt()
        val turYazi = turAdi()?.let { "  ·  $it" } ?: ""
        c.drawText(
            "Bölüm $seviye$turYazi  ·  ${deterjan.emoji} ${karakter.emoji}",
            ustPanel.centerX(), ustPanel.bottom - 11 * d, yaziBoya,
        )
    }

    private fun duraklatDugmesiCiz(c: Canvas) {
        panel(c, duraklatR)
        boya.color = Color.WHITE
        val cx = duraklatR.centerX()
        val cy = duraklatR.centerY()
        val h = 18f * d
        c.drawRoundRect(cx - 8 * d, cy - h / 2, cx - 3 * d, cy + h / 2, 2 * d, 2 * d, boya)
        c.drawRoundRect(cx + 3 * d, cy - h / 2, cx + 8 * d, cy + h / 2, 2 * d, 2 * d, boya)
    }

    private fun altPanelCiz(c: Canvas) {
        panel(c, altPanel)
        for ((g, r) in gucDugmeleri) {
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

    private fun bildirimCiz(c: Canvas) {
        val b = bildirimler.firstOrNull() ?: return
        val giris = ((2.8f - b.omur) / 0.25f).coerceIn(0f, 1f)
        val cikis = (b.omur / 0.3f).coerceIn(0f, 1f)
        val k = minOf(giris, cikis)
        yaziBoya.textSize = 16f * d
        val tw = yaziBoya.measureText(b.metin) + 36 * d
        val top = ustPanel.bottom + 10 * d - (1f - k) * 30 * d
        val r = RectF((width - tw) / 2, top, (width + tw) / 2, top + 40 * d)
        boya.color = 0xFF2E7D32.toInt()
        boya.alpha = (k * 235).toInt()
        c.drawRoundRect(r, 20 * d, 20 * d, boya)
        boya.alpha = 255
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.color = Color.WHITE
        yaziBoya.alpha = (k * 255).toInt()
        c.drawText(b.metin, r.centerX(), r.centerY() + 6 * d, yaziBoya)
        yaziBoya.alpha = 255
    }

    private fun karart(alfa: Int) {
        boya.color = Color.argb(alfa, 0, 0, 0)
    }

    private fun buyukPanel(c: Canvas, r: RectF) {
        boya.style = Paint.Style.FILL
        boya.color = 0xF21E1A2E.toInt()
        c.drawRoundRect(r, 24 * d, 24 * d, boya)
        boya.style = Paint.Style.STROKE
        boya.strokeWidth = 2f * d
        boya.color = 0xFFFFB300.toInt()
        c.drawRoundRect(r, 24 * d, 24 * d, boya)
        boya.style = Paint.Style.FILL
    }

    private fun dugmeCiz(c: Canvas, b: Dugme) {
        boya.color = b.renk
        c.drawRoundRect(b.r, 16 * d, 16 * d, boya)
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.textSize = 18f * d
        yaziBoya.color = b.yaziRengi
        c.drawText(b.yazi, b.r.centerX(), b.r.centerY() + 6.5f * d, yaziBoya)
    }

    private fun hazirEkrani(c: Canvas) {
        karart(150)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boya)
        yaziBoya.textAlign = Paint.Align.CENTER
        val cx = width / 2f
        val cy = height / 2f

        yaziBoya.color = Color.WHITE
        yaziBoya.textSize = 30f * d
        c.drawText("Bölüm $seviye", cx, cy - 70 * d, yaziBoya)

        turAdi()?.let {
            yaziBoya.textSize = 17f * d
            yaziBoya.color = if (tur == Tur.HAZINE) 0xFFFFD54F.toInt() else 0xFFA5D6A7.toInt()
            val aciklama = if (tur == Tur.HAZINE) "🏆 $it: neredeyse iki kat hazine!" else "🦠 $it: temizlemezsen geri yayılır!"
            c.drawText(aciklama, cx, cy - 38 * d, yaziBoya)
        }

        yaziBoya.color = Color.WHITE
        yaziBoya.textSize = 18f * d
        c.drawText("Parmağınla halıyı ovala,", cx, cy, yaziBoya)
        c.drawText("kirin altındaki altınları bul!", cx, cy + 26 * d, yaziBoya)

        if (sureli) {
            yaziBoya.color = 0xFFB0BEC5.toInt()
            yaziBoya.textSize = 15f * d
            val r = kayit.rekor(seviye)
            val ek = if (r > 0) "  ·  Rekor: $r sn" else ""
            c.drawText("Süre: ${toplamSure.toInt()} sn$ek", cx, cy + 58 * d, yaziBoya)
        }

        yaziBoya.color = 0xFFFFD54F.toInt()
        yaziBoya.textSize = 19f * d
        yaziBoya.alpha = (180 + 75 * sin(animZaman * 5f)).toInt().coerceIn(0, 255)
        c.drawText("Başlamak için dokun", cx, cy + 104 * d, yaziBoya)
        yaziBoya.alpha = 255

        if (!kayit.egitimGoruldu) {
            // Eğitim: halının üstünde ileri geri ovalayan el
            val t = animZaman * 2.2f
            val ex = hali.centerX() + sin(t) * hali.width() * 0.3f
            val ey = hali.top + hali.height() * 0.22f + sin(t * 2f) * 12 * d
            emojiBoya.textSize = 44f * d
            emojiCiz(c, "👆", ex, ey)
        }
    }

    private fun duraklatEkrani(c: Canvas) {
        karart(170)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boya)
        buyukPanel(c, menuPanel)
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.color = Color.WHITE
        yaziBoya.textSize = 26f * d
        c.drawText("Duraklatıldı", menuPanel.centerX(), menuPanel.top + 50 * d, yaziBoya)
        yaziBoya.textSize = 14f * d
        yaziBoya.color = 0xFFB0BEC5.toInt()
        c.drawText("Bölüm $seviye  ·  %${(tahta.temizOran / 0.965f * 100).toInt().coerceAtMost(100)} temiz", menuPanel.centerX(), menuPanel.top + 76 * d, yaziBoya)
        for (b in menuDugmeleri) dugmeCiz(c, b)
    }

    private fun zipla(t: Float): Float = when {
        t < 0f -> 0f
        t < 0.2f -> t / 0.2f * 1.35f
        t < 0.35f -> 1.35f - (t - 0.2f) / 0.15f * 0.35f
        else -> 1f
    }

    private fun sonucEkrani(c: Canvas) {
        karart((sonucZaman / 0.3f).coerceIn(0f, 1f).let { (it * 175).toInt() })
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boya)

        val giris = (sonucZaman / 0.3f).coerceIn(0f, 1f)
        c.save()
        c.translate(0f, (1f - giris) * 60 * d)
        buyukPanel(c, menuPanel)
        val r = menuPanel
        val cx = r.centerX()
        yaziBoya.textAlign = Paint.Align.CENTER

        val baslik = when {
            kazandi && sureli -> "Bölüm $seviye Tamam!"
            kazandi -> "Tertemiz!"
            else -> "Süre Doldu!"
        }
        yaziBoya.textSize = 27f * d
        yaziBoya.color = if (kazandi) 0xFFFFD54F.toInt() else 0xFFEF5350.toInt()
        c.drawText(baslik, cx, r.top + 50 * d, yaziBoya)

        // Yıldızlar
        val yy = r.top + 104 * d
        if (sureli) {
            for (i in 0 until 3) {
                val sx = cx + (i - 1) * 72 * d
                yaziBoya.textSize = 46f * d
                yaziBoya.color = 0xFF37474F.toInt()
                c.drawText("★", sx, yy + 16 * d, yaziBoya)
                if (kazandi && i < yildiz) {
                    val s = zipla(sonucZaman - (0.45f + i * 0.35f))
                    if (s > 0f) {
                        emojiBoya.textSize = 44f * d * s
                        emojiCiz(c, "⭐", sx, yy)
                    }
                }
            }
        } else {
            emojiBoya.textSize = 44f * d
            emojiCiz(c, if (kazandi) "✨🧽✨" else "🧽", cx, yy)
        }

        // Satırlar
        val say = ((sonucZaman - 0.3f) / 0.9f).coerceIn(0f, 1f)
        var y = r.top + 170 * d
        fun satir(etiket: String, deger: String, renk: Int = Color.WHITE) {
            yaziBoya.textSize = 15f * d
            yaziBoya.textAlign = Paint.Align.LEFT
            yaziBoya.color = 0xFFB0BEC5.toInt()
            c.drawText(etiket, r.left + 28 * d, y, yaziBoya)
            yaziBoya.textSize = 17f * d
            yaziBoya.textAlign = Paint.Align.RIGHT
            yaziBoya.color = renk
            c.drawText(deger, r.right - 28 * d, y, yaziBoya)
            y += 31 * d
        }
        satir("Süre", "${gecen.toInt()} sn")
        if (kazandi && sureli) {
            if (yeniRekor) satir("Rekor", "YENİ REKOR! 🏅", 0xFFFFD54F.toInt()) else satir("Rekor", "$rekor sn")
        }
        satir("Bulunan altın", "+${(toplanan * say).toInt()}", 0xFFFFD54F.toInt())
        if (kazandi && sureli) satir("Hız bonusu", "+${(bonus * say).toInt()}", 0xFFFFD54F.toInt())
        satir("En iyi kombo", if (enIyiKombo >= 2) "🔥 x$enIyiKombo" else "—")
        satir("Kasadaki altın", "💰 ${kayit.altin}")

        yaziBoya.textAlign = Paint.Align.CENTER
        for (b in kazanilanBasarimlar.take(2)) {
            yaziBoya.textSize = 15f * d
            yaziBoya.color = 0xFF81C784.toInt()
            c.drawText("🏆 ${b.ad}  +${b.odul}", cx, y, yaziBoya)
            y += 24 * d
        }
        if (!kazandi) {
            yaziBoya.textSize = 13f * d
            yaziBoya.color = 0xFF90A4AE.toInt()
            c.drawText("İpucu: Mağazadan daha güçlü bir deterjan al.", cx, y + 4 * d, yaziBoya)
        }

        for (b in menuDugmeleri) dugmeCiz(c, b)
        c.restore()
    }
}
