package com.burhanettin.altinhali

import android.content.Context

/** Oyuncunun ilerlemesini, ayarlarını ve istatistiklerini cihazda saklar. */
class Kayit(ctx: Context) {
    private val p = ctx.getSharedPreferences("altin_hali", Context.MODE_PRIVATE)

    var altin: Int
        get() = p.getInt("altin", 0)
        set(v) = p.edit().putInt("altin", maxOf(0, v)).apply()

    var seviye: Int
        get() = p.getInt("seviye", 1)
        set(v) = p.edit().putInt("seviye", v).apply()

    var deterjan: String
        get() = p.getString("deterjan", "sabun") ?: "sabun"
        set(v) = p.edit().putString("deterjan", v).apply()

    var karakter: String
        get() = p.getString("karakter", "usta") ?: "usta"
        set(v) = p.edit().putString("karakter", v).apply()

    var toplamYildiz: Int
        get() = p.getInt("yildiz", 0)
        set(v) = p.edit().putInt("yildiz", v).apply()

    // Ayarlar
    var sesAcik: Boolean
        get() = p.getBoolean("ayar_ses", true)
        set(v) = p.edit().putBoolean("ayar_ses", v).apply()

    var titresimAcik: Boolean
        get() = p.getBoolean("ayar_titresim", true)
        set(v) = p.edit().putBoolean("ayar_titresim", v).apply()

    var muzikAcik: Boolean
        get() = p.getBoolean("ayar_muzik", true)
        set(v) = p.edit().putBoolean("ayar_muzik", v).apply()

    var sonBedavaGuc: Long
        get() = p.getLong("bedava_guc", 0L)
        set(v) = p.edit().putLong("bedava_guc", v).apply()

    var grafik3d: Boolean
        get() = p.getBoolean("ayar_3d", true)
        set(v) = p.edit().putBoolean("ayar_3d", v).apply()

    var egitimGoruldu: Boolean
        get() = p.getBoolean("egitim", false)
        set(v) = p.edit().putBoolean("egitim", v).apply()

    // Günlük ödül
    var sonOdulGunu: Long
        get() = p.getLong("odul_gun", 0L)
        set(v) = p.edit().putLong("odul_gun", v).apply()

    var seri: Int
        get() = p.getInt("odul_seri", 0)
        set(v) = p.edit().putInt("odul_seri", v).apply()

    fun sahip(tur: String, id: String): Boolean =
        id in VARSAYILAN || p.getBoolean("sahip_${tur}_$id", false)

    /** Formül laboratuvarındaki bir yuvanın malzeme ölçüleri. */
    fun formul(slot: String): IntArray {
        val n = Katalog.malzemeler.size
        val dizi = IntArray(n)
        val s = p.getString("formul_$slot", null)
        if (s == null) {
            dizi[0] = 2 // varsayılan: biraz sabun bazı
            return dizi
        }
        s.split(",").forEachIndexed { i, v -> if (i < n) dizi[i] = v.toIntOrNull() ?: 0 }
        return dizi
    }

    fun formulKaydet(slot: String, dizi: IntArray) =
        p.edit().putString("formul_$slot", dizi.joinToString(",")).apply()

    /** Yeterli altın varsa düşer ve true döner. */
    fun harca(fiyat: Int): Boolean {
        if (altin < fiyat) return false
        altin -= fiyat
        return true
    }

    fun sahiplen(tur: String, id: String) = p.edit().putBoolean("sahip_${tur}_$id", true).apply()

    fun adet(id: String): Int = p.getInt("guc_$id", 0)

    fun adetEkle(id: String, n: Int) = p.edit().putInt("guc_$id", maxOf(0, adet(id) + n)).apply()

    fun enIyiYildiz(seviye: Int): Int = p.getInt("en_iyi_$seviye", 0)

    fun yildizKaydet(seviye: Int, yildiz: Int) {
        val eski = enIyiYildiz(seviye)
        if (yildiz > eski) {
            p.edit().putInt("en_iyi_$seviye", yildiz).apply()
            toplamYildiz += yildiz - eski
        }
    }

    /** Bölümün en hızlı bitiş süresi (saniye); 0 = henüz yok. */
    fun rekor(seviye: Int): Int = p.getInt("rekor_$seviye", 0)

    fun rekorKaydet(seviye: Int, sn: Int) = p.edit().putInt("rekor_$seviye", sn).apply()

    // İstatistikler (başarımlar için)
    fun istat(ad: String): Int = p.getInt("istat_$ad", 0)

    fun istatEkle(ad: String, n: Int = 1) = p.edit().putInt("istat_$ad", istat(ad) + n).apply()

    fun istatEnFazla(ad: String, v: Int) {
        if (v > istat(ad)) p.edit().putInt("istat_$ad", v).apply()
    }

    fun istatEnAz(ad: String, v: Int) {
        val eski = istat(ad)
        if (eski == 0 || v < eski) p.edit().putInt("istat_$ad", v).apply()
    }

    fun basarimAlindi(id: String): Boolean = p.getBoolean("basarim_$id", false)

    fun basarimIsaretle(id: String) = p.edit().putBoolean("basarim_$id", true).apply()

    fun sifirla() = p.edit().clear().apply()

    companion object {
        private val VARSAYILAN = setOf("sabun", "usta", "baz", "sunger", "su", "cimbiz")
    }
}
