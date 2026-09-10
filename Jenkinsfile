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

