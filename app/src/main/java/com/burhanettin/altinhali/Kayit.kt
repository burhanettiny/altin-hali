package com.burhanettin.altinhali

import android.content.Context

/** Oyuncunun ilerlemesini cihazda saklar. */
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

    fun sahip(tur: String, id: String): Boolean =
        id == "sabun" || id == "usta" || p.getBoolean("sahip_${tur}_$id", false)

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
}
