import ci.SharedJobDSL

SharedJobDSL.createJobFromTemplate(this, [
    name: 'Test-Spring-Services',
    stages: [
        { "stage('Test') { steps { dir(env.CODEBASE_DIR) { sh 'mvn -B clean test' } } }" },
        { "stage('Report') { steps { junit '${env.CODEBASE_DIR}/**/surefire-reports/*.xml' } }" }
    ]
])