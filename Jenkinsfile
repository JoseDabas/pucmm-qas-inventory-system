pipeline {
    agent any // Usa cualquier agente disponible para ejecutar el pipeline

    // ========================================================
    // VARIABLES DE ENTORNO GLOBALES
    // ========================================================
    // NOTA DE SEGURIDAD: Ningún secreto (contraseñas, client secrets) debe estar hardcodeado aquí.
    // La configuración no sensible se extraerá del .env, pero los secretos
    // serán inyectados dinámicamente mediante el gestor de credenciales de Jenkins.

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
                    // Se ha habilitado RYUK (eliminando TESTCONTAINERS_RYUK_DISABLED=true) para que los contenedores se eliminen automáticamente.
                    sh 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal ./gradlew test --tests "*IntegrationTest"'
                }
            }
        }

        // Quality gates
        stage('Quality Gates') {
            environment {
                // Inyectamos el token de SonarQube para autenticar el análisis
                SONAR_TOKEN = credentials('sonar-token')
            }
            steps {
                echo 'Ejecutando analisis de codigo estatico y validando barreras de calidad (Quality Gate)'
                dir('backend') {
                    // El comando debe fallar si no cumple el Quality Gate de SonarQube (se eliminó el || true)
                    sh './gradlew sonar'
                }
            }
        }

        // Security scan
        stage('Security Scan') {
            environment {
                // Inyectamos el token de Snyk para autenticar el escaneo
                SNYK_TOKEN = credentials('snyk-token')
            }
            steps {
                echo 'Ejecutando escaneo de seguridad para detectar vulnerabilidades en dependencias'
                // El pipeline se detendrá si Snyk detecta vulnerabilidades críticas (se eliminó el || true)
                sh 'snyk test --all-subprojects'
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

        // Despliegue hacia el ambiente de Staging
        stage('Deploy to Staging') {
            steps {
                echo 'Desplegando la aplicacion en el entorno de Staging...'
                dir('infrastructure') {
                    sh 'docker compose -f docker-compose.yml pull || true'
                    sh 'docker compose -f docker-compose.yml up -d'
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
            environment {
                // Secrets Management: Inyectando contraseña de forma segura desde Jenkins Credentials
                KEYCLOAK_PASSWORD = credentials('keycloak-test-user-password')
            }
            steps {
                echo 'Ejecutando pruebas de rendimiento (Load Test) contra la API en Staging'
                sh '''
                    if [ ! -f infrastructure/.env ]; then
                        cp infrastructure/.env.example infrastructure/.env
                    fi
                    # Cargamos configuraciones no sensibles del .env (ignoramos secrets para no sobrescribir)
                    export $(grep -v "^#" infrastructure/.env | grep -v -i "password\\|secret" | tr -d '\\r' | xargs)
                    
                    k6 run performance/api-performance.js
                '''
            }
        }

        // Ejecutar pruebas E2E con Playwright simulando usuarios reales en el navegador
        stage('E2E Tests (Playwright)') {
            environment {
                // Secrets Management: Inyectando secretos críticos desde Jenkins Credentials
                VITE_KEYCLOAK_CLIENT_SECRET = credentials('keycloak-client-secret')
                KEYCLOAK_ADMIN_PASSWORD = credentials('keycloak-admin-password')
                KEYCLOAK_TEST_USER_PASSWORD = credentials('keycloak-test-user-password')
            }
            steps {
                echo 'Ejecutando suite de pruebas End-to-End con Playwright'
                dir('frontend') {
                    sh 'npm install'
                    sh 'npx playwright install --with-deps'
                    
                    sh '''
                        # Limpiar BD de los datos sucios generados por las pruebas de rendimiento (k6)
                        docker exec inventory_postgres psql -U inventory_user -d inventory_db -c "TRUNCATE TABLE products CASCADE;"
                        
                        if [ ! -f ../infrastructure/.env ]; then
                            cp ../infrastructure/.env.example ../infrastructure/.env
                        fi
                        # Cargamos configuraciones base desde el .env excluyendo los secrets y passwords
                        export $(grep -v "^#" ../infrastructure/.env | grep -v -i "password\\|secret" | tr -d '\\r' | xargs)
                        
                        export VITE_KEYCLOAK_URL=${KEYCLOAK_URL%/protocol/openid-connect/token}
                        export VITE_KEYCLOAK_CLIENT_ID=$KEYCLOAK_CLIENT_ID
                        export FRONTEND_URL=http://localhost:5173
                        export VITE_API_BASE_URL=${API_BASE_URL%/api/v1}
                        
                        export KEYCLOAK_ADMIN_USERNAME=$KEYCLOAK_USERNAME
                        export KEYCLOAK_VIEWER_USERNAME=$KEYCLOAK_VIEWER_USERNAME
                        export KEYCLOAK_VIEWER_PASSWORD=$KEYCLOAK_TEST_USER_PASSWORD
                        
                        # Iniciar frontend en modo dev para pruebas (localhost es contexto seguro)
                        npm run dev -- --port 5173 &
                        FRONTEND_PID=$!
                        
                        # Esperar a que Vite inicie
                        sleep 3
                        
                        npx playwright test tests/e2e/inventory.spec.ts --update-snapshots || (kill $FRONTEND_PID && exit 1)
                        
                        kill $FRONTEND_PID
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
                dir('infrastructure') {
                    sh 'docker compose -f docker-compose.yml pull || true'
                    sh 'docker compose -f docker-compose.yml up -d'
                }
            }
        }
    }

    // Define acciones a realizar después de la ejecución del pipeline
    post {
        always {
            echo 'Pipeline finalizado. Limpiando imagenes huérfanas (dangling) de Docker...'
            sh 'docker image prune -f || true'
            
            echo 'Archivando artefactos...' 
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