// Pipeline Jobs Generator
// Load and evaluate config files
def loadConfig(path) {
    def file = new File("${WORKSPACE}/${path}")
    return evaluate(file.text)
}

def services = loadConfig('CI/config/ServiceConfig.groovy')
def config = loadConfig('CI/config/CIConfig.groovy')

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