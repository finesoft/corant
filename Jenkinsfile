def REVISION = 'none'
pipeline {
  agent none
  stages {
    stage('Build') {
      agent {
        docker {
          image 'maven:3.6.3-openjdk-11'
          args '-u root -v $HOME/devops/mgr/settings-docker.xml:/usr/share/maven/ref/settings.xml -v maven-data:/root/.m2/repository'
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
      slackSend channel: '#jenkins',
        color: 'good',
        message: "successfully\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} \n revision: ${revision} \n ${env.BUILD_URL}"
    }
    failure {
      slackSend channel: '#jenkins',
        color: '#EA4335',
        message: "failed!!!\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} \n${env.BUILD_URL}"
    }
  }
}