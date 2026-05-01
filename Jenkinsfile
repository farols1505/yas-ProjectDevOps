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

							sh "docker ps"

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

        withEnv([
            "JAVA_HOME=${javaHome}",
            "PATH+JAVA=${javaHome}/bin",
            "PATH+MAVEN=${mvnHome}/bin"
        ]) {
            withSonarQubeEnv("${SONARQUBE_ENV}") { // Đưa Sonar vào đây
                sh """
                find . -name "logback.xml" -delete
                find . -name "logback-spring.xml" -delete

                # Chạy test, tạo report và đẩy lên Sonar cho module này
                mvn clean verify sonar:sonar \
                -pl ${moduleName} -am \
                -Dsonar.projectKey=yas-project-${moduleName} \
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                """
            }

            junit allowEmptyResults: true, testResults: "**/target/surefire-reports/*.xml"

            recordCoverage(
                tools: [[parser: 'JACOCO', pattern: "${moduleName}/target/site/jacoco/jacoco.xml"]],
                qualityGates: [[threshold: 70.0, metric: 'LINE', baseline: 'PROJECT', criticality: 'FAILURE']]
            )
        }
    }
}
