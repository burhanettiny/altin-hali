package com.burhanettin.altinhali

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.widget.FrameLayout
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher

class OyunActivity : Activity() {

    private lateinit var oyun: OyunView
    private var glGorunum: GLSurfaceView? = null
    private var sureli = true
    private var seviye = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ses.baslat(this)
        sureli = intent.getBooleanExtra(EK_SURELI, true)
        val yaris = intent.getBooleanExtra(EK_YARIS, false) && Yaris.aktif
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val ayar = if (yaris) {
            OyunAyari(sureli = true, seviye = Yaris.seviye, tohum = Yaris.tohum, yaris = true, oyuncu = Yaris.siradaki)
        } else {
            val enFazla = Kayit(this).seviye
            seviye = intent.getIntExtra(EK_SEVIYE, enFazla).coerceIn(1, enFazla)
            OyunAyari(sureli = sureli, seviye = seviye, tohum = System.nanoTime())
        }
        // 3B: cihaz OpenGL ES 3.0 destekliyorsa ve ayarlarda açıksa
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val gl3 = am.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
        var renderer: HaliRenderer? = null
        if (gl3 && Kayit(this).grafik3d) {
            renderer = HaliRenderer(applicationContext) { _ ->
                runOnUiThread {
                    oyun.glBasarisiz()
                    glGorunum?.visibility = android.view.View.GONE
                }
            }
            glGorunum = GLSurfaceView(this).apply {
                setEGLContextClientVersion(3)
                preserveEGLContextOnPause = true
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
        oyun = OyunView(this, ayar, renderer) { eylem ->
            when (eylem) {
                "tekrar" -> yeniden(yaris, seviye)
                "sonraki" -> Reklam.gecisGoster(this, seviye) { yeniden(false, minOf(seviye + 1, Kayit(this).seviye)) }
                "reklam_devam", "reklam_katla" -> {
                    val tip = eylem.removePrefix("reklam_")
                    if (!Reklam.odulluGoster(this) { oyun.reklamOdulu(tip) }) {
                        android.widget.Toast.makeText(this, "Reklam şu an hazır değil, biraz sonra dene.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
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
        val kok = FrameLayout(this)
        glGorunum?.let { kok.addView(it, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)) }
        kok.addView(oyun, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(kok)
        // Android 13+ (ve Android 16'yı hedefleyen uygulamalarda zorunlu): yeni geri hareketi
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { geri() }
        }
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
        glGorunum?.onPause()
        Ses.muzikDurdur()
    }

    override fun onResume() {
        super.onResume()
        glGorunum?.onResume()
        Ses.muzikBaslat(0.18f)
        Reklam.baslat(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = geri()

    private fun geri() {
        if (!oyun.geriBasildi()) finish()
    }

    private fun yeniden(yaris: Boolean, sv: Int) {
        startActivity(
            Intent(this, OyunActivity::class.java)
                .putExtra(EK_SURELI, sureli).putExtra(EK_YARIS, yaris).putExtra(EK_SEVIYE, sv),
        )
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EK_SURELI = "sureli"
        const val EK_YARIS = "yaris"
        const val EK_SEVIYE = "seviye"
    }
}
