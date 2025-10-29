// CI/Spring/Jobs/BuildSpringService.groovy
// Build a specific Spring microservice module

SharedJobDSL.createJobFromTemplate(this, [
    name: 'Build-Spring-Service',
    stages: [
        { "stage('Build') { steps { dir(env.CODEBASE_DIR) { sh \"mvn -B clean package -DskipTests -pl ${params.SERVICE_NAME} -am\" } } }" },
        { "stage('Archive') { steps { archiveArtifacts \"${env.CODEBASE_DIR}/${params.SERVICE_NAME}/target/*.jar\" } }" }
    ]
])