pipeline {
    agent any // Usa cualquier agente disponible para ejecutar el pipeline

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
                    sh 'TESTCONTAINERS_RYUK_DISABLED=true ./gradlew test --tests "*IntegrationTest"' // Deshabilita Ryuk para evitar problemas de permisos en entornos CI/CD
                }
            }
        }
    }

    // Define acciones a realizar después de la ejecución del pipeline
    post {
        always {
            echo 'Pipeline finalizado' 
        }
        success {
            echo 'Pipeline ejecutado con exito'
        }
        failure {
            echo 'El pipeline fallo'
        }
    }
}