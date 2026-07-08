import type { Page } from '@playwright/test';
import type { Product } from '../../src/types/Product';

// Intercepta las llamadas al backend y responde con datos en memoria.
// El listado refleja los cambios de crear/editar/eliminar para poder verificarlos.
export async function mockProductsApi(page: Page, initial: Product[] = []) {
  let products = [...initial];

  await page.route('**/api/v1/products', async (route) => {
    const request = route.request();

    if (request.method() === 'GET') {
      return route.fulfill({ json: { content: products } });
    }

    if (request.method() === 'POST') {
      const body = request.postDataJSON();
      products.push({ id: String(products.length + 1), isActive: true, ...body });
      return route.fulfill({ status: 201, json: {} });
    }

    return route.continue();
  });

  await page.route('**/api/v1/products/*', async (route) => {
    const request = route.request();
    const id = request.url().split('/').pop();

    if (request.method() === 'PUT') {
      const body = request.postDataJSON();
      products = products.map((p) => (p.id === id ? { ...p, ...body } : p));
      return route.fulfill({ status: 200, json: {} });
    }

    if (request.method() === 'DELETE') {
      products = products.filter((p) => p.id !== id);
      return route.fulfill({ status: 204 });
    }

    return route.continue();
  });
}
