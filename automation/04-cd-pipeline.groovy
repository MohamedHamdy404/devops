pipeline {
	agent {
		kubernetes {
			// label 'build-pod'  // all your pods will be named with this prefix, followed by a unique id
			// idleMinutes 10  // how long the pod will live after no jobs have run on it
			yamlFile 'automation/bin/build-pod-deploy.yaml'  // path to the pod definition relative to the root of our project 
			// defaultContainer 'maven'  // define a default container if more than a few stages use it, will default to jnlp container
    	}

  	}

	  parameters {
		choice(name: 'ACTION', choices: ['Upgrade/Install','Uninstall'], description: 'Select needed action')
		//string(defaultValue: '', name: 'TAG_VERSION', trim: true, description: 'ex: preprod-1.0.0-pp-1') 
		gitParameter branch: 'main', branchFilter: 'main', defaultValue: '', description: 'TAG_VERSION', name: 'TAG_VERSION', quickFilterEnabled: false, selectedValue: 'NONE', sortMode: 'DESCENDING', tagFilter: 'beta-*', type: 'PT_TAG', listSize: "0"

	 }

	
	environment{
		CONFIG_FILE = "automation/bin/generic-config.yaml"  	//Generic-specific variables file
        ENV_VARS_FILE = "automation/bin/environment-vars.yaml"	//Environment-specific variables file
		SONAR_TOKEN = credentials('JENKINS_SONAR_TOKEN_USER')
		TAG = "${params.TAG_VERSION}"
		HELM_ACTION = "${params.ACTION}"

	}
	
	
	stages{

		stage('Load Environment and Configs') {
            steps {
                // Load environment-specific variables
                script {
                    def envVars = readYaml(file: ENV_VARS_FILE)
                    envVars.each { microservice, values ->
                        values.each { key, value ->
                            env."${microservice}_${key}" = value
                        }
                    }
					// Load Generic-specific variables
					def configVar = readYaml(file: CONFIG_FILE)
                    configVar.each { conf, values ->
                        values.each { key, value ->
                            env."${conf}_${key}" = value
                        }
                    }
                }
            }
        }
        stage('Variables Declaration') {
            steps {
                script {
				//Variables Declaration
				env.PROJECT_URL = "${env.service_PROJECT_URL}"
				env.REPOSITORY = "${env.service_REPOSITORY}"
				env.REGISTRY = "${env.service_REGISTRY}"
				env.SERVICE_NAME = "${env.service_SERVICE_NAME}"
				env.SONAR_URL = "${env.service_SONAR_URL}"
				env.MY_BRANCH = "${env.preprod_MY_BRANCH}"
				env.PROJECT_KEY= "${env.SERVICE_NAME}-${env.MY_BRANCH}"
				env.IMAGE = "${env.service_IMAGE}"
				env.NAMESPACE="${env.preprod_NAMESPACE}"
				env.KUBECONFIG="${env.preprod_KUBECONFIG}"
				env.VALUE_YAML_FILE="${env.preprod_VALUE_YAML_FILE}"
                }
            }
        }

		stage('deploy image') {
    		steps{
                container('k8stools') {
					script {
						echo 'we check if we will deploy to Preprod Namespace'
							if (env.HELM_ACTION == 'Upgrade/Install'){
							echo 'we will call deployment script for Preprod Namespace'

							pipelineScripts = load "automation/bin/DeployScriptToK8s.groovy"
							pipelineScripts.DeployImage()

							env.DEPLOYMENT_STATUS = "Deployment Done Successfully on Preprod Environment"
							details = """<h1>Jenkins Job Output </h1>
						<p> Build Status:   ${currentBuild.currentResult} </p>
						<p> Jenkins Job Name:   [ ${env.JOB_NAME} ] ==== BUILD_NUMBER:   [ ${env.BUILD_NUMBER} ] </p>
						<p> Jenkins Job Console Log:   <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>
						<p> Deployed Code Artifacts:  [ ${TAG}.jar ] 
						<p> Deployed Container Image:   [ ${TAG} ] 
						<p> Deployment Status:   [ ${DEPLOYMENT_STATUS} ] </p>
						"""
							}
							else {
								echo 'we will not call deployment script for Preprod Namespace as per Trigger option'
							}

                    			
    				}
                }
			}
    	}

		stage('uninstall image') {
    		steps{
                container('k8stools') {
					script {
						echo 'we check if we will uninstall deployment from Preprod Namespace'
							if (env.HELM_ACTION == 'Uninstall'){
							echo 'we will call remove deployment script for Preprod Namespace'

							pipelineScripts = load "automation/bin/DeployScriptToK8s.groovy"
							pipelineScripts.RemoveDeployImage()

							env.DEPLOYMENT_STATUS = "Remove Deployment Done Successfully from Preprod Environment"
							details = """<h1>Jenkins Job Output </h1>
						<p> Build Status:   ${currentBuild.currentResult} </p>
						<p> Jenkins Job Name:   [ ${env.JOB_NAME} ] ==== BUILD_NUMBER:   [ ${env.BUILD_NUMBER} ] </p>
						<p> Jenkins Job Console Log:   <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>
						<p> Deployed Code Artifacts:  [ ${TAG}.jar ] 
						<p> Deployed Container Image:   [ ${TAG} ] 
						<p> Deployment Status:   [ ${DEPLOYMENT_STATUS} ] </p>
						"""
							}
							else {
								echo 'we will not call Remove deployment script for Preprod Namespace as per Trigger option'
							}
                    			
    				}
                }
			}
    	}

	}

	post{
		always {
			
            writeFile (file: 'template.html', text: details )
			archiveArtifacts artifacts: 'template.html'

			buildDescription "Deployed Version: ${TAG}"

			emailext body: '''${SCRIPT, template="groovy-html.template"}''',
			subject: 'CD pipeline',
			to: 'qeema.cicd@qeema.net'

					
        }

	}

	
}	
