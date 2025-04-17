def call (Map configMap) {   
    pipeline {
        agent {
            label 'agent-1-label'
        }
        options{
            timeout(time: 50, unit: 'MINUTES')
            disableConcurrentBuilds()
            ansiColor('xterm')
        }

        parameters{
        booleanParam(name: 'deploy', defaultValue: false, description: 'Select to deploy or not')
        }
        
        environment {
            DEBUG = 'true'
            appversion = ''
            region = 'us-east-1'
            acc_ID = '135808959960'
            project = configMap.get("project")
            environment = 'prod'
            component = configMap.get("component")

        }
        
        stages {
            stage('Read the application version') {
                steps {
                    script{
                        def packageJson = readJSON file: 'scripts/package.json'
                        appversion = packageJson.version
                        echo "app Version: ${appversion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    sh """
                        cd scripts
                        npm install
                    """
                }
            }

            // stage('SonarQube analysis') {
            //     environment {
            //         SCANNER_HOME = tool 'sonar-6.0' //scanner config
            //     }
            //     steps {
            //         // sonar server injection
            //         withSonarQubeEnv('sonar-6.0') {
            //             sh '$SCANNER_HOME/bin/sonar-scanner'
            //             //generic scanner, it automatically understands the language and provide scan results
            //         }
            //     }
            // }
            // stage('SQuality Gate') {
            //     steps {
            //         timeout(time: 5, unit: 'MINUTES') {
            //             waitForQualityGate abortPipeline: true
            //         }
            //     }
            // }

            // stage("Docker Build Image") {
            //     steps {
            //         sh """
            //             docker build -t naveenrajoli/backend:${appversion} .
            //             docker images
            //         """
            //     }
            // }

            stage('Push docker image to ECR') {
                steps {
                    withAWS(region: 'us-east-1', credentials: 'aws-cred') {
                        sh """                   
                            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${acc_ID}.dkr.ecr.${region}.amazonaws.com

                            docker build -t ${acc_ID}.dkr.ecr.${region}.amazonaws.com/kdp-${project}-${environment}/${component}:${appversion} .

                            docker images

                            docker push ${acc_ID}.dkr.ecr.${region}.amazonaws.com/kdp-${project}-${environment}/${component}:${appversion}                 

                        """
                    }
                }
            }

            stage('Deploy Backend') {
                when {
                    expression { params.deploy }
                }
                steps {
                    
                    build job: '../${component}-CD', parameters: [
                        string(name: 'version', value: "$appversion"),
                        string(name: 'ENVIRONMENT', value: "prod"),
                    ], wait: true
                }
            }
            

        }
        








        post {
            always{
                echo 'this will run always'
                deleteDir()
            }
            success{
                echo 'this will run on success'
            }
            failure{
                echo 'this will run at failure'
            }
        }
    }
}