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
package org.apache.syncope.client.cli.commands.anyobject;

import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyObjectReadAttributeBySchemaTypeAndSchemaName extends AbstractAnyObjectCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AnyObjectReadAttributeBySchemaTypeAndSchemaName.class);

    private static final String READ_HELP_MESSAGE
            = "any --read-attr-by-schema {ANY-ID} {SCHEMA-TYPE} {SCHEMA-NAME}\n"
            + "   Schema type: PLAIN / DERIVED / VIRTUAL";

    private final Input input;

    public AnyObjectReadAttributeBySchemaTypeAndSchemaName(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() == 3) {
            try {
                anyResultManager.printAttribute(anySyncopeOperations.readAttribute(
                        input.firstParameter(), input.secondParameter(), input.thirdParameter()));
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading any object", ex);
                anyResultManager.genericError(ex.getMessage());
            } catch (final NumberFormatException ex) {
                anyResultManager.numberFormatException("any object", input.firstParameter());
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error reading schema", ex);
                anyResultManager.typeNotValidError(
                        "schema", input.secondParameter(), CommandUtils.fromEnumToArray(SchemaType.class));
            }
        } else {
            anyResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
