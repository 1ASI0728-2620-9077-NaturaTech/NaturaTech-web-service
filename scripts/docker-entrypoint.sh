#!/bin/sh
set -eu

# Provider CA is supplied at runtime, never baked into the image.
# Scope trust to Connector/J: keep the JVM public CAs for Gemini and Sepolia.
if [ -n "${DB_CA_CERT_PATH:-}" ]; then
  if [ ! -r "$DB_CA_CERT_PATH" ]; then
    echo 'Configured MySQL CA certificate is not readable.' >&2
    exit 1
  fi
  umask 077
  trust_dir=$(mktemp -d /tmp/plantsync-mysql.XXXXXX)
  keytool -importcert -noprompt -alias mysql-provider-ca \
    -file "$DB_CA_CERT_PATH" -keystore "$trust_dir/truststore.p12" \
    -storetype PKCS12 -storepass changeit >/dev/null
  # This store contains public certificates only; its password is not a credential.
  set -- \
    "--spring.datasource.hikari.data-source-properties.trustCertificateKeyStoreUrl=file:$trust_dir/truststore.p12" \
    '--spring.datasource.hikari.data-source-properties.trustCertificateKeyStoreType=PKCS12' \
    '--spring.datasource.hikari.data-source-properties.trustCertificateKeyStorePassword=changeit' \
    "$@"
fi
exec java -jar /app/app.jar "$@"
