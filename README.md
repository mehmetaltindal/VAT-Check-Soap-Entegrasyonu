# SOAP/VIES Fatura Doğrulama

Bir faturanın satıcı ve alıcı KDV numaralarını, fatura kesilmeden önce Avrupa
Komisyonu VIES SOAP servisi üzerinden doğrulayan Java 17 / Spring Boot
uygulamasıdır.

## Gereksinimler

- Java 17 veya üzeri
- Maven 3.9 veya üzeri

## Build ve test

```bash
mvn clean verify
```

Testler gerçek VIES servisine bağlanmaz. SOAP sözleşmesi, fault, retry ve timeout
senaryoları yerel mock/stub sunucularla deterministik olarak çalışır.

Uygulamayı başlatmak için:

```bash
mvn spring-boot:run
```

veya:

```bash
mvn clean package
java -jar target/vies-invoice-0.0.1-SNAPSHOT.jar
```

## REST API

`POST /api/v1/invoices/validate`

```bash
curl --request POST 'http://localhost:8080/api/v1/invoices/validate' \
  --header 'Content-Type: application/json' \
  --data '{
    "invoiceNumber": "INV-2026-001",
    "seller": {
      "countryCode": "DE",
      "vatNumber": "129273398"
    },
    "buyer": {
      "countryCode": "DE",
      "vatNumber": "129273398"
    }
  }'
```
Request örneği yukardaki gibidir. Ayrıca maile ek olarak Bruno üzerinden oluşturduğum
collection'da rest isteği yer almaktadır.

Her taraf için `countryCode` ve `vatNumber` ayrı alınır. Ülke kodu trim edilip
büyük harfe çevrilir; KDV numarasına yalnız trim uygulanır.

İki taraf da geçerliyse örnek cevap:

```json
{
  "invoiceNumber": "INV-2026-001",
  "issuable": true,
  "message": "Fatura kesilebilir",
  "seller": {
    "status": "VALID",
    "countryCode": "DE",
    "vatNumber": "129273398",
    "requestDate": "2026-07-18+02:00",
    "name": "Example GmbH",
    "address": "Berlin"
  },
  "buyer": {
    "status": "VALID",
    "countryCode": "DE",
    "vatNumber": "129273398",
    "requestDate": "2026-07-18+02:00",
    "name": "Example GmbH",
    "address": "Berlin"
  }
}
```

Satıcı geçersizse alıcı sorgulanmaz:

```json
{
  "invoiceNumber": "INV-2026-001",
  "issuable": false,
  "message": "Fatura kesilemez: satıcı KDV numarası geçersiz",
  "seller": {
    "status": "INVALID",
    "countryCode": "DE",
    "vatNumber": "000000000",
    "requestDate": "2026-07-18"
  },
  "buyer": {
    "status": "NOT_CHECKED",
    "countryCode": "FR",
    "vatNumber": "40303265045"
  }
}
```

`valid=false` bir HTTP veya teknik hata değildir; HTTP `200` ve
`issuable=false` olarak döner.

Teknik hata gövdesi yalnız kod ve mesaj içerir:

```json
{
  "errorCode": "VIES_TIMEOUT",
  "message": "VIES servisi belirlenen süre içinde yanıt vermedi"
}
```

## Hata eşlemeleri

| Durum | HTTP | Kod |
|---|---:|---|
| Geçersiz REST isteği | 400 | `VALIDATION_ERROR` |
| VIES `INVALID_INPUT` fault | 422 | `VIES_INVALID_INPUT` |
| Geçici fault veya bağlantı sorunu | 503 | `VIES_SERVICE_UNAVAILABLE` |
| Retry sonrası timeout | 504 | `VIES_TIMEOUT` |
| Bilinmeyen SOAP fault | 502 | `VIES_UPSTREAM_ERROR` |
| XSD'ye uymayan VIES cevabı | 502 | `SOAP_RESPONSE_SCHEMA_INVALID` |
| Uygulamanın ürettiği geçersiz SOAP isteği | 500 | `SOAP_REQUEST_SCHEMA_INVALID` |

Geçici kabul edilen fault değerleri:

- `GLOBAL_MAX_CONCURRENT_REQ`
- `MS_MAX_CONCURRENT_REQ`
- `SERVICE_UNAVAILABLE`
- `MS_UNAVAILABLE`
- `TIMEOUT`

`INVALID_INPUT`, bilinmeyen fault ve `valid=false` retry edilmez.

## Yapılandırma

Varsayılanlar `src/main/resources/application.yml` içindedir:

| Ortam değişkeni | Varsayılan | Açıklama |
|---|---|---|
| `VIES_ENDPOINT` | Resmî VIES endpoint | SOAP servis adresi |
| `VIES_CONNECT_TIMEOUT` | `2s` | Bağlantı kurma timeout'u |
| `VIES_READ_TIMEOUT` | `5s` | Cevap okuma timeout'u |
| `VIES_MAX_ATTEMPTS` | `2` | İlk çağrı dahil toplam girişim |
| `VIES_RETRY_DELAY` | `500ms` | Girişimler arası sabit bekleme |

Örnek:

```bash
VIES_READ_TIMEOUT=8s VIES_MAX_ATTEMPTS=2 mvn spring-boot:run
```

## Tasarım kararları

- REST DTO'ları service katmanına taşınmaz. Controller DTO-domain dönüşümünü
  yapar; iş akışı `Invoice` ve `VatIdentifier` modelleriyle çalışır.
- Satıcı ve alıcı sıralı sorgulanır. Satıcı geçersizse dış servise gereksiz ikinci
  çağrı yapılmaz ve alıcı `NOT_CHECKED` olarak raporlanır.
- Spring-WS `WebServiceTemplate` ve XSD'den Maven ile üretilen JAXB sınıfları
  kullanılır. Üretilen kaynaklar `target/generated-sources` altında kalır.
- `ValidatorHelper`, thread-safe `Schema` nesnesini bir kez oluşturur ve her
  çağrıda yeni `Validator` kullanır.
- SOAP istek payload'ı gönderilmeden önce, normal SOAP cevap payload'ı ise JAXB
  unmarshalling yapılmadan önce XSD schema'sına göre doğrulanır.
- SOAP Fault gövdeleri iş cevabı XSD'sine ait olmadığı için response
  validation interceptor'ından geçirilmez.
- Retry işlemi yalnız `resilience4j-retry` çekirdek modülüyle uygulanır.
  Endpoint, timeout ve retry ayarları `ViesProperties` üzerinden yönetilir.
- Yalnzıca ihtiyaç duyulan modülün kullanılabilmesi, spring boot ile uyumlu olması,
  annotation ve `application.yml` üzerinden kolay konfigüre edilebilmesi sebebiyle
  `resilience4j` kütüphanesini tercih ettim.
- Dış servis detaylarını, iş mantığından ayırmak için interface'ler kullanılır.
  Böylelikle kod değiştirilmeye kapalı, geliştirilmeye açık duruma gelir.
  Örneğin AB bölgesi için değil MENA bölgesi için aynı sorguyu yapmak gerektiğinde
  mevcut kodu değiştirmek yerine yeni bir service implement ederek geliştirmeleri
  sağlayabiliriz. İlerleyen süreçte `Factory Pattern` ile business seçim gelen requeste
  göre otomatik servis seçimiyle karşılanabilir.
- Birim testleri için JUnit ve mocklamalar için Mockito kütüphanesi kullanılır.
- Loglama için Log4j kütüphanesi kullanılmıştır.
