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
- **Kombo:** Değerli eşyaları (altın, kese, elmas) art arda hızlıca bulursan her biri daha çok altın verir (x2'ye kadar). Hayalet komboyu bozar.
- **Özel bölümler:** Her 5. bölüm 🏆 *Hazine Halısı* (neredeyse iki kat hazine, daha çok elmas). 6. bölümden sonra bazı bölümler 🦠 *İnatçı Kir*: temizlemediğin kir yeniden yayılır.
- **Sonuç ekranı:** Animasyonlu yıldızlar, bölüm rekoru (en hızlı süre), hız bonusu, en iyi kombo ve kazanılan başarımlar.
- **Duraklatma menüsü:** Devam, yeniden başla, ses aç/kapa, ana menü. Uygulama arka plana geçince oyun kendiliğinden duraklar.

## ✨ 3B grafik (OpenGL ES 3.0)

Halı yüzeyi gerçek zamanlı ışıkla çizilir:

- **Yün tüyleri ve düğümler** ışığı yakalar; telefonu eğince ışık kayar, kadife parlaması değişir.
- **Kir** kabuklu ve kabarıktır, kenarları doğal ve düzensizdir; kahve gibi lekelerin kenarı koyu halka yapar.
- **Köpük** kabarcık kabarcık kabarır ve parlar; **ıslak** yerler koyulaşır ve parıldar.
- **Silkeleme** halının üstünden geçen bir dalga olarak görünür.
- **Cilalı parke zemin** ışığı yansıtır, halının altında yumuşak gölge vardır.

Cihaz OpenGL ES 3.0 desteklemiyorsa ya da Ayarlar'dan kapatılırsa oyun 2B çizime döner. Shader'lar `app/src/main/assets/shaders/` içindedir.

## 🧶 Halı türleri ve hasar

| Halı | Dikkat |
|---|---|
| 🐑 Yün | Dayanıklı |
| 🧵 Sentetik | Yağ kolay çıkar, alkol (çözücü) eritir |
| 🧣 İpek | Sert fırça, güçlü asit ve oksijen soldurur — 1,6 kat altın |
| 🏺 Antika | Çok hassas ve çok değerli — 2 kat altın |

Yanlış alet ya da kimya halıyı soldurur (hasar çubuğu). Hasar sınırı aşılırsa halı mahvolur; az hasar yıldızı ve bonusu düşürür. Formül Laboratuvarı her formülün hangi halıda güvenli olduğunu gösterir.

## Gerçek halı yıkama

- **🥢 Çöpler:** Muz kabuğu, çorap, kola kutusu, gazete... Cımbızla tutup sağ alttaki 🗑️ çöp kutusuna at. Çöpün altı temizlenemez.
- **🚿 Durulama:** Deterjan köpük bırakır; köpüğü su ile durulamadan halı temiz sayılmaz. Islak halı %35 daha kolay temizlenir, zamanla kurur.
- **📱 Silkeleme:** Telefonu salla (ya da 🧺 düğmesi): halı silkelenir, toz ve hafif çöpler camdan uçar. Pencerede halı silkeleyen teyze görünür.
- **🧪 Zorlu lekeler:** Kahve, yağ, şarap, pas, mürekkep, sakız. Her biri farklı kimya ister (oksijen, yağ çözücü, asit, çözücü...). Bölüm ilerledikçe yeni ve daha çok leke çıkar.
- **🧰 Aletler:** 🧽 Sünger, 🧹 Sert Fırça, 🚿 Su, 🥢 Cımbız, 🌀 Süpürge (toz), 🧊 Buz (sakız).
- **🧪 Formül Laboratuvarı:** Karbonat, sirke, limon tuzu, oksijenli su, enzim, alkol, yumuşatıcı, lavanta esansı ile kendi deterjanını yap. Asit ile baz birbirini nötrler! 3 formül yuvası.
- **🧒 Şemi ve 👧 Nils:** Şemi arada bir halının üstünden geçip çamurlu ayak izi bırakır. Nils onu kızdırınca Şemi kahve döker, çöp atar. Şemi'ye dokun, kaçsın.
- **👴 Acı Dede:** Evin reisi; işi o verir, sonunda halıyı o değerlendirir.

## 🏁 Yarışma

Aynı telefonda 2–4 kişi sırayla oynar; herkes aynı halıyı temizler. Bitiren 1000 + kalan saniye × 20 + bulduğu altın puan alır. Sonunda madalyalı sıralama ve rövanş.

## Daha fazlası

- **🔊 Ses efektleri ve titreşim:** Bütün sesler (altın, elmas, patlama, hayalet, kombo, zafer...) dosya kullanılmadan kodla üretilir.
- **🏆 12 başarım:** Her biri altın ödüllü (ör. *Elmas Avcısı*, *Kombo Ustası*, *Şimşek*). Başarımlar ekranında ilerleme ve istatistikler görünür.
- **🎁 Günlük ödül:** Her gün girdiğinde altın; 7 gün üst üste gelirsen büyük ödül ve bedava 🧲.
- **⚙️ Ayarlar:** Ses, titreşim, eğitimi yeniden gösterme, ilerlemeyi sıfırlama.
- **🎓 İlk oyunda eğitim:** Halıyı nasıl ovalayacağını gösteren hareketli el.

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

**Karakterler:** 👨 Hasan Usta, 👩 Ayşe Abla (+%25 altın), 🏃 Turbo Tarık (geniş fırça), 👵 Songül Nine (+12 sn), 🌹 Gül Hala (hızlı durulama), 💃 Tef Teyze (camdan halı silkeleme ustası), 👓 Güzi Hala (leke uzmanı), 🐱 Şanslı Kedi (daha çok hazine), 🤖 Süpürgeç (robot yardımcı), 👴 Acı Dede (evin reisi: çöpler kendiliğinden toplanır).

## APK'yı indirmek

📲 **Telefondan doğrudan indir:** https://github.com/burhanettiny/altin-hali/releases/latest/download/AltinHali.apk

Bağlantıyı telefonun tarayıcısında açın, inen `AltinHali.apk` dosyasına dokunup kurun ("bilinmeyen kaynaklar" izni istenebilir). `main` dalına her gönderimde GitHub Actions yeni APK'yı derleyip [Sürümler](https://github.com/burhanettiny/altin-hali/releases) sayfasında yayınlar; bağlantı her zaman en yenisini indirir.

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
| `OyunView.kt` | Oyun ekranı: dokunma, efektler, arayüz, duraklatma ve sonuç ekranı |
| `OyunActivity.kt` | Oyun ekranını barındırır |
| `MainActivity.kt` | Menü, mağaza, karakterler, başarımlar, ayarlar, günlük ödül |
| `Kayit.kt` | Altın, bölüm, satın alımlar, ayarlar ve istatistiklerin kaydı |
| `Ses.kt` | Kodla üretilen ses efektleri ve titreşim |
| `Basarimlar.kt` | Başarımlar ve ödülleri |
| `MenuArka.kt` | Menülerin hareketli arka planı |
