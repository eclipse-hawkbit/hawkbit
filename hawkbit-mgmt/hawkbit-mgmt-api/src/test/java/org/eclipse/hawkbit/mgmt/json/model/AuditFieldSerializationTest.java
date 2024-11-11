/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Management API")
@Story("Serialization")
public class AuditFieldSerializationTest {

    @Test
    public void assertAuditingFields() throws JsonProcessingException {
        final MgmtTarget mgmtTarget = new MgmtTarget();
        mgmtTarget.setCreatedBy("user");
        mgmtTarget.setCreatedAt(System.currentTimeMillis() - 1_000_000);
        mgmtTarget.setLastModifiedBy("user2");
        mgmtTarget.setLastModifiedAt(System.currentTimeMillis());
        final ObjectMapper objectMapper = new ObjectMapper();
        final String serialized = objectMapper.writeValueAsString(mgmtTarget);
        final MgmtTarget mgmtTargetDeserialization = objectMapper.readValue(serialized, MgmtTarget.class);
        assertThat(mgmtTargetDeserialization).isEqualTo(mgmtTarget);
    }
}