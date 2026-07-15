# RabbitMQ secp256k1 İmzalama Servisi — Native ve Gramine SGX

Bu proje Java 17, Bouncy Castle ve RabbitMQ kullanarak `secp256k1` üzerinde
`SHA256withECDSA` imzası üretir. Aynı iş yükünü native Linux ve Gramine SGX
üzerinde çalıştırıp throughput/latency farkını ölçmek için hazırlanmıştır.

İmza DER, payload ve imza taşıma formatı Base64, public key formatı ise X.509
SubjectPublicKeyInfo'dur.

## Kısa güvenlik cevabı

Production enclave (`sgx.debug = false`) çalışırken enclave belleği normal host
processlerinden, kernelden ve SGX-aware debugger'dan korunur. Debug enclave'de
ise SGX-aware debugger enclave belleğini okuyabildiği için private key gizli
kabul edilemez.

Bu projedeki SGX profili şu önlemleri birlikte uygular:

- secp256k1 private key servis başladıktan sonra enclave/JVM belleğinde üretilir;
- private key diske, loga, RabbitMQ mesajına veya uygulama API'sine verilmez;
- manifest `sgx.debug = false` ve `sgx.remote_attestation = "dcap"` kullanır;
- uygulama anahtarı üretmeden önce DCAP quote'u okur ve SGX `DEBUG` bitini
  kontrol eder; debug/SGX dışı/bozuk quote durumunda fail-closed kapanır;
- manifest argümanları sabittir; host `--key-mode=file` gibi bir argüman enjekte
  edemez;
- JAR, JDK ve native kütüphaneler `sgx.trusted_files` ile ölçüme dahil edilir.

Dolayısıyla bu profil değiştirilmeden çalıştırıldığında private key uygulamanın
normal çıkış yollarından enclave dışına çıkarılmaz. Yine de SGX "hiçbir koşulda
sızamaz" garantisi değildir: uygulama/kriptografi açığı, side-channel, eski
mikrokod/TCB veya yanlış attestation politikası risktir. Uzak istemci ayrıca
quote'u doğrulayıp beklenen `MRENCLAVE`/`MRSIGNER`, production/debug özelliği ve
TCB durumunu kontrol etmelidir. Bu repo DCAP quote üretimini etkinleştirir fakat
uzak attestation verifier/provisioning protokolü sağlamaz.

RabbitMQ bağlantısı şu anda TLS kullanmaz. Host payload'ları ve RabbitMQ
kimlik bilgilerini görebilir/değiştirebilir ve servisi bir signing oracle gibi
kullanabilir; private key'in enclave içinde kalması ağ trafiğini korumaz.

## Modlar

| Mod | Private key | Kullanım |
|---|---|---|
| `keygen` + `signer --key-mode=file` | `keys/*.pem` | Geliştirme/fonksiyon testi |
| `signer --key-mode=memory` | Yalnız JVM belleği | Native, SGX ile adil performans baz çizgisi |
| Gramine SGX signer | Yalnız production enclave belleği | SGX ölçümü |
| `verifier` | Private key yok | İmza doğrulama |

Bellek-içi anahtar servis her yeniden başladığında değişir. Kalıcı enclave
anahtarı gerekiyorsa attestation sonrası secret provisioning veya ayrı bir
anahtarla Gramine encrypted files/sealing tasarlanmalıdır; private key'i normal
host dosyasına yazmak çözüm değildir.

## Gereksinimler

Native çalışma için:

- Linux
- JDK 17
- Maven 3.8+
- Docker Engine ve Docker Compose eklentisi (veya erişilebilir RabbitMQ)

SGX çalışma için bunlara ek olarak:

- BIOS'ta etkin Intel SGX ve uygun Intel CPU
- Linux SGX driver (kernel 5.11+)
- Gramine ve Gramine SGX desteği
- DCAP quote üretimi için Intel DCAP bileşenleri/AESM

Kurulum ayrıntıları sürüme ve dağıtıma göre değiştiği için güncel resmi
[Gramine kurulum](https://gramine.readthedocs.io/en/latest/installation.html) ve
[SGX host hazırlığı](https://gramine.readthedocs.io/en/latest/sgx-setup.html)
dokümanlarını kullanın. Host'u kontrol edin:

Ubuntu 22.04/24.04 üzerinde temel araçlar dağıtımın APT deposundan kurulabilir:

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven docker.io docker-compose-v2 make curl ca-certificates
sudo usermod -aG docker "$USER"
```

Grup değişikliğinin geçerli olması için oturumu kapatıp açın. `docker` grubunun
root eşdeğeri yetkiler verdiğini göz önünde bulundurun; isterseniz Docker
komutlarını `sudo` ile çalıştırın.

Gramine standart Ubuntu deposunda güncel olmayabilir. Yukarıdaki resmi Gramine
kurulum sayfasındaki Gramine ve Intel SGX APT depolarını bir kez tanımladıktan
sonra paket yöneticisiyle kurulum komutu şudur:

```bash
sudo apt update
sudo apt install gramine
```

Rastgele internet arşivi indirmek yerine dağıtım/Gramine APT deposu ve imzalı
keyring kullanılması önerilir.

Kurulum doğrulaması:

```bash
is-sgx-available
java -version
mvn -version
docker compose version
```

## Derleme ve test

```bash
mvn clean test
mvn clean package
```

Oluşan fat JAR:

```text
target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar
```

RabbitMQ'yu başlatın:

```bash
docker compose up -d
docker compose ps
```

Varsayılan bağlantı `localhost:5672`, kullanıcı `sgxuser`, parola `sgxpass`;
yönetim arayüzü `http://localhost:15672` adresindedir.

## Native fonksiyon testi

### 1. Dosyaya anahtar üretme

```bash
./scripts/run-keygen.sh
```

Bu komut geliştirme amacıyla aşağıdaki dosyaları oluşturur:

```text
keys/test-key-001-private.pem   # PKCS#8 PEM, POSIX sistemde 0600
keys/test-key-001-public.pem    # X.509 SubjectPublicKeyInfo PEM
```

Private key dosyaları `.gitignore` kapsamındadır. Bu mod production için
kullanılmamalıdır.

### 2. Dosya tabanlı signer ve benchmark

Bir terminalde:

```bash
./scripts/run-signer.sh
```

Başka terminalde:

```bash
./scripts/run-benchmark-sign.sh
```

### 3. SGX ile adil native baz çizgisi

SGX profili private key'i bellekte tuttuğu için karşılaştırmada native signer da
bellek modunda çalıştırılmalıdır. Böylece dosya I/O farkı SGX overhead'i gibi
ölçülmez.

Bir terminalde:

```bash
KEY_ID=enclave-key ./scripts/run-signer-memory.sh
```

Başka terminalde:

```bash
KEY_ID=enclave-key ./scripts/run-benchmark-sign.sh
```

## Gramine SGX derleme ve çalıştırma

Önce JAR'ı ve RabbitMQ'yu hazırlayın:

```bash
mvn clean package
docker compose up -d
```

Manifest üretme ve enclave imzalama:

```bash
make -C gramine sign
```

İlk çalıştırmada `gramine/signer-key.pem` adlı RSA-3072 enclave imzalama
anahtarı oluşturulur. Bu anahtar secp256k1 işlem anahtarı değildir; yalnızca
enclave kimliğini/MRSIGNER'ı imzalar. Dosya Git tarafından yok sayılır ve gizli
tutulmalıdır. Kurum anahtarını kullanmak için:

```bash
make -C gramine sign SGX_SIGNER_KEY=/gizli/yol/enclave-key.pem
```

Varsayılan JDK yolları sisteminizde farklıysa:

```bash
make -C gramine sign \
  JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  JAVA_CONFIG_DIR=/etc/java-17-openjdk \
  ARCH_LIBDIR=/lib/x86_64-linux-gnu
```

İmzalanmış enclave bilgisini kontrol edin. Çıktıda `debug_enclave = false`
olmalıdır:

```bash
gramine-sgx-sigstruct-view --verbose --output-format=toml gramine/signer.sig
```

Signer'ı çalıştırın:

```bash
./scripts/run-signer-sgx.sh
```

Başlangıçta uygulama production DCAP quote kontrolünden geçtikten sonra
`enclave-key` kimlikli secp256k1 anahtarını üretir. `keys/` altında private key
oluşmaz. Debug manifest, `gramine-direct`, quote alınamaması veya desteklenmeyen
quote formatında servis anahtar üretmeden hata verir.

## Native ve SGX performans karşılaştırması

Benchmark istemcisi enclave dışında/native çalışır; iki deneyde de aynı broker,
payload, mesaj sayısı ve `key-id` kullanılmalıdır. Her ölçümden önce signer'ı
yeniden başlatın ve makinedeki başka yükleri azaltın.

Native bellek modu:

```bash
KEY_ID=enclave-key ./scripts/run-signer-memory.sh
# başka terminal:
KEY_ID=enclave-key ./scripts/run-benchmark-sign.sh | tee benchmark-native.txt
```

SGX modu:

```bash
./scripts/run-signer-sgx.sh
# başka terminal:
KEY_ID=enclave-key ./scripts/run-benchmark-sign.sh | tee benchmark-sgx.txt
```

Script varsayılan olarak 10.000 istek ve 32-byte payload gönderir. Farklı test:

```bash
java -jar target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar \
  --mode=benchmark-client \
  --operation=sign \
  --message-count=50000 \
  --payload-size=1024 \
  --key-id=enclave-key
```

Benchmark gerçek request/response gidiş-dönüş latency'sini `System.nanoTime()`
ile ölçer, hata yanıtlarını başarı saymaz, P95/P99 ve throughput raporlar. Eski
yanıtların ölçüme karışmaması için başlangıçta response queue temizlenir; aynı
queue üzerinde eşzamanlı başka benchmark çalıştırmayın.

Performans kaybını şu şekilde hesaplayın:

```text
throughput kaybı (%) = (native_req_s - sgx_req_s) / native_req_s * 100
latency artışı (%)   = (sgx_latency - native_latency) / native_latency * 100
```

JIT ısınmasının etkisini azaltmak için önce rapora almayacağınız kısa bir koşu,
ardından en az 3 gerçek koşu yapıp medyanı karşılaştırın. EPC paging oluşursa
sonuç ciddi biçimde bozulabilir; JVM heap'i manifestte `-Xmx512m`, enclave alanı
`1G` olarak sınırlandırılmıştır. Donanımınıza göre bu değerleri birlikte
ayarlayın.

Verify benchmark için önce verifier'ı çalıştırın; benchmark istemcisi geçerli
bir geçici imza/public key çifti üretir:

```bash
./scripts/run-verifier.sh
./scripts/run-benchmark-verify.sh
```

## Temizleme

```bash
make -C gramine clean
docker compose down
```

`make clean` enclave imzalama anahtarını silmez. Native test private key'lerini
gerekmiyorsa ayrıca güvenli yaşam döngüsü politikanıza göre kaldırın.

## İlgili güvenlik dokümanları

- [Gramine manifest sözdizimi](https://gramine.readthedocs.io/en/latest/manifest-syntax.html)
- [Gramine attestation ve secret provisioning](https://gramine.readthedocs.io/en/latest/attestation.html)
- [Gramine SGX signing key hazırlığı](https://gramine.readthedocs.io/en/latest/sgx-setup.html#prepare-a-signing-key)
- [Intel: SGX debug ve production enclave farkı](https://www.intel.com/content/dam/develop/external/us/en/documents/intel-sgx-build-configuration-737361.pdf)
