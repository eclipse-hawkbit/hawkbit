/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiArtifact'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiArtifactTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String filename = "testfile.txt";
        DdiArtifactHash hashes = new DdiArtifactHash("123", "456", "789");
        Long size = 12345L;

        DdiArtifact ddiArtifact = new DdiArtifact();
        ddiArtifact.setFilename(filename);
        ddiArtifact.setHashes(hashes);
        ddiArtifact.setSize(size);

        // Test
        String serializedDdiArtifact = mapper.writeValueAsString(ddiArtifact);
        DdiArtifact deserializedDdiArtifact = mapper.readValue(serializedDdiArtifact, DdiArtifact.class);

        assertThat(serializedDdiArtifact).contains(filename, "12345");
        assertThat(deserializedDdiArtifact.getFilename()).isEqualTo(filename);
        assertThat(deserializedDdiArtifact.getSize()).isEqualTo(size);
        assertThat(deserializedDdiArtifact.getHashes().getSha1()).isEqualTo(hashes.getSha1());
        assertThat(deserializedDdiArtifact.getHashes().getMd5()).isEqualTo(hashes.getMd5());
        assertThat(deserializedDdiArtifact.getHashes().getSha256()).isEqualTo(hashes.getSha256());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiArtifact = "{\"filename\":\"test.file\",\"hashes\":{\"sha1\":\"123\",\"md5\":\"456\",\"sha256\":\"789\"},\"size\":111,\"links\":[],\"unknownProperty\": \"test\"}";

        // Test
        DdiArtifact ddiArtifact = mapper.readValue(serializedDdiArtifact, DdiArtifact.class);

        assertThat(ddiArtifact.getFilename()).isEqualTo("test.file");
        assertThat(ddiArtifact.getSize()).isEqualTo(111);
        assertThat(ddiArtifact.getHashes().getSha1()).isEqualTo("123");
        assertThat(ddiArtifact.getHashes().getMd5()).isEqualTo("456");
        assertThat(ddiArtifact.getHashes().getSha256()).isEqualTo("789");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiArtifact = "{\"filename\": [\"test.file\"],\"hashes\":{\"sha1\":\"123\",\"md5\":\"456\",\"sha256\":\"789\"},\"size\":111,\"links\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiArtifact, DdiArtifact.class));
    }
}