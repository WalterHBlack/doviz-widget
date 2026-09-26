# Döviz Cepte: dosyaları tanıyalım

Bu projede Kotlin davranışı, Jetpack Compose uygulama ekranını, Jetpack Glance ana ekran widget'ını oluşturur.

## Önce şu sırayla oku

1. `app/src/main/java/com/walterhblack/dovizwidget/MainActivity.kt`: Android'in uygulamayı açtığı kapı. `setContent` hangi ekranı göstereceğimizi söyler.
2. `ui/ConverterScreen.kt`: Tutar kutusu, sabit klavye, para birimi seçicileri, sonuç, favoriler ve tema düğmeleri. `@Composable` bir fonksiyonun ekran çizdiğini belirtir.
3. `ui/ConverterViewModel.kt`: Yükleniyor mu, hata var mı, hangi kurlar mevcut? Ekranın ihtiyaç duyduğu bu durumları tutar. Telefon dönse de ViewModel korunur.
4. `data/CurrencyMath.kt`: Yalnızca sayı hesabı. Android ekranını bilmez; bu yüzden hızlıca test edebiliriz.
5. `data/RateRepository.kt`: İnternetten veriyi alır, doğrular, telefonda saklar. Ekran internet adresiyle doğrudan uğraşmaz.
6. `widget/RatesWidget.kt`: Ana ekranda görünen kurlar ve Yenile düğmesi. Uygulama ile widget aynı kaydı kullanır.
7. `widget/RateRefreshWorker.kt`: Arka planda kur alma işi. İnternet hatasında sınırlı sayıda yeniden dener.
8. `DovizApplication.kt`: Android uygulama işlemini başlatınca, altı saatlik arka plan işini bir kez planlar. Android bu zamanı pil ve bağlantıya göre erteleyebilir.

Yukarıda kısaltılan yollar `app/src/main/java/com/walterhblack/dovizwidget/` altında yer alır.

## Bir dokunuşun yolculuğu

**Yenile → ConverterViewModel.refresh → RateRepository.refresh → internet → kayıt → ekran ve widget**

**Tutar yaz → CurrencyMath.parseAmount → CurrencyMath.convert → sonuç yazısı**

Tutar klavyesi kaydırılabilir ekran Column'unun dışında durur; bu yüzden liste yukarı aşağı giderken klavye altta sabit kalır. İlk üç sütun 7-8-9, 4-5-6, 1-2-3 sırasındadır; 0 en alt orta tuştadır. Sağ sütunda sırasıyla C, kur yenileme, virgül ve silme vardır. Tuşların siyah kenarlıkları çizimdeki ızgara çizgilerini oluşturur. Yenileme tuşu ekrandaki Yenile düğmesiyle aynı `model.refresh()` işlemini çağırır. Panel iki kenara kadar uzanır.

Üst ortadaki yeşil çizgi tutma yeridir: basılı tutup sürükleyince klavye parmağı takip eder, bırakınca kapalı, normal veya tam ekran konumlarından en yakınına yumuşakça oturur. Tek başına dokunmak klavyeyi hareket ettirmez. Geçişte panel yalnızca kayarak boyut değiştirir; ekran ve tuşlar soluklaşmaz. Normal durumda sonuçları kaydırırken klavye yerinden oynamaz. `KeyboardMode` bu üç görünüm durumunu tutar; bu bilgi ekran yeniden oluşturulunca `rememberSaveable` ile korunur.

Tuş ızgarası açılıp kapanırken 272 dp yüksekliğini korur; kapanışta sıkışmak yerine panelin altına kayar. Tam ekran açıldığında tuşlar altta kalır, üstte tutar için alan açılır. Tutar başlığı ve değer yalnızca tamamına yer olduğunda gösterilir. Sürükleme ve bırakma animasyonu aynı yükseklik değerini kullanır; böylece bırakırken eski konuma dönülmez.

Tutar yazarken yeni internet isteği yapmayız. Kur tablosunu bir kez alıp hesaplamayı telefonda yaparız.

## Kotlin'den dört küçük parça

```kotlin
val code = "USD"       // val: yeniden atanmaz
var amount = "1"       // var: değişebilir
fun double(x: Int) = x * 2 // fun: bir işlem tanımlar
val result: String? = null // ?: değer henüz olmayabilir
```

`rememberSaveable`, ekrandaki tutarı ve seçimleri ekran yeniden oluşturulduğunda geri getirir.
`mutableStateOf`, Compose'a "bu değer değişince ilgili ekranı yenile" der.
`suspend`, internet gibi bekleyen işleri arayüzü kilitlemeden yürütmemize olanak verir. Bu projede bağlantı ayrıca `Dispatchers.IO` üzerinde çalışır.

## Kur hesabı

Tüm kurlar 1 EUR karşılığı olarak gelir. Örnek **uydurma test verisi**: 1 EUR = 1,25 USD, 1 EUR = 50 TRY.

10 USD → TRY: `10 / 1,25 × 50 = 400 TRY`.

`BigDecimal` kullanıyoruz; para hesabında ondalık değerlerle kontrollü çalışmak ve yalnızca gösterirken yuvarlamak için.

## Kotlin olmayan dosyalar

| Dosya | Görevi |
|---|---|
| `app/src/main/AndroidManifest.xml` | Android'e açılış ekranını, internet iznini ve widget alıcısını bildirir. |
| `app/src/main/res/xml/rates_widget_info.xml` | Widget'ın boyutunu ve yeniden boyutlandırma davranışını tanımlar. |
| `app/src/main/res/drawable/ic_currency.xml` | Uygulama simgesi; vektör çizim. |
| `app/build.gradle.kts` | Android sürümleri ve kullandığımız kütüphaneler. |
| `build.gradle.kts` | Derleme eklentilerinin sürümleri. |
| `settings.gradle.kts` | Projenin modülleri ve kütüphane depoları. |
| `gradlew.bat` | Windows'ta projeyi aynı Gradle sürümüyle derleyen giriş dosyası. |
| `.gitignore` | GitHub'a gitmeyecek dosyalar: derleme çıktıları, yerel ayarlar ve anahtarlar. |

## Testler neyi koruyor?

`app/src/test/java/com/walterhblack/dovizwidget/data/` altında:
- `CurrencyMathTest.kt`: çapraz kur, ters dönüşüm, aynı para birimi, Türkçe virgül, hatalı giriş ve sıfır kur.
- `RateSnapshotTest.kt`: API yanıtının tam olması, tarihlerin eşleşmesi, pozitif kur ve kaydet/oku tutarlılığı.

## Küçük denemeler

GitHub'a başlangıç durumunu kaydettikten sonra:
1. `ConverterScreen.kt` içindeki "Bir bakışta döviz." yazısını değiştirip yeniden derle.
2. Aynı dosyadaki `Color(0xFFA5F3CF)` değerini değiştir; koyu temanın vurgu rengini gözle.
3. `CurrencyMathTest.kt` içine 0 USD → TRY sonucunun 0 olduğunu kontrol eden bir test ekle.

Yeni para birimi eklemek yalnızca menüye bir satır koymak değildir: servis isteği, doğrulama ve widget listesi de birlikte güncellenmelidir.
