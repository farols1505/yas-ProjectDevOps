pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3.9'
    }

    environment {
        SONARQUBE_ENV = 'SonarQubeServer'
		TESTCONTAINERS_RYUK_DISABLED = 'true' // Vô hiệu hóa Ryuk để tránh lỗi permission[cite: 3]
    }

    stages {

        stage('Checkout') {
            steps {
                // Đảm bảo có đủ lịch sử để changeset hoạt động ổn định
                checkout([
                    $class: 'GitSCM',
                    branches: scm.branches,
                    userRemoteConfigs: scm.userRemoteConfigs,
                    extensions: [[$class: 'CloneOption', depth: 0]]
                ])
            }
        }

        // ========================
        // SECURITY SCAN (Yêu cầu 7c)
        // ========================
        stage('Security Scans') {
            parallel {
                stage('Gitleaks') {
                    steps {
                        sh "gitleaks detect --source . --report-format json --report-path gitleaks-report.json || true"
                        archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
                    }
                }

                stage('Snyk') {
                    steps {
                        withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                            sh """
                            snyk auth \$SNYK_TOKEN
                            snyk test --all-projects --severity-threshold=high || true
                            """

			    archiveArtifacts artifacts: 'snyk-report.json', allowEmptyArchive: true
                        }
                    }
                }
            }
        }

        // ========================
        // PROCESS MODULES (Yêu cầu 6)
        // ========================
        stage('Modules Processing') {
            parallel {

                stage('Product') {
                    when { changeset "product/**" }
                    steps { processModule("product") }
                }

                stage('Media') {
                    when { changeset "media/**" }
                    steps { processModule("media") }
                }

                stage('Cart') {
                    when { changeset "cart/**" }
                    steps { processModule("cart") }
                }

                stage('Customer') {
                    when { changeset "customer/**" }
                    steps { processModule("customer") }
                }

                stage('Inventory') {
                    when { changeset "inventory/**" }
                    steps { processModule("inventory") }
                }

                stage('Payment') {
                    when { changeset "payment/**" }
                    steps { processModule("payment") }
                }

                stage('Tax') {
                    when { changeset "tax/**" }
                    steps { processModule("tax") }
                }

                stage('Rating') {
                    when { changeset "rating/**" }
                    steps { processModule("rating") }
                }

                stage('Location') {
                    when { changeset "location/**" }
                    steps { processModule("location") }
                }
            }
        }

        // ========================
        // FALLBACK (tránh pipeline pass rỗng)
        // ========================
        stage('No Changes Fallback') {
            when {
                not {
                    anyOf {
                        changeset "product/**"
                        changeset "media/**"
                        changeset "cart/**"
                        changeset "customer/**"
                        changeset "inventory/**"
                        changeset "payment/**"
                        changeset "tax/**"
                        changeset "rating/**"
                        changeset "location/**"
                    }
                }
            }
            steps {
                echo "No module changes detected → skipping module build."
            }
        }

        // ========================
        // SONARQUBE (Yêu cầu 7c)
        // ========================
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_ENV}") {
	            // Build để tạo .class files
		    	sh "mvn clean install -DskipTests"

		    	// Sonar scan
                sh "mvn sonar:sonar -Dsonar.projectKey=yas-project"
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline SUCCESS'
        }
        failure {
            echo 'Pipeline FAILED'
        }
    }
}


/**
 * ========================
 * MODULE PROCESS FUNCTION
 * ========================
 */
def processModule(String moduleName) {
    script {
        def javaHome = tool 'JDK21'
        def mvnHome = tool 'Maven3.9'

        withEnv(["JAVA_HOME=${javaHome}", "PATH+JAVA=${javaHome}/bin", "PATH+MAVEN=${mvnHome}/bin", "DOCKER_HOST=unix:///var/run/docker.sock"]) {
            
            // Chạy verify thay vì test để kích hoạt cả Integration Test (Failsafe)
            sh "mvn clean verify jacoco:report -pl ${moduleName} -am -DtrimStackTrace=true"

            // 1. Thu thập kết quả Test
            junit allowEmptyResults: true, testResults: "**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml"

            // 2. Sử dụng recordCoverage để tạo báo cáo đẹp và check gate[cite: 3]
            recordCoverage(
                tools: [[parser: 'JACOCO', pattern: "${moduleName}/target/site/jacoco/jacoco.xml"]],
                qualityGates: [
                    [threshold: 70.0, metric: 'LINE', baseline: 'PROJECT', criticality: 'FAILURE']
                ]
            )

            // 3. Build package[cite: 3, 4]
            sh "mvn clean package -pl ${moduleName} -am -DskipTests"
        }
    }
}
