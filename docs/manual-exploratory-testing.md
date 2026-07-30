# Manual de Exploratory Testing — Sistema de Gestión de Inventarios (PUCMM QAS)

> Documento de evidencias de pruebas exploratorias (manuales) **ejecutadas contra la aplicación en vivo**.
> Complementa a la batería automatizada (JUnit, MockMvc, Testcontainers, Playwright, k6) explorando el
> comportamiento del producto **desde la perspectiva del usuario** y buscando defectos no cubiertos por los
> tests automáticos.
>
> **Estado:** Sesiones ejecutadas y verificadas contra el stack real (Docker Compose: PostgreSQL + Keycloak
> + backend Spring Boot + frontend) el **2026-07-30**. Cada resultado incluye el **código HTTP observado**
> como evidencia.

## Tabla de Contenidos

- [1. Introducción](#1-introducción)
- [2. Alcance y entorno de pruebas](#2-alcance-y-entorno-de-pruebas)
- [3. Estrategia (Session-Based Test Management)](#3-estrategia-session-based-test-management)
- [4. Exploratory Charters](#4-exploratory-charters)
- [5. Escenarios explorados (bitácora de sesiones con evidencia)](#5-escenarios-explorados-bitácora-de-sesiones-con-evidencia)
- [6. Hallazgos y bugs (con veredicto de verificación)](#6-hallazgos-y-bugs-con-veredicto-de-verificación)
- [7. Resumen de cobertura y métricas](#7-resumen-de-cobertura-y-métricas)
- [8. Conclusiones](#8-conclusiones)

---

## 1. Introducción

El **Exploratory Testing** es una técnica de prueba manual en la que el tester diseña y ejecuta los casos
de forma simultánea, aprendiendo del sistema mientras lo prueba. A diferencia de las pruebas guionadas,
el foco está en **descubrir** comportamientos inesperados, casos borde y defectos que los tests
automatizados —por su naturaleza determinista— no suelen detectar.

Este manual documenta las sesiones exploratorias sobre el Sistema de Gestión de Inventarios, cubriendo:

- **Exploratory Charters** — misiones acotadas que guían cada sesión.
- **Escenarios explorados** — bitácora de lo que realmente se hizo, con la evidencia HTTP obtenida.
- **Hallazgos y bugs** — cada hipótesis de defecto se **verificó ejecutando la app**, y se registra su
  veredicto (confirmado / rechazado / pendiente) con la evidencia correspondiente.

> **Nota metodológica importante:** este manual es honesto respecto al resultado. La mayoría de las
> hipótesis de defecto planteadas en el diseño de las sesiones **fueron rechazadas al verificarlas** —el
> sistema valida correctamente. Documentar una hipótesis refutada es evidencia de QA tan válida como
> encontrar un bug: demuestra que el control fue probado.

## 2. Alcance y entorno de pruebas

| Ítem | Detalle |
|------|---------|
| Aplicación bajo prueba | Frontend SPA (`http://localhost:5173`) + API REST (`http://localhost:8080/api/v1`) |
| Autenticación | Keycloak 26 — realm `Inventario`, cliente `inventory-client` (Direct Access Grants) |
| Base de datos | PostgreSQL 16 (`inventory_db`) — contiene datos semilla + residuos de corridas E2E previas |
| Documentación de API | Swagger UI (`http://localhost:8080/swagger-ui.html`) |
| Orquestación | `docker compose up` en `infrastructure/` (stack completo levantado) |
| Herramientas de apoyo | cURL + tokens JWT reales de Keycloak, decodificación de claims del JWT, Swagger UI |
| Fecha de ejecución | 2026-07-30 |

### Verificación del entorno (evidencia de arranque)

```
GET  http://localhost:8080/actuator/health        -> 200  {"status":"UP"}
GET  http://localhost:9080/realms/Inventario       -> 200  (realm activo)
POST .../token (admin-user / viewer-user)          -> 200  (JWT emitido)
```

### Usuarios utilizados

| Usuario | Rol documentado | Permisos **reales observados en el entorno** | Uso |
|---------|-----------------|-----------------------------------------------|-----|
| `admin-user` | ADMIN | Los 7 permisos ✅ (coincide) | Flujos de escritura completos |
| `viewer-user` | VIEWER (3 permisos) | ⚠️ **Los 7 permisos** (NO coincide — ver **FND-001**) | Inicialmente para RBAC negativo |
| `viewer_fresh_*` | VIEWER (creado durante la sesión) | `product:view`, `stock:view`, `report:view` ✅ | RBAC negativo correcto |

> Durante la sesión se descubrió que el usuario semilla `viewer-user` **ya no es de solo lectura** en el
> entorno en ejecución: su token incluye los 7 permisos. Para poder verificar RBAC de forma fiable se
> **creó un usuario VIEWER nuevo** vía la API de administración. Ver **FND-001**.

### Módulos dentro del alcance

Productos, Categorías, Movimientos de Stock, Dashboard, Alertas de Stock Crítico, Reportes PDF,
Usuarios y Roles, Auditoría, y los controles transversales de Autenticación/Autorización (JWT + RBAC).

## 3. Estrategia (Session-Based Test Management)

Se aplicó **SBTM (Session-Based Test Management)**:

- Cada sesión se organiza alrededor de un **charter** (misión).
- Cada sesión produce notas, evidencia (HTTP) y un veredicto por escenario.
- Clasificación del tiempo: **T** (Test design/execution), **B** (Bug investigation), **S** (Setup).

### Heurísticas y oráculos aplicados

- **SFDIPOT**: Structure, Function, Data, Interfaces, Platform, Operations, Time.
- **CRUD**: Create, Read, Update, Delete sobre cada entidad.
- **Boundaries**: valores límite (0, negativos, máximos, cadenas en blanco, Unicode/acentos).
- **RBAC / Least Privilege**: el usuario solo puede hacer lo que su permiso concede (UI + backend).
- **CRUD ↔ Audit**: cada cambio debe quedar reflejado en la auditoría de Envers.

### Escala de severidad

| Severidad | Criterio |
|-----------|----------|
| **Crítica** | Pérdida de datos, bypass de seguridad/autorización, caída del sistema |
| **Alta** | Función principal rota, cálculo incorrecto de inventario, error 500 no controlado |
| **Media** | Comportamiento incorrecto con workaround, validación faltante, inconsistencia de datos |
| **Baja** | Usabilidad, mensajes poco claros, discrepancia documental, detalles cosméticos |

---

## 4. Exploratory Charters

> Formato: **Explore** (objetivo) **con** (recursos) **para descubrir** (información buscada).

| ID | Charter | Área | Prioridad |
|----|---------|------|-----------|
| **CH-01** | Explorar la **creación/edición de productos** para descubrir validaciones faltantes en SKU, precio, cantidad, stock mínimo y nombre en blanco. | Productos | Alta |
| **CH-02** | Explorar el **cálculo de stock desde el ledger** y la **validación de stock negativo** y cantidad cero. | Movimientos | Alta |
| **CH-03** | Explorar la **eliminación de categorías con/sin productos** y la unicidad de nombre. | Categorías | Media |
| **CH-04** | Explorar el **control de acceso (RBAC)** con un usuario de solo lectura para descubrir fugas de autorización. | Seguridad | Crítica |
| **CH-05** | Explorar la **generación de reportes PDF por rango de fechas** con rangos válidos, invertidos y formatos inválidos. | Reportes | Media |
| **CH-06** | Explorar la **asignación y cambio de rol de usuario** para descubrir si los permisos se **reemplazan** o se **acumulan**. | Usuarios | Alta |
| **CH-07** | Explorar **búsqueda, paginación e inyección** en productos/movimientos. | Transversal | Media |
| **CH-08** | Explorar la **consistencia de la auditoría (Envers)** en CREATE/UPDATE/DELETE. | Auditoría | Media |
| **CH-09** | Explorar el **manejo del token JWT** (ausente, malformado). | Seguridad | Alta |

---

## 5. Escenarios explorados (bitácora de sesiones con evidencia)

> Leyenda de veredicto: ✅ correcto (comportamiento esperado) · ❌ defecto · ⚠️ hallazgo/observación.

### Sesión S-01 — Validaciones de productos (CH-01)

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| 1 | Crear producto válido (`category:"Hogar"`, precio/cantidad ≥ 0) | `POST /products` → **201** (`stockActual: 20`) | ✅ |
| 2 | Crear con **SKU duplicado** | `POST /products` → **409** | ✅ rechazado |
| 3 | Crear con **precio negativo** (`-10`) | `POST /products` → **400** | ✅ rechazado |
| 4 | Crear con **nombre en blanco** (`"   "`) | `POST /products` → **400** | ✅ rechazado (`@NotBlank` + `@Pattern`) |
| 5 | Crear con body **UTF-8 inválido** (acento mal codificado) | `POST /products` → **400** "Formato de petición inválido" | ✅ manejado |

**Notas:** el DTO exige `@NotBlank` + patrón con al menos un alfanumérico para `name` y `skuCode`, y
`@Min(0)`/`@Max` en precio y cantidades. El nombre en blanco (hipótesis BUG-004) **se rechaza**.

---

### Sesión S-02 — Ledger de movimientos (CH-02)

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| 1 | Registrar **OUT** válido (5 sobre stock 20) | `POST /stock-movements` → **201** | ✅ |
| 2 | Registrar **OUT** que deja negativo (9999 sobre 20) | `POST /stock-movements` → **400** | ✅ rechazado (regla de negocio) |
| 3 | Registrar movimiento con **cantidad = 0** | `POST /stock-movements` → **400** (`@Min(1)`) | ✅ rechazado |
| 4 | Body de validación devuelve *problem+json* claro | `{"title":"Bad Request","status":400,"detail":"...validación."}` | ✅ |

**Notas:** el `StockMovementRequestDTO` fija `@Min(value=1)`; la hipótesis de aceptar cantidad 0 (BUG-002)
**queda refutada**.

---

### Sesión S-03 — Categorías (CH-03)

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| 1 | Crear categoría con nombre único | `POST /categories` → **201** | ✅ |
| 2 | Crear categoría con **nombre duplicado** | `POST /categories` → **409** | ✅ rechazado |
| 3 | Eliminar categoría **con producto asociado** | `DELETE /categories/{id}` → **409** | ✅ bloqueado |
| 4 | Eliminar el producto asociado | `DELETE /products/{id}` → **204** | ✅ |

---

### Sesión S-04 — RBAC / autorización (CH-04)

**Parte A — usuario semilla `viewer-user` (token con 7 permisos):**

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| A1 | Decodificar claims del JWT de `viewer-user` | `realm_access.roles` = **los 7 permisos** | ⚠️ **FND-001** |
| A2 | `viewer-user` GET `/admin/users` | **200** (porque sí tiene `user:manage`) | ⚠️ consecuencia de FND-001 |

**Parte B — usuario VIEWER creado en la sesión (permisos correctos: view×3):**

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| B1 | `GET /products` | **200** | ✅ |
| B2 | `GET /dashboard/metrics` | **200** | ✅ (autenticado basta) |
| B3 | `POST /products` (requiere `product:manage`) | **403** | ✅ denegado |
| B4 | `GET /admin/users` (requiere `user:manage`) | **403** | ✅ denegado |
| B5 | `GET /audit/products` (requiere `audit:view`) | **403** | ✅ denegado |

**Conclusión de la sesión:** el **RBAC del backend funciona correctamente**. Con un usuario de permisos
correctos, la autorización por scope (`@PreAuthorize`) devuelve **403** en cada operación no permitida y
**200** en las permitidas. No se encontró ningún bypass. El único hallazgo es de **datos/entorno**
(FND-001): el usuario demo `viewer-user` quedó sobre-privilegiado por corridas E2E previas.

---

### Sesión S-05 — Reportes PDF (CH-05)

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| 1 | Reporte con **date-time** válido (`...T00:00:00`), rango normal | `GET /reports/movements` → **200** (PDF) | ✅ |
| 2 | Reporte con **rango invertido** (`startDate > endDate`, date-time) | **409** | ✅ rechazado (regla de negocio) |
| 3 | Reporte con **fecha simple** (`2026-01-01`, sin hora) | **409** | ⚠️ **FND-002** (exige date-time; README dice "rango de fechas") |

**Notas:** la hipótesis de que el rango invertido pasa sin validar (BUG-001) **queda refutada**: devuelve
**409**. Sí se detectó una discrepancia de formato/documentación y de código de estado (409 vs 400) → FND-002.

---

### Sesión S-06 — Cambio de rol de usuario (CH-06)

Sobre el usuario VIEWER creado, se aplicaron cambios de rol consecutivos decodificando el JWT tras cada uno:

| # | Transición | Permisos en el JWT tras el cambio | Veredicto |
|---|------------|-----------------------------------|-----------|
| 1 | inicial **VIEWER** | `product:view, report:view, stock:view` | ✅ |
| 2 | → **INVENTORY_MANAGER** | `product:view, product:manage, stock:view, stock:manage, report:view` | ✅ (añade manage) |
| 3 | → **VIEWER** (degradación) | `product:view, report:view, stock:view` | ✅ **revoca manage** |
| 4 | → **ADMIN** | los 7 permisos | ✅ |
| 5 | → **AUDITOR** | `audit:view, report:view` | ✅ **revoca los otros 5** |

**Conclusión:** el cambio de rol **reemplaza** el conjunto de permisos exactamente (concede los nuevos y
**revoca** los que ya no corresponden). La hipótesis de acumulación de privilegios (BUG-006) **queda
refutada** — no hay escalada de privilegios al degradar el rol.

---

### Sesión S-07 — Búsqueda / paginación (CH-07)

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| 1 | `?search=` con payload de **inyección** (`'; DROP TABLE products;--`) | **200**, sin error 500 | ✅ (JPA parametrizado) |
| 2 | `?page=9999` (fuera de rango) | **200** (página vacía) | ✅ |

---

### Sesión S-08 — Auditoría Envers (CH-08)

Se creó, editó y eliminó un producto, y se consultó `/audit/products`:

| # | Escenario | Evidencia | Veredicto |
|---|-----------|-----------|-----------|
| 1 | Ciclo CREATE → UPDATE → DELETE sobre un producto | Revisiones halladas: **`CREATED`, `UPDATED`, `DELETED`** | ✅ |
| 2 | El registro incluye autor del cambio | Campo `modifiedBy` presente en cada revisión | ✅ |
| 3 | La eliminación (hard delete) queda auditada | Aparece revisión `DELETED` pese al borrado físico | ✅ |

---

### Sesión S-09 — Token JWT (CH-09)

| # | Escenario | Evidencia (HTTP) | Veredicto |
|---|-----------|------------------|-----------|
| 1 | Petición **sin token** | `GET /products` → **401** | ✅ |
| 2 | Token **malformado** (`Bearer abc.def.ghi`) | **401**, sin stacktrace | ✅ |

---

## 6. Hallazgos y bugs (con veredicto de verificación)

Cada hipótesis de defecto planteada al diseñar las sesiones se verificó ejecutando la aplicación. Se
reportan **2 hallazgos reales** (FND-001, FND-002) y se documenta el **rechazo** de las hipótesis
refutadas (evidencia de que el control fue probado y funciona).

### FND-001 — El usuario demo `viewer-user` quedó sobre-privilegiado (fuga de higiene de datos E2E)

| Campo | Detalle |
|-------|---------|
| **Severidad** | Media |
| **Estado** | Confirmado |
| **Tipo** | Datos / entorno (no es un defecto de código de la app) |
| **Charter / Sesión** | CH-04 / S-04 |

**Descripción:** el usuario semilla `viewer-user`, documentado como **solo lectura** (3 permisos:
`product:view`, `stock:view`, `report:view`), en el entorno en ejecución tiene **los 7 permisos**. Al
decodificar su JWT, `realm_access.roles` incluye `product:manage`, `stock:manage`, `user:manage` y
`audit:view`, que no le corresponden.

**Evidencia:**
```
JWT de viewer-user -> realm_access.roles:
  [product:view, product:manage, stock:view, stock:manage, user:manage, audit:view, report:view]
GET /api/v1/admin/users con ese token -> 200 (debería ser 403 para un VIEWER)
```

**Causa raíz probable:** las pruebas E2E de Playwright (`users.spec.ts`) cambian roles de usuarios y **no
restauran** el estado de `viewer-user` al terminar; al compartir el mismo Keycloak/realm, el usuario demo
queda con permisos elevados. La BD muestra abundantes residuos de corridas E2E (categorías/productos
`E2E-*`), lo que respalda esta hipótesis.

**Impacto:** en un entorno demo/staging compartido, la cuenta que se presenta como "de solo consulta"
puede **escribir y administrar**. Riesgo de demostración incorrecta de RBAC y de confianza en un usuario
que ya no es de solo lectura.

**Recomendación:**
1. Que las suites E2E **restauren** el rol de los usuarios semilla en el `afterAll`/teardown, o que operen
   **solo sobre usuarios que ellas mismas crean** (nunca sobre `admin-user`/`viewer-user`).
2. Reimportar el realm (`docker compose down -v && up`) para devolver a `viewer-user` a VIEWER, o corregir
   su rol vía `PUT /api/v1/admin/users/{id}/role` con `VIEWER`.
3. Considerar un realm/instancia de Keycloak **dedicado a E2E**, aislado del entorno de demostración.

> **Verificación cruzada:** creando un usuario VIEWER nuevo (permisos correctos), el RBAC devolvió **403**
> en todas las operaciones no permitidas. Es decir, **el motor de autorización es correcto**; el problema
> es exclusivamente el estado del usuario demo.

---

### FND-002 — El reporte exige `date-time` ISO y devuelve 409 (no 400) ante fecha inválida

| Campo | Detalle |
|-------|---------|
| **Severidad** | Baja |
| **Estado** | Confirmado |
| **Charter / Sesión** | CH-05 / S-05 |
| **Módulo** | Reportes (`GET /api/v1/reports/movements`) |

**Descripción (dos matices):**
1. **Formato:** el endpoint requiere un `date-time` ISO-8601 completo (`2026-01-01T00:00:00`). Una **fecha
   simple** (`2026-01-01`) produce **409**. El README lo describe como reporte "por **rango de fechas**
   (ISO-8601)", lo que puede inducir a enviar fechas sin hora.
2. **Código de estado:** ante una fecha mal formada o un rango inválido, el controller responde **409
   Conflict** en lugar del semánticamente correcto **400 Bad Request**. Es una decisión deliberada
   documentada en el código (para satisfacer la validación de contrato de Schemathesis), pero un cliente
   REST esperaría 400 por entrada malformada.

**Evidencia:**
```
GET /reports/movements?startDate=2026-01-01&endDate=2026-12-31              -> 409  (fecha simple)
GET /reports/movements?startDate=2026-01-01T00:00:00&endDate=2026-12-31T23:59:59 -> 200 (date-time OK)
GET /reports/movements?startDate=2026-12-31T00:00:00&endDate=2026-01-01T00:00:00 -> 409 (rango invertido, correctamente rechazado)
```

**Impacto:** bajo. Funcionalmente el reporte funciona; el riesgo es de **experiencia de integración** (un
consumidor de la API puede confundirse por el formato exigido y por el 409 en vez de 400).

**Recomendación:** aclarar en el README/Swagger que el parámetro es `date-time` (no solo fecha), y evaluar
devolver **400** para entradas malformadas reservando **409** solo para conflictos de negocio reales.

---

### Hipótesis de defecto verificadas y **refutadas**

Estas hipótesis se plantearon durante el diseño de las sesiones y se **descartaron al ejecutarlas**. Se
documentan porque constituyen evidencia de que el control fue probado.

| ID hipótesis | Descripción | Resultado real | Veredicto |
|--------------|-------------|----------------|-----------|
| H-01 (ex BUG-001) | Reporte acepta rango de fechas invertido sin validar | Rango invertido → **409** (rechazado) | ❌ Refutada |
| H-02 (ex BUG-002) | Se permite movimiento con cantidad = 0 | `quantity=0` → **400** (`@Min(1)`) | ❌ Refutada |
| H-03 (ex BUG-004) | Nombre de producto en blanco se acepta | `name="   "` → **400** (`@NotBlank`+patrón) | ❌ Refutada |
| H-04 (ex BUG-006) | Cambiar de rol **acumula** permisos del rol anterior | Cambio de rol **reemplaza** (revoca los sobrantes) | ❌ Refutada |

### Ítems de UX **pendientes de verificación por sesión de UI**

Los siguientes puntos son de experiencia de usuario en el frontend y **no se verificaron en esta sesión**
(requieren interacción real de navegador con temporización; la verificación de esta ronda fue a nivel de
API/backend con cURL). Se dejan como candidatos abiertos, **no confirmados**.

| ID | Descripción | Cómo verificar |
|----|-------------|----------------|
| UX-01 | Posible "flash" de una ruta protegida del SPA antes de redirigir a un usuario sin permiso. | Sesión Playwright/manual navegando por URL a una ruta sin permiso y observando el render. |
| UX-02 | Claridad del mensaje al usuario cuando la API devuelve 409 al borrar una categoría con productos. | Intentar el borrado desde `/categorias` en el navegador y observar el toast/mensaje. |
| UX-03 | Aviso al usuario ante expiración del token antes de redirigir a login. | Dejar expirar el JWT y disparar una acción; observar si hay aviso o pérdida silenciosa de datos. |

---

## 7. Resumen de cobertura y métricas

### Charters ejecutados

| Charter | Sesión | Estado | Resultado |
|---------|--------|--------|-----------|
| CH-01 Productos — validaciones | S-01 | ✅ Ejecutado | Validaciones correctas |
| CH-02 Movimientos — ledger/negativos/cero | S-02 | ✅ Ejecutado | Validaciones correctas |
| CH-03 Categorías — 409 y unicidad | S-03 | ✅ Ejecutado | Correcto |
| CH-04 RBAC / autorización | S-04 | ✅ Ejecutado | RBAC correcto + **FND-001** |
| CH-05 Reportes PDF | S-05 | ✅ Ejecutado | Correcto + **FND-002** |
| CH-06 Cambio de rol | S-06 | ✅ Ejecutado | Reemplazo correcto de permisos |
| CH-07 Búsqueda/paginación | S-07 | ✅ Ejecutado | Correcto |
| CH-08 Auditoría (Envers) | S-08 | ✅ Ejecutado | CREATE/UPDATE/DELETE correctos |
| CH-09 Token JWT | S-09 | ✅ Ejecutado | 401 correcto |

### Resultado de las hipótesis de defecto

| Categoría | Cantidad | IDs |
|-----------|----------|-----|
| Hallazgos confirmados | 2 | FND-001 (Media), FND-002 (Baja) |
| Hipótesis refutadas (control OK) | 4 | H-01, H-02, H-03, H-04 |
| Pendientes de verificación por UI | 3 | UX-01, UX-02, UX-03 |

### Escenarios verificados con evidencia HTTP (resumen)

| Control | Resultado observado |
|---------|---------------------|
| Autenticación sin token / token malformado | **401** ✅ |
| Autorización por scope (usuario VIEWER correcto) | **403** en no permitido, **200** en permitido ✅ |
| Validación de dominio (precio<0, cantidad=0, nombre en blanco, stock negativo) | **400** ✅ |
| Unicidad (SKU y nombre de categoría duplicados) | **409** ✅ |
| Integridad referencial (borrar categoría con productos) | **409** ✅ |
| Reemplazo de permisos al cambiar rol | Exacto, sin acumulación ✅ |
| Auditoría inmutable (Envers) | `CREATED/UPDATED/DELETED` + `modifiedBy` ✅ |
| Robustez de búsqueda (inyección, página fuera de rango) | **200**, sin 500 ✅ |

## 8. Conclusiones

Las sesiones exploratorias se **ejecutaron contra la aplicación en vivo** y confirmaron que la **lógica
central de inventario y los controles de seguridad son sólidos**: el stock se recalcula desde un ledger
inmutable, la autorización por scope (`@PreAuthorize`) es correcta (403/200 según permiso), el cambio de
rol reemplaza los permisos sin permitir escalada, las validaciones de dominio rechazan entradas inválidas
con **400**, la unicidad y la integridad referencial responden con **409**, y la auditoría de Envers
registra CREATE/UPDATE/DELETE con autor.

De las hipótesis de defecto planteadas, **las 4 verificables a nivel de API fueron refutadas** (el sistema
se comporta correctamente). Se confirmaron **2 hallazgos**:

1. **FND-001 (Media):** el usuario demo `viewer-user` quedó **sobre-privilegiado** por falta de limpieza en
   las suites E2E; conviene aislar los datos de E2E y restaurar/recrear los usuarios semilla.
2. **FND-002 (Baja):** el endpoint de reportes exige `date-time` (no fecha simple) y usa **409** en vez de
   **400** para entradas malformadas; conviene aclararlo en la documentación y afinar el código de estado.

Quedan **3 ítems de UX pendientes** de una sesión de verificación por navegador (UX-01..03).

### Recomendaciones priorizadas

1. **FND-001** — aislar el entorno/realm de E2E y añadir teardown que restaure los usuarios semilla.
2. **FND-002** — documentar el formato `date-time` y evaluar `400` para entradas malformadas.
3. **UX-01..03** — ejecutar una sesión exploratoria de UI (Playwright/manual) para confirmar o descartar.

> Este manual es un artefacto vivo: cada nueva sesión exploratoria debe añadir su charter, su bitácora con
> evidencia y su veredicto, manteniendo la trazabilidad Charter → Sesión → Hallazgo.
