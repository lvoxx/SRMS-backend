// Test Jobs Generator
def serviceConfig = load('CI/config/ServiceConfig.groovy')
def globalConfig = load('CI/config/CIConfig.groovy')

serviceConfig.each { serviceName, config ->
    folder("SRMS/${serviceName}/Test")
    config.modules.each { moduleName, moduleCfg ->
        job("SRMS/${serviceName}/Test/${moduleName}") {
            description("Test for ${moduleName}")
            multibranchPipelineJob {  // Support feature branches
                branchSources {
                    github {
                        repoOwner('lvoxx')
                        repository('SRMS-backend')
                    }
                }
                factory {
                    workflowBranchProjectFactory {
                        scriptPath('CI/pipelines/ModuleTestPipeline.groovy')  // Pass params if needed
                    }
                }
            }
        }
    }
}