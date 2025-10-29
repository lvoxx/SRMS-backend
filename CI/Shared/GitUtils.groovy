// CI/Shared/GitUtils.groovy
// Git checkout with credentials

def checkoutRepo(job) {
    job.with {
        scm {
            git {
                remote {
                    url(env.REPO_URL)
                    credentials('github-token')
                }
                branch('${BRANCH}')
                extensions {
                    cleanAfterCheckout()
                    pruneStaleRemoteTrackingBranches()
                }
            }
        }
    }
}