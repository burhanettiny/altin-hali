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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Ana menü, mağaza ve karakter ekranları (hepsi koddan kurulur, XML yok). */
class MainActivity : Activity() {

    private enum class Ekran { MENU, MAGAZA, KARAKTER }

    private lateinit var kayit: Kayit
    private var ekran = Ekran.MENU
    private val d by lazy { resources.displayMetrics.density }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kayit = Kayit(this)
        if (!kayit.sahip("baslangic", "hediye")) {
            // İlk açılışta küçük bir hediye.
            kayit.sahiplen("baslangic", "hediye")
            kayit.adetEkle("sure", 1)
            kayit.adetEkle("gozluk", 1)
        }
    }

    override fun onResume() {
        super.onResume()
        goster(ekran)
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
        }
    }

    // ------------------------------------------------------------ yardımcılar

    private fun dp(v: Int) = (v * d).toInt()

    private fun kok(icerik: LinearLayout.() -> Unit) {
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            icerik()
        }
        val kaydir = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF3E1F47.toInt(), 0xFF1B2A35.toInt()),
            )
            addView(kap)
        }
        setContentView(kaydir)
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

    private fun LinearLayout.dugme(metin: String, renk: Int = 0xFFFFB300.toInt(), yaziRengi: Int = 0xFF2B1B00.toInt(), tik: () -> Unit): Button {
        val b = Button(this@MainActivity).apply {
            text = metin
            textSize = 18f
            isAllCaps = false
            setTextColor(yaziRengi)
            background = yuvarlak(renk)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener { tik() }
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

    /** Mağaza ve karakter ekranı için kart. */
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
            background = yuvarlak(if (vurgulu) 0xFF2E7D32.toInt() else 0x33FFFFFF)
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
                setOnClickListener { tik() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        addView(kart, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })
    }

    private fun bolumBasligi(ll: LinearLayout, metin: String) {
        ll.yazi(metin, 20f, 0xFFB3E5FC.toInt(), kalin = true, alt = 10).apply {
            gravity = Gravity.START
            setPadding(0, dp(12), 0, 0)
        }
    }

    private fun yetersiz() = Toast.makeText(this, "Yeterli altının yok. Biraz daha halı yıka! 🧽", Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------ ekranlar

    private fun menu() = kok {
        val det = Katalog.deterjan(kayit.deterjan)
        val kar = Katalog.karakter(kayit.karakter)
        yazi("🧽", 64f, alt = 0)
        yazi("Altın Halı", 40f, 0xFFFFD54F.toInt(), kalin = true, alt = 2)
        yazi("Halıyı yıka, kirin altındaki altınları bul!", 15f, 0xFFCFD8DC.toInt(), alt = 22)
        altinSatiri()
        yazi("⭐ ${kayit.toplamYildiz} yıldız   ·   Bölüm ${kayit.seviye}", 15f, 0xFFCFD8DC.toInt(), alt = 4)
        yazi("${kar.emoji} ${kar.ad}  ·  ${det.emoji} ${det.ad}", 15f, 0xFFCFD8DC.toInt(), alt = 26)

        dugme("▶  Süreli Oyun  (Bölüm ${kayit.seviye})") {
            startActivity(Intent(this@MainActivity, OyunActivity::class.java).putExtra(OyunActivity.EK_SURELI, true))
        }
        dugme("🧘  Rahat Mod  (süre yok)", 0xFF4FC3F7.toInt(), 0xFF0D2A3A.toInt()) {
            startActivity(Intent(this@MainActivity, OyunActivity::class.java).putExtra(OyunActivity.EK_SURELI, false))
        }
        dugme("🛒  Mağaza", 0xFF81C784.toInt(), 0xFF0F2E12.toInt()) { goster(Ekran.MAGAZA) }
        dugme("🧑  Karakterler", 0xFFBA68C8.toInt(), 0xFF2A0F30.toInt()) { goster(Ekran.KARAKTER) }
        dugme("❓  Nasıl oynanır?", 0x33FFFFFF, Color.WHITE) { nasilOynanir() }
    }

    private fun magaza() = kok {
        baslik("🛒 Mağaza")

        bolumBasligi(this, "Deterjanlar")
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
                }
                kayit.deterjan = dt.id
                goster(Ekran.MAGAZA)
            }
        }

        bolumBasligi(this, "Güçlendiriciler (oyun içinde kullanılır)")
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
                goster(Ekran.MAGAZA)
            }
        }
        dugme("← Geri", 0x33FFFFFF, Color.WHITE) { goster(Ekran.MENU) }
    }

    private fun karakterler() = kok {
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
                }
                kayit.karakter = k.id
                goster(Ekran.KARAKTER)
            }
        }
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
                    "  👻 Hayalet: 4 saniye kaybettirir!\n\n" +
                    "⏱ Süreli modda halıyı ne kadar hızlı temizlersen o kadar çok hız bonusu ve yıldız alırsın. " +
                    "Her bölümde kir kalınlaşır.\n\n" +
                    "🛒 Altınlarla daha güçlü deterjanlar, güçlendiriciler ve yetenekli karakterler satın al.\n\n" +
                    "🧘 Rahat modda süre yoktur ama altınların yarısını kazanırsın.",
            )
            .setPositiveButton("Anladım", null)
            .show()
    }
}
