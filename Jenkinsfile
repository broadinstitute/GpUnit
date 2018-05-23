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
        sh '''withAnt(installation: \'ant-1.9.5\', jdk: \'jdk8\') {
 ant -p
}
'''
        }
      }
    }
  }