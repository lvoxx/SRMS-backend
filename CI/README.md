# Jenkins CI/CD Pipeline for SRMS Backend

## 📋 Overview

This directory contains the complete Jenkins CI/CD pipeline configuration for the Student Record Management System (SRMS) backend services. The pipeline is designed to be modular, scalable, and supports multiple service types (Spring Boot, Python, Go).

## 📁 Directory Structure

```
CI/
├── Spring/                         # Spring Boot specific configurations
│   ├── Jobs/                       # Pipeline job definitions
│   │   ├── BuildSpringService.groovy     # Build Maven projects
│   │   ├── TestSpringService.groovy      # Run unit & integration tests
│   │   └── PushSpringDocker.groovy       # Build & push Docker images
│   └── Configs/                    # Configuration files
│       ├── SpringServiceConfig.groovy    # Spring-specific settings
│       └── CommonEnv.groovy              # Shared environment variables
├── MasterDSL/                      # Main DSL entry points
│   └── Spring-Service-DSL.groovy         # Creates folders and jobs
├── Python/                         # (Future) Python service pipelines
│   ├── Jobs/
│   └── Configs/
├── Go/                             # (Future) Go service pipelines
│   ├── Jobs/
│   └── Configs/
└── README.md                       # This file
```

## 🚀 Getting Started

### Prerequisites

1. **Jenkins Setup**
   - Jenkins 2.400+ with Pipeline DSL plugin installed
   - Docker installed on Jenkins agents
   - Maven 3.9+ configured as a tool
   - Java 21 configured as a tool

2. **Required Jenkins Credentials**
   - `github-token`: GitHub personal access token for repository access
   - `docker-hub-creds`: Docker Hub username/password for image push

3. **Repository Structure**
   ```
   SRMS-backend/
   ├── SpringServices/           # Spring Boot services root
   │   ├── pom.xml              # Parent POM
   │   ├── customers/           # Customer service
   │   │   ├── pom.xml
   │   │   ├── Dockerfile
   │   │   └── src/
   │   ├── orders/              # Order service
   │   │   ├── pom.xml
   │   │   ├── Dockerfile
   │   │   └── src/
   │   └── ...                  # Other services
   └── CI/                      # This directory
   ```

### Installation

1. **Add Credentials to Jenkins**
   ```
   Jenkins → Manage Jenkins → Credentials → Global
   
   Add:
   - Username with password (docker-hub-creds)
   - Secret text (github-token)
   ```

2. **Configure Tools in Jenkins**
   ```
   Jenkins → Manage Jenkins → Tools
   
   Add:
   - JDK: name="Java-21", version=21
   - Maven: name="Maven-3.9", version=3.9
   ```

3. **Load the Master DSL**
   - Create a new Pipeline job in Jenkins
   - Configure SCM: `https://github.com/lvoxx/SRMS-backend.git`
   - Set Script Path: `CI/MasterDSL/Spring-Service-DSL.groovy`
   - Save and run the job

4. **Verify Job Creation**
   After running the master DSL, you should see:
   ```
   SRMS/
   └── SpringServices/
       ├── Build              # Maven build job
       ├── Test               # Test execution job
       ├── Push               # Docker build/push job
       └── FullPipeline       # Complete CI/CD pipeline
   ```

## 🔧 Pipeline Jobs

### 1. Build Job (`SRMS/SpringServices/Build`)

**Purpose**: Compiles and packages all Spring Boot services

**Triggers**: 
- GitHub push events
- Manual execution

**Steps**:
1. Checkout source code
2. Verify Java 21 and Maven 3.9
3. Clean previous build artifacts
4. Compile Java sources
5. Package JARs (skip tests)
6. Archive artifacts

**Output**: JAR files in `SpringServices/*/target/*.jar`

**Duration**: ~5-10 minutes

---

### 2. Test Job (`SRMS/SpringServices/Test`)

**Purpose**: Runs unit and integration tests with coverage analysis

**Triggers**:
- GitHub push events
- After successful build
- Manual execution

**Steps**:
1. Checkout source code
2. Run unit tests (`mvn test`)
3. Run integration tests (`mvn verify`)
4. Generate JaCoCo coverage reports
5. Publish test results and coverage

**Quality Gates**:
- All tests must pass
- Code coverage ≥ 70% (configurable)

**Output**: 
- JUnit test reports
- JaCoCo coverage reports

**Duration**: ~10-15 minutes

---

### 3. Docker Push Job (`SRMS/SpringServices/Push`)

**Purpose**: Builds and pushes Docker images for all services

**Parameters**:
- `DOCKER_TAG`: Image tag (default: `latest`)
- `PUSH_TO_REGISTRY`: Enable/disable push (default: `true`)
- `BUILD_STRATEGY`: `all` or `specific`
- `SPECIFIC_SERVICES`: Comma-separated service names

**Steps**:
1. Build JAR files
2. Discover services with Dockerfiles
3. Build Docker images for each service
4. Test image validity
5. Push to Docker Hub

**Image Naming Convention**:
```
docker.io/lvoxx/<service-name>:<tag>
docker.io/lvoxx/<service-name>:<git-commit-hash>
```

**Examples**:
```bash
lvoxx/customers:latest
lvoxx/customers:a1b2c3d
lvoxx/orders:latest
lvoxx/orders:a1b2c3d
```

**Duration**: ~15-30 minutes (depends on number of services)

---

### 4. Full Pipeline (`SRMS/SpringServices/FullPipeline`)

**Purpose**: Orchestrates complete CI/CD workflow

**Workflow**:
```
Build → Test → Docker Push
  ↓       ↓         ↓
 ✓       ✓         ✓
```

**Triggers**: Manual or scheduled

**Duration**: ~30-50 minutes

## 🛠️ Configuration Files

### SpringServiceConfig.groovy

Contains Spring Boot specific configurations:

```groovy
- Java version: 21
- Maven version: 3.9
- Build goals: clean package
- Test goals: clean test
- Coverage threshold: 70%
- Docker Hub username: lvoxx
- Artifact patterns
```

### CommonEnv.groovy

Shared utilities and environment variables:

```groovy
- Project metadata
- Git utilities
- Version generation
- Notification helpers
- Build summary formatters
```

## 📝 Service Requirements

Each Spring Boot service must have:

1. **pom.xml**: Maven configuration
2. **Dockerfile**: Container definition
3. **src/**: Source code directory

### Example Dockerfile Template

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# Service metadata
ARG SERVICE_NAME
ARG BUILD_DATE
ARG VCS_REF

LABEL maintainer="lvoxx" \
      service.name="${SERVICE_NAME}" \
      build.date="${BUILD_DATE}" \
      vcs.ref="${VCS_REF}"

# Create app directory
WORKDIR /app

# Copy JAR file
COPY target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 🔐 Security Best Practices

1. **Credentials Management**
   - Never hardcode credentials in Groovy files
   - Use Jenkins credentials store
   - Rotate tokens regularly

2. **Docker Images**
   - Use minimal base images (alpine)
   - Scan images for vulnerabilities
   - Sign images before pushing

3. **Access Control**
   - Restrict job execution permissions
   - Use role-based access control (RBAC)
   - Audit pipeline changes

## 🐛 Troubleshooting

### Build Failures

**Issue**: Maven build fails
```bash
# Check Java version
java -version  # Should be 21

# Check Maven settings
mvn -version

# Clean and rebuild
mvn clean install -U
```

**Issue**: Dependency resolution fails
```bash
# Update snapshots
mvn clean install -U

# Clear local repository
rm -rf ~/.m2/repository
```

### Test Failures

**Issue**: Tests fail intermittently
```bash
# Run tests locally
cd SpringServices
mvn clean test

# Check test reports
cat target/surefire-reports/*.txt
```

### Docker Issues

**Issue**: Docker build fails
```bash
# Verify Dockerfile exists
ls SpringServices/*/Dockerfile

# Test Docker build locally
cd SpringServices/customers
docker build -t test:latest .
```

**Issue**: Docker push fails
```bash
# Verify Docker Hub credentials
docker login docker.io

# Check image exists
docker images | grep lvoxx
```

## 📊 Monitoring and Reports

### Build Artifacts
- Location: `SpringServices/*/target/*.jar`
- Retention: 5 builds
- Fingerprinting: Enabled

### Test Reports
- JUnit XML: `SpringServices/*/target/surefire-reports/*.xml`
- JaCoCo HTML: `SpringServices/target/site/jacoco/index.html`
- Retention: 10 builds

### Docker Images
- Registry: `docker.io/lvoxx/*`
- Tags: `latest`, `<git-commit>`
- Retention: Manual cleanup required

## 🔄 Future Enhancements

### Python Services (Planned)
```
CI/Python/
├── Jobs/
│   ├── BuildPythonService.groovy
│   ├── TestPythonService.groovy
│   └── PushPythonDocker.groovy
└── Configs/
    └── PythonServiceConfig.groovy
```

### Go Services (Planned)
```
CI/Go/
├── Jobs/
│   ├── BuildGoService.groovy
│   ├── TestGoService.groovy
│   └── PushGoDocker.groovy
└── Configs/
    └── GoServiceConfig.groovy
```

### Additional Features
- [ ] Kubernetes deployment integration
- [ ] Automated rollback on failure
- [ ] Performance testing stage
- [ ] Security scanning (Trivy, Snyk)
- [ ] Slack/Email notifications
- [ ] Deployment to staging/production environments

## 📚 References

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Job DSL Plugin](https://plugins.jenkins.io/job-dsl/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

## 🤝 Contributing

To add a new pipeline or modify existing ones:

1. Create feature branch
2. Update/add Groovy files in appropriate directory
3. Test locally using Jenkins Pipeline Linter
4. Submit pull request
5. Update this README

## 📞 Support

For issues or questions:
- Open GitHub issue: `https://github.com/lvoxx/SRMS-backend/issues`
- Contact: DevOps Team

---

**Last Updated**: October 29, 2025  
**Version**: 1.0.0  
**Maintained by**: lvoxx