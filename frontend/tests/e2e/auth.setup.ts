import { test as setup, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

// Rutas locales donde se guardarán las cookies y el almacenamiento local capturado
const authFileAdmin = 'tests/e2e/.auth/admin.json';
const authFileViewer = 'tests/e2e/.auth/viewer.json';

/**
 * Hook global que se ejecuta antes de todas las pruebas de setup.
 * 
 * Se encarga de verificar que la carpeta de almacenamiento de estado (.auth)
 * exista en el sistema de archivos, creándola de manera recursiva si no es el caso.
 * Esto evita errores de escritura al momento de guardar los storageStates.
 */
setup.beforeAll(() => {
  const authDirAdmin = path.dirname(authFileAdmin);
  if (!fs.existsSync(authDirAdmin)) {
    fs.mkdirSync(authDirAdmin, { recursive: true });
  }
  const authDirViewer = path.dirname(authFileViewer);
  if (!fs.existsSync(authDirViewer)) {
    fs.mkdirSync(authDirViewer, { recursive: true });
  }
});

/**
 * Script de configuración de estado: Autenticación como Administrador.
 * 
 * Navega a la aplicación, interactúa con el botón de SSO para ser redirigido
 * al servidor de Keycloak, inyecta las credenciales del usuario Administrador,
 * y luego de un inicio de sesión exitoso, captura y guarda el estado (cookies,
 * localStorage) en un archivo JSON.
 * 
 * Este archivo JSON será posteriormente inyectado en las pruebas que requieran
 * el rol de Administrador.
 */
setup('authenticate as admin', async ({ page }) => {
  // En entornos CI/Docker, redirigir peticiones de localhost:9080 hacia host.docker.internal:9080
  // y forzar el encabezado Host: localhost:9080 para coincidir con la validación JWT de Spring Boot
  await page.route('**/*', async (route) => {
    const url = route.request().url();
    if (url.includes('localhost:9080') || url.includes('host.docker.internal:9080')) {
      const redirectedUrl = url.replace('localhost:9080', 'host.docker.internal:9080');
      const headers = { ...route.request().headers(), 'Host': 'localhost:9080' };
      await route.continue({ url: redirectedUrl, headers });
    } else if (url.includes('localhost:8080')) {
      await route.continue({ url: url.replace('localhost:8080', 'host.docker.internal:8080') });
    } else {
      await route.continue();
    }
  });

  await page.goto('/');

  const adminUser = process.env.KEYCLOAK_ADMIN_USERNAME || 'admin-user';
  const adminPass = process.env.KEYCLOAK_ADMIN_PASSWORD || process.env.KEYCLOAK_TEST_USER_PASSWORD || 'ejemplo12345';

  await page.fill('#username', adminUser);
  await page.fill('#password', adminPass);
  await page.click('button[type="submit"]');

  await expect(page.getByRole('link', { name: 'Inventario', exact: true })).toBeVisible({ timeout: 15000 });

  await page.waitForTimeout(2000);


  const sessionStorageAdmin = await page.evaluate(() => JSON.stringify(window.sessionStorage));
  fs.writeFileSync('tests/e2e/.auth/admin-session.json', sessionStorageAdmin);

  await page.context().storageState({ path: authFileAdmin });
});


/**
 * Script de configuración de estado: Autenticación como Viewer.
 */
setup('authenticate as viewer', async ({ page }) => {
  await page.route('**/*', async (route) => {
    const url = route.request().url();
    if (url.includes('localhost:9080') || url.includes('host.docker.internal:9080')) {
      const redirectedUrl = url.replace('localhost:9080', 'host.docker.internal:9080');
      const headers = { ...route.request().headers(), 'Host': 'localhost:9080' };
      await route.continue({ url: redirectedUrl, headers });
    } else if (url.includes('localhost:8080')) {
      await route.continue({ url: url.replace('localhost:8080', 'host.docker.internal:8080') });
    } else {
      await route.continue();
    }
  });


  await page.goto('/');

  const viewerUser = process.env.KEYCLOAK_VIEWER_USERNAME || 'viewer-user';
  const viewerPass = process.env.KEYCLOAK_VIEWER_PASSWORD || process.env.KEYCLOAK_TEST_USER_PASSWORD || 'ejemplo12345';

  await page.fill('#username', viewerUser);
  await page.fill('#password', viewerPass);
  await page.click('button[type="submit"]');

  await expect(page.getByRole('link', { name: 'Inventario', exact: true })).toBeVisible({ timeout: 15000 });


  await page.waitForTimeout(2000);

  const sessionStorageViewer = await page.evaluate(() => JSON.stringify(window.sessionStorage));
  fs.writeFileSync('tests/e2e/.auth/viewer-session.json', sessionStorageViewer);

  await page.context().storageState({ path: authFileViewer });
});


