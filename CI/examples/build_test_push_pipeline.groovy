pipeline {
    agent any

    tools {
        maven 'LocalMaven'  // Tên Maven trong Global Tool Configuration
    }

    environment {
        DOCKER_HUB_REPO = 'lvoxx/srms'
        DOCKER_CREDENTIALS_ID = 'docker-hub-creds'
        VERSION = '1.0.0'
        WORKSPACE = 'SpringServices'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/lvoxx/SRMS-backend.git', branch: 'main'
            }
        }

        stage('Build and Test') {
            steps {
                dir(env.WORKSPACE) {
                    sh 'mvn clean package -DskipTests'
                    sh 'mvn test'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    def modules = ['contactor', 'customer', 'dashboard', 'gateway', 'kitchen', 'notification', 'order', 'payment', 'reporting', 'warehouse']
                    for (module in modules) {
                        script {
                            def artifactName = sh(
                                script: "mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout -f ${env.WORKSPACE}/${module}/pom.xml",
                                returnStdout: true
                            ).trim()
                        }
                        sh """
                            echo ">>> Building Docker image for ${module}"
                            cd ${env.WORKSPACE}/${module} && \
                            docker build -t ${env.DOCKER_HUB_REPO}/${module}:${env.VERSION} .
                        """
                    }
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS_ID}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    script {
                        def modules = ['contactor', 'customer', 'dashboard', 'gateway', 'kitchen', 'notification', 'order', 'payment', 'reporting', 'warehouse']
                        for (module in modules) {
                            sh "docker push ${env.DOCKER_HUB_REPO}/${module}:${env.VERSION}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
        }
    }
}