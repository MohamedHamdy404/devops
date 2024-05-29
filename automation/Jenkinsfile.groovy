pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // Checkout the repository with submodules
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'SubmoduleOption', recursiveSubmodules: true, trackingSubmodules: true]
                    ],
                    userRemoteConfigs: [[url: 'https://github.com/MohamedHamdy404/repo.git']]
                ])
            }
        }
        stage('Load Pipeline from Submodule') {
            steps {
                script {
                    // Load the Jenkinsfile from the submodule and execute it
                    load 'automation/Jenkinsfile.groovy'
                }
            }
        }
    }
}