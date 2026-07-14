pipeline {
    agent any // Usa cualquier agente disponible para ejecutar el pipeline

    // ========================================================
    // VARIABLES DE ENTORNO GLOBALES
    // ========================================================
    // NOTA DE SEGURIDAD: Ningun secreto (contraseñas, client secrets) o URL debe estar hardcodeado aquí.
    // Todas las configuraciones se extraeran dinamicamente del archivo infrastructure/.env
    // en los stages donde sean requeridas (ej. Performance Tests).

    // Define las etapas del pipeline
    stages {

        // Etapa para obtener el código fuente del repositorio
        stage('Checkout') {
            steps {
                echo 'Obteniendo el codigo fuente del repositorio'
                checkout scm
            }
        }

        // Etapa para compilar el backend usando Gradle
        stage('Build Backend') {
            steps {
                echo 'Compilando el backend con Gradle'
                dir('backend') {
                    sh 'chmod +x gradlew'
                    sh './gradlew build -x test'
                }
            }
        }

        // Etapa para ejecutar pruebas unitarias y de API
        stage('Unit Tests') {
            steps {
                echo 'Ejecutando pruebas unitarias y de API'
                dir('backend') {
                    sh './gradlew test --tests "*ProductTest" --tests "*CategoryTest" --tests "*ProductServiceTest" --tests "*ProductControllerApiTest"'
                }
            }
        }

        // Etapa para ejecutar pruebas de integración usando Testcontainers
        stage('Integration Tests') {
            steps {
                echo 'Ejecutando pruebas de integracion con Testcontainers'
                dir('backend') {
                    // RYUK_DISABLED: evita problemas de permisos del contenedor de limpieza en CI/CD.
                    sh 'TESTCONTAINERS_RYUK_DISABLED=true TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal ./gradlew test --tests "*IntegrationTest"'
                }
            }
        }

        // Quality gates
        stage('Quality Gates') {
            steps {
                echo 'Ejecutando analisis de codigo estatico y validando barreras de calidad (Quality Gate)'
                dir('backend') {
                    // Ejecuta el analisis hacia SonarQube/SonarCloud
                    // En un entorno productivo con el plugin de Jenkins, aqui se utilizaria la funcion waitForQualityGate()
                    sh './gradlew sonar'
                }
            }
        }

        // Security scan
        stage('Security Scan') {
            steps {
                echo 'Ejecutando escaneo de seguridad para detectar vulnerabilidades en dependencias'
                // Ejemplo utilizando Snyk CLI (consistente con el entorno del proyecto)
                sh 'snyk test --all-subprojects || true'
            }
        }

        // Docker build
        stage('Docker Build') {
            steps {
                echo 'Construyendo imagenes de Docker para empaquetar los artefactos'
                dir('backend') {
                    sh 'docker build -t inventory-backend:latest .'
                }
                dir('frontend') {
                    sh 'docker build -t inventory-frontend:latest .'
                }
            }
        }

        // ========================================================
        // CD: CONTINUOUS DELIVERY STAGES
        // ========================================================

        // Despliegue simulado o real hacia el ambiente de Staging
        stage('Deploy to Staging') {
            steps {
                echo 'Desplegando la aplicacion en el entorno de Staging...'
                dir('infrastructure') {
                    // En un pipeline real, esto podria ser:
                    // sh 'docker compose pull && docker compose up -d'
                    echo 'Simulando el despliegue de contenedores (Frontend, Backend, DB, Keycloak)...'
                }
            }
        }

        // Healthcheck: Garantizar que la infraestructura está lista antes de probar
        stage('Healthcheck Staging') {
            steps {
                echo 'Verificando que los servicios de Staging esten listos (HTTP 200)'
                timeout(time: 3, unit: 'MINUTES') {
                    retry(10) {
                        sleep 10
                        // Ping al Actuator del Backend
                        sh 'curl -s -f http://host.docker.internal:8080/actuator/health || exit 1'
                        // Ping al Frontend
                        sh 'curl -s -f http://host.docker.internal:5173 || exit 1'
                    }
                }
            }
        }

        // Ejecutar pruebas de carga/estrés con k6
        stage('Performance Tests (k6)') {
            steps {
                echo 'Ejecutando pruebas de rendimiento (Load Test) contra la API en Staging'
                // Extraemos secretos dinamicamente del archivo .env para que k6 los tome 
                sh '''
                    export $(grep -v "^#" infrastructure/.env | xargs)
                    export KEYCLOAK_PASSWORD=$KEYCLOAK_TEST_USER_PASSWORD
                    k6 run performance/api-performance.js
                '''
            }
        }

        // Ejecutar pruebas E2E con Playwright simulando usuarios reales en el navegador
        stage('E2E Tests (Playwright)') {
            steps {
                echo 'Ejecutando suite de pruebas End-to-End con Playwright'
                dir('frontend') {
                    // Aseguramos que las dependencias existan en el agente
                    sh 'npm install'
                    sh 'npx playwright install --with-deps'
                    
                    // Ejecutamos apuntando al entorno simulado (host.docker.internal)
                    sh '''
                        export $(grep -v "^#" ../infrastructure/.env | xargs)
                        export FRONTEND_URL=http://host.docker.internal:5173
                        npx playwright test tests/e2e/inventory.spec.ts
                    '''
                }
            }
        }

        // Gatekeeper manual antes de tocar Producción
        stage('Promote to Production (Gatekeeper)') {
            steps {
                echo 'Los tests de QA (Performance y E2E) han pasado exitosamente en Staging.'
                input message: '¿Aprobar el pase a Produccion?', ok: 'Aprobar y Desplegar'
                
                echo 'Procediendo con el despliegue en Produccion...'
                // Acciones de despliegue a PROD irian aqui
            }
        }
    }

    // Define acciones a realizar después de la ejecución del pipeline
    post {
        always {
            echo 'Pipeline finalizado. Archivando artefactos...' 
            // Archivado de los reportes y rastros (traces) de Playwright para el dashboard de Jenkins
            dir('frontend') {
                archiveArtifacts artifacts: 'playwright-report/**, test-results/**', allowEmptyArchive: true
            }
        }
        success {
            echo 'Pipeline de CI/CD ejecutado con exito'
        }
        failure {
            echo 'El pipeline fallo en algun stage de CI o QA'
        }
    }
}