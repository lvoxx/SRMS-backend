// Notification Utilities
def notifyBuildStatus(config, status) {
    if ('slack' in config.notify) {
        slackSend(channel: '#ci-channel', message: "Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}")
    }
    if ('email' in config.notify) {
        emailext subject: "Build ${status}", body: "...", to: 'team@email.com'
    }
}