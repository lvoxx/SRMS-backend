// Test Jobs Generator

multibranchPipelineJob("SRMS/Test/RootTest") {
    description("Run mvn clean test from project")

    branchSources {
        github {
            id("srms-test-source")
            repoOwner('lvoxx')
            repository('SRMS-backend')
            // Nếu repo private thì thêm credentialsId(...)
        }
    }

    factory {
        workflowBranchProjectFactory {
            scriptPath("CI/pipelines/ModuleTestPipeline.groovy")
        }
    }
}