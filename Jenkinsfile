pipeline {
  agent any
  tools {
    ant 'ant-1.9.5'
  }

  parameters {
    // 'Abort' the job is a quick way to bootstrap the 'parameters' from this Jenkinsfile
    choice(name: 'Abort', choices:"Yes\nNo", description: "Do you whish to do a dry run to grab parameters?" )
    choice(name: 'Action', choices:"Show version\nShow targets\nShow help", description: "Select an ant target" )
  }

  stages {
    stage('checkout') {
      steps {
        git(url: 'git@github.com:broadinstitute/GpUnit.git', branch: 'develop')
      }
    }

    // special-case: abort the build early
    //   when a Pipeline job with 'parameters' is run, it overwrites all params in 
    //   the job's config with the parameters declared in this Jenkinsfile
    //
    // use this stage to short-circuit the rest of the build
    stage('refresh-parameters-and-exit') {
      when {
        expression { params.Abort == "Yes" }
      }
      steps {
        script {
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
