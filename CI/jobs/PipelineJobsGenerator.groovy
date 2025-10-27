// Pipeline Jobs Generator
// This file is evaluated by seedJob.groovy
// Variable 'services' is expected to be available from parent context

services.each { serviceName, serviceData ->
    folder("SRMS/${serviceName}/Build")
    folder("SRMS/${serviceName}/Deploy")
    
    // Build Job
    multibranchPipelineJob("SRMS/${serviceName}/Build/BuildAndDocker") {
        description("Build and Docker job for ${serviceName}")
        branchSources {
            github {
                id("${serviceName}-build-source")
                repoOwner('lvoxx')
                repository('SRMS-backend')

                // For private repo
                // credentialsId(globalConfig.creds.github)
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
                id("${serviceName}-deploy-source")
                repoOwner('lvoxx')
                repository('SRMS-backend')

                // For private repo
                // credentialsId(globalConfig.creds.github)
            }
        }
        factory {
            workflowBranchProjectFactory {
                scriptPath('CI/pipelines/FullCIPipeline.groovy')
            }
        }
    }
}