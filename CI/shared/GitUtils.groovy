// Git Utilities
def checkoutRepo() {
    checkout scm
}

def getChangedModules() {
    // Logic to detect changed modules based on git diff (for optimization)
    sh 'git diff --name-only HEAD~1'
    // Return list of changed paths
    return []  // Placeholder
}