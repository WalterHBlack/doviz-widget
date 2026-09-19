# Döviz Cepte

Kotlin ile Android döviz çevirici ve ana ekran widget'ı. Android 8.0 ve üzeri.

## İlk sürüm

- TRY, USD, EUR ve GBP arasında çeviri; virgül veya noktayla ondalık giriş.
- Tek dokunuşla para birimlerini değiştirme.
- Kalıcı favoriler; widget'ta favorilerin TL karşılığı.
- Sistem, açık ve koyu tema.
- Son başarılı kur kaydını çevrimdışı kullanma; kaynak tarihi ve alınma zamanı.
- Widget ekleme düğmesi, widget'tan çeviriciyi açma ve elle yenileme.
- WorkManager ile yaklaşık 6 saatte bir bağlantı uygunsa güncelleme; kesin zaman garantisi yok.

Veri: [Frankfurter v2](https://frankfurter.dev/), ECB sağlayıcısı. **Günlük referans kurlarıdır; anlık banka alış/satış fiyatları değildir.** API anahtarı gerekmez. Hafta sonu ve tatillerde son yayımlanan iş günü kuru kullanılabilir.

## Dosyaları öğren

[Türkçe dosya haritası ve örnekler](docs/OGRENME.md)

## Derleme

Gerekenler: JDK 17 veya 21, Android SDK 35. Android Studio'da bu klasörü açabilirsin.
SDK konumunu yerel `local.properties` dosyasında `sdk.dir=...` olarak belirt; bu dosya GitHub'a gitmez.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Bu geliştirme APK'sıdır; mağaza yayını değildir.
Telefona aktarıp aç; Android istediğinde dosyayı açan uygulamaya kurulum izni ver.
Widget için uygulamadaki **Widget ekle** düğmesini veya ana ekrana uzun basıp **Widget'lar → Döviz Cepte** yolunu kullan.

## Geri dönüş kuralı

Her değişiklik turundan önce mevcut durum commit edilir, GitHub'a gönderilir ve uzak kayıt doğrulanır.
Başlangıç sürümü: `b758e23`. Anahtarlar, şifreler, makineye özel ayarlar ve APK'lar kaynak deposuna eklenmez.
