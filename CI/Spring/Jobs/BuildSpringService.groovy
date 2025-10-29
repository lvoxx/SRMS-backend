// CI/Spring/Jobs/BuildSpringService.groovy
// Build a specific Spring microservice module

createJobFromTemplate(
    name: 'Build-Spring-Service',
    description: 'Builds and archives a Spring Boot microservice JAR',
    stages: [
        {
            """
            stage('Validate Parameters') {
                when { expression { return params.SERVICE_NAME?.trim() } }
                steps {
                    script {
                        env.MODULE_PATH = "${env.CODEBASE_DIR}/${params.SERVICE_NAME}"
                        if (!fileExists(env.MODULE_PATH)) {
                            error "Service directory not found: \${env.MODULE_PATH}"
                        }
                    }
                }
            }
            """
        },
        {
            """
            stage('Maven Build') {
                steps {
                    dir(env.CODEBASE_DIR) {
                        sh "mvn -B -Dmaven.repo.local=.m2 clean package -DskipTests -pl ${params.SERVICE_NAME} -am"
                    }
                }
            }
            """
        },
        {
            """
            stage('Archive Artifact') {
                steps {
                    archiveArtifacts artifacts: "${env.MODULE_PATH}/target/*.jar", allowEmptyArchive: false
                }
            }
            """
        }
    ]
)