APP := signer
PROJECT_DIR := $(abspath ..)
JAR := $(PROJECT_DIR)/target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar
JAVA_HOME ?= /usr/lib/jvm/java-17-openjdk-amd64
JAVA_CONFIG_DIR ?= /etc/java-17-openjdk
ARCH_LIBDIR ?= /lib/x86_64-linux-gnu
SGX_SIGNER_KEY ?=

.PHONY: all manifest sign run clean check check-signer-key

all: sign

check:
	@test -f "$(JAR)" || (echo "JAR bulunamadı: önce 'mvn clean package' çalıştırın"; exit 1)
	@command -v gramine-manifest >/dev/null
	@command -v gramine-sgx-sign >/dev/null
	@command -v gramine-sgx >/dev/null

manifest: check $(APP).manifest.template
	gramine-manifest \
		-Djava_home="$(JAVA_HOME)" \
		-Djava_config_dir="$(JAVA_CONFIG_DIR)" \
		-Darch_libdir="$(ARCH_LIBDIR)" \
		-Dproject_dir="$(PROJECT_DIR)" \
		-Djar_path="$(JAR)" \
		$(APP).manifest.template > $(APP).manifest

check-signer-key:
	@test -n "$(SGX_SIGNER_KEY)" || (echo "SGX_SIGNER_KEY repo dışındaki bir private key dosyasını göstermelidir"; exit 1)
	@test -f "$(SGX_SIGNER_KEY)" || (echo "SGX imzalama anahtarı bulunamadı: $(SGX_SIGNER_KEY)"; exit 1)

sign: manifest check-signer-key
	gramine-sgx-sign --manifest $(APP).manifest --output $(APP).manifest.sgx --key $(SGX_SIGNER_KEY)

run: sign
	gramine-sgx ./$(APP)

clean:
	rm -f $(APP).manifest $(APP).manifest.sgx $(APP).sig $(APP).token
