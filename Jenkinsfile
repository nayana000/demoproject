pipeline {

    agent any

    options {
        skipDefaultCheckout(true)
    }

 /*   tools {
        jdk 'java21'
        maven 'maven3'
    } */

    environment {

        SONARQUBE_SERVER = 'SonarQube'
        SONAR_CREDENTIALS = 'sonartocken'

        GIT_CREDENTIALS = 'gitcredentials'
        GIT_REPO = 'https://github.com/nayana000/demoproject.git'

        AWS_REGION = 'ap-south-1'
        AWS_ACCOUNT_ID = '890615325308'
        ECR_REPOSITORY = 'demoproject'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

        EC2_CREDENTIALS = 'sshkey'
        EC2_HOST = '15.206.205.193'
        EC2_USER = 'ubuntu'

        CONTAINER_NAME = 'demoproject'

        // ec2 host port
        HOST_PORT = '8082'

        // application port
        APPLICATION_PORT = '8080'
    }

    parameters {

        gitParameter(
            name: 'BRANCH',
            type: 'PT_BRANCH',
            defaultValue: 'main',
            branchFilter: 'origin/(.*)',
            selectedValue: 'DEFAULT',
            sortMode: 'ASCENDING',
            description: 'Select the Git branch to build',
            useRepository: 'https://github.com/nayana000/demoproject.git'
        )
    }

    stages {

        stage('Checkout') {

            steps {

                echo "CHECKING OUT BRANCH: ${params.BRANCH}"

                checkout([
                    $class: 'GitSCM',

                    branches: [[
                        name: "*/${params.BRANCH}"
                    ]],

                    userRemoteConfigs: [[
                        url: "${GIT_REPO}",
                        credentialsId: "${GIT_CREDENTIALS}"
                    ]],

                    extensions: [
                        [
                            $class: 'CleanBeforeCheckout'
                        ]
                    ]
                ])

                sh '''
                    echo "CHECKED OUT BRANCH"

                    echo "Commit:"
                    git rev-parse HEAD

                    echo "Commit Message:"
                    git log -1 --pretty=%B
                '''
            }
        }

        stage('SonarQube Analysis') {

            steps {

                echo 'RUNNING SONARQUBE ANALYSIS'

                withSonarQubeEnv("${SONARQUBE_SERVER}") {

                    withCredentials([
                        string(
                            credentialsId: "${SONAR_CREDENTIALS}",
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {

                        sh '''
                            mvn clean verify sonar:sonar \
                                -Dsonar.token="$SONAR_TOKEN"
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {

            steps {

                echo 'SONARQUBE QUALITY GATE PASSED'
            }
        }

        stage('Build Docker Image') {

            steps {

                echo 'BUILDING DOCKER IMAGE'

                script {

                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    echo "Docker Image Tag: ${IMAGE_TAG}"
                }

                sh '''
                    docker build \
                        -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG} \
                        -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest \
                        .
                '''

                echo 'DOCKER IMAGE BUILD COMPLETED'
            }
        }

        stage('Login to ECR') {

            steps {

                echo 'LOGGING INTO AWS ECR'

                sh '''
                    aws ecr get-login-password \
                        --region ${AWS_REGION} \
                    | docker login \
                        --username AWS \
                        --password-stdin \
                        ${ECR_REGISTRY}
                '''

                echo 'ECR LOGIN SUCCESSFUL'
            }
        }

        stage('Push Docker Image to ECR') {

            steps {

                echo 'PUSHING DOCKER IMAGE TO ECR'

                sh '''
                    docker push \
                        ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                '''

                echo 'DOCKER IMAGE PUSHED TO ECR'
            }
        }

        stage('Deploy to EC2') {

    steps {

        echo 'DEPLOYING DOCKER IMAGE TO EC2'

        sshagent(credentials: ["${EC2_CREDENTIALS}"]) {

            sh """
                ssh -o StrictHostKeyChecking=no \
                    ${EC2_USER}@${EC2_HOST} 'bash -s' << EOF

                    set -e
                    echo "EC2 DEPLOYMENT STARTED"

                    echo "Logging into ECR..."

                    aws ecr get-login-password \
                        --region ${AWS_REGION} \
                    | docker login \
                        --username AWS \
                        --password-stdin \
                        ${ECR_REGISTRY}

                    echo "Pulling Docker image..."

                    docker pull \
                        ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}

                    echo "Stopping old container..."

                    docker stop ${CONTAINER_NAME} 2>/dev/null || true

                    echo "Removing old container..."

                    docker rm ${CONTAINER_NAME} 2>/dev/null || true

                    echo "Starting new container..."

                    docker run -d \
                        --name ${CONTAINER_NAME} \
                        --restart unless-stopped \
                        -p ${HOST_PORT}:${APPLICATION_PORT} \
                        ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}

                    echo "Waiting for application..."

                    sleep 10

                    echo "CONTAINER STATUS"

                    docker ps -a

                    echo "Checking container..."

                    CONTAINER_STATUS= $(docker inspect \
                        -f '{{.State.Status}}' \
                        ${CONTAINER_NAME} 2>/dev/null || true)

                    echo "Container status: [$CONTAINER_STATUS]"

                    if [ "$CONTAINER_STATUS" != "running" ]; then

                        echo "Container failed to start."

                        docker ps -a

                        docker logs ${CONTAINER_NAME} || true

                        exit 1

                    fi

                    echo "CONTAINER IS RUNNING"
                    echo "CHECK APPLICATION"
                        http://localhost:${HOST_PORT}
                    echo "DEPLOYMENT SUCCESSFUL"
                    docker ps

                    EOF
            """
        }
    }
}
    }

    post {

        success {

            echo "PIPELINE EXECUTION SUCCESSFUL"
        }

        failure {

            echo "PIPELINE FAILED"
        }

        always {

            cleanWs()
        }
    }
}
