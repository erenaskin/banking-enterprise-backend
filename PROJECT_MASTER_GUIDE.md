# Banking Enterprise Backend - Master Documentation

Bu doküman, **Banking Enterprise Backend** projesinin mimarisini, kurulumunu, test süreçlerini ve CI/CD boru hattını (Pipeline) detaylı bir şekilde açıklar. Proje geliştirme sürecinde yapılan iyileştirmeler, karşılaşılan hatalar ve çözümleri de bu dokümanda kayıt altına alınmıştır.

---

## 1. Proje Mimarisi

Proje, ölçeklenebilir ve modüler bir yapı sağlamak için **Mikroservis Mimarisi** kullanılarak geliştirilmiştir.

### Servisler ve Sorumlulukları

1.  **API Gateway (`api-gateway`)**:
    *   **Tek Giriş Noktası:** Tüm dış istekler buraya gelir.
    *   **Authentication Filter:** Gelen isteklerin `Authorization` header'ını kontrol eder, JWT doğrulamasını yapar ve `X-User-Id`, `X-Correlation-ID` gibi headerları downstream servislere ekler.
    *   **Routing:** İstekleri ilgili mikroservise yönlendirir.

2.  **Identity Service (`identity-service`)**:
    *   **Kimlik Doğrulama:** Kullanıcı kaydı (`/register`), giriş (`/token`), token yenileme (`/refreshToken`) ve çıkış (`/logout`) işlemlerini yönetir.
    *   **Güvenlik:** Spring Security ve JWT kullanır.
    *   **Veritabanı:** Kullanıcı bilgileri için PostgreSQL kullanır.

3.  **Transaction Service (`transaction-service`)**:
    *   **Hesap Yönetimi:** Hesap oluşturma, bakiye sorgulama.
    *   **Para Transferi:** Hesaplar arası para transferi işlemlerini yönetir (ACID prensiplerine uygun).
    *   **Veritabanı:** İşlem ve hesap kayıtları için PostgreSQL kullanır.

4.  **Notification Service (`notification-service`)**:
    *   **Bildirimler:** Diğer servislerden gelen olayları (event) dinler ve kullanıcılara bildirim gönderir (şu an loglama seviyesinde simüle edilmiştir).

5.  **Common (`common`)**:
    *   Tüm servisler tarafından paylaşılan DTO'lar, Exception sınıfları ve Yardımcı araçları (örn. `TCKNValidator`) içerir.

---

## 2. Teknoloji Yığını

*   **Dil:** Java 17
*   **Framework:** Spring Boot 3.4.1, Spring Cloud (Gateway)
*   **Veritabanı:** PostgreSQL (Production), H2 (Test)
*   **Cache:** Redis (Token Blacklist ve Caching için)
*   **Test:** JUnit 5, Mockito, Testcontainers, Embedded Redis
*   **Analiz:** SonarQube (Kod kalitesi ve kapsamı)
*   **Konteynerizasyon:** Docker
*   **Orkestrasyon:** Kubernetes (Minikube)
*   **CI/CD:** GitHub Actions (Self-Hosted Runner)

---

## 3. Kurulum ve Çalıştırma (Lokal Geliştirme)

### Ön Gereksinimler
*   Java 17 JDK
*   Maven 3.8+
*   Docker & Docker Compose
*   Minikube (Kubernetes için)

### Adımlar
1.  **Bağımlılıkları Derle:**
    ```bash
    mvn clean install -DskipTests
    ```
2.  **Altyapıyı Kaldır (Docker Compose):**
    Veritabanı ve Redis servislerini ayağa kaldırmak için:
    ```bash
    docker-compose up -d
    ```
3.  **Servisleri Başlat:**
    Her servisi IDE üzerinden veya terminalden `mvn spring-boot:run` ile başlatabilirsiniz.

---

## 4. Test Stratejisi ve Kod Kalitesi

Projede **%100 Kod Kapsamı (Code Coverage)** hedeflenmiştir.

### Test Araçları
*   **Birim Testleri:** İş mantığını izole test etmek için Mockito kullanılır.
*   **Entegrasyon Testleri:** `@SpringBootTest` ve `Testcontainers` kullanılarak gerçek veritabanı senaryoları test edilir.
*   **Embedded Redis:** Test ortamında Redis bağımlılığını simüle etmek için kullanılır.

### SonarQube Entegrasyonu
Proje, her push işleminde SonarQube üzerinde analiz edilir.
*   **Coverage Raporu:** JaCoCo plugin'i ile üretilir.
*   **Yapılandırma:** `pom.xml` dosyasında rapor yolları dinamik hale getirilmiştir:
    ```xml
    <sonar.coverage.jacoco.xmlReportPaths>${project.build.directory}/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
    ```

---

## 5. CI/CD Pipeline ve Kubernetes Deployment

Proje, GitHub Actions kullanılarak otomatik olarak test edilir, derlenir, Docker imajı oluşturulur ve Kubernetes ortamına dağıtılır.

### ⚠️ Kritik: Self-Hosted Runner ve Minikube Yapılandırması

GitHub'ın sunduğu standart runner'lar (ubuntu-latest), sizin yerel bilgisayarınızda çalışan **Minikube** kümesine erişemez. Bu nedenle deployment adımının çalışması için **Self-Hosted Runner** kullanılması zorunludur. Bu runner, sizin kendi bilgisayarınızda çalışarak GitHub Actions ile yerel Minikube'ünüz arasında bir köprü kurar.

#### Kurulum ve Çalıştırma Adımları:

1.  **Runner Kurulumu:**
    *   GitHub reponuzda **Settings > Actions > Runners > New self-hosted runner** adımlarını takip edin.
    *   İşletim sisteminize uygun komutları izleyerek runner'ı bilgisayarınızda proje dizini **dışında** bir klasöre (örneğin `~/actions-runner`) indirin ve yapılandırın.
    *   **ÖNEMLİ:** Bu `actions-runner` klasörü, hassas bilgiler içerdiğinden **asla** Git reponuza eklenmemelidir.

2.  **Kubeconfig Ayarı (Secret):**
    Runner'ın Minikube'e erişebilmesi için `kubeconfig` dosyanızın içeriği GitHub Secret olarak eklenmelidir.
    *   Terminalde: `cat ~/.kube/config` komutunu çalıştırın.
    *   Çıktıyı kopyalayın.
    *   GitHub -> Settings -> Secrets -> Actions -> New Repository Secret.
    *   İsim: `KUBE_CONFIG`, Değer: Kopyalanan içerik.

3.  **Deployment Öncesi Hazırlık:**
    Deployment işlemini başlatmadan önce (`git push` yapmadan önce) aşağıdaki adımları **kesinlikle** uygulayın:

    *   **Adım 1: Minikube'ü Başlatın**
        ```bash
        minikube start
        ```
    *   **Adım 2: Runner'ı Başlatın**
        Runner'ı kurduğunuz dizine gidin (örneğin `cd ~/actions-runner`) ve script'i çalıştırın:
        ```bash
        ./run.sh
        ```
    *   **Adım 3: "Listening for Jobs" Yazısını Bekleyin**
        Terminalde bu yazıyı gördüğünüzde runner, GitHub'dan gelecek işleri dinlemeye hazırdır.

4.  **Deployment'ı Tetikleme:**
    Kodunuzu pushladığınızda (`git push`), GitHub Actions işi sizin yerel runner'ınıza gönderecek ve deployment yerel Minikube kümenize yapılacaktır.

---

## 6. Sonuç

Bu proje, modern DevOps pratikleri ve mikroservis mimarisi kullanılarak, yüksek kod kalitesi ve test kapsamı ile geliştirilmiştir. Lokal Kubernetes ortamına (Minikube) otomatik dağıtım yapabilen, kendi kendine yeten bir CI/CD hattına sahiptir.
