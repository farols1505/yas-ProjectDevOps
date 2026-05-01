pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3.9'
    }

    environment {
        SONARQUBE_ENV = 'SonarQubeServer'
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

        // Bọc toàn bộ các lệnh Maven vào withEnv để nhận đúng Java 25[cite: 3]
        withEnv([
            "JAVA_HOME=${javaHome}", 
            "PATH+JAVA=${javaHome}/bin", 
            "PATH+MAVEN=${mvnHome}/bin",
            "DOCKER_HOST=unix:///var/run/docker.sock" // Cấu hình then chốt[cite: 3]
        ]) {
            echo "Processing module: ${moduleName}"

            // Test + Coverage với quyền truy cập Docker cho Testcontainers[cite: 2, 3]
            sh "mvn clean test jacoco:report -pl ${moduleName} -am"

            junit "**/${moduleName}/target/surefire-reports/*.xml"
            jacoco execPattern: "**/${moduleName}/target/jacoco.exec",
                   classPattern: "**/${moduleName}/target/classes",
                   sourcePattern: "**/${moduleName}/src/main/java"

            // Kiểm tra Coverage >= 70%[cite: 2, 3]
            def coverageFile = "${moduleName}/target/site/jacoco/jacoco.xml"
            if (fileExists(coverageFile)) {
                def xml = readFile(coverageFile)
                def parser = new XmlSlurper().parseText(xml)
                def percent = parser.@'line-rate'.toFloat() * 100
                echo "Coverage of ${moduleName}: ${percent}%"
                if (percent < 70) { error "Coverage of ${moduleName} is below 70%" }
            }

            // Build module[cite: 3, 4]
            sh "mvn clean package -pl ${moduleName} -am -DskipTests"
        }
    }
}
