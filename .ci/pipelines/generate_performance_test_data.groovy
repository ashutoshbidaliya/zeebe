#!/usr/bin/env groovy


// general properties for CI execution
def static NODE_POOL() { return "slaves-ssd-small" }
def static MAVEN_DOCKER_IMAGE() { return "maven:3.6.1-jdk-8-slim" }
def static POSTGRES_DOCKER_IMAGE(String postgresVersion) { return "postgres:${postgresVersion}" }
def static CAMBPM_DOCKER_IMAGE(String cambpmVersion) { return "camunda/camunda-bpm-platform:${cambpmVersion}" }

static String agent(env, postgresVersion='9.6-alpine', cambpmVersion = '7.10.0') {
    return """
metadata:
  labels:
    agent: optimize-ci-build
spec:
  nodeSelector:
    cloud.google.com/gke-nodepool: ${NODE_POOL()}
  tolerations:
   - key: \"${NODE_POOL()}"
     operator: "Exists"
     effect: "NoSchedule"
  securityContext:
    fsGroup: 1000
  volumes:
    - name: cambpm-storage
      emptyDir: {}
    - name: ssd-storage
      hostPath:
        path: /mnt/disks/ssd0
        type: Directory
  initContainers:
  - name: init-cleanup
    image: busybox
    command: ['sh', '-c', 'rm -fr /var/lib/postgresql/data/*; rm -fr /export/*']
    volumeMounts:
    - name: ssd-storage
      mountPath: /var/lib/postgresql/data
      subPath: pgdata
  containers:
  - name: maven
    image: ${MAVEN_DOCKER_IMAGE()}
    command: ["cat"]
    tty: true
    env:
      - name: LIMITS_CPU
        valueFrom:
          resourceFieldRef:
            resource: limits.cpu
      - name: TZ
        value: Europe/Berlin
    resources:
      limits:
        cpu: 1.5
        memory: 2Gi
      requests:
        cpu: 1.5
        memory: 1Gi
    volumeMounts:
      - name: cambpm-storage
        mountPath: /cambpm-storage
      - name: ssd-storage
        mountPath: /export
        subPath: gcloud
  - name: postgres
    image: ${POSTGRES_DOCKER_IMAGE(postgresVersion)}
    env:
      - name: POSTGRES_USER
        value: camunda
      - name: POSTGRES_PASSWORD
        value: camunda
      - name: POSTGRES_DB
        value: engine
      - name: TZ
        value: Europe/Berlin
    ports:
    - containerPort: 5432
      name: postgres
      protocol: TCP
    resources:
      requests:
        cpu: 3 
        memory: 10Gi
    volumeMounts:
        - name: ssd-storage
          mountPath: /var/lib/postgresql/data
          subPath: pgdata
        - name: ssd-storage
          mountPath: /export
          subPath: gcloud
  - name: cambpm
    image: ${CAMBPM_DOCKER_IMAGE(cambpmVersion)}
    env:
      - name: DB_DRIVER
        value: org.postgresql.Driver
      - name: DB_USERNAME
        value: camunda
      - name: DB_PASSWORD
        value: camunda
      - name: DB_URL
        value: jdbc:postgresql://localhost:5432/engine
      - name: TZ
        value: Europe/Berlin
      - name: WAIT_FOR
        value: localhost:5432
      - name: JAVA_OPTS
        value: "-Xms2g -Xmx2g -XX:MaxMetaspaceSize=256m"
    resources:
      limits:
        cpu: 2 
        memory: 3Gi
    volumeMounts:
      - name: cambpm-storage
        mountPath: /camunda/logs
  - name: gcloud
    image: google/cloud-sdk:alpine
    imagePullPolicy: Always
    command: ["cat"]
    tty: true
    resources:
      limits:
        cpu: 500m 
        memory: 512Mi
      requests:
        cpu: 200m
        memory: 128Mi
    volumeMounts:
      - name: ssd-storage
        mountPath: /export
        subPath: gcloud
"""
}
/******** START PIPELINE *******/

pipeline {
    agent {
        kubernetes {
            cloud 'optimize-ci'
            label "optimize-ci-build_${env.JOB_BASE_NAME.replaceAll("%2F", "-").replaceAll("\\.", "-").take(20)}-${env.BUILD_ID}"
            defaultContainer 'jnlp'
            yaml agent(env, POSTGRES_VERSION, CAMBPM_VERSION)
        }
    }

    environment {
        NEXUS = credentials("camunda-nexus")
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 48, unit: 'HOURS')
    }

    stages {
        stage('Prepare') {
            steps {
                git url: 'git@github.com:camunda/camunda-optimize',
                        branch: "$BRANCH",
                        credentialsId: 'camunda-jenkins-github-ssh',
                        poll: false
                container('postgres') {
                    sh ("df -h /export /var/lib/postgresql/data")
                }
            }
        }
        stage('Data Generation') {
            steps {
                container('maven') {
                    // Generate Data
                    sh ("mvn -T1C -B -s settings.xml -f qa/data-generation compile exec:java -Dexec.args=\"--numberOfProcessInstances ${NUM_INSTANCES}\"")
                }
            }
        }
        stage('Dump Data') {
            steps {
                container('postgres') {
                    // Export dump
                    sh ("pg_dump -h localhost -U camunda -n public --format=c --file=\"/export/${SQL_DUMP}\" engine")
                }
            }
        }
        stage('Upload Data') {
            steps {
                container('gcloud') {
                    // Upload data
                    sh ("gsutil -h \"x-goog-meta-NUM_INSTANCES: ${NUM_INSTANCES}\" cp \"/export/${SQL_DUMP}\" gs://optimize-data/")
                    // Cleanup
                    sh ("rm /export/${SQL_DUMP}")
                }
            }
        }
    }

    post {
        changed {
            buildNotification(currentBuild.result)
        }
    }
}

void buildNotification(String buildStatus) {
    // build status of null means successful
    buildStatus = buildStatus ?: 'SUCCESS'

    String buildResultUrl = "${env.BUILD_URL}"
    if(env.RUN_DISPLAY_URL) {
        buildResultUrl = "${env.RUN_DISPLAY_URL}"
    }

    def subject = "[${buildStatus}] - ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}"
    def body = "See: ${buildResultUrl}"
    def recipients = [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]

    emailext subject: subject, body: body, recipientProviders: recipients
}
