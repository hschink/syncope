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
package org.apache.syncope.core.services;

import java.util.List;

import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.wrap.EntitlementTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.EntitlementController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntitlementServiceImpl extends AbstractServiceImpl implements EntitlementService {

    @Autowired
    private EntitlementController controller;

    @Override
    public List<EntitlementTO> getAllEntitlements() {
        return CollectionWrapper.wrap(controller.getAll(), EntitlementTO.class);
    }

    @Override
    public List<EntitlementTO> getOwnEntitlements() {
        return CollectionWrapper.wrap(controller.getOwn(), EntitlementTO.class);
    }
}
