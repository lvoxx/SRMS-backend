// CI/Shared/BaseJobTemplate.groovy
// Template to create parameterized pipeline jobs

def createJobFromTemplate(Map config) {
    def jobName = config.name
    def folder = config.folder ?: 'SRMS/SpringServices'
    def description = config.description ?: ''
    def stages = config.stages ?: []

    pipelineJob("${folder}/${jobName}") {
        description description
        keepDependencies(false)
        definition {
            cps {
                script("""
                    pipeline {
                        agent any
                        options {
                            timeout(time: 30, unit: 'MINUTES')
                            buildDiscarder(logRotator(numToKeepStr: '10'))
                        }
                        stages {
                            ${stages.collect { it() }.join('\n')}
                        }
                        post {
                            always {
                                cleanWs()
                            }
                        }
                    }
                """.stripIndent())
                sandbox(false)
            }
        }

        // Apply common setup
        setupEnvironment(delegate)
        checkoutRepo(delegate)
    }
}