// Seed Job DSL
job('SRMS/SeedJob') {
    description('Master Seed Job for SRMS CI/CD')
    scm {
        git {
            remote { url('https://github.com/lvoxx/SRMS-backend.git') }
            branch('main')
        }
    }
    steps {
        dsl {
            external('CI/jobs/TestJobsGenerator.groovy')
            external('CI/jobs/PipelineJobsGenerator.groovy')
            external('CI/jobs/DashboardGenerator.groovy')
            removeAction('DELETE')
        }
    }
}