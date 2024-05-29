pipeline {
    agent any

    stages {

        stage('Load Pipeline from Submodule') {
            steps {
                script {
                    //Load the Jenkinsfile from the submodule and execute it
                    load 'repo/Jenkinsfile.groovy'
                }
            }
        }
    }
}