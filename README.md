<div align="center">

<p align="center">
  <picture>
    <source 
      media="(prefers-color-scheme: dark)" 
      srcset="frontend/src/assets/ProveedoresLogo_Light.png"
    >
    <source 
      media="(prefers-color-scheme: light)" 
      srcset="frontend/src/assets/ProveedoresLogo_Dark.png"
    >
    <img 
      src="frontend/src/assets/ProveedoresLogo_Light.png"
      alt="Logo" 
      width="300"
    >
  </picture>
</p>

# Inventory & Supplier Management System

### Full Stack Enterprise Inventory Platform

Aplicación full stack enfocada en gestión de inventario, proveedores, monitoreo y seguridad de secretos utilizando una arquitectura moderna basada en contenedores.

<br>

<img src="https://skillicons.dev/icons?i=java,spring,react,ts,docker,mysql,grafana,nginx"/>

<br>

![Status](https://img.shields.io/badge/status-active-success?style=for-the-badge)
![Java](https://img.shields.io/badge/java-21-orange?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-blue?style=for-the-badge)

</div>

---

## Features

- Gestión de inventario y proveedores
- API REST segura con JWT
- Monitoreo con Grafana y Prometheus
- Gestión de secretos con Vault
- Stack dockerizado completo
- Arquitectura escalable y desacoplada

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
