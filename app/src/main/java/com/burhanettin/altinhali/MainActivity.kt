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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.TimeZone

/** Ana menü, mağaza, karakterler, başarımlar ve ayarlar (hepsi koddan kurulur, XML yok). */
class MainActivity : Activity() {

    private enum class Ekran { MENU, MAGAZA, KARAKTER, BASARIM, AYAR }

    private lateinit var kayit: Kayit
    private lateinit var kok: FrameLayout
    private var icerik: ScrollView? = null
    private var ekran = Ekran.MENU
    private val d by lazy { resources.displayMetrics.density }

    private val GUNLUK_ODUL = intArrayOf(50, 75, 100, 150, 200, 300, 500)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kayit = Kayit(this)
        Ses.baslat(this)
        ilkHediye()
        kok = FrameLayout(this)
        kok.addView(MenuArka(this), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(kok)
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
        goster(ekran)
        if (ekran == Ekran.MENU) gunlukOdul()
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
        icerik?.let { kok.removeView(it) }
        icerik = kaydir
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

    private fun oyna(sureli: Boolean) {
        startActivity(Intent(this, OyunActivity::class.java).putExtra(OyunActivity.EK_SURELI, sureli))
    }

    // ------------------------------------------------------------ ekranlar

    private fun menu() = sayfa {
        val det = Katalog.deterjan(kayit.deterjan)
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
        dugme("🧘  Rahat Mod  (süre yok)", 0xFF4FC3F7.toInt(), 0xFF0D2A3A.toInt()) { oyna(false) }
        dugme("🛒  Mağaza", 0xFF81C784.toInt(), 0xFF0F2E12.toInt()) { goster(Ekran.MAGAZA) }
        dugme("🧑  Karakterler", 0xFFBA68C8.toInt(), 0xFF2A0F30.toInt()) { goster(Ekran.KARAKTER) }
        dugme("🏆  Başarımlar", 0xFFFF8A65.toInt(), 0xFF3A1206.toInt()) { goster(Ekran.BASARIM) }

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
            val ozellik = "Genişlik x${"%.2f".format(dt.yaricap)} · Güç x${"%.1f".format(dt.guc)}"
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

        bolumBasligi("Güçlendiriciler (oyun içinde kullanılır)")
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
        dugme(if (kayit.titresimAcik) "📳  Titreşim: Açık" else "📴  Titreşim: Kapalı", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.titresimAcik = !kayit.titresimAcik
            Ses.ayarla(kayit.sesAcik, kayit.titresimAcik)
            Ses.titret(40)
            goster(Ekran.AYAR)
        }
        dugme("🎓  Eğitimi tekrar göster", 0xFF455A64.toInt(), Color.WHITE) {
            kayit.egitimGoruldu = false
            Toast.makeText(this@MainActivity, "Bir sonraki oyunda eğitim gösterilecek.", Toast.LENGTH_SHORT).show()
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
        yazi("Altın Halı · sürüm 0.2", 13f, 0xFF90A4AE.toInt(), alt = 16)
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
