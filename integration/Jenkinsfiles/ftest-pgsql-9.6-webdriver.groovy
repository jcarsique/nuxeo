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
        timestamps {
            stage 'clone'
                checkout([$class: 'GitSCM', branches: [[name: '*/${BRANCH}']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'], doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'PathRestriction', excludedRegions: '', includedRegions: '''nuxeo-distribution/.*
integration/.*'''],
                                       [$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'nuxeo-distribution'], [path: 'integration']]],
                                       [$class: 'CleanBeforeCheckout'], [$class: 'WipeWorkspace']
                          ], submoduleCfg: [], userRemoteConfigs: [[url: 'git://github.com/nuxeo/nuxeo.git']]
                    ])

                    try {
                        timeout(time: 2, unit: 'HOURS') {
                            stage 'tests'
                                copyArtifacts filter: 'archives/mp-nuxeo-server/nuxeo-jsf-ui.zip', fingerprintArtifacts: true, projectName: '$UPSTREAM_JOB', selector: upstream(allowUpstreamDependencies: false, fallbackToLastSuccessful: true, upstreamFilterStrategy: 'UseGlobalSetting')
                                sh """#!/bin/bash -x

                                    LASTBUILD_URL=https://qa.nuxeo.org/jenkins/job/Deploy/job/IT-nuxeo-$BRANCH-build/lastSuccessfulBuild/artifact/archives ./download.sh
                                    rm -rf tomcat-*
                                    cp -R tomcat tomcat-funkload
                                    mv tomcat tomcat-webdriver

/*
from /Deploy/IT-nuxeo-master-tests-cap-tomcat-postgresql/
mvn clean verify -Pqa,tomcat,nightly,pgsql -f nuxeo/nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/pom.xml
  env.NUXEO_HOME=$WORKSPACE/tomcat-webdriver
  nuxeo.wizard.done=true
  mp.install=$WORKSPACE/archives/mp-nuxeo-server/nuxeo-jsf-ui.zip

mvn clean verify -Pqa,tomcat,nightly,pgsql -f nuxeo/nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/pom.xml
  env.NUXEO_HOME=$WORKSPACE/tomcat-funkload
  nuxeo.wizard.done=true
  mp.install=$WORKSPACE/archives/mp-nuxeo-server/nuxeo-jsf-ui.zip

*/

                                    export TESTS_COMMAND="mvn -B -f $WORKSPACE/nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/pom.xml clean verify -Pqa,tomcat,pgsql"
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml pull
                                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml --project-name $JOB_NAME-$BUILD_NUMBER up --no-color --build --abort-on-container-exit tests db
                                    ! grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}.*ERROR.*' nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/tomcat/log/server.log
                                """
                                // setBuildStatus("WebDriver Build complete", "SUCCESS");
                        }
                    } finally {
// nuxeo/nuxeo-distribution/**/log/*.log, tomcat*/log/*.log, tomcat*/nxserver/config/distribution.properties, nuxeo/nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/failsafe-reports/*, nuxeo/nuxeo-distribution/**/target/*.png, nuxeo/nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/nxtools-reports/*
                        archive 'nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.png, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.json, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/*.log, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/log/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/results/*/*'
// nuxeo/nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/failsafe-reports/*.xml, nuxeo/nuxeo-distribution/nuxeo-jsf-ui-funkload-tests/target/nxtools-reports/*.xml
                        junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
// set build description .*\[INFO\] Building Nuxeo JSF UI WebDriver Tests\s*([^\s]*)
                        // missing Claim plugin
                        // emailext body: '$DEFAULT_CONTENT', recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: '$DEFAULT_RECIPIENTS,ecm-qa@lists.nuxeo.com'
                        emailext body: '$DEFAULT_CONTENT', replyTo: '$DEFAULT_RECIPIENTS', subject: '$DEFAULT_SUBJECT', to: 'jcarsique@nuxeo.com'
                        // missing Jabber plugin
                        sh """#!/bin/bash -x
                            docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml down
                        """
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
