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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement(name = "attribute")
@XmlType
public class AttributeTO extends AbstractBaseBean {

    private static final long serialVersionUID = 4941691338796323623L;

    /**
     * Name of the schema that this attribute is referring to.
     */
    private String schema;

    /**
     * Set of (string) values of this attribute.
     */
    private final List<String> values;

    /**
     * Whether this attribute is read-only or not.
     */
    private boolean readonly;

    /**
     * Default constructor.
     */
    public AttributeTO() {
        super();
        values = new ArrayList<String>();
        readonly = false;
    }

    /**
     * @return the name of the schema that this attribute is referring to
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema name to be set
     */
    public void setSchema(final String schema) {
        this.schema = schema;

    }

    /**
     * @return attribute values as strings
     */
    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    @JsonProperty("values")
    public List<String> getValues() {
        return values;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
