// Full CI Pipeline
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Full Test') {
            steps {
                script {
                    echo "Running full test suite..."
                    sh 'mvn test -f SpringServices/pom.xml'
                }
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
        
        stage('Docker Build & Push') {
            steps {
                script {
                    echo "Building and pushing Docker image..."
                    // Docker build and push logic here
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    echo "Deploying to environment..."
                    // Deploy to Kubernetes or other platform
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Full CI completed with status: ${currentBuild.result}"
            }
        }
    }
}