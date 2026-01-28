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
```

---

## 🔄 CI/CD ve Self-Hosted Runner Kurulumu

Projenin CI/CD hattı, kod değişikliklerini yerel Minikube kümenize dağıtmak için **Self-Hosted Runner** kullanımını zorunlu kılar.

**⚠️ Önemli:** Runner, hassas bilgiler içerdiğinden **proje klasörünün dışında** kurulmalıdır.

1.  **Runner'ı Kurun:** GitHub reponuzda `Settings > Actions > Runners > New self-hosted runner` adımlarını izleyerek runner'ı bilgisayarınızda ayrı bir dizine kurun.
2.  **`KUBE_CONFIG` Secret'ını Ekleyin:** `cat ~/.kube/config` komutunun çıktısını kopyalayıp, reponun `Settings > Secrets > Actions` bölümünde `KUBE_CONFIG` adıyla yeni bir secret olarak ekleyin.
3.  **Deployment Öncesi:** Kodunuzu `git push` yapmadan önce Minikube'ün ve Self-Hosted Runner'ın (`./run.sh`) çalıştığından emin olun.

---

## 🧪 Uçtan Uca Test Senaryosu (cURL)

Sistem ayaktayken, aşağıdaki komutlarla temel bir kullanıcı akışını test edebilirsiniz.

**Adım 1: Kullanıcı Oluştur (Register)**
```bash
curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" -d '{"username": "testuser", "password": "password123", "tckn": "10000000146"}'
```

**Adım 2: Giriş Yap ve Token Al (Login)**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token -H "Content-Type: application/json" -d '{"username": "testuser", "password": "password123"}' | jq -r .accessToken)
```

**Adım 3: Para Transferi Yap**
```bash
# Bu adımdan önce bir hesap oluşturup para yatırmanız gerekir.
# Detaylar için TESTING_GUIDE.md'ye bakın.
curl -X POST http://localhost:8080/api/v1/transactions -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"fromIban": "...", "toIban": "...", "amount": 150.00}'
```

---

## 📚 Dokümantasyon ve Rehberler

Proje hakkında daha derinlemesine bilgi için aşağıdaki rehberleri inceleyebilirsiniz:

*   📘 **[PROJECT_MASTER_GUIDE.md](PROJECT_MASTER_GUIDE.md)**: Detaylı mimari, CI/CD kurulumu ve karşılaşılan sorunların çözümleri.
*   🧪 **[TESTING_GUIDE.md](TESTING_GUIDE.md)**: Adım adım manuel test senaryoları ve izleme panelleri hakkında bilgiler.
