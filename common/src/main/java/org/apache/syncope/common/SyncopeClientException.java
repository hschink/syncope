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
package org.apache.syncope.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.types.ClientExceptionType;

public class SyncopeClientException extends RuntimeException {

    private static final long serialVersionUID = 3380920886511913475L;

    private ClientExceptionType type;

    private final List<String> elements = new ArrayList<String>();

    public static SyncopeClientException build(final ClientExceptionType type) {
        if (type == ClientExceptionType.Composite) {
            throw new IllegalArgumentException("Composite exceptions must be obtained via buildComposite()");
        }
        return new SyncopeClientException(type);
    }

    public static SyncopeClientCompositeException buildComposite() {
        return new SyncopeClientCompositeException();
    }

    protected SyncopeClientException(final ClientExceptionType type) {
        super();
        setType(type);
    }

    public boolean isComposite() {
        return getType() == ClientExceptionType.Composite;
    }

    public SyncopeClientCompositeException asComposite() {
        if (!isComposite()) {
            throw new IllegalArgumentException("This is not a composite exception");
        }

        return (SyncopeClientCompositeException) this;
    }

    public ClientExceptionType getType() {
        return type;
    }

    public final void setType(final ClientExceptionType type) {
        this.type = type;
    }

    public List<String> getElements() {
        return elements;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder();

        message.append(getType());
        message.append(" ");
        message.append(getElements());

        return message.toString();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

}
