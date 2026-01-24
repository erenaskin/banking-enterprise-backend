# Banking Backend - Proje Master Dokümantasyonu

Bu doküman, Banking Backend projesinin Kubernetes (Minikube) üzerindeki kurulumunu, mimarisini, karşılaşılan sorunların çözümlerini ve operasyonel komutları içerir.

---

## 1. Proje Mimarisi ve Bileşenler

Proje, mikroservis mimarisi üzerine kurulmuş olup, aşağıdaki bileşenlerden oluşmaktadır:

### A. Mikroservisler (Spring Boot 3.4.1)
Tüm servisler Kubernetes içinde **Port 80** üzerinden birbirleriyle iletişim kuracak şekilde yapılandırılmıştır.

1.  **API Gateway (`api-gateway`)**
    *   **Görevi:** Dış dünyadan gelen tüm istekleri karşılar ve ilgili servise yönlendirir.
    *   **Port:** Dışarıya 8080 (Port-forward ile), Cluster içine 80.
    *   **Teknoloji:** Spring Cloud Gateway.

2.  **Identity Service (`identity-service`)**
    *   **Görevi:** Kullanıcı kaydı, giriş işlemleri ve JWT token doğrulama.
    *   **Bağımlılıklar:** PostgreSQL (Kullanıcı verisi), Redis (Refresh token cache).
    *   **Güvenlik:** Spring Security ile korunur.

3.  **Transaction Service (`transaction-service`)**
    *   **Görevi:** Para transferi işlemlerini yönetir.
    *   **İşleyiş:** Bir işlem gerçekleştiğinde Kafka'ya (`transaction-events`) bir olay (event) fırlatır.
    *   **Bağımlılıklar:** PostgreSQL, Kafka.

4.  **Notification Service (`notification-service`)**
    *   **Görevi:** İşlem olaylarını dinler ve bildirim gönderir (Simülasyon).
    *   **İşleyiş:** Kafka'daki `transaction-events` konusunu dinler (Consumer).
    *   **Bağımlılıklar:** Kafka.

### B. Altyapı (Infrastructure)
1.  **PostgreSQL:** İlişkisel veritabanı.
2.  **Redis:** Token yönetimi (Cache).
3.  **Kafka & Zookeeper:** Asenkron mesajlaşma.
4.  **Minikube (Kubernetes):** Konteyner orkestrasyonu.

### C. İzlenebilirlik (Observability)
1.  **Zipkin:** Dağıtık izleme (Distributed Tracing).
2.  **Prometheus:** Metrik toplama.
3.  **Grafana:** Görselleştirme.

---

## 2. Karşılaşılan Kritik Sorunlar ve Çözümleri

Geliştirme sürecinde çözülen kritik hatalar:

### 1. Identity Service - 403 Forbidden (Readiness Probe)
*   **Çözüm:** `AuthConfig.java` dosyasında Actuator için ayrı ve öncelikli (`@Order(1)`) bir `SecurityFilterChain` tanımlandı.

### 2. Notification Service - Kafka Bağlantı Hatası
*   **Çözüm:** `notification-service.yaml` dosyasına `SPRING_KAFKA_BOOTSTRAP_SERVERS` ortam değişkeni eklenerek adres `kafka-service:9092` olarak güncellendi.

### 3. Kafka Port Çakışması
*   **Çözüm:** Servis adı `kafka` yerine `kafka-service` olarak değiştirildi.

### 4. Prometheus - Connection Refused
*   **Çözüm:** Tüm servislerin Kubernetes Service portları **80** olarak standartlaştırıldı.

---

## 3. Komut Sözlüğü (Cheat Sheet)

### Kubernetes (kubectl)
*   `kubectl apply -f k8s/`: Tüm konfigürasyonları uygular.
*   `kubectl get pods -w`: Pod'ları izler.
*   `kubectl logs <pod-adi>`: Logları görüntüler.
*   `kubectl port-forward svc/<servis-adi> <yerel-port>:<uzak-port>`: Servise erişim tüneli açar.

### Docker & Maven
*   `mvn clean install -DskipTests`: Projeyi derler (JAR oluşturur).
*   `docker build -t <imaj-adi>:0.0.1 .`: Docker imajı oluşturur.
*   `minikube image load <imaj-adi>:0.0.1`: İmajı Minikube'e yükler.

---

## 4. Test ve Erişim

Detaylı test senaryoları için **`TESTING_GUIDE.md`** dosyasına bakınız.

**Hızlı Erişim Portları (Port-Forward Gerekir):**
*   **API Gateway:** `localhost:8080`
*   **Prometheus:** `localhost:9090`
*   **Grafana:** `localhost:3000` (admin/admin)
*   **Zipkin:** `localhost:9411`

---

## 5. Mevcut Durum

**✅ Durum:** SİSTEM STABLE (KARARLI)

*   Mikroservisler Kubernetes üzerinde çalışıyor.
*   Kafka ile asenkron iletişim (Transaction -> Notification) sağlanıyor.
*   Prometheus ve Grafana ile metrik takibi yapılıyor.
*   Zipkin ile trace takibi aktif.

**Sıradaki Adımlar:**
(Yeni görevler bekleniyor...)
