import { test, expect } from '@playwright/test';
import type { Product } from '../src/types/Product';
import { loginAs } from './support/auth';
import { mockProductsApi } from './support/api';

const sampleProducts: Product[] = [
  {
    id: '1',
    name: 'Laptop',
    skuCode: 'LAP-01',
    description: 'Equipo de oficina',
    category: 'Tecnología',
    price: 1200,
    initialQuantity: 10,
    minimumStock: 2,
    isActive: true,
  },
];

test.beforeEach(async ({ page }) => {
  await loginAs(page);
});

// Pruebas para la gestión de productos
test('lista los productos existentes', async ({ page }) => {
  await mockProductsApi(page, sampleProducts);
  await page.goto('/');

  await expect(page.getByTestId('product-row')).toHaveCount(1);
  await expect(page.getByText('Laptop')).toBeVisible();
});

// Pruebas para creación, edición y eliminación de productos
test('crea un producto nuevo', async ({ page }) => {
  await mockProductsApi(page, []);
  await page.goto('/');

  await page.getByTestId('create-product-button').click();
  await page.getByTestId('product-name').fill('Teclado mecánico');
  await page.getByTestId('product-sku').fill('TEC-01');
  await page.getByTestId('product-price').fill('50');
  await page.getByTestId('product-initial-quantity').fill('20');
  await page.getByTestId('product-minimum-stock').fill('5');
  await page.getByTestId('product-submit').click();

  await expect(page.getByText('Teclado mecánico')).toBeVisible();
});

// Prueba para editar un producto existente
test('edita un producto existente', async ({ page }) => {
  await mockProductsApi(page, sampleProducts);
  await page.goto('/');

  await page.getByTestId('product-row').hover();
  await page.getByTestId('edit-product-button').click();
  await page.getByTestId('product-name').fill('Laptop Pro');
  await page.getByTestId('product-submit').click();

  await expect(page.getByText('Laptop Pro')).toBeVisible();
});

// Prueba para eliminar un producto existente
test('elimina un producto', async ({ page }) => {
  await mockProductsApi(page, sampleProducts);
  await page.goto('/');

  page.on('dialog', (dialog) => dialog.accept());
  await page.getByTestId('product-row').hover();
  await page.getByTestId('delete-product-button').click();

  await expect(page.getByTestId('product-row')).toHaveCount(0);
});
