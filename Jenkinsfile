pipeline {
    agent any

    environment {
        IMAGE_REPO = 'docker.io/REPLACE_WITH_DOCKERHUB_USERNAME/greatest-three'
        DOCKER_CREDENTIALS_ID = 'dockerhub-creds'
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
                sh 'mvn -B clean verify'
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
                            sh '''
                                set -eu
                                printf '%s' "$DOCKER_PASSWORD" |
                                    docker login --username "$DOCKER_USER" --password-stdin
                                trap 'docker logout >/dev/null 2>&1 || true' EXIT
                                docker build --tag "$IMAGE" .
                                docker push "$IMAGE"
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
                            sh '''
                                set -eu
                                sed \
                                    -e "s|__IMAGE__|$IMAGE|g" \
                                    -e "s|__NAMESPACE__|$K8S_NAMESPACE|g" \
                                    k8s/deployment.yaml | kubectl apply -f -
                                sed \
                                    -e "s|__NAMESPACE__|$K8S_NAMESPACE|g" \
                                    k8s/service.yaml | kubectl apply -f -

                                if ! kubectl rollout status \
                                    deployment/greatest-three \
                                    --namespace "$K8S_NAMESPACE" \
                                    --timeout=180s; then
                                    echo "Deployment rollout failed; collecting Kubernetes diagnostics."
                                    kubectl describe deployment greatest-three \
                                        --namespace "$K8S_NAMESPACE" || true
                                    kubectl get pods \
                                        --namespace "$K8S_NAMESPACE" \
                                        --selector app=greatest-three -o wide || true
                                    kubectl describe pods \
                                        --namespace "$K8S_NAMESPACE" \
                                        --selector app=greatest-three || true
                                    kubectl get events \
                                        --namespace "$K8S_NAMESPACE" \
                                        --sort-by=.metadata.creationTimestamp || true
                                    kubectl logs \
                                        --namespace "$K8S_NAMESPACE" \
                                        --selector app=greatest-three \
                                        --all-containers=true --tail=100 || true
                                    exit 1
                                fi
                            '''
                        }
                    }
                }
            }
        }
    }
}
