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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        oyun = OyunView(this, sureli) { eylem ->
            when (eylem) {
                "tekrar" -> yeniden()
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

    private fun yeniden() {
        startActivity(Intent(this, OyunActivity::class.java).putExtra(EK_SURELI, sureli))
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EK_SURELI = "sureli"
    }
}
