pipeline {
  agent any
  tools {
    ant 'ant-1.9.5'
  }
  stages {
    stage('checkout') {
      steps {
        git(url: 'git@github.com:broadinstitute/GpUnit.git', branch: 'develop')
      }
    }
    stage('step1') {
      steps {
        sh 'ant -version'
        sh 'ant -p'
      }
    }
  }
}