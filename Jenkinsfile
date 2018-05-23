pipeline {
  agent {
    node {
      label 'my-env'
    }

  }
  stages {
    stage('checkout') {
      steps {
        git(url: 'git@github.com:broadinstitute/GpUnit.git', branch: 'develop')
      }
    }
    stage('step1') {
      steps {
        sh 'ant -p'
      }
    }
  }
}