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
package org.apache.syncope.client.cli.commands.group;

import java.util.List;
import java.util.Set;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.service.GroupService;

public class GroupSyncopeOperations {

    private final GroupService groupService = SyncopeServices.get(GroupService.class);

    public List<GroupTO> list() {
        return groupService.list(new AnyListQuery()).getResult();
    }

    public GroupTO read(final String groupKey) {
        return groupService.read(groupKey);
    }

    public Set<AttrTO> readAttributes(final String groupKey, final String schemaType) {
        return groupService.read(groupKey, SchemaType.valueOf(schemaType));
    }

    public AttrTO readAttribute(final String groupKey, final String schemaType, final String schema) {
        return groupService.read(groupKey, SchemaType.valueOf(schemaType), schema);
    }

    public void delete(final String groupKey) {
        groupService.delete(groupKey);
    }
}
