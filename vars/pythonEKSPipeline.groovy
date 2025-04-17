def call(Map configMap){
    pipeline {
        agent {
            label 'agent-1-label'
        }
        options{
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
            //retry(1)
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Select to deploy or not')
        }
        environment {
            appversion = '' // this will become global, we can use across pipeline
            region = 'us-east-1'
            account_id = '135808959960'
            project = configMap.get("project")
            environment = 'prod'
            component = configMap.get("component")
        }

        stages {
            stage('Read the version') {
                steps {
                    script{
                        def packageJson = readJSON file: 'package.json'
                        appversion = packageJson.version
                        echo "App version: ${appversion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    sh 'pip3.11 install -r requirements.txt'
                }
            }
            /* stage('SonarQube analysis') {
                environment {
                    SCANNER_HOME = tool 'sonar-6.0' //scanner config
                }
                steps {
                    // sonar server injection
                    withSonarQubeEnv('sonar-6.0') {
                        sh '$SCANNER_HOME/bin/sonar-scanner'
                        //generic scanner, it automatically understands the language and provide scan results
                    }
                }
            }

            stage('Quality Gate') {
                steps {
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            } */
            stage('Docker build') {
                
                steps {
                    withAWS(region: 'us-east-1', credentials: "aws-creds-${environment}") {
                        sh """
                            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${acc_ID}.dkr.ecr.${region}.amazonaws.com

                            docker build -t ${acc_ID}.dkr.ecr.${region}.amazonaws.com/kdp-${project}-${environment}/${component}:${appversion} .

                            docker images

                            docker push ${acc_ID}.dkr.ecr.${region}.amazonaws.com/kdp-${project}-${environment}/${component}:${appversion}                 

                        """
                    }
                }
            }
            stage('Deploy'){
                when{
                    expression {params.deploy}
                }

                steps{
                    build job: "../${component}-CD", parameters: [
                        string(name: 'version', value: "$appversion"),
                        string(name: 'ENVIRONMENT', value: "prod"),
                    ], wait: true
                }
            }
        }

        post {
            always{
                echo "This sections runs always"
                deleteDir()
            }
            success{
                echo "This section run when pipeline success"
            }
            failure{
                echo "This section run when pipeline failure"
            }
        }
    }
}