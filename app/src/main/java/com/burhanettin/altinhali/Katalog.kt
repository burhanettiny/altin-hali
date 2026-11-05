package com.burhanettin.altinhali

import kotlin.math.exp

/** Deterjanların kimyasal özellikleri. Her leke türü bunlardan birine/ikisine duyarlıdır. */
enum class Ozellik(val ad: String, val renk: Int) {
    GENEL("Genel temizlik", 0xFF90CAF9.toInt()),
    YAG("Yağ çözücü", 0xFFFFE082.toInt()),
    ASIT("Asit", 0xFFC5E1A5.toInt()),
    OKSIJEN("Oksijen", 0xFF80DEEA.toInt()),
    ENZIM("Enzim", 0xFFCE93D8.toInt()),
    COZUCU("Çözücü", 0xFFFFAB91.toInt()),
}

/** Halıdaki kir/leke türleri. */
enum class Leke(
    val ad: String,
    val renk: Int,
    val zorluk: Float,
    val ana: Ozellik?,
    val yardimci: Ozellik?,
    val genelOran: Float,
    val ilkSeviye: Int,
    val ipucu: String,
) {
    CAMUR("Çamur", 0xFF6D4C2F.toInt(), 1.0f, Ozellik.ENZIM, null, 1.0f, 1, ""),
    TOZ("Toz", 0xFF8D8270.toInt(), 0.6f, null, null, 1.0f, 1, "Toz! 🌀 Süpürge tozu çok hızlı çeker."),
    KAHVE("Kahve", 0xFF4E2A12.toInt(), 1.3f, Ozellik.OKSIJEN, Ozellik.ENZIM, 0.45f, 2,
        "Kahve lekesi: oksijenli ya da enzimli bir deterjan iyi gelir."),
    YAG("Yağ", 0xFF9C7A1C.toInt(), 1.5f, Ozellik.YAG, null, 0.25f, 3,
        "Yağ lekesi! Karbonatlı (yağ çözücü) bir formül dene."),
    SARAP("Şarap", 0xFF6A1035.toInt(), 1.5f, Ozellik.OKSIJEN, Ozellik.ASIT, 0.3f, 4,
        "Şarap lekesi: oksijenli su çok işe yarar."),
    PAS("Pas", 0xFFB2561B.toInt(), 1.8f, Ozellik.ASIT, null, 0.2f, 6,
        "Pas lekesi: sirke ya da limon tuzu gibi bir asit gerekir."),
    MUREKKEP("Mürekkep", 0xFF1A2F7A.toInt(), 1.7f, Ozellik.COZUCU, null, 0.2f, 8,
        "Mürekkep: alkol gibi bir çözücü dene."),
    SAKIZ("Sakız", 0xFFE87FB0.toInt(), 2.0f, Ozellik.COZUCU, null, 0.1f, 10,
        "Sakız! 🧊 Buzla dondurup kazı ya da çözücü kullan."),
    ;

    /** Bu deterjan profili bu lekeye ne kadar etkili (yaklaşık 0.1 – 3.5). */
    fun etki(profil: FloatArray): Float {
        var v = genelOran * profil[Ozellik.GENEL.ordinal]
        ana?.let { v += profil[it.ordinal] }
        yardimci?.let { v += 0.5f * profil[it.ordinal] }
        return v.coerceIn(0.08f, 3.5f)
    }
}

/** Fırçanın genişliğini ve kimyasını belirleyen deterjan. */
class Deterjan(
    val id: String,
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val fiyat: Int,
    val yaricap: Float,
    /** [Ozellik] sırasıyla güçler. */
    val profil: FloatArray,
    /** Ovalarken bırakılan köpük (durulanmalı). */
    val kopuk: Float,
    val altinBonus: Float = 0f,
    val kopukRengi: Int = 0xFFFFFFFF.toInt(),
) {
    fun iyiOlduklari(): List<Leke> = Leke.values().filter { it.etki(profil) >= 1.0f }
}

/** Oyun içinde değiştirilebilen aletler. */
enum class Alet(
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val fiyat: Int,
    val yaricap: Float,
    val guc: Float,
    val deterjanli: Boolean,
) {
    SUNGER("Sünger", "🧽", "Deterjanla ovalar. Köpük bırakır, sonra durulamak gerekir.", 0, 1.0f, 1.0f, true),
    FIRCA("Sert Fırça", "🧹", "Dar ama çok güçlü. İnatçı lekeler için.", 250, 0.6f, 2.2f, true),
    SU("Su", "🚿", "Köpüğü durular, halıyı ıslatır. Islak halı daha kolay temizlenir.", 0, 1.4f, 1.0f, false),
    CIMBIZ("Cımbız", "🥢", "Çöpleri tutup çöp kutusuna at.", 0, 0f, 0f, false),
    SUPURGE("Süpürge", "🌀", "Tozu çok hızlı çeker, ıslak halıyı kurutur.", 300, 1.3f, 1.0f, false),
    BUZ("Buz", "🧊", "Sakızı dondurup kazır.", 350, 0.55f, 1.0f, false),
    ;

    val id get() = name.lowercase()

    /** Aletin leke türüne göre çarpanı. */
    fun carpan(l: Leke): Float = when (this) {
        SUNGER -> if (l == Leke.SAKIZ) 0.12f else 1f
        FIRCA -> if (l == Leke.SAKIZ) 0.5f else 1f
        SUPURGE -> when (l) { Leke.TOZ -> 4f; Leke.CAMUR -> 0.3f; else -> 0.05f }
        BUZ -> if (l == Leke.SAKIZ) 5f else 0f
        SU -> 0.12f
        CIMBIZ -> 0f
    }
}

/** Formül laboratuvarındaki malzemeler. */
class Malzeme(
    val id: String,
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val fiyat: Int,
    val katki: FloatArray,
    val kopuk: Float,
    val altin: Float = 0f,
    val asit: Boolean = false,
    val baz: Boolean = false,
)

/** Bir formülün hesaplanmış sonucu. */
class FormulSonuc(val profil: FloatArray, val kopuk: Float, val altinBonus: Float, val uyarilar: List<String>)

/** Her karakterin kendine özgü bir yeteneği var. */
data class Karakter(
    val id: String,
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val fiyat: Int,
    val altinCarpani: Float = 1f,
    val yaricapCarpani: Float = 1f,
    val ekstraSure: Int = 0,
    val robot: Boolean = false,
    val sansli: Boolean = false,
    /** Zorlu lekelere (çamur/toz dışı) etki çarpanı. */
    val lekeUzmani: Float = 1f,
    /** Durulama hızı ve köpük azaltma. */
    val durulama: Float = 1f,
    /** Silkeleme gücü çarpanı. */
    val silkeleme: Float = 1f,
    /** Çöpler kendiliğinden çöp kutusuna gider. */
    val copToplatir: Boolean = false,
)

/** Oyun içinde kullanılan, tek seferlik güçlendiriciler. */
data class Guclendirici(
    val id: String,
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val fiyat: Int,
)

/** Kirin altına saklanan sürprizler. */
enum class Esya(val emoji: String, val agirlik: Int) {
    PARA("", 45),
    KESE("💰", 14),
    ELMAS("💎", 4),
    SAAT("⏰", 9),
    YILDIZ("⭐", 6),
    BOMBA("💣", 7),
    HEDIYE("🎁", 6),
    HAYALET("👻", 9),
}

/** Halı türleri: her biri farklı özen ister. */
enum class HaliTuru(val ad: String, val emoji: String, val aciklama: String, val odulCarpani: Float) {
    YUN("Yün Halı", "🐑", "Dayanıklı. Sadece çok güçlü asit ve sert fırça az da olsa yıpratır.", 1f),
    SENTETIK("Sentetik Halı", "🧵", "Yağ lekesi kolay çıkar ama alkol (çözücü) halıyı eritir!", 0.9f),
    IPEK("İpek Halı", "🧣", "Çok hassas! Sert fırça, güçlü asit ve oksijen renklerini soldurur.", 1.6f),
    ANTIKA("Antika Halı", "🏺", "Çok değerli: hazinesi bol ama her hata pahalı. Nazik ol!", 2f),
    ;

    /** Bu alet ve deterjanla ovalarken halıya verilen hasar oranı. */
    fun hasarOrani(alet: Alet, det: Deterjan): Float {
        if (!alet.deterjanli) return 0f
        val p = det.profil
        fun fazla(o: Ozellik, esik: Float) = (p[o.ordinal] - esik).coerceAtLeast(0f)
        val firca = alet == Alet.FIRCA
        return when (this) {
            YUN -> (if (firca) 0.03f else 0f) + fazla(Ozellik.ASIT, 1.6f) * 0.08f
            SENTETIK -> (if (firca) 0.02f else 0f) + fazla(Ozellik.COZUCU, 0.5f) * 0.45f
            IPEK -> (if (firca) 0.35f else 0.02f) + fazla(Ozellik.ASIT, 0.8f) * 0.28f +
                fazla(Ozellik.OKSIJEN, 1.0f) * 0.28f + fazla(Ozellik.YAG, 1.2f) * 0.15f
            ANTIKA -> (if (firca) 0.25f else 0.03f) + fazla(Ozellik.ASIT, 1.0f) * 0.2f +
                fazla(Ozellik.OKSIJEN, 1.2f) * 0.2f + fazla(Ozellik.COZUCU, 0.8f) * 0.2f
        }
    }

    /** Bu halıda bu leke için ek kolaylık. */
    fun lekeCarpani(l: Leke): Float = if (this == SENTETIK && l == Leke.YAG) 1.6f else 1f
}

/** Halının üstündeki çöpler. */
enum class CopTipi(val emoji: String, val boyut: Float) {
    MUZ("🍌", 2.6f), GAZETE("📰", 3.2f), KUTU("🥫", 2.4f), CORAP("🧦", 2.6f), YAPRAK("🍂", 2.3f),
    KEMIK("🦴", 2.5f), SEKER("🍬", 1.9f), KALEM("✏️", 2.4f), PIZZA("🍕", 2.7f), BARDAK("🥤", 2.5f),
}

private fun p(genel: Float = 0f, yag: Float = 0f, asit: Float = 0f, oksijen: Float = 0f, enzim: Float = 0f, cozucu: Float = 0f) =
    floatArrayOf(genel, yag, asit, oksijen, enzim, cozucu)

object Katalog {
    val deterjanlar = listOf(
        Deterjan("sabun", "Sade Sabun", "🧼", "Her işe yarar ama yavaş.", 0, 1.0f,
            p(genel = 1.0f, yag = 0.1f, enzim = 0.1f), 0.6f),
        Deterjan("sampuan", "Köpük Şampuan", "🧴", "Geniş fırça alanı, bol köpük.", 150, 1.35f,
            p(genel = 1.1f, yag = 0.2f, enzim = 0.3f), 0.8f, kopukRengi = 0xFFB3E5FC.toInt()),
        Deterjan("limon", "Limon Kuvveti", "🍋", "Asitli: pas ve şaraba karşı iyi.", 350, 1.1f,
            p(genel = 1.0f, yag = 0.1f, asit = 1.4f, oksijen = 0.4f), 0.5f, kopukRengi = 0xFFFFF59D.toInt()),
        Deterjan("oksijen", "Oksijen Bombası", "💨", "Kahve ve şarap lekelerinin düşmanı.", 750, 1.4f,
            p(genel = 1.3f, yag = 0.2f, asit = 0.2f, oksijen = 1.5f, enzim = 0.6f), 0.6f, kopukRengi = 0xFFB9F6CA.toInt()),
        Deterjan("kristal", "Turbo Kristal", "🔮", "Her lekeye karşı güçlü, az köpük.", 1600, 1.7f,
            p(genel = 1.8f, yag = 1.2f, asit = 1.0f, oksijen = 1.0f, enzim = 1.0f, cozucu = 0.9f), 0.4f,
            kopukRengi = 0xFFE1BEE7.toInt()),
    )

    val malzemeler = listOf(
        Malzeme("baz", "Sabun Bazı", "🧼", "Genel temizlik sağlar, köpürür.", 0, p(genel = 0.55f, yag = 0.05f, enzim = 0.05f), 0.3f),
        Malzeme("karbonat", "Karbonat", "🧂", "Bazik. Yağı çözer.", 120, p(genel = 0.1f, yag = 0.75f), 0f, baz = true),
        Malzeme("sirke", "Sirke", "🍶", "Hafif asit. Pas ve kireç söker.", 120, p(asit = 0.6f, oksijen = 0.1f), 0f, asit = true),
        Malzeme("limontuzu", "Limon Tuzu", "🍋", "Güçlü asit. Pasa birebir.", 220, p(asit = 0.9f, oksijen = 0.2f), 0f, asit = true),
        Malzeme("oksijenli", "Oksijenli Su", "🧪", "Kahve, şarap gibi renkli lekeleri ağartır.", 260, p(genel = 0.1f, oksijen = 0.9f), 0.05f),
        Malzeme("enzim", "Enzim Tozu", "🧫", "Çamur ve yemek gibi organik kirleri parçalar.", 300, p(genel = 0.1f, oksijen = 0.2f, enzim = 0.9f), 0.05f),
        Malzeme("alkol", "Alkol", "⚗️", "Çözücü. Mürekkep ve sakızı yumuşatır.", 350, p(yag = 0.2f, cozucu = 0.9f), 0f),
        Malzeme("yumusatici", "Yumuşatıcı", "🌸", "Köpüğü azaltır, durulaması kolaylaşır.", 150, p(), -0.35f),
        Malzeme("esans", "Lavanta Esansı", "🌿", "Mis gibi koku: müşteri her altına %8 fazla öder.", 400, p(), 0f, altin = 0.08f),
    )

    const val FORMUL_EN_FAZLA = 6
    const val MALZEME_EN_FAZLA = 3
    val formulSlotlari = listOf("A", "B", "C")

    fun formulHesapla(miktar: IntArray): FormulSonuc {
        val ham = FloatArray(Ozellik.values().size)
        var kopuk = 0.1f
        var altin = 0f
        var asitU = 0
        var bazU = 0
        var toplam = 0
        malzemeler.forEachIndexed { i, m ->
            val n = miktar.getOrElse(i) { 0 }
            if (n <= 0) return@forEachIndexed
            toplam += n
            for (k in ham.indices) ham[k] += m.katki[k] * n
            kopuk += m.kopuk * n
            altin += m.altin * n
            if (m.asit) asitU += n
            if (m.baz) bazU += n
        }
        val uyarilar = ArrayList<String>()
        if (asitU > 0 && bazU > 0) {
            val n = minOf(asitU, bazU)
            ham[Ozellik.ASIT.ordinal] = (ham[Ozellik.ASIT.ordinal] - n * 0.75f).coerceAtLeast(0f)
            ham[Ozellik.YAG.ordinal] = (ham[Ozellik.YAG.ordinal] - n * 0.6f).coerceAtLeast(0f)
            kopuk += 0.3f
            uyarilar.add("⚠️ Asit ile baz birbirini nötrler: fokurdar ama etkisi azalır!")
        }
        if (miktar.getOrElse(0) { 0 } == 0) uyarilar.add("ℹ️ Sabun bazı yok: genel temizlik zayıf ama köpük de az.")
        if (toplam == 0) uyarilar.add("ℹ️ Formül boş: sadece su gibi davranır.")
        val profil = FloatArray(ham.size) { 2.6f * (1f - exp(-ham[it] / 1.6f)) }
        profil[Ozellik.GENEL.ordinal] = profil[Ozellik.GENEL.ordinal].coerceAtLeast(0.25f)
        return FormulSonuc(profil, kopuk.coerceIn(0.05f, 1.2f), altin, uyarilar)
    }

    fun formulDeterjani(slot: String, miktar: IntArray): Deterjan {
        val s = formulHesapla(miktar)
        val baskin = Ozellik.values().maxByOrNull { if (it == Ozellik.GENEL) 0f else s.profil[it.ordinal] } ?: Ozellik.GENEL
        return Deterjan("formul_$slot", "Formül $slot", "🧪", "Senin geliştirdiğin deterjan.", 0, 1.15f,
            s.profil, s.kopuk, s.altinBonus, baskin.renk)
    }

    val karakterler = listOf(
        Karakter("usta", "Hasan Usta", "👨", "Tecrübeli halıcı. Özel yeteneği yok ama işini bilir.", 0),
        Karakter("ayse", "Ayşe Abla", "👩", "Pazarlıkçı: her altından %25 fazla kazanır.", 300, altinCarpani = 1.25f),
        Karakter("tarik", "Turbo Tarık", "🏃", "Kolları uzun: fırça alanı %25 daha geniş.", 450, yaricapCarpani = 1.25f),
        Karakter("nine", "Songül Nine", "👵", "Acele etmez: her bölüme +12 saniye ile başlar.", 500, ekstraSure = 12),
        Karakter("gul", "Gül Hala", "🌹", "Gül kokulu temizlik: köpük az, durulama iki kat hızlı.", 550, durulama = 2f),
        Karakter("tef", "Tef Teyze", "💃", "Camdan halı silkeleme ustası: silkelemesi üç kat güçlü.", 600, silkeleme = 3f),
        Karakter("guzi", "Güzi Hala", "👓", "Leke uzmanı: zorlu lekeleri %40 daha hızlı söker.", 700, lekeUzmani = 1.4f),
        Karakter("kedi", "Şanslı Kedi", "🐱", "Daha çok hazine bulur, hayaletlerden kaçar.", 800, sansli = true),
        Karakter("robot", "Süpürgeç", "🤖", "Kendi kendine dolaşıp halıyı temizleyen robot yardımcı.", 1200, robot = true),
        Karakter("dede", "Acı Dede", "👴", "Evin reisi. Bir bakışı yeter: çöpler kendiliğinden çöp kutusuna gider.", 2000, copToplatir = true),
    )

    val guclendiriciler = listOf(
        Guclendirici("sure", "Ekstra Süre", "⏳", "Süreye 15 saniye ekler.", 60),
        Guclendirici("gozluk", "Hazine Gözlüğü", "🔍", "4 saniye boyunca gizli eşyaları gösterir.", 80),
        Guclendirici("bomba", "Köpük Bombası", "💣", "Dokunduğun yerde büyük bir alanı anında temizler.", 100),
        Guclendirici("miknatis", "Altın Mıknatısı", "🧲", "Kirin altındaki 3 değerli eşyayı çeker.", 120),
    )

    fun deterjan(id: String, kayit: Kayit? = null): Deterjan {
        if (id.startsWith("formul_") && kayit != null) {
            val slot = id.removePrefix("formul_")
            return formulDeterjani(slot, kayit.formul(slot))
        }
        return deterjanlar.firstOrNull { it.id == id } ?: deterjanlar[0]
    }

    fun karakter(id: String) = karakterler.firstOrNull { it.id == id } ?: karakterler[0]
    fun guclendirici(id: String) = guclendiriciler.first { it.id == id }

    // ---------------------------------------------------------------- zorluk

    /** Kir katmanı: arttıkça daha çok ovalamak gerekir. */
    fun katman(seviye: Int): Float = minOf(4.0f, 1f + (seviye - 1) * 0.25f)

    fun lekeSayisi(seviye: Int): Int = if (seviye < 2) 0 else minOf(14, 1 + (seviye * 0.8f).toInt())

    fun copSayisi(seviye: Int): Int = minOf(10, 2 + seviye / 2)

    fun haliTuru(seviye: Int): HaliTuru = when {
        seviye >= 9 && seviye % 4 == 1 -> HaliTuru.ANTIKA
        seviye >= 7 && seviye % 4 == 3 -> HaliTuru.IPEK
        seviye >= 3 && seviye % 4 == 2 -> HaliTuru.SENTETIK
        else -> HaliTuru.YUN
    }

    /** Hasar bu orana ulaşırsa halı mahvolur. */
    const val HASAR_SINIRI = 0.22f

    fun sure(seviye: Int): Int =
        (50 + katman(seviye) * 16 + copSayisi(seviye) * 3f + lekeSayisi(seviye) * 4.5f).toInt()
}
