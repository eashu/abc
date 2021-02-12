@Library('dt') _
import dt.dd.DD
import dt.dtJenkins.BMJenkins

def deploymentDash = new DD(this)
def bmJenkins = new BMJenkins(this)

API_IMAGE = null
REDIS_IMAGE = null
VERSION = "1.1.${env.BRANCH_NAME=='master' ? '0' : '1'}.${env.BUILD_NUMBER}"
DD_DASHBOARD = "microservices"
DD_PROJECT = "dr-leaddriver-api"
DD_PROJECT_NAME = "DR Lead Driver API"


pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent {
        node {
            label 'amazon-docker-x-large'
        }
    }
    stages {
        stage('BUILD DOCKER IMAGES') {
            options {
                timeout(time: 20, unit: 'MINUTES')
            }
            steps {
                script {
                    dir("."){
                        API_IMAGE = docker.build("dtfni-docker.artifactory.coxautoinc.com/dt/leaddriver-api", "-f Dockerfile .")
                        REDIS_IMAGE = docker.build("dtfni-docker.artifactory.coxautoinc.com/dt/leaddriver-redis", "-f Dockerfile.redis .")
                    }
                }
            }
        }
        stage('RUN TESTS') {
            options {
                timeout(time: 20, unit: 'MINUTES')
            }
            steps {
                script {
                    try {
                        sh "docker-compose -f docker-compose-tests.yml run --rm --service-ports tests"
                    }
                    catch (error) {
                        throw error
                    }
                    finally {
                        sh "docker-compose down"
                    }
                    docker.image('calthorpeanalytics/pre-commit').inside("--entrypoint= -u root") {
                        sh "pre-commit run --all-files"
                    }
                }
            }
        }
        stage('PUSH DOCKER IMAGES') {
            options {
                timeout(time: 20, unit: 'MINUTES')
            }
            when {
                expression { env.BRANCH_NAME ==~ /master|\d\.\d{1,2}/ }
            }
            steps {
                script {
                    VERSION = "${VERSION}-${env.GIT_COMMIT[0..6]}"

                    docker.withRegistry("http://dtfni-docker.artifactory.coxautoinc.com", 'cai-artifactory') {
                        API_IMAGE.push(VERSION)
                        API_IMAGE.push('latest')
                        REDIS_IMAGE.push(VERSION)
                        REDIS_IMAGE.push('latest')
                    }
                }
            }
        }
        stage('Publish Octopus Release') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            when {
                expression { env.BRANCH_NAME ==~ /master|\d\.\d{1,2}/ }
            }
            steps {
                script {
                    bmJenkins.triggerJob("OCTOPUS_CREATE_RELEASE", [OCTOPUS_PROJECT: "leaddriver", BUILD_VERSION: "${VERSION}"])
                }
            }
        }
        stage('Report to Deployment-dash') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            when {
                expression { env.BRANCH_NAME ==~ /master|\d\.\d{1,2}/ }
            }
            steps {
                script {
                    deploymentDash.publishBuild(DD_DASHBOARD, DD_PROJECT, DD_PROJECT_NAME, "${VERSION}", DD_PROJECT_NAME)
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
