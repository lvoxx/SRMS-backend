// CI/Shared/SharedJobDSL.groovy
// TẬP TRUNG TẤT CẢ HÀM DÙNG CHUNG CHO JOB DSL

import javaposse.jobdsl.dsl.DslFactory

class SharedJobDSL {
    static void setupEnvironment(DslFactory dsl) {
        dsl.properties([
            parameters([
                string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch'),
                string(name: 'SERVICE_NAME', defaultValue: '', description: 'Service name (e.g., customers)')
            ])
        ])

        dsl.environmentVariables {
            env('REPO_URL', 'https://github.com/lvoxx/SRMS-backend.git')
            env('CODEBASE_DIR', 'SpringServices')
            env('DOCKERHUB_USER', 'lvoxx')
            credential('GITHUB_TOKEN', 'github-token')
            credential('DOCKER_CREDS', 'docker-hub-creds')
        }
    }

    static void checkoutRepo(DslFactory dsl) {
        dsl.scm {
            git {
                remote {
                    url('${REPO_URL}')
                    credentials('github-token')
                }
                branch('${BRANCH}')
                extensions {
                    cleanAfterCheckout()
                }
            }
        }
    }

    static void createJobFromTemplate(DslFactory dsl, Map config) {
        def name = config.name
        def folder = config.folder ?: 'SRMS/SpringServices'
        def description = config.description ?: ''
        def stages = config.stages ?: []

        dsl.pipelineJob("${folder}/${name}") {
            description description
            definition {
                cps {
                    script("""
                        pipeline {
                            agent any
                            options {
                                timeout(time: 30, unit: 'MINUTES')
                                buildDiscarder(logRotator(numToKeepStr: '10'))
                            }
                            stages {
                                ${stages.collect { it() }.join('\n')}
                            }
                            post { always { cleanWs() } }
                        }
                    """.stripIndent())
                    sandbox(false)
                }
            }
            // Áp dụng chung
            setupEnvironment(delegate)
            checkoutRepo(delegate)
        }
    }
}