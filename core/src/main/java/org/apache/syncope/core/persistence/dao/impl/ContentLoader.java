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
import java.io.InputStream;
import javax.annotation.Resource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.beans.conf.SyncopeConf;
import org.apache.syncope.core.util.ContentLoaderHandler;
import org.apache.syncope.core.util.ResourceWithFallbackLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialize Database with default content if no data is present already.
 */
@Component
public class ContentLoader extends AbstractContentDealer {

    @Resource(name = "contentXML")
    private ResourceWithFallbackLoader contentXML;

    @Transactional
    public void load() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        boolean existingData;
        try {
            existingData = jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + SyncopeConf.class.getSimpleName(),
                    Integer.class) > 0;
        } catch (DataAccessException e) {
            LOG.error("Could not access to table " + SyncopeConf.class.getSimpleName(), e);
            existingData = true;
        }

        if (existingData) {
            LOG.info("Data found in the database, leaving untouched");
        } else {
            LOG.info("Empty database found, loading default content");

            try {
                loadDefaultContent();
            } catch (Exception e) {
                LOG.error("While loading default content", e);
            }
            try {
                createIndexes();
                createViews();
            } catch (IOException e) {
                LOG.error("While creating indexes and views", e);
            }
        }
    }

    private void loadDefaultContent() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        InputStream in = null;
        try {
            in = contentXML.getResource().getInputStream();

            SAXParser parser = factory.newSAXParser();
            parser.parse(in, new ContentLoaderHandler(dataSource, ROOT_ELEMENT));
            LOG.debug("Default content successfully loaded");
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
