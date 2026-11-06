package com.burhanettin.altinhali

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google AdMob reklamları.
 *
 * UYGULAMA KİMLİĞİ GERÇEK (manifest); REKLAM BİRİMLERİ HENÜZ GOOGLE'IN TEST BİRİMLERİ: yalnızca "Test Ad" etiketli deneme reklamları
 * görünür ve para kazandırmaz. Yayına çıkarken AdMob hesabındaki kendi kimliklerini aşağıya ve
 * AndroidManifest.xml'deki APPLICATION_ID'ye yaz (bkz. YAYIN.md). Kendi reklamına asla kendin tıklama.
 *
 * Reklamlar oyunu bölmez: ödüllü reklamlar tamamen isteğe bağlıdır (oyuncu ödül için kendisi seçer),
 * geçiş reklamı ise en erken 6. bölümden sonra, en fazla 3 bölümde bir ve 3 dakikada bir gösterilir.
 */
object Reklam {
    /** Reklamları tamamen kapatmak için false yap. */
    const val ACIK = true

    // Google'ın herkese açık TEST reklam birimleri
    private const val ODULLU_BIRIM = "ca-app-pub-3940256099942544/5224354917"
    private const val GECIS_BIRIM = "ca-app-pub-3940256099942544/1033173712"

    private var basladi = false
    private var odullu: RewardedAd? = null
    private var gecis: InterstitialAd? = null
    private var odulluYukleniyor = false
    private var gecisYukleniyor = false
    private var sonGecis = 0L
    private var gecisSayaci = 0
    private var izin: ConsentInformation? = null

    /** Uygulama açılışında çağrılır: önce gizlilik izni (AB/İngiltere için), sonra reklam SDK'sı. */
    fun baslat(a: Activity) {
        if (!ACIK) return
        if (basladi) {
            yukle(a.applicationContext)
            return
        }
        try {
            val ci = UserMessagingPlatform.getConsentInformation(a)
            izin = ci
            val parametre = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()
            ci.requestConsentInfoUpdate(a, parametre, {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(a) { _ ->
                    if (ci.canRequestAds()) sdkBaslat(a.applicationContext)
                }
            }, { hata ->
                Log.w("AltinHali", "İzin bilgisi alınamadı: ${hata.message}")
                if (ci.canRequestAds()) sdkBaslat(a.applicationContext)
            })
            if (ci.canRequestAds()) sdkBaslat(a.applicationContext)
        } catch (e: Exception) {
            Log.w("AltinHali", "Reklam başlatılamadı", e)
        }
    }

    private fun sdkBaslat(c: Context) {
        if (basladi) return
        basladi = true
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        MobileAds.initialize(c) { yukle(c) }
    }

    private fun yukle(c: Context) {
        if (!basladi) return
        if (odullu == null && !odulluYukleniyor) {
            odulluYukleniyor = true
            RewardedAd.load(c, ODULLU_BIRIM, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    odullu = ad
                    odulluYukleniyor = false
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    odulluYukleniyor = false
                }
            })
        }
        if (gecis == null && !gecisYukleniyor) {
            gecisYukleniyor = true
            InterstitialAd.load(c, GECIS_BIRIM, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    gecis = ad
                    gecisYukleniyor = false
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    gecisYukleniyor = false
                }
            })
        }
    }

    /** İzlenmeye hazır bir ödüllü reklam var mı? */
    fun odulHazir(): Boolean = ACIK && odullu != null

    /**
     * Ödüllü reklamı gösterir. Oyuncu reklamı sonuna kadar izleyip kapatınca [odul] çağrılır.
     * Reklam hazır değilse false döner.
     */
    fun odulluGoster(a: Activity, odul: () -> Unit): Boolean {
        val ad = odullu ?: return false
        odullu = null
        var kazandi = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (kazandi) odul()
                yukle(a.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                yukle(a.applicationContext)
            }
        }
        ad.show(a) { kazandi = true }
        return true
    }

    /** Bölümler arasında, sıklık sınırına uyarak geçiş reklamı gösterir; ardından [devam] çalışır. */
    fun gecisGoster(a: Activity, seviye: Int, devam: () -> Unit) {
        val ad = gecis
        val simdi = System.currentTimeMillis()
        gecisSayaci++
        if (!ACIK || ad == null || seviye < 6 || gecisSayaci % 3 != 0 || simdi - sonGecis < 180_000L) {
            devam()
            return
        }
        gecis = null
        sonGecis = simdi
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                yukle(a.applicationContext)
                devam()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                yukle(a.applicationContext)
                devam()
            }
        }
        ad.show(a)
    }

    /** AB kullanıcıları için "reklam tercihlerini değiştir" düğmesi gerekiyor mu? */
    fun gizlilikSecenegiGerekli(): Boolean =
        izin?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gizlilikFormu(a: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(a) { _ -> }
    }
}
