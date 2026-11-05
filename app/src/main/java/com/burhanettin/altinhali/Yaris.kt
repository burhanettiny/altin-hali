package com.burhanettin.altinhali

import kotlin.random.Random

class YarisSonuc(val ad: String, val puan: Int, val temizYuzde: Int, val sure: Int, val altin: Int, val bitirdi: Boolean)

/**
 * Aynı telefonda sırayla oynanan yarışma. Herkes aynı halıyı (aynı tohum) temizler,
 * en yüksek puan kazanır.
 */
object Yaris {
    var oyuncular: List<String> = emptyList()
        private set
    var sira = 0
    var seviye = 1
        private set
    var tohum = 0L
        private set
    val sonuclar = ArrayList<YarisSonuc>()

    fun basla(isimler: List<String>, seviye: Int) {
        oyuncular = isimler
        this.seviye = seviye
        tohum = Random.nextLong()
        sira = 0
        sonuclar.clear()
    }

    val aktif get() = oyuncular.isNotEmpty() && sira < oyuncular.size
    val siradaki get() = oyuncular.getOrNull(sira)
    val sonraki get() = oyuncular.getOrNull(sira + 1)

    fun bitir() {
        oyuncular = emptyList()
        sira = 0
    }

    fun puan(bitirdi: Boolean, temizOran: Float, kalanSure: Float, altin: Int): Int =
        if (bitirdi) 1000 + (kalanSure * 20).toInt() + altin else (temizOran * 800).toInt() + altin
}
