def VERSION = 'none'
pipeline {
  agent any
  parameters {
    booleanParam(name: 'releasable', defaultValue: false, description: 'deploy release version')
  }
  stages {
    stage('Build') {
      agent {
        docker {
          image 'maven:3.8.6-openjdk-11'
          args '-u root -v /root/devops/dev/ci-settings.xml:/usr/share/maven/conf/settings.xml -v maven-data:/root/.m2/repository'
        }
      }
      steps {
        script {
          sh "mvn clean deploy -Dmaven.test.skip=true"
          def flattened = readMavenPom file: 'corant-boms/.flattened-pom.xml'
          VERSION = flattened.version
        }
      }
    }
  }
  post {
    success {
      script {
        wrap([$class: 'BuildUser']) {
          slackSend channel: '#jenkins',color: 'good',
            message: """successfully @${env.BUILD_USER}
            ${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} ; version: ${VERSION}
            ${env.BUILD_URL}"""
        }
      }
    }
    failure {
      script {
        wrap([$class: 'BuildUser']) {
          slackSend channel: '#jenkins',color: '#EA4335',
        	message:
        	"""failed!!! @${env.BUILD_USER} @here
        	${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} ; version: ${VERSION}
        	${env.BUILD_URL}"""
        }
      }
    }
  }
}
