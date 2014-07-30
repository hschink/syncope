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
package org.apache.syncope.core.persistence.dao.impl;

import java.io.IOException;
import java.util.Properties;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.apache.syncope.core.util.ResourceWithFallbackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractContentDealer {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractContentDealer.class);

    protected static final String ROOT_ELEMENT = "dataset";

    @Resource(name = "database.schema")
    protected String dbSchema;

    @Resource(name = "indexesXML")
    private ResourceWithFallbackLoader indexesXML;

    @Resource(name = "viewsXML")
    private ResourceWithFallbackLoader viewsXML;

    @Autowired
    protected DataSource dataSource;

    protected void createIndexes() throws IOException {
        LOG.debug("Creating indexes");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Properties indexes = PropertiesLoaderUtils.loadProperties(indexesXML.getResource());
        for (String idx : indexes.stringPropertyNames()) {
            LOG.debug("Creating index {}", indexes.get(idx).toString());

            try {
                jdbcTemplate.execute(indexes.get(idx).toString());
            } catch (DataAccessException e) {
                LOG.error("Could not create index ", e);
            }
        }

        LOG.debug("Indexes created");
    }

    protected void createViews() throws IOException {
        LOG.debug("Creating views");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Properties views = PropertiesLoaderUtils.loadProperties(viewsXML.getResource());
        for (String idx : views.stringPropertyNames()) {
            LOG.debug("Creating view {}", views.get(idx).toString());

            try {
                jdbcTemplate.execute(views.get(idx).toString().replaceAll("\\n", " "));
            } catch (DataAccessException e) {
                LOG.error("Could not create view ", e);
            }
        }

        LOG.debug("Ciews created");
    }
}
