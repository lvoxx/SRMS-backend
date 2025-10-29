// CI/Spring/Configs/SpringServiceConfig.groovy
// Centralized config for Spring services (e.g., module list, ports, etc.)

def services = [
    'contactor',
    'customer',
    'dashboard',
    'kitchen',
    'notification',
    'order',
    'payment',
    'reporting',
    'warehouse'
]

// Example: validate service exists
def isValidService(String name) {
    return services.contains(name.toLowerCase())
}