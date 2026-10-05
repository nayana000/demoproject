pipeline {

    agent any
    options {
    	skipDefaultCheckout(true)
    }

    tools {
        jdk 'java21'
        maven 'maven3'
    }

    environment {

        SONARQUBE_SERVER = 'SonarQube'
        NEXUS_URL = 'http://13.201.240.69:8081'
        NEXUS_REPOSITORY = 'maven-releases'
        GIT_CREDENTIALS = 'gitcredentials'
        NEXUS_CREDENTIALS = 'nexuscredentials'
        GIT_REPO = 'https://github.com/nayana000/demoproject.git'
	AWS_REGION = 'ap-south-1'
	AWS_ACCOUNT_ID = '890615325308'
	ECR_REPOSITORY = 'demoproject'
	ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
	EC2_CREDENTIALS = 'sshkey'
	EC2_HOST = '3.109.206.28'
	EC2_USER = 'ubuntu'
	CONTAINER_NAME = 'demoproject'
	HOST_PORT = '8082'
        APPLICATION_PORT = '8081'

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
                    git branch --show-current

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

                    sh '''
                        mvn clean verify sonar:sonar
                    '''
                }
            }
        }


        stage('Quality Gate') {

            steps {
                echo 'WAITING FOR SONARQUBE QUALITY GATE'
                timeout(
                    time: 10,
                    unit: 'MINUTES'
                ) {

                    waitForQualityGate(
                        abortPipeline: true
                    )
                }

                echo 'SONARQUBE QUALITY GATE PASSED'
            }
        }


        stage('Build') {

            steps {

                echo 'BUILDING APPLICATION'

                sh '''
                    mvn package -DskipTests
                '''

                echo 'BUILD COMPLETED'
                sh '''
                    echo "Generated artifacts:"
                    find target -type f
                '''
            }
        }

        stage('Push to Nexus') {

            steps {

                echo 'PUSH ARTIFACTS TO NEXUS'
                withCredentials([
                    usernamePassword(
                        credentialsId: "${NEXUS_CREDENTIALS}",
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {

                    sh '''
                        set +x

                        echo "Creating temporary Maven settings..."

                        cat > settings.xml <<EOF
<settings>
    <servers>
        <server>
            <id>nexus</id>
            <username>${NEXUS_USERNAME}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        echo "Uploading artifact to Nexus..."

                        mvn deploy \
                            -DskipTests \
                            -s settings.xml

                        echo "NEXUS UPLOAD SUCCESSFUL"

                        rm -f settings.xml
                    '''
                }
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

            docker push \
                ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
        '''

        echo 'DOCKER IMAGE PUSHED TO ECR'
    }
}

stage('Deploy to EC2') {

    steps {

        echo 'DEPLOYING DOCKER IMAGE TO EC2'

        sshagent(credentials: ["${EC2_CREDENTIALS}"]) {

            sh '''
                ssh -o StrictHostKeyChecking=no \
                    ${EC2_USER}@${EC2_HOST} << EOF

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

                    docker stop \
                        ${CONTAINER_NAME} || true


                    echo "Removing old container..."

                    docker rm \
                        ${CONTAINER_NAME} || true


                    echo "Starting new container..."

                    docker run -d \
                        --name ${CONTAINER_NAME} \
                        --restart unless-stopped \
                        -p ${HOST_PORT}:${APPLICATION_PORT} \
                        ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}


                    echo "Waiting for application..."

                    sleep 10


                    echo "Checking container..."

                    if [ "\$(docker inspect -f '{{.State.Running}}' ${CONTAINER_NAME})" != "true" ]; then

                        echo "Container failed to start."

                        docker logs ${CONTAINER_NAME}

                        exit 1

                    fi


                    
                    echo "DEPLOYMENT SUCCESSFUL"
                    

                    docker ps

EOF
            '''
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

            sh '''
                rm -f settings.xml || true
            '''

            archiveArtifacts(
                artifacts: 'target/*.jar',
                allowEmptyArchive: true
            )

            cleanWs()
        }
    }
}

