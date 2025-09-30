// Pipeline Jobs Generator
def serviceConfig = load('CI/config/ServiceConfig.groovy')
def globalConfig = load('CI/config/CIConfig.groovy')

serviceConfig.each { serviceName, config ->
    folder("SRMS/${serviceName}/Build")
    folder("SRMS/${serviceName}/Deploy")
    
    // Build Job
    multibranchPipelineJob("SRMS/${serviceName}/Build/BuildAndDocker") {
        branchSources { github { repoOwner('lvoxx'); repository('SRMS-backend') } }
        factory { workflowBranchProjectFactory { scriptPath('CI/pipelines/BuildAndDockerPipeline.groovy') } }
    }
    
    // Full CI Job
    multibranchPipelineJob("SRMS/${serviceName}/Deploy/FullCI") {
        branchSources { github { repoOwner('lvoxx'); repository('SRMS-backend') } }
        factory { workflowBranchProjectFactory { scriptPath('CI/pipelines/FullCIPipeline.groovy') } }
    }
}