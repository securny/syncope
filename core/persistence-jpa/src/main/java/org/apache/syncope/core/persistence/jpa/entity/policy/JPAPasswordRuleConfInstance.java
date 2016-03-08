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
package org.apache.syncope.core.persistence.jpa.entity.policy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;

@Entity
@Table(name = JPAPasswordRuleConfInstance.TABLE)
public class JPAPasswordRuleConfInstance extends AbstractEntity<Long> {

    private static final long serialVersionUID = -2436055132955674610L;

    public static final String TABLE = "PasswordRuleConfInstance";

    @Id
    private Long id;

    @Lob
    private String serializedInstance;

    @ManyToOne
    private JPAPasswordPolicy passwordPolicy;

    @Override
    public Long getKey() {
        return id;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final PasswordPolicy report) {
        checkType(report, JPAPasswordPolicy.class);
        this.passwordPolicy = (JPAPasswordPolicy) report;
    }

    public PasswordRuleConf getInstance() {
        return serializedInstance == null
                ? null
                : POJOHelper.deserialize(serializedInstance, PasswordRuleConf.class);
    }

    public void setInstance(final PasswordRuleConf instance) {
        this.serializedInstance = instance == null
                ? null
                : POJOHelper.serialize(instance);
    }
}
