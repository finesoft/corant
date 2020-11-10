pipeline {
  agent none
  stages {
    stage('Build') {
      agent {
        docker {
          image 'maven:3.6.2-jdk-8'
          args '-u root -v $HOME/devops/mgr/settings-docker.xml:/usr/share/maven/ref/settings.xml -v maven-data:/root/.m2/repository'
        }
      }
      steps {
        script {
          sh "mvn clean deploy -Dmaven.test.skip=true"
        }
      }
    }
  }
  post {
    success {
      slackSend channel: '#jenkins',
        color: 'good',
        message: "successfully\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME}\n ${env.BUILD_URL}"
    }
    failure {
      slackSend channel: '#jenkins',
        color: '#EA4335',
        message: "failed!!!\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} \n${env.BUILD_URL}"
    }
  }
}