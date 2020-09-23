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
                    sh "mvn clean deplou -Dmaven.test.skip=true"
                }
            }
        }
    }
    post {
        success {
            slackSend channel: '#gsft',
                    color: 'good',
                    message: "successfully\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME}\n${env.BUILD_URL}"
        }
        failure {
            slackSend channel: '#gsft',
                    color: '#EA4335',
                    message: "failed!!!\n${currentBuild.fullDisplayName} ; branch:${env.BRANCH_NAME} \n${env.BUILD_URL}"
        }
    }
}