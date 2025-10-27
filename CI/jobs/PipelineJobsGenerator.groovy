// Pipeline Jobs Generator
// This file is evaluated by seedJob.groovy
// Variable 'services' is expected to be available from parent context

def generatePipelineJobs(services) {
    services.each { serviceName, serviceData ->
        folder("SRMS/${serviceName}/Build")
        folder("SRMS/${serviceName}/Deploy")
        
        // Build Job
        multibranchPipelineJob("SRMS/${serviceName}/Build/BuildAndDocker") {
            description("Build and Docker job for ${serviceName}")
            branchSources {
                github {
                    repoOwner('lvoxx')
                    repository('SRMS-backend')
                }
            }
            factory {
                workflowBranchProjectFactory {
                    scriptPath('CI/pipelines/BuildAndDockerPipeline.groovy')
                }
            }
        }
        
        // Full CI Job
        multibranchPipelineJob("SRMS/${serviceName}/Deploy/FullCI") {
            description("Full CI/CD pipeline for ${serviceName}")
            branchSources {
                github {
                    repoOwner('lvoxx')
                    repository('SRMS-backend')
                }
            }
            factory {
                workflowBranchProjectFactory {
                    scriptPath('CI/pipelines/FullCIPipeline.groovy')
                }
            }
        }
    }
}

// Call the generator
generatePipelineJobs(services)