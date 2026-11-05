package com.burhanettin.altinhali

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Kirin altına saklanmış bir eşya. Konum ızgara biriminde. */
class Hazine(val x: Float, val y: Float, val hucre: Int, val tip: Esya) {
    var toplandi = false
    var animasyon = 0f
}

/** Halının üstündeki bir çöp. Konum ızgara biriminde. */
class Cop(var x: Float, var y: Float, val tip: CopTipi, val aci: Float) {
    var tutuluyor = false
    var atildi = false
    var animasyon = 0f
    val boyut get() = tip.boyut
}

/**
 * Halının üstündeki kir ızgarası. Android'e bağlı değildir, yalnızca oyun mantığını tutar.
 * Her hücrede bir leke türü, kir miktarı, köpük kalıntısı ve ıslaklık vardır.
 * Bir hücre, kiri bitmiş ve köpüğü durulanmışsa temiz sayılır.
 */
class Tahta(val sutun: Int, val satir: Int, val katman: Float, private val rnd: Random, seviye: Int) {
    val toplam = sutun * satir
    val kir = FloatArray(toplam)
    val tip = ByteArray(toplam)
    val kopuk = FloatArray(toplam)
    val islak = FloatArray(toplam)
    val ton = FloatArray(toplam)
    private val ortu = IntArray(toplam)
    private val temiz = BooleanArray(toplam)
    private val kopukBayrak = BooleanArray(toplam)

    var temizSayisi = 0
        private set
    var kopukluSayisi = 0
        private set
    var islakVar = false
        private set

    private val bayrak = BooleanArray(toplam)
    private val kuyruk = IntArray(toplam)
    private var kuyrukBoyu = 0

    val hazineler = ArrayList<Hazine>()
    val coplar = ArrayList<Cop>()
    val lekeTurleri = LinkedHashSet<Leke>()

    init {
        val gs = 8
        val gy = 12
        val kaba = FloatArray((gs + 1) * (gy + 1)) { rnd.nextFloat() }
        val tozluMu = seviye % 2 == 0
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
                kir[i] = katman * (0.75f + 0.45f * n)
                ton[i] = 0.8f + 0.32f * rnd.nextFloat() - 0.12f * n
                // Taban kir: çamur; tozlu bölümlerde bazı bölgeler toz.
                tip[i] = (if (tozluMu && n > 0.5f) Leke.TOZ else Leke.CAMUR).ordinal.toByte()
            }
        }
        lekeTurleri.add(Leke.CAMUR)
        if (tozluMu) lekeTurleri.add(Leke.TOZ)

        // Zorlu lekeler
        val uygun = Leke.values().filter { it.ilkSeviye <= seviye && it != Leke.CAMUR && it != Leke.TOZ }
        if (uygun.isNotEmpty()) {
            repeat(Katalog.lekeSayisi(seviye)) {
                // Yeni açılan leke türleri daha sık çıksın
                val l = if (rnd.nextFloat() < 0.35f) uygun.last() else uygun[rnd.nextInt(uygun.size)]
                lekeTurleri.add(l)
                val lx = 5f + rnd.nextFloat() * (sutun - 10)
                val ly = 5f + rnd.nextFloat() * (satir - 10)
                if (l == Leke.SAKIZ) {
                    repeat(2 + rnd.nextInt(3)) {
                        lekeBloku(lx + (rnd.nextFloat() - 0.5f) * 10f, ly + (rnd.nextFloat() - 0.5f) * 10f, 1.3f + rnd.nextFloat(), l)
                    }
                } else {
                    lekeBloku(lx, ly, 3.5f + rnd.nextFloat() * 4.5f, l)
                }
            }
        }
        for (i in 0 until toplam) isaretle(i)
    }

    /** Kenarları düzensiz bir leke lekesi. */
    private fun lekeBloku(cx: Float, cy: Float, r: Float, l: Leke) {
        val f1 = rnd.nextFloat() * 6.28f
        val f2 = rnd.nextFloat() * 6.28f
        val rr = r * 1.4f
        herCember(cx, cy, rr) { i, _ ->
            val x = i % sutun + 0.5f - cx
            val y = i / sutun + 0.5f - cy
            val aci = kotlin.math.atan2(y, x)
            val sinir = r * (0.8f + 0.18f * sin(3 * aci + f1) + 0.12f * sin(5 * aci + f2))
            val dd = sqrt(x * x + y * y)
            if (dd <= sinir) {
                tip[i] = l.ordinal.toByte()
                val kenar = (1f - dd / sinir).coerceIn(0f, 1f)
                kir[i] = maxOf(kir[i], katman * (0.9f + 0.5f * kenar))
            }
        }
    }

    fun hazineleriYerlestir(adet: Int, sansli: Boolean, hazineBolumu: Boolean = false) {
        val toplamAgirlik = Esya.values().sumOf { agirlik(it, sansli, hazineBolumu) }
        var deneme = 0
        while (hazineler.size < adet && deneme < adet * 50) {
            deneme++
            val x = 3f + rnd.nextFloat() * (sutun - 6)
            val y = 3f + rnd.nextFloat() * (satir - 6)
            if (hazineler.any { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) < 40f }) continue
            var r = rnd.nextInt(toplamAgirlik)
            var t = Esya.PARA
            for (e in Esya.values()) {
                r -= agirlik(e, sansli, hazineBolumu)
                if (r < 0) { t = e; break }
            }
            hazineler.add(Hazine(x, y, y.toInt() * sutun + x.toInt(), t))
        }
    }

    /** Şemi'nin bıraktığı kir: (gx, gy) çevresini belirtilen lekeyle kirletir. */
    fun kirlet(gx: Float, gy: Float, rg: Float, l: Leke, miktar: Float) {
        herCember(gx, gy, rg) { i, k ->
            if (ortu[i] > 0) return@herCember
            val m = miktar * (0.5f + 0.5f * k)
            if (m <= kir[i]) return@herCember
            if (kir[i] <= 0f || l != Leke.CAMUR) tip[i] = l.ordinal.toByte()
            kir[i] = m
            lekeTurleri.add(l)
            durumGuncelle(i)
            isaretle(i)
        }
    }

    fun copEkle(x: Float, y: Float): Cop {
        val tipler = CopTipi.values()
        val c = Cop(x.coerceIn(3f, sutun - 3f), y.coerceIn(3f, satir - 3f), tipler[rnd.nextInt(tipler.size)], (rnd.nextFloat() - 0.5f) * 70f)
        coplar.add(c)
        ortula(c, 1)
        return c
    }

    fun copleriYerlestir(adet: Int) {
        var deneme = 0
        val tipler = CopTipi.values()
        while (coplar.size < adet && deneme < adet * 50) {
            deneme++
            val x = 5f + rnd.nextFloat() * (sutun - 10)
            val y = 5f + rnd.nextFloat() * (satir - 10)
            if (coplar.any { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) < 64f }) continue
            val c = Cop(x, y, tipler[rnd.nextInt(tipler.size)], (rnd.nextFloat() - 0.5f) * 70f)
            coplar.add(c)
            ortula(c, 1)
        }
    }

    /** Çöpün altındaki hücreler temizlenemez. */
    fun ortula(c: Cop, isaret: Int) {
        herCember(c.x, c.y, c.boyut * 0.8f) { i, _ -> ortu[i] = (ortu[i] + isaret).coerceAtLeast(0) }
    }

    fun ortulu(gx: Float, gy: Float): Boolean {
        val x = gx.toInt()
        val y = gy.toInt()
        if (x !in 0 until sutun || y !in 0 until satir) return false
        return ortu[y * sutun + x] > 0
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
    val kalanCop: Int get() = coplar.count { !it.atildi }

    /**
     * Ovalama. [etki] her leke türü için (zorluğa bölünmüş) kir azaltma miktarıdır.
     * Sökülen kirin [kopukOran] katı kadar köpük kalır. Islak hücrelerde [islakBonus] uygulanır.
     * Dönüş: etkilenen hücre sayısı ve en zayıf kaldığı leke (ipucu için).
     */
    fun ovala(gx: Float, gy: Float, rg: Float, etki: FloatArray, kopukOran: Float, islakBonus: Float): Int {
        var n = 0
        herCember(gx, gy, rg) { i, k ->
            if (ortu[i] > 0 || kir[i] <= 0f) return@herCember
            var m = etki[tip[i].toInt()] * (0.35f + 0.65f * k)
            if (m <= 0f) return@herCember
            if (islak[i] > 0.3f) m *= islakBonus
            val once = kir[i]
            kir[i] = (kir[i] - m).let { if (it <= 0.04f) 0f else it }
            val sokulen = once - kir[i]
            if (kopukOran > 0f) kopukEkle(i, sokulen * kopukOran / maxOf(1f, katman) * 1.2f)
            durumGuncelle(i)
            isaretle(i)
            n++
        }
        return n
    }

    /** Su: köpüğü alır, halıyı ıslatır, hafif kiri de biraz söker. */
    fun durula(gx: Float, gy: Float, rg: Float, miktar: Float, etki: FloatArray) {
        herCember(gx, gy, rg) { i, k ->
            if (ortu[i] > 0) return@herCember
            val f = 0.4f + 0.6f * k
            islak[i] = 1f
            islakVar = true
            if (kopuk[i] > 0f) {
                kopuk[i] = (kopuk[i] - miktar * f).coerceAtLeast(0f)
                if (kopuk[i] < 0.03f) kopuk[i] = 0f
            }
            if (kir[i] > 0f) {
                kir[i] = (kir[i] - etki[tip[i].toInt()] * f).let { if (it <= 0.04f) 0f else it }
            }
            durumGuncelle(i)
            isaretle(i)
        }
    }

    fun kurut(gx: Float, gy: Float, rg: Float, miktar: Float) {
        herCember(gx, gy, rg) { i, _ -> if (islak[i] > 0f) islak[i] = (islak[i] - miktar).coerceAtLeast(0f) }
    }

    /** Islaklık zamanla kurur. */
    fun kuru(dt: Float) {
        if (!islakVar) return
        var kalan = false
        for (i in 0 until toplam) {
            if (islak[i] > 0f) {
                islak[i] = (islak[i] - dt * 0.1f).coerceAtLeast(0f)
                if (islak[i] > 0f) kalan = true
            }
        }
        islakVar = kalan
    }

    private fun kopukEkle(i: Int, m: Float) {
        kopuk[i] = (kopuk[i] + m).coerceAtMost(1f)
    }

    /** İnatçı kir: kirli hücrelerin yanındaki temiz hücreler yeniden kirlenir. */
    fun yayil(deneme: Int, miktar: Float) {
        repeat(deneme) {
            val i = rnd.nextInt(toplam)
            if (kir[i] > 0f || ortu[i] > 0) return@repeat
            val x = i % sutun
            val y = i / sutun
            var komsu = -1
            if (x > 0 && kir[i - 1] > katman * 0.4f) komsu = i - 1
            else if (x < sutun - 1 && kir[i + 1] > katman * 0.4f) komsu = i + 1
            else if (y > 0 && kir[i - sutun] > katman * 0.4f) komsu = i - sutun
            else if (y < satir - 1 && kir[i + sutun] > katman * 0.4f) komsu = i + sutun
            if (komsu >= 0) {
                kir[i] = miktar
                tip[i] = tip[komsu]
                durumGuncelle(i)
                isaretle(i)
            }
        }
    }

    /** Halıyı silkeleme: tozu döker, çamuru biraz gevşetir. */
    fun silkele(guc: Float) {
        val toz = Leke.TOZ.ordinal.toByte()
        val camur = Leke.CAMUR.ordinal.toByte()
        for (i in 0 until toplam) {
            if (kir[i] <= 0f || ortu[i] > 0) continue
            val m = when (tip[i]) {
                toz -> guc * (0.7f + 0.6f * rnd.nextFloat())
                camur -> guc * 0.12f * rnd.nextFloat()
                else -> 0f
            }
            if (m <= 0f) continue
            kir[i] = (kir[i] - m).let { if (it <= 0.04f) 0f else it }
            durumGuncelle(i)
            isaretle(i)
        }
    }

    /** Alanı anında tertemiz yapar (köpük dahil). */
    fun patlat(gx: Float, gy: Float, rg: Float) {
        herCember(gx, gy, rg) { i, _ ->
            if (ortu[i] > 0) return@herCember
            kir[i] = 0f
            kopuk[i] = 0f
            durumGuncelle(i)
            isaretle(i)
        }
    }

    fun hepsiniTemizle() {
        for (i in 0 until toplam) {
            kir[i] = 0f
            kopuk[i] = 0f
            durumGuncelle(i)
            isaretle(i)
        }
    }

    /** Bu noktadaki leke türü (kir varsa). */
    fun lekeAt(gx: Float, gy: Float): Leke? {
        val x = gx.toInt()
        val y = gy.toInt()
        if (x !in 0 until sutun || y !in 0 until satir) return null
        val i = y * sutun + x
        return if (kir[i] > 0f) Leke.values()[tip[i].toInt()] else null
    }

    private fun durumGuncelle(i: Int) {
        val t = kir[i] <= 0f && kopuk[i] <= 0.05f
        if (t != temiz[i]) {
            temiz[i] = t
            temizSayisi += if (t) 1 else -1
        }
        val kopuklu = kopuk[i] > 0.05f
        if (kopuklu != kopukBayrak[i]) {
            kopukBayrak[i] = kopuklu
            kopukluSayisi += if (kopuklu) 1 else -1
        }
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
