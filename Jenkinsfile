def mavenDockerImage    = 'nexus.intranda.com:4443/goobi-viewer-testing-index:latest'
def mavenDockerArgs     = '-v $HOME/.m2:/var/maven/.m2:z -v $HOME/.config:/var/maven/.config -v $HOME/.sonar:/var/maven/.sonar -u 1000 -e _JAVA_OPTIONS=-Duser.home=/var/maven -e MAVEN_CONFIG=/var/maven/.m2'
def nodeLabel           = 'controller'
def nexusRegistryUrl    = 'https://nexus.intranda.com:4443/'
def nexusRegistryCredId = 'jenkins-docker'

pipeline {

  agent { label nodeLabel }

  options {
    buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '5', daysToKeepStr: '90', numToKeepStr: '')
    disableConcurrentBuilds()
    timeout(time: 60, unit: 'MINUTES')
  }

  environment {
    GHCR_IMAGE_BASE      = 'ghcr.io/intranda/goobi-viewer'
    DOCKERHUB_IMAGE_BASE = 'intranda/goobi-viewer'
    NEXUS_IMAGE_BASE     = 'nexus.intranda.com:4443/goobi-viewer'
    NEXUS_BASE           = 'intranda-nexus::https://nexus.intranda.com/repository'
  }

  parameters {
    booleanParam(name: 'RUN_SONAR_ANALYSIS', defaultValue: false, description: 'Manually trigger sonar analysis (v* tags and release* branches always do it)')
    booleanParam(name: 'BUILD_DOCKER_IMAGE', defaultValue: false, description: 'Build & push the Docker image (applies only on develop and v* tags)')
  }

  stages {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SETUP
    // ─────────────────────────────────────────────────────────────────────────
    stage('setup') {
      steps {
        sh 'git reset --hard HEAD && git clean -fdx'
        sh 'git submodule update --init --recursive'
        script {
          if (env.TAG_NAME) {
            env.BUILD_VERSION = env.TAG_NAME.replaceAll('^v', '')
            env.BUILD_TYPE_NAME = "RELEASE"
            env.BUILD_TYPE = ""
          } else {
            env.BUILD_VERSION = 'dev'
            env.BUILD_TYPE_NAME = "SNAPSHOT"
            env.BUILD_TYPE = "-SNAPSHOT"
          }
          echo "BUILD_VERSION=${env.BUILD_VERSION} TYPE=${env.BUILD_TYPE_NAME}"
        }
      }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. BUILD  (skip tests/lint/depcheck; stash output for later stages)
    // ─────────────────────────────────────────────────────────────────────────
    stage('build') {
      agent {
        docker {
          image mavenDockerImage
          alwaysPull true
          registryUrl nexusRegistryUrl
          registryCredentialsId nexusRegistryCredId
          args mavenDockerArgs
          reuseNode true
        }
      }
      steps {
        sh "mvn -f pom.xml clean install -U -Dchangelist=\$BUILD_TYPE -DskipTests -Dcheckstyle.skip=true -DskipDependencyCheck=true --no-transfer-progress"
        // Stashes are used by the parallel checkstyle/dependency-check stages
        // that run in their own workspaces. test/sonar/deploy/docker reuse the
        // pipeline workspace and don't need to unstash.
        stash name: 'build-output', includes: [
                '**/target/classes/**',
                '**/target/generated-sources/**',
                '**/target/test-classes/**',
                '**/target/*.jar',
                '**/target/*.war',
                '**/target/.flattened-pom.xml',
                '**/target/maven-archiver/**',
                '**/target/maven-status/**'
        ].join(',')
        sh 'cp -r /var/maven/.m2/repository/io/goobi/viewer m2-goobi-viewer || true'
        stash name: 'm2-goobi-viewer', includes: 'm2-goobi-viewer/**', allowEmpty: true
      }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TEST + LINT + DEPENDENCY CHECK
    // ─────────────────────────────────────────────────────────────────────────
    stage('verify') {
      when {
        not { branch 'master' }
      }
      parallel {

        // test reuses the pipeline workspace; build artifacts are already there.
        stage('test') {
          agent {
            docker {
              image mavenDockerImage
              registryUrl nexusRegistryUrl
              registryCredentialsId nexusRegistryCredId
              args mavenDockerArgs
              reuseNode true
            }
          }
          steps {
            sh "mvn -f pom.xml test -Dchangelist=\$BUILD_TYPE -DskipTests=false -Dmaven.main.skip=true -Dcheckstyle.skip=true -DskipDependencyCheck=true --no-transfer-progress"
            junit '**/target/surefire-reports/*.xml'
            step([
                    $class           : 'JacocoPublisher',
                    execPattern      : '**/target/jacoco.exec',
                    classPattern     : '**/target/classes/',
                    sourcePattern    : '**/src/main/java',
                    exclusionPattern : '**/*Test.class'
            ])
            sh "mvn -f pom.xml org.jacoco:jacoco-maven-plugin:report -Dchangelist=\$BUILD_TYPE -Dmaven.main.skip=true --no-transfer-progress"
          }
        }

        // checkstyle and dependency-check pin to the same node label so the
        // docker image cache is reused, but get their own workspaces (so they
        // don't collide with test's target/ writes during the parallel run).
        stage('checkstyle') {
          agent {
            docker {
              image mavenDockerImage
              registryUrl nexusRegistryUrl
              registryCredentialsId nexusRegistryCredId
              label nodeLabel
              args mavenDockerArgs
            }
          }
          steps {
            sh 'git submodule update --init --recursive'
            unstash 'm2-goobi-viewer'
            sh 'mkdir -p /var/maven/.m2/repository/io/goobi/viewer && cp -r m2-goobi-viewer/. /var/maven/.m2/repository/io/goobi/viewer/ || true'
            sh "mvn -f pom.xml checkstyle:checkstyle -Dchangelist=\$BUILD_TYPE -Dcheckstyle.skip=false --no-transfer-progress"
            recordIssues(
                    enabledForFailure: true, aggregatingResults: false,
                    tools: [checkStyle(pattern: '**/target/checkstyle-result.xml', reportEncoding: 'UTF-8')]
            )
          }
        }

        stage('dependency-check') {
          agent {
            docker {
              image mavenDockerImage
              registryUrl nexusRegistryUrl
              registryCredentialsId nexusRegistryCredId
              label nodeLabel
              args mavenDockerArgs
            }
          }
          steps {
            sh 'git submodule update --init --recursive'
            unstash 'm2-goobi-viewer'
            sh 'mkdir -p /var/maven/.m2/repository/io/goobi/viewer && cp -r m2-goobi-viewer/. /var/maven/.m2/repository/io/goobi/viewer/ || true'
            unstash 'build-output'
            sh "mvn -f pom.xml verify -Dchangelist=\$BUILD_TYPE -Dmaven.main.skip=true -DskipTests -Dcheckstyle.skip=true -DskipDependencyCheck=false --no-transfer-progress"
            dependencyCheckPublisher pattern: '**/target/dependency-check-report.xml'
          }
        }

      }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. SONAR  (v* tags, release* branches, or manual trigger)
    // ─────────────────────────────────────────────────────────────────────────
    stage('sonar') {
      when {
        beforeAgent true
        anyOf {
          tag 'v*'
          branch 'release*'
          branch 'sonar*'
          expression { return params.RUN_SONAR_ANALYSIS == 'true' }
        }
      }
      agent {
        docker {
          image mavenDockerImage
          registryUrl nexusRegistryUrl
          registryCredentialsId nexusRegistryCredId
          args mavenDockerArgs
          reuseNode true
        }
      }
      steps {
        unstash 'm2-goobi-viewer'
        sh 'mkdir -p /var/maven/.m2/repository/io/goobi/viewer && cp -r m2-goobi-viewer/. /var/maven/.m2/repository/io/goobi/viewer/ || true'
        withCredentials([string(credentialsId: 'jenkins-sonarcloud', variable: 'TOKEN')]) {
          sh "mvn -f goobi-viewer-core/pom.xml sonar:sonar -Dchangelist=\$BUILD_TYPE -Dsonar.token=\$TOKEN -Dmaven.main.skip=true --no-transfer-progress"
          sh "mvn -f goobi-viewer-connector/pom.xml  sonar:sonar -Dchangelist=\$BUILD_TYPE -Dsonar.token=\$TOKEN -Dmaven.main.skip=true --no-transfer-progress"
        }
      }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. DEPLOY  (develop and v* tags only; pin ${revision}/dev-SNAPSHOT in
    //            deployed poms on tag builds)
    // ─────────────────────────────────────────────────────────────────────────
    stage('deploy') {
      when {
        beforeAgent true
        anyOf {
          branch 'develop'
          tag 'v*'
        }
      }
      agent {
        docker {
          image mavenDockerImage
          registryUrl nexusRegistryUrl
          registryCredentialsId nexusRegistryCredId
          args mavenDockerArgs
          reuseNode true
        }
      }
      steps {
        script {
          if (env.TAG_NAME) {
            sh '''#!/bin/bash -xe
              find . -name 'pom.xml' -not -path '*/target/*' -not -path '*/node_modules/*' \
                | xargs sed -i \
                  -e 's/\\${revision}/'"${BUILD_VERSION}"'/g' \
                  -e 's/dev-SNAPSHOT/'"${BUILD_VERSION}"'/g'
            '''
          }
        }
        sh '''#!/bin/bash -xe
          ALT_REPO="-DaltDeploymentRepository=${NEXUS_BASE}/${NEXUS_PUBLIC_REPO}-releases -DaltSnapshotDeploymentRepository=${NEXUS_BASE}/${NEXUS_PUBLIC_REPO}-snapshots"
          mvn -f goobi-viewer-config/pom.xml deploy -Dchangelist=$BUILD_TYPE -Dmaven.main.skip=true -DskipTests -Dcheckstyle.skip=true -DskipDependencyCheck=true -U $ALT_REPO --no-transfer-progress
          mvn -f goobi-viewer-core/pom.xml deploy -Dchangelist=$BUILD_TYPE -Dmaven.main.skip=true -DskipTests -Dcheckstyle.skip=true -DskipDependencyCheck=true -U $ALT_REPO --no-transfer-progress
          mvn -f goobi-viewer-connector/pom.xml deploy -Dchangelist=$BUILD_TYPE -Dmaven.main.skip=true -DskipTests -Dcheckstyle.skip=true -DskipDependencyCheck=true -U $ALT_REPO --no-transfer-progress
        '''
      }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. DOCKER  (develop and v* tags; reuses build output, build=false)
    // ─────────────────────────────────────────────────────────────────────────
    stage('docker') {
      when {
        beforeAgent true
        anyOf {
          expression { return params.BUILD_DOCKER_IMAGE }
          tag 'v*'
        }
      }
      steps {
        withCredentials([
                usernamePassword(
                        credentialsId: 'jenkins-github-container-registry',
                        usernameVariable: 'GHCR_USER',
                        passwordVariable: 'GHCR_PASS'
                ),
                usernamePassword(
                        credentialsId: '0b13af35-a2fb-41f7-8ec7-01eaddcbe99d',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_PASS'
                ),
                usernamePassword(
                        credentialsId: 'jenkins-docker',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                )
        ]) {
          sh '''
            echo "$GHCR_PASS"      | docker login ghcr.io                  -u "$GHCR_USER"      --password-stdin
            echo "$DOCKERHUB_PASS" | docker login docker.io                -u "$DOCKERHUB_USER" --password-stdin
            echo "$NEXUS_PASS"     | docker login nexus.intranda.com:4443  -u "$NEXUS_USER"     --password-stdin

            docker buildx create --name multiarch-builder --use || docker buildx use multiarch-builder
            docker buildx inspect --bootstrap

            SHORT_SHA=$(git rev-parse --short=7 HEAD)

            if [ -n "$TAG_NAME" ]; then
              TAGS="-t $GHCR_IMAGE_BASE:$TAG_NAME -t $DOCKERHUB_IMAGE_BASE:$TAG_NAME -t $NEXUS_IMAGE_BASE:$TAG_NAME"
              # If this tag is the highest v* tag in the upstream repo, also publish as :latest
              git fetch --tags --quiet origin || true
              HIGHEST=$(git tag -l 'v*' | sort -V | tail -n 1)
              echo "Current tag: $TAG_NAME, highest upstream v* tag: $HIGHEST"
              if [ "$HIGHEST" = "$TAG_NAME" ]; then
                echo "Tagging as latest"
                TAGS="$TAGS -t $GHCR_IMAGE_BASE:latest -t $DOCKERHUB_IMAGE_BASE:latest -t $NEXUS_IMAGE_BASE:latest"
              fi
              PLATFORMS="linux/amd64,linux/arm64/v8"
            else
              # Rolling :dev tag plus an immutable :dev-<short-sha> tag for the commit
              TAGS="-t $NEXUS_IMAGE_BASE:dev -t $NEXUS_IMAGE_BASE:dev-$SHORT_SHA"
              PLATFORMS="linux/amd64,linux/arm64/v8"
            fi

            CACHE="--cache-from type=registry,ref=$NEXUS_IMAGE_BASE:buildcache --cache-to type=registry,ref=$NEXUS_IMAGE_BASE:buildcache,mode=max"

            docker buildx build --pull --build-arg build=false \
              --platform $PLATFORMS \
              $CACHE \
              $TAGS \
              --push .
          '''
        }
      }
    }

  }

  post {
    always {
      sh 'rm -rf $HOME/.m2/repository/io/goobi/viewer/* || true'
    }
    success {
      // Keep the main built targets (config/core/connector jars + theme-reference
      // war) as Jenkins build artifacts. The attached test/javadoc jars are still
      // deployed to Nexus by the deploy stage, but not archived here.
      archiveArtifacts(
              artifacts: '**/target/*.jar, **/target/*.war',
              excludes: '**/target/*-tests.jar, **/target/*-javadoc.jar, **/target/*-sources.jar',
              fingerprint: true
      )
    }
    changed {
      emailext(
              subject: '${DEFAULT_SUBJECT}',
              body: '${DEFAULT_CONTENT}',
              recipientProviders: [requestor(), culprits()],
              attachLog: true
      )
    }
    failure {
      emailext(
              subject: '${DEFAULT_SUBJECT}',
              body: '${DEFAULT_CONTENT}',
              to: 'andrey.kozhushkov@intranda.com',
              attachLog: true
      )
    }
  }
}
/* vim: set ts=2 sw=2 tw=120 et :*/
