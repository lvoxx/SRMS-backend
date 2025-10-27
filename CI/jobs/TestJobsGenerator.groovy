// Test Jobs Generator
// This file is evaluated by seedJob.groovy
// Variable 'services' is expected to be available from parent context

def generateTestJobs(services) {
    services.each { serviceName, serviceData ->
        folder("SRMS/${serviceName}/Test")
        
        serviceData.modules.each { moduleName, moduleCfg ->
            // Use multibranchPipelineJob directly, not wrapped in job()
            multibranchPipelineJob("SRMS/${serviceName}/Test/${moduleName}") {
                description("Test job for ${serviceName}/${moduleName}")
                branchSources {
                    github {
                        repoOwner('lvoxx')
                        repository('SRMS-backend')
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
}

// Call the generator
generateTestJobs(services)