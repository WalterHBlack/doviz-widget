# Döviz Cepte

Kotlin ile Android döviz çevirici ve ana ekran widget'ı. Android 8.0 ve üzeri.

## Özellikler

- 160 para birimi arasında çeviri; virgül veya noktayla ondalık giriş ve dört işlem.
- Kod ve isimle arama, bayraklı seçim listesi ve favori filtresi.
- Ayrı ana sayfa ve widget favorileri; başlangıçta USD, EUR, TRY, GBP. Widget en fazla dört favori gösterir.
- Önizlemeli ayarlar: sistem, açık ve koyu tema; küçük, normal ve büyük arayüz.
- Son başarılı kur kaydını çevrimdışı kullanma; kaynak tarihi ve alınma zamanı.
- Widget'tan çeviriciyi açma ve elle yenileme; sayı girişinde kısmi güncelleme.
- WorkManager ile yaklaşık 6 saatte bir bağlantı uygunsa güncelleme; kesin zaman garantisi yok.

Veri: [Frankfurter v2](https://frankfurter.dev/), merkez bankaları ve resmî kaynaklardan derlenen referans kurlar. **Anlık banka alış/satış fiyatları değildir.** API anahtarı gerekmez. Birimlerin kur tarihleri farklı olabilir; uygulama kaydın tarih aralığını gösterir. Hafta sonu ve tatillerde son yayımlanan kur kullanılabilir.

## Dosyaları öğren

[Türkçe dosya haritası ve örnekler](docs/OGRENME.md)

## Derleme

Gerekenler: JDK 17 veya 21, Android SDK 36. Android Studio'da bu klasörü açabilirsin.
SDK konumunu yerel `local.properties` dosyasında `sdk.dir=...` olarak belirt; bu dosya GitHub'a gitmez.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Bu geliştirme APK'sıdır; mağaza yayını değildir.
Telefona aktarıp aç; Android istediğinde dosyayı açan uygulamaya kurulum izni ver.
Widget için ana ekrana uzun basıp **Widget'lar → Döviz Cepte** yolunu kullan.

## Geri dönüş kuralı

Her değişiklik turundan önce mevcut durum commit edilir, GitHub'a gönderilir ve uzak kayıt doğrulanır.
Başlangıç sürümü: `b758e23`. Anahtarlar, şifreler, makineye özel ayarlar ve APK'lar kaynak deposuna eklenmez.
