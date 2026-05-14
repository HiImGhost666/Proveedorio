#!/bin/sh
set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
export VAULT_ADDR
MAX_RETRIES=15
RETRY_DELAY=3

log_info()  { echo "[vault-init] INFO:  $1"; }
log_error() { echo "[vault-init] ERROR: $1" >&2; }

wait_for_vault() {
  log_info "Esperando a que Vault este disponible en ${VAULT_ADDR}..."
  i=0
  while [ "$i" -lt "$MAX_RETRIES" ]; do
    if vault status >/dev/null 2>&1; then
      log_info "Vault disponible y desbloqueado."
      return 0
    fi
    i=$((i + 1))
    log_info "Intento ${i}/${MAX_RETRIES} - Vault no disponible aun. Reintentando en ${RETRY_DELAY}s..."
    sleep "$RETRY_DELAY"
  done
  log_error "Vault no estuvo disponible despues de ${MAX_RETRIES} intentos. Abortando."
  exit 1
}

authenticate() {
  if [ -n "${VAULT_TOKEN}" ]; then
    log_info "Usando VAULT_TOKEN proporcionado por entorno."
    return 0
  fi

  log_info "VAULT_TOKEN no definido. Intentando login con VAULT_DEV_ROOT_TOKEN_ID..."
  if [ -z "${VAULT_DEV_ROOT_TOKEN_ID}" ]; then
    log_error "No hay credenciales de Vault: define VAULT_TOKEN o VAULT_DEV_ROOT_TOKEN_ID."
    exit 1
  fi

  if ! vault login -no-print "${VAULT_DEV_ROOT_TOKEN_ID}"; then
    log_error "Fallo de autenticacion con Vault. Verifica VAULT_DEV_ROOT_TOKEN_ID."
    exit 1
  fi

  log_info "Autenticacion exitosa."
}

validate_required_vars() {
  log_info "Validando variables de entorno requeridas..."
  MISSING=""

  [ -z "${SPRING_DATASOURCE_PASSWORD}" ] && MISSING="${MISSING} SPRING_DATASOURCE_PASSWORD"
  [ -z "${APP_SECURITY_SECRET_KEY}" ] && MISSING="${MISSING} APP_SECURITY_SECRET_KEY"
  [ -z "${JWT_SECRET_KEY}" ] && MISSING="${MISSING} JWT_SECRET_KEY"

  if [ -n "${MISSING}" ]; then
    log_error "Las siguientes variables de entorno no estan definidas:${MISSING}"
    exit 1
  fi

  JWT_LEN=$(printf '%s' "${JWT_SECRET_KEY}" | wc -c)
  if [ "${JWT_LEN}" -lt 32 ]; then
    log_error "JWT_SECRET_KEY debe tener al menos 32 caracteres. Actual: ${JWT_LEN}"
    exit 1
  fi

  AES_LEN=$(printf '%s' "${APP_SECURITY_SECRET_KEY}" | wc -c)
  if [ "${AES_LEN}" -ne 16 ] && [ "${AES_LEN}" -ne 24 ] && [ "${AES_LEN}" -ne 32 ]; then
    log_error "APP_SECURITY_SECRET_KEY debe tener 16, 24 o 32 caracteres. Actual: ${AES_LEN}"
    exit 1
  fi

  log_info "Validacion de variables completada."
}

seed_secrets() {
  log_info "Escribiendo secretos en secret/inventario..."

  vault kv put secret/inventario \
    "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}" \
    "app.security.secret-key=${APP_SECURITY_SECRET_KEY}" \
    "jwt.secret-key=${JWT_SECRET_KEY}"

  log_info "Secretos sembrados correctamente en secret/inventario."
}

verify_secrets() {
  log_info "Verificando integridad de secretos en Vault..."

  i=0
  while [ "$i" -lt "$MAX_RETRIES" ]; do
    missing=0
    vault kv get -field=spring.datasource.password secret/inventario >/dev/null 2>&1 || missing=$((missing + 1))
    vault kv get -field=app.security.secret-key secret/inventario >/dev/null 2>&1 || missing=$((missing + 1))
    vault kv get -field=jwt.secret-key secret/inventario >/dev/null 2>&1 || missing=$((missing + 1))

    if [ "$missing" -eq 0 ]; then
      log_info "Verificacion exitosa: 3 secretos presentes en secret/inventario."
      return 0
    fi

    i=$((i + 1))
    log_info "Verificacion intento ${i}/${MAX_RETRIES} - faltan ${missing} secretos; reintentando en ${RETRY_DELAY}s..."
    sleep "$RETRY_DELAY"
  done

  log_error "Verificacion fallida: no se encontraron los 3 secretos esperados despues de ${MAX_RETRIES} intentos."
  exit 1
}

main() {
  log_info "=== Inicio de inicializacion de Vault ==="
  wait_for_vault
  authenticate
  validate_required_vars
  seed_secrets
  verify_secrets
  log_info "=== Inicializacion de Vault completada exitosamente ==="
}

main
