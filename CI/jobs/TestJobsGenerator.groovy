// Test Jobs Generator
// Load and evaluate config files
def loadConfig(path) {
    def file = new File("${WORKSPACE}/${path}")
    return evaluate(file.text)
}

def services = loadConfig('CI/config/ServiceConfig.groovy')
def config = loadConfig('CI/config/CIConfig.groovy')

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