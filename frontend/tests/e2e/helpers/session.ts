import type { Page, APIRequestContext } from '@playwright/test';
import * as fs from 'fs';

type Role = 'admin' | 'viewer';

// URLs desde el contexto Node (test runner). En CI (Playwright dentro de Docker)
// se usa host.docker.internal; en local, localhost.
const stripApi = (u: string) => u.replace(/\/api\/v1\/?$/, '');
export const backendURL = stripApi(
  process.env.API_BASE_URL || (process.env.CI ? 'http://host.docker.internal:8080' : 'http://localhost:8080')
);
export const keycloakURL = process.env.CI ? 'http://host.docker.internal:9080' : 'http://localhost:9080';

/**
 * Instala la reescritura de red localhost -> host.docker.internal SOLO en CI.
 * En local no reescribe: las peticiones llegan directo a Keycloak/Backend.
 */
export async function installNetworkRewrite(page: Page): Promise<void> {
  await page.route('**/*', async (route) => {
    const url = route.request().url();
    if (!process.env.CI) {
      await route.continue();
      return;
    }
    let newUrl = url;
    if (newUrl.includes('localhost:9080')) newUrl = newUrl.replace('localhost:9080', 'host.docker.internal:9080');
    if (newUrl.includes('localhost:8080')) newUrl = newUrl.replace('localhost:8080', 'host.docker.internal:8080');
    if (newUrl !== url) await route.continue({ url: newUrl });
    else await route.continue();
  });
}

/**
 * Inicia sesión inyectando el sessionStorage capturado en el setup y navega a `path`.
 * Requiere que el describe use test.use({ storageState: '.auth/<role>.json' }).
 */
export async function loginAs(page: Page, role: Role, path = '/dashboard'): Promise<void> {
  await installNetworkRewrite(page);
  await page.goto('/');
  const sessionData = fs.readFileSync(`tests/e2e/.auth/${role}-session.json`, 'utf8');
  await page.evaluate((data) => {
    const parsed = JSON.parse(data) as Record<string, string>;
    for (const key of Object.keys(parsed)) window.sessionStorage.setItem(key, parsed[key]);
  }, sessionData);
  await page.goto(path);
}

/** Lee el access_token del sessionStorage guardado del rol (para sembrar datos vía API). */
export function getAccessToken(role: Role): string {
  const raw = fs.readFileSync(`tests/e2e/.auth/${role}-session.json`, 'utf8');
  const store = JSON.parse(raw) as Record<string, string>;
  const key = Object.keys(store).find((k) => k.startsWith('oidc.user'));
  if (!key) throw new Error(`No hay entrada oidc.user en ${role}-session.json`);
  return (JSON.parse(store[key]) as { access_token: string }).access_token;
}

/** Crea un producto vía API con el token dado. Devuelve el producto creado (incluye name/skuCode). */
export async function seedProduct(request: APIRequestContext, token: string) {
  const sku = `E2E-SEED-${Date.now()}`;
  const res = await request.post(`${backendURL}/api/v1/products`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: {
      name: `Producto Seed ${sku}`,
      skuCode: sku,
      description: 'Producto semilla para E2E',
      category: '',
      price: 10,
      initialQuantity: 100,
      minimumStock: 5,
      isActive: true,
    },
  });
  if (!res.ok()) throw new Error(`seedProduct falló: ${res.status()} ${await res.text()}`);
  return (await res.json()) as { name: string; skuCode: string };
}

/** Token client_credentials del service account inventory-client. */
export async function keycloakAdminToken(request: APIRequestContext): Promise<string> {
  const secret = process.env.VITE_KEYCLOAK_CLIENT_SECRET;
  if (!secret) throw new Error('VITE_KEYCLOAK_CLIENT_SECRET no está definido');
  const res = await request.post(`${keycloakURL}/realms/Inventario/protocol/openid-connect/token`, {
    form: { grant_type: 'client_credentials', client_id: 'inventory-client', client_secret: secret },
  });
  if (!res.ok()) throw new Error(`keycloakAdminToken falló: ${res.status()} ${await res.text()}`);
  return (await res.json()).access_token as string;
}

/** Borra en Keycloak el/los usuario(s) con ese username exacto (best-effort). */
export async function deleteKeycloakUserByUsername(request: APIRequestContext, username: string): Promise<void> {
  const token = await keycloakAdminToken(request);
  const listRes = await request.get(`${keycloakURL}/admin/realms/Inventario/users`, {
    params: { username, exact: 'true' },
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!listRes.ok()) return;
  const users = (await listRes.json()) as Array<{ id: string }>;
  for (const u of users) {
    await request.delete(`${keycloakURL}/admin/realms/Inventario/users/${u.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}
