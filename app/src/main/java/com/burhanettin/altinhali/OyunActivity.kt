package com.burhanettin.altinhali

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class OyunActivity : Activity() {

    private lateinit var oyun: OyunView
    private var sureli = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ses.baslat(this)
        sureli = intent.getBooleanExtra(EK_SURELI, true)
        val yaris = intent.getBooleanExtra(EK_YARIS, false) && Yaris.aktif
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val ayar = if (yaris) {
            OyunAyari(sureli = true, seviye = Yaris.seviye, tohum = Yaris.tohum, yaris = true, oyuncu = Yaris.siradaki)
        } else {
            OyunAyari(sureli = sureli, seviye = Kayit(this).seviye, tohum = System.nanoTime())
        }
        oyun = OyunView(this, ayar) { eylem ->
            when (eylem) {
                "tekrar" -> yeniden(yaris)
                "yaris_sonuc" -> {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .putExtra(MainActivity.EK_YARIS_SONUC, true)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                    finish()
                }
                else -> finish()
            }
        }
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
        if (!oyun.geriBasildi()) finish()
    }

    private fun yeniden(yaris: Boolean) {
        startActivity(Intent(this, OyunActivity::class.java).putExtra(EK_SURELI, sureli).putExtra(EK_YARIS, yaris))
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EK_SURELI = "sureli"
        const val EK_YARIS = "yaris"
    }
}
