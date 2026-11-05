package com.burhanettin.altinhali

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class OyunActivity : Activity() {

    private lateinit var oyun: OyunView
    private var sureli = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sureli = intent.getBooleanExtra(EK_SURELI, true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        oyun = OyunView(this, sureli) { sonucGoster(it) }
        setContentView(oyun)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    override fun onPause() {
        super.onPause()
        oyun.duraklat()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (oyun.geriBasildi()) {
            AlertDialog.Builder(this)
                .setTitle("Oyundan çık?")
                .setMessage("Topladığın altınlar cebinde kalır.")
                .setPositiveButton("Çık") { _, _ -> finish() }
                .setNegativeButton("Devam", null)
                .show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun sonucGoster(s: Sonuc) {
        if (isFinishing) return
        val kayit = Kayit(this)
        val baslik: String
        val mesaj = StringBuilder()
        if (s.kazandi) {
            baslik = if (s.sureli) "Bölüm ${s.seviye} tamam! " + "⭐".repeat(s.yildiz) else "Halı tertemiz! ✨"
            mesaj.append("Süre: ${s.gecenSure.toInt()} sn\n")
            mesaj.append("Bulunan altın: ${s.toplanan}\n")
            if (s.sureli) mesaj.append("Hız bonusu: ${s.bonus}\n")
        } else {
            baslik = "Süre doldu! ⏰"
            mesaj.append("Halının bir kısmı hâlâ kirli.\n")
            mesaj.append("Bulunan altın: ${s.toplanan} (cebinde kaldı)\n\n")
            mesaj.append("İpucu: Mağazadan daha güçlü bir deterjan ya da ekstra süre al.")
        }
        mesaj.append("\nToplam altın: 💰 ${kayit.altin}")

        val b = AlertDialog.Builder(this)
            .setTitle(baslik)
            .setMessage(mesaj.toString())
            .setCancelable(false)
            .setNegativeButton("Menü") { _, _ -> finish() }
        if (s.kazandi && s.sureli) {
            b.setPositiveButton("Sonraki bölüm") { _, _ -> yeniden() }
        } else {
            b.setPositiveButton("Tekrar") { _, _ -> yeniden() }
        }
        b.show()
    }

    private fun yeniden() {
        startActivity(Intent(this, OyunActivity::class.java).putExtra(EK_SURELI, sureli))
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EK_SURELI = "sureli"
    }
}
