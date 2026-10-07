pipeline {

    agent any

    parameters {

        string(
            name: 'BRANCH',
            defaultValue: 'main',
            description: 'Git branch to build and deploy'
        )
    }

    environment {


        AWS_REGION = 'ap-south-1'

        AWS_ACCOUNT_ID = '890615325308'

        ECR_REPOSITORY = 'demoproject'

        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

        ECR_IMAGE = "${ECR_REGISTRY}/${ECR_REPOSITORY}"

        EC2_HOST = '15.252.73.248'

        EC2_USER = 'ubuntu'

        SSH_CREDENTIALS = 'sshkey'

        CONTAINER_NAME = 'demoproject'

        HOST_PORT = '8082'

        CONTAINER_PORT = '8080'

        SONARQUBE_ENV = 'SonarQube'
    }


    stages {


        stage('Show Selected Branch') {

            steps {

                echo "Selected Git Branch: ${params.BRANCH}"
            }
        }


        stage('Checkout') {

            steps {

                git(
                    branch: "${params.BRANCH}",
                    credentialsId: 'git-credentials',
                    url: 'https://github.com/nayana000/demoproject.git'
                )

                sh '''
                    echo "Checked out branch:"
                    git branch --show-current

                    echo "Commit:"
                    git rev-parse HEAD
                '''
            }
        }


        stage('SonarQube Analysis') {

            steps {

                withSonarQubeEnv("${SONARQUBE_ENV}") {

                    sh '''
                        echo "Running SonarQube analysis..."

                        mvn clean verify sonar:sonar \
                          -Dsonar.projectKey=demoproject \
                          -Dsonar.projectName=demoproject

                    '''
                }
            }
        }

        stage('Quality Gate') {

            steps {

                timeout(
                    time: 5,
                    unit: 'MINUTES'
                ) {

                    waitForQualityGate(
                        abortPipeline: true
                    )
                }
            }
        }


        stage('Docker Build') {

            steps {

                sh '''
                    echo "Building Docker image"

                    docker build \
                        -t ${ECR_IMAGE}:${BUILD_NUMBER} .

                    docker tag \
                        ${ECR_IMAGE}:${BUILD_NUMBER} \
                        ${ECR_IMAGE}:latest

                    echo "Docker images created:"
                    docker images | grep demoproject
                '''
            }
        }



        stage('ECR Login') {

            steps {

                sh '''
                    echo "Logging in to Amazon ECR..."

                    aws ecr get-login-password \
                        --region ${AWS_REGION} | \
                    docker login \
                        --username AWS \
                        --password-stdin ${ECR_REGISTRY}
                '''
            }
        }


        stage('Push Image to ECR') {

            steps {

                sh '''
                    echo "Pushing image to ECR..."

                    docker push ${ECR_IMAGE}:${BUILD_NUMBER}

                    docker push ${ECR_IMAGE}:latest

                    echo "Images pushed successfully."
                '''
            }
        }



        stage('Deploy') {

            steps {

                sshagent(credentials: ["${SSH_CREDENTIALS}"]) {

                    sh '''

                        echo "Connecting to EC2-2"

                        ssh \
                            -o StrictHostKeyChecking=no \
                            ${EC2_USER}@${EC2_HOST} \
                            "hostname"

                        echo "Connected successfully."

                        echo "Deploying application"


                        ssh \
                            -o StrictHostKeyChecking=no \
                            ${EC2_USER}@${EC2_HOST} \
                            "
                                set -e

                                echo 'Logging in to ECR...'

                                aws ecr get-login-password \
                                    --region ${AWS_REGION} | \
                                docker login \
                                    --username AWS \
                                    --password-stdin ${ECR_REGISTRY}


                                echo 'Pulling Docker image...'

                                docker pull \
                                    ${ECR_IMAGE}:${BUILD_NUMBER}


                                echo 'Stopping old container...'

                                docker stop \
                                    ${CONTAINER_NAME} || true


                                echo 'Removing old container...'

                                docker rm \
                                    ${CONTAINER_NAME} || true


                                echo 'Starting new container...'

                                docker run -d \
                                    --name ${CONTAINER_NAME} \
                                    --restart unless-stopped \
                                    -p ${HOST_PORT}:${CONTAINER_PORT} \
                                    ${ECR_IMAGE}:${BUILD_NUMBER}


                                echo 'Deployment completed.'

                                echo 'Running containers:'

                                docker ps
                            "
                    '''
                }
            }
        }



        stage('Application Health Check') {

            steps {

                sshagent(credentials: ["${SSH_CREDENTIALS}"]) {

                    sh '''

                        echo "Waiting for application to start..."

                        sleep 10


                        echo "Checking application health..."


                        ssh \
                            -o StrictHostKeyChecking=no \
                            ${EC2_USER}@${EC2_HOST} \
                            "
                                curl -f \
                                http://localhost:${HOST_PORT}/demoproject/health
                            "


                        echo ""
                        echo "=========================================="
                        echo "APPLICATION HEALTH CHECK PASSED"
                        echo "=========================================="
                    '''
                }
            }
        }
    }


    post {

        success {

            echo """
                 DEPLOYMENT SUCCESSFUL

            Application:
            ${ECR_IMAGE}:${BUILD_NUMBER}

            Git Branch:
            ${params.BRANCH}

            EC2:
            ${EC2_HOST}

            Application Port:
            ${HOST_PORT}

            Container Port:
            ${CONTAINER_PORT}

            Health URL:
            http://${EC2_HOST}:${HOST_PORT}/demoproject/health

            """
        }


        failure {

            echo """
                    PIPELINE FAILED

            """
        }


        always {

            echo "Pipeline execution completed."
        }
    }
}

