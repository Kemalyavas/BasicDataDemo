# BasicDataDemo

Android'de üç farklı veri saklama yönteminin tek uygulamada karşılaştırmalı demosu.

## Saklama Yöntemleri

| Alan | Yöntem | Veri Türü |
|------|--------|-----------|
| Profil bilgileri (kullanıcı adı, yaş) | **SharedPreferences** | XML key-value |
| Ayarlar (karanlık mod, bildirim, ziyaret sayacı) | **Preferences DataStore** | Asenkron key-value |
| Not (metin + kayıt zamanı) | **Proto DataStore** | Tipli, şema tabanlı (protobuf) |

## Proto DataStore

- Şema [`app/src/main/proto/user_note.proto`](app/src/main/proto/user_note.proto) dosyasında tanımlı (`UserNote` mesajı).
- `protobuf` Gradle eklentisi `.proto` dosyasından `UserNote` Java sınıfını otomatik üretir.
- [`UserNoteSerializer`](app/src/main/java/com/example/basicdatademo/UserNoteSerializer.kt) veriyi diske serialize/deserialize eder.
- Okuma/yazma `MainActivity` içinde `userNoteDataStore` ile yapılır — tip güvenli, key string'i yok.

## Teknik

- Kotlin, View tabanlı UI (Material 3)
- minSdk 24, targetSdk 36
- `androidx.datastore` 1.0.0, `protobuf-javalite` 3.21.12
