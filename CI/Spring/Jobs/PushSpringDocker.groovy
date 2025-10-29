import ci.SharedJobDSL

SharedJobDSL.createJobFromTemplate(this, [
    name: 'Push-Spring-Docker-Image',
    description: 'Build and push Docker image',
    stages: [
        {
            """
            stage('Validate') {
                when { expression { params.SERVICE_NAME } }
                steps {
                    script {
                        def dir = "${env.CODEBASE_DIR}/${params.SERVICE_NAME}"
                        if (!fileExists("${dir}/Dockerfile")) {
                            error "Dockerfile not found in \${dir}"
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
            stage('Build & Push') {
                steps {
                    script {
                        def image = "${env.DOCKERHUB_USER}/${params.SERVICE_NAME.toLowerCase()}:latest"
                        dir("${env.CODEBASE_DIR}/${params.SERVICE_NAME}") {
                            sh "docker build -t \${image} ."
                            sh "docker push \${image}"
                        }
                    }
                }
            }
            """
        }
    ]
])