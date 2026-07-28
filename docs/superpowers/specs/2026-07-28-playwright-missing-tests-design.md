# Diseño: Tests E2E de Playwright faltantes

**Fecha:** 2026-07-28  
**Rama:** fix/issues-1  
**Enfoque aprobado:** A (añadir sin refactorizar inventory.spec.ts existente)

---

## Contexto

El proyecto tiene 6 spec files de Playwright. Tras analizar las páginas del frontend (`App.tsx`, navigation config), se identificaron 3 páginas sin cobertura alguna y gaps en specs existentes.

---

## Cambios en componentes

### `ReportsPage.tsx`
Añadir `data-testid` a los elementos interactivos:
- `reports-start-date` — input datetime-local fecha inicio
- `reports-end-date` — input datetime-local fecha fin
- `reports-category-select` — select de categoría
- `reports-download-button` — botón Descargar Reporte PDF
- `reports-error-message` — div de error (validación cliente)

### `AlertsPage.tsx`
Añadir `data-testid` a los elementos clave:
- `alerts-table` — tabla de productos críticos
- `alerts-empty-state` — div "¡Todo en orden!" cuando no hay alertas
- `alerts-error-message` — div de error de carga

---

## Nuevos spec files

### `reports.spec.ts`

**Admin describe** (`storageState: admin.json`):
1. Admin ve el botón "Descargar Reporte PDF" visible y habilitado
2. Bloquea descarga si no se seleccionan fechas → muestra `reports-error-message`
3. Bloquea descarga si `startDate > endDate` → muestra `reports-error-message` distinto
4. Snapshot de la página (botón enmascarado para evitar flake por estado loading)

**Viewer describe** (`storageState: viewer.json`):
1. Viewer también puede acceder a `/reportes` (permiso `report:view` compartido)

### `alerts.spec.ts`

**Admin describe** (`storageState: admin.json`):
1. Admin ve la página: o bien `alerts-table` visible, o bien `alerts-empty-state` visible
2. Snapshot de la página

**Viewer describe** (`storageState: viewer.json`):
1. Viewer puede acceder a `/alertas` (mismo permiso `report:view`)

### `audit.spec.ts`

**Admin describe** (`storageState: admin.json`) — permiso `audit:view` es exclusivo de admin:
1. Admin ve las dos tabs: `audit-products-tab` y `audit-movements-tab`
2. Al hacer click en `audit-movements-tab`, se muestra `audit-movements-table`
3. Snapshot de la página de auditoría (tbody enmascarado)

---

## Adiciones a specs existentes

### `inventory.spec.ts` — nuevo describe "Inventario Avanzado - Admin"

Usa `loginAs` (patrón moderno, igual que categories/movements/users):

1. **Editar producto**: `seedProduct` vía API → click `edit-product-button` → cambiar nombre → `product-submit` → verificar nuevo nombre en tabla
2. **Eliminar producto**: `seedProduct` vía API → buscar SKU → click `delete-product-button` → confirmar → verificar que desaparece
3. **Búsqueda por SKU**: `seedProduct` vía API → escribir SKU en `product-search-input` → verificar que aparece solo ese producto

### `movements.spec.ts` — nuevo test en describe "Movimientos - Admin"

1. **Movimiento OUT**: `seedProduct` con `initialQuantity: 100` → abrir formulario → seleccionar producto → tipo OUT → cantidad 5 → guardar → verificar aparece en historial

---

## Restricciones

- Los nuevos tests usan `loginAs` de `helpers/session.ts`, no leen `fs.readFileSync` directamente
- Los tests de `inventory.spec.ts` existentes no se tocan
- Los snapshots de páginas nuevas usan `mask` en tbody para evitar flake por datos dinámicos
- `seedProduct` ya existe en `session.ts`; no necesita helpers nuevos
