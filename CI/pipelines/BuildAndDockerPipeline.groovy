// Build and Docker Pipeline
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    echo "Building service..."
                    sh 'mvn clean package -f SpringServices/pom.xml -DskipTests'
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    def workspace = env.WORKSPACE
                    echo "Building Docker image..."
                    // Docker build will be handled based on module configuration
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Build completed with status: ${currentBuild.result}"
            }
        }
    }
}