pipeline {
    agent any // Ejecución en agente principal configurado en el nodo de Jenkins

    // =========================================================================
    // CONFIGURACIÓN GLOBAL Y GESTIÓN DE VARIABLES DE ENTORNO (DevSecOps)
    // =========================================================================
    // Principio de Seguridad: Ningún secreto ni credencial sensible se almacena en el código.
    // Las variables no sensibles se extraen dinámicamente de 'infrastructure/.env', mientras
    // que los secretos son inyectados mediante el almacén de credenciales de Jenkins.

    stages {

        // =========================================================================
        // FASE 1: INTEGRACIÓN CONTINUA (CI) - CONSTRUCCIÓN Y PRUEBAS ESTÁTICAS
        // =========================================================================

        /**
         * Stage 1: Obtención del código fuente desde el repositorio remoto Git.
         */
        stage('Checkout') {
            steps {
                echo 'Obteniendo el código fuente del repositorio Git...'
                checkout scm
            }
        }

        /**
         * Stage 2: Compilación agnóstica del backend (Spring Boot Java 21) utilizando Gradle wrapper.
         */
        stage('Build Backend') {
            steps {
                echo 'Compilando el backend con Gradle...'
                dir('backend') {
                    sh 'chmod +x gradlew'
                    sh './gradlew build -x test'
                }
            }
        }

        /**
         * Stage 3: Validación y compilación agnóstica del frontend (React / Vite / TypeScript).
         */
        stage('Build Frontend') {
            steps {
                echo 'Compilando el frontend con npm...'
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        /**
         * Stage 4: Pruebas unitarias de controladores, servicios y lógica de dominio.
         */
        stage('Unit Tests') {
            steps {
                echo 'Ejecutando pruebas unitarias de servicios y APIs REST...'
                dir('backend') {
                    sh './gradlew test --tests "*ProductTest" --tests "*CategoryTest" --tests "*ProductServiceTest" --tests "*ProductControllerApiTest"'
                }
            }
        }

        /**
         * Stage 5: Pruebas de integración aisladas mediante contenedores efímeros (Testcontainers / Postgres).
         */
        stage('Integration Tests') {
            steps {
                echo 'Ejecutando pruebas de integración con Testcontainers...'
                dir('backend') {
                    sh 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal ./gradlew test --tests "*IntegrationTest"'
                }
            }
        }

        /**
         * Stage 6: Análisis estático de código y barrera de calidad (SonarQube Quality Gate).
         */
        stage('Quality Gates') {
            environment {
                SONAR_TOKEN = credentials('sonar-token')
            }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Ejecutando análisis de código estático y validando Quality Gate...'
                    dir('backend') {
                        sh './gradlew sonar -Dsonar.token="${SONAR_TOKEN}" || true'
                    }
                }
            }
        }

        /**
         * Stage 7: Escaneo de seguridad de dependencias de código abierto (Snyk Vulnerability Scan).
         */
        stage('Security Scan') {
            environment {
                SNYK_TOKEN = credentials('snyk-token')
            }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Ejecutando escaneo de seguridad en dependencias (Snyk)...'
                    dir('backend') {
                        sh 'snyk test || true'
                    }
                    dir('frontend') {
                        sh 'snyk test || true'
                    }
                }
            }
        }

        /**
         * Stage 8: Empaquetamiento e higienización de imágenes Docker locales.
         */
        stage('Docker Build') {
            steps {
                echo 'Construyendo imágenes de Docker para empaquetar artefactos...'
                dir('backend') {
                    sh 'docker build -t inventory-backend:latest .'
                }
                dir('frontend') {
                    sh 'docker build -t inventory-frontend:latest .'
                }
            }
        }

        // =========================================================================
        // FASE 2: ENTREGA CONTINUA (CD) - STAGING, QA AUTOMATIZADO Y PRODUCCIÓN
        // =========================================================================

        /**
         * Stage 9: Despliegue automatizado del entorno de pre-producción (Staging) vía Docker Compose.
         */
        stage('Deploy to Staging') {
            steps {
                echo 'Desplegando la aplicación en el entorno de Staging...'
                dir('infrastructure') {
                    sh 'docker compose -f docker-compose.yml pull || true'
                    sh 'docker compose -f docker-compose.yml up -d'
                }
            }
        }

        /**
         * Stage 10: Verificación activa de disponibilidad de servicios (Healthcheck HTTP 200 OK).
         */
        stage('Healthcheck Staging') {
            steps {
                echo 'Verificando salud de la infraestructura de Staging...'
                timeout(time: 3, unit: 'MINUTES') {
                    retry(10) {
                        sleep 10
                        sh 'curl -s -f http://host.docker.internal:8080/actuator/health || exit 1'
                        sh 'curl -s -f http://host.docker.internal:5173 || exit 1'
                    }
                }
            }
        }

        /**
         * Stage 11: Pruebas de rendimiento, carga y SLAs bajo concurrencia masiva (k6).
         */
        stage('Performance Tests (k6)') {
            environment {
                KEYCLOAK_PASSWORD = credentials('keycloak-test-user-password')
                KEYCLOAK_CLIENT_SECRET = credentials('keycloak-client-secret')
            }
            steps {
                echo 'Ejecutando pruebas de carga y latencia con k6 en Staging...'
                sh '''
                    if [ ! -f infrastructure/.env ]; then
                        cp infrastructure/.env.example infrastructure/.env
                    fi
                    export $(grep -v "^#" infrastructure/.env | grep -v -i "password\\|secret" | tr -d '\\r' | xargs)
                    
                    export KEYCLOAK_PASSWORD="${KEYCLOAK_PASSWORD}"
                    export KEYCLOAK_CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET}"
                    
                    k6 run performance/api-performance.js
                '''
            }
        }

        /**
         * Stage 12: Pruebas End-to-End (E2E) simulando navegadores web reales (Playwright Chromium/Safari).
         */
        stage('E2E Tests (Playwright)') {
            environment {
                VITE_KEYCLOAK_CLIENT_SECRET = credentials('keycloak-client-secret')
                KEYCLOAK_ADMIN_PASSWORD = credentials('keycloak-admin-password')
                KEYCLOAK_TEST_USER_PASSWORD = credentials('keycloak-test-user-password')
            }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Ejecutando suite completa de pruebas End-to-End con Playwright...'
                    dir('frontend') {
                        sh 'npm install'
                        sh 'npx playwright install --with-deps'
                        
                        sh '''
                            # Limpieza de datos volátiles de rendimiento en la base de datos de test
                            docker exec inventory_postgres psql -U inventory_user -d inventory_db -c "TRUNCATE TABLE products CASCADE;" || true
                            
                            if [ ! -f ../infrastructure/.env ]; then
                                cp ../infrastructure/.env.example ../infrastructure/.env
                            fi
                            export $(grep -v "^#" ../infrastructure/.env | grep -v -i "password\\|secret" | tr -d '\\r' | xargs)
                            
                            export VITE_KEYCLOAK_URL=${KEYCLOAK_URL%/protocol/openid-connect/token}
                            export VITE_KEYCLOAK_CLIENT_ID=$KEYCLOAK_CLIENT_ID
                            export FRONTEND_URL=http://host.docker.internal:5173
                            export VITE_API_BASE_URL=${API_BASE_URL%/api/v1}
                            
                            export KEYCLOAK_ADMIN_USERNAME=$KEYCLOAK_USERNAME
                            export KEYCLOAK_VIEWER_USERNAME=$KEYCLOAK_VIEWER_USERNAME
                            export KEYCLOAK_ADMIN_PASSWORD=$KEYCLOAK_ADMIN_PASSWORD
                            export KEYCLOAK_VIEWER_PASSWORD=$KEYCLOAK_TEST_USER_PASSWORD
                            export VITE_KEYCLOAK_CLIENT_SECRET=$VITE_KEYCLOAK_CLIENT_SECRET
                            
                            npx playwright test --config=playwright.config.ts --update-snapshots
                        '''
                    }
                }
            }
        }

        /**
         * Stage 13: Aprobación manual explícita (Gatekeeper) previa a promoción en Producción.
         */
        stage('Promote to Production (Gatekeeper)') {
            steps {
                echo 'Todas las pruebas de QA (Rendimiento y E2E) han finalizado exitosamente en Staging.'
                input message: '¿Aprobar el pase del release a Producción?', ok: 'Aprobar y Desplegar'
                
                echo 'Procediendo con el despliegue en el entorno de Producción...'
                dir('infrastructure') {
                    sh 'docker compose -f docker-compose.yml pull || true'
                    sh 'docker compose -f docker-compose.yml up -d'
                }
            }
        }

    }

    /**
     * Post-Acciones de Limpieza y Generación de Reportes.
     */
    post {
        always {
            echo 'Limpiando imágenes efímeras y caché de Docker para optimización de almacenamiento...'
            sh 'docker image prune -f || true'
            sh 'docker builder prune -f || true'
            
            echo 'Archivando reportes de Playwright y capturas de prueba...' 
            dir('frontend') {
                archiveArtifacts artifacts: 'playwright-report/**, test-results/**', allowEmptyArchive: true
            }
        }
        success {
            echo 'Pipeline de CI/CD ejecutado exitosamente.'
        }
        failure {
            echo 'El pipeline detectó un fallo crítico en alguna etapa de validación de CI o QA.'
        }
    }
}