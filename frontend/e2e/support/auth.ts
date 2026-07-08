import type { Page } from '@playwright/test';

// Simula una sesión válida de Keycloak inyectando el usuario OIDC en sessionStorage
// antes de que React monte. Así evitamos pasar por el login real en los tests.
const STORAGE_KEY = 'oidc.user:http://localhost:9080/realms/Inventario:inventory-client';

export async function loginAs(page: Page, username = 'admin') {
  const user = {
    access_token: 'fake-token',
    token_type: 'Bearer',
    scope: 'openid profile email',
    profile: { preferred_username: username, name: username },
    expires_at: Math.floor(Date.now() / 1000) + 3600,
  };

  await page.addInitScript(
    ([key, value]) => sessionStorage.setItem(key, value),
    [STORAGE_KEY, JSON.stringify(user)] as const,
  );
}
