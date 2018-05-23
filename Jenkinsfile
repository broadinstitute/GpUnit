pipeline {
  agent any
  stages {
    stage('checkout') {
      steps {
        git(url: 'git@github.com:broadinstitute/GpUnit.git', branch: 'develop')
      }
    }
    stage('step1') {
      steps {
        tool 'ant-1.9.5'
        sh 'ant -p'
      }
    }
  }
}