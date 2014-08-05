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
package org.apache.syncope.common.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.annotation.SchemaList;

@XmlType
public class AccountPolicySpec extends AbstractPolicySpec {

    private static final long serialVersionUID = 3259256974414758406L;

    /**
     * Minimum length.
     */
    private int maxLength;

    /**
     * Maximum length.
     */
    private int minLength;

    /**
     * Pattern (regular expression) that must match.
     */
    private String pattern;

    /**
     * Substrings not permitted.
     */
    private List<String> wordsNotPermitted;

    /**
     * User attribute values not permitted.
     */
    @SchemaList
    private List<String> schemasNotPermitted;

    /**
     * Substrings not permitted as prefix.
     */
    private List<String> prefixesNotPermitted;

    /**
     * Substrings not permitted as suffix.
     */
    private List<String> suffixesNotPermitted;

    /**
     * Specify if one or more lowercase characters are permitted.
     */
    private boolean allUpperCase;

    /**
     * Specify if one or more uppercase characters are permitted.
     */
    private boolean allLowerCase;

    /**
     * Specify if, when reached the maximum allowed number of subsequent login failures, user shall be suspended.
     */
    private boolean propagateSuspension;

    /**
     * Number of permitted login retries.
     * 0 disabled; &gt;0 enabled.
     * If the number of subsequent failed logins will be greater then this value
     * the account will be suspended (lock-out).
     */
    private int permittedLoginRetries;

    public boolean isAllLowerCase() {
        return allLowerCase;
    }

    public void setAllLowerCase(final boolean allLowerCase) {
        this.allLowerCase = allLowerCase;
    }

    public boolean isAllUpperCase() {
        return allUpperCase;
    }

    public void setAllUpperCase(final boolean allUpperCase) {
        this.allUpperCase = allUpperCase;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(final int minLength) {
        this.minLength = minLength;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    @XmlElementWrapper(name = "prefixesNotPermitted")
    @XmlElement(name = "prefix")
    @JsonProperty("prefixesNotPermitted")
    public List<String> getPrefixesNotPermitted() {
        if (prefixesNotPermitted == null) {
            prefixesNotPermitted = new ArrayList<String>();
        }
        return prefixesNotPermitted;
    }

    @XmlElementWrapper(name = "schemasNotPermitted")
    @XmlElement(name = "schema")
    @JsonProperty("schemasNotPermitted")
    public List<String> getSchemasNotPermitted() {
        if (schemasNotPermitted == null) {
            schemasNotPermitted = new ArrayList<String>();
        }
        return schemasNotPermitted;
    }

    @XmlElementWrapper(name = "suffixesNotPermitted")
    @XmlElement(name = "suffix")
    @JsonProperty("suffixesNotPermitted")
    public List<String> getSuffixesNotPermitted() {
        if (suffixesNotPermitted == null) {
            suffixesNotPermitted = new ArrayList<String>();
        }
        return suffixesNotPermitted;
    }

    @XmlElementWrapper(name = "wordsNotPermitted")
    @XmlElement(name = "word")
    @JsonProperty("wordsNotPermitted")
    public List<String> getWordsNotPermitted() {
        if (wordsNotPermitted == null) {
            wordsNotPermitted = new ArrayList<String>();
        }
        return wordsNotPermitted;
    }

    public boolean isPropagateSuspension() {
        return propagateSuspension;
    }

    public void setPropagateSuspension(final boolean propagateSuspension) {
        this.propagateSuspension = propagateSuspension;
    }

    public int getPermittedLoginRetries() {
        return permittedLoginRetries;
    }

    public void setPermittedLoginRetries(final int permittedLoginRetries) {
        this.permittedLoginRetries = permittedLoginRetries;
    }
}
