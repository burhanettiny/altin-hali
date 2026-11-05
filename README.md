# 🧽 Altın Halı — Halı Yıkama Oyunu (Android)

Kirli halıyı parmağınla ovala, kirin altında saklı altınları ve sürprizleri bul. Topladığın altınlarla daha güçlü deterjanlar, güçlendiriciler ve yetenekli karakterler satın al.

## Oynanış

- **Ovala:** Parmağını halının üstünde gezdir. Kir katman katmandır; ovaladıkça açılır ve altındaki desenli halı ortaya çıkar.
- **Kirin altındakiler:**
  | Simge | Ne yapar |
  |---|---|
  | ₺ altın para | +10 altın |
  | 💰 kese | +45 altın |
  | 💎 elmas | +120 altın |
  | ⏰ saat | +5 saniye |
  | ⭐ yıldız | 10 saniye boyunca çift altın |
  | 💣 bomba | Etrafını anında temizler |
  | 🎁 hediye | Rastgele altın ya da güçlendirici |
  | 👻 hayalet | 4 saniye kaybettirir |
- **Süreli mod:** Her bölümün bir süresi var. Ne kadar hızlı temizlersen o kadar çok **hız bonusu** ve ⭐ alırsın. Bölümler ilerledikçe kir kalınlaşır, halının rengi ve kirin türü (çamur, toz, is, yosun) değişir.
- **Rahat mod:** Süre yok, ama altınların yarısını kazanırsın.

## Mağaza

**Deterjanlar** (kalıcı): fırçanın genişliğini ve gücünü artırır.

| Deterjan | Fiyat | Etkisi |
|---|---|---|
| 🧼 Sade Sabun | başlangıç | — |
| 🧴 Köpük Şampuan | 150 | Daha geniş fırça |
| 🍋 Limon Kuvveti | 350 | Neredeyse 2 kat güç |
| 💨 Oksijen Bombası | 750 | Geniş ve güçlü |
| 🔮 Turbo Kristal | 1600 | En güçlüsü |

**Güçlendiriciler** (tek kullanımlık, oyunda alttaki düğmelerden): ⏳ Ekstra Süre (+15 sn), 🔍 Hazine Gözlüğü, 💣 Köpük Bombası, 🧲 Altın Mıknatısı.

**Karakterler:** 👨 Hasan Usta, 👩 Ayşe Abla (+%25 altın), 🏃 Turbo Tarık (geniş fırça), 👵 Fatma Nine (+12 sn), 🐱 Şanslı Kedi (daha çok hazine, az hayalet), 🤖 Süpürgeç (kendi kendine temizleyen robot).

## APK'yı indirmek

Her push'ta GitHub Actions APK'yı derler: **Actions → Altın Halı APK → son çalıştırma → Artifacts → `altin-hali-apk`**. Zip'i açıp `app-debug.apk` dosyasını telefona atın ve kurun ("bilinmeyen kaynaklar" izni gerekir).

## Geliştirme

Depoyu Android Studio ile açın ve çalıştırın, ya da komut satırından:

```bash
./gradlew assembleDebug
```

Kod tamamen Kotlin; ek kütüphane yok. Grafikler (halı deseni, altınlar, köpük) kodla çizilir.

| Dosya | İçerik |
|---|---|
| `Katalog.kt` | Deterjanlar, karakterler, güçlendiriciler, bölüm zorluğu |
| `Tahta.kt` | Kir ızgarası ve hazineler (Android'den bağımsız oyun mantığı) |
| `HaliCizici.kt` | Desenli halının çizimi |
| `OyunView.kt` | Oyun ekranı: dokunma, efektler, arayüz |
| `OyunActivity.kt` | Oyun ekranı ve sonuç penceresi |
| `MainActivity.kt` | Menü, mağaza, karakterler |
| `Kayit.kt` | Altın, bölüm ve satın alımların kaydı |
