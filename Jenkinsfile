pipeline {
    agent any

    environment {
        IMAGE_REPO = 'docker.io/24mis0585/greatest-three'
        DOCKER_CREDENTIALS_ID = 'dockerhub-cred'
        KUBECONFIG_CREDENTIAL_ID = 'kubeconfig'
        K8S_NAMESPACE = 'greatest-three'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and test') {
            steps {
                bat 'mvn -B clean verify'
            }
        }

        stage('Build and push image') {
            steps {
                script {
                    def image = "${env.IMAGE_REPO}:${env.BUILD_NUMBER}"
                    withEnv(["IMAGE=${image}"]) {
                        withCredentials([usernamePassword(
                            credentialsId: env.DOCKER_CREDENTIALS_ID,
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )]) {
                            powershell '''
                                $ErrorActionPreference = 'Stop'
                                $env:DOCKER_PASSWORD | docker login --username $env:DOCKER_USER --password-stdin
                                if ($LASTEXITCODE -ne 0) { throw 'Docker login failed.' }
                                try {
                                    docker build --tag $env:IMAGE .
                                    if ($LASTEXITCODE -ne 0) { throw 'Docker build failed.' }
                                    docker push $env:IMAGE
                                    if ($LASTEXITCODE -ne 0) { throw 'Docker push failed.' }
                                } finally {
                                    docker logout | Out-Null
                                }
                            '''
                        }
                    }
                }
            }
        }

        stage('Deploy and verify') {
            steps {
                script {
                    def image = "${env.IMAGE_REPO}:${env.BUILD_NUMBER}"
                    withEnv(["IMAGE=${image}"]) {
                        withCredentials([file(
                            credentialsId: env.KUBECONFIG_CREDENTIAL_ID,
                            variable: 'KUBECONFIG'
                        )]) {
                            powershell '''
                                $ErrorActionPreference = 'Stop'
                                $namespace = $env:K8S_NAMESPACE
                                $deployment = (Get-Content 'k8s/deployment.yaml' -Raw).Replace('__IMAGE__', $env:IMAGE).Replace('__NAMESPACE__', $namespace)
                                $service = (Get-Content 'k8s/service.yaml' -Raw).Replace('__NAMESPACE__', $namespace)
                                Set-Content 'deployment.generated.yaml' $deployment -Encoding ASCII
                                Set-Content 'service.generated.yaml' $service -Encoding ASCII

                                try {
                                    kubectl apply -f deployment.generated.yaml
                                    if ($LASTEXITCODE -ne 0) { throw 'Deployment manifest apply failed.' }
                                    kubectl apply -f service.generated.yaml
                                    if ($LASTEXITCODE -ne 0) { throw 'Service manifest apply failed.' }
                                    kubectl rollout status deployment/greatest-three `
                                        --namespace $namespace --timeout=180s
                                    if ($LASTEXITCODE -ne 0) { throw 'Deployment rollout failed.' }
                                } catch {
                                    Write-Host "Deployment failed: $_"
                                    kubectl describe deployment greatest-three --namespace $namespace
                                    kubectl get pods --namespace $namespace --selector app=greatest-three -o wide
                                    kubectl describe pods --namespace $namespace --selector app=greatest-three
                                    kubectl get events --namespace $namespace --sort-by=.metadata.creationTimestamp
                                    kubectl logs --namespace $namespace --selector app=greatest-three `
                                        --all-containers=true --tail=100
                                    exit 1
                                } finally {
                                    Remove-Item 'deployment.generated.yaml', 'service.generated.yaml' `
                                        -ErrorAction SilentlyContinue
                                }
                            '''
                        }
                    }
                }
            }
        }
    }
}
