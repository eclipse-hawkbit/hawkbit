/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

/**
 * Builder class for building certain json strings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class JsonBuilder {

    static JSONObject configData(final Map<String, String> attributes) throws JSONException {
        return configData(attributes, null);
    }

    static JSONObject configData(final Map<String, String> attributes, final String mode) throws JSONException {
        final JSONObject data = new JSONObject();
        attributes.forEach((key, value) -> {
            try {
                data.put(key, value);
            } catch (final JSONException e) {
                log.error("JSONException (skip)", e);
            }
        });

        final JSONObject json = new JSONObject().put("data", data);
        if (mode != null) {
            json.put("mode", mode);
        }
        return json;
    }
}