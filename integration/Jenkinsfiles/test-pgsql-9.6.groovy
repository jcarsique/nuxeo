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
timeout(time: 2, unit: 'HOURS') {
    timestamps {
        node('SLAVE') {
            stage 'clone' {
                checkout([$class: 'GitSCM', branches: [[name: "*/${BRANCH}"]],
                    browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
                    doGenerateSubmoduleConfigurations: false, extensions: [
                        [$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false, timeout: 300]
                    ],
                    submoduleCfg: [], userRemoteConfigs: [
                        [url: 'git@github.com:nuxeo/nuxeo.git']
                    ]])
                sh """#!/bin/bash -xe
                    ./clone.py $BRANCH -f $PARENT_BRANCH
                """
            }

            stage 'tests' {
                sh """#!/bin/bash -xe
                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml --project-name $JOB_NAME-$BUILD_NUMBER pull
                    docker-compose -f integration/Jenkinsfiles/docker-compose-pgsql-9.6.yml --project-name $JOB_NAME-$BUILD_NUMBER up --build --exit-code-from tests db
                """
            }

            stage 'results' {
                step([$class: 'ArtifactArchiver', artifacts: '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*', fingerprint: false])
                step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/*.xml'])
            }
        }
    }
}
