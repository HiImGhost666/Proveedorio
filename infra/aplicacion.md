# Aplicacion Inventario - Estado Actual (Real)

Documento basado en el contenido actual de:

- `Web-App-Inventario-API`
- `Web-App-Inventario-Client`
- `Web-App-Inventario-Infra`

## 1) Mapa de arquitectura actual

### Resumen

- Frontend y backend estan separados en repositorios e imagenes Docker.
- CI/CD se gestiona con GitHub Actions por repo.
- Imagenes se publican en GHCR.
- El despliegue operativo se centraliza en Infra (Proxmox + Docker Compose + proxy + DB externa + Vault).
- Backend usa Vault en runtime para secretos.
- `json-mock-server` vive ahora en Infra y el backend lo consume por hostname interno `json-mock-server`.

### Diagrama ASCII (simplificado)

```txt
Usuario
  |
  v
Nginx Proxy Manager (productos.lab)
  |-- "/" ---------> frontend1/frontend2 (React+Vite, Nginx)
  |-- "/api" ------> backend1/backend2 (Spring Boot)
                        |
                        |-- lee secretos ---> Vault (secret/inventario)
                        |
                        |-- JDBC -----------> MariaDB externa (otro CT/VM)
                        |
                        |-- HTTP -----------> json-mock-server (3001..3004)
                        |
                        |-- metrics --------> Prometheus -> Grafana
```

## 2) Que pertenece a app, infra y CI/CD

### App (codigo de negocio)

- `Web-App-Inventario-API`
  - Spring Boot Java 21
  - `src/main/java/...`
  - `application.properties` con `spring.config.import=vault://`
  - `Dockerfile` (imagen backend)
- `Web-App-Inventario-Client`
  - React + Vite
  - `src/...`
  - `API_BASE=/api/v1` (ruta relativa orientada a reverse proxy)
  - `Dockerfile` + `nginx/` interno para servir SPA

### Infra (operacion/despliegue)

- `Web-App-Inventario-Infra`
  - docs de despliegue (`README.md`, `despliegue.md`, `DEPLOY_TEMPLATES_PROXMOX.md`)
  - `json-mock-server/` (Dockerfile + `api1..api4.json`)
  - `vault/init/init-vault.sh`
  - `monitoring/prometheus/prometheus.yml`
  - `monitoring/grafana/...`
  - variables runtime `.env.docker` (fuera de git)

### CI/CD (automatizacion)

- API:
  - `.github/workflows/ci.yml`
    - valida test/package
    - usa perfil `test` para tests
    - prepara Vault en CI para secretos de prueba
  - `.github/workflows/docker-publish.yml`
    - build + push imagen backend a GHCR
    - tags: branch, tag `v*`, `sha-*`, `latest` en default branch
- Client:
  - `.github/workflows/ci.yml`
    - npm ci + lint + test + build
  - `.github/workflows/docker-publish.yml`
    - build + push imagen frontend a GHCR
    - inyecta build args `VITE_*` desde `vars.*`

## 3) Flujo completo (commit -> CI -> publish -> deploy -> acceso)

1. Developer hace commit/push.
2. GitHub Actions ejecuta `ci.yml` del repo afectado.
3. Si es push/tag permitido, `docker-publish.yml` construye y publica imagen en GHCR.
4. En entorno de despliegue (Proxmox), Infra hace pull de imagenes (`BACKEND_IMAGE:BACKEND_TAG`, `FRONTEND_IMAGE:FRONTEND_TAG`).
5. Docker Compose levanta servicios.
6. Nginx Proxy Manager enruta:
   - `/` a frontend
   - `/api` a backend
7. Backend lee secretos de Vault y conecta a MariaDB externa.

## 4) Dependencias criticas

1. **MariaDB externa**
   - sin DB, backend no levanta correctamente.
2. **Vault**
   - backend espera secretos desde `secret/inventario`.
3. **Proxy + DNS**
   - `productos.lab` debe resolver al proxy para que el host-based routing funcione.
4. **Red interna Proxmox**
   - conectividad entre app CT, DB CT y proxy.
5. **GHCR**
   - para pull de imagenes en despliegue.
6. **json-mock-server**
   - para sincronizaciones con proveedores mock (segun uso/entorno).

## 5) Riesgos actuales y quick wins

### Riesgos

- Dependencia fuerte de Vault dev mode en algunos escenarios.
- Si DNS interno no resuelve, el acceso por dominio falla (aunque servicios esten vivos).
- Diferencias entre docs y estado real si no se mantiene compose/versionado sincronizado.
- CI frontend tiene `continue-on-error` en tests, lo que puede ocultar fallos.

### Quick wins

1. Versionar y mantener estable `docker-compose.external-db.yml` en Infra.
2. Hacer obligatorio test frontend (quitar `continue-on-error` cuando haya suite madura).
3. Definir politica de tags release (`vX.Y.Z`) para despliegue reproducible.
4. Añadir smoke tests post-deploy (health endpoints y ruta front).
5. Planificar transicion de Vault dev a modo productivo.

## 6) "Microservicios" aqui: aplica o no aplica

Actualmente, esto encaja mejor como:

- **Frontend separado + Backend monolitico modular** + componentes de infraestructura.

No es microservicios estrictos (backend no esta partido en varios servicios de dominio independientes con despliegue/autonomia propia).  
Si crece, puede evolucionar a microservicios, pero hoy el backend principal funciona como un servicio unico.

## 7) Checklist de validacion de despliegue

### Basico

- [ ] `docker compose ps` sin servicios en estado error.
- [ ] Backend 1 health OK: `http://<host>:8080/actuator/health`
- [ ] Backend 2 health OK: `http://<host>:8083/actuator/health`
- [ ] Frontend 1 responde: `http://<host>:8081`
- [ ] Frontend 2 responde: `http://<host>:8082`
- [ ] Proxy enruta `https://productos.lab/` (frontend)
- [ ] Proxy enruta `https://productos.lab/api/...` (backend)
- [ ] Vault accesible y secretos cargados en `secret/inventario`
- [ ] Conexion DB correcta (logs backend sin errores JDBC)

### Logs

- [ ] `docker logs backend1 --tail 200` sin errores criticos
- [ ] `docker logs backend2 --tail 200` sin errores criticos
- [ ] `docker logs vault --tail 200` sin fallos de init
- [ ] `docker logs json-mock-server --tail 200` si aplica sincronizacion

### Rollback basico

1. Cambiar `BACKEND_TAG`/`FRONTEND_TAG` en `.env.docker` a version previa estable.
2. Ejecutar:
   - `docker compose --env-file .env.docker pull`
   - `docker compose --env-file .env.docker up -d`
3. Repetir checklist de health.
