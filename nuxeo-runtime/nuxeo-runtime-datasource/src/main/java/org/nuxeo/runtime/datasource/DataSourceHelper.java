/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.w3c.dom.DOMException;

/**
 * Helper class to look up {@link DataSource}s without having to deal with vendor-specific JNDI prefixes.
 *
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class DataSourceHelper {

    private DataSourceHelper() {
    }

    /**
     * Get the JNDI prefix used for DataSource lookups.
     */
    public static String getDataSourceJNDIPrefix() {
        return NuxeoContainer.nameOf("jdbc");
    }

    /**
     * Look up a datasource JNDI name given a partial name.
     * <p>
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass {@code "foo"} to this method.
     *
     * @param partialName the partial name
     * @return the datasource JNDI name
     */
    public static String getDataSourceJNDIName(String name) {
        return NuxeoContainer.nameOf(relativize(name));
    }

    protected static String relativize(String name) {
        int idx = name.lastIndexOf("/");
        if (idx > 0) {
            name = name.substring(idx + 1);
        }
        return "jdbc/".concat(name);
    }

    /**
     * Look up a datasource given a partial name.
     * <p>
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass {@code "foo"} to this method.
     *
     * @param partialName the partial name
     * @return the datasource
     * @throws NamingException
     */
    public static DataSource getDataSource(String partialName) throws NamingException {
        DataSource ds = getDataSource(partialName, DataSource.class);
        return new DataSource() {

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return ds.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return ds.isWrapperFor(iface);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                ds.setLoginTimeout(seconds);
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                ds.setLogWriter(out);
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return ds.getParentLogger();
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return ds.getLoginTimeout();
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return ds.getLogWriter();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return validateAutoCommit(ds.getConnection(username, password));
            }

            @Override
            public Connection getConnection() throws SQLException {
                return validateAutoCommit(ds.getConnection());
            }

            Connection validateAutoCommit(Connection connection) throws SQLException {
                boolean actual = connection.getAutoCommit();
                boolean expected = TransactionHelper.isNoTransaction();
                if (actual != expected) {
                    connection.setAutoCommit(expected);
                }
                return connection;
            }
        };
    }

    public static XADataSource getXADataSource(String partialName) throws NamingException {
        return getDataSource(partialName, XADataSource.class);
    }

    protected static <T> T getDataSource(String name, Class<T> clazz) throws NamingException {
        PooledDataSourceRegistry pools = Framework.getService(PooledDataSourceRegistry.class);
        if (pools == null) {
            throw new NamingException("runtime datasource no installed");
        }
        T ds = pools.getPool(relativize(name), clazz);
        if (ds == null) {
            ds = clazz.cast(new InitialContext().lookup(DataSourceHelper.getDataSourceJNDIName(name)));
        }
        if (ds == null) {
            throw new NameNotFoundException(name + " not found in container");
        }
        return ds;
    }

    public static Map<String, DataSource> getDatasources() throws NamingException {
        String prefix = getDataSourceJNDIPrefix();
        Context naming = NuxeoContainer.getRootContext();
        if (naming == null) {
            throw new NamingException("No root context");
        }
        Context jdbc = (Context) naming.lookup(prefix);
        Enumeration<NameClassPair> namesPair = jdbc.list("");
        Map<String, DataSource> datasourcesByName = new HashMap<String, DataSource>();
        while (namesPair.hasMoreElements()) {
            NameClassPair pair = namesPair.nextElement();
            String name = pair.getName();
            if (pair.isRelative()) {
                name = prefix + "/" + name;
            }
            Object ds = naming.lookup(name);
            if (ds instanceof DataSource) {
                datasourcesByName.put(name, (DataSource) ds);
            }
        }
        return datasourcesByName;
    }

    public static String getDataSourceRepositoryJNDIName(String repositoryName) {
        return getDataSourceJNDIName(ConnectionHelper.getPseudoDataSourceNameForRepository(repositoryName));
    }

    /**
     * @since 7.4
     */
    public static DataSource addDataSource(String name, String dbUrl, String dbDriver, String dbUser,
            String dbPassword) throws NamingException {
        DataSourceDescriptor config = new DataSourceDescriptor();
        config.name = relativize(name);
        try {
            config.element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement(
                    "empty");
        } catch (DOMException | ParserConfigurationException cause) {
            throw new RuntimeException("Cannot create empty element", cause);
        }
        config.driverClasssName = dbDriver;
        config.properties = new HashMap<String, String>();
        config.properties.put("name", config.name);
        config.properties.put("driverClassName", dbDriver);
        config.properties.put("url", dbUrl);
        config.properties.put("username", dbUser);
        config.properties.put("password", dbPassword);
        DataSourceComponent.self.addDataSource(config);
        return DataSourceHelper.getDataSource(name);
    }

    /**
     * @since 7.4
     */
    public static void removeDataSource(String name) {
        DataSourceDescriptor config = new DataSourceDescriptor();
        config.name = relativize(name);
        DataSourceComponent.self.removeDataSource(config);
    }


    /**
     * @since 7.4
     */
    public static void addLink(String name, String dataSourceName) {
       DataSourceLinkDescriptor config = new DataSourceLinkDescriptor();
       config.name = name;
       config.global = dataSourceName;
       DataSourceComponent.self.addDataSourceLink(config);
    }

    /**
     * @since 7.4
     */
    public static void removeLink(String name) {
        DataSourceLinkDescriptor config = new DataSourceLinkDescriptor();
        config.name = name;
        DataSourceComponent.self.removeDataSourceLink(config);
     }
}
