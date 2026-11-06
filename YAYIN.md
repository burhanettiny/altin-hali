# Play Store'a yayın ve reklamla para kazanma rehberi

Bu rehber, Altın Halı'yı Google Play'e koyup AdMob reklamlarıyla gelir elde etmek için gereken adımları sırayla anlatır. Kod tarafı hazır; senin yapman gerekenler hesap açmak ve birkaç kimliği yerine yazmak.

---

## 1. Reklam nasıl çalışıyor?

Mobil oyunlarda en yaygın sistem **Google AdMob**'dur:

1. Oyuna AdMob'un kütüphanesi eklenir (**eklendi**).
2. AdMob hesabında oyunun için bir **uygulama kimliği** ve her reklam yeri için bir **reklam birimi kimliği** alırsın.
3. Oyun reklamı gösterdiğinde reklamveren Google'a ödeme yapar, Google da bunun bir kısmını (yaklaşık %68'ini) sana öder.
4. Bakiyen **100 dolara** ulaşınca her ay banka hesabına gönderilir.

**Kazanç neye bağlı?** Gösterim başına kazanç ülkeye ve reklam türüne göre değişir. Türkiye'de oyun reklamlarında 1000 gösterim başına (eCPM) genellikle şu aralıktadır:

| Reklam türü | Ne zaman | Türkiye'de kabaca eCPM |
|---|---|---|
| **Ödüllü reklam** | Oyuncu ödül için kendi seçer | 1 – 5 $ |
| **Geçiş reklamı** | Bölümler arasında tam ekran | 0,5 – 3 $ |
| Banner | Ekranın altında sürekli şerit | 0,05 – 0,3 $ |

Avrupa ve ABD'deki oyuncularda bu rakamlar birkaç kat yüksektir. Gerçek kazanç tamamen oyuncu sayısına bağlıdır: günde 1000 aktif oyuncunun ikişer ödüllü reklam izlemesi ayda kabaca 60 – 300 $ eder.

### Oyunda reklamlar nerede?

Oyuncuyu kaçırmamak için reklamlar **oyunu bölmeyecek** şekilde yerleştirildi:

- **📺 Süre dolunca: "Reklam izle, +20 saniye devam et"** (ödüllü, bölüm başına bir kez)
- **📺 Bölüm bitince: "Reklam izle, altınları 2 katla"** (ödüllü)
- **📺 Günlük ödülü 2 katına çıkar** (ödüllü)
- **📺 Mağazada 4 saatte bir bedava güçlendirici** (ödüllü)
- **Geçiş reklamı:** En erken 6. bölümden sonra, en fazla 3 bölümde bir ve 3 dakikada bir
- Oynarken banner **yok** (halıya dokunurken yanlışlıkla tıklanır, AdMob bunu cezalandırır)
- Yarışma modunda reklam **yok**

Şu an Google'ın resmî **test kimlikleri** kullanılıyor: oyunda "Test Ad" yazan deneme reklamları görürsün, para kazandırmaz ama hesabın da risk altına girmez.

> ⚠️ **Kendi reklamına asla tıklama, arkadaşlarına da tıklatma.** AdMob bunu sahte tıklama sayar ve hesabı kalıcı olarak kapatır. Kendi telefonunda denerken test kimliklerini kullan ya da AdMob'da telefonunu "test cihazı" olarak ekle.

---

## 2. Google Play geliştirici hesabı

1. https://play.google.com/console adresinden hesap aç. Tek seferlik **25 $** ücret alınır.
2. Kimlik doğrulaması istenir (kimlik, adres).
3. **Yeni kişisel hesaplar için:** Uygulamayı herkese açmadan önce **en az 12 kişiyle 14 gün kapalı test** yapman gerekiyor. Aile ve arkadaşlarından 12+ Gmail adresi topla, "Kapalı test" kanalına ekle; 14 gün boyunca oyunu yüklü tutsunlar.

## 3. İmzalama anahtarı (bir kez)

Play Store, uygulamanın senin tarafından yüklendiğini bir **yükleme anahtarıyla** doğrular. Bilgisayarında (Java kuruluysa) şu komutla oluştur:

```bash
keytool -genkeypair -v -keystore yukleme.jks -keyalg RSA -keysize 2048 -validity 10000 -alias altinhali
```

Sorulan şifreyi ve bilgileri gir. **`yukleme.jks` dosyasını ve şifreyi kaybetme, kimseyle paylaşma, depoya yükleme.** (Play App Signing açık olduğu için kaybedersen Google destekle sıfırlatılabilir ama uğraştırır.)

Sonra GitHub'da **altin-hali → Settings → Secrets and variables → Actions → New repository secret** ile şu üçünü ekle:

| Ad | Değer |
|---|---|
| `ANAHTAR_BASE64` | `base64 -w0 yukleme.jks` komutunun çıktısı (Windows: `certutil -encode`) |
| `ANAHTAR_SIFRESI` | Anahtar şifren |
| `ANAHTAR_ADI` | `altinhali` |

Bundan sonra her derlemede Actions sayfasında **`altin-hali-play-store-aab`** adında imzalı bir `.aab` dosyası çıkar. Play Console'a bunu yüklersin. Sürüm kodu her derlemede kendiliğinden artar.

## 4. AdMob hesabı ve kimlikler

1. https://admob.google.com adresinden aynı Google hesabıyla kayıt ol, ödeme bilgilerini gir.
2. **Uygulamalar → Uygulama ekle → Android** ("Play'de yayında mı?" sorusuna önce "Hayır" de; yayınlanınca bağlarsın).
3. Sana `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY` biçiminde bir **uygulama kimliği** verilir.
4. **Reklam birimleri → Reklam birimi ekle** ile iki birim oluştur:
   - **Ödüllü** → `ca-app-pub-XXXX/AAAA`
   - **Geçiş** → `ca-app-pub-XXXX/BBBB`
5. Kimlikleri koda yaz (ya da bana gönder, ben yazayım):
   - `app/src/main/AndroidManifest.xml` → `APPLICATION_ID` değeri ✅ (eklendi)
   - `app/build.gradle.kts` → `release` bölümündeki `ODULLU_BIRIM` ✅ (eklendi) ve `GECIS_BIRIM` ✅ (eklendi)
   - Deneme APK'sı (debug) bilerek her zaman Google'ın test reklamlarını gösterir; gerçek reklamlar yalnızca Play Store sürümündedir.
6. AdMob'da **Gizlilik ve mesajlaşma → GDPR** mesajını oluştur ve yayınla (Avrupa'daki oyuncular için izin formu; kodu hazır).
7. Oyun Play'de yayınlanınca AdMob'da uygulamayı Play mağaza kaydına bağla ve `app-ads.txt` dosyasını kur (AdMob adım adım gösterir).

> AdMob yeni hesaplarda reklam göstermeye başlamadan önce uygulamayı inceler; ilk günlerde az reklam gelmesi normaldir.

## 5. Play Console'da doldurulacaklar

- **Gizlilik politikası adresi:** `GIZLILIK.md` dosyası hazır. Herkese açık bir adreste olmalı:
  - Depo herkese açıksa: `https://github.com/burhanettiny/altin-hali/blob/main/GIZLILIK.md`
  - Depo gizliyse: Settings → Pages ile GitHub Pages'i aç ya da metni Google Sites gibi bir yere koy.
- **Reklam içeriyor mu?** → Evet
- **Veri güvenliği formu:** Reklam kimliği ve cihaz bilgileri AdMob tarafından toplanır, şifreli aktarılır, reklam amaçlıdır.
- **Hedef kitle ve içerik:** Hedef yaş grubunu seç. **13 yaş altını seçersen** Google'ın "Aileler" politikası devreye girer (yalnızca aile sertifikalı reklam ağları, ek kurallar). Genel kitle (13+) seçmek daha basittir; oyun yine herkese uygun reklamlarla sınırlı.
- **İçerik derecelendirmesi anketi:** Şiddet yok, kumar yok → büyük olasılıkla "3+".
- **Mağaza kaydı:** Kısa/uzun açıklama, 512×512 simge, 1024×500 öne çıkan görsel, en az 2 ekran görüntüsü.

## 6. Reklam dışında gelir

- **"Reklamları kaldır" satın alımı** (ör. 49,99 TL) — Google Play Faturalandırma ile eklenebilir.
- **Altın paketleri** — oyun içi para satışı.

İkisi de ayrı bir adım; istersen sonra eklerim.

---

**Kısaca sıra:** Play Console hesabı → anahtarı oluştur, GitHub'a ekle → AdMob hesabı, kimlikleri bana gönder → kapalı test (12 kişi, 14 gün) → mağaza kaydı ve formlar → yayın.
