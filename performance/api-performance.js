import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Script de Pruebas de Rendimiento y Carga (k6) para la API REST del Sistema de Inventario.
 * 
 * Evalúa la capacidad de procesamiento del backend en el entorno de Staging/CI bajo escenarios
 * de carga normal (50 VUs) o estrés masivo (200 VUs). Integra autenticación OIDC con Keycloak 
 * y verifica los umbrales de SLAs de calidad de servicio (errores < 1%, latencia p(95) < 500ms).
 */

// ==========================================
// CONFIGURACIÓN DE OPCIONES (Thresholds y Stages)
// ==========================================

const isStressTest = __ENV.TEST_TYPE === 'stress';

export const options = {
    // Configuración de los umbrales de rendimiento (Thresholds y SLAs)
    thresholds: {
        // Tasa de errores HTTP debe ser inferior al 1% (rate < 0.01)
        http_req_failed: ['rate<0.01'],
        // El 95% de las peticiones deben responder en menos de 500ms
        http_req_duration: ['p(95)<500']
    },

    // Configuración dinámica de rampas de Usuarios Virtuales (stages)
    stages: isStressTest ? [
        // Stress Test (Estrés agresivo para detectar el punto de ruptura del sistema)
        { duration: '10s', target: 200 }, // Rampa de subida a 200 VUs en 10s
        { duration: '30s', target: 200 }, // Mantenimiento sostenido de 200 VUs durante 30s
        { duration: '10s', target: 0 }    // Rampa de bajada a 0 VUs en 10s
    ] : [
        // Load Test (Carga operacional estándar) - Modo Por Defecto
        { duration: '30s', target: 50 },  // Rampa de subida a 50 VUs en 30s
        { duration: '1m', target: 50 },   // Mantenimiento sostenido de 50 VUs durante 1 minuto
        { duration: '30s', target: 0 }    // Rampa de bajada a 0 VUs en 30s
    ]
};

// ==========================================
// VARIABLES DE ENTORNO Y CONSTANTES DE RED
// ==========================================

// Base URL de la API REST (inyectada desde Jenkins / .env)
const API_BASE_URL = __ENV.API_BASE_URL;

// Credenciales OIDC de Keycloak
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL;
const KEYCLOAK_CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID;
const KEYCLOAK_USERNAME = __ENV.KEYCLOAK_USERNAME;
const KEYCLOAK_PASSWORD = __ENV.KEYCLOAK_PASSWORD;
const KEYCLOAK_CLIENT_SECRET = __ENV.KEYCLOAK_CLIENT_SECRET;

// ==========================================
// CICLO DE VIDA: SETUP DE AUTENTICACIÓN
// ==========================================

/**
 * Hook Setup: Autenticación inicial con Keycloak OIDC.
 * <p>
 * Se ejecuta una única vez antes de iniciar las VUs. Emite una solicitud POST de otorgamiento
 * de credenciales de propietario de recurso (Resource Owner Password Credentials) e inyecta 
 * el encabezado 'Host: localhost:9080' para garantizar coincidencia con el claim 'iss' del JWT.
 * 
 * @returns {Object} Contiene el token JWT emitido por Keycloak para ser compartido entre las VUs.
 */
export function setup() {
    const payload = {
        client_id: KEYCLOAK_CLIENT_ID,
        username: KEYCLOAK_USERNAME,
        password: KEYCLOAK_PASSWORD,
        grant_type: 'password'
    };

    if (KEYCLOAK_CLIENT_SECRET) {
        payload.client_secret = KEYCLOAK_CLIENT_SECRET;
    }

    const headers = {
        'Host': 'localhost:9080',
        'Content-Type': 'application/x-www-form-urlencoded'
    };

    const res = http.post(KEYCLOAK_URL, payload, { headers });

    if (res.status !== 200) {
        console.error(`Fallo al obtener el token OIDC de Keycloak. HTTP ${res.status}: ${res.body}`);
        return { token: null };
    }

    const token = res.json('access_token');
    return { token: token };
}

// ==========================================
// FLUJO PRINCIPAL DE EJECUCIÓN (VUs)
// ==========================================

/**
 * Función Principal de Iteración para Usuarios Virtuales (VUs).
 * <p>
 * Ejecuta transacciones HTTP concurrentes contra la API REST, simulando la navegación real 
 * de lectura (GET /products) y escritura (POST /products) garantizando unicidad de SKU.
 * 
 * @param {Object} data Objeto retornado por setup() con el token de acceso JWT.
 */
export default function (data) {
    if (!data || !data.token) {
        console.warn("No se encontró token JWT válido. Abortando iteración.");
        sleep(1);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${data.token}`,
        'Content-Type': 'application/json'
    };

    // 1. Petición GET al endpoint de consulta de productos
    let getRes = http.get(`${API_BASE_URL}/products`, { headers });

    check(getRes, {
        'Listar productos responde con status 200 OK': (r) => r.status === 200,
    });

    // 2. Petición POST para creación de nuevos productos (con SKU único)
    const randomSalt = Math.floor(Math.random() * 10000000);
    const uniqueCode = `PRD-${__VU}-${__ITER}-${randomSalt}`;

    const newProduct = {
        name: `K6 Product ${uniqueCode}`,
        description: `Creado por script automatizado k6 para testing de rendimiento`,
        skuCode: uniqueCode,
        price: 150.75,
        category: "Performance",
        initialQuantity: 100,
        minimumStock: 10,
        isActive: true
    };

    let postRes = http.post(`${API_BASE_URL}/products`, JSON.stringify(newProduct), { headers });

    check(postRes, {
        'Crear producto responde con status 200 o 201': (r) => r.status === 201 || r.status === 200,
    });

    // Pausa breve para simular tiempo de reflexión/interacción del usuario (Think Time)
    sleep(1);
}

