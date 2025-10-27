// Test Jobs Generator
// This file is evaluated by seedJob.groovy
// Variable 'services' is expected to be available from parent context

services.each { serviceName, serviceData ->
    folder("SRMS/${serviceName}/Test")
    
    serviceData.modules.each { moduleName, moduleCfg ->
        // Use multibranchPipelineJob directly, not wrapped in job()
        multibranchPipelineJob("SRMS/${serviceName}/Test/${moduleName}") {
            description("Test job for ${serviceName}/${moduleName}")
            branchSources {
                github {
                    id("${serviceName}-${moduleName}-test-source")
                    repoOwner('lvoxx')
                    repository('SRMS-backend')

                    // If the repo is private, should add credentialsId below
                    // credentialsId(globalConfig.creds.github)
                }
            }
            factory {
                workflowBranchProjectFactory {
                    scriptPath("CI/pipelines/ModuleTestPipeline.groovy")
                }
            }
        }
    }
}