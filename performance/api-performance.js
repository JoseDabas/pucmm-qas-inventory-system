import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Script de Pruebas de Rendimiento (k6) para la API de Inventario
 * 
 * Este script implementa las pruebas requeridas.
 * Soporta dos modalidades controladas por la variable de entorno TEST_TYPE:
 * - Load Test (por defecto): Carga normal de 50 VUs.
 * - Stress Test (TEST_TYPE=stress): Estrés agresivo a 200 VUs.
 * 
 * Evalúa umbrales estrictos de errores (<1%) y tiempos de respuesta (p(95) < 500ms).
 */

// ==========================================
// CONFIGURACIÓN DE OPCIONES (Thresholds y Stages)
// ==========================================

const isStressTest = __ENV.TEST_TYPE === 'stress';

export const options = {
    // Configuración de los umbrales de rendimiento (Thresholds)
    thresholds: {
        // Tasa de errores HTTP debe ser menor al 1% (rate < 0.01)
        http_req_failed: ['rate<0.01'],
        // El 95% de las peticiones deben completarse en menos de 500ms
        // Si no se cumple, k6 retorna un código de salida > 0 (fallando el test)
        http_req_duration: ['p(95)<500']
    },

    // Configuración dinámica de las fases (stages) según la modalidad del test
    stages: isStressTest ? [
        // Stress Test (Estrés agresivo)
        { duration: '10s', target: 200 }, // Rampa de subida brusca a 200 VUs en 10 segundos
        { duration: '30s', target: 200 }, // Mantenimiento de 200 VUs durante 30 segundos
        { duration: '10s', target: 0 }    // Rampa de bajada a 0 VUs en 10 segundos
    ] : [
        // Load Test (Carga normal) - Por defecto
        { duration: '30s', target: 50 },  // Rampa de subida a 50 VUs en 30 segundos
        { duration: '1m', target: 50 },   // Mantenimiento de 50 VUs durante 1 minuto
        { duration: '30s', target: 0 }    // Rampa de bajada a 0 VUs en 30 segundos
    ]
};

// ==========================================
// VARIABLES DE ENTORNO Y CONSTANTES
// ==========================================

// Base URL de la API (ej. entorno de Staging)
const API_BASE_URL = __ENV.API_BASE_URL;

// ==========================================
// CONFIGURACIÓN DE AUTENTICACIÓN (OIDC Keycloak)
// ==========================================
// IMPORTANTE: Por requerimientos de seguridad, ninguna credencial sensible
// o variable de entorno está hardcodeada en este script. Estas deben ser inyectadas 
// obligatoriamente mediante el archivo .env o CI/CD.
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL; // OBLIGATORIO en el entorno
const KEYCLOAK_CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID; // OBLIGATORIO en el entorno
const KEYCLOAK_USERNAME = __ENV.KEYCLOAK_USERNAME; // OBLIGATORIO en el entorno
const KEYCLOAK_PASSWORD = __ENV.KEYCLOAK_PASSWORD; // OBLIGATORIO en el entorno
const KEYCLOAK_CLIENT_SECRET = __ENV.KEYCLOAK_CLIENT_SECRET; // OBLIGATORIO en el entorno

// ==========================================
// CICLO DE VIDA: SETUP
// ==========================================

/**
 * Función setup() que se ejecuta una única vez antes de iniciar las VUs (Virtual Users).
 * Realiza una petición al endpoint de tokens OIDC de Keycloak para obtener un access_token.
 * 
 * @returns {Object} Un objeto que contiene el token JWT para que las VUs lo utilicen.
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
        return { token: null }; // Retorna nulo para indicar fallo, aunque las VUs se ejecutarán (y fallarán).
    }

    const token = res.json('access_token');
    return { token: token };
}

// ==========================================
// FLUJO PRINCIPAL (VUs)
// ==========================================

/**
 * Función por defecto que ejecuta el flujo de trabajo de cada Usuario Virtual (VU).
 * Realiza peticiones autenticadas para listar y crear productos, evitando conflictos 409.
 * 
 * @param {Object} data Objeto retornado por la función setup(), contiene el access token.
 */
export default function (data) {
    if (!data || !data.token) {
        // Fallo temprano si el setup() no pudo obtener el token
        console.warn("No se encontró token JWT. Abortando iteración.");
        sleep(1);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${data.token}`,
        'Content-Type': 'application/json'
    };

    // 1. Petición GET al endpoint de listar productos
    let getRes = http.get(`${API_BASE_URL}/products`, { headers });

    check(getRes, {
        'Listar productos responde con status 200 OK': (r) => r.status === 200,
    });

    // 2. Petición POST para crear un producto
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


    // Pausa breve para simular tiempo de reflexión/navegación del usuario real
    sleep(1);
}
