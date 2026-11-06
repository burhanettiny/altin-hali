package com.burhanettin.altinhali

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.TimeZone

/** Ana menü, mağaza, karakterler, başarımlar ve ayarlar (hepsi koddan kurulur, XML yok). */
class MainActivity : Activity() {

    private enum class Ekran { MENU, MAGAZA, KARAKTER, BASARIM, AYAR, LAB, YARIS, YARIS_SONUC, BOLUMLER }

    private lateinit var kayit: Kayit
    private lateinit var kok: FrameLayout
    private var icerik: ScrollView? = null
    private var ekran = Ekran.MENU
    private var sonEkran: Ekran? = null
    private var labSlot = "A"
    private var labMiktar: IntArray? = null
    private var yarisSayisi = 2
    private var yarisZorluk = 3
    private val yarisIsimleri = arrayOf("Şemi", "Nils", "Güzi", "Gül")
    private val d by lazy { resources.displayMetrics.density }

    private val GUNLUK_ODUL = intArrayOf(50, 75, 100, 150, 200, 300, 500)

    companion object {
        const val EK_YARIS_SONUC = "yaris_sonuc"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kayit = Kayit(this)
        Ses.baslat(this)
        ilkHediye()
        kok = FrameLayout(this)
        kok.addView(MenuArka(this), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(kok)
        if (intent.getBooleanExtra(EK_YARIS_SONUC, false)) ekran = Ekran.YARIS_SONUC
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EK_YARIS_SONUC, false)) ekran = Ekran.YARIS_SONUC
    }

    private fun ilkHediye() {
        if (!kayit.sahip("baslangic", "hediye")) {
            kayit.sahiplen("baslangic", "hediye")
            kayit.adetEkle("sure", 1)
            kayit.adetEkle("gozluk", 1)
        }
    }

    override fun onResume() {
        super.onResume()
        Ses.muzikBaslat(0.25f)
        Reklam.baslat(this)
        goster(ekran)
        if (ekran == Ekran.MENU) gunlukOdul()
    }

    override fun onPause() {
        super.onPause()
        Ses.muzikDurdur()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (ekran != Ekran.MENU) {
            goster(Ekran.MENU)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun goster(e: Ekran) {
        ekran = e
        when (e) {
            Ekran.MENU -> menu()
            Ekran.MAGAZA -> magaza()
            Ekran.KARAKTER -> karakterler()
            Ekran.BASARIM -> basarimlar()
            Ekran.AYAR -> ayarlar()
            Ekran.LAB -> lab()
            Ekran.YARIS -> yarisKur()
            Ekran.YARIS_SONUC -> yarisSonuc()
            Ekran.BOLUMLER -> bolumler()
        }
    }

    // ------------------------------------------------------------ günlük ödül

    private fun bugun(): Long {
        val simdi = System.currentTimeMillis()
        return (simdi + TimeZone.getDefault().getOffset(simdi)) / 86_400_000L
    }

    private fun gunlukOdul() {
        val gun = bugun()
        val son = kayit.sonOdulGunu
        if (gun <= son) return
        val seri = if (son == gun - 1) (kayit.seri % 7) + 1 else 1
        kayit.seri = seri
        kayit.sonOdulGunu = gun
        val odul = GUNLUK_ODUL[seri - 1]
        kayit.altin += odul
        if (seri == 7) kayit.adetEkle("miknatis", 1)

        val takvim = StringBuilder()
        for (i in 1..7) {
            takvim.append(
                when {
                    i < seri -> "✅"
                    i == seri -> "🎁"
                    else -> "⬜"
                },
            )
            if (i < 7) takvim.append(" ")
        }
        val ek = if (seri == 7) "\n+1 🧲 Altın Mıknatısı" else ""
        Ses.cal("basarim", 0.8f)
        AlertDialog.Builder(this)
            .setTitle("🎁 Günlük Ödül — $seri. gün")
            .setMessage("$takvim\n\n💰 +$odul altın$ek\n\nHer gün gel, ödül büyüsün! 7. gün en büyük ödül seni bekliyor.")
            .setPositiveButton("Topla") { _, _ -> goster(Ekran.MENU) }
            .setCancelable(false)
            .apply {
                if (Reklam.odulHazir()) {
                    setNeutralButton("📺 2 katına çıkar") { _, _ ->
                        val gosterildi = Reklam.odulluGoster(this@MainActivity) {
                            kayit.altin += odul
                            Ses.cal("kese", 0.9f)
                            Toast.makeText(this@MainActivity, "🎁 Günlük ödül 2 katına çıktı: +$odul", Toast.LENGTH_SHORT).show()
                            goster(Ekran.MENU)
                        }
                        if (!gosterildi) goster(Ekran.MENU)
                    }
                }
            }
            .show()
    }

    // ------------------------------------------------------------ yardımcılar

    private fun dp(v: Int) = (v * d).toInt()

    private fun sayfa(icerikKur: LinearLayout.() -> Unit) {
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            icerikKur()
        }
        val kaydir = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.TRANSPARENT)
            addView(kap)
        }
        val eskiY = if (ekran == sonEkran) icerik?.scrollY ?: 0 else 0
        sonEkran = ekran
        icerik?.let { kok.removeView(it) }
        icerik = kaydir
        kaydir.post { kaydir.scrollTo(0, eskiY) }
        kok.addView(kaydir, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun LinearLayout.yazi(metin: String, boyut: Float, renk: Int = Color.WHITE, kalin: Boolean = false, alt: Int = 8): TextView {
        val t = TextView(this@MainActivity).apply {
            text = metin
            textSize = boyut
            setTextColor(renk)
            gravity = Gravity.CENTER
            if (kalin) setTypeface(typeface, Typeface.BOLD)
        }
        addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(alt)
        })
        return t
    }

    private fun yuvarlak(renk: Int, yaricap: Int = 16) = GradientDrawable().apply {
        setColor(renk)
        cornerRadius = dp(yaricap).toFloat()
    }

    private fun LinearLayout.dugme(
        metin: String,
        renk: Int = 0xFFFFB300.toInt(),
        yaziRengi: Int = 0xFF2B1B00.toInt(),
        boyut: Float = 18f,
        tik: () -> Unit,
    ): Button {
        val b = Button(this@MainActivity).apply {
            text = metin
            textSize = boyut
            isAllCaps = false
            setTextColor(yaziRengi)
            setTypeface(typeface, Typeface.BOLD)
            background = yuvarlak(renk)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            setOnClickListener {
                Ses.cal("dugme", 0.7f)
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }.start()
                tik()
            }
        }
        addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })
        return b
    }

    private fun LinearLayout.altinSatiri() {
        yazi("💰 ${kayit.altin} altın", 22f, 0xFFFFD54F.toInt(), kalin = true, alt = 16)
    }

    private fun LinearLayout.baslik(metin: String) {
        yazi(metin, 28f, kalin = true, alt = 6)
        altinSatiri()
    }

    /** Mağaza, karakter ve başarım ekranları için kart. */
    private fun LinearLayout.kart(
        emoji: String,
        ad: String,
        aciklama: String,
        durumYazisi: String,
        dugmeYazisi: String?,
        dugmeAktif: Boolean,
        vurgulu: Boolean,
        tik: () -> Unit,
    ) {
        val kart = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = yuvarlak(if (vurgulu) 0xCC2E7D32.toInt() else 0x33FFFFFF)
        }
        kart.addView(TextView(this@MainActivity).apply {
            text = emoji
            textSize = 34f
        }, LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.WRAP_CONTENT))

        val orta = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        orta.addView(TextView(this@MainActivity).apply {
            text = ad
            textSize = 17f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        })
        orta.addView(TextView(this@MainActivity).apply {
            text = aciklama
            textSize = 13f
            setTextColor(0xFFCFD8DC.toInt())
        })
        orta.addView(TextView(this@MainActivity).apply {
            text = durumYazisi
            textSize = 13f
            setTextColor(0xFFFFD54F.toInt())
        })
        kart.addView(orta, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        if (dugmeYazisi != null) {
            kart.addView(Button(this@MainActivity).apply {
                text = dugmeYazisi
                isAllCaps = false
                textSize = 14f
                isEnabled = dugmeAktif
                setTextColor(if (dugmeAktif) 0xFF2B1B00.toInt() else 0xFF90A4AE.toInt())
                background = yuvarlak(if (dugmeAktif) 0xFFFFB300.toInt() else 0xFF455A64.toInt(), 12)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                stateListAnimator = null
                setOnClickListener { tik() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        addView(kart, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })
    }

    private fun LinearLayout.bolumBasligi(metin: String) {
        yazi(metin, 20f, 0xFFB3E5FC.toInt(), kalin = true, alt = 10).apply {
            gravity = Gravity.START
            setPadding(0, dp(12), 0, 0)
        }
    }

    private fun yetersiz() = Toast.makeText(this, "Yeterli altının yok. Biraz daha halı yıka! 🧽", Toast.LENGTH_SHORT).show()

    private fun satinAlindi() {
        Ses.cal("satin", 0.8f)
        Ses.titret(20)
        for (b in Basarimlar.kontrol(kayit)) {
            Ses.cal("basarim", 0.8f)
            Toast.makeText(this, "🏆 Başarım: ${b.ad}  +${b.odul} altın", Toast.LENGTH_LONG).show()
        }
    }

    private fun oyna(sureli: Boolean, seviye: Int = kayit.seviye) {
        startActivity(
            Intent(this, OyunActivity::class.java)
                .putExtra(OyunActivity.EK_SURELI, sureli)
                .putExtra(OyunActivity.EK_SEVIYE, seviye),
        )
    }

    // ------------------------------------------------------------ ekranlar

    private fun menu() = sayfa {
        val det = Katalog.deterjan(kayit.deterjan, kayit)
        val kar = Katalog.karakter(kayit.karakter)
        yazi("🧽", 64f, alt = 0)
        yazi("Altın Halı", 42f, 0xFFFFD54F.toInt(), kalin = true, alt = 2).apply {
            setShadowLayer(8f * d, 0f, 3f * d, 0xAA000000.toInt())
        }
        yazi("Halıyı yıka, kirin altındaki altınları bul!", 15f, 0xFFCFD8DC.toInt(), alt = 22)
        altinSatiri()
        val alinan = Basarimlar.liste.count { kayit.basarimAlindi(it.id) }
        yazi("⭐ ${kayit.toplamYildiz} yıldız   ·   🏆 $alinan/${Basarimlar.liste.size}", 15f, 0xFFCFD8DC.toInt(), alt = 4)
        yazi("${kar.emoji} ${kar.ad}  ·  ${det.emoji} ${det.ad}", 15f, 0xFFCFD8DC.toInt(), alt = 24)

        val sv = kayit.seviye
        val ozel = when {
            sv % 5 == 0 -> "  🏆"
            sv >= 6 && sv % 3 == 0 -> "  🦠"
            else -> ""
        }
        dugme("▶  Oyna — Bölüm $sv$ozel", boyut = 20f) { oyna(true) }
        dugme("🗺  Bölüm Haritası", 0xFF9575CD.toInt(), 0xFF1A0F33.toInt()) { goster(Ekran.BOLUMLER) }
        dugme("🧘  Rahat Mod  (süre yok)", 0xFF4FC3F7.toInt(), 0xFF0D2A3A.toInt()) { oyna(false) }
        dugme("🏁  Yarışma  (2–4 kişi)", 0xFFFF7043.toInt(), 0xFF3A1206.toInt()) { goster(Ekran.YARIS) }
        dugme("🛒  Mağaza", 0xFF81C784.toInt(), 0xFF0F2E12.toInt()) { goster(Ekran.MAGAZA) }
        dugme("🧪  Formül Laboratuvarı", 0xFF4DB6AC.toInt(), 0xFF06302B.toInt()) { labMiktar = null; goster(Ekran.LAB) }
        dugme("🧑  Karakterler", 0xFFBA68C8.toInt(), 0xFF2A0F30.toInt()) { goster(Ekran.KARAKTER) }
        dugme("🏆  Başarımlar", 0xFFFFD54F.toInt(), 0xFF3A2A06.toInt()) { goster(Ekran.BASARIM) }

        val satir = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        addView(satir, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val sol = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        val sag = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        satir.addView(sol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) })
        satir.addView(sag, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6) })
        sol.dugme("⚙️  Ayarlar", 0x33FFFFFF, Color.WHITE, 16f) { goster(Ekran.AYAR) }
        sag.dugme("❓  Nasıl?", 0x33FFFFFF, Color.WHITE, 16f) { nasilOynanir() }
    }

    private fun magaza() = sayfa {
        baslik("🛒 Mağaza")

        bolumBasligi("Deterjanlar")
        for (dt in Katalog.deterjanlar) {
            val sahip = kayit.sahip("deterjan", dt.id)
            val secili = kayit.deterjan == dt.id
            val iyi = dt.iyiOlduklari().filter { it != Leke.CAMUR && it != Leke.TOZ }.joinToString(", ") { it.ad }
            val ozellik = "Genişlik x${"%.2f".format(dt.yaricap)}" + if (iyi.isNotEmpty()) " · İyi: $iyi" else ""
            kart(
                dt.emoji, dt.ad, "${dt.aciklama}\n$ozellik",
                when { secili -> "Kullanılıyor"; sahip -> "Sende var"; else -> "💰 ${dt.fiyat}" },
                when { secili -> null; sahip -> "Kullan"; else -> "Satın al" },
                dugmeAktif = sahip || kayit.altin >= dt.fiyat,
                vurgulu = secili,
            ) {
                if (!sahip) {
                    if (!kayit.harca(dt.fiyat)) { yetersiz(); return@kart }
                    kayit.sahiplen("deterjan", dt.id)
                    satinAlindi()
                } else {
                    Ses.cal("dugme", 0.7f)
                }
                kayit.deterjan = dt.id
                goster(Ekran.MAGAZA)
            }
        }

        bolumBasligi("Aletler")
        for (a in Alet.values()) {
            val sahip = kayit.sahip("alet", a.id)
            kart(
                a.emoji, a.ad, a.aciklama,
                if (sahip) "Sende var (oyunda alttan seç)" else "💰 ${a.fiyat}",
                if (sahip) null else "Satın al",
                dugmeAktif = kayit.altin >= a.fiyat,
                vurgulu = false,
            ) {
                if (!kayit.harca(a.fiyat)) { yetersiz(); return@kart }
                kayit.sahiplen("alet", a.id)
                satinAlindi()
                goster(Ekran.MAGAZA)
            }
        }

        bolumBasligi("Kendi deterjanını yap")
        dugme("🧪  Formül Laboratuvarı", 0xFF4DB6AC.toInt(), 0xFF06302B.toInt(), 16f) { labMiktar = null; goster(Ekran.LAB) }

        bolumBasligi("Güçlendiriciler (oyun içinde kullanılır)")
        val bekleme = 4 * 60 * 60 * 1000L
        val kalanMs = kayit.sonBedavaGuc + bekleme - System.currentTimeMillis()
        if (Reklam.ACIK) {
            if (kalanMs <= 0 && Reklam.odulHazir()) {
                dugme("📺  Reklam izle: bedava güçlendirici", 0xFF26A69A.toInt(), Color.WHITE, 16f) {
                    Reklam.odulluGoster(this@MainActivity) {
                        val g = Katalog.guclendiriciler.random()
                        kayit.adetEkle(g.id, 1)
                        kayit.sonBedavaGuc = System.currentTimeMillis()
                        Ses.cal("satin", 0.8f)
                        Toast.makeText(this@MainActivity, "Bedava: +1 ${g.emoji} ${g.ad}", Toast.LENGTH_SHORT).show()
                        goster(Ekran.MAGAZA)
                    }
                }
            } else if (kalanMs > 0) {
                yazi("📺 Sonraki bedava güçlendirici: ${kalanMs / 3_600_000} sa ${(kalanMs / 60_000) % 60} dk sonra", 13f, 0xFF80CBC4.toInt(), alt = 10)
            }
        }
        for (g in Katalog.guclendiriciler) {
            kart(
                g.emoji, g.ad, g.aciklama,
                "Elinde: ${kayit.adet(g.id)}  ·  💰 ${g.fiyat}",
                "+1 al",
                dugmeAktif = kayit.altin >= g.fiyat,
                vurgulu = false,
            ) {
                if (!kayit.harca(g.fiyat)) { yetersiz(); return@kart }
                kayit.adetEkle(g.id, 1)
                satinAlindi()
                goster(Ekran.MAGAZA)
            }
        }
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun karakterler() = sayfa {
        baslik("🧑 Karakterler")
        for (k in Katalog.karakterler) {
            val sahip = kayit.sahip("karakter", k.id)
            val secili = kayit.karakter == k.id
            kart(
                k.emoji, k.ad, k.aciklama,
                when { secili -> "Seçili"; sahip -> "Sende var"; else -> "💰 ${k.fiyat}" },
                when { secili -> null; sahip -> "Seç"; else -> "Satın al" },
                dugmeAktif = sahip || kayit.altin >= k.fiyat,
                vurgulu = secili,
            ) {
                if (!sahip) {
                    if (!kayit.harca(k.fiyat)) { yetersiz(); return@kart }
                    kayit.sahiplen("karakter", k.id)
                    satinAlindi()
                } else {
                    Ses.cal("dugme", 0.7f)
                }
                kayit.karakter = k.id
                goster(Ekran.KARAKTER)
            }
        }
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun basarimlar() = sayfa {
        val alinan = Basarimlar.liste.count { kayit.basarimAlindi(it.id) }
        yazi("🏆 Başarımlar", 28f, kalin = true, alt = 6)
        yazi("$alinan / ${Basarimlar.liste.size} tamamlandı", 16f, 0xFFFFD54F.toInt(), alt = 16)
        for (b in Basarimlar.liste) {
            val tamam = kayit.basarimAlindi(b.id)
            val ilerleme = b.ilerleme(kayit).coerceAtMost(b.hedef)
            val durum = if (tamam) "✅ Tamamlandı · +${b.odul} altın alındı" else "$ilerleme / ${b.hedef}  ·  Ödül: 💰 ${b.odul}"
            kart(if (tamam) b.emoji else "🔒", b.ad, b.aciklama, durum, null, false, tamam) {}
        }

        bolumBasligi("İstatistikler")
        val istat = listOf(
            "Bitirilen bölüm" to kayit.istat("bolum").toString(),
            "Toplam kazanılan altın" to kayit.istat("altin").toString(),
            "Bulunan elmas" to kayit.istat("elmas").toString(),
            "En iyi kombo" to "x${kayit.istat("kombo")}",
            "En hızlı bölüm" to (kayit.istat("hizli").takeIf { it > 0 }?.let { "$it sn" } ?: "—"),
            "Karşılaşılan hayalet" to kayit.istat("hayalet").toString(),
        )
        for ((ad, deger) in istat) {
            val s = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = yuvarlak(0x22FFFFFF, 12)
            }
            s.addView(TextView(this@MainActivity).apply {
                text = ad
                textSize = 15f
                setTextColor(0xFFCFD8DC.toInt())
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            s.addView(TextView(this@MainActivity).apply {
                text = deger
                textSize = 15f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(s, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            })
        }
        yazi("", 8f)
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun ayarlar() = sayfa {
        yazi("⚙️ Ayarlar", 28f, kalin = true, alt = 20)
        dugme(if (kayit.sesAcik) "🔊  Ses: Açık" else "🔇  Ses: Kapalı", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.sesAcik = !kayit.sesAcik
            Ses.ayarla(kayit.sesAcik, kayit.titresimAcik)
            goster(Ekran.AYAR)
        }
        dugme(if (kayit.muzikAcik) "🎵  Müzik: Açık" else "🎵  Müzik: Kapalı", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.muzikAcik = !kayit.muzikAcik
            Ses.ayarla(kayit.sesAcik, kayit.titresimAcik, kayit.muzikAcik)
            goster(Ekran.AYAR)
        }
        dugme(if (kayit.titresimAcik) "📳  Titreşim: Açık" else "📴  Titreşim: Kapalı", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.titresimAcik = !kayit.titresimAcik
            Ses.ayarla(kayit.sesAcik, kayit.titresimAcik)
            Ses.titret(40)
            goster(Ekran.AYAR)
        }
        dugme(if (kayit.grafik3d) "✨  3B Grafik: Açık" else "▫️  3B Grafik: Kapalı (2B)", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.grafik3d = !kayit.grafik3d
            goster(Ekran.AYAR)
        }
        yazi("3B grafik halıyı gerçek zamanlı ışıkla çizer; telefonu eğince parlaklık değişir. Telefon ısınırsa ya da yavaşlarsa kapatın.", 12f, 0xFF90A4AE.toInt(), alt = 12)
        dugme("🎓  Eğitimi tekrar göster", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.egitimGoruldu = false
            Toast.makeText(this@MainActivity, "Bir sonraki oyunda eğitim gösterilecek.", Toast.LENGTH_SHORT).show()
        }
        if (Reklam.gizlilikSecenegiGerekli()) {
            dugme("🔒  Reklam ve gizlilik tercihleri", 0xFF455A64.toInt(), Color.WHITE) { Reklam.gizlilikFormu(this@MainActivity) }
        }
        dugme("🗑  İlerlemeyi sıfırla", 0xFFC62828.toInt(), Color.WHITE) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Emin misin?")
                .setMessage("Tüm altınların, bölümlerin, satın aldıkların ve başarımların silinecek. Bu geri alınamaz.")
                .setPositiveButton("Sıfırla") { _, _ ->
                    kayit.sifirla()
                    ilkHediye()
                    Ses.ayarla(true, true)
                    goster(Ekran.MENU)
                }
                .setNegativeButton("Vazgeç", null)
                .show()
        }
        yazi("Altın Halı · sürüm 0.5", 13f, 0xFF90A4AE.toInt(), alt = 16)
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    // ------------------------------------------------------------ formül laboratuvarı

    private fun lab() = sayfa {
        val miktar = labMiktar ?: kayit.formul(labSlot).also { labMiktar = it }
        baslik("🧪 Formül Laboratuvarı")
        yazi("Malzemeleri karıştır, zorlu lekelere özel deterjanını yap. En fazla ${Katalog.FORMUL_EN_FAZLA} ölçü.", 14f, 0xFFCFD8DC.toInt(), alt = 12)

        // Yuvalar
        val yuvalar = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        addView(yuvalar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) })
        for (slot in Katalog.formulSlotlari) {
            val secili = slot == labSlot
            val kullaniliyor = kayit.deterjan == "formul_$slot"
            yuvalar.addView(Button(this@MainActivity).apply {
                text = "Formül $slot" + if (kullaniliyor) " ✓" else ""
                isAllCaps = false
                textSize = 14f
                setTextColor(if (secili) 0xFF06302B.toInt() else Color.WHITE)
                background = yuvarlak(if (secili) 0xFF4DB6AC.toInt() else 0x33FFFFFF, 12)
                stateListAnimator = null
                setOnClickListener {
                    Ses.cal("dugme", 0.6f)
                    labSlot = slot
                    labMiktar = kayit.formul(slot)
                    goster(Ekran.LAB)
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
        }

        val toplam = miktar.sum()
        bolumBasligi("Malzemeler  ($toplam/${Katalog.FORMUL_EN_FAZLA} ölçü)")
        Katalog.malzemeler.forEachIndexed { i, m ->
            val sahip = kayit.sahip("malzeme", m.id)
            val satir = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(8), dp(8))
                background = yuvarlak(if (miktar[i] > 0) 0x5526A69A else 0x33FFFFFF, 14)
            }
            satir.addView(TextView(this@MainActivity).apply { text = m.emoji; textSize = 28f },
                LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT))
            val orta = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            orta.addView(TextView(this@MainActivity).apply {
                text = m.ad
                textSize = 15f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
            })
            orta.addView(TextView(this@MainActivity).apply {
                text = m.aciklama
                textSize = 12f
                setTextColor(0xFFCFD8DC.toInt())
            })
            satir.addView(orta, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            fun kucukDugme(yazi: String, aktif: Boolean, tik: () -> Unit) = Button(this@MainActivity).apply {
                text = yazi
                isAllCaps = false
                textSize = 16f
                isEnabled = aktif
                setTextColor(if (aktif) 0xFF2B1B00.toInt() else 0xFF90A4AE.toInt())
                background = yuvarlak(if (aktif) 0xFFFFB300.toInt() else 0xFF455A64.toInt(), 10)
                minWidth = 0
                minimumWidth = 0
                setPadding(dp(10), dp(4), dp(10), dp(4))
                stateListAnimator = null
                setOnClickListener { tik() }
            }
            if (sahip) {
                satir.addView(kucukDugme("−", miktar[i] > 0) {
                    miktar[i]--
                    Ses.cal("dugme", 0.5f, 0.8f)
                    goster(Ekran.LAB)
                }, LinearLayout.LayoutParams(dp(42), dp(42)))
                satir.addView(TextView(this@MainActivity).apply {
                    text = "${miktar[i]}"
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setTypeface(typeface, Typeface.BOLD)
                }, LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT))
                satir.addView(kucukDugme("+", miktar[i] < Katalog.MALZEME_EN_FAZLA && toplam < Katalog.FORMUL_EN_FAZLA) {
                    miktar[i]++
                    Ses.cal("dugme", 0.5f, 1.2f)
                    goster(Ekran.LAB)
                }, LinearLayout.LayoutParams(dp(42), dp(42)))
            } else {
                satir.addView(kucukDugme("💰 ${m.fiyat}", kayit.altin >= m.fiyat) {
                    if (!kayit.harca(m.fiyat)) { yetersiz(); return@kucukDugme }
                    kayit.sahiplen("malzeme", m.id)
                    satinAlindi()
                    goster(Ekran.LAB)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)))
            }
            addView(satir, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        }

        // Sonuç
        val sonuc = Katalog.formulHesapla(miktar)
        bolumBasligi("Formülün gücü")
        for (o in Ozellik.values()) cubuk(o.ad, sonuc.profil[o.ordinal] / 2.6f, o.renk, "%.1f".format(sonuc.profil[o.ordinal]))
        cubuk("Köpük (durulama gerekir)", sonuc.kopuk / 1.2f, 0xFFECEFF1.toInt(), "%.1f".format(sonuc.kopuk))
        if (sonuc.altinBonus > 0f) cubuk("Altın bonusu", sonuc.altinBonus / 0.24f, 0xFFFFD54F.toInt(), "+%${(sonuc.altinBonus * 100).toInt()}")

        val det = Katalog.formulDeterjani(labSlot, miktar)
        val iyi = Leke.values().filter { it.etki(det.profil) >= 1.0f }
        val zayif = Leke.values().filter { it.etki(det.profil) < 0.45f }
        yazi("✅ İyi geldiği: " + (iyi.joinToString(", ") { it.ad }.ifEmpty { "—" }), 14f, 0xFFA5D6A7.toInt(), alt = 4).gravity = Gravity.START
        yazi("❌ Zayıf kaldığı: " + (zayif.joinToString(", ") { it.ad }.ifEmpty { "—" }), 14f, 0xFFEF9A9A.toInt(), alt = 8).gravity = Gravity.START
        val guvenlik = HaliTuru.values().joinToString("   ") { t ->
            val h = t.hasarOrani(Alet.SUNGER, det)
            "${t.emoji} ${t.ad.substringBefore(' ')}: " + when { h < 0.04f -> "güvenli"; h < 0.12f -> "dikkat"; else -> "SOLDURUR" }
        }
        yazi("🛡 Sünger ile: $guvenlik", 13f, 0xFFB3E5FC.toInt(), alt = 8).gravity = Gravity.START
        for (u in sonuc.uyarilar) yazi(u, 13f, 0xFFFFE082.toInt(), alt = 4).gravity = Gravity.START
        yazi("", 6f)

        dugme("💾  Kaydet ve Kullan (Formül $labSlot)", 0xFF4DB6AC.toInt(), 0xFF06302B.toInt()) {
            kayit.formulKaydet(labSlot, miktar)
            kayit.deterjan = "formul_$labSlot"
            Ses.cal("satin", 0.8f)
            Toast.makeText(this@MainActivity, "🧪 Formül $labSlot kaydedildi ve kullanılıyor!", Toast.LENGTH_SHORT).show()
            goster(Ekran.LAB)
        }
        dugme("↺  Sıfırla", 0x33FFFFFF, Color.WHITE, 15f) {
            labMiktar = IntArray(Katalog.malzemeler.size).also { it[0] = 2 }
            goster(Ekran.LAB)
        }
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun LinearLayout.cubuk(ad: String, oran: Float, renk: Int, deger: String) {
        val kap = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        kap.addView(TextView(this@MainActivity).apply {
            text = ad
            textSize = 13f
            setTextColor(0xFFCFD8DC.toInt())
        }, LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT))
        val iz = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = yuvarlak(0x33FFFFFF, 6)
        }
        val o = oran.coerceIn(0f, 1f)
        iz.addView(android.view.View(this@MainActivity).apply { background = yuvarlak(renk, 6) },
            LinearLayout.LayoutParams(0, dp(12), o.coerceAtLeast(0.001f)))
        iz.addView(android.view.View(this@MainActivity), LinearLayout.LayoutParams(0, dp(12), (1f - o).coerceAtLeast(0.001f)))
        kap.addView(iz, LinearLayout.LayoutParams(0, dp(12), 1f))
        kap.addView(TextView(this@MainActivity).apply {
            text = deger
            textSize = 13f
            gravity = Gravity.END
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(kap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) })
    }

    // ------------------------------------------------------------ yarışma

    private val isimKutulari = ArrayList<EditText>()

    private fun yarisKur() = sayfa {
        yazi("🏁 Yarışma", 28f, kalin = true, alt = 4)
        yazi("Aynı telefonda sırayla oynayın. Herkes aynı halıyı temizler, en yüksek puan kazanır!", 14f, 0xFFCFD8DC.toInt(), alt = 16)

        bolumBasligi("Kaç kişi?")
        val sayi = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        addView(sayi, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        for (n in 2..4) {
            sayi.addView(Button(this@MainActivity).apply {
                text = "$n kişi"
                isAllCaps = false
                textSize = 15f
                val secili = n == yarisSayisi
                setTextColor(if (secili) 0xFF3A1206.toInt() else Color.WHITE)
                background = yuvarlak(if (secili) 0xFFFF7043.toInt() else 0x33FFFFFF, 12)
                stateListAnimator = null
                setOnClickListener { isimleriSakla(); yarisSayisi = n; goster(Ekran.YARIS) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
        }

        bolumBasligi("Oyuncular")
        isimKutulari.clear()
        for (i in 0 until yarisSayisi) {
            val e = EditText(this@MainActivity).apply {
                setText(yarisIsimleri[i])
                setSelectAllOnFocus(true)
                textSize = 17f
                setTextColor(Color.WHITE)
                setHintTextColor(0xFF90A4AE.toInt())
                hint = "${i + 1}. oyuncu"
                isSingleLine = true
                background = yuvarlak(0x33FFFFFF, 12)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            isimKutulari.add(e)
            addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        }

        bolumBasligi("Zorluk")
        val zorluk = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        addView(zorluk, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })
        for ((ad, sv) in listOf("Kolay" to 1, "Orta" to 4, "Zor" to 8, "Usta" to 12)) {
            zorluk.addView(Button(this@MainActivity).apply {
                text = ad
                isAllCaps = false
                textSize = 14f
                val secili = sv == yarisZorluk
                setTextColor(if (secili) 0xFF3A1206.toInt() else Color.WHITE)
                background = yuvarlak(if (secili) 0xFFFF7043.toInt() else 0x33FFFFFF, 12)
                stateListAnimator = null
                setOnClickListener { isimleriSakla(); yarisZorluk = sv; goster(Ekran.YARIS) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
        }

        dugme("▶  Yarışı Başlat", 0xFFFF7043.toInt(), 0xFF3A1206.toInt(), 20f) {
            isimleriSakla()
            val isimler = (0 until yarisSayisi).map { yarisIsimleri[it].trim().ifEmpty { "${it + 1}. oyuncu" } }
            Yaris.basla(isimler, yarisZorluk)
            startActivity(Intent(this@MainActivity, OyunActivity::class.java)
                .putExtra(OyunActivity.EK_SURELI, true)
                .putExtra(OyunActivity.EK_YARIS, true))
        }
        yazi("Puan: halıyı bitiren 1000 + kalan saniye × 20 + bulduğu altın. Bitiremeyen temizlediği kadar puan alır. Yarışmada güçlendirici kullanılmaz.", 12f, 0xFF90A4AE.toInt(), alt = 12)
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun isimleriSakla() {
        isimKutulari.forEachIndexed { i, e -> if (i < yarisIsimleri.size) yarisIsimleri[i] = e.text.toString() }
    }

    private fun yarisSonuc() = sayfa {
        val sirali = Yaris.sonuclar.sortedByDescending { it.puan }
        yazi("🏆", 56f, alt = 0)
        yazi("Yarışma Sonuçları", 28f, kalin = true, alt = 6)
        sirali.firstOrNull()?.let { yazi("Kazanan: ${it.ad}!", 22f, 0xFFFFD54F.toInt(), kalin = true, alt = 16) }
        val madalya = listOf("🥇", "🥈", "🥉", "🎗️")
        sirali.forEachIndexed { i, s ->
            kart(
                madalya.getOrElse(i) { "🎗️" }, "${s.ad}  —  ${s.puan} puan",
                if (s.bitirdi) "Halıyı ${s.sure} saniyede bitirdi" else "Süre doldu · %${s.temizYuzde} temiz",
                "💰 ${s.altin} altın buldu", null, false, i == 0,
            ) {}
        }
        if (sirali.isEmpty()) yazi("Henüz sonuç yok.", 16f)
        yazi("", 8f)
        dugme("↻  Rövanş", 0xFFFF7043.toInt(), 0xFF3A1206.toInt()) {
            val isimler = sirali.map { it.ad }.ifEmpty { yarisIsimleri.take(yarisSayisi) }
            Yaris.basla(Yaris.sonuclar.map { it.ad }.ifEmpty { isimler }, Yaris.seviye)
            startActivity(Intent(this@MainActivity, OyunActivity::class.java)
                .putExtra(OyunActivity.EK_SURELI, true)
                .putExtra(OyunActivity.EK_YARIS, true))
        }
        dugme("🏠  Ana Menü", 0x33FFFFFF, Color.WHITE) {
            Yaris.bitir()
            goster(Ekran.MENU)
        }
    }

    // ------------------------------------------------------------ bölüm haritası

    private fun bolumler() = sayfa {
        yazi("🗺 Bölüm Haritası", 28f, kalin = true, alt = 4)
        yazi("⭐ ${kayit.toplamYildiz} yıldız · Eski bölümleri tekrar oyna, 3 yıldızın peşine düş!", 14f, 0xFFCFD8DC.toInt(), alt = 14)
        val acik = kayit.seviye
        val son = acik + 3
        var satir: LinearLayout? = null
        for (sv in 1..son) {
            if ((sv - 1) % 4 == 0) {
                satir = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
                addView(satir, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
            }
            val kilitli = sv > acik
            val y = kayit.enIyiYildiz(sv)
            val ozel = when {
                sv % 5 == 0 -> "🏆"
                sv >= 6 && sv % 3 == 0 -> "🦠"
                else -> Katalog.haliTuru(sv).let { if (it == HaliTuru.YUN) "" else it.emoji }
            }
            val yildizlar = if (kilitli) "🔒" else "★".repeat(y) + "☆".repeat(3 - y)
            satir?.addView(Button(this@MainActivity).apply {
                text = "$sv $ozel\n$yildizlar"
                isAllCaps = false
                textSize = 15f
                isEnabled = !kilitli
                setTextColor(if (kilitli) 0xFF78909C.toInt() else if (sv == acik) 0xFF2B1B00.toInt() else Color.WHITE)
                background = yuvarlak(
                    when {
                        kilitli -> 0x22FFFFFF
                        sv == acik -> 0xFFFFB300.toInt()
                        y == 3 -> 0xCC2E7D32.toInt()
                        else -> 0x44FFFFFF
                    }, 14,
                )
                stateListAnimator = null
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener {
                    Ses.cal("dugme", 0.7f)
                    oyna(true, sv)
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        }
        yazi("🏆 Hazine Halısı · 🦠 İnatçı Kir · 🧵 Sentetik · 🧣 İpek · 🏺 Antika", 12f, 0xFF90A4AE.toInt(), alt = 12)
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun nasilOynanir() {
        AlertDialog.Builder(this)
            .setTitle("Nasıl oynanır?")
            .setMessage(
                "🧽 Parmağını halının üstünde gezdirerek kiri ov. Ne kadar çok ovarsan o kadar temizlenir.\n\n" +
                    "💰 Kirin altında altınlar ve sürprizler saklı:\n" +
                    "  ₺ Altın para, 💰 kese, 💎 elmas\n" +
                    "  ⏰ +5 saniye   ⭐ 10 sn çift altın\n" +
                    "  💣 Köpük patlaması   🎁 Hediye\n" +
                    "  👻 Hayalet: 4 saniye kaybettirir ve komboyu bozar!\n\n" +
                    "🔥 Kombo: Değerli eşyaları art arda hızlıca bulursan her biri daha çok altın verir.\n\n" +
                    "⏱ Süreli modda halıyı ne kadar hızlı temizlersen o kadar çok hız bonusu ve yıldız alırsın.\n\n" +
                    "🥢 Halının üstündeki çöpleri cımbızla tutup 🗑️ çöp kutusuna at. Çöpün altı temizlenmez!\n\n" +
                    "🚿 Deterjan köpük bırakır: köpüğü su ile durula. Islak halı daha kolay temizlenir.\n\n" +
                    "📱 Telefonu salla ya da 🧺 düğmesine bas: halı silkelenir, toz ve hafif çöpler dökülür.\n\n" +
                    "🧪 Zorlu lekeler: kahve, yağ, şarap, pas, mürekkep, sakız. Her biri farklı kimya ister. Formül Laboratuvarında kendi deterjanını yap!\n\n" +
                    "🧒 Şemi ortalığı kirletir, Nils onu kızdırınca daha da beter olur. Şemi'ye dokun, kaçsın!\n\n" +
                    "🏆 Her 5. bölüm bir Hazine Halısı: neredeyse iki kat hazine!\n" +
                    "🦠 İnatçı kir bölümlerinde temizlemediğin kir yeniden yayılır.\n\n" +
                    "🛒 Altınlarla daha güçlü deterjanlar, güçlendiriciler ve yetenekli karakterler al.\n\n" +
                    "🎁 Her gün oyuna gir, günlük ödülünü topla.\n\n" +
                    "🧘 Rahat modda süre yoktur ama altınların yarısını kazanırsın.",
            )
            .setPositiveButton("Anladım", null)
            .show()
    }
}
