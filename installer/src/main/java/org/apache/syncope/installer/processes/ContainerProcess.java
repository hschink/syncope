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
package org.apache.syncope.installer.processes;

import org.apache.syncope.installer.utilities.FileSystemUtils;
import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.syncope.installer.containers.Glassfish;
import org.apache.syncope.installer.containers.Tomcat;
import org.apache.syncope.installer.containers.jboss.JBoss;
import org.apache.syncope.installer.enums.Containers;
import org.apache.syncope.installer.files.GlassfishCoreWebXml;
import org.apache.syncope.installer.files.JBossDeploymentStructureXml;
import org.apache.syncope.installer.files.PersistenceContextEMFactoryXml;
import org.apache.syncope.installer.files.CoreWebXml;
import org.apache.syncope.installer.utilities.InstallLog;
import org.apache.syncope.installer.utilities.MavenUtils;
import org.xml.sax.SAXException;

public class ContainerProcess {

    private String installPath;

    private String mavenDir;

    private String artifactId;

    private String tomcatUser;

    private String tomcatPassword;

    private boolean tomcatSsl;

    private String tomcatHost;

    private String tomcatPort;

    private String glassfishDir;

    private String confDirectory;

    private String logsDirectory;

    private String bundlesDirectory;

    private boolean withDataSource;

    private boolean jbossSsl;

    private String jbossHost;

    private String jbossPort;

    private String jbossJdbcModuleName;

    private String jbossAdminUsername;

    private String jbossAdminPassword;

    private boolean isProxyEnabled;

    private String proxyHost;

    private String proxyPort;

    private String proxyUser;

    private String proxyPwd;

    private boolean mavenProxyAutoconf;

    public void run(final AbstractUIProcessHandler handler, final String[] args) {

        installPath = args[0];
        mavenDir = args[1];
        artifactId = args[2];
        final Containers selectedContainer = Containers.fromContainerName(args[3]);
        tomcatSsl = Boolean.valueOf(args[4]);
        tomcatHost = args[5];
        tomcatPort = args[6];
        tomcatUser = args[7];
        tomcatPassword = args[8];
        glassfishDir = args[9];
        confDirectory = args[10];
        logsDirectory = args[11];
        bundlesDirectory = args[12];
        withDataSource = Boolean.valueOf(args[13]);
        jbossSsl = Boolean.valueOf(args[14]);
        jbossHost = args[15];
        jbossPort = args[16];
        jbossJdbcModuleName = args[17];
        jbossAdminUsername = args[18];
        jbossAdminPassword = args[19];
        isProxyEnabled = Boolean.valueOf(args[20]);
        proxyHost = args[21];
        proxyPort = args[22];
        proxyUser = args[23];
        proxyPwd = args[24];
        mavenProxyAutoconf = Boolean.valueOf(args[25]);

        final FileSystemUtils fileSystemUtils = new FileSystemUtils(handler);

        handler.logOutput("Configure web.xml file according to " + selectedContainer + " properties", true);
        InstallLog.getInstance().info("Configure web.xml file according to " + selectedContainer + " properties");

        if (withDataSource) {
            fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + CoreWebXml.PATH), CoreWebXml.
                    withDataSource());
            switch (selectedContainer) {
                case JBOSS:
                    fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + CoreWebXml.PATH),
                            CoreWebXml.withDataSourceForJBoss());
                    fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId
                            + PersistenceContextEMFactoryXml.PATH), PersistenceContextEMFactoryXml.FILE);
                    fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId
                            + JBossDeploymentStructureXml.PATH),
                            String.format(JBossDeploymentStructureXml.FILE, jbossJdbcModuleName));
                    break;
                case GLASSFISH:
                    fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + GlassfishCoreWebXml.PATH),
                            GlassfishCoreWebXml.withDataSource());
                    break;
            }
        }

        final MavenUtils mavenUtils = new MavenUtils(mavenDir, handler);
        File customMavenProxySettings = null;
        try {
            if (isProxyEnabled && mavenProxyAutoconf) {
                customMavenProxySettings = MavenUtils.createSettingsWithProxy(installPath, proxyHost, proxyPort,
                        proxyUser, proxyPwd);
            }
        } catch (IOException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "I/O error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        } catch (ParserConfigurationException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "Parser configuration error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        } catch (TransformerException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "Transformer error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        } catch (SAXException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "XML parsing error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        }
        mavenUtils.mvnCleanPackageWithProperties(installPath + "/" + artifactId, confDirectory, logsDirectory, bundlesDirectory,
                customMavenProxySettings);
        if (isProxyEnabled && mavenProxyAutoconf) {
            FileSystemUtils.delete(customMavenProxySettings);
        }

        switch (selectedContainer) {
            case TOMCAT:
                final Tomcat tomcat = new Tomcat(
                        tomcatSsl, tomcatHost, tomcatPort, installPath, artifactId, tomcatUser, tomcatPassword, handler);
                boolean deployCoreResult = tomcat.deployCore();
                if (deployCoreResult) {
                    handler.logOutput("Core successfully deployed ", true);
                    InstallLog.getInstance().info("Core successfully deployed ");
                } else {
                    final String messageError = "Deploy core on Tomcat failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }

                boolean deployConsoleResult = tomcat.deployConsole();
                if (deployConsoleResult) {
                    handler.logOutput("Console successfully deployed ", true);
                    InstallLog.getInstance().info("Console successfully deployed ");
                } else {
                    final String messageError = "Deploy console on Tomcat failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }
                break;
            case JBOSS:
                final JBoss jBoss = new JBoss(
                        jbossSsl, jbossHost, jbossPort, jbossAdminUsername,
                        jbossAdminPassword, installPath, artifactId, handler);

                boolean deployCoreJboss = jBoss.deployCore();
                if (deployCoreJboss) {
                    handler.logOutput("Core successfully deployed ", true);
                    InstallLog.getInstance().info("Core successfully deployed ");
                } else {
                    final String messageError = "Deploy core on JBoss failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }

                boolean deployConsoleJBoss = jBoss.deployConsole();
                if (deployConsoleJBoss) {
                    handler.logOutput("Console successfully deployed ", true);
                    InstallLog.getInstance().info("Console successfully deployed ");
                } else {
                    final String messageError = "Deploy console on JBoss failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }
                break;
            case GLASSFISH:
                final String createJavaOptCommand = "sh " + glassfishDir + Glassfish.CREATE_JAVA_OPT_COMMAND;
                fileSystemUtils.exec(createJavaOptCommand, null);

                final Glassfish glassfish = new Glassfish(installPath, artifactId);

                fileSystemUtils.exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND + glassfish.deployCore(), null);
                fileSystemUtils.exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND + glassfish.deployConsole(), null);
                break;
        }
    }

}
