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
| `keygen` + `signer --key-mode=file` | Repo dışındaki geçici dizin | Geliştirme/fonksiyon testi |
| `signer --key-mode=memory` | Yalnız JVM belleği | Native, SGX ile adil performans baz çizgisi |
| Gramine SGX signer | Yalnız production enclave belleği | SGX ölçümü |
| `verifier` | Private key yok | İmza doğrulama |

Uygulama SGX olmayan bir CPU'da da çalışır. `keygen`, `verifier`,
`benchmark-client`, dosya tabanlı signer ve `--key-mode=memory` native signer
Gramine/SGX gerektirmez. SGX zorunluluğu yalnız imzalı manifestte sabit olan
`--require-sgx=true` seçeneğiyle etkinleşir; native scriptler bu seçeneği
vermez.

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

`gramine` Ubuntu'nun varsayılan deposunda bulunmaz. Ubuntu 22.04 (`jammy`) için
resmî Gramine ve Intel SGX APT depolarını önce ekleyin:

```bash
sudo install -d -m 0755 /etc/apt/keyrings

sudo curl -fsSLo /etc/apt/keyrings/gramine-keyring-jammy.gpg \
  https://packages.gramineproject.io/gramine-keyring-jammy.gpg

echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/gramine-keyring-jammy.gpg] https://packages.gramineproject.io/ jammy main" \
  | sudo tee /etc/apt/sources.list.d/gramine.list

sudo curl -fsSLo /etc/apt/keyrings/intel-sgx-deb.asc \
  https://download.01.org/intel-sgx/sgx_repo/ubuntu/intel-sgx-deb.key

echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/intel-sgx-deb.asc] https://download.01.org/intel-sgx/sgx_repo/ubuntu jammy main" \
  | sudo tee /etc/apt/sources.list.d/intel-sgx.list

sudo apt update
sudo apt install gramine
```

Ubuntu 24.04 kullanılıyorsa yukarıdaki `jammy` değerlerini `noble` ile değiştirin.
Kurulumdan önce APT'nin paketi gördüğünü doğrulayabilirsiniz:

```bash
apt-cache policy gramine
```

`Candidate: (none)` görünüyorsa `sudo apt update` çıktısındaki Gramine/Intel
deposu hatasını çözmeden kuruluma devam etmeyin. Rastgele binary arşiv yerine
resmî APT deposu ve imzalı keyring kullanılması önerilir.

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

`mvn test` yalnız hızlı/unit testleri çalıştırır. RabbitMQ entegrasyon testleri
broker ayaktayken ayrı profille çalıştırılır:

```bash
docker compose up -d
mvn -Pintegration-tests verify
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
/tmp/rabbitmq-secp256k1-service-keys/test-key-001-private.pem
/tmp/rabbitmq-secp256k1-service-keys/test-key-001-public.pem
```

Private key PKCS#8 PEM ve POSIX sistemde `0600`, public key X.509
SubjectPublicKeyInfo PEM biçimindedir. Hiçbir anahtar repo içine üretilmez. Bu
mod production için kullanılmamalıdır. Farklı bir repo dışı dizin kullanmak için
hem keygen hem signer'a aynı değeri verin:

```bash
KEY_DIR=/gizli/gecici-test-dizini ./scripts/run-keygen.sh
KEY_DIR=/gizli/gecici-test-dizini ./scripts/run-signer.sh
```

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

Bu akışta birbirinden bağımsız iki private key vardır:

| Anahtar | Nerede üretilir/kullanılır? | Enclave çalışırken durumu |
|---|---|---|
| Enclave build/kimlik RSA anahtarı | Gramine `sign` aşamasında, repo dışında | Enclave'e verilmez, mount edilmez, runtime için gerekmez |
| Uygulama secp256k1 anahtarı | Production enclave başladıktan sonra enclave belleğinde | Private kısmı dışarı verilmez; yalnız public key ve imza yanıtta çıkar |

`SGX_SIGNER_KEY` yalnız build-time Gramine kimlik anahtarıdır. Uygulamanın
imzaladığı mesajlara erişemez ve `run` komutuna aktarılmaz.

Önce JAR'ı ve RabbitMQ'yu hazırlayın:

```bash
mvn clean package
docker compose up -d
```

Manifest üretme ve enclave imzalama:

```bash
install -d -m 0700 /tmp/gramine-build-identity
gramine-sgx-gen-private-key /tmp/gramine-build-identity/signer-key.pem
make -C gramine sign \
  SGX_SIGNER_KEY=/tmp/gramine-build-identity/signer-key.pem
```

Bu RSA-3072 anahtar secp256k1 işlem anahtarı değildir; yalnızca enclave
kimliğini/MRSIGNER'ı imzalar. Makefile anahtar üretmez ve repo içindeki bir
varsayılan anahtar kullanmaz; `SGX_SIGNER_KEY` açıkça verilmelidir. `/tmp`
örneği yalnız test içindir ve yeniden başlatmada kaybolabilir. Kurum anahtarını
repo dışındaki korumalı kalıcı konumdan kullanmak için:

```bash
make -C gramine sign SGX_SIGNER_KEY=/gizli/yol/enclave-key.pem
```

Varsayılan JDK yolları sisteminizde farklıysa:

```bash
make -C gramine sign \
  SGX_SIGNER_KEY=/gizli/yol/enclave-key.pem \
  JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  JAVA_CONFIG_DIR=/etc/java-17-openjdk \
  ARCH_LIBDIR=/lib/x86_64-linux-gnu
```

İmzalanmış enclave bilgisini kontrol edin. Çıktıda `debug_enclave = false`
olmalıdır:

```bash
gramine-sgx-sigstruct-view --verbose --output-format=toml gramine/signer.sig
```

Signer'ı build anahtarını vermeden çalıştırın:

```bash
./scripts/run-signer-sgx.sh
```

Başlangıçta uygulama production DCAP quote kontrolünden geçtikten sonra
`enclave-key` kimlikli secp256k1 anahtarını üretir. `keys/` altında private key
oluşmaz. RabbitMQ cevabında yalnız public key ve üretilen imza bulunur; private
key/private-key yolu için model veya API alanı yoktur. Debug manifest,
`gramine-direct`, quote alınamaması veya desteklenmeyen quote formatında servis
anahtar üretmeden hata verir.

## SGX destekli CPU üzerinde uçtan uca test

Aşağıdaki adımlar gerçek SGX enclave'i, production/debug kontrolünü ve native ↔
SGX performans karşılaştırmasını doğrular. Önce BIOS'ta SGX'in etkin olduğunu,
DCAP/AESM servislerinin hazır olduğunu kontrol edin:

```bash
is-sgx-available
test -e /dev/sgx_enclave
test -e /dev/sgx_provision
systemctl is-active aesmd
```

`is-sgx-available` çıktısında en az `SGX supported by CPU: true` görülmelidir.
Driver veya AESM/DCAP hatası varsa enclave testine geçmeden host kurulumunu
düzeltin. Ardından:

```bash
# 1. Birim ve gerçek RabbitMQ entegrasyon testleri
mvn clean test
docker compose up -d
mvn -Pintegration-tests verify

# 2. Fat JAR ve repo dışında test amaçlı enclave imzalama anahtarı
mvn clean package
install -d -m 0700 /tmp/gramine-build-identity
gramine-sgx-gen-private-key /tmp/gramine-build-identity/signer-key.pem
make -C gramine sign \
  SGX_SIGNER_KEY=/tmp/gramine-build-identity/signer-key.pem

# 3. İmzalanan enclave'in production olduğunu doğrula
gramine-sgx-sigstruct-view --verbose --output-format=toml gramine/signer.sig
```

Son komutun çıktısında `debug_enclave = false` olmalıdır. Birinci terminalde
production enclave signer'ı başlatın:

```bash
./scripts/run-signer-sgx.sh
```

Bu aşamada process environment veya argümanlarda `SGX_SIGNER_KEY` bulunmaz;
build kimlik anahtarı enclave'e açılan dosyalar arasında değildir. Logda
`memory-only:enclave-key` görülmeli; SGX/DCAP veya debug denetimi başarısız
olursa süreç secp256k1 anahtarı üretmeden çıkmalıdır. İkinci terminalde kısa
işlev ve performans testi çalıştırın:

```bash
KEY_ID=enclave-key MESSAGE_COUNT=1000 ./scripts/run-benchmark-sign.sh
```

Ardından SGX signer'ı durdurun, aynı broker ve parametrelerle native bellek
modunu ölçün:

```bash
KEY_ID=enclave-key ./scripts/run-signer-memory.sh
# başka terminal:
KEY_ID=enclave-key MESSAGE_COUNT=1000 ./scripts/run-benchmark-sign.sh
```

Gerçek ölçümde kısa koşuyu ısınma olarak kullanıp en az üç uzun native ve üç
uzun SGX koşusunun medyanlarını karşılaştırın. Repo içinde PEM/key oluşmadığını
son olarak kontrol edebilirsiniz:

```bash
find . -path ./.git -prune -o -type f \
  \( -name '*.pem' -o -name '*.key' \) -print
git status --short
```

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
rm -rf /tmp/rabbitmq-secp256k1-service-keys /tmp/gramine-build-identity
```

`make clean` yalnız üretilmiş Gramine manifest/imza çıktılarını siler. Repo
dışındaki native test ve enclave imzalama anahtarlarını kendi güvenli yaşam
döngüsü politikanıza göre ayrıca kaldırın.

## İlgili güvenlik dokümanları

- [Gramine manifest sözdizimi](https://gramine.readthedocs.io/en/latest/manifest-syntax.html)
- [Gramine attestation ve secret provisioning](https://gramine.readthedocs.io/en/latest/attestation.html)
- [Gramine SGX signing key hazırlığı](https://gramine.readthedocs.io/en/latest/sgx-setup.html#prepare-a-signing-key)
- [Intel: SGX debug ve production enclave farkı](https://www.intel.com/content/dam/develop/external/us/en/documents/intel-sgx-build-configuration-737361.pdf)
