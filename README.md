# Web App Inventario

Proyecto full stack para gestion de inventario y proveedores.

## Resumen

- Backend: Spring Boot (Java 21)
- Frontend: React + Vite (Nginx)
- Base de datos: MariaDB (local por defecto)
- Secretos: Vault (dev mode)
- Observabilidad: Prometheus + Grafana + Alloy
- Mock de proveedores: json-mock-server (puertos 3001-3004)

## Estructura

- backend/: API Spring Boot
- frontend/: UI React + Vite
- infra/: Vault init, monitoring, mock server, docs de infraestructura
- docker-compose.yml: stack completo local
- .env / .env.example: variables de entorno
- logs/: logs de backend y frontend

## Requisitos

- Docker Desktop (con Docker Compose)

## Inicio rapido (local)

1) Copia variables de entorno:

```bash
copy .env.example .env
```

2) Levanta el stack:

```bash
docker compose up -d --build
```

3) Verifica:

- Frontend: http://localhost:8081
- Backend health: http://localhost:8080/actuator/health
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

## Variables de entorno clave (.env)

Minimo recomendado para local:

- SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb-local:3306/inventariodb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
- SPRING_DATASOURCE_USERNAME=inventario
- SPRING_DATASOURCE_PASSWORD=...
- APP_SECURITY_SECRET_KEY=...
- JWT_SECRET_KEY=...
- VAULT_DEV_ROOT_TOKEN_ID=...
- VAULT_TOKEN=...
- GF_SECURITY_ADMIN_USER=admin
- GF_SECURITY_ADMIN_PASSWORD=...

Opcional (si quieres setear valores en build del frontend):

- VITE_GRAFANA_DASHBOARD_URL
- VITE_PROMETHEUS_DASHBOARD_URL

## Vault y secretos

El backend lee secretos desde Vault (spring.config.import=vault://). Flujo:

1) .env define SPRING_DATASOURCE_PASSWORD, APP_SECURITY_SECRET_KEY y JWT_SECRET_KEY.
2) vault-init escribe esos secretos en secret/inventario.
3) backend arranca con VAULT_TOKEN y los consume desde Vault.

Nota: Vault esta en dev mode (solo para laboratorio/local).

## Logs y datos

- Logs de backend: logs/backend/
- Logs de frontend: logs/frontend/
- MariaDB y Grafana usan volumenes Docker: mariadb_data, grafana_data

## Reverse proxy (produccion)

La app usa base relativa /api/v1. En un proxy reverso:

- / -> frontend
- /api -> backend

## CI/CD

Workflows por carpeta:

- backend/.github/workflows/ci.yml y docker-publish.yml
- frontend/.github/workflows/ci.yml y docker-publish.yml

## Comandos utiles

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```
