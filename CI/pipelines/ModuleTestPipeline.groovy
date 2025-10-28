// Root Test Pipeline - Runs mvn clean test at the root of SRMS-backend

pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📦 Checking out SRMS-backend repository..."
                checkout scm
            }
        }

        stage('Run Root Tests') {
            steps {
                script {
                    echo "🧪 Running mvn clean test at project root..."
                    dir("${env.WORKSPACE}") {
                        sh 'mvn -B clean test -f pom.xml'
                    }
                }
            }
        }

        stage('Archive Test Results') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }
    }

    post {
        success {
            echo "✅ All root-level tests passed successfully!"
        }
        failure {
            echo "❌ Root-level tests failed. Check logs for details."
        }
        always {
            cleanWs()
        }
    }
}
