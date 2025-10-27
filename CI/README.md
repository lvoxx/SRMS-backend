# Jenkins CI/CD Infrastructure as Code (IaC) for SRMS-backend

This repository contains the configuration and scripts for setting up a Jenkins CI/CD pipeline for the SRMS-backend project. It automates job creation, builds, tests, and deployments using a structured, extensible approach.

---

## 📂 Repository Structure

- **`config/`**: Stores global and service-specific configurations.
- **`shared/`**: Contains reusable utilities (intended for future Shared Library).
- **`pipelines/`**: Defines pipeline scripts for Jenkins.
- **`jobs/`**: Houses DSL scripts for job generation.
- **`scripts/`**: Includes setup scripts for Jenkins initialization.
- **`templates/`**: Templates for jobs and views.

---

## 🚀 Getting Started

### Prerequisites
Ensure the following tools are configured in Jenkins:
- **Maven**: Version 3
- **JDK**: Version 21
- **SonarQube Scanner**: Optional, for code quality analysis
- **Docker Plugin**: For containerized builds and deployments

### Required Credentials
Configure these credentials in Jenkins:
- `github-token`: GitHub personal access token
- `docker-hub-cred`: Docker Hub credentials
- `sonar-token`: SonarQube token (optional)
- `slack-webhook`: Slack webhook URL (optional)

### Setup Instructions
1. **Initialize Jenkins**:
   - Run the setup script: `./scripts/setup-jenkins.sh`.

2. **Create Seed Job**:
   - In Jenkins UI, create a new Freestyle project:
     - **Name**: `SRMS-SeedJob`
     - **Source Code Management**: Git
     - **Repository URL**: `https://github.com/lvoxx/SRMS-backend.git`
     - **Branch**: `main`
     - **Build Step**: Execute Groovy Script from SCM
     - **Script Path**: `CI/jobs/SeedJob.groovy`

3. **Run Seed Job**:
   - Click **Build Now** on the `SRMS-SeedJob` to generate all jobs and views.

4. **Enable Auto-Trigger**:
   - Configure a GitHub webhook to trigger the `Jenkinsfile` on code push.
   - The `Jenkinsfile` triggers the Seed Job, which regenerates jobs as needed.

5. **Add New Modules**:
   - Update `ServiceConfig.groovy` in the `config/` directory with new module details.
   - Re-run the Seed Job to generate jobs for the new modules.

---

## 🛠️ CI/CD Structure

### Jobs Hierarchy
```
SRMS/
├── SpringServices/
│   ├── Build/
│   │   └── BuildAndDocker
│   ├── Deploy/
│   │   └── FullCI
│   └── Test/
│       ├── contactor
│       ├── customer
│       ├── order
│       ├── payment
│       ├── kitchen
│       ├── warehouse
│       ├── dashboard
│       ├── reporting
│       └── notification
```

### Views
- `SpringServices/Test`: Displays test-related jobs.
- `SpringServices/Build`: Displays build-related jobs.
- `SpringServices/Deploy`: Displays deployment-related jobs.

---

## ⚙️ Key Commands

### Test Commands
Each module runs tests using:
```bash
mvn test -f SpringServices/{module-name}
```

### Build Commands
Build the root project with:
```bash
mvn clean package -f SpringServices/pom.xml
```

### Docker Registry
The default registry is configured as `lvoxx/srms` in `CIConfig.groovy`. Modify as needed.

---

## 🔧 Extensibility

- **Add a New Service**:
  - Update `ServiceConfig.groovy` with the new service details and re-run the Seed Job.
- **Enable SonarQube**:
  - Add a SonarQube token in Jenkins and enable it in the pipeline configuration.
- **Multibranch Support**:
  - Multibranch pipelines are natively supported.

---

## 📋 To-Do List

### High Priority
- [ ] Test Seed Job on the Jenkins server.
- [ ] Verify all generated jobs are correct.
- [ ] Test pipeline execution for all modules.

### Medium Priority
- [ ] Configure Docker credentials and registry.
- [ ] Implement Docker build logic in pipelines.
- [ ] Set up SonarQube integration (optional).

### Low Priority
- [ ] Migrate to Jenkins Configuration as Code (JCasC).
- [ ] Add pipeline-specific Docker build scripts.
- [ ] Implement notification logic for Slack and Email.

---

## 🛡️ Troubleshooting

### If the Seed Job Fails
1. Check Jenkins logs for errors.
2. Verify file paths in configuration files.
3. Validate Groovy syntax in `SeedJob.groovy`.

### If Pipelines Fail
1. Confirm Maven is correctly installed.
2. Ensure project structure matches configured paths.
3. Review `Jenkinsfile` syntax for errors.

---

## 📬 Contact & Support
For issues, check the following:
- **Jenkins Logs**: For system-level errors.
- **Build Logs**: For job-specific issues.
- **Groovy Syntax**: For errors in DSL scripts.

If problems persist, reach out to the repository maintainers or refer to the GitHub issues page.