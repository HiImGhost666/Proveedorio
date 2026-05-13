# Deploy Templates (Proxmox + Nginx Proxy Manager)

Esta guia es para copiar/pegar en un entorno donde:

- Backend, Frontend, Mock, Vault, Prometheus y Grafana corren en un CT/VM de aplicacion.
- MariaDB corre en otro CT/VM separado (Proxmox).
- Reverse proxy: Nginx Proxy Manager (NPM).

---

## 1) Docker Compose (aplicacion)

Guarda esto como `docker-compose.external-db.yml` en `Web-App-Inventario-Infra`.

```yaml
services:
  json-mock-server:
    build:
      context: ./json-mock-server
    container_name: json-mock-server
    restart: unless-stopped
    expose:
      - "3001"
      - "3002"
      - "3003"
      - "3004"
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:3001/products"]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 20s

  vault:
    image: hashicorp/vault:1.16.2
    container_name: vault
    restart: unless-stopped
    cap_add:
      - IPC_LOCK
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: ${VAULT_DEV_ROOT_TOKEN_ID}
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    command: vault server -dev
    ports:
      - "8200:8200"
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://127.0.0.1:8200/v1/sys/health"]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 20s

  vault-init:
    image: curlimages/curl:8.8.0
    container_name: vault-init
    depends_on:
      vault:
        condition: service_healthy
    env_file:
      - .env.docker
    volumes:
      - ./vault/init:/scripts:ro
    entrypoint: ["/bin/sh", "/scripts/init-vault.sh"]
    restart: "no"

  backend1:
    image: ${BACKEND_IMAGE}:${BACKEND_TAG}
    container_name: backend1
    restart: unless-stopped
    depends_on:
      vault-init:
        condition: service_completed_successfully
      json-mock-server:
        condition: service_healthy
    env_file:
      - .env.docker
    environment:
      SERVER_PORT: 8080
      VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
    ports:
      - "8080:8080"

  backend2:
    image: ${BACKEND_IMAGE}:${BACKEND_TAG}
    container_name: backend2
    restart: unless-stopped
    depends_on:
      vault-init:
        condition: service_completed_successfully
      json-mock-server:
        condition: service_healthy
    env_file:
      - .env.docker
    environment:
      SERVER_PORT: 8080
      VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
    ports:
      - "8083:8080"

  frontend1:
    image: ${FRONTEND_IMAGE}:${FRONTEND_TAG}
    container_name: frontend1
    restart: unless-stopped
    depends_on:
      - backend1
    ports:
      - "8081:80"

  frontend2:
    image: ${FRONTEND_IMAGE}:${FRONTEND_TAG}
    container_name: frontend2
    restart: unless-stopped
    depends_on:
      - backend2
    ports:
      - "8082:80"

  prometheus:
    image: prom/prometheus:v2.53.1
    container_name: prometheus
    restart: unless-stopped
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:11.1.4
    container_name: grafana
    restart: unless-stopped
    environment:
      GF_SECURITY_ADMIN_USER: ${GF_SECURITY_ADMIN_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GF_SECURITY_ADMIN_PASSWORD}
    volumes:
      - ./monitoring/grafana:/var/lib/grafana
    ports:
      - "3000:3000"
```

---

## 2) Archivo `.env.docker`

Guarda esto como `Web-App-Inventario-Infra/.env.docker`.

```env
###############################################
# Images (GHCR)
###############################################
BACKEND_IMAGE=ghcr.io/<owner>/inventario-backend
BACKEND_TAG=main
FRONTEND_IMAGE=ghcr.io/<owner>/inventario-frontend
FRONTEND_TAG=main

###############################################
# External MariaDB (CT/VM separado)
# Opcion A: por IP (sin DNS interno)
###############################################
SPRING_DATASOURCE_URL=jdbc:mariadb://db.inventario.interno:3306/WebApiInventario?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root

###############################################
# Vault (dev mode lab)
###############################################
VAULT_DEV_ROOT_TOKEN_ID=inventario-dev-token
VAULT_TOKEN=inventario-dev-token

###############################################
# Secrets seeded in Vault by vault-init
###############################################
SPRING_DATASOURCE_PASSWORD=TestRootPwd123!
APP_SECURITY_SECRET_KEY=CLAVE_SUPER_SECRETA_DE_PRUEBA___
JWT_SECRET_KEY=CLAVE_JWT_SEGURA_PARA_HS256_MINIMO_32_CHARS__

###############################################
# Grafana
###############################################
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin12345
```

---

## 3) IP vs DNS en Proxmox (tu duda principal)

Si los CT/VM estan separados:

- Puedes usar **IP directa** en `SPRING_DATASOURCE_URL` (funciona sin DNS).
- Puedes usar **nombre DNS** solo si existe resolucion DNS real (servidor DNS interno o `/etc/hosts` bien configurado).

No es obligatorio crear un CT de DNS, pero es recomendable para no depender de IPs fijas.

---

## 4) Por que la web solo abre si editas `hosts`

No es engano: es comportamiento normal de Host-based routing.

- NPM enruta por **Host** (ej. `productos.lab`).
- Si entras por IP (`http://192.168.1.50`), el host no coincide y puede devolver pagina por defecto o 404.

Opciones:

1. Mantener dominio (`productos.lab`) y resolverlo por DNS interno.
2. Temporal: agregar entrada en `hosts` de clientes.
3. Crear tambien un proxy host en NPM para la IP directa (menos recomendable).

---

## 5) NPM con HTTPS (ahora solo HTTP)

Para HTTPS en Nginx Proxy Manager:

1. Crear `Proxy Host` para `productos.lab`.
2. Pestaña SSL:
   - Request a new SSL certificate (Let's Encrypt) si el dominio resuelve publicamente.
   - O usar certificado interno (CA corporativa) en entorno privado.
3. Activar:
   - Force SSL
   - HTTP/2 Support
   - HSTS (opcional en lab; recomendado en estable)

Si es red privada sin dominio publico, Let's Encrypt no valida. En ese caso usa:

- DNS interno + CA interna, o
- wildcard/cert gestionado por tu organizacion.

---

## 6) Comandos de arranque/verificacion

```bash
docker login ghcr.io
docker compose -f docker-compose.external-db.yml --env-file .env.docker up -d
docker compose ps

curl http://localhost:8080/actuator/health
curl http://localhost:8083/actuator/health
curl -I http://localhost:8081
curl -I http://localhost:8082
```

---

## 7) Nota sobre perfiles `test`

Los cambios de `profile=test` afectan solo pruebas (`mvn test`) y CI.

En despliegue normal:

- no uses `SPRING_PROFILES_ACTIVE=test`,
- backend arranca con configuracion real (`application.properties` + Vault + datasource externo).

