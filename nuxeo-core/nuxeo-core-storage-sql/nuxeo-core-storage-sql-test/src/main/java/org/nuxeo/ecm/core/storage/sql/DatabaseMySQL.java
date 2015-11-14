/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseMySQL extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseMySQL();

    private static final String DEF_URL = "jdbc:mysql://localhost:3306/nuxeojunittests";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-mysql-contrib.xml";

    private static final String DRIVER = "com.mysql.jdbc.Driver";

    private void setProperties() {
        setProperty(URL_PROPERTY, DEF_URL);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        setProperty(DRIVER_PROPERTY, DRIVER);
    }

    @Override
    public void setUp() throws SQLException {
        super.setUp();
        try {
            Class.forName(DRIVER);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        setProperties();
        Connection connection = DriverManager.getConnection(Framework.getProperty(URL_PROPERTY),
                Framework.getProperty(USER_PROPERTY), Framework.getProperty(PASSWORD_PROPERTY));
        doOnAllTables(connection, null, null, "DROP TABLE `%s` CASCADE");
        connection.close();
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("URL", Framework.getProperty(URL_PROPERTY));
        properties.put("User", Framework.getProperty(USER_PROPERTY));
        properties.put("Password", Framework.getProperty(PASSWORD_PROPERTY));
        descriptor.properties = properties;
        return descriptor;
    }

    @Override
    public boolean hasSubSecondResolution() {
        return false;
    }

    @Override
    public int getRecursiveRemovalDepthLimit() {
        // Stupid MySQL limitations:
        // "Cascading operations may not be nested more than 15 levels deep."
        // "Currently, triggers are not activated by cascaded foreign key
        // actions."
        // Use a bit less that 15 to cater for complex properties
        return 13;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
