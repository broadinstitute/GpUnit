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
        //withAnt(installation: 'ant-1.9.5', jdk: 'jdk8') {
        //  sh "ant -version"
        //}
        sh 'ant -version'
      }
    }
}

// withEnv(["JAVA_HOME=${ tool 'jdk-1.8.0_64bits' }", "PATH+MAVEN=${tool 'maven-3.2.1'}/bin:${env.JAVA_HOME}/bin"]) {
//    // Apache Maven related side notes:
//    // --batch-mode : recommended in CI to inform maven to not run in interactive mode (less logs)
//    // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
//    //      Very useful to be quickly sure the selected versions were the ones you think.
//    // -U : force maven to update snapshots each time (default : once an hour, makes no sense in CI).
//    // -Dsurefire.useFile=false : useful in CI. Displays test errors in the logs directly (instead of
//    //                            having to crawl the workspace files to see the cause).
//    sh "mvn --batch-mode -V -U -e clean deploy -Dsurefire.useFile=false"
// }