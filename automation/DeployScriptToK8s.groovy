def DeployImage() {
	stage('PrintParameter'){
			sh 'echo "DeployImage function"'
			withCredentials([file(credentialsId: env.KUBECONFIG, variable: 'KUBECRED')]) {
			sh 'helm -n ${NAMESPACE} list  --kubeconfig=$KUBECRED'
			sh 'helm -n ${NAMESPACE} upgrade --install --atomic --cleanup-on-fail ${SERVICE_NAME} --set image.tag=${TAG} ./package-charts/ -f ./package-charts/${VALUE_YAML_FILE} --kubeconfig=$KUBECRED'
			sh 'helm -n ${NAMESPACE} list --kubeconfig=$KUBECRED'
			
			} 
	}
}
def RemoveDeployImage() {
	stage('PrintParameter'){
			sh 'echo "RemoveDeployImage function"'
			withCredentials([file(credentialsId: env.KUBECONFIG, variable: 'KUBECRED')]) {
			
			sh 'helm -n ${NAMESPACE} list  --kubeconfig=$KUBECRED'
			sh 'helm -n ${NAMESPACE} uninstall ${SERVICE_NAME} --kubeconfig=$KUBECRED'
			sh 'helm -n ${NAMESPACE} list --kubeconfig=$KUBECRED'
			
			} 
	}
}



return this;
