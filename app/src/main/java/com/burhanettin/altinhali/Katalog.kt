package com.burhanettin.altinhali

/** Fırçanın gücünü ve genişliğini belirleyen deterjanlar (kalıcı satın alınır). */
data class Deterjan(
    val id: String,
    val ad: String,
    val emoji: String,
    val aciklama: String,
    val fiyat: Int,
    val yaricap: Float,
    val guc: Float,
    val kopukRengi: Int,
)

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

object Katalog {
    val deterjanlar = listOf(
        Deterjan("sabun", "Sade Sabun", "🧼", "Her işe yarar ama yavaş.", 0, 1.0f, 1.0f, 0xFFFFFFFF.toInt()),
        Deterjan("sampuan", "Köpük Şampuan", "🧴", "Daha geniş fırça alanı.", 150, 1.35f, 1.0f, 0xFFB3E5FC.toInt()),
        Deterjan("limon", "Limon Kuvveti", "🍋", "Kiri neredeyse iki kat hızlı söker.", 350, 1.1f, 1.8f, 0xFFFFF59D.toInt()),
        Deterjan("oksijen", "Oksijen Bombası", "💨", "Hem geniş hem güçlü.", 750, 1.5f, 2.2f, 0xFFB9F6CA.toInt()),
        Deterjan("kristal", "Turbo Kristal", "🔮", "Efsanevi temizlik gücü.", 1600, 1.8f, 3.0f, 0xFFE1BEE7.toInt()),
    )

    val karakterler = listOf(
        Karakter("usta", "Hasan Usta", "👨", "Tecrübeli halıcı. Özel yeteneği yok ama işini bilir.", 0),
        Karakter("ayse", "Ayşe Abla", "👩", "Pazarlıkçı: her altından %25 fazla kazanır.", 300, altinCarpani = 1.25f),
        Karakter("tarik", "Turbo Tarık", "🏃", "Kolları uzun: fırça alanı %25 daha geniş.", 450, yaricapCarpani = 1.25f),
        Karakter("nine", "Fatma Nine", "👵", "Acele etmez: her bölüme +12 saniye ile başlar.", 500, ekstraSure = 12),
        Karakter("kedi", "Şanslı Kedi", "🐱", "Daha çok hazine bulur, hayaletlerden kaçar.", 800, sansli = true),
        Karakter("robot", "Süpürgeç", "🤖", "Kendi kendine dolaşıp halıyı temizleyen robot yardımcı.", 1200, robot = true),
    )

    val guclendiriciler = listOf(
        Guclendirici("sure", "Ekstra Süre", "⏳", "Süreye 15 saniye ekler.", 60),
        Guclendirici("gozluk", "Hazine Gözlüğü", "🔍", "4 saniye boyunca gizli eşyaları gösterir.", 80),
        Guclendirici("bomba", "Köpük Bombası", "💣", "Dokunduğun yerde büyük bir alanı anında temizler.", 100),
        Guclendirici("miknatis", "Altın Mıknatısı", "🧲", "Kirin altındaki 3 değerli eşyayı çeker.", 120),
    )

    fun deterjan(id: String) = deterjanlar.firstOrNull { it.id == id } ?: deterjanlar[0]
    fun karakter(id: String) = karakterler.firstOrNull { it.id == id } ?: karakterler[0]
    fun guclendirici(id: String) = guclendiriciler.first { it.id == id }

    /** Bölüm zorluğu: kir katmanı arttıkça daha çok ovalamak gerekir. */
    fun katman(seviye: Int): Float = minOf(4.5f, 1f + (seviye - 1) * 0.3f)

    fun sure(seviye: Int): Int = (40 + katman(seviye) * 16).toInt()
}
