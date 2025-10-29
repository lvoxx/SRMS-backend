// CI/Spring/Jobs/TestSpringService.groovy
// Run all unit tests across SpringServices

createJobFromTemplate(
    name: 'Test-Spring-Services',
    description: 'Runs all unit tests for Spring Boot services',
    stages: [
        {
            """
            stage('Run Tests') {
                steps {
                    dir(env.CODEBASE_DIR) {
                        sh 'mvn -B -Dmaven.repo.local=.m2 clean test'
                    }
                }
            }
            """
        },
        {
            """
            stage('Publish Test Results') {
                steps {
                    junit testResults: '${env.CODEBASE_DIR}/**/target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
            """
        }
    ]
)