pipeline {
  agent any
  tools {
    maven 'MAVEN_3_9_15'
    jdk 'JDK_21'
  }

  stages {
    stage ('Compile Project') {
      steps {
        withMaven(maven : 'MAVEN_3_9_15') {
            bat 'mvn clean compile'
        }
      }
    }

    /* stage('Validate Checkstyle') {
      steps {
        withMaven(maven: 'MAVEN_3_9_15') {
          bat 'mvn checkstyle:check'
        }
      }
    }
    */

    stage('Validate Unit Tests') {
      steps {
        withMaven(maven: 'MAVEN_3_9_15') {
          bat 'mvn test'
        }
      }
    }

    stage('Validate Test Coverage') {
      steps {
        withMaven(maven: 'MAVEN_3_9_15') {
          bat 'mvn clean verify jacoco:report'
          bat 'mvn jacoco:check'
        }
      }
    }

    /* stage ('SonarQube Analysis') {
        steps {
            withSonarQubeEnv('sonarLocal') {
                bat 'mvn verify sonar:sonar -Dsonar.projectKey=plantsync_backend'
            }
        }
     }
     */

    stage ('Package Project') {
        steps {
            withMaven(maven : 'MAVEN_3_9_15') {
                bat 'mvn package'
            }
        }
    }
  }
}