/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence;

import java.util.List;
import java.util.Set;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.client.to.PropertyTO;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;
import org.syncope.core.persistence.util.ApplicationContextManager;
import org.syncope.core.rest.controller.ConnectorInstanceController;
import org.syncope.core.rest.data.ConnectorInstanceDataBinder;

public class ConnectorInstanceBeansLoader implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(
            ConnectorInstanceBeansLoader.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        ConnectorInstanceDAO connectorInstanceDAO =
                (ConnectorInstanceDAO) context.getBean(
                "connectorInstanceDAOImpl");

        SyncopeConfigurationDAO syncopeConfigurationDAO =
                (SyncopeConfigurationDAO) context.getBean(
                "syncopeConfigurationDAOImpl");

        SyncopeConfiguration syncopeConfiguration = null;
        try {
            syncopeConfiguration = syncopeConfigurationDAO.find(
                    "identityconnectors.bundle.directory");
        } catch (MissingConfKeyException e) {
            log.error("Missing configuration", e);
        }

        List<ConnectorInstance> instances = connectorInstanceDAO.findAll();
        Set<PropertyTO> properties = null;

        for (ConnectorInstance instance : instances) {
            try {

                properties = (Set<PropertyTO>) ConnectorInstanceDataBinder.buildFromXML(
                        instance.getXmlConfiguration());

                ConnectorInfoManager manager =
                        ConnectorInstanceController.getConnectorManager(
                        syncopeConfiguration.getConfValue());

                ConnectorFacade connector =
                        ConnectorInstanceController.getConnectorFacade(
                        manager,
                        instance.getBundleName(),
                        instance.getVersion(),
                        instance.getConnectorName(),
                        properties);

                if (log.isInfoEnabled()) {
                    log.info("Connector instance " + connector);
                }

                beanFactory.registerSingleton(
                        instance.getId().toString(), connector);

                if (log.isInfoEnabled()) {
                    log.info("Registered bean " + instance.getId().toString());
                }

            } catch (Throwable t) {
                log.error("While loading bundles", t);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
