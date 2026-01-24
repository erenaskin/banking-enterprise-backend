# 🚀 Sıradaki Adımlar ve Görev Listesi

Bu dosya, projenin mevcut durumundan bir sonraki aşamaya geçmek için yapılması gerekenleri listeler.

---

## ✅ Tamamlananlar (Bugün)
*   [x] **Dockerizasyon:** Tüm servisler için optimize edilmiş `Dockerfile` oluşturuldu.
*   [x] **CI/CD Pipeline:** GitHub Actions ile otomatik build, test ve deploy süreci kuruldu.
*   [x] **Code Quality:** SonarQube entegrasyonu sağlandı ve Quality Gate kuralları devreye alındı.
*   [x] **Security Fix:** `AuthConfig` sınıfında güvenlik iyileştirmeleri yapıldı.
*   [x] **Kubernetes Deploy:** Pipeline üzerinden otomatik Kubernetes dağıtımı eklendi.

---

## 📋 Yapılacaklar Listesi (Yarın)

### 1. Test Kapsamını Artırma (Kritik)
SonarQube Quality Gate'in geçmesi ve kod güvenilirliği için test yazılmalıdır.
*   **Görev:** `identity-service` ve `transaction-service` için Unit Testler yazın.
*   **Hedef:** Test Coverage oranını en az %80'e çıkarmak.
*   **Araçlar:** JUnit 5, Mockito.

### 2. Kubernetes Secret Yönetimi
Şu an veritabanı şifreleri ve hassas bilgiler YAML dosyalarında veya environment variable'larda açık duruyor olabilir.
*   **Görev:** Hassas verileri (DB şifresi, JWT secret vb.) Kubernetes `Secret` objelerine taşıyın.
*   **Dosya:** `k8s/secrets.yaml` (Bu dosyayı git'e atmayın veya şifreli tutun).

### 3. Log Yönetimi (Centralized Logging)
Pod loglarını tek tek `kubectl logs` ile izlemek zordur.
*   **Görev:** ELK Stack (Elasticsearch, Logstash, Kibana) veya EFK (Fluentd) kurulumunu araştırın.
*   **Alternatif:** Basit başlangıç için Loki + Grafana entegrasyonu yapılabilir.

### 4. Ingress Controller Kurulumu
Şu an servislere erişmek için `kubectl port-forward` kullanıyoruz. Bu prodüksiyon için uygun değildir.
*   **Görev:** Minikube üzerinde Nginx Ingress Controller'ı aktif edin.
*   **Hedef:** `http://banking.local/api/...` gibi domain tabanlı erişim sağlamak.

### 5. Yük Testi (Load Testing)
Sistemin ne kadar yük kaldırabileceğini görmek için.
*   **Görev:** JMeter veya k6 kullanarak sisteme eş zamanlı 1000+ istek gönderin.
*   **İzleme:** Grafana üzerinden CPU/RAM ve Response Time değişimlerini gözlemleyin.

---

## 💡 İpucu
Yarına başlarken ilk iş olarak **Unit Test** yazmaya odaklanın. Bu, hem SonarQube hatasını kalıcı olarak çözecek hem de kodunuzun sağlamlığını artıracaktır.
