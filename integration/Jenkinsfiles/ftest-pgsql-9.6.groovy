/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     jcarsique
 */

currentBuild.setDescription("PostgreSQL 9.6")
node('SLAVE') {
    tool name: 'ant-1.9', type: 'ant'
    tool name: 'java-8-openjdk', type: 'hudson.model.JDK'
    tool name: 'maven-3', type: 'hudson.tasks.Maven$MavenInstallation'
    timeout(time: 12, unit: 'HOURS') {
        timestamps {
            stage 'clone'
                checkout([$class: 'GitSCM', branches: [[name: '*/${BRANCH}']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'], doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'PathRestriction', excludedRegions: '', includedRegions: ['nuxeo-distribution/.*'], ['integration/.*']],
                                       [$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'nuxeo-distribution'], [path: 'integration']]],
                                       [$class: 'CleanBeforeCheckout'], [$class: 'WipeWorkspace']
                          ], submoduleCfg: [], userRemoteConfigs: [[url: 'git://github.com/nuxeo/nuxeo.git']]
                    ])
            stash 'clone'

            stage 'tests'
            parallel (
                "tests CMIS" : {
                    node('SLAVE') {
                        timeout(time: 2, unit: 'HOURS') {
                            unstash "clone"
                            try {
                                sh """#!/bin/bash -x
                                    ls -la . $WORKSPACE/
                                    export TESTS_COMMAND="mvn -B -f $WORKSPACE/nuxeo-distribution/nuxeo-server-cmis-tests/pom.xml clean verify -Pqa,tomcat,pgsql"
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml pull
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml --project-name $JOB_NAME-$BUILD_NUMBER up --no-color --build --abort-on-container-exit tests db
                                    ! grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}.*ERROR.*' nuxeo-distribution/nuxeo-server-cmis-tests/target/tomcat/log/server.log
                                """
                                // setBuildStatus("CMIS Build complete", "SUCCESS");
                            } finally {
                                archive 'nuxeo-distribution/nuxeo-server-cmis-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-server-cmis-tests/target/*.png, nuxeo-distribution/nuxeo-server-cmis-tests/target/*.json, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/*.log, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/log/*, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*'
                                junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                                // missing Claim plugin
                                // emailext body: '$DEFAULT_CONTENT', recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: '$DEFAULT_RECIPIENTS,ecm-qa@lists.nuxeo.com'
                                emailext body: '$DEFAULT_CONTENT', replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: 'jcarsique@nuxeo.com'
                                // missing Jabber plugin
                                sh """#!/bin/bash -x
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml down
                                """
                            }
                        }
                    }
                },
                "tests FunkLoad" : {
                    node('SLAVE') {
                        timeout(time: 2, unit: 'HOURS') {
                            unstash "clone"
                            try {
                                sh """#!/bin/bash -x
                                    ls -la . $WORKSPACE/
                                    export TESTS_COMMAND="mvn -B -f $WORKSPACE/nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/pom.xml clean verify -Pqa,tomcat,pgsql"
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml pull
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml --project-name $JOB_NAME-$BUILD_NUMBER up --no-color --build --abort-on-container-exit tests db
                                    ! grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}.*ERROR.*' nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/tomcat/log/server.log
                                """
                                // setBuildStatus("FunkLoad Build complete", "SUCCESS");
                            } finally {
                                archive 'nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/*.png, nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/*.json, nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/**/*.log, nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/**/log/*, nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*, nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/results/*/*'
                                junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                                // missing Claim plugin
                                // emailext body: '$DEFAULT_CONTENT', recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: '$DEFAULT_RECIPIENTS,ecm-qa@lists.nuxeo.com'
                                emailext body: '$DEFAULT_CONTENT', replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: 'jcarsique@nuxeo.com'
                                // missing Jabber plugin
                                sh """#!/bin/bash -x
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml down
                                """
                            }
                        }
                    }
                },
                "tests WebDriver" : {
                    node('SLAVE') {
                        timeout(time: 2, unit: 'HOURS') {
                            unstash "clone"
                            try {
                                sh """#!/bin/bash -x
                                    ls -la . $WORKSPACE/
                                    export TESTS_COMMAND="mvn -B -f $WORKSPACE/nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/pom.xml clean verify -Pqa,tomcat,pgsql"
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml pull
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml --project-name $JOB_NAME-$BUILD_NUMBER up --no-color --build --abort-on-container-exit tests db
                                    ! grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}.*ERROR.*' nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/tomcat/log/server.log
                                """
                                // setBuildStatus("FunkLoad Build complete", "SUCCESS");
                            } finally {
                                archive 'nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.png, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.json, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/*.log, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/log/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/results/*/*'
                                junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                                // missing Claim plugin
                                // emailext body: '$DEFAULT_CONTENT', recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: '$DEFAULT_RECIPIENTS,ecm-qa@lists.nuxeo.com'
                                emailext body: '$DEFAULT_CONTENT', replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: 'jcarsique@nuxeo.com'
                                // missing Jabber plugin
                                sh """#!/bin/bash -x
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml down
                                """
                            }
                        }
                    }
                }
            )
        }
    }
}

void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
//      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/my-org/my-repo"],
//      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}
