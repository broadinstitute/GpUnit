pipeline {
  agent any
  tools {
    ant 'ant-1.9.5'
  }
  parameters {
    choice(name: 'Invoke_Parameters', choices:"Yes\nNo", description: "Do you whish to do a dry run to grab parameters?" )
    choice(name: 'Action', choices:"Show version\nShow targets\nShow help", description: "Select an ant target" )
  }
  stages {
    stage('checkout') {
      steps {
        git(url: 'git@github.com:broadinstitute/GpUnit.git', branch: 'develop')
      }
    }
    stage('invoke-parameters') {
      steps {
        script {
          if ("${params.Invoke_Parameters}" == "Yes") {
            currentBuild.result = 'ABORTED'
            error('DRY RUN COMPLETED. JOB PARAMETERIZED.')
          }
        }
    }
    stage('show-version') {
      when {
        expression { params.Action == 'Show version' }
      }
      steps {
        sh 'ant -version'
      }
    }
    stage('show-targets') {
      when {
        expression { params.Action == 'Show targets' }
      }
      steps {
        sh 'ant -p'
      }
    }
    stage('show-help') {
      when {
        expression { params.Action == 'Show help' }
      }
      steps {
        sh 'ant help'
      }
    }
  }
}