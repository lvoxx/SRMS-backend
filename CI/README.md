# CI/ Folder: Jenkins CI/CD IaC for SRMS-backend

## Structure
- config/: Global and service configs.
- shared/: Reusable utils (future Shared Lib).
- pipelines/: Pipeline definitions.
- jobs/: DSL generators.
- scripts/: Setup scripts.
- templates/: Job/view templates.

## Usage
1. Setup Jenkins with `setup-jenkins.sh`.
2. Create SeedJob in Jenkins UI, point to `CI/jobs/SeedJob.groovy`.
3. Push to repo → Webhook triggers Jenkinsfile → SeedJob generates everything.
4. Add new modules in `ServiceConfig.groovy`, re-run SeedJob.

## Extensibility
- Add service: Update `ServiceConfig.groovy`.
- Sonar: Add token in Jenkins, enable in pipelines.
- Multibranch: Already supported.