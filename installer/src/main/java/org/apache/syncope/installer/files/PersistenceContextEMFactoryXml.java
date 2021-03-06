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
package org.apache.syncope.installer.files;

public class PersistenceContextEMFactoryXml {

    public static final String PATH = "/core/src/main/resources/persistenceContextEMFactory.xml";

    public static final String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!--\n"
            + "Licensed to the Apache Software Foundation (ASF) under one\n"
            + "or more contributor license agreements.  See the NOTICE file\n"
            + "distributed with this work for additional information\n"
            + "regarding copyright ownership.  The ASF licenses this file\n"
            + "to you under the Apache License, Version 2.0 (the\n"
            + "\"License\"); you may not use this file except in compliance\n"
            + "with the License.  You may obtain a copy of the License at\n" + "\n"
            + "  http://www.apache.org/licenses/LICENSE-2.0\n" + "\n"
            + "Unless required by applicable law or agreed to in writing,\n"
            + "software distributed under the License is distributed on an\n"
            + "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n"
            + "KIND, either express or implied.  See the License for the\n"
            + "specific language governing permissions and limitations\n" + "under the License.\n" + "-->\n"
            + "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n"
            + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "       xsi:schemaLocation=\"http://www.springframework.org/schema/beans\n"
            + "       http://www.springframework.org/schema/beans/spring-beans.xsd\">\n" + "\n"
            + "  <bean id=\"entityManagerFactory\"\n"
            + "        class=\"org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean\">\n"
            + "    <property name=\"persistenceXmlLocation\" value=\"classpath*:META-INF/spring-persistence.xml\"/>\n"
            + "    <property name=\"persistenceUnitName\" value=\"syncopePersistenceUnit\"/>\n"
            + "    <property name=\"dataSource\" ref=\"dataSource\"/>\n" + "    <property name=\"jpaVendorAdapter\">\n"
            + "      <bean class=\"org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter\">\n"
            + "        <property name=\"showSql\" value=\"false\"/>\n"
            + "        <property name=\"generateDdl\" value=\"true\"/>\n"
            + "        <property name=\"databasePlatform\" value=\"${jpa.dialect}\"/>\n" + "      </bean>\n"
            + "    </property>\n" + "    <property name=\"jpaPropertyMap\">\n" + "      <map>\n"
            + "        <!--<entry key=\"openjpa.Log\" value=\"SQL=TRACE\"/>\n"
            + "        <entry key=\"openjpa.ConnectionFactoryProperties\" value=\"PrettyPrint=true, PrettyPrintLineLength=80\"/>-->\n"
            + "                \n" + "        <entry key=\"openjpa.NontransactionalWrite\" value=\"false\"/>\n"
            + "        <entry key=\"openjpa.AutoDetach\" value=\"close, commit, nontx-read, rollback\"/>\n" + "\n"
            + "        <entry key=\"openjpa.jdbc.SchemaFactory\" value=\"native(ForeignKeys=true)\"/>\n"
            + "        <entry key=\"openjpa.jdbc.MappingDefaults\" value=\"ForeignKeyDeleteAction=restrict, JoinForeignKeyDeleteAction=restrict\"/>\n"
            + "                \n" + "        <entry key=\"openjpa.ReadLockLevel\" value=\"none\"/>\n"
            + "        <entry key=\"openjpa.WriteLockLevel\" value=\"write\"/>\n"
            + "        <entry key=\"openjpa.LockTimeout\" value=\"30000\"/>\n" + "                                \n"
            + "        <entry key=\"openjpa.DataCache\" value=\"true\" />\n"
            + "        <entry key=\"openjpa.QueryCache\" value=\"true\"/>\n"
            + "        <entry key=\"openjpa.RemoteCommitProvider\" value=\"sjvm\"/>\n" + "        \n"
            + "        <entry key=\"openjpa.MetaDataFactory\" value=\"jpa(URLs=vfs:/content/${project.build.finalName}.war/WEB-INF/classes/, Resources=META-INF/orm.xml)\"/>\n"
            + "      </map>\n" + "    </property>\n" + "  </bean>\n" + "\n" + "</beans>";

}
