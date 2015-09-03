package org.nuxeo.cap.bench

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

object NuxeoRest {

  def encodePath = (path: String) => {
    java.net.URLEncoder.encode(path, "UTF-8")
  }

  /** Create a document ins the gatling workspace  */
  def createDocument() = {
    http("Create ${type}")
      .post(Constants.GAT_API_PATH + "/${parentPath}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .body(StringBody("${payload}"))
      .check(status.saveAs("status")).check(status.is(201))
  }

  /** Update the description of a document in the gatling workspace */
  def updateDocument() = {
    http("Update ${type}")
      .put(Constants.GAT_API_PATH + "/${url}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .body(StringBody( """{ "entity-type": "document","properties": {"dc:source":"nxgatudpate_${counterName}"}}"""))
      .check(status.in(200))
  }

  def createDocumentIfNotExists = (parent: String, name: String, docType: String) => {
    exitBlockOnFail {
      exec(
        http("Check if document exists")
          .head(Constants.API_PATH + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .check(status.in(404)))
        .exec(
          http("Create " + docType)
            .post(Constants.API_PATH + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${user}", "${password}")
            .body(StringBody( """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
            """","properties": {"dc:title":"""" + name + """", "dc:description": "Gatling bench """ +
            docType + """"}}"""))
            .check(status.in(201)))
    }
  }

  def createDocumentIfNotExistsAsAdmin = (parent: String, name: String, docType: String) => {
    exitBlockOnFail {
      exec(
        http("Check if document exists")
          .head(Constants.API_PATH + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create " + docType + " as admin")
            .post(Constants.API_PATH + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
              """","properties": {"dc:title":"""" + name + """", "dc:description": "Gatling bench folder"}}""".stripMargin))
            .check(status.in(201)))
    }
  }

  def createFileDocument = (parent: String, name: String) => {
    val batchId = name
    val filename = name + ".txt"
    exec(
      http("Create server file Upload")
        .post("/api/v1/automation/batch/upload")
        .headers(Headers.base)
        .header("X-Batch-Id", batchId)
        .header("X-File-Idx", "0")
        .header("X-File-Name", filename)
        .basicAuth("${user}", "${password}")
        .body(StringBody("You know content file"))
    ).exec(
        http("Create server File")
          .post(Constants.API_PATH + parent)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(
          """{ "entity-type": "document", "name":"""" + name + """", "type": "File","properties": {"dc:title":"""" +
            name +
            """", "dc:description": "Gatling bench file", "file:content": {"upload-batch":"""" + batchId +
            """","upload-fileId":"0"}}}""".stripMargin))
          .check(status.in(201)))
  }

  def updateFileDocument = (parent: String, name: String) => {
    val batchId = name
    val filename = name + "txt"
    exec(
      http("Update server file Upload")
        .post("/api/v1/automation/batch/upload")
        .headers(Headers.base)
        .header("X-Batch-Id", batchId)
        .header("X-File-Idx", "0")
        .header("X-File-Name", filename)
        .basicAuth("${user}", "${password}")
        .body(StringBody("You know content file " + Random.alphanumeric.take(2)))
    ).exec(
        http("Update server File")
          .put(Constants.API_PATH + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(
          """{ "entity-type": "document", "name":"""" + name + """", "type": "File","properties": {"file:content": {"upload-batch":"""" + batchId +
            """","upload-fileId":"0"}}}""".stripMargin))
          .check(status.in(200)))
  }

  def deleteFileDocument = (path: String) => {
    http("Delete server File")
      .delete(Constants.API_PATH + path)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .check(status.in(204))
  }

  def deleteFileDocumentAsAdmin = (path: String) => {
    http("Delete server File")
      .delete(Constants.API_PATH + path)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  def createUserIfNotExists = (groupName: String) => {
    exitBlockOnFail {
      exec(
        http("Check if user exists")
          .head("/api/v1/user/${user}")
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create user")
            .post("/api/v1/user")
            .headers(Headers.default)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{"entity-type":"user","id":"${user}","properties":{"firstName":null,"lastName":null,"password":"${password}","groups":["""" +
              groupName + """"],"company":null,"email":"devnull@nuxeo.com","username":"${user}"},"extendedGroups":[{"name":"members","label":"Members group","url":"group/members"}],"isAdministrator":false,"isAnonymous":false}"""))
            .check(status.in(201)))
    }
  }

  def deleteUser = () => {
    http("Delete user")
      .delete("/api/v1/user/${user}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  def createGroupIfNotExists = (groupName: String) => {
    exitBlockOnFail {
      exec(
        http("Check if group exists")
          .head("/api/v1/group/" + groupName)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create group")
            .post("/api/v1/group")
            .headers(Headers.default)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{"entity-type":"group","groupname":"""" + groupName + """", "groupLabel": "Gatling group"}"""))
            .check(status.in(201)))
    }
  }

  def deleteGroup = (groupName: String) => {
    http("Delete user")
      .delete("/api/v1/group/" + groupName)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }


  def grantReadWritePermission = (path: String, principal: String) => {
    http("Grant write permission")
      .post(Constants.API_PATH + path + "/@op/Document.SetACE")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .body(StringBody( """{"params":{"permission": "ReadWrite", "user": """" + principal + """"}}""".stripMargin))
      .check(status.in(200))
  }
}
