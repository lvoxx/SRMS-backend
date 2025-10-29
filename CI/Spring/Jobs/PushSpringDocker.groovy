// CI/Spring/Jobs/PushSpringDocker.groovy
// Build and push Docker image for a service

createJobFromTemplate(
    name: 'Push-Spring-Docker-Image',
    description: 'Builds and pushes Docker image to Docker Hub',
    stages: [
        {
            """
            stage('Validate Service') {
                when { expression { return params.SERVICE_NAME?.trim() } }
                steps {
                    script {
                        env.SERVICE_DIR = "${env.CODEBASE_DIR}/${params.SERVICE_NAME}"
                        if (!fileExists("${env.SERVICE_DIR}/Dockerfile")) {
                            error "Dockerfile not found in \${env.SERVICE_DIR}"
                        }
                    }
                }
            }
            """
        },
        {
            """
            stage('Docker Login') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh 'echo "\$PASS" | docker login -u "\$USER" --password-stdin'
                    }
                }
            }
            """
        },
        {
            """
            stage('Build & Push Image') {
                steps {
                    script {
                        def image = "${env.DOCKERHUB_USER}/${params.SERVICE_NAME.toLowerCase()}:latest"
                        dir(env.SERVICE_DIR) {
                            sh "docker build -t \${image} ."
                            sh "docker push \${image}"
                        }
                    }
                }
            }
            """
        }
    ]
)