/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.hawkbit.dmf.DmfMessageConverter;
import org.eclipse.hawkbit.dmf.json.model.DmfActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfBatchDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfConfirmRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DmfMessageConverterTest {

    private static final String EXTERNAL_REF = "external-system-reference";

    private final DmfMessageConverter converter = new DmfMessageConverter();
    private final JsonMapper mapper = new JsonMapper();

    @ParameterizedTest
    @MethodSource("legacyRequests")
    void readsLegacyPayloadsWithoutExternalRef(final Object expected, final String json) {
        final MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", expected.getClass().getName());

        final Object request = converter.fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), properties));

        assertThat(request).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("legacyRequests")
    void legacyConstructorsPreservePayloadAndOmitNullExternalRef(final Object request, final String json) {
        final Message message = converter.toMessage(request, new MessageProperties());
        final JsonNode payload = mapper.readTree(message.getBody());

        assertThat(payload.has("externalRef")).isFalse();
        assertThat(payload).isEqualTo(mapper.readTree(json));
    }

    @ParameterizedTest
    @MethodSource("requestsWithExternalRef")
    void roundTripsExternalRef(final Object request) {
        final Message message = converter.toMessage(request, new MessageProperties());

        assertThat(mapper.readTree(message.getBody()).path("externalRef").asString()).isEqualTo(EXTERNAL_REF);
        assertThat(converter.fromMessage(message)).isEqualTo(request);
    }

    @Test
    void roundTripsBatchWithAndWithoutExternalRefs() {
        final DmfBatchDownloadAndUpdateRequest request = new DmfBatchDownloadAndUpdateRequest(1L, List.of(
                new DmfTarget(1L, "target-1", "token-1", EXTERNAL_REF),
                new DmfTarget(2L, "target-2", "token-2", "another-reference"),
                new DmfTarget(3L, "target-3", "token-3", null)), List.of());

        final Message message = converter.toMessage(request, new MessageProperties());
        final JsonNode payload = mapper.readTree(message.getBody());
        final JsonNode targets = payload.path("targets");

        assertThat(payload.has("externalRef")).isFalse();
        assertThat(targets.size()).isEqualTo(3);
        assertThat(targets.get(0).path("externalRef").asString()).isEqualTo(EXTERNAL_REF);
        assertThat(targets.get(1).path("externalRef").asString()).isEqualTo("another-reference");
        assertThat(targets.get(2).has("externalRef")).isFalse();
        assertThat(converter.fromMessage(message)).isEqualTo(request);
    }

    private static Stream<Arguments> legacyRequests() {
        return Stream.of(
                Arguments.of(new DmfActionRequest(1L), """
                        {"actionId":1}
                        """),
                Arguments.of(new DmfDownloadAndUpdateRequest(1L, "token", List.of()), """
                        {"actionId":1,"targetSecurityToken":"token","softwareModules":[]}
                        """),
                Arguments.of(new DmfConfirmRequest(1L, "token", List.of()), """
                        {"actionId":1,"targetSecurityToken":"token","softwareModules":[]}
                        """),
                Arguments.of(new DmfTarget(1L, "target", "token"), """
                        {"actionId":1,"controllerId":"target","targetSecurityToken":"token"}
                        """));
    }

    private static Stream<Object> requestsWithExternalRef() {
        return Stream.of(
                new DmfActionRequest(1L, EXTERNAL_REF),
                new DmfDownloadAndUpdateRequest(1L, "token", List.of(), EXTERNAL_REF),
                new DmfConfirmRequest(1L, "token", List.of(), EXTERNAL_REF),
                new DmfTarget(1L, "target", "token", EXTERNAL_REF));
    }
}


