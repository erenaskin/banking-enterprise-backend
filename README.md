# 🏦 Banking Enterprise Backend

Bu proje, modern **DevOps** pratikleri ve **Mikroservis Mimarisi** kullanılarak geliştirilmiş, ölçeklenebilir, hataya dayanıklı ve tam otomatize bir bankacılık altyapısı simülasyonudur. Sistem; güvenli kimlik doğrulama (JWT), ACID uyumlu finansal işlemler ve olay tabanlı (event-driven) bildirim mekanizmalarını içerir.

Proje, **Kubernetes (Minikube)** üzerinde çalışacak şekilde tasarlanmış olup, **GitHub Actions** ile kurulan Self-Hosted CI/CD hattı sayesinde kod değişiklikleri otomatik olarak analiz edilir, test edilir ve küme üzerine dağıtılır.

---

## ✨ Anahtar Özellikler

* **Mikroservis Mimarisi:** Sorumluluklarına göre ayrılmış servis yapısı (`Identity`, `Transaction`, `Notification`, `API Gateway`).
* **%100 Kod Kapsamı (Test Coverage):** `JUnit 5`, `Mockito` ve `Testcontainers` kullanılarak yazılan birim ve entegrasyon testleri ile yüksek güvenilirlik hedeflenmiştir.
* **Otomatize CI/CD:** GitHub Actions ve Self-Hosted Runner ile her push işleminde SonarQube analizi, build ve Kubernetes dağıtımı yapılır.
* **Güvenlik:** `Spring Security` ve `JWT` ile korunan uç noktalar, Redis tabanlı token karaliste (blacklist) yönetimi.
* **Gözlemlenebilirlik:** `Prometheus`, `Grafana` ve `Zipkin` ile dağıtık sistem izleme (Tracing) ve metrik takibi.
* **Veri Bütünlüğü:** Finansal işlemler için PostgreSQL üzerinde ACID prensiplerine uygun transaction yönetimi.

---

## 🚀 Teknoloji Haritası

| Kategori               | Teknoloji        | Sürüm / Detay                           |
|:-----------------------|:-----------------|:----------------------------------------|
| **Dil & Framework**    | Java 17          | Spring Boot 3.4.1, Spring Cloud Gateway |
| **Veritabanı**         | PostgreSQL       | Production DB                           |
| **Cache & NoSQL**      | Redis            | Token Blacklist & Caching               |
| **Test**               | JUnit 5, Mockito | Testcontainers, Embedded Redis          |
| **CI/CD & Kalite**     | GitHub Actions   | SonarQube, JaCoCo, Docker Hub           |
| **Orkestrasyon**       | Kubernetes       | Minikube (Local Cluster)                |
| **Gözlemlenebilirlik** | Grafana & Zipkin | Distributed Tracing & Monitoring        |

---

## 🏗️ Servis Mimarisi

Sistem aşağıdaki temel bileşenlerden oluşur:

1.  **API Gateway:** Tek giriş noktası. Authentication filter ile JWT doğrulaması yapar ve istekleri yönlendirir.
2.  **Identity Service:** Kullanıcı kaydı, giriş ve token (Access/Refresh) yönetimini sağlar.
3.  **Transaction Service:** Hesap oluşturma, bakiye sorgulama ve para transferi işlemlerini yönetir.
4.  **Notification Service:** Diğer servislerden gelen olayları dinler ve bildirim süreçlerini yönetir.

---

## ⚡ Hızlı Başlangıç: Sistemi Ayağa Kaldırma

Bu adımlar, projenin Kubernetes (Minikube) ortamında çalıştırılmasını kapsar.

**Ön Gereksinimler:** `Java 17`, `Docker`, `Minikube` ve `kubectl`.

**1. Minikube'ü Başlatın:**
```bash
minikube start

```

**2. Kubernetes Deployment:**
Servisleri, veritabanlarını ve konfigürasyonları kümeye uygulayın.
*(Not: CI/CD pipeline'ı bunu otomatik yapar, ancak manuel kurulum için aşağıdaki komutu kullanabilirsiniz)*

```bash
kubectl apply -f k8s/

```

**3. Pod Durumlarını Kontrol Edin:**
Tüm servislerin `Running` durumuna geçmesini bekleyin.

```bash
kubectl get pods -w

```

**4. Port Yönlendirme (Port-Forward):**
API Gateway ve İzleme araçlarına erişmek için tünel açın:

```bash
# API Gateway (Uygulama Erişimi)
kubectl port-forward svc/api-gateway 8080:8080

# Grafana (Opsiyonel - Monitoring)
kubectl port-forward svc/grafana 3000:3000

```

---

## 🧪 Uçtan Uca Test Senaryosu (cURL)

Aşağıdaki komutlarla sisteme kayıt olup para transferi gerçekleştirebilirsiniz.

**Adım 1: Kullanıcı Oluştur (Register)**

```bash
curl -X POST http://localhost:8080/auth/register \
-H "Content-Type: application/json" \
-d '{"username": "testuser", "password": "password123", "tckn": "10000000146", "firstName": "Test", "lastName": "User", "email": "test@example.com"}'

```

**Adım 2: Giriş Yap ve Token Al (Login)**

```bash
# Token'ı alıp bir değişkene atar (jq kurulu olmalıdır, yoksa manuel kopyalayınız)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
-H "Content-Type: application/json" \
-d '{"username": "testuser", "password": "password123"}' | jq -r .accessToken)

echo "Access Token: $TOKEN"

```

**Adım 3: Banka Hesabı Oluştur**

```bash
# Oluşan IBAN'ı alır
IBAN=$(curl -s -X POST http://localhost:8080/api/v1/accounts \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"currency": "TRY"}' | jq -r .data.iban)

echo "Oluşturulan IBAN: $IBAN"

```

**Adım 4: Hesaba Para Yatır (Deposit)**

```bash
curl -X POST http://localhost:8080/api/v1/accounts/$IBAN/deposits \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"amount": 1000.00}'

```

**Adım 5: Para Transferi Yap (Transaction)**

```bash
# Not: toIban olarak sistemde var olan başka bir IBAN kullanmalısınız.
curl -X POST http://localhost:8080/api/v1/transactions \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"fromIban": "'$IBAN'", "toIban": "TR9999999999999999999999", "amount": 150.00}'

```

---

## 📚 Dokümantasyon ve Rehberler

Proje hakkında daha derinlemesine bilgi için aşağıdaki rehberleri inceleyebilirsiniz:

* 📘 **[PROJECT_MASTER_GUIDE.md](PROJECT_MASTER_GUIDE.md)**:
* Detaylı mimari kararlar.
* **Self-Hosted Runner** ve CI/CD kurulum adımları.
* Karşılaşılan kritik hatalar ve çözüm süreçleri.


* 🧪 **[TESTING_GUIDE.md](TESTING_GUIDE.md)**:
* Adım adım manuel test süreçleri.
* Grafana ve Prometheus ile izleme panelleri.
* Sık karşılaşılan hatalar (Troubleshooting).
