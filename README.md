# 🏦 Banking Backend Microservices

Bu proje, modern mikroservis mimarisi prensipleriyle geliştirilmiş, ölçeklenebilir, olay güdümlü (event-driven) bir bankacılık simülasyonudur.

Kullanıcı yönetimi, hesap işlemleri, para transferleri ve bildirim sistemlerini içerir. Altyapı olarak **Kubernetes**, **Kafka**, **Redis** ve **PostgreSQL** kullanır.

Proje, **GitHub Actions** ile tam otomatik CI/CD hattına sahiptir ve kod kalitesi **SonarQube** ile denetlenmektedir.

---

## 🚀 Teknolojiler ve Mimari

Proje **Spring Boot 3.4.1** ve **Java 17+** kullanılarak geliştirilmiştir.

| Bileşen | Teknoloji | Açıklama |
| :--- | :--- | :--- |
| **API Gateway** | Spring Cloud Gateway | Tek giriş noktası, yönlendirme ve güvenlik. |
| **Identity Service** | Spring Security, JWT | Kimlik doğrulama, Token yönetimi (Redis). |
| **Transaction Service** | Spring Data JPA | Hesap yönetimi, Para transferi (Outbox Pattern). |
| **Notification Service** | Apache Kafka | Asenkron bildirim gönderimi (Consumer). |
| **CI/CD** | GitHub Actions | Otomatik Build, Test ve Deploy. |
| **Code Quality** | SonarQube | Statik kod analizi ve güvenlik taraması. |
| **Orchestration** | Kubernetes (Minikube) | Konteyner yönetimi. |

---

## 📂 Proje Yapısı

```bash
banking-backend/
├── .github/workflows/    # CI/CD Pipeline tanımları (YAML)
├── api-gateway/          # İstek karşılama ve yönlendirme
├── identity-service/     # Auth (Register, Login, Token)
├── transaction-service/  # Hesap ve Transfer işlemleri
├── notification-service/ # Bildirim (Kafka Consumer)
├── common/               # Ortak DTO, Exception ve Utils
├── k8s/                  # Kubernetes Deployment & Service dosyaları
├── PROJECT_MASTER_GUIDE.md # Detaylı Mimari ve Operasyon Rehberi
└── TESTING_GUIDE.md      # Uçtan Uca Test Senaryoları
```

---

## 🛠 Kurulum ve Çalıştırma (Kubernetes)

Proje, Kubernetes (Minikube) üzerinde çalışacak şekilde yapılandırılmıştır.

### 1. Ön Gereksinimler
*   Docker Desktop
*   Minikube
*   kubectl
*   Java 17+ & Maven

### 2. Başlatma
Tüm altyapıyı ve servisleri ayağa kaldırmak için:

```bash
# 1. Minikube'ü başlatın
minikube start

# 2. Kubernetes konfigürasyonlarını uygulayın
kubectl apply -f k8s/

# 3. Pod'ların durumunu izleyin
kubectl get pods -w
```

### 3. Erişim (Port-Forward)
Servislere yerel makinenizden erişmek için tünel açmanız gerekir:

```bash
# API Gateway (Uygulama)
kubectl port-forward svc/api-gateway 8080:8080

# Grafana (İzleme)
kubectl port-forward svc/grafana 3000:3000
```

---

## 🧪 Test ve Kullanım

Sistemi uçtan uca test etmek (Kayıt olma, Para yatırma, Transfer vb.) için detaylı rehberimizi inceleyin:

👉 **[TESTING_GUIDE.md](TESTING_GUIDE.md)**

---

## 📊 İzleme (Monitoring)

Sistem ayaktayken aşağıdaki araçlarla sağlık durumunu izleyebilirsiniz:

*   **Grafana:** `http://localhost:3000` (Kullanıcı: `admin`, Şifre: `admin`)
*   **Zipkin:** `http://localhost:9411`
*   **Prometheus:** `http://localhost:9090`
*   **SonarCloud:** Kod kalitesi raporları için SonarCloud panelini ziyaret edin.

---

## 📝 Lisans

Bu proje eğitim ve portfolyo amaçlı geliştirilmiştir.
