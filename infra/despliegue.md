# Guia de Despliegue Separado - Inventario

Fecha: 2026-03-20  
Audiencia: Desarrollo y Sistemas  
Objetivo: Desplegar frontend y backend en repos separados, con proxy reverso dedicado, DB externa y stack app en servidor de aplicacion.

## Arquitectura objetivo

Usuario -> `https://productos.lab` -> Proxy (Nginx/Traefik) -> Servidor App (frontend/backend) -> Servidor DB (MariaDB)

En servidor de aplicacion se despliega:

- `frontend1`, `frontend2`
- `backend1`, `backend2`
- `vault`, `vault-init`
- `prometheus`, `grafana`
- `json-mock-server` (build local desde repo infra)

Dominio de referencia:

- publico/interno: `productos.lab`
- routing: `/` al frontend, `/api` al backend

---

## Parte 1 - Lo que debe hacer Developer

## 1) Repos y contenido esperado

- `Web-App-Inventario-Client`
  - codigo React/Vite
  - `Dockerfile`
  - `.github/workflows/ci.yml`
  - `.github/workflows/docker-publish.yml`
- `Web-App-Inventario-API`
  - codigo Spring Boot
  - `Dockerfile`
  - `.github/workflows/ci.yml`
  - `.github/workflows/docker-publish.yml`
- `Web-App-Inventario-Infra`
  - `docker-compose.external-db.yml`
  - `json-mock-server/`
  - `.env.example`
  - `README.md`
  - `despliegue.md` (este archivo)
  - `vault/init/init-vault.sh`
  - `monitoring/prometheus/prometheus.yml`
  - `monitoring/grafana/`

Referencias:

- `Web-App-Inventario-API/README.md`
- `Web-App-Inventario-Client/README.md`
- `Web-App-Inventario-Infra/README.md`

## 2) CI/CD de repos de codigo

Backend y Frontend deben mantener 2 workflows:

- `ci.yml`: valida (lint/tests/build), no publica imagen de produccion.
- `docker-publish.yml`: publica imagen en GHCR en `main`, tags `v*`, y manual.

Politica recomendada:

- `feature/*`, `bugfix/*`, `chore/*` -> CI solamente.
- `main` -> imagen tecnica (`main`, `sha-<commit>`).
- `vX.Y.Z` -> imagen release (para despliegue).

## 3) Variables y secretos en GitHub

Ruta: `Repository -> Settings -> Secrets and variables -> Actions`

Frontend (Variables no sensibles):

- `VITE_GRAFANA_DASHBOARD_URL`
- `VITE_PROMETHEUS_DASHBOARD_URL`

Backend:

- normalmente basta `GITHUB_TOKEN` para GHCR publish.
- no guardar secretos reales de runtime (DB/Vault/JWT) en workflows de build.

## 4) Release usable por Sistemas

Cuando quieras version desplegable:

```bash
git tag v1.4.0
git push origin v1.4.0
```

Sistemas desplegara esa version (`v1.4.0`) o una `sha-*` concreta.

---

## Parte 2 - Lo que debe hacer Sistemas (DevOps)

## 1) Preparacion de infraestructura

Servidores recomendados:

- Proxy: Nginx/Traefik
- App: Docker Compose con frontend/backend/vault/monitoring/mock
- DB: MariaDB externa

DNS interno:

- `productos.lab` -> IP del proxy
- opcional: `app.productos.lab`, `db.productos.lab`

Conectividad minima:

- Proxy -> App: `8081`, `8082`, `8080`, `8083`
- App -> DB: `3306`
- Admin -> App: `3000` (Grafana), `9090` (Prometheus), `8200` (Vault si aplica)

## 2) Estructura de carpetas en servidor app

Ruta sugerida:

- `/opt/inventario/`
  - `Web-App-Inventario-Infra/`
  - `Web-App-Inventario-API/`
  - `Web-App-Inventario-Client/`

`Web-App-Inventario-Infra` es el punto de despliegue.

## 3) Crear archivo de entorno de runtime

Crear:

- `/opt/inventario/Web-App-Inventario-Infra/.env.docker`

Puedes partir de:

- `/opt/inventario/Web-App-Inventario-Infra/.env.example`

Variables minimas que debe completar Sistemas:

- `BACKEND_IMAGE`, `BACKEND_TAG`
- `FRONTEND_IMAGE`, `FRONTEND_TAG`
- `SPRING_DATASOURCE_URL` (a DB externa)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_SECURITY_SECRET_KEY`
- `JWT_SECRET_KEY`
- `VAULT_DEV_ROOT_TOKEN_ID`, `VAULT_TOKEN` (si sigues en dev mode)
- `GF_SECURITY_ADMIN_USER`, `GF_SECURITY_ADMIN_PASSWORD`

Nota:

- el backend lee secretos desde Vault en runtime.
- `vault-init` toma variables de `.env.docker` y las escribe en `secret/inventario`.

## 4) Desplegar en servidor app

Desde `Web-App-Inventario-Infra`:

```bash
docker login ghcr.io
docker compose -f docker-compose.external-db.yml --env-file .env.docker up -d
docker compose ps
```

Checks minimos:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8083/actuator/health
curl -I http://localhost:8081
curl -I http://localhost:8082
```

## 5) Configurar proxy de entrada (`productos.lab`)

Regla funcional obligatoria:

- `/` -> frontend pool (`8081`, `8082`)
- `/api` -> backend pool (`8080`, `8083`)

Referencia de bloque Nginx/Traefik:

- ver `Web-App-Inventario-Infra/README.md`

## 6) Orden de arranque recomendado

1. DB externa
2. Servidor App (compose)
3. Proxy

## 7) Seguridad minima recomendada

- No exponer MariaDB a internet.
- No guardar secretos reales en workflows de GitHub.
- Mantener `.env.docker` solo en servidor (`.gitignore` ya lo excluye).
- Restringir firewall por IP privada entre servidores.

---

## Checklist rapido

- [ ] Repos `API`, `Client`, `Infra` actualizados
- [ ] Workflows `ci.yml` y `docker-publish.yml` funcionando
- [ ] DNS `productos.lab` resuelve al proxy
- [ ] `.env.docker` creado en `Web-App-Inventario-Infra`
- [ ] Compose levantado sin errores
- [ ] Proxy enruta `/` y `/api`
- [ ] Healthchecks backend OK
- [ ] Acceso web por `https://productos.lab`

