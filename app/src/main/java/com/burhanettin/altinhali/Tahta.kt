package com.burhanettin.altinhali

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/** Kirin altına saklanmış bir eşya. Konum ızgara biriminde. */
class Hazine(val x: Float, val y: Float, val hucre: Int, val tip: Esya) {
    var toplandi = false
    var animasyon = 0f
}

/**
 * Halının üstündeki kir ızgarası. Android'e bağlı değildir, yalnızca oyun mantığını tutar.
 * Her hücrede 0 (temiz) ile yaklaşık [katman] arasında bir kir miktarı vardır.
 */
class Tahta(val sutun: Int, val satir: Int, val katman: Float, private val rnd: Random) {
    val toplam = sutun * satir
    val kir = FloatArray(toplam)
    val ton = FloatArray(toplam)
    var temizSayisi = 0
        private set

    private val bayrak = BooleanArray(toplam)
    private val kuyruk = IntArray(toplam)
    private var kuyrukBoyu = 0

    val hazineler = ArrayList<Hazine>()

    init {
        // Kaba bir gürültü ızgarası: halının bazı yerleri daha kirli olsun.
        val gs = 7
        val gy = 10
        val kaba = FloatArray((gs + 1) * (gy + 1)) { rnd.nextFloat() }
        for (y in 0 until satir) {
            for (x in 0 until sutun) {
                val fx = x.toFloat() / sutun * gs
                val fy = y.toFloat() / satir * gy
                val ix = fx.toInt()
                val iy = fy.toInt()
                val tx = fx - ix
                val ty = fy - iy
                val a = kaba[iy * (gs + 1) + ix]
                val b = kaba[iy * (gs + 1) + ix + 1]
                val c = kaba[(iy + 1) * (gs + 1) + ix]
                val d = kaba[(iy + 1) * (gs + 1) + ix + 1]
                val n = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
                val i = y * sutun + x
                kir[i] = katman * (0.8f + 0.45f * n)
                ton[i] = 0.82f + 0.3f * rnd.nextFloat() - 0.12f * n
            }
        }
        // Birkaç inatçı leke.
        repeat(4 + rnd.nextInt(4)) {
            val lx = rnd.nextFloat() * sutun
            val ly = rnd.nextFloat() * satir
            val lr = 3f + rnd.nextFloat() * 5f
            herCember(lx, ly, lr) { i, k -> kir[i] += katman * 0.5f * k }
        }
        for (i in 0 until toplam) isaretle(i)
    }

    fun hazineleriYerlestir(adet: Int, sansli: Boolean, hazineBolumu: Boolean = false) {
        val toplamAgirlik = Esya.values().sumOf { agirlik(it, sansli, hazineBolumu) }
        var deneme = 0
        while (hazineler.size < adet && deneme < adet * 50) {
            deneme++
            val x = 3f + rnd.nextFloat() * (sutun - 6)
            val y = 3f + rnd.nextFloat() * (satir - 6)
            if (hazineler.any { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) < 36f }) continue
            var r = rnd.nextInt(toplamAgirlik)
            var tip = Esya.PARA
            for (e in Esya.values()) {
                r -= agirlik(e, sansli, hazineBolumu)
                if (r < 0) { tip = e; break }
            }
            hazineler.add(Hazine(x, y, y.toInt() * sutun + x.toInt(), tip))
        }
    }

    private fun agirlik(e: Esya, sansli: Boolean, hazineBolumu: Boolean): Int {
        var a = e.agirlik
        if (sansli && e == Esya.HAYALET) a /= 3
        if (hazineBolumu) when (e) {
            Esya.ELMAS -> a *= 3
            Esya.KESE -> a *= 2
            Esya.HAYALET -> a /= 2
            else -> {}
        }
        return a
    }

    val temizOran: Float get() = temizSayisi.toFloat() / toplam

    /** (gx, gy) merkezli, rg yarıçaplı alanda kiri azaltır. Kenara doğru etkisi düşer. */
    fun fircala(gx: Float, gy: Float, rg: Float, miktar: Float): Int {
        var n = 0
        herCember(gx, gy, rg) { i, k -> if (azalt(i, miktar * (0.35f + 0.65f * k))) n++ }
        return n
    }

    /** İnatçı kir: kirli hücrelerin yanındaki temiz hücreler yeniden kirlenir. */
    fun yayil(deneme: Int, miktar: Float) {
        repeat(deneme) {
            val i = rnd.nextInt(toplam)
            if (kir[i] > 0f) return@repeat
            val x = i % sutun
            val y = i / sutun
            var komsu = 0f
            if (x > 0) komsu = maxOf(komsu, kir[i - 1])
            if (x < sutun - 1) komsu = maxOf(komsu, kir[i + 1])
            if (y > 0) komsu = maxOf(komsu, kir[i - sutun])
            if (y < satir - 1) komsu = maxOf(komsu, kir[i + sutun])
            if (komsu > katman * 0.4f) {
                kir[i] = miktar
                temizSayisi--
                isaretle(i)
            }
        }
    }

    /** Alanı anında tertemiz yapar. */
    fun patlat(gx: Float, gy: Float, rg: Float) {
        herCember(gx, gy, rg) { i, _ -> azalt(i, Float.MAX_VALUE) }
    }

    fun hepsiniTemizle() {
        for (i in 0 until toplam) azalt(i, Float.MAX_VALUE)
    }

    private inline fun herCember(gx: Float, gy: Float, rg: Float, islem: (Int, Float) -> Unit) {
        val x0 = maxOf(0, floor(gx - rg).toInt())
        val x1 = minOf(sutun - 1, ceil(gx + rg).toInt())
        val y0 = maxOf(0, floor(gy - rg).toInt())
        val y1 = minOf(satir - 1, ceil(gy + rg).toInt())
        val r2 = rg * rg
        for (y in y0..y1) {
            val dy = y + 0.5f - gy
            for (x in x0..x1) {
                val dx = x + 0.5f - gx
                val d2 = dx * dx + dy * dy
                if (d2 > r2) continue
                islem(y * sutun + x, 1f - sqrt(d2) / rg)
            }
        }
    }

    private fun azalt(i: Int, m: Float): Boolean {
        if (kir[i] <= 0f) return false
        kir[i] -= m
        if (kir[i] <= 0.04f) {
            kir[i] = 0f
            temizSayisi++
        }
        isaretle(i)
        return true
    }

    private fun isaretle(i: Int) {
        if (!bayrak[i]) {
            bayrak[i] = true
            kuyruk[kuyrukBoyu++] = i
        }
    }

    /** Son çağrıdan beri değişen hücreleri verir (ekran güncellemesi için). */
    fun degisenler(islem: (Int) -> Unit) {
        for (k in 0 until kuyrukBoyu) {
            val i = kuyruk[k]
            bayrak[i] = false
            islem(i)
        }
        kuyrukBoyu = 0
    }
}
