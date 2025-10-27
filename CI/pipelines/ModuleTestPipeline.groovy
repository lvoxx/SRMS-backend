// Module Test Pipeline Template
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Test Module') {
            steps {
                script {
                    def workspace = env.WORKSPACE
                    echo "Running tests for module..."
                    // Test execution will be handled per module via configured testCmd
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Test completed with status: ${currentBuild.result}"
            }
        }
    }
}