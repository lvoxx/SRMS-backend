// Dashboard Generator
// Load and evaluate config files
def loadConfig(path) {
    def file = new File("${WORKSPACE}/${path}")
    return evaluate(file.text)
}

def services = loadConfig('CI/config/ServiceConfig.groovy')

services.keySet().each { serviceName ->
    folder("SRMS/${serviceName}")
    
    // Test View
    listView("SRMS/${serviceName}/Test") {
        jobs {
            regex("SRMS/${serviceName}/Test/.*")
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
    
    // Build View
    listView("SRMS/${serviceName}/Build") {
        jobs {
            regex("SRMS/${serviceName}/Build/.*")
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
    
    // Deploy View
    listView("SRMS/${serviceName}/Deploy") {
        jobs {
            regex("SRMS/${serviceName}/Deploy/.*")
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}