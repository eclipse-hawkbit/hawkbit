/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

@ContextConfiguration(classes = { DdiApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
@TestPropertySource(locations = "classpath:/ddi-test.properties")
public abstract class AbstractDDiApiIntegrationTest extends AbstractRestIntegrationTest {

    /**
     * Convert JSON to a CBOR equivalent.
     * 
     * @param json
     *            JSON object to convert
     * @return Equivalent CBOR data
     * @throws IOException
     *             Invalid JSON input
     */
    protected static byte[] jsonToCbor(String json) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonParser jsonParser = jsonFactory.createParser(json);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CBORFactory cborFactory = new CBORFactory();
        CBORGenerator cborGenerator = cborFactory.createGenerator(out);
        while (jsonParser.nextToken() != null) {
            cborGenerator.copyCurrentEvent(jsonParser);
        }
        cborGenerator.flush();
        return out.toByteArray();
    }

    /**
     * Convert CBOR to JSON equivalent.
     * 
     * @param input
     *            CBOR data to convert
     * @return Equivalent JSON string
     * @throws IOException
     *             Invalid CBOR input
     */
    protected static String cborToJson(byte[] input) throws IOException {
        CBORFactory cborFactory = new CBORFactory();
        CBORParser cborParser = cborFactory.createParser(input);
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(stringWriter);
        while (cborParser.nextToken() != null) {
            jsonGenerator.copyCurrentEvent(cborParser);
        }
        jsonGenerator.flush();
        return stringWriter.toString();
    }
}
