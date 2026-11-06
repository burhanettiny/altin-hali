package com.burhanettin.altinhali

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Bir oyunun ayarları. */
class OyunAyari(
    val sureli: Boolean,
    val seviye: Int,
    val tohum: Long,
    val yaris: Boolean = false,
    val oyuncu: String? = null,
)

/**
 * Oyun ekranı. Halıyı, kiri, köpüğü, çöpleri, karakterleri ve tüm oyun içi arayüzü çizer.
 * [eylem] "tekrar", "menu" veya "yaris_sonuc" ile çağrılır.
 */
class OyunView(
    context: Context,
    private val ayar: OyunAyari,
    private val renderer: HaliRenderer?,
    private val eylem: (String) -> Unit,
) : View(context), SensorEventListener {

    /** 3B (OpenGL) çizim açık mı; açıksa zemin ve halı yüzeyi [renderer] tarafından çizilir. */
    private var glAktif = renderer != null

    /** OpenGL başarısız olursa 2B çizime dön. */
    fun glBasarisiz() {
        glAktif = false
        invalidate()
    }

    private enum class Durum { HAZIR, OYNUYOR, DURAKLADI, KUTLAMA, BITTI }
    private enum class Tur { NORMAL, HAZINE, INATCI }

    private class Dugme(val r: RectF, var yazi: String, val renk: Int, val yaziRengi: Int) {
        var eylem: () -> Unit = {}
    }

    private val kayit = Kayit(context)
    private val sureli = ayar.sureli
    private val yaris = ayar.yaris
    private val seviye = ayar.seviye
    private val deterjan = Katalog.deterjan(kayit.deterjan, kayit)
    private val karakter = Katalog.karakter(kayit.karakter)
    private val d = resources.displayMetrics.density
    private val rnd = Random(ayar.tohum)

    private val haliTuru = Katalog.haliTuru(seviye)
    private val tur = when {
        seviye % 5 == 0 -> Tur.HAZINE
        seviye >= 6 && seviye % 3 == 0 -> Tur.INATCI
        else -> Tur.NORMAL
    }

    private val SUTUN = 72
    private val SATIR = 108
    private val tahta = Tahta(SUTUN, SATIR, Katalog.katman(seviye), rnd, seviye).apply {
        var adet = (8 + seviye * 1.5f).coerceAtMost(26f)
        if (karakter.sansli) adet *= 1.4f
        if (tur == Tur.HAZINE) adet *= 1.8f
        hazineleriYerlestir(adet.toInt(), karakter.sansli, tur == Tur.HAZINE)
        copleriYerlestir(Katalog.copSayisi(seviye))
    }

    // Aletlerin leke türlerine göre etkileri (önceden hesaplanır)
    private val aletEtkisi: Map<Alet, FloatArray> = Alet.values().associateWith { a ->
        FloatArray(Leke.values().size) { li ->
            val l = Leke.values()[li]
            var e = a.guc * a.carpan(l) * 0.3f / l.zorluk
            if (a.deterjanli) e *= l.etki(deterjan.profil)
            if (l != Leke.CAMUR && l != Leke.TOZ) e *= karakter.lekeUzmani
            e * haliTuru.lekeCarpani(l)
        }
    }
    private val hasarOranlari: Map<Alet, Float> = Alet.values().associateWith { haliTuru.hasarOrani(it, deterjan) }

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
    private var alet = Alet.SUNGER

    // Kombo, sarsıntı, ses zamanlaması
    private var kombo = 0
    private var komboSure = 0f
    private var enIyiKombo = 0
    private var sarsinti = 0f
    private var sesZaman = 0f
    private var yayilZaman = 0f
    private var ipucuZaman = 0f
    private val gosterilenIpuclari = HashSet<String>()

    // Silkeleme
    private var silkeAnim = 0f
    private var silkeBekleme = 0f
    private var pencereSure = 0f

    // Şemi ve Nils
    private var semiZaman = 14f
    private var semiVar = false
    private var semiX = 0f
    private var semiY = 0f
    private var semiVx = 0f
    private var semiVy = 0f
    private var semiKizgin = false
    private var semiKacti = false
    private var semiAdim = 0f
    private var semiAdimSol = false
    private var semiDokme = 0
    private var semiDokmeZaman = 0f
    private var nilsSure = 0f
    private var nilsX = 0f
    private var nilsY = 0f

    // Acı Dede'nin çöp toplatması
    private var dedeZaman = 1f

    // Sonuç
    private var kazandi = false
    private var mahvoldu = false
    private var bonus = 0
    private var yildiz = 0
    private var rekor = 0
    private var yeniRekor = false
    private var sonucZaman = 0f
    private var calinanYildiz = 0
    private var yarisPuani = 0
    private val kazanilanBasarimlar = ArrayList<Basarim>()
    private val dedeSozu = DEDE_GIRIS[rnd.nextInt(DEDE_GIRIS.size)]

    // Yerleşim
    private val hali = RectF()
    private var hucre = 1f
    private var haliBmp: Bitmap? = null
    private var zeminBmp: Bitmap? = null
    private var tanecikBmp: Bitmap? = null
    private var kabarcikBmp: Bitmap? = null
    private val kirBmp = Bitmap.createBitmap(SUTUN, SATIR, Bitmap.Config.ARGB_8888)
    private val kopukBmp = Bitmap.createBitmap(SUTUN, SATIR, Bitmap.Config.ARGB_8888)
    private val islakBmp = Bitmap.createBitmap(SUTUN, SATIR, Bitmap.Config.ARGB_8888)
    private val hasarBmp = Bitmap.createBitmap(SUTUN, SATIR, Bitmap.Config.ARGB_8888)
    private val hasarPx = IntArray(SUTUN * SATIR)
    private var esyaKirli = true
    private var glKatmanKirli = true
    private var isikX = -0.45f
    private var isikY = -0.5f
    private val kirPx = IntArray(SUTUN * SATIR)
    private val kopukPx = IntArray(SUTUN * SATIR)
    private val islakPx = IntArray(SUTUN * SATIR)
    private var islakCizili = false
    private val ustPanel = RectF()
    private val duraklatR = RectF()
    private val altPanel = RectF()
    private val copKutusu = RectF()
    private val gucDugmeleri = Katalog.guclendiriciler.map { it to RectF() }
    private val silkeR = RectF()
    private val aletDugmeleri = Alet.values().map { it to RectF() }
    private val menuPanel = RectF()
    private val menuDugmeleri = ArrayList<Dugme>()
    private val sacak = ArrayList<FloatArray>()

    // Dokunma
    private var dokunuyor = false
    private var sonX = 0f
    private var sonY = 0f
    private var dugmeyeBasildi = false
    private var tutulan: Cop? = null

    private fun firca(a: Alet): Float {
        var r = 24f * d * a.yaricap * karakter.yaricapCarpani
        if (a.deterjanli) r *= deterjan.yaricap
        return r
    }

    // Robot
    private var robotX = SUTUN / 2f
    private var robotY = SATIR / 2f
    private var robotVx = 8f
    private var robotVy = 6f

    // Efektler
    private class Parcacik(var x: Float, var y: Float, var vx: Float, var vy: Float, var r: Float, var omur: Float, val renk: Int, val yercekimi: Float = 30f)
    private class Yazi(var x: Float, var y: Float, val metin: String, val renk: Int, var omur: Float, val boyut: Float = 22f)
    private class Bildirim(val metin: String, var omur: Float, val renk: Int)
    private val parcaciklar = ArrayList<Parcacik>()
    private val yazilar = ArrayList<Yazi>()
    private val bildirimler = ArrayList<Bildirim>()

    // Boyalar
    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpBoya = Paint(Paint.FILTER_BITMAP_FLAG)
    private val ustBoya = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
    private val yaziBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val emojiBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    // Sallama algılama
    private val sensorler = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    init {
        cizimleriGuncelle()
    }

    // ---------------------------------------------------------------- yerleşim

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val kenar = 12f * d
        duraklatR.set(w - kenar - 52f * d, kenar, w - kenar, kenar + 52f * d)
        ustPanel.set(kenar, kenar, duraklatR.left - 8f * d, kenar + 96f * d)
        altPanel.set(kenar, h - kenar - 126f * d, w - kenar, h - kenar)
        val ustSinir = ustPanel.bottom + 26f * d
        val altSinir = altPanel.top - 38f * d
        val musaitW = w - 2 * kenar - 8 * d
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
        copKutusu.set(hali.right - 58 * d, hali.bottom - 6 * d, hali.right - 6 * d, hali.bottom + 46 * d)

        val bw = hw.toInt().coerceAtLeast(1)
        val bh = hh.toInt().coerceAtLeast(1)
        haliBmp = HaliCizici.ciz(bw, bh, seviye)
        zeminBmp = HaliCizici.zeminCiz(w, h, hali, d)
        tanecikBmp = tanecikDokusu(bw / 2, bh / 2)
        kabarcikBmp = kabarcikDokusu(bw, bh)
        renderer?.let { r ->
            r.yogunluk = d
            r.hali = floatArrayOf(hali.left, hali.top, hali.right, hali.bottom)
            haliBmp?.let { r.haliVer(it) }
            zeminBmp?.let { r.zeminVer(it) }
        }
        esyaKirli = true
        glKatmanKirli = true

        // Saçaklar
        sacak.clear()
        val r = Random(seviye)
        var x = hali.left + 3f * d
        while (x < hali.right - 2 * d) {
            sacak.add(floatArrayOf(x, (10f + r.nextFloat() * 6f) * d, (r.nextFloat() - 0.5f) * 4f * d))
            x += 5f * d
        }

        // Alt panel: üst sıra güçlendiriciler + silkele, alt sıra aletler
        val bosluk = 8f * d
        val ust = gucDugmeleri.size + 1
        val uw = (altPanel.width() - bosluk * (ust + 1)) / ust
        val uy0 = altPanel.top + bosluk
        val uy1 = uy0 + 44f * d
        gucDugmeleri.forEachIndexed { i, (_, rr) ->
            val bx = altPanel.left + bosluk + i * (uw + bosluk)
            rr.set(bx, uy0, bx + uw, uy1)
        }
        val sx = altPanel.left + bosluk + gucDugmeleri.size * (uw + bosluk)
        silkeR.set(sx, uy0, sx + uw, uy1)
        val aw = (altPanel.width() - bosluk * (aletDugmeleri.size + 1)) / aletDugmeleri.size
        aletDugmeleri.forEachIndexed { i, (_, rr) ->
            val bx = altPanel.left + bosluk + i * (aw + bosluk)
            rr.set(bx, uy1 + 6 * d, bx + aw, altPanel.bottom - bosluk)
        }

        when (durum) {
            Durum.DURAKLADI -> duraklatMenusuKur()
            Durum.BITTI -> sonucDugmeleriniKur()
            else -> {}
        }
    }

    /** Kire tanecikli, gerçekçi bir görünüm veren doku. */
    private fun tanecikDokusu(w: Int, h: Int): Bitmap {
        val ww = w.coerceAtLeast(1)
        val hh = h.coerceAtLeast(1)
        val px = IntArray(ww * hh)
        val r = Random(5)
        for (i in px.indices) {
            val v = r.nextFloat()
            px[i] = when {
                v < 0.18f -> Color.argb(90 + r.nextInt(60), 20, 12, 6)
                v < 0.26f -> Color.argb(60, 255, 240, 210)
                v < 0.3f -> Color.argb(120, 0, 0, 0)
                else -> Color.TRANSPARENT
            }
        }
        val b = Bitmap.createBitmap(ww, hh, Bitmap.Config.ARGB_8888)
        b.setPixels(px, 0, ww, 0, 0, ww, hh)
        // Biraz topak/kırıntı
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat((ww * hh) / 900) {
            p.color = Color.argb(110, 40 + r.nextInt(30), 25 + r.nextInt(20), 10)
            c.drawCircle(r.nextFloat() * ww, r.nextFloat() * hh, 0.6f + r.nextFloat() * 1.6f, p)
        }
        return b
    }

    /** Köpük kabarcıkları dokusu. */
    private fun kabarcikDokusu(w: Int, h: Int): Bitmap {
        val b = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = Random(9)
        repeat((w * h) / 300) {
            val x = r.nextFloat() * w
            val y = r.nextFloat() * h
            val rr = (1.5f + r.nextFloat() * r.nextFloat() * 7f) * d
            p.style = Paint.Style.FILL
            p.color = Color.argb(70, 255, 255, 255)
            c.drawCircle(x, y, rr, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.8f * d
            p.color = Color.argb(170, 255, 255, 255)
            c.drawCircle(x, y, rr, p)
            p.style = Paint.Style.FILL
            p.color = Color.argb(220, 255, 255, 255)
            c.drawCircle(x - rr * 0.35f, y - rr * 0.35f, rr * 0.22f, p)
        }
        return b
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sensorler?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorler.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDetachedFromWindow() {
        sensorler?.unregisterListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val x = e.values[0]
        val y = e.values[1]
        val z = e.values[2]
        val ivme = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (ivme > 11f) silkele()
        // Telefon eğildikçe ışık kayar (kadife parlaması ve cila yansıması değişir)
        val hedefX = (-0.45f - x * 0.09f).coerceIn(-1f, 1f)
        val hedefY = (-0.5f + (y - 6.5f) * 0.09f).coerceIn(-1f, 1f)
        isikX += (hedefX - isikX) * 0.15f
        isikY += (hedefY - isikY) * 0.15f
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    /** Uygulama arka plana geçince çağrılır. */
    fun duraklat() {
        if (durum == Durum.OYNUYOR || (durum == Durum.HAZIR && gecen > 0f)) duraklatMenusu()
        birak()
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
            if (karakter.robot) robotuGezdir(dt)
            if (tur == Tur.INATCI) {
                yayilZaman += dt
                while (yayilZaman > 0.25f) {
                    yayilZaman -= 0.25f
                    tahta.yayil(50, tahta.katman * 0.35f)
                }
            }
            semiGuncelle(dt)
            if (karakter.copToplatir) dedeToplatir(dt)
            tahta.kuru(dt)
        }
        if (sesZaman > 0) sesZaman -= dt
        if (ipucuZaman > 0) ipucuZaman -= dt
        if (silkeAnim > 0) silkeAnim -= dt
        if (silkeBekleme > 0) silkeBekleme -= dt
        if (pencereSure > 0) pencereSure -= dt

        if (durum == Durum.KUTLAMA) {
            kutlamaSure -= dt
            if (rnd.nextFloat() < 0.7f) konfeti()
            if (kutlamaSure <= 0f) bitir(true)
        }

        if (durum == Durum.BITTI) {
            sonucZaman += dt
            if (kazandi && sureli && !yaris && calinanYildiz < yildiz && sonucZaman > 0.45f + calinanYildiz * 0.35f) {
                calinanYildiz++
                Ses.cal("yildiz", 0.8f, 0.9f + 0.15f * calinanYildiz)
                Ses.titret(15)
            }
        }

        if (sarsinti > 0) sarsinti = (sarsinti - dt).coerceAtLeast(0f)

        cizimleriGuncelle()
        if (durum == Durum.OYNUYOR && tahta.hasarOrani >= Katalog.HASAR_SINIRI) {
            mahvoldu = true
            bitir(false)
        }
        if (durum == Durum.OYNUYOR) {
            hazineleriKontrolEt()
            if (tahta.temizOran >= 0.965f) {
                if (tahta.kalanCop == 0 && tutulan == null) {
                    kazan()
                } else {
                    ipucu("cop_son", "Halı temiz! Şimdi 🥢 çöpleri çöp kutusuna at.")
                }
            } else if (tahta.kopukluSayisi > tahta.toplam * 0.12f) {
                ipucu("durula", "Köpük birikti! 🚿 Su ile durula.")
            }
        }

        for (h in tahta.hazineler) if (h.toplandi && h.animasyon < 1f) h.animasyon += dt * 2.2f
        for (c in tahta.coplar) if (c.atildi && c.animasyon < 1f) c.animasyon += dt * 2.5f

        val it1 = parcaciklar.iterator()
        while (it1.hasNext()) {
            val p = it1.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.yercekimi * d * dt
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

    private fun cizimleriGuncelle() {
        var degisti = false
        tahta.degisenler { i ->
            kirPx[i] = kirRengi(i)
            kopukPx[i] = kopukRengi(i)
            val h = tahta.hasar[i]
            hasarPx[i] = if (h <= 0f) Color.TRANSPARENT else Color.argb((h * 255).toInt().coerceIn(0, 255), 235, 224, 205)
            degisti = true
        }
        var islakDegisti = false
        if (tahta.islakVar || islakCizili) {
            for (i in islakPx.indices) {
                val w = tahta.islak[i]
                islakPx[i] = if (w <= 0f) Color.TRANSPARENT else Color.argb((w * 120).toInt(), 12, 18, 30)
            }
            islakCizili = tahta.islakVar
            islakDegisti = true
        }
        if (glAktif && renderer != null) {
            if (degisti || islakDegisti || glKatmanKirli) {
                // Islaklık alfasını 3B için tam ölçeğe çıkar
                renderer.katmanlar(SUTUN, SATIR, kirPx, kopukPx, islakPx, hasarPx)
                glKatmanKirli = false
            }
            if (esyaKirli) {
                esyaKirli = false
                esyaKatmaniGonder()
            }
        } else {
            if (degisti || glKatmanKirli) {
                kirBmp.setPixels(kirPx, 0, SUTUN, 0, 0, SUTUN, SATIR)
                kopukBmp.setPixels(kopukPx, 0, SUTUN, 0, 0, SUTUN, SATIR)
                hasarBmp.setPixels(hasarPx, 0, SUTUN, 0, 0, SUTUN, SATIR)
                glKatmanKirli = false
            }
            if (islakDegisti) islakBmp.setPixels(islakPx, 0, SUTUN, 0, 0, SUTUN, SATIR)
        }
    }

    private fun kirRengi(i: Int): Int {
        val k = tahta.kir[i]
        if (k <= 0f) return Color.TRANSPARENT
        val l = Leke.values()[tahta.tip[i].toInt()]
        val oran = (k / tahta.katman).coerceIn(0f, 1.3f)
        val tabanAlfa = if (l == Leke.SAKIZ) 0.9f else 0.32f
        val a = ((tabanAlfa + 0.72f * oran).coerceAtMost(1f) * 255).toInt()
        val t = tahta.ton[i]
        return Color.argb(
            a,
            (Color.red(l.renk) * t).toInt().coerceIn(0, 255),
            (Color.green(l.renk) * t).toInt().coerceIn(0, 255),
            (Color.blue(l.renk) * t).toInt().coerceIn(0, 255),
        )
    }

    private fun kopukRengi(i: Int): Int {
        val k = tahta.kopuk[i]
        if (k <= 0f) return Color.TRANSPARENT
        val r = deterjan.kopukRengi
        val t = 0.92f + 0.08f * tahta.ton[i]
        return Color.argb(
            (k.coerceAtMost(1f) * 215).toInt(),
            (Color.red(r) * t).toInt().coerceIn(0, 255),
            (Color.green(r) * t).toInt().coerceIn(0, 255),
            (Color.blue(r) * t).toInt().coerceIn(0, 255),
        )
    }

    /** Kirin altındaki hazineleri 3B halı dokusuna (yarım çözünürlük) çizer. */
    private fun esyaKatmaniGonder() {
        val r = renderer ?: return
        val w = (hali.width() / 2).toInt().coerceAtLeast(1)
        val h = (hali.height() / 2).toInt().coerceAtLeast(1)
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.scale(0.5f, 0.5f)
        c.translate(-hali.left, -hali.top)
        for (hz in tahta.hazineler) if (!hz.toplandi) hazineCiz(c, hz, 255, 1f)
        r.esyaVer(b)
    }

    private fun robotuGezdir(dt: Float) {
        robotX += robotVx * dt
        robotY += robotVy * dt
        if (robotX < 3f || robotX > SUTUN - 3f) { robotVx = -robotVx; robotX = robotX.coerceIn(3f, SUTUN - 3f) }
        if (robotY < 3f || robotY > SATIR - 3f) { robotVy = -robotVy; robotY = robotY.coerceIn(3f, SATIR - 3f) }
        if (rnd.nextFloat() < dt * 0.6f) {
            val aci = rnd.nextFloat() * 6.283f
            robotVx = 10f * cos(aci)
            robotVy = 10f * sin(aci)
        }
        val e = aletEtkisi.getValue(Alet.SUNGER)
        val r = FloatArray(e.size) { e[it] * dt * 4f }
        tahta.ovala(robotX, robotY, 3.6f, r, 0f, 1f)
        tahta.durula(robotX, robotY, 3.6f, dt * 1.5f, FloatArray(e.size))
    }

    private fun hazineleriKontrolEt() {
        for (h in tahta.hazineler) {
            if (!h.toplandi && tahta.kir[h.hucre] <= 0.15f) topla(h)
        }
    }

    private fun gx(x: Float) = hali.left + x * hucre
    private fun gy(y: Float) = hali.top + y * hucre
    private fun izgaraX(px: Float) = (px - hali.left) / hucre
    private fun izgaraY(py: Float) = (py - hali.top) / hucre

    private fun ipucu(anahtar: String, metin: String) {
        if (anahtar in gosterilenIpuclari || ipucuZaman > 0f) return
        gosterilenIpuclari.add(anahtar)
        ipucuZaman = 3f
        bildirimler.add(Bildirim(metin, 3.2f, 0xFF1565C0.toInt()))
    }

    // ---------------------------------------------------------------- Şemi ve Nils

    private fun semiGuncelle(dt: Float) {
        if (seviye < 3) return
        if (nilsSure > 0) nilsSure -= dt
        if (!semiVar) {
            semiZaman -= dt
            if (nilsSure > 0f && nilsSure < 0.3f && !semiVar) semiBaslat(true)
            if (semiZaman <= 0f && nilsSure <= 0f) {
                semiZaman = maxOf(10f, 28f - seviye) + rnd.nextFloat() * 8f
                if (seviye >= 4 && rnd.nextFloat() < 0.45f) {
                    nilsSure = 1.8f
                    val sag = rnd.nextBoolean()
                    nilsX = if (sag) SUTUN - 4f else 4f
                    nilsY = 8f + rnd.nextFloat() * (SATIR - 16)
                    Ses.cal("nils", 0.7f)
                } else {
                    semiBaslat(false)
                }
            }
            return
        }
        val hiz = if (semiKacti) 3f else 1f
        semiX += semiVx * dt * hiz
        semiY += semiVy * dt * hiz
        if (!semiKacti) {
            semiAdim += hypot(semiVx, semiVy) * dt
            if (semiAdim > 2.4f) {
                semiAdim = 0f
                semiAdimSol = !semiAdimSol
                val nx = -semiVy
                val ny = semiVx
                val n = hypot(nx, ny).coerceAtLeast(0.01f)
                val o = if (semiAdimSol) 0.9f else -0.9f
                tahta.kirlet(semiX + nx / n * o, semiY + ny / n * o, 1.2f, Leke.CAMUR, tahta.katman * 0.75f)
            }
            if (semiDokme > 0) {
                semiDokmeZaman -= dt
                if (semiDokmeZaman <= 0f && semiX in 6f..(SUTUN - 6f)) {
                    semiDokme--
                    semiDokmeZaman = 0.8f + rnd.nextFloat()
                    if (rnd.nextFloat() < 0.5f) {
                        val l = if (seviye >= 4 && rnd.nextBoolean()) Leke.SARAP else Leke.KAHVE
                        tahta.kirlet(semiX, semiY, 3.2f, l, tahta.katman * 1.1f)
                        yazilar.add(Yazi(gx(semiX), gy(semiY) - 30 * d, "Şemi ${l.ad.lowercase()} döktü!", 0xFFFFAB91.toInt(), 1.4f, 16f))
                    } else {
                        tahta.copEkle(semiX, semiY)
                        yazilar.add(Yazi(gx(semiX), gy(semiY) - 30 * d, "Şemi çöp attı!", 0xFFFFAB91.toInt(), 1.4f, 16f))
                    }
                    Ses.cal("semi", 0.5f)
                }
            }
        }
        if (semiX < -8f || semiX > SUTUN + 8f || semiY < -8f || semiY > SATIR + 8f) semiVar = false
    }

    private fun semiBaslat(kizgin: Boolean) {
        semiVar = true
        semiKizgin = kizgin
        semiKacti = false
        val soldan = rnd.nextBoolean()
        semiX = if (soldan) -4f else SUTUN + 4f
        semiY = 10f + rnd.nextFloat() * (SATIR - 20)
        val hedefX = if (soldan) SUTUN + 6f else -6f
        val hedefY = 10f + rnd.nextFloat() * (SATIR - 20)
        val hiz = if (kizgin) 15f else 9f
        val n = hypot(hedefX - semiX, hedefY - semiY)
        semiVx = (hedefX - semiX) / n * hiz
        semiVy = (hedefY - semiY) / n * hiz
        semiDokme = if (kizgin) 2 else if (rnd.nextFloat() < 0.35f) 1 else 0
        semiDokmeZaman = 0.8f + rnd.nextFloat()
        Ses.cal("semi", 0.7f)
        if (kizgin) {
            bildirimler.add(Bildirim("😡 Nils, Şemi'yi kızdırdı! Şemi ortalığı birbirine katıyor!", 2.6f, 0xFFC62828.toInt()))
        } else {
            ipucu("semi", "Şemi geldi! Ortalığı kirletmeden ona dokun, kaçsın.")
        }
    }

    private fun semiKov() {
        semiKacti = true
        semiDokme = 0
        val n = hypot(semiVx, semiVy).coerceAtLeast(0.01f)
        val hedefSol = semiX < SUTUN / 2
        semiVx = (if (hedefSol) -1f else 1f) * 18f
        semiVy = semiVy / n * 3f
        yazilar.add(Yazi(gx(semiX), gy(semiY) - 20 * d, "Şemi kaçtı! 🏃", 0xFF81C784.toInt(), 1.3f, 18f))
        altinVer(8, gx(semiX), gy(semiY))
        Ses.cal("semi", 0.8f, 1.4f)
        Ses.titret(25)
        if (!yaris) kayit.istatEkle("semi")
    }

    private fun dedeToplatir(dt: Float) {
        dedeZaman -= dt
        if (dedeZaman > 0f) return
        dedeZaman = 0.7f
        val c = tahta.coplar.firstOrNull { !it.atildi && !it.tutuluyor } ?: return
        copuAt(c, kendiliginden = true)
    }

    // ---------------------------------------------------------------- altın ve eşyalar

    private val komboCarpani get() = (1f + (kombo - 1) * 0.2f).coerceIn(1f, 2f)

    private fun altinVer(miktar: Int, x: Float, y: Float, kombolu: Boolean = false) {
        var m = miktar * karakter.altinCarpani * (1f + deterjan.altinBonus) * haliTuru.odulCarpani
        if (carpanSure > 0) m *= 2
        if (kombolu) m *= komboCarpani
        if (!sureli) m *= 0.5f
        val n = m.toInt().coerceAtLeast(1)
        toplanan += n
        if (!yaris) {
            kayit.altin += n
            kayit.istatEkle("altin", n)
        }
        yazilar.add(Yazi(x, y, "+$n", 0xFFFFD54F.toInt(), 1.1f))
    }

    private fun komboArtir(x: Float, y: Float) {
        kombo = if (komboSure > 0) kombo + 1 else 1
        komboSure = 2.2f
        if (kombo > enIyiKombo) {
            enIyiKombo = kombo
            if (!yaris) kayit.istatEnFazla("kombo", kombo)
        }
        if (kombo >= 2) {
            yazilar.add(Yazi(x, y - 30 * d, "KOMBO x$kombo", 0xFFFF9800.toInt(), 1.0f, 18f))
            Ses.cal("kombo", 0.5f, 1f + minOf(kombo, 8) * 0.06f)
        }
    }

    private fun topla(h: Hazine, miknatis: Boolean = false) {
        h.toplandi = true
        esyaKirli = true
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
                if (!yaris) kayit.istatEkle("elmas")
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
                if (yaris || rnd.nextFloat() < 0.6f) {
                    altinVer(40 + rnd.nextInt(110), x, y)
                } else {
                    val g = Katalog.guclendiriciler[rnd.nextInt(Katalog.guclendiriciler.size)]
                    kayit.adetEkle(g.id, 1)
                    yazilar.add(Yazi(x, y, "Hediye: +1 ${g.emoji}", 0xFFF48FB1.toInt(), 1.5f))
                }
                Ses.cal("guc", 0.7f, 1.4f)
            }
            Esya.HAYALET -> if (!miknatis) {
                if (!yaris) kayit.istatEkle("hayalet")
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

    private fun copuAt(c: Cop, kendiliginden: Boolean = false) {
        if (!c.tutuluyor) tahta.ortula(c, -1)
        c.tutuluyor = false
        c.atildi = true
        c.animasyon = 0f
        altinVer(4, copKutusu.centerX(), copKutusu.top)
        if (!yaris) kayit.istatEkle("cop")
        Ses.cal("cop", 0.8f, if (kendiliginden) 1.2f else 1f)
        Ses.titret(15)
        if (tahta.kalanCop == 0) yazilar.add(Yazi(width / 2f, hali.centerY(), "Çöpler tamam! 🗑️", 0xFFA5D6A7.toInt(), 1.3f, 20f))
    }

    private fun basarimKontrol() {
        if (yaris) return
        val yeni = Basarimlar.kontrol(kayit)
        for (b in yeni) {
            kazanilanBasarimlar.add(b)
            bildirimler.add(Bildirim("🏆 ${b.ad}   +${b.odul} 💰", 2.8f, 0xFF2E7D32.toInt()))
            Ses.cal("basarim", 0.8f)
        }
    }

    private fun kazan() {
        tahta.hepsiniTemizle()
        for (h in tahta.hazineler) if (!h.toplandi && h.tip != Esya.HAYALET) topla(h, miknatis = true)
        durum = Durum.KUTLAMA
        birak()
        kutlamaSure = 1.6f
        yazilar.add(Yazi(width / 2f, hali.centerY(), "TERTEMİZ!", 0xFFFFFFFF.toInt(), 1.6f, 34f))
        Ses.cal("kazan")
        Ses.titret(80)
    }

    private fun bitir(kazandiMi: Boolean) {
        if (durum == Durum.BITTI) return
        durum = Durum.BITTI
        birak()
        kazandi = kazandiMi
        sonucZaman = 0f
        if (yaris) {
            yarisPuani = Yaris.puan(kazandi, tahta.temizOran, kalan, toplanan)
            Yaris.sonuclar.add(
                YarisSonuc(ayar.oyuncu ?: "Oyuncu", yarisPuani, (tahta.temizOran / 0.965f * 100).toInt().coerceAtMost(100),
                    gecen.toInt(), toplanan, kazandi),
            )
        } else if (kazandi && sureli) {
            bonus = (kalan * 3f * karakter.altinCarpani * haliTuru.odulCarpani).toInt()
            val oran = kalan / toplamSure
            yildiz = when {
                oran >= 0.45f -> 3
                oran >= 0.2f -> 2
                else -> 1
            }
            val hasarYuzde = tahta.hasarOrani / Katalog.HASAR_SINIRI
            if (hasarYuzde > 0.6f) yildiz -= 2 else if (hasarYuzde > 0.25f) yildiz -= 1
            yildiz = yildiz.coerceAtLeast(1)
            bonus = (bonus * (1f - hasarYuzde.coerceIn(0f, 1f) * 0.8f)).toInt()
            kayit.altin += bonus
            kayit.istatEkle("altin", bonus)
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

    private fun birak() {
        dokunuyor = false
        tutulan?.let { copuBirak(it, hali.centerX(), hali.centerY(), zorla = true) }
    }

    // ---------------------------------------------------------------- menüler

    private fun duraklatMenusu() {
        if (durum == Durum.BITTI || durum == Durum.KUTLAMA) return
        durum = Durum.DURAKLADI
        birak()
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
        if (!yaris) yeni("↻  Yeniden Başla", 0xFF4FC3F7.toInt(), 0xFF0D2A3A.toInt()) { eylem("tekrar") }
        val ses = yeni(sesYazisi(), 0xFF455A64.toInt(), Color.WHITE) {}
        ses.eylem = {
            kayit.sesAcik = !kayit.sesAcik
            Ses.ayarla(kayit.sesAcik, kayit.titresimAcik)
            ses.yazi = sesYazisi()
        }
        yeni(if (yaris) "🏳  Yarışmadan Çık" else "🏠  Ana Menü", 0xFF37474F.toInt(), Color.WHITE) {
            if (yaris) Yaris.bitir()
            eylem("menu")
        }
    }

    private fun sonucDugmeleriniKur() {
        val pw = minOf(width - 32f * d, 380f * d)
        val ph = minOf(height - 40f * d, if (reklamTeklifi() != null) 640f * d else 580f * d)
        menuPanel.set((width - pw) / 2, (height - ph) / 2, (width + pw) / 2, (height + ph) / 2)
        menuDugmeleri.clear()
        val sol = menuPanel.left + 22 * d
        val sag = menuPanel.right - 22 * d
        val anaYazi = when {
            yaris && Yaris.sonraki != null -> "Sıradaki: ${Yaris.sonraki}  ▶"
            yaris -> "Sonuçları Gör  🏆"
            kazandi && sureli -> "Sonraki Bölüm  ▶"
            else -> "Tekrar Oyna  ↻"
        }
        val ana = Dugme(RectF(sol, menuPanel.bottom - 132 * d, sag, menuPanel.bottom - 76 * d), anaYazi, 0xFFFFB300.toInt(), 0xFF2B1B00.toInt())
        ana.eylem = {
            if (yaris) {
                Yaris.sira++
                eylem(if (Yaris.aktif) "tekrar" else "yaris_sonuc")
            } else {
                eylem(if (kazandi && sureli) "sonraki" else "tekrar")
            }
        }
        val menu = Dugme(
            RectF(sol, menuPanel.bottom - 66 * d, sag, menuPanel.bottom - 16 * d),
            if (yaris) "Yarışmadan Çık" else "Ana Menü", 0xFF37474F.toInt(), Color.WHITE,
        )
        menu.eylem = {
            if (yaris) Yaris.bitir()
            eylem("menu")
        }
        menuDugmeleri.add(ana)
        menuDugmeleri.add(menu)
        // Ödüllü reklam teklifi (isteğe bağlı)
        reklamTeklifi()?.let { (yazi, tip) ->
            val rb = Dugme(RectF(sol, menuPanel.bottom - 194 * d, sag, menuPanel.bottom - 142 * d), yazi, 0xFF26A69A.toInt(), Color.WHITE)
            rb.eylem = { eylem("reklam_$tip") }
            menuDugmeleri.add(rb)
        }
    }

    private var devamKullanildi = false
    private var katlaKullanildi = false
    private var reklamBonusu = 0

    /** Sonuç ekranında gösterilecek ödüllü reklam teklifi (yazı, tip) ya da null. */
    private fun reklamTeklifi(): Pair<String, String>? {
        if (yaris || !Reklam.odulHazir()) return null
        return when {
            !kazandi && sureli && !mahvoldu && !devamKullanildi -> "📺 Reklam izle: +20 sn devam et" to "devam"
            kazandi && !katlaKullanildi && toplanan > 0 -> "📺 Reklam izle: altınları 2 katla" to "katla"
            else -> null
        }
    }

    /** Ödüllü reklam izlendiğinde etkinlik tarafından çağrılır. */
    fun reklamOdulu(tip: String) {
        when (tip) {
            "devam" -> if (durum == Durum.BITTI && !kazandi) {
                devamKullanildi = true
                kalan = 20f
                durum = Durum.OYNUYOR
                menuDugmeleri.clear()
                yazilar.add(Yazi(width / 2f, hali.centerY(), "+20 sn! Devam!", 0xFF81D4FA.toInt(), 1.5f, 26f))
                Ses.cal("guc", 0.8f)
            }
            "katla" -> if (durum == Durum.BITTI && kazandi && !katlaKullanildi) {
                katlaKullanildi = true
                reklamBonusu = toplanan + bonus
                kayit.altin += reklamBonusu
                Ses.cal("kese", 0.9f)
                sonucDugmeleriniKur()
            }
        }
        invalidate()
    }

    // ---------------------------------------------------------------- güçlendiriciler

    private fun guclendiriciKullan(g: Guclendirici) {
        if (yaris) {
            yazilar.add(Yazi(width / 2f, altPanel.top - 10 * d, "Yarışmada güçlendirici yok!", 0xFFFFFFFF.toInt(), 1.2f, 16f))
            return
        }
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

    /** Halıyı silkele: telefonu sallayınca ya da düğmeyle. */
    private fun silkele() {
        if (durum == Durum.HAZIR) durumuBaslat()
        if (durum != Durum.OYNUYOR || silkeBekleme > 0f) return
        silkeBekleme = 0.35f
        silkeAnim = 0.45f
        pencereSure = 1.8f
        val guc = 0.3f * karakter.silkeleme * maxOf(1f, tahta.katman * 0.6f)
        tahta.silkele(guc)
        // Hafif çöpler camdan uçar
        for (c in tahta.coplar) {
            if (c.atildi || c.tutuluyor) continue
            val hafif = c.tip == CopTipi.YAPRAK || c.tip == CopTipi.SEKER || c.tip == CopTipi.GAZETE || c.tip == CopTipi.KALEM
            if (hafif && rnd.nextFloat() < 0.25f * karakter.silkeleme) {
                tahta.ortula(c, -1)
                c.atildi = true
                c.animasyon = 0f
                yazilar.add(Yazi(gx(c.x), gy(c.y), "Camdan uçtu!", 0xFFFFFFFF.toInt(), 1.1f, 15f))
            }
        }
        // Toz bulutu
        repeat(30) {
            val kenarSol = rnd.nextBoolean()
            val x = if (kenarSol) hali.left else hali.right
            val y = hali.top + rnd.nextFloat() * hali.height()
            parcaciklar.add(
                Parcacik(x, y, (if (kenarSol) -1f else 1f) * (40f + rnd.nextFloat() * 90f) * d, (rnd.nextFloat() - 0.6f) * 60f * d,
                    (3f + rnd.nextFloat() * 6f) * d, 0.7f + rnd.nextFloat() * 0.5f, 0xFFBCAAA4.toInt(), -10f),
            )
        }
        Ses.cal("silkele", 0.8f, 0.9f + rnd.nextFloat() * 0.2f)
        Ses.titret(40)
        if (!yaris) kayit.istatEkle("silkele")
    }

    private fun durumuBaslat() {
        durum = Durum.OYNUYOR
        if (!kayit.egitimGoruldu) kayit.egitimGoruldu = true
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
                // Alet seçimi
                for ((a, r) in aletDugmeleri) {
                    if (r.contains(x, y)) {
                        aletSec(a)
                        dugmeyeBasildi = true
                        return true
                    }
                }
                if (silkeR.contains(x, y)) {
                    silkele()
                    dugmeyeBasildi = true
                    return true
                }
                if (durum == Durum.OYNUYOR) {
                    for ((g, r) in gucDugmeleri) {
                        if (r.contains(x, y)) {
                            guclendiriciKullan(g)
                            dugmeyeBasildi = true
                            return true
                        }
                    }
                }
                if (altPanel.contains(x, y)) return true
                if (durum == Durum.HAZIR) durumuBaslat()

                // Şemi'yi kovala
                if (semiVar && !semiKacti && hypot(x - gx(semiX), y - gy(semiY)) < 38 * d) {
                    semiKov()
                    dugmeyeBasildi = true
                    return true
                }
                if (bombaHazir && hali.contains(x, y)) {
                    bombaHazir = false
                    tahta.patlat(izgaraX(x), izgaraY(y), 11f)
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
                if (alet == Alet.CIMBIZ) {
                    copuTut(x, y)
                } else {
                    uygula(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dokunuyor || dugmeyeBasildi || durum != Durum.OYNUYOR) return true
                if (alet == Alet.CIMBIZ) {
                    tutulan?.let {
                        it.x = izgaraX(e.x)
                        it.y = izgaraY(e.y)
                    }
                    sonX = e.x
                    sonY = e.y
                } else {
                    for (h in 0 until e.historySize) cizgi(e.getHistoricalX(h), e.getHistoricalY(h))
                    cizgi(e.x, e.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tutulan?.let { copuBirak(it, e.x, e.y) }
                dokunuyor = false
            }
        }
        return true
    }

    private fun aletSec(a: Alet) {
        if (!kayit.sahip("alet", a.id)) {
            yazilar.add(Yazi(width / 2f, altPanel.top - 10 * d, "${a.emoji} ${a.ad}: mağazadan al!", 0xFFFFFFFF.toInt(), 1.3f, 16f))
            Ses.cal("dugme", 0.5f, 0.7f)
            return
        }
        if (alet != a) {
            alet = a
            Ses.cal("alet", 0.6f)
            yazilar.add(Yazi(width / 2f, altPanel.top - 10 * d, "${a.emoji} ${a.ad}", 0xFFFFFFFF.toInt(), 0.9f, 16f))
        }
    }

    private fun copuTut(x: Float, y: Float) {
        val ix = izgaraX(x)
        val iy = izgaraY(y)
        val c = tahta.coplar.filter { !it.atildi }.minByOrNull { hypot(it.x - ix, it.y - iy) - it.boyut }
        if (c != null && hypot(c.x - ix, c.y - iy) < c.boyut + 2.5f) {
            tutulan = c
            c.tutuluyor = true
            tahta.ortula(c, -1)
            c.x = ix
            c.y = iy
            Ses.cal("cimbiz", 0.8f)
            Ses.titret(8)
        } else {
            ipucu("cimbiz_bos", "🥢 Cımbızla çöpleri tutup 🗑️ çöp kutusuna sürükle.")
        }
    }

    private fun copuBirak(c: Cop, x: Float, y: Float, zorla: Boolean = false) {
        tutulan = null
        val kutuya = !zorla && hypot(x - copKutusu.centerX(), y - copKutusu.centerY()) < 52 * d
        if (kutuya) {
            copuAt(c)
            return
        }
        c.tutuluyor = false
        c.x = c.x.coerceIn(3f, SUTUN - 3f)
        c.y = c.y.coerceIn(3f, SATIR - 3f)
        tahta.ortula(c, 1)
        if (!zorla) ipucu("kutu", "Çöpü sağ alttaki 🗑️ çöp kutusuna bırak!")
    }

    private fun cizgi(x: Float, y: Float) {
        val mesafe = hypot(x - sonX, y - sonY)
        val adim = firca(alet) * 0.35f
        val n = (mesafe / adim).toInt()
        for (s in 1..n) {
            val t = s / n.toFloat()
            uygula(sonX + (x - sonX) * t, sonY + (y - sonY) * t)
        }
        if (n > 0) {
            sonX = x
            sonY = y
        }
    }

    /** Seçili aleti bu noktaya uygular. */
    private fun uygula(x: Float, y: Float) {
        val bx = izgaraX(x)
        val by = izgaraY(y)
        val rg = firca(alet) / hucre
        val etki = aletEtkisi.getValue(alet)
        var ses = ""
        when (alet) {
            Alet.SUNGER, Alet.FIRCA -> {
                val n = tahta.ovala(bx, by, rg, etki, deterjan.kopuk / karakter.durulama, 1.35f, hasarOranlari.getValue(alet))
                if (hasarOranlari.getValue(alet) > 0.08f) {
                    ipucu("hasar_${alet.name}", if (alet == Alet.FIRCA) "Dikkat! ${haliTuru.ad} sert fırçadan yıpranıyor." else "Dikkat! Bu deterjan ${haliTuru.ad} için fazla sert, renkleri soldurur!")
                }
                if (n > 0) ses = "ovala"
                if (tahta.ortulu(bx, by)) ipucu("ortu", "Çöpün altı temizlenmez! Önce 🥢 cımbızla al.")
                tahta.lekeAt(bx, by)?.let { l ->
                    if (etki[l.ordinal] * l.zorluk < 0.16f && l.ipucu.isNotEmpty()) ipucu(l.name, l.ipucu)
                }
                if (parcaciklar.size < 220 && hali.contains(x, y)) kopukParcacigi(x, y)
            }
            Alet.SU -> {
                tahta.durula(bx, by, rg, 0.35f * karakter.durulama, etki)
                ses = "su"
                if (parcaciklar.size < 260) repeat(2) {
                    parcaciklar.add(
                        Parcacik(x + (rnd.nextFloat() - 0.5f) * firca(alet), y + (rnd.nextFloat() - 0.5f) * firca(alet),
                            (rnd.nextFloat() - 0.5f) * 40f * d, (rnd.nextFloat() - 0.2f) * 40f * d,
                            (1.5f + rnd.nextFloat() * 2.5f) * d, 0.4f + rnd.nextFloat() * 0.3f, 0xFF81D4FA.toInt(), 60f),
                    )
                }
            }
            Alet.SUPURGE -> {
                val n = tahta.ovala(bx, by, rg, etki, 0f, 1f)
                tahta.kurut(bx, by, rg, 0.25f)
                ses = "supurge"
                if (n > 0 && parcaciklar.size < 220) repeat(2) {
                    val a = rnd.nextFloat() * 6.283f
                    val rr = firca(alet) * 1.2f
                    val px = x + cos(a) * rr
                    val py = y + sin(a) * rr
                    parcaciklar.add(Parcacik(px, py, (x - px) * 3f, (y - py) * 3f, (1.5f + rnd.nextFloat() * 2f) * d, 0.33f, 0xFF8D8270.toInt(), 0f))
                }
                tahta.lekeAt(bx, by)?.let { l -> if (l != Leke.TOZ && l != Leke.CAMUR) ipucu("supurge_leke", "Süpürge sadece tozu çeker. Lekeler için 🧽 sünger kullan.") }
            }
            Alet.BUZ -> {
                val n = tahta.ovala(bx, by, rg, etki, 0f, 1f)
                if (n > 0) ses = "buz"
                if (parcaciklar.size < 220) parcaciklar.add(
                    Parcacik(x, y, (rnd.nextFloat() - 0.5f) * 60f * d, -rnd.nextFloat() * 50f * d,
                        (1.5f + rnd.nextFloat() * 2f) * d, 0.5f, 0xFFE1F5FE.toInt()),
                )
                if (n == 0) ipucu("buz", "🧊 Buz sadece sakıza işler.")
            }
            Alet.CIMBIZ -> {}
        }
        if (ses.isNotEmpty() && sesZaman <= 0f) {
            Ses.cal(ses, if (ses == "ovala") 0.22f else 0.3f, 0.85f + rnd.nextFloat() * 0.3f)
            sesZaman = 0.09f
        }
    }

    private fun kopukParcacigi(x: Float, y: Float) {
        val r = firca(alet) * 0.8f
        parcaciklar.add(
            Parcacik(
                x + (rnd.nextFloat() - 0.5f) * r, y + (rnd.nextFloat() - 0.5f) * r,
                (rnd.nextFloat() - 0.5f) * 30f * d, -rnd.nextFloat() * 30f * d,
                (2f + rnd.nextFloat() * 6f) * d, 0.5f + rnd.nextFloat() * 0.5f, deterjan.kopukRengi,
            ),
        )
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

        var kx = 0f
        var ky = 0f
        if (sarsinti > 0) {
            val g = sarsinti * 30f * d
            kx = (rnd.nextFloat() - 0.5f) * g
            ky = (rnd.nextFloat() - 0.5f) * g
        }
        if (silkeAnim > 0) {
            val t = silkeAnim / 0.45f
            ky += sin(t * 18f) * 10f * d * t
        }
        renderer?.let { r ->
            r.kaydirX = kx
            r.kaydirY = ky
            r.isikX = isikX
            r.isikY = isikY
            r.zaman = animZaman
            r.dalga = (silkeAnim / 0.45f).coerceIn(0f, 1f)
        }
        if (!glAktif) zeminBmp?.let { c.drawBitmap(it, 0f, 0f, null) } ?: c.drawColor(0xFF4E342E.toInt())
        c.save()
        c.translate(kx, ky)
        sacakCiz(c)
        if (!glAktif) {
            haliBmp?.let { c.drawBitmap(it, hali.left, hali.top, null) }
            c.drawBitmap(islakBmp, null, hali, bmpBoya)
            for (h in tahta.hazineler) if (!h.toplandi) hazineCiz(c, h, 255, 1f)
            c.drawBitmap(hasarBmp, null, hali, bmpBoya)

            // Kir: renk katmanı + tanecik dokusu (yalnızca kirli yerlere)
            val kat1 = c.saveLayer(hali, null)
            c.drawBitmap(kirBmp, null, hali, bmpBoya)
            tanecikBmp?.let { c.drawBitmap(it, null, hali, ustBoya) }
            c.restoreToCount(kat1)

            // Köpük: renk + kabarcık dokusu
            if (tahta.kopukluSayisi > 0) {
                val kat2 = c.saveLayer(hali, null)
                c.drawBitmap(kopukBmp, null, hali, bmpBoya)
                kabarcikBmp?.let { c.drawBitmap(it, null, hali, ustBoya) }
                c.restoreToCount(kat2)
            }
        }

        for (cp in tahta.coplar) if (!cp.atildi && !cp.tutuluyor) copCiz(c, cp, 1f, 255)

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
            emojiBoya.textSize = hucre * 6f
            emojiBoya.alpha = 255
            emojiCiz(c, "🤖", gx(robotX), gy(robotY))
        }
        semiCiz(c)

        for (p in parcaciklar) {
            boya.color = p.renk
            boya.alpha = (p.omur.coerceIn(0f, 1f) * 220).toInt()
            c.drawCircle(p.x, p.y, p.r, boya)
        }
        boya.alpha = 255
        c.restore()

        copKutusuCiz(c)

        // Atılan çöplerin kutuya uçuşu
        for (cp in tahta.coplar) if (cp.atildi && cp.animasyon < 1f) {
            val t = cp.animasyon
            val x = gx(cp.x) + (copKutusu.centerX() - gx(cp.x)) * t
            val y = gy(cp.y) + (copKutusu.centerY() - gy(cp.y)) * t - sin(t * Math.PI.toFloat()) * 60 * d
            c.save()
            c.translate(x - gx(cp.x), y - gy(cp.y))
            copCiz(c, cp, 1f - 0.6f * t, ((1f - t * 0.7f) * 255).toInt())
            c.restore()
        }
        tutulan?.let {
            copCiz(c, it, 1.25f, 255, golge = true)
            emojiBoya.textSize = 30 * d
            emojiCiz(c, "🥢", gx(it.x) + 16 * d, gy(it.y) - 18 * d)
        }

        if (dokunuyor && durum == Durum.OYNUYOR && alet != Alet.CIMBIZ) {
            boya.style = Paint.Style.STROKE
            boya.strokeWidth = 2f * d
            boya.color = Color.argb(120, 255, 255, 255)
            c.drawCircle(sonX, sonY, firca(alet), boya)
            boya.style = Paint.Style.FILL
            emojiBoya.textSize = 34f * d
            emojiBoya.alpha = 255
            emojiCiz(c, alet.emoji, sonX, sonY)
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

        if (pencereSure > 0) pencereCiz(c)

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

    private fun sacakCiz(c: Canvas) {
        boya.strokeCap = Paint.Cap.ROUND
        for (s in sacak) {
            val x = s[0]
            val uzun = s[1]
            val egim = s[2]
            boya.strokeWidth = 2.2f * d
            boya.color = 0xFFE9DCC0.toInt()
            c.drawLine(x, hali.top, x + egim, hali.top - uzun, boya)
            c.drawLine(x, hali.bottom, x + egim, hali.bottom + uzun, boya)
            boya.strokeWidth = 1f
            boya.color = 0x55000000
            c.drawLine(x + 1f * d, hali.top, x + egim + 1f * d, hali.top - uzun, boya)
            c.drawLine(x + 1f * d, hali.bottom, x + egim + 1f * d, hali.bottom + uzun, boya)
            // Düğüm
            boya.color = 0xFFD7C7A6.toInt()
            c.drawCircle(x, hali.top - 2.5f * d, 1.6f * d, boya)
            c.drawCircle(x, hali.bottom + 2.5f * d, 1.6f * d, boya)
        }
        boya.strokeCap = Paint.Cap.BUTT
    }

    private fun copCiz(c: Canvas, cp: Cop, olcek: Float, alfa: Int, golge: Boolean = false) {
        val x = gx(cp.x)
        val y = gy(cp.y)
        val boyut = cp.boyut * hucre * 2.1f * olcek
        // Gölge
        boya.color = Color.argb((alfa * (if (golge) 0.45f else 0.35f)).toInt(), 0, 0, 0)
        val gk = if (golge) 10 * d else 3 * d
        c.drawOval(x - boyut * 0.42f + gk, y + boyut * 0.2f + gk * 0.5f, x + boyut * 0.42f + gk, y + boyut * 0.42f + gk * 0.5f, boya)
        boya.alpha = 255
        c.save()
        c.rotate(cp.aci, x, y)
        emojiBoya.textSize = boyut
        emojiBoya.alpha = alfa
        emojiCiz(c, cp.tip.emoji, x, y)
        emojiBoya.alpha = 255
        c.restore()
    }

    private fun copKutusuCiz(c: Canvas) {
        val r = copKutusu
        val vurgu = tutulan != null
        boya.color = if (vurgu) Color.argb(200, 102, 187, 106) else Color.argb(170, 20, 18, 30)
        c.drawRoundRect(r, 14 * d, 14 * d, boya)
        if (vurgu) {
            boya.style = Paint.Style.STROKE
            boya.strokeWidth = 2f * d
            boya.color = Color.WHITE
            val s = 1f + 0.08f * sin(animZaman * 8f)
            val gw = r.width() * (s - 1f) / 2
            c.drawRoundRect(r.left - gw, r.top - gw, r.right + gw, r.bottom + gw, 14 * d, 14 * d, boya)
            boya.style = Paint.Style.FILL
        }
        emojiBoya.textSize = 30 * d
        emojiCiz(c, "🗑️", r.centerX(), r.centerY() - 2 * d)
        val kalanCop = tahta.kalanCop
        if (kalanCop > 0) {
            boya.color = 0xFFEF5350.toInt()
            c.drawCircle(r.right - 4 * d, r.top + 4 * d, 10 * d, boya)
            yaziBoya.textAlign = Paint.Align.CENTER
            yaziBoya.textSize = 12 * d
            yaziBoya.color = Color.WHITE
            c.drawText("$kalanCop", r.right - 4 * d, r.top + 8.5f * d, yaziBoya)
        }
    }

    private fun semiCiz(c: Canvas) {
        if (nilsSure > 0f) {
            val x = gx(nilsX)
            val y = gy(nilsY)
            emojiBoya.textSize = 40 * d
            emojiCiz(c, "👧", x, y)
            etiket(c, "Nils", x, y + 30 * d)
            konusmaBalonu(c, "Şemiii, yakala beni! 😜", x, y - 34 * d)
        }
        if (!semiVar) return
        val x = gx(semiX)
        val y = gy(semiY) + sin(animZaman * 16f) * 3 * d
        emojiBoya.textSize = 44 * d
        emojiCiz(c, if (semiKacti) "🏃" else "🧒", x, y)
        if (semiKizgin && !semiKacti) {
            emojiBoya.textSize = 22 * d
            emojiCiz(c, "💢", x + 18 * d, y - 22 * d)
        }
        etiket(c, "Şemi", x, y + 32 * d)
    }

    private fun etiket(c: Canvas, metin: String, x: Float, y: Float) {
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.textSize = 12 * d
        val w = yaziBoya.measureText(metin) + 10 * d
        boya.color = Color.argb(180, 0, 0, 0)
        c.drawRoundRect(x - w / 2, y - 12 * d, x + w / 2, y + 4 * d, 6 * d, 6 * d, boya)
        yaziBoya.color = Color.WHITE
        c.drawText(metin, x, y, yaziBoya)
    }

    private fun konusmaBalonu(c: Canvas, metin: String, x: Float, y: Float) {
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.textSize = 14 * d
        val w = yaziBoya.measureText(metin) + 20 * d
        val sol = (x - w / 2).coerceIn(8 * d, width - w - 8 * d)
        boya.color = Color.WHITE
        c.drawRoundRect(sol, y - 22 * d, sol + w, y + 6 * d, 12 * d, 12 * d, boya)
        yaziBoya.color = 0xFF212121.toInt()
        c.drawText(metin, sol + w / 2, y - 3 * d, yaziBoya)
    }

    /** Camdan halı silkeleyen teyze. */
    private fun pencereCiz(c: Canvas) {
        val a = (pencereSure / 0.3f).coerceIn(0f, 1f)
        val r = RectF(14 * d, ustPanel.bottom + 8 * d, 14 * d + 120 * d, ustPanel.bottom + 8 * d + 96 * d)
        c.save()
        c.translate(-(1f - a) * 140 * d, 0f)
        // Duvar ve pencere
        boya.color = 0xFFEFE6D8.toInt()
        c.drawRoundRect(r, 10 * d, 10 * d, boya)
        val cam = RectF(r.left + 10 * d, r.top + 10 * d, r.right - 10 * d, r.bottom - 26 * d)
        boya.color = 0xFF90CAF9.toInt()
        c.drawRect(cam, boya)
        boya.color = 0xFF6D4C41.toInt()
        boya.style = Paint.Style.STROKE
        boya.strokeWidth = 4 * d
        c.drawRect(cam, boya)
        c.drawLine(cam.centerX(), cam.top, cam.centerX(), cam.bottom, boya)
        boya.style = Paint.Style.FILL
        // Teyze
        val teyze = if (karakter.id == "tef") karakter.emoji else "👵"
        emojiBoya.textSize = 34 * d
        emojiCiz(c, teyze, cam.centerX(), cam.centerY() + 2 * d)
        // Pencereden sarkan, sallanan halı
        val sallan = sin(animZaman * 30f) * 6 * d
        boya.color = 0xFF8E1B2C.toInt()
        c.drawRect(cam.left + 6 * d, cam.bottom - 4 * d, cam.right - 6 * d + sallan, r.bottom - 4 * d, boya)
        boya.color = 0xFFD9A85B.toInt()
        c.drawRect(cam.left + 10 * d, cam.bottom, cam.right - 10 * d + sallan, cam.bottom + 3 * d, boya)
        c.restore()
        konusmaBalonu(c, if (karakter.id == "tef") "Tef Teyze: Silkele silkele! 💃" else "Silkele silkele!", r.centerX() + 50 * d, r.top + 6 * d)
    }

    private fun hazineCiz(c: Canvas, h: Hazine, alfa: Int, olcek: Float) {
        val x = gx(h.x)
        val y = gy(h.y)
        val r = hucre * 1.9f * olcek
        if (h.tip == Esya.PARA) {
            boya.color = 0xFF8D6608.toInt(); boya.alpha = alfa
            c.drawCircle(x + r * 0.08f, y + r * 0.1f, r, boya)
            boya.color = 0xFFB8860B.toInt(); boya.alpha = alfa
            c.drawCircle(x, y, r, boya)
            boya.color = 0xFFFFC83D.toInt(); boya.alpha = alfa
            c.drawCircle(x, y, r * 0.86f, boya)
            boya.color = 0xFFFFE082.toInt(); boya.alpha = alfa
            c.drawCircle(x - r * 0.3f, y - r * 0.3f, r * 0.28f, boya)
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

        val durumlar = ArrayList<String>()
        if (kombo >= 2) durumlar.add("🔥x$kombo")
        if (carpanSure > 0) durumlar.add("⭐x2")
        if (gozlukSure > 0) durumlar.add("🔍")
        if (tahta.kopukluSayisi > tahta.toplam * 0.04f) durumlar.add("🚿")
        if (durumlar.isNotEmpty()) {
            yaziBoya.textAlign = Paint.Align.CENTER
            yaziBoya.textSize = 15f * d
            yaziBoya.color = 0xFFFFB74D.toInt()
            c.drawText(durumlar.joinToString(" "), ustPanel.centerX(), y1 - 2 * d, yaziBoya)
        }

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

        // Hasar çubuğu (ince, ilerleme çubuğunun altında)
        val hasarYuzde = (tahta.hasarOrani / Katalog.HASAR_SINIRI).coerceIn(0f, 1f)
        if (hasarYuzde > 0.005f || haliTuru == HaliTuru.IPEK || haliTuru == HaliTuru.ANTIKA) {
            val hy0 = by1 + 3 * d
            boya.color = 0xFF3E2723.toInt()
            c.drawRoundRect(sol, hy0, sag, hy0 + 5 * d, 3 * d, 3 * d, boya)
            if (hasarYuzde > 0f) {
                boya.color = if (hasarYuzde > 0.6f && (gecen * 4).toInt() % 2 == 0) 0xFFFF1744.toInt() else 0xFFE57373.toInt()
                c.drawRoundRect(sol, hy0, sol + (sag - sol) * hasarYuzde, hy0 + 5 * d, 3 * d, 3 * d, boya)
            }
        }

        yaziBoya.textSize = 12f * d
        yaziBoya.color = 0xFFB0BEC5.toInt()
        val bas = if (yaris) "${ayar.oyuncu}" else "Bölüm $seviye"
        val turYazi = turAdi()?.let { " · $it" } ?: ""
        val hasarYazi = if (hasarYuzde > 0.005f) " · hasar %${(hasarYuzde * 100).toInt()}" else ""
        c.drawText(sigdir("$bas$turYazi · ${haliTuru.emoji} ${haliTuru.ad}$hasarYazi · ${deterjan.emoji}", ustPanel.width() - 20 * d),
            ustPanel.centerX(), ustPanel.bottom - 9 * d, yaziBoya)
    }

    /** Metni verilen genişliğe sığdırır (sonuna … koyar). */
    private fun sigdir(metin: String, enFazla: Float): String {
        if (yaziBoya.measureText(metin) <= enFazla) return metin
        var m = metin
        while (m.length > 3 && yaziBoya.measureText("$m…") > enFazla) m = m.dropLast(1)
        return "$m…"
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
            val aktif = !yaris && adet > 0 && !(g.id == "sure" && !sureli)
            val secili = (g.id == "bomba" && bombaHazir) || (g.id == "gozluk" && gozlukSure > 0)
            boya.color = if (secili) 0xFF1E88E5.toInt() else if (aktif) 0xFF455A64.toInt() else 0xFF263238.toInt()
            c.drawRoundRect(r, 12 * d, 12 * d, boya)
            emojiBoya.textSize = 20f * d
            emojiBoya.alpha = if (aktif) 255 else 80
            emojiCiz(c, g.emoji, r.centerX() - 8 * d, r.centerY())
            emojiBoya.alpha = 255
            yaziBoya.textAlign = Paint.Align.CENTER
            yaziBoya.textSize = 12f * d
            yaziBoya.color = if (aktif) Color.WHITE else 0xFF78909C.toInt()
            c.drawText("$adet", r.centerX() + 13 * d, r.centerY() + 4 * d, yaziBoya)
        }
        // Silkele
        boya.color = if (silkeAnim > 0) 0xFFEF6C00.toInt() else 0xFF6D4C41.toInt()
        c.drawRoundRect(silkeR, 12 * d, 12 * d, boya)
        emojiBoya.textSize = 20f * d
        emojiCiz(c, "🧺", silkeR.centerX(), silkeR.centerY() - 5 * d)
        yaziBoya.textSize = 10f * d
        yaziBoya.color = Color.WHITE
        c.drawText("Silkele", silkeR.centerX(), silkeR.bottom - 4 * d, yaziBoya)

        for ((a, r) in aletDugmeleri) {
            val sahip = kayit.sahip("alet", a.id)
            val secili = a == alet
            boya.color = when {
                secili -> 0xFFFFB300.toInt()
                sahip -> 0xFF455A64.toInt()
                else -> 0xFF263238.toInt()
            }
            c.drawRoundRect(r, 14 * d, 14 * d, boya)
            emojiBoya.textSize = 26f * d
            emojiBoya.alpha = if (sahip) 255 else 70
            emojiCiz(c, a.emoji, r.centerX(), r.centerY() - 7 * d)
            emojiBoya.alpha = 255
            yaziBoya.textAlign = Paint.Align.CENTER
            yaziBoya.textSize = 10f * d
            yaziBoya.color = if (secili) 0xFF2B1B00.toInt() else if (sahip) Color.WHITE else 0xFF78909C.toInt()
            c.drawText(if (sahip) a.ad.substringAfterLast(' ') else "🔒", r.centerX(), r.bottom - 5 * d, yaziBoya)
        }
    }

    private fun bildirimCiz(c: Canvas) {
        val b = bildirimler.firstOrNull() ?: return
        val giris = ((3.2f - b.omur) / 0.25f).coerceIn(0f, 1f)
        val cikis = (b.omur / 0.3f).coerceIn(0f, 1f)
        val k = minOf(giris, cikis)
        yaziBoya.textSize = 14f * d
        var metin = b.metin
        val enFazla = width - 40 * d
        while (yaziBoya.measureText(metin) + 30 * d > enFazla && metin.length > 4) metin = metin.dropLast(2)
        if (metin != b.metin) metin = metin.dropLast(1) + "…"
        val tw = yaziBoya.measureText(metin) + 30 * d
        val top = ustPanel.bottom + 10 * d - (1f - k) * 30 * d
        val r = RectF((width - tw) / 2, top, (width + tw) / 2, top + 38 * d)
        boya.color = b.renk
        boya.alpha = (k * 240).toInt()
        c.drawRoundRect(r, 19 * d, 19 * d, boya)
        boya.alpha = 255
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.color = Color.WHITE
        yaziBoya.alpha = (k * 255).toInt()
        c.drawText(metin, r.centerX(), r.centerY() + 5 * d, yaziBoya)
        yaziBoya.alpha = 255
    }

    private fun karart(c: Canvas, alfa: Int) {
        boya.color = Color.argb(alfa, 0, 0, 0)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boya)
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
        karart(c, 160)
        yaziBoya.textAlign = Paint.Align.CENTER
        val cx = width / 2f
        val cy = height / 2f

        if (yaris) {
            yaziBoya.color = 0xFFFFD54F.toInt()
            yaziBoya.textSize = 18f * d
            c.drawText("🏁 Yarışma · ${Yaris.sira + 1}. oyuncu / ${Yaris.oyuncular.size}", cx, cy - 150 * d, yaziBoya)
            yaziBoya.color = Color.WHITE
            yaziBoya.textSize = 32f * d
            c.drawText("Sıra: ${ayar.oyuncu}", cx, cy - 110 * d, yaziBoya)
            yaziBoya.textSize = 15f * d
            yaziBoya.color = 0xFFB0BEC5.toInt()
            c.drawText("Telefonu ${ayar.oyuncu}'a ver", cx, cy - 84 * d, yaziBoya)
        } else {
            yaziBoya.color = Color.WHITE
            yaziBoya.textSize = 30f * d
            c.drawText("Bölüm $seviye", cx, cy - 140 * d, yaziBoya)
            // Acı Dede işi verir
            konusmaBalonu(c, "👴 Acı Dede: $dedeSozu", cx, cy - 98 * d)
        }

        turAdi()?.let {
            yaziBoya.textSize = 16f * d
            yaziBoya.color = if (tur == Tur.HAZINE) 0xFFFFD54F.toInt() else 0xFFA5D6A7.toInt()
            val aciklama = if (tur == Tur.HAZINE) "🏆 $it: neredeyse iki kat hazine!" else "🦠 $it: temizlemezsen geri yayılır!"
            c.drawText(aciklama, cx, cy - 40 * d, yaziBoya)
        }

        yaziBoya.textSize = 14f * d
        yaziBoya.color = if (haliTuru == HaliTuru.YUN) 0xFFCFD8DC.toInt() else 0xFFFFE082.toInt()
        val kisa = when (haliTuru) {
            HaliTuru.YUN -> "dayanıklı"
            HaliTuru.SENTETIK -> "alkol eritir!"
            HaliTuru.IPEK -> "çok hassas, nazik ol!"
            HaliTuru.ANTIKA -> "çok değerli, nazik ol!"
        }
        c.drawText("${haliTuru.emoji} ${haliTuru.ad}: ${haliTuru.aciklama}".let { if (yaziBoya.measureText(it) > width - 24 * d) "${haliTuru.emoji} ${haliTuru.ad}: $kisa" else it }, cx, cy - 60 * d, yaziBoya)

        // Lekeler
        val zorlu = tahta.lekeTurleri.filter { it != Leke.CAMUR && it != Leke.TOZ }
        if (zorlu.isNotEmpty()) {
            yaziBoya.textSize = 14f * d
            yaziBoya.color = 0xFFFFAB91.toInt()
            c.drawText("Zorlu lekeler: " + zorlu.joinToString(", ") { it.ad }, cx, cy - 14 * d, yaziBoya)
        }

        yaziBoya.color = Color.WHITE
        yaziBoya.textSize = 15f * d
        c.drawText("1) 🥢 Çöpleri 🗑️ kutuya at", cx, cy + 14 * d, yaziBoya)
        c.drawText("2) 🧽 Ovala   3) 🚿 Köpüğü durula", cx, cy + 36 * d, yaziBoya)
        c.drawText("📱 Telefonu salla: halı silkelenir!", cx, cy + 58 * d, yaziBoya)

        if (sureli) {
            yaziBoya.color = 0xFFB0BEC5.toInt()
            yaziBoya.textSize = 14f * d
            val r = if (yaris) 0 else kayit.rekor(seviye)
            val ek = if (r > 0) "  ·  Rekor: $r sn" else ""
            c.drawText("Süre: ${toplamSure.toInt()} sn$ek", cx, cy + 84 * d, yaziBoya)
        }

        yaziBoya.color = 0xFFFFD54F.toInt()
        yaziBoya.textSize = 19f * d
        yaziBoya.alpha = (180 + 75 * sin(animZaman * 5f)).toInt().coerceIn(0, 255)
        c.drawText("Başlamak için dokun", cx, cy + 122 * d, yaziBoya)
        yaziBoya.alpha = 255

        if (!kayit.egitimGoruldu) {
            val t = animZaman * 2.2f
            val ex = hali.centerX() + sin(t) * hali.width() * 0.3f
            val ey = hali.bottom - hali.height() * 0.1f + sin(t * 2f) * 12 * d
            emojiBoya.textSize = 44f * d
            emojiCiz(c, "👆", ex, ey)
        }
    }

    private fun duraklatEkrani(c: Canvas) {
        karart(c, 170)
        buyukPanel(c, menuPanel)
        yaziBoya.textAlign = Paint.Align.CENTER
        yaziBoya.color = Color.WHITE
        yaziBoya.textSize = 26f * d
        c.drawText("Duraklatıldı", menuPanel.centerX(), menuPanel.top + 50 * d, yaziBoya)
        yaziBoya.textSize = 14f * d
        yaziBoya.color = 0xFFB0BEC5.toInt()
        c.drawText(
            "%${(tahta.temizOran / 0.965f * 100).toInt().coerceAtMost(100)} temiz · 🗑 ${tahta.kalanCop} çöp kaldı",
            menuPanel.centerX(), menuPanel.top + 76 * d, yaziBoya,
        )
        for (b in menuDugmeleri) dugmeCiz(c, b)
    }

    private fun zipla(t: Float): Float = when {
        t < 0f -> 0f
        t < 0.2f -> t / 0.2f * 1.35f
        t < 0.35f -> 1.35f - (t - 0.2f) / 0.15f * 0.35f
        else -> 1f
    }

    private fun sonucEkrani(c: Canvas) {
        karart(c, ((sonucZaman / 0.3f).coerceIn(0f, 1f) * 175).toInt())
        val giris = (sonucZaman / 0.3f).coerceIn(0f, 1f)
        c.save()
        c.translate(0f, (1f - giris) * 60 * d)
        buyukPanel(c, menuPanel)
        val r = menuPanel
        val cx = r.centerX()
        yaziBoya.textAlign = Paint.Align.CENTER

        val baslik = when {
            mahvoldu -> "Halı Mahvoldu!"
            yaris -> if (kazandi) "${ayar.oyuncu} bitirdi!" else "${ayar.oyuncu}: süre doldu"
            kazandi && sureli -> "Bölüm $seviye Tamam!"
            kazandi -> "Tertemiz!"
            else -> "Süre Doldu!"
        }
        yaziBoya.textSize = 25f * d
        yaziBoya.color = if (kazandi) 0xFFFFD54F.toInt() else 0xFFEF5350.toInt()
        c.drawText(baslik, cx, r.top + 48 * d, yaziBoya)

        val yy = r.top + 100 * d
        if (yaris) {
            yaziBoya.textSize = 40f * d
            yaziBoya.color = Color.WHITE
            c.drawText("${(yarisPuani * ((sonucZaman - 0.2f) / 0.8f).coerceIn(0f, 1f)).toInt()} puan", cx, yy + 14 * d, yaziBoya)
        } else if (sureli) {
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

        // Acı Dede'nin yorumu
        if (!yaris) {
            val soz = when {
                mahvoldu -> "Halımı soldurdun! Ah benim güzel halım..."
                !kazandi -> "Bu halı mı temiz? Bir daha!"
                tahta.hasarOrani / Katalog.HASAR_SINIRI > 0.25f -> "Temiz ama yer yer solmuş. Dikkat et!"
                !sureli -> "Eline sağlık evladım."
                yildiz == 3 -> "Maşallah, pırıl pırıl! Al şu bahşişi."
                yildiz == 2 -> "Fena değil... ama köşelere bak."
                else -> "Hıh. İdare eder."
            }
            yaziBoya.textSize = 14f * d
            yaziBoya.color = 0xFFFFE0B2.toInt()
            c.drawText("👴 Acı Dede: $soz", cx, r.top + 146 * d, yaziBoya)
        }

        val say = ((sonucZaman - 0.3f) / 0.9f).coerceIn(0f, 1f)
        var y = r.top + 180 * d
        fun satir(etiket: String, deger: String, renk: Int = Color.WHITE) {
            yaziBoya.textSize = 15f * d
            yaziBoya.textAlign = Paint.Align.LEFT
            yaziBoya.color = 0xFFB0BEC5.toInt()
            c.drawText(etiket, r.left + 28 * d, y, yaziBoya)
            yaziBoya.textSize = 17f * d
            yaziBoya.textAlign = Paint.Align.RIGHT
            yaziBoya.color = renk
            c.drawText(deger, r.right - 28 * d, y, yaziBoya)
            y += 30 * d
        }
        satir("Süre", "${gecen.toInt()} sn")
        if (yaris) satir("Temizlik", "%${(tahta.temizOran / 0.965f * 100).toInt().coerceAtMost(100)}")
        if (kazandi && sureli && !yaris) {
            if (yeniRekor) satir("Rekor", "YENİ REKOR! 🏅", 0xFFFFD54F.toInt()) else satir("Rekor", "$rekor sn")
        }
        satir("Bulunan altın", "+${(toplanan * say).toInt()}", 0xFFFFD54F.toInt())
        if (kazandi && sureli && !yaris) satir("Hız bonusu", "+${(bonus * say).toInt()}", 0xFFFFD54F.toInt())
        satir("En iyi kombo", if (enIyiKombo >= 2) "🔥 x$enIyiKombo" else "—")
        val hy = (tahta.hasarOrani / Katalog.HASAR_SINIRI * 100).toInt().coerceAtMost(100)
        if (hy > 0) satir("Halı hasarı", "%$hy", if (hy > 25) 0xFFEF5350.toInt() else 0xFFFFCC80.toInt())
        if (reklamBonusu > 0) satir("Reklam bonusu", "+$reklamBonusu", 0xFF80CBC4.toInt())
        if (!yaris) satir("Kasadaki altın", "💰 ${kayit.altin}")

        yaziBoya.textAlign = Paint.Align.CENTER
        for (b in kazanilanBasarimlar.take(2)) {
            yaziBoya.textSize = 15f * d
            yaziBoya.color = 0xFF81C784.toInt()
            c.drawText("🏆 ${b.ad}  +${b.odul}", cx, y, yaziBoya)
            y += 24 * d
        }
        if (!kazandi && !yaris) {
            yaziBoya.textSize = 13f * d
            yaziBoya.color = 0xFF90A4AE.toInt()
            c.drawText(
                if (mahvoldu) "İpucu: Hassas halıda sünger ve yumuşak formül kullan." else "İpucu: Lekeye uygun deterjan ya da 🧪 formül kullan.",
                cx, y + 4 * d, yaziBoya,
            )
        }

        for (b in menuDugmeleri) dugmeCiz(c, b)
        c.restore()
    }

    companion object {
        private val DEDE_GIRIS = listOf(
            "Bu halı akşama tertemiz olacak!",
            "Misafir geliyor, çabuk ol!",
            "Bu halı babamdan kaldı, dikkat et!",
            "Şemi yine ortalığı batırmış...",
            "Hadi bakalım, göster kendini!",
            "Halı yıkamak sabır ister evladım.",
        )
    }
}
