/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.audit;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

/**
 * LOG4J SQL connection factory that first attempts to obtain a {@link javax.sql.DataSource} from the JNDI name
 * configured in Spring or, when not found, builds a new {@link javax.sql.DataSource DataSource} via Commons DBCP; if
 * any datasource if found, the SQL init script is used to populate the database.
 */
public final class AuditConnectionFactory {

    private static DataSource datasource;

    private static final String PERSISTENCE_CONTEXT = "/persistenceContext.xml";

    static {
        // 1. Attempts to lookup for configured JNDI datasource (if present and available)
        InputStream springConf = AuditConnectionFactory.class.getResourceAsStream(PERSISTENCE_CONTEXT);
        String primary = null;
        String fallback = null;
        try {
            DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
            LSParser parser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
            LSInput lsinput = impl.createLSInput();
            lsinput.setByteStream(springConf);
            Document source = parser.parse(lsinput);

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            XPathExpression expr = xpath.compile("//*[local-name()='bean' and @id='persistenceProperties']/"
                    + "child::*[local-name()='property' and @name='primary']/@value");
            primary = (String) expr.evaluate(source, XPathConstants.STRING);
            expr = xpath.compile("//*[local-name()='bean' and @id='persistenceProperties']/"
                    + "child::*[local-name()='property' and @name='fallback']/@value");
            fallback = (String) expr.evaluate(source, XPathConstants.STRING);

            expr = xpath.compile("//*[local-name()='property' and @name='jndiName']/@value");
            String jndiName = (String) expr.evaluate(source, XPathConstants.STRING);

            Context ctx = new InitialContext();
            Object obj = ctx.lookup(jndiName);

            datasource = (DataSource) PortableRemoteObject.narrow(obj, DataSource.class);
        } catch (Exception e) {
            // ignore
        } finally {
            IOUtils.closeQuietly(springConf);
        }

        // 2. Creates Commons DBCP datasource
        String initSQLScript = null;
        try {
            Resource persistenceProperties = null;
            if (primary != null) {
                if (primary.startsWith("file:")) {
                    persistenceProperties = new FileSystemResource(primary.substring(5));
                }
                if (primary.startsWith("classpath:")) {
                    persistenceProperties = new ClassPathResource(primary.substring(10));
                }
            }
            if ((persistenceProperties == null || !persistenceProperties.exists()) && fallback != null) {
                if (fallback.startsWith("file:")) {
                    persistenceProperties = new FileSystemResource(fallback.substring(5));
                }
                if (fallback.startsWith("classpath:")) {
                    persistenceProperties = new ClassPathResource(fallback.substring(10));
                }
            }
            Properties persistence = PropertiesLoaderUtils.loadProperties(persistenceProperties);

            initSQLScript = persistence.getProperty("audit.sql");

            if (datasource == null) {
                BasicDataSource bds = new BasicDataSource();
                bds.setDriverClassName(persistence.getProperty("jpa.driverClassName"));
                bds.setUrl(persistence.getProperty("jpa.url"));
                bds.setUsername(persistence.getProperty("jpa.username"));
                bds.setPassword(persistence.getProperty("jpa.password"));

                bds.setLogAbandoned(true);
                bds.setRemoveAbandoned(true);

                datasource = bds;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Audit datasource configuration failed", e);
        }

        // 3. Initializes the chosen datasource
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setScripts(new Resource[] { new ClassPathResource("/audit/" + initSQLScript) });
        // forces no statement separation
        populator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
        Connection conn = DataSourceUtils.getConnection(datasource);
        try {
            populator.populate(conn);
        } finally {
            DataSourceUtils.releaseConnection(conn, datasource);
        }
    }

    public static Connection getConnection() {
        if (datasource != null) {
            return DataSourceUtils.getConnection(datasource);
        }

        throw new IllegalStateException("Audit dataSource init failed: check logs");
    }

    private AuditConnectionFactory() {
        // empty constructor for static utility class
    }
}
