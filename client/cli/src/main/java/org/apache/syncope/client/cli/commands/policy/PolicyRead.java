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
package org.apache.syncope.client.cli.commands.policy;

import java.util.LinkedList;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyRead extends AbstractPolicyCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyRead.class);

    private static final String READ_HELP_MESSAGE = "policy --read {POLICY-ID} {POLICY-ID} [...]";

    private final Input input;

    public PolicyRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            final LinkedList<AbstractPolicyTO> policyTOs = new LinkedList<>();
            for (final String parameter : input.getParameters()) {
                try {
                    policyTOs.add(policySyncopeOperations.read(parameter));
                } catch (final NumberFormatException ex) {
                    LOG.error("Error reading policy", ex);
                    policyResultManager.notBooleanDeletedError("policy", parameter);
                } catch (final WebServiceException | SyncopeClientException ex) {
                    LOG.error("Error reading policy", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        policyResultManager.notFoundError("Policy", parameter);
                    } else {
                        policyResultManager.genericError(ex.getMessage());
                    }
                }
            }
            policyResultManager.printPolicies(policyTOs);
        } else {
            policyResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
