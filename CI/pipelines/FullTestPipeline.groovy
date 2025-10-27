// Full Test Pipeline
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Full Test Suite') {
            parallel {
                stage('Test Customer') {
                    steps {
                        sh 'mvn test -f SpringServices/customer'
                    }
                }
                stage('Test Order') {
                    steps {
                        sh 'mvn test -f SpringServices/order'
                    }
                }
                stage('Test Payment') {
                    steps {
                        sh 'mvn test -f SpringServices/payment'
                    }
                }
                stage('Test Kitchen') {
                    steps {
                        sh 'mvn test -f SpringServices/kitchen'
                    }
                }
                stage('Test Warehouse') {
                    steps {
                        sh 'mvn test -f SpringServices/warehouse'
                    }
                }
                stage('Test Dashboard') {
                    steps {
                        sh 'mvn test -f SpringServices/dashboard'
                    }
                }
                stage('Test Reporting') {
                    steps {
                        sh 'mvn test -f SpringServices/reporting'
                    }
                }
                stage('Test Notification') {
                    steps {
                        sh 'mvn test -f SpringServices/notification'
                    }
                }
                stage('Test Contactor') {
                    steps {
                        sh 'mvn test -f SpringServices/contactor'
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Full test suite completed with status: ${currentBuild.result}"
            }
        }
    }
}