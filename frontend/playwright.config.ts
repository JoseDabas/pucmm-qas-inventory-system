import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

// Cargar variables de entorno desde el archivo .env
// dotenv por defecto busca el archivo .env en el directorio actual (frontend)
dotenv.config();

/**
 * Configuración global para la suite de pruebas End-to-End (E2E) utilizando Playwright.
 * 
 * Este archivo centraliza la configuración de ejecución de pruebas, definiendo los
 * navegadores (proyectos) a utilizar, las estrategias de reintentos, el paralelismo
 * y el manejo de evidencias (trazas y capturas de pantalla).
 * 
 * Para cumplir con los requisitos estrictos de validación, se habilitan pruebas
 * responsive ("Desktop Chrome" y "Mobile Safari") y la captura automática de
 * evidencias visuales únicamente cuando una prueba falla.
 */
export default defineConfig({
  // Directorio raíz donde se alojan los archivos de pruebas E2E
  testDir: './tests/e2e',
  
  // Ejecuta todas las pruebas en paralelo para reducir el tiempo de integración
  fullyParallel: true,
  
  // Falla la ejecución en CI si accidentalmente se deja un test.only en el código
  forbidOnly: !!process.env.CI,
  
  // Estrategia de reintentos: 2 intentos en CI para evitar "flaky tests", 0 en desarrollo local
  retries: process.env.CI ? 2 : 0,
  
  // Limita a un solo worker en CI para evitar sobrecarga del runner, indefinido localmente
  workers: process.env.CI ? 1 : undefined,
  
  // Genera un reporte HTML detallado al finalizar la ejecución
  reporter: 'html',
  
  use: {
    /*
     * URL base del frontend. Facilita la navegación relativa (ej. page.goto('/'))
     * Puede ser sobreescrita mediante la variable de entorno FRONTEND_URL.
     */
    baseURL: process.env.FRONTEND_URL || 'http://localhost:5173',

    /* 
     * Requisitos Estrictos: Capturas de evidencia
     * - trace: Conserva las trazas completas de la ejecución solo en caso de fallo.
     * - screenshot: Toma una captura de pantalla final automáticamente si el test falla.
     */
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },

  /* 
   * Requisitos Estrictos: Configuración de proyectos para pruebas responsive.
   * Se definen entornos simulados que ejecutarán la misma suite de pruebas.
   */
  projects: [
    // Proyecto de setup para autenticación, encargado de generar las sesiones (storageState)
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
    },
    // Ejecución en un navegador de escritorio (Chrome)
    {
      name: 'Desktop Chrome',
      use: { ...devices['Desktop Chrome'] },
      // Depende del proyecto 'setup' para asegurar que las credenciales ya existan
      dependencies: ['setup'],
    },
    // Ejecución en un dispositivo móvil simulado (Safari en iPhone 12)
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
      // Depende del proyecto 'setup' para heredar la sesión
      dependencies: ['setup'],
    },
  ],
});
