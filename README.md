# pucmm-qas-inventory-system

Sistema de Gestión de Inventarios Empresarial con enfoque en Full Stack Testing, Observabilidad y DevSecOps. Desarrollado como proyecto final para aplicar principios de calidad continua y automatización en un entorno empresarial.

El sistema permite administrar un catálogo de productos y categorías, llevar un historial inmutable de movimientos de stock (entradas/salidas), consultar métricas y alertas de inventario, generar reportes en PDF, gestionar cuentas y permisos, y auditar cada cambio. Todo bajo autenticación OAuth2/JWT con permisos granulares, cubierto por múltiples niveles de pruebas, escaneos de seguridad automatizados y un stack de observabilidad (métricas, logs y trazas).

## Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Repositorio](#estructura-del-repositorio)
- [Requisitos Previos](#requisitos-previos)
- [Inicialización del Proyecto](#inicialización-del-proyecto)
- [Funcionalidades de la Aplicación](#funcionalidades-de-la-aplicación)
  - [Referencia de la API REST](#referencia-de-la-api-rest)
  - [Pantallas del Frontend](#pantallas-del-frontend)
- [Autenticación y Autorización](#autenticación-y-autorización)
- [Usuarios de Prueba](#usuarios-de-prueba)
- [Pruebas](#pruebas)
- [Security Testing (DevSecOps)](#security-testing-devsecops)
- [Observabilidad](#observabilidad)
- [CI/CD](#cicd)
- [Secretos y Variables de Entorno](#secretos-y-variables-de-entorno)

## Arquitectura

El sistema sigue una arquitectura por capas (Clean / Hexagonal) con separación estricta entre dominio, aplicación e infraestructura.

```
Frontend (React + Vite)  ──►  Backend (Spring Boot 3)  ──►  PostgreSQL
        │                             │
        └──────────► Keycloak ◄───────┘
                 (OAuth2 / JWT)

Observabilidad:
  Backend ──► /actuator/prometheus ──► Prometheus ──► Grafana
  Backend ──► OTLP ──► Grafana Alloy ──► Loki (logs) + Tempo (trazas) ──► Grafana
```

- **Autenticación:** OAuth2 / JWT vía Keycloak (Direct Access Grants desde el frontend; validación de firma RSA vía JWKS en el backend).
- **Autorización:** permisos granulares (scopes) validados con `@PreAuthorize("hasAuthority('...')")`; el mapeo se hace desde `realm_access.roles` del token.
- **Persistencia:** PostgreSQL con migraciones gestionadas por Flyway (`ddl-auto: validate`, Hibernate nunca modifica el esquema).
- **Auditoría:** Hibernate Envers (tablas `_AUD`); cada cambio, incluida la eliminación, queda registrado.

### Capas del backend

Paquete raíz `edu.pucmm.cs.inventory`:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| **Dominio** | `domain` | Entidades y reglas de negocio puras (`Product`, `Category`, `StockMovement`, `MovementType`). |
| **Aplicación** | `application` | Casos de uso / servicios que orquestan dominio e infraestructura (`ProductService`, `CategoryService`, `StockMovementService`, `DashboardService`, `ReportService`, `ProductAuditService`, `StockMovementAuditService`, `KeycloakAdminService`). |
| **Infraestructura – Web** | `infrastructure.web` | Controllers REST + DTOs, filtros y manejo global de excepciones. |
| **Infraestructura – Persistencia** | `infrastructure.persistence` | Repositorios Spring Data JPA, entidades y vistas/proyecciones. |
| **Infraestructura – Seguridad** | `infrastructure.security` | `SecurityConfig`, `Permissions`, `SystemRole`, conversor de roles de Keycloak. |
| **Infraestructura – Config** | `infrastructure.config` | Beans de Spring (OpenAPI, Jackson, cliente admin de Keycloak). |

**Flujo de una petición:** `Controller` (enrutamiento HTTP + `@PreAuthorize` + validación estructural) → `Service` (lógica de negocio, `@Transactional`) → `JpaRepository` → Entidad JPA → PostgreSQL.

### Migraciones de base de datos (Flyway)

| Versión | Propósito |
|---------|-----------|
| `V1` | Esquema inicial: tablas `categories` y `products` con restricciones. |
| `V2` | Tabla `stock_movements` con FK a `products`. |
| `V3` | Tablas de auditoría de Envers (`revinfo`, `products_aud`, `stock_movements_aud`). |
| `V4` | Alinea la auditoría de producto a `category_id` (FK) en lugar de texto. |
| `V5` | Datos de prueba (categorías, productos y movimientos) para QA. |
| `V6` | Añade snapshots `previous_quantity` y `new_quantity` a los movimientos. |
| `V7` | Elimina la columna `quantity` redundante (se conservan los snapshots). |
| `V8` | Añade `created_at` a productos y categorías (backfill desde el primer movimiento). |
| `V9` | Añade la columna `deleted` a productos, categorías y `products_aud`. |
| `V10` | Elimina la columna `deleted`; la eliminación vuelve a ser física (hard delete). |

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.5, Java 21, Gradle |
| Frontend | React 19, Vite, TypeScript, Tailwind CSS |
| Cliente HTTP / Auth (frontend) | axios, react-oidc-context / oidc-client-ts |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | Keycloak 26, OAuth2, JWT |
| Auditoría | Hibernate Envers |
| Reportes | OpenPDF (generación de PDF) |
| Logs estructurados | Logstash Logback Encoder (JSON) |
| Testing backend | JUnit 5, Mockito, Testcontainers, MockMvc, REST Assured |
| Testing frontend / E2E | Playwright |
| Contract testing | Schemathesis (OpenAPI) |
| Performance testing | k6 |
| Cobertura | JaCoCo |
| Documentación API | OpenAPI / Swagger UI (springdoc) |
| Observabilidad | Prometheus, Grafana, Micrometer, Loki, Tempo, Grafana Alloy, Node Exporter |
| DevSecOps | SonarCloud (SAST), Snyk + OWASP Dependency-Check (SCA), OWASP ZAP (DAST) |
| CI/CD | GitHub Actions, Jenkins |
| Contenedores | Docker, Docker Compose |

## Estructura del Repositorio

```
pucmm-qas-inventory-system/
├── backend/            # API Spring Boot (dominio, aplicación, infraestructura) + tests + migraciones Flyway
├── frontend/           # SPA React + Vite + TypeScript + tests E2E de Playwright
├── infrastructure/     # Docker Compose (Postgres, Keycloak, backend, frontend, observabilidad, Jenkins)
│   ├── keycloak/       # realm-export.json (realm, cliente, roles, usuarios de prueba)
│   ├── prometheus/     # Configuración de scraping de métricas
│   ├── grafana/        # Datasources y dashboards provisionados
│   ├── loki/ tempo/ alloy/  # Stack de logs y trazas (OTLP)
│   └── .env.example    # Plantilla de variables de entorno
├── performance/        # Scripts de carga con k6 (api-performance.js)
├── .github/workflows/  # Pipeline de GitHub Actions (ci.yml)
├── .zap/               # Reglas de OWASP ZAP (rules.tsv)
├── Jenkinsfile         # Pipeline declarativo de Jenkins (CI + CD)
└── README.md
```

## Requisitos Previos

Antes de iniciar, asegúrate de tener instalado:

- **Docker Desktop** (con Docker Compose)
- **Java 21** (JDK Temurin recomendado)
- **Node.js 20+** y npm
- **Git**

Notas:
- El backend usa el Gradle Wrapper (`./gradlew`), por lo que no necesitas instalar Gradle por separado.
- `k6` y los navegadores de Playwright solo hacen falta para las etapas avanzadas del pipeline de Jenkins (performance y E2E); no son necesarios para levantar la aplicación localmente.

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

```
KEYCLOAK_CLIENT_SECRET=<tu-client-secret>
KEYCLOAK_TEST_USER_PASSWORD=<password-usuarios-de-prueba>
```

### 3. Levantar la infraestructura (PostgreSQL + Keycloak)

```bash
cd infrastructure
docker compose up
```

Esto levanta:
- **PostgreSQL** en `localhost:5432` (base `inventory_db`).
- **Keycloak** en `localhost:9080`, con el realm `Inventario` importado automáticamente desde `keycloak/realm-export.json` (incluye 2 usuarios de prueba, 7 permisos granulares y el cliente `inventory-client`).

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

El frontend arranca en `localhost:5173`. El login valida las credenciales contra Keycloak (Direct Access Grants) y guarda el JWT en `sessionStorage`.

## Funcionalidades de la Aplicación

La API expone su lógica bajo el prefijo `/api/v1`. Todos los endpoints (salvo el dashboard, que solo exige estar autenticado) validan un permiso granular. A continuación, el detalle por módulo.

### Productos

- **CRUD completo** de productos (crear, listar, actualizar, eliminar).
- **SKU único** y `precio`, `cantidad inicial` y `stock mínimo` no negativos (validado por restricciones de BD y validaciones de dominio).
- El **stock actual se calcula al vuelo** desde el historial inmutable de movimientos (cantidad inicial + entradas − salidas), evitando condiciones de carrera.
- Al **crear** un producto se registra automáticamente un movimiento de entrada (IN) con la cantidad inicial.
- **Búsqueda** por nombre o SKU (`?search=`, case-insensitive), con paginación y ordenamiento.
- **Eliminación** del producto; el historial de cambios queda en la auditoría de Envers.
- Permisos: `product:view` (consulta), `product:manage` (escritura).

### Categorías

- **Crear, listar y eliminar** categorías; cada listado incluye el **conteo de productos** asociados.
- **Nombre único**.
- **Bloqueo 409 Conflict** al intentar eliminar una categoría con productos asociados.
- Permisos: `product:view` (consulta), `product:manage` (escritura).

### Movimientos de Stock

- **Historial inmutable** (ledger) de entradas (IN) y salidas (OUT).
- Cada movimiento guarda **snapshots** de la cantidad anterior (`previousQuantity`) y nueva (`newQuantity`).
- **Validación de negocio:** una salida que dejaría el inventario en negativo es rechazada.
- **Búsqueda** por nombre de producto o usuario (`?search=`), ordenado por fecha descendente, con paginación.
- Permisos: `stock:view` (consulta), `stock:manage` (registrar movimientos).

### Dashboard

- Métricas agregadas del inventario: total de productos, total de unidades, valor total del inventario, total de categorías, total de movimientos y conteo de productos en stock crítico.
- Accesible para **cualquier usuario autenticado** (no exige un permiso específico).

### Alertas de Stock Crítico

- Lista los productos cuyo **stock actual es menor o igual a su stock mínimo** (estado crítico o agotado), calculando el stock desde el historial.
- Permiso: `report:view`.

### Reportes

- Generación de un **reporte PDF** del historial de movimientos por **rango de fechas** (ISO-8601) y **categoría opcional**.
- Permiso: `report:view`.

### Usuarios y Roles

- **Listar y crear cuentas** en Keycloak desde el backend, **asignar/cambiar el rol** de un usuario y ver sus permisos efectivos.
- Un **rol** es una combinación de permisos (ver [Autenticación y Autorización](#autenticación-y-autorización)); al asignarlo, el backend concede en Keycloak solo los permisos que lo componen.
- Permiso: `user:manage`.

### Auditoría

- Consulta del **historial inmutable de cambios** (Hibernate Envers) para productos y movimientos de stock, con el tipo de revisión (CREATED / UPDATED / DELETED).
- Permiso: `audit:view`.

### Referencia de la API REST

| Método | Ruta | Permiso | Descripción | Éxito |
|--------|------|---------|-------------|-------|
| `GET` | `/api/v1/products` | `product:view` | Lista paginada de productos; `?search=` filtra por nombre o SKU. | 200 |
| `GET` | `/api/v1/products/alerts/critical-stock` | `report:view` | Productos con stock ≤ mínimo. | 200 |
| `POST` | `/api/v1/products` | `product:manage` | Crea un producto y registra el movimiento IN inicial. | 201 |
| `PUT` | `/api/v1/products/{id}` | `product:manage` | Actualiza los metadatos de un producto. | 200 |
| `DELETE` | `/api/v1/products/{id}` | `product:manage` | Elimina el producto. | 204 |
| `GET` | `/api/v1/categories` | `product:view` | Lista categorías con su conteo de productos. | 200 |
| `POST` | `/api/v1/categories` | `product:manage` | Crea una categoría (nombre único). | 201 |
| `DELETE` | `/api/v1/categories/{id}` | `product:manage` | Elimina la categoría; 409 si tiene productos asociados. | 204 |
| `GET` | `/api/v1/stock-movements` | `stock:view` | Historial paginado; `?search=` por producto o usuario. | 200 |
| `POST` | `/api/v1/stock-movements` | `stock:manage` | Registra un movimiento IN/OUT; rechaza stock negativo. | 201 |
| `GET` | `/api/v1/dashboard/metrics` | *autenticado* | Métricas e indicadores agregados del inventario. | 200 |
| `GET` | `/api/v1/reports/movements` | `report:view` | Reporte PDF por `startDate`, `endDate` y `categoryId` opcional. | 200 |
| `GET` | `/api/v1/admin/roles` | `user:manage` | Catálogo de roles del sistema y sus permisos. | 200 |
| `GET` | `/api/v1/admin/users` | `user:manage` | Lista de cuentas con su rol y permisos. | 200 |
| `POST` | `/api/v1/admin/users` | `user:manage` | Crea una cuenta y le asigna un rol. | 201 |
| `PUT` | `/api/v1/admin/users/{id}/role` | `user:manage` | Cambia el rol (conjunto de permisos) de una cuenta. | 200 |
| `GET` | `/api/v1/audit/products` | `audit:view` | Historial de revisiones de productos (Envers). | 200 |
| `GET` | `/api/v1/audit/stock-movements` | `audit:view` | Historial de revisiones de movimientos (Envers). | 200 |

> Toda petición sin token válido recibe **401**; con token válido pero sin el permiso requerido, **403**. El contrato completo está en Swagger UI (`/swagger-ui.html`) y en `/v3/api-docs`.

### Pantallas del Frontend

La SPA monta un layout de dashboard tras el login. El menú y los botones se ocultan según los permisos del usuario (defensa en profundidad: la autorización real siempre la impone el backend).

| Ruta | Pantalla | Permiso requerido |
|------|----------|-------------------|
| `/dashboard` | Métricas e indicadores del inventario (aterrizaje). | *autenticado* |
| `/inventario` | Gestión de productos (CRUD, búsqueda, orden, paginación, indicador de stock crítico). | `product:view` |
| `/historial` | Historial de movimientos + registro de entradas/salidas. | `stock:view` |
| `/categorias` | Gestión de categorías (crear/eliminar, conteo de productos). | `product:view` |
| `/alertas` | Productos en estado crítico/agotado. | `report:view` |
| `/reportes` | Generación de reportes PDF por rango de fechas. | `report:view` |
| `/usuarios` | Administración de cuentas y roles. | `user:manage` |
| `/auditoria` | Historial de auditoría (pestañas de productos y movimientos). | `audit:view` |

## Autenticación y Autorización

- **Identidad:** Keycloak 26, realm `Inventario`, cliente `inventory-client`.
- **Frontend → Keycloak:** login por *Direct Access Grants* (grant `password`); el JWT se guarda en `sessionStorage` y axios lo inyecta como `Authorization: Bearer <token>` en cada petición.
- **Backend (OAuth2 Resource Server):** valida criptográficamente la firma del JWT descargando las claves públicas RSA del endpoint JWKS de Keycloak y verifica la vigencia temporal (`exp`/`nbf`). Sesión **stateless**, **CSRF deshabilitado** (token en cabecera, no en cookies).
- **Mapeo de autoridades:** un conversor extrae `realm_access.roles` del token y los convierte en autoridades que consumen las anotaciones `@PreAuthorize("hasAuthority('...')")`.
- **CORS:** orígenes `http://localhost:*` y `http://host.docker.internal:*`; cabeceras permitidas incluyen `Authorization`, `Content-Type`, `traceparent` y `X-Correlation-ID`; credenciales habilitadas.
- **Endpoints públicos:** solo `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` y `/actuator/**`. Todo lo demás exige autenticación.

### Permisos granulares (7)

La fuente única de verdad es `Permissions.java`, y cada permiso corresponde a un realm role en Keycloak.

| Permiso | Alcance |
|---------|---------|
| `product:view` | Ver catálogo de productos y categorías. |
| `product:manage` | Crear, editar y eliminar productos y categorías. |
| `stock:view` | Ver niveles de inventario e historial de movimientos. |
| `stock:manage` | Registrar entradas, salidas y ajustes de stock. |
| `report:view` | Acceder y exportar reportes y el dashboard/alertas. |
| `user:manage` | Gestionar usuarios, roles y permisos. |
| `audit:view` | Consultar registros de auditoría. |

### Roles del sistema (5)

Un rol es solo una **combinación de permisos** (`SystemRole.java`) para simplificar la asignación; la autorización siempre se hace por permiso, nunca por nombre de rol.

| Rol | Nombre visible | Permisos |
|-----|----------------|----------|
| `ADMIN` | Administrador | Los 7 permisos. |
| `INVENTORY_MANAGER` | Gerente de Inventario | `product:view`, `product:manage`, `stock:view`, `stock:manage`, `report:view`. |
| `WAREHOUSE_CLERK` | Almacenista | `product:view`, `stock:view`, `stock:manage`. |
| `VIEWER` | Consulta | `product:view`, `stock:view`, `report:view`. |
| `AUDITOR` | Auditor | `report:view`, `audit:view`. |

## Usuarios de Prueba

El realm de Keycloak incluye dos usuarios para demostrar la autorización granular:

| Usuario | Rol | Permisos | Uso |
|---------|-----|----------|-----|
| `admin-user` | ADMIN | Todos (7) | Puede crear/editar/eliminar en todos los módulos. |
| `viewer-user` | VIEWER | `product:view`, `stock:view`, `report:view` | Solo consulta (recibe 403 al intentar escribir). |

## Pruebas

El proyecto cubre varios niveles de testing. Los comandos del backend se ejecutan desde la carpeta `backend/`.

### Pruebas Unitarias (JUnit + Mockito)

Validaciones de dominio y lógica de servicio. No requieren base de datos.

- **Dominio:** `ProductTest`, `CategoryTest`, `StockMovementTest`.
- **Permisos:** `PermissionsTest` (confirma los 7 permisos), `SystemRoleTest`.
- **Servicios (con mocks):** `ProductServiceTest`, `CategoryServiceTest`, `StockMovementServiceTest`, `DashboardServiceTest`, `ReportServiceTest`, `KeycloakAdminServiceTest`.
- **Generación de PDF:** `PdfReportGeneratorTest`.

```bash
cd backend
./gradlew test --tests "*ProductTest" --tests "*CategoryTest" --tests "*StockMovementTest" --tests "*ProductServiceTest" --tests "*CategoryServiceTest" --tests "*StockMovementServiceTest"
```

### Pruebas de API (MockMvc)

Validan endpoints, status codes y permisos con seguridad simulada: `ProductControllerApiTest`, `CategoryControllerApiTest`, `StockMovementControllerApiTest`, `AdminControllerApiTest`, `DashboardControllerApiTest`, `ReportControllerApiTest`.

### Pruebas de Seguridad

- `JwtValidationApiTest` — token ausente/malformado → 401; válido con rol → 200; mapeo de `realm_access.roles`.
- `CorsValidationApiTest` — preflight `OPTIONS`: origen permitido devuelve cabeceras CORS; origen no permitido → 403.
- `ProductApiContractTest` — verifica que sin `Authorization` la API responde 401 (REST Assured + Testcontainers).

### Pruebas de Integración (Testcontainers)

Levantan un PostgreSQL real efímero (`AbstractIntegrationTest`). **Requieren Docker corriendo.** Cubren repositorios y la auditoría de Envers.

```bash
cd backend
./gradlew test --tests "*IntegrationTest"
```

### Pruebas E2E del Frontend (Playwright)

En `frontend/`, cubren flujos completos por rol: `inventory`, `categories`, `users`, `dashboard`, `movements` y `sidebar-rbac` (visibilidad de menú según permisos).

### Pruebas de Rendimiento (k6)

Script `performance/api-performance.js` para carga y validación de SLAs; se ejecuta como etapa del pipeline de Jenkins sobre Staging.

### Ejecutar todas las pruebas del backend

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

El reporte HTML se genera en `backend/build/reports/jacoco/test/html/index.html`. La cobertura excluye DTOs, entidades de persistencia, clases de configuración y la clase de arranque. El reporte de pruebas queda en `backend/build/reports/tests/test/index.html`.

## Security Testing (DevSecOps)

El proyecto cubre los controles obligatorios de seguridad combinando tests automatizados y escaneos en CI.

| Control | Tipo | Cómo se cubre | Dónde |
|---------|------|---------------|-------|
| **OWASP ZAP** | DAST | API scan autenticado sobre el spec OpenAPI (`/v3/api-docs`) con JWT real de Keycloak, con reglas en `.zap/rules.tsv`. | Job CI `owasp-zap-dast` |
| **Validación JWT** | Test | Token ausente/malformado → 401; válido con rol → 200; mapeo de `realm_access.roles`. | `JwtValidationApiTest` |
| **Validación de permisos** | Test | Autorización por scope (`@PreAuthorize`): permiso correcto → 200/201/204, incorrecto → 403. | `*ControllerApiTest` |
| **Validación de CORS** | Test | Preflight `OPTIONS`: origen permitido devuelve cabeceras CORS; no permitido → 403. | `CorsValidationApiTest` |
| **OWASP Dependency-Check** | SCA | Analiza dependencias contra la NVD; falla el build con CVSS ≥ 7.0. | Job CI `backend-security-scan` + plugin en `build.gradle` |
| **Snyk** | SCA | Escaneo de vulnerabilidades en dependencias (backend y frontend). | Job CI `backend-security-scan` + Jenkins |
| **SonarCloud** | SAST | Calidad, bugs, vulnerabilidades, code smells, duplicación y cobertura. | Job CI `backend-sonarqube` + Jenkins |
| **Schemathesis** | Contrato | Property-based testing del contrato OpenAPI, autenticado. | Job CI `openapi-contract-validation` |

### Ejecución rápida

```bash
cd backend
# Tests de seguridad (JWT, CORS, permisos)
./gradlew test --tests "*JwtValidationApiTest" --tests "*CorsValidationApiTest" --tests "*ProductControllerApiTest"

# Análisis de dependencias (requiere la variable de entorno NVD_API_KEY)
./gradlew dependencyCheckAnalyze
```

> **Secret requerido:** `NVD_API_KEY` (API key gratuita de la NVD) debe configurarse en **GitHub → Settings → Secrets and variables → Actions**. No va en ningún `.env` del repo.

## Observabilidad

Stack completo de métricas, logs y trazas para monitoreo en tiempo real.

### Componentes

- **Prometheus** — recolecta (scrape cada 15s) las métricas expuestas por el backend en `/actuator/prometheus` (vía Micrometer) y las del `node-exporter`.
- **Grafana** — visualización; incluye datasources y **4 dashboards provisionados**: `application` (tráfico HTTP, JVM/CPU, pool HikariCP), `security` (401/403, autenticación, logs de seguridad), `business` (operaciones y creación de entidades) y `node-exporter-full` (host).
- **Loki** — agregación centralizada de logs.
- **Tempo** — backend de trazas distribuidas.
- **Grafana Alloy** — colector OTLP que recibe la telemetría del backend y la enruta: logs → Loki, trazas → Tempo.
- **Node Exporter** — métricas del host (CPU, memoria, disco, red).

El backend se instrumenta con **OpenTelemetry** (variables `OTEL_*` en Docker Compose) para exportar trazas y logs vía OTLP a Alloy, y el frontend propaga la cabecera `traceparent` (W3C Trace Context) para el rastreo extremo a extremo.

### Levantar el stack

Con el backend corriendo (expone métricas en `/actuator/prometheus`):

```bash
cd infrastructure
docker compose -f docker-compose-observability.yml up -d
```

### Accesos

| Herramienta | URL / Puerto | Credenciales |
|-------------|--------------|--------------|
| Prometheus | `http://localhost:9090` | — |
| Grafana | `http://localhost:3000` | admin / admin (o `GRAFANA_ADMIN_PASSWORD`) |
| Loki | `localhost:3100` | — |
| Tempo | `localhost:3200` | — |
| Alloy (OTLP) | `localhost:4317` (gRPC), `4318` (HTTP) | — |

**Verificación:**
- En Prometheus, `http://localhost:9090/targets` → el target `inventory-backend` debe estar **UP**.
- En Grafana, los dashboards muestran métricas reales (requests, JVM, seguridad, negocio) y las trazas/logs aparecen desde Tempo/Loki.

## CI/CD

### GitHub Actions

Se ejecuta en cada Pull Request hacia `main` y en cada push a `main` (para que el dashboard de SonarCloud refleje el Quality Gate). Definido en `.github/workflows/ci.yml`.

| Job | Qué hace | Herramientas / secretos |
|-----|----------|-------------------------|
| `backend-build` | Compila el backend (`./gradlew build -x test`). | JDK 21 |
| `backend-unit-tests` | Ejecuta las pruebas unitarias, de servicio y de API (dominio, seguridad, controllers, PDF). | JDK 21 |
| `backend-integration-tests` | Pruebas de integración con Testcontainers (`*IntegrationTest`). | Docker |
| `backend-security-scan` | SCA con **Snyk** (`snyk test --all-subprojects`) y **OWASP Dependency-Check** (`dependencyCheckAnalyze`); publica el reporte como artefacto. | `SNYK_TOKEN`, `NVD_API_KEY` |
| `backend-sonarqube` | Corre tests + `jacocoTestReport` + análisis SonarCloud (calidad y cobertura); requiere historial completo para decorar el PR. | `SONAR_TOKEN`, `GITHUB_TOKEN` |
| `frontend-build` | Instala dependencias (`npm ci`) y compila el frontend (`npm run build`). | Node 20 |
| `openapi-contract-validation` | Levanta Postgres + Keycloak + backend, obtiene un token real y valida el contrato con **Schemathesis** sobre `/v3/api-docs`. | Docker |
| `owasp-zap-dast` | Levanta la app, obtiene un JWT real e inyecta el Bearer para un **API scan autenticado de OWASP ZAP**; publica el reporte como artefacto. | Docker |

### Jenkins

Pipeline declarativo en `Jenkinsfile`, con dos fases (CI y CD). Los secretos se inyectan desde el almacén de credenciales de Jenkins; las variables no sensibles se leen de `infrastructure/.env`.

**Fase 1 — CI:**
1. **Checkout** — obtiene el código (`checkout scm`).
2. **Build Backend** — compila con Gradle (`build -x test`).
3. **Build Frontend** — `npm ci` + `npm run build`.
4. **Unit Tests** — pruebas unitarias y de API del backend.
5. **Integration Tests** — Testcontainers (`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`).
6. **Quality Gates** — análisis SonarQube (`sonar`), tolerante a fallo (marca `UNSTABLE`).
7. **Security Scan** — Snyk en backend y frontend (`UNSTABLE` ante hallazgos).
8. **Docker Build** — construye las imágenes de backend y frontend.

**Fase 2 — CD:**
9. **Deploy to Staging** — despliega el stack con Docker Compose.
10. **Healthcheck Staging** — verifica `/actuator/health` (8080) y el frontend (5173) con reintentos.
11. **Performance Tests (k6)** — carga y SLAs contra Staging (`k6 run performance/api-performance.js`).
12. **E2E Tests (Playwright)** — instala navegadores, limpia datos volátiles y corre la suite E2E.
13. **Promote to Production (Gatekeeper)** — **aprobación manual** (`input`) previa al despliegue a producción.

**Post-acciones:** limpieza de imágenes y caché de Docker, y archivado de reportes/capturas de Playwright.

#### Levantar Jenkins

```bash
cd infrastructure
docker compose -f docker-compose-jenkins.yml up -d
```

Accede en `http://localhost:8082`. La configuración del servidor se gestiona como código (`casc.yaml`).

## Secretos y Variables de Entorno

No hay credenciales hardcodeadas en el repositorio:

- **Local / Docker:** `infrastructure/.env` (ignorado por git) a partir de `infrastructure/.env.example`. Claves principales: `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_TEST_USER_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`.
- **GitHub Actions:** `NVD_API_KEY`, `SNYK_TOKEN`, `SONAR_TOKEN` (y el `GITHUB_TOKEN` automático) en *Settings → Secrets and variables → Actions*.
- **Jenkins:** credenciales `sonar-token`, `snyk-token`, `keycloak-client-secret`, `keycloak-test-user-password`, `keycloak-admin-password` en el almacén de credenciales.
