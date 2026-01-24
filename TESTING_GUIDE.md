# Bankacılık Platformu - Kapsamlı Test Rehberi

Bu doküman, Kubernetes (Minikube) üzerinde çalışan mikroservis tabanlı bankacılık uygulamasının uçtan uca (End-to-End) test edilmesi için hazırlanmıştır.

---

## 1. Ön Hazırlık ve Altyapı

Testlere başlamadan önce Kubernetes cluster'ının ve servislerin ayakta olduğundan emin olmalıyız.

### 1.1. Cluster Durumunu Kontrol Etme
Tüm podların `Running` durumunda olduğunu doğrulayın:
```bash
kubectl get pods
```
*Beklenen Durum:* Tüm servislerin (api-gateway, identity, transaction, notification, postgres, kafka, redis, vb.) `1/1 Running` durumunda olması gerekir.

### 1.2. Port Yönlendirme (Port-Forward)
Servislere yerel makinenizden erişebilmek için aşağıdaki tünelleri açmanız gerekmektedir. Her bir komutu ayrı bir terminal sekmesinde çalıştırın:

1.  **API Gateway (Uygulama Erişimi):**
    ```bash
    kubectl port-forward svc/api-gateway 8080:8080
    ```
2.  **Grafana (İzleme):**
    ```bash
    kubectl port-forward svc/grafana 3000:3000
    ```
3.  **Zipkin (Trace):**
    ```bash
    kubectl port-forward svc/zipkin 9411:9411
    ```
4.  **Prometheus (Metrikler):**
    ```bash
    kubectl port-forward svc/prometheus 9090:9090
    ```

**ÖNEMLİ:** Tüm test istekleri **API Gateway (localhost:8080)** üzerinden yapılacaktır.

---

## 2. Test Senaryoları (Adım Adım)

Bu senaryoda, sıfırdan bir kullanıcı oluşturup, hesap açıp, para transferi yaparak döngüyü tamamlayacağız.

### Adım 1: Yeni Kullanıcı Kaydı (Register)

Sisteme giriş yapabilmek için önce kayıt olmalıyız. TCKN algoritma kontrolü vardır, rastgele sayı giremezsiniz.

*   **Amaç:** Veritabanına yeni bir kullanıcı eklemek.
*   **Endpoint:** `POST http://localhost:8080/auth/register`

**cURL Komutu:**
```bash
curl -X POST http://localhost:8080/auth/register \
-H "Content-Type: application/json" \
-d '{
    "username": "testuser1",
    "password": "password123",
    "tckn": "10000000146",
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com"
}'
```

**Beklenen Yanıt:**
```text
Kullanıcı başarıyla kaydedildi
```

---

### Adım 2: Giriş Yapma ve Token Alma (Login)

Kayıtlı kullanıcı ile giriş yaparak, diğer işlemlerde kullanacağımız "Anahtar"ı (JWT Token) alacağız.

*   **Amaç:** Kimlik doğrulaması yapıp `accessToken` almak.
*   **Endpoint:** `POST http://localhost:8080/auth/token`

**cURL Komutu:**
```bash
curl -X POST http://localhost:8080/auth/token \
-H "Content-Type: application/json" \
-d '{
    "username": "testuser1",
    "password": "password123"
}'
```

**Beklenen Yanıt (Örnek):**
```json
{
    "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJ...",
    "refreshToken": "..."
}
```

⚠️ **DİKKAT:** Bu `accessToken` değerini kopyalayın! Aşağıdaki tüm adımlarda `<TOKEN_BURAYA>` yazan yere bu uzun metni yapıştıracaksınız.

---

### Adım 3: Banka Hesabı Oluşturma

Artık elimizde bir kimlik (Token) var. Bankaya gidip "Bana bir vadesiz hesap aç" diyoruz.

*   **Amaç:** Kullanıcıya ait bir IBAN oluşturmak.
*   **Endpoint:** `POST http://localhost:8080/api/v1/accounts`
*   **Header:** `Authorization: Bearer <TOKEN>`

**cURL Komutu:**
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
-H "Authorization: Bearer <TOKEN_BURAYA>" \
-H "Content-Type: application/json" \
-d '{
    "currency": "TRY"
}'
```

**Beklenen Yanıt (Örnek):**
```json
{
    "message": "Account created successfully.",
    "data": {
        "id": 1,
        "iban": "TR123456789012345678901234",
        "balance": 0,
        "currency": "TRY",
        "userId": 1
    }
}
```

⚠️ **NOT:** Yanıttaki `iban` değerini not edin!

---

### Adım 4: Hesaba Para Yatırma (Deposit)

Hesap açıldı ama bakiyesi 0. Transfer yapabilmek için önce para yatırmalıyız.

*   **Amaç:** Hesabın bakiyesini artırmak.
*   **Endpoint:** `POST http://localhost:8080/api/v1/accounts/{iban}/deposits`

**cURL Komutu:**
(Aşağıdaki `TR...` kısmını kendi oluşturduğunuz IBAN ile değiştirin)
```bash
curl -X POST http://localhost:8080/api/v1/accounts/TR123456789012345678901234/deposits \
-H "Authorization: Bearer <TOKEN_BURAYA>" \
-H "Content-Type: application/json" \
-d '{
    "amount": 1000.00
}'
```

**Beklenen Yanıt:**
```json
{
    "message": "Deposit successful.",
    "data": {
        "iban": "TR...",
        "balance": 1000.00,
        ...
    }
}
```

---

### Adım 5: Para Transferi (Transaction)

Şimdi parası olan hesabımızdan, başka bir hesaba para göndereceğiz.

*   **Amaç:** İki hesap arasında bakiye değişimi yapmak ve işlem kaydı oluşturmak.
*   **Endpoint:** `POST http://localhost:8080/api/v1/transactions`

**cURL Komutu:**
(`fromIban`: Sizin IBAN'ınız, `toIban`: Rastgele bir IBAN veya oluşturduğunuz ikinci bir hesap)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
-H "Authorization: Bearer <TOKEN_BURAYA>" \
-H "Content-Type: application/json" \
-d '{
    "fromIban": "TR123456789012345678901234",
    "toIban": "TR987654321098765432109876",
    "amount": 150.00
}'
```

**Beklenen Yanıt:**
```json
{
    "message": "Transaction successful",
    "transactionId": "..."
}
```

---

## 3. Grafana ile İzleme (Monitoring)

Yaptığınız işlemlerin sisteme nasıl yansıdığını görmek için:

1.  **Grafana'ya Girin:** `http://localhost:3000` (Kullanıcı: `admin`, Şifre: `admin`)
2.  **Dashboard:** "Banking Service Dashboard" isimli paneli açın.
3.  **Veri Akışı:**
    *   **Transaction Success Rate:** Para transferi (Adım 5) yaptıkça bu grafiğin değiştiğini göreceksiniz.
    *   **Response Time:** İsteklerin ne kadar sürede cevaplandığını izleyebilirsiniz.

---

## 4. Sık Karşılaşılan Hatalar ve Çözümleri

| Hata Kodu | Mesaj | Olası Sebep | Çözüm |
| :--- | :--- | :--- | :--- |
| **401** | Unauthorized | Token eksik veya hatalı. | Token'ı doğru kopyaladığınızdan emin olun. Token süresi dolmuş olabilir, tekrar login olun. |
| **400** | Bad Request | JSON formatı hatalı veya TCKN geçersiz. | JSON parantezlerini kontrol edin. Geçerli bir TCKN kullanın. |
| **500** | Internal Server Error | `X-User-Id` header eksik olabilir. | Identity ve Gateway servislerini yeniden başlatın ve **yeni bir token** alın. |
| **Connection Refused** | - | Port-forward çalışmıyor veya pod çökmüş. | `kubectl get pods` ile durumu kontrol edin ve port-forward komutunu yeniden başlatın. |
