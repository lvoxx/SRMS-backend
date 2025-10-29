// CI/Shared/CommonEnv.groovy
// Global environment & credentials setup

def setupEnvironment(job) {
    job.with {
        properties([
            [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
            parameters([
                string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build'),
                string(name: 'SERVICE_NAME', defaultValue: '', description: 'Microservice name (e.g., customers)')
            ])
        ])

        environmentVariables {
            env('REPO_URL', 'https://github.com/lvoxx/SRMS-backend.git')
            env('CODEBASE_DIR', 'SpringServices')
            env('DOCKERHUB_USER', 'lvoxx') // Replace if dynamic
            credential('GITHUB_TOKEN', 'github-token')
            credential('DOCKER_CREDS', 'docker-hub-creds')
        }
    }
}