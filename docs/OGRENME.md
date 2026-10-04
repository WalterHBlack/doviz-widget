# Döviz Cepte: dosyaları tanıyalım

Bu projede Kotlin davranışı, Jetpack Compose uygulama ekranını, Jetpack Glance ana ekran widget'ını oluşturur.

## Önce şu sırayla oku

1. `app/src/main/java/com/walterhblack/dovizwidget/MainActivity.kt`: Android'in uygulamayı açtığı kapı. `setContent` hangi ekranı göstereceğimizi söyler.
2. `ui/ConverterScreen.kt`: Güncelleme zamanı, bayraklı kur satırları, kaynak seçimi, karşılıklar, para birimi yönetimi ve açılıp kapanan klavye. Widget favorileri para birimi yönetme penceresinden seçilir. `@Composable` bir fonksiyonun ekran çizdiğini belirtir.
3. `ui/ConverterViewModel.kt`: Yükleniyor mu, hata var mı, hangi kurlar mevcut? Ekranın ihtiyaç duyduğu bu durumları tutar. Telefon dönse de ViewModel korunur.
4. `data/CurrencyMath.kt`: Yalnızca sayı hesabı. Android ekranını bilmez; bu yüzden hızlıca test edebiliriz.
5. `data/RateRepository.kt`: İnternetten veriyi alır, doğrular, telefonda saklar. Ekran internet adresiyle doğrudan uğraşmaz.
6. `widget/RatesWidget.kt`: Ana ekranda görünen kurlar ve Yenile düğmesi. Uygulama ile widget aynı kaydı kullanır.
7. `widget/RateRefreshWorker.kt`: Arka planda kur alma işi. İnternet hatasında sınırlı sayıda yeniden dener.
8. `DovizApplication.kt`: Android uygulama işlemini başlatınca, altı saatlik arka plan işini bir kez planlar. Android bu zamanı pil ve bağlantıya göre erteleyebilir.

Yukarıda kısaltılan yollar `app/src/main/java/com/walterhblack/dovizwidget/` altında yer alır.

`ui/CurrencyDialogs.kt` aramalı para birimi seçimini, yıldızlı favorileri, bayrak eşlemelerini ve tam ekran ayarları içerir. Kod veya Türkçe isimle arama yapılır; Tümü/Favoriler filtresi kullanılır. Ayarlar tema ve arayüz boyutunu örnek satırda gösterir. `ConverterScreen.kt` ana listedeki satırları ve seçilen kaynağı yönetir.

Desteklenen birim sayısı 160'tır. ILS katalogda ve servis isteğinde bulunmaz. Frankfurter isteğinin `quotes` listesi `currencyNames` üzerinden üretilir; farklı merkez bankaları ve resmî kaynaklardan kurlar alınır. Eski sürümün daha küçük kur kaydı okunabilir; eksik birimler için açılışta güncelleme istenir. İnternet yoksa yeni birimlerin karşılığı veri gelene kadar çizgiyle gösterilir. `RateSnapshot.rateDates` her birimin tarihini, `source` kaynağı saklar; farklı tarihli kurların genel etiketi tarih aralığıdır.

## Bir dokunuşun yolculuğu

**Yenile → ConverterViewModel.refresh → RateRepository.refresh → internet → kayıt → ekran ve widget**

**Tutar veya işlem yaz → WidgetCalculator.evaluate → CurrencyMath.convert → bütün kur satırlarının sonucu**

Tutar klavyesi kaydırılabilir ekran Column'unun dışında durur; bu yüzden kur listesini kaydırırken altta sabit kalır. Normal konumunda widget'taki beş sütunlu düzenle sayılar, `00`, virgül, C, silme, eşittir, kur yenileme ve dört işlem tuşları birlikte görünür. İşlem tuşları widget ile aynı yeşili kullanır. Tuşların siyah kenarlıkları çizimdeki ızgara çizgilerini oluşturur. Panel iki kenara kadar uzanır.

Üst ortadaki yeşil çizgi tutma yeridir: sürükleyince klavye parmağı takip eder, bırakınca kapalı veya normal konumdan en yakınına yumuşakça oturur. Çizgiye dokunmak da klavyeyi açıp kapatır. Tam ekran büyütme yoktur. Geçişte sabit boylu panel ekrandan aşağı kayar; tuşlar soluklaşmaz veya sıkışmaz. Normal durumda sonuçları kaydırırken klavye yerinden oynamaz. `KeyboardMode` bu iki görünüm durumunu tutar; bu bilgi ekran yeniden oluşturulunca `rememberSaveable` ile korunur.

Tuş ızgarası açılıp kapanırken 272 dp yüksekliğini korur. Animasyon yalnızca panelin konumunu ve üstteki kaydırılabilir alanın ölçüsünü değiştirir; her karede bütün kur satırlarını yeniden çizmek gerekmez. Sürükleme ve bırakma animasyonu aynı yükseklik değerini kullanır; böylece bırakırken eski konuma dönülmez.

Bayraklı satırlardan birine dokununca o para birimi kaynak olur. Satırlar aralıksız tek bir liste halinde birleşir. Yazılan tutarın seçili para birimindeki değeri aynı satırda, diğer para birimlerine çevrilmiş değerleri diğer satırlarda görünür. Para birimi okları satırların yerini değiştirebilir; yıldızlar widget favorilerini günceller. Tutar yazarken yeni internet isteği yapmayız. Kur tablosunu bir kez alıp hesaplamayı telefonda yaparız.

## Widget hesap makinesi

Sayı, silme ve matematik tuşları bütün Glance görünümünü yeniden oluşturmadan `partiallyUpdateAppWidget` ile satır tutarlarını ve hesaplama mesajını günceller. `WidgetValueViews.kt` ve `widget_values.xml` sabit kimliklerle çalışır. Boyut, kur veya favori değişimi tam görünüm güncellemesi kullanır. Eski widget tuşundan ilk basış yeni düzeni kurar.

Hızlı basışlar sırayla işlenir; her widget'ın girdisi DataStore'a kaydedilir. Açık Glance oturumu boyut değiştirirse eski sayıyı göstermemesi için son girdi oturumda da tutulur. Günlük kur verisi değişmedikçe aynı JSON tekrar çözümlenmez. Performans farkı cihazda ölçülmeden sayısal bir hızlanma iddia edilmez.

Glance `Row` ve `Column` en fazla 10 doğrudan çocuk destekler. Başlık, tutar, liste, boşluklar ve dört klavye satırı aynı köke eklenince son satır bu sınırı aşıyordu. Dört satır artık tek bir klavye `Column` içinde; yüksekliği dört satırın toplamıdır. Bu sorunu yalnızca yazıları veya tuşları küçültmek çözmez.

Widget üstte başlık, ortada en fazla dört favori satırı, altta kur tarihi ve dört satırlı klavyeden oluşur. Ayrı Tutar/Hedef alanı yoktur. Seçili kaynak satır yeşil şeritle belirginleşir; yazılan ifade o satırda, dönüştürülmüş sonuçlar diğer satırlarda görünür. `RatesWidget.kt` boyut hesabında favori sayısını, sistem yazı ölçeğini, dört klavye satırını ve iki satırlık hata mesajını birlikte değerlendirir. Genişlik tabanı 300 dp'dir; yükseklik ihtiyacı içeriğe göre hesaplanır. Küçük yüzeyde alt ölçek sınırı uygulanmaz. Fazla yükseklik tuşlara ve satır içi boşluklara sınırlı olarak dağıtılır; `WidgetValueViews.kt` bu boşlukları native satırlara uygular.

`widget/RatesWidget.kt` kaynak seçimini ve tuş işlemlerini yönetir. Native satırdaki PendingIntent, `SelectWidgetSourceReceiver` üzerinden klavyeyle aynı kilitli işlem yoluna girer. Her widget kendi kaynak birimini ve ifadesini saklar. Kaynağı seçtikten sonraki ilk rakam eski tutarın yerine yazılır. USD satırına 100 yazılınca TRY satırı 100 USD'nin TRY karşılığını gösterir. Widget favorileri uygulamadaki Favoriler > Widget sekmesinden seçilir.

`data/WidgetCalculator.kt` virgüllü tutarları ve dört işlemi hesaplar. Çarpma ve bölme önceliklidir; sıfıra bölme ve eksik işlemler mesajla gösterilir. Tamamlanmış ifade için sonuçlar otomatik güncellenir; eşittir ifadeyi hesaplanan tutarla değiştirir. Eşittirden sonra rakam yeni işlem başlatır, işlem işareti sonuç üzerinden devam eder. Yenileme hariç tuşlar ağ isteği yapmaz. Negatif sonuçların dönüşümü de işaret korunarak gösterilir.

Widget için ilk öneri 5×7 hücrelik büyük alan, küçültülebilen alt sınır ise 220×320 dp'dir. İçerik widget'ın genişlik ve yüksekliğine göre birlikte büyüyüp küçülür; klavye ve favori satırları her iki boyutta da görünür. Üst başlık uygulamayı açar; klavye ve kaynak satırı seçimi uygulamayı açmaz. Başlık ve favori listesi uygulamadaki koyu yeşil yüzey tonlarını kullanır; tuşlar sayı, işlem ve yardımcı tuş olarak ayrı yeşil tonlarla görünür. Tuş sıraları `7 8 9 ÷ C`, `4 5 6 × ⌫`, `1 2 3 − ↻`, `0 00 , + =` şeklindedir.

## Kotlin'den dört küçük parça

```kotlin
val code = "USD"       // val: yeniden atanmaz
var amount = "1"       // var: değişebilir
fun double(x: Int) = x * 2 // fun: bir işlem tanımlar
val result: String? = null // ?: değer henüz olmayabilir
```

`rememberSaveable`, örneğin klavyenin açık veya kapalı durumunu ekran yeniden oluşturulduğunda geri getirir. Tutar her yeni açılışta `0,00` başlar; son kaynak kuru `RateRepository` telefonda saklar.
`mutableStateOf`, Compose'a "bu değer değişince ilgili ekranı yenile" der.
`suspend`, internet gibi bekleyen işleri arayüzü kilitlemeden yürütmemize olanak verir. Bu projede bağlantı ayrıca `Dispatchers.IO` üzerinde çalışır.

## Kur hesabı

Ayarlar > Ondalık basamak seçimi `RateRepository` içinde `decimal_places` olarak saklanır; 0, 2 veya 4 seçilebilir, varsayılan 2'dir. `ConverterViewModel` uygulamayı, `publishWidgetState` widget'ları günceller. Sonuçları biçimlendirme hesabın saklanan hassasiyetini değiştirmez. İlk silme seçilen basamak sayısıyla gösterilmiş tutarı düzenler; yazma sırasında gerçek girdi görünür. Sıfır basamak seçildiğinde virgül de gösterilmez.

Kaynak değişiminden gelen tutar iki ondalıkla gösterilir, hesap hassasiyeti ilk düzenlemeye kadar saklanır. İlk geri silme, görünür iki ondalıklı sayıdan başlar. Sonraki girişlerde seçili satır düzenlenen ifadeyi doğrudan gösterir; örneğin `4900,86 → 4900,8 → 4900,` akışında gizli rakam veya otomatik eklenen sıfır silinmez. `ConverterViewModel.amountIsConversion` bu ayrımı tutar.

Kaynak para birimi değiştiğinde `WidgetCalculator.changeCurrency` mevcut ifadeyi hesaplayıp yeni birime çevirir. Örneğin 100 TRY yazılıyken USD seçilirse dolar karşılığı yeni tutar olur; diğer satırlar aynı değeri göstermeye devam eder. Uygulama tutarı `ConverterViewModel` içinde tutar; widget kendi kalıcı durumunu günceller. Negatif tutarın işareti korunur. Tamamlanmamış işlem veya eksik kur verisinde kaynak değiştirilmez ve hata gösterilir. Kaynak değişiminden sonraki ilk rakam yeni tutar başlatır; işlem tuşları dönüştürülmüş tutarla devam eder.

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
- `WidgetCalculatorTest.kt`: işlem önceliği, Türkçe ondalık, sıfıra bölme ve operatör düzeltmesi.
- `RateSnapshotTest.kt`: temel kurların bulunması, farklı tarihlerin korunması, pozitif kur ve kaydet/oku tutarlılığı.

## Küçük denemeler

GitHub'a başlangıç durumunu kaydettikten sonra:
1. `ConverterScreen.kt` içindeki "Bir bakışta döviz." yazısını değiştirip yeniden derle.
2. Aynı dosyadaki `Color(0xFFA5F3CF)` değerini değiştir; koyu temanın vurgu rengini gözle.
3. `CurrencyMathTest.kt` içine 0 USD → TRY sonucunun 0 olduğunu kontrol eden bir test ekle.

Yeni para birimi eklerken servis desteği, katalog ve bayrak eşlemesi birlikte ele alınır. `widget_values.xml` artık para birimi başına satır içermez: dört genel satırı `WidgetValueViews.kt` seçilen favorilerle doldurur. Böylece katalog büyüdüğünde tuş başına yüzlerce gizli satır güncellenmez.

## Ana sayfa ve widget favorileri

Kur satırına uzun basıp sürüklemek sıralamayı değiştirir. `ConverterScreen.kt` satır konumlarını ve sürüklenen satırı izler; `ConverterViewModel.moveHomeCurrency` yeni sırayı `home_favorites` kaydına yazar. Seçili kaynak ve widget favorileri bu işlemden etkilenmez. Güncelleme zamanı kur listesinin altında gösterilir.

Ana sayfanın kur listesi `ConverterScreen.kt` içinde çerçevesiz ve tam genişlikte çizilir. Seçilen satır köşeleri düz yeşil bir şerittir; diğer satırlar sayfanın arka planını kullanır. Satır içindeki yazı ve bayrak boşlukları korunur.

`CurrencyDialogs.kt` içindeki `HandleOnlySheet` panelin sürükleme hareketini yalnızca üst tutamaçta dinler. Listeyi kaydırmak paneli kapatmaz; üst çizgiyi aşağı çekmek kapatır, Android geri düğmesi de kullanılabilir. Ana sayfa yıldızları yeşil, widget yıldızları altın sarısıdır; sekme adları da aynı renklerle ayrılır.

Ana sayfa ilk açılışta USD, EUR, TRY, GBP sırasıyla başlar. Üstteki Favoriler düğmesi aynı panelde Ana sayfa ve Widget sekmelerini açar. Ana sayfa seçimleri `home_favorites` kaydında sıralı liste olarak tutulur; en az bir birim seçili kalır. Widget seçimleri ayrı `favorites` kaydındadır ve en fazla dört birim alır. Ana sayfa seçimi widget listesini değiştirmez. Son kaynak ana sayfadan kaldırılırsa kalan ilk birim kaynak olur. Satırdaki para birimi değiştirilirse yeni liste de kaydedilir.

Üstteki hesaplanan tutar kartı ve alttaki widget favorileri düğmesi kaldırılmıştır. Hesaplama hataları listenin üstünde gösterilir. Desteklenmeyen birimler eski favori/kur kayıtlarından okunurken filtrelenir; widget'ın eski uzun favori listesi en fazla dört geçerli birime indirilir.
