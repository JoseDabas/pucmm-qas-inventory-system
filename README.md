# pucmm-qas-inventory-system
Sistema de Gestión de Inventarios Empresarial con enfoque en Full Stack Testing, Observabilidad y DevSecOps. Desarrollado como proyecto final para aplicar principios de calidad continua y automatización en un entorno empresarial.

## Arquitectura

El sistema sigue una arquitectura por capas (Clean Architecture) con separación entre dominio, aplicación e infraestructura.

Frontend (React + Vite)  ──►  Backend (Spring Boot 3)  ──►  PostgreSQL

│                              │

└──────────► Keycloak ◄────────┘

(OAuth2 / JWT)
Observabilidad:  Backend ──► Prometheus ──► Grafana

- **Autenticación:** OAuth2 / JWT vía Keycloak (Authorization Code Flow con redirect).
- **Autorización:** permisos granulares (scopes) validados con `@PreAuthorize("hasAuthority('...')")`.
- **Persistencia:** PostgreSQL con migraciones gestionadas por Flyway.
- **Auditoría:** Hibernate Envers (tablas `_AUD`).

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.5, Java 21, Gradle |
| Frontend | React, Vite, TypeScript |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | Keycloak 26, OAuth2, JWT |
| Auditoría | Hibernate Envers |
| Testing | JUnit 5, Mockito, Testcontainers, MockMvc, Playwright |
| Cobertura | JaCoCo |
| Documentación API | OpenAPI / Swagger UI |
| Observabilidad | Prometheus, Grafana, Micrometer |
| CI/CD | GitHub Actions, Jenkins |
| Contenedores | Docker, Docker Compose |


## Requisitos Previos

Antes de iniciar, asegúrate de tener instalado:

- **Docker Desktop** (con Docker Compose)
- **Java 21** (JDK Temurin recomendado)
- **Node.js 20+** y npm
- **Git**

Nota: el backend usa el Gradle Wrapper (`./gradlew`), por lo que no necesitas instalar Gradle por separado.

## Inicialización del Proyecto

El proyecto se levanta por capas. Sigue este orden la primera vez.

### 1. Clonar el repositorio

```bash
git clone https://github.com/JoseDabas/pucmm-qas-inventory-system.git
cd pucmm-qas-inventory-system
```


### 2. Configurar variables de entorno

La infraestructura usa secretos vía variables de entorno (no hay credenciales hardcodeadas en el repo). Crea el archivo `.env` dentro de `infrastructure/` a partir del ejemplo:

```bash
cd infrastructure
cp .env.example .env
```

Edita `infrastructure/.env` con los valores reales:

KEYCLOAK_CLIENT_SECRET=<tu-client-secret>
KEYCLOAK_TEST_USER_PASSWORD=<password-usuarios-de-prueba>

### 3. Levantar la infraestructura (PostgreSQL + Keycloak)

```bash
cd infrastructure
docker compose up
```

Esto levanta:
- **PostgreSQL** en `localhost:5432` (base `inventory_db`).
- **Keycloak** en `localhost:9080`, con el realm `Inventario` importado automáticamente desde `realm-export.json` (incluye 2 usuarios de prueba y 7 permisos granulares).

Para reiniciar desde cero (borrando datos): `docker compose down -v && docker compose up`

**Verificación:** abre `http://localhost:9080`, entra con el admin de Keycloak y confirma que el realm `Inventario` existe con los usuarios `admin-user` y `viewer-user`.

### 4. Levantar el backend (Spring Boot)

Con la infraestructura corriendo, en otra terminal:

```bash
cd backend
./gradlew bootRun
```

El backend arranca en `localhost:8080`. Al iniciar, Flyway aplica las migraciones automáticamente.

**Verificación:**
- API: `http://localhost:8080/api/v1/products` (requiere token JWT → responde 401 sin él).
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Métricas: `http://localhost:8080/actuator/prometheus`

### 5. Levantar el frontend (React + Vite)

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

El frontend arranca en `localhost:5173`. El login redirige a Keycloak (OAuth2).

## Usuarios de Prueba

El realm de Keycloak incluye dos usuarios para demostrar la autorización granular:

| Usuario | Permisos | Uso |
|---------|----------|-----|
| `admin-user` | Todos (product:manage, etc.) | Puede crear/editar/eliminar |
| `viewer-user` | Solo lectura (product:view, etc.) | Solo consulta (recibe 403 al intentar crear) |

## Pruebas

El proyecto cubre cuatro niveles de testing. Los comandos del backend se ejecutan desde la carpeta `backend/`.

### Pruebas Unitarias (JUnit + Mockito)

Validaciones de dominio y lógica de servicio. No requieren base de datos.

```bash
cd backend
./gradlew test --tests "*ProductTest" --tests "*CategoryTest" --tests "*ProductServiceTest" --tests "*ProductControllerApiTest"
```

### Pruebas de Integración (Testcontainers)

Levantan un PostgreSQL real efímero. **Requieren Docker corriendo.**

```bash
cd backend
./gradlew test --tests "*IntegrationTest"
```

### Pruebas de API (MockMvc)

Validan endpoints, status codes y permisos con seguridad simulada. Incluidas en el comando de unitarias (`*ProductControllerApiTest`).

### Ejecutar todas las pruebas

```bash
cd backend
./gradlew test
```

> Forzar re-ejecución (Gradle cachea resultados): `./gradlew cleanTest test`

### Reporte de Cobertura (JaCoCo)

```bash
cd backend
./gradlew test jacocoTestReport
```

El reporte HTML se genera en:

backend/build/reports/jacoco/test/html/index.html

### Reporte de Pruebas

Tras ejecutar los tests, el reporte detallado está en:

backend/build/reports/tests/test/index.html

## Security Testing (DevSecOps)

El proyecto cubre los controles obligatorios de seguridad combinando tests automatizados y escaneos en CI. Detalle completo y comandos en [`docs/security-testing.md`](docs/security-testing.md).

| Control | Cómo se cubre | Dónde |
|---------|---------------|-------|
| **Escaneo OWASP ZAP** | DAST autenticado: API scan sobre el spec OpenAPI (`/v3/api-docs`) con JWT real de Keycloak | Job CI `owasp-zap-dast` + `.zap/rules.tsv` |
| **Validación JWT** | Token ausente/malformado → 401, válido con rol → 200; mapeo de `realm_access.roles` a autoridades | `JwtValidationApiTest` |
| **Validación de permisos** | Autorización por scope (`@PreAuthorize`): permiso correcto → 200/201/204, incorrecto → 403 | `ProductControllerApiTest` |
| **Validación de CORS** | Preflight `OPTIONS`: origen permitido devuelve cabeceras CORS; origen no permitido → 403 | `CorsValidationApiTest` |
| **OWASP Dependency-Check / Snyk** | SCA de dependencias contra la NVD (Dependency-Check) + Snyk | Job CI `backend-security-scan` + plugin en `build.gradle` |
| **Validación de autenticación** | Sin token → 401; login real contra Keycloak validado en CI; ZAP corre autenticado | Tests + job `owasp-zap-dast` |

### Ejecución rápida

```bash
cd backend
# Tests de seguridad (JWT, CORS, permisos, autenticación)
./gradlew test --tests "*JwtValidationApiTest" --tests "*CorsValidationApiTest" --tests "*ProductControllerApiTest"

# Análisis de dependencias (requiere la variable de entorno NVD_API_KEY)
./gradlew dependencyCheckAnalyze
```

> **Secret requerido:** `NVD_API_KEY` (API key gratuita de la NVD) debe configurarse en **GitHub → Settings → Secrets and variables → Actions**. No va en ningún `.env` del repo.

## Observabilidad

Stack de Prometheus + Grafana para monitoreo de métricas en tiempo real.

### Levantar el stack

Con el backend corriendo (expone métricas en `/actuator/prometheus`):

```bash
cd infrastructure
docker compose -f docker-compose-observability.yml up -d
```

### Accesos

| Herramienta | URL | Credenciales |
|-------------|-----|--------------|
| Prometheus | `http://localhost:9090` | — |
| Grafana | `http://localhost:3000` | admin / admin |

**Verificación:**
- En Prometheus, `http://localhost:9090/targets` → el target `inventory-backend` debe estar **UP**.
- En Grafana, el dashboard de Spring Boot muestra métricas reales (requests, JVM, memoria, CPU).

## CI/CD

### GitHub Actions

Se ejecuta automáticamente en cada Pull Request hacia `main`. Definido en `.github/workflows/ci.yml`.

Jobs:
- `backend-build` — compila el backend.
- `backend-unit-tests` — pruebas unitarias y de API (incluye JWT y CORS).
- `backend-integration-tests` — pruebas de integración (Testcontainers).
- `backend-security-scan` — SCA de dependencias (Snyk + OWASP Dependency-Check).
- `backend-sonarqube` — análisis de calidad y cobertura (SonarCloud).
- `owasp-zap-dast` — escaneo dinámico OWASP ZAP (API scan autenticado).
- `openapi-contract-validation` — validación de contrato OpenAPI (Schemathesis).
- `frontend-build` — compila el frontend.

### Jenkins

Pipeline declarativo definido en `Jenkinsfile`. Jenkins corre localmente en Docker.

#### Levantar Jenkins

```bash
cd infrastructure
docker compose -f docker-compose-jenkins.yml up -d
```
Accede en `http://localhost:8082`. El pipeline `inventory-pipeline` ejecuta las etapas:
**Checkout → Build Backend → Unit Tests.**



