package com.burhanettin.altinhali

class Basarim(
    val id: String,
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val odul: Int,
    val hedef: Int,
    val ilerleme: (Kayit) -> Int,
)

object Basarimlar {
    val liste = listOf(
        Basarim("ilk", "İlk Halı", "🧽", "Süreli modda bir bölüm bitir.", 50, 1) { it.istat("bolum") },
        Basarim("bes", "Usta Çırak", "🥉", "5 bölüm bitir.", 150, 5) { it.istat("bolum") },
        Basarim("on", "Halı Ustası", "🥈", "10 bölüm bitir.", 400, 10) { it.istat("bolum") },
        Basarim("yirmi", "Halı Efsanesi", "🥇", "20 bölüm bitir.", 1000, 20) { it.istat("bolum") },
        Basarim("kumbara", "Kumbaracı", "🐷", "Toplam 1.000 altın kazan.", 100, 1000) { it.istat("altin") },
        Basarim("altinbaba", "Altın Babası", "👑", "Toplam 10.000 altın kazan.", 800, 10000) { it.istat("altin") },
        Basarim("elmas", "Elmas Avcısı", "💎", "10 elmas bul.", 200, 10) { it.istat("elmas") },
        Basarim("kombo", "Kombo Ustası", "🔥", "x5 kombo yap.", 150, 5) { it.istat("kombo") },
        Basarim("simsek", "Şimşek", "⚡", "Bir bölümü 25 saniyenin altında bitir.", 250, 1) {
            if (it.istat("hizli") in 1..24) 1 else 0
        },
        Basarim("yildiz", "Yıldız Avcısı", "🌟", "30 yıldız topla.", 300, 30) { it.toplamYildiz },
        Basarim("korkusuz", "Korkusuz", "👻", "10 hayalet bul.", 60, 10) { it.istat("hayalet") },
        Basarim("koleksiyon", "Koleksiyoncu", "🧴", "Bütün deterjanları al.", 500, Katalog.deterjanlar.size) { k ->
            Katalog.deterjanlar.count { k.sahip("deterjan", it.id) }
        },
    )

    /** Yeni kazanılan başarımları işaretler, ödüllerini verir ve listesini döner. */
    fun kontrol(k: Kayit): List<Basarim> {
        val yeni = liste.filter { !k.basarimAlindi(it.id) && it.ilerleme(k) >= it.hedef }
        for (b in yeni) {
            k.basarimIsaretle(b.id)
            k.altin += b.odul
        }
        return yeni
    }
}
