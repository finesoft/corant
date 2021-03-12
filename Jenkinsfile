def REVISION = 'none'
pipeline {
  agent none
  stages {
    stage('Build') {
      agent {
        docker {
          image 'maven:3.6.3-openjdk-11'
          args '-u root -v $HOME/devops/dev/settings-docker.xml:/usr/share/maven/ref/settings.xml -v maven-data:/root/.m2/repository'
        }
      }
      steps {
        script {
          def pom = readMavenPom file: 'pom.xml'
          revision = pom.properties['revision'].replace('-SNAPSHOT','.RELEASE')
          sh "mvn clean deploy -Dmaven.test.skip=true -Drevision=${revision}"
        }
      }
    }
  }
  post {
    success {
      script {
        wrap([$class: 'BuildUser']) {
          slackSend channel: '#jenkins',color: 'good',
            message: "successfully @${env.BUILD_USER}\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} \n revision: ${revision} \n ${env.BUILD_URL}"
        }
      }
    }
    failure {
      script {
        wrap([$class: 'BuildUser']) {
          slackSend channel: '#jenkins',color: '#EA4335',
        	message: "failed!!! @${env.BUILD_USER} @here\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} \n revision: ${revision} \n ${env.BUILD_URL}"
        }
      }
    }
  }
}