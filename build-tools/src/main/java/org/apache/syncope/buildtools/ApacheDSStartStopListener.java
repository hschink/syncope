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
package org.apache.syncope.buildtools;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start and stop an embedded ApacheDS instance alongside with Servlet Context.
 */
public class ApacheDSStartStopListener implements ServletContextListener {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ApacheDSStartStopListener.class);

    private DirectoryService service;

    private LdapServer server;

    /**
     * Initialize the schema manager and add the schema partition to directory service.
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition(final ServletContext servletContext) throws Exception {
        final Pattern sharedLdapSchemaManagerPattern = Pattern.compile(".*apacheds-all-.*\\.jar");
        File found = null;
        for (final File jarFile : new File(servletContext.getRealPath("/WEB-INF/lib")).listFiles()) {
            if (sharedLdapSchemaManagerPattern.matcher(jarFile.getAbsolutePath()).matches()) {
                found = jarFile;
            }
        }
        if (found == null) {
            throw new RuntimeException("No apache-ds-all JAR found under WEB-INF/lib");
        }

        final SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        final LdifPartition ldifPartition = new LdifPartition();
        final String workingDirectory = service.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workingDirectory + "/schema");

        // Extract the schema on disk (a brand new one) and load the registries
        final File schemaRepository = new File(workingDirectory, "schema");
        final SchemaLdifExtractor extractor = new JarSchemaLdifExtractor(new File(workingDirectory), found);
        extractor.extractOrCopy(true);

        schemaPartition.setWrappedPartition(ldifPartition);

        final SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
        final SchemaManager schemaManager = new DefaultSchemaManager(loader);
        service.setSchemaManager(schemaManager);

        // Enable nis so that posixAccount and posixGroup are available
        schemaManager.enable("nis");
        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager(schemaManager);

        final List<Throwable> errors = schemaManager.getErrors();
        if (!errors.isEmpty()) {
            throw new RuntimeException("Schema load failed : " + errors);
        }
    }

    /**
     * Add a new partition to the server.
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition(final String partitionId, final String partitionDn) throws Exception {
        // Create a new partition named 'foo'.
        final JdbmPartition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setPartitionDir(new File(service.getWorkingDirectory(), partitionId));
        partition.setSuffix(partitionDn);
        service.addPartition(partition);

        return partition;
    }

    /**
     * Add a new set of index on the given attributes.
     *
     * @param partition The partition on which we want to add index
     * @param attrs The list of attributes to index
     */
    private void addIndex(final Partition partition, final String... attrs) {
        // Index some attributes on the apache partition
        final HashSet<Index<?, ServerEntry, Long>> indexedAttributes = new HashSet<Index<?, ServerEntry, Long>>();
        for (String attribute : attrs) {
            indexedAttributes.add(new JdbmIndex<String, ServerEntry>(attribute));
        }

        ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
    }

    /**
     * Initialize the server. It creates the partition, adds the index, and injects the context entries for the created
     * partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @param loadDefaultContent if default content should be loaded
     * @throws Exception if there were some problems while initializing
     */
    private void initDirectoryService(final ServletContext servletContext, final File workDir,
            final boolean loadDefaultContent) throws Exception {

        // Initialize the LDAP service
        service = new DefaultDirectoryService();
        service.setWorkingDirectory(workDir);

        // first load the schema
        initSchemaPartition(servletContext);

        // then the system partition
        // this is a MANDATORY partition
        final Partition systemPartition = addPartition("system", ServerDNConstants.SYSTEM_DN);
        service.setSystemPartition(systemPartition);

        // Disable the ChangeLog system
        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);

        // Now we can create as many partitions as we need
        final Partition ispPartition = addPartition("isp", "o=isp");
        addIndex(ispPartition, "objectClass", "ou", "uid");

        // And start the service
        service.startup();

        // Finally, load content LDIF
        if (loadDefaultContent) {
            final LdifURLLoader contentLoader = new LdifURLLoader(service.getAdminSession(),
                    servletContext.getResource("/WEB-INF/classes/content.ldif"));
            final int numEntries = contentLoader.execute();
            LOG.info("Successfully created {} entries", numEntries);
        }
    }

    /**
     * Startup ApacheDS embedded.
     *
     * @param sce ServletContext event
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        File workDir = (File) sce.getServletContext().getAttribute("javax.servlet.context.tempdir");
        workDir = new File(workDir, "server-work");

        final boolean loadDefaultContent = !workDir.exists();

        if (loadDefaultContent && !workDir.mkdirs()) {
            throw new RuntimeException("Could not create " + workDir.getAbsolutePath());
        }

        Entry result;
        try {
            initDirectoryService(sce.getServletContext(), workDir, loadDefaultContent);

            server = new LdapServer();
            server.setTransports(
                    new TcpTransport(Integer.valueOf(sce.getServletContext().getInitParameter("testds.port"))));
            server.setDirectoryService(service);

            server.start();

            // store directoryService in context to provide it to servlets etc.
            sce.getServletContext().setAttribute(DirectoryService.JNDI_KEY, service);

            result = service.getAdminSession().lookup(new DN("o=isp"));
        } catch (Exception e) {
            LOG.error("Fatal error in context init", e);
            throw new RuntimeException(e);
        }

        if (result == null) {
            throw new RuntimeException("Base DN not found");
        } else {
            LOG.info("ApacheDS startup completed succesfully");
        }
    }

    /**
     * Shutdown ApacheDS embedded.
     *
     * @param scEvent ServletContext event
     */
    @Override
    public void contextDestroyed(final ServletContextEvent scEvent) {
        try {
            if (server != null) {
                server.stop();
            }
            if (service != null) {
                service.shutdown();
            }
        } catch (Exception e) {
            LOG.error("Fatal error in context shutdown", e);
            throw new RuntimeException(e);
        }
    }
}
