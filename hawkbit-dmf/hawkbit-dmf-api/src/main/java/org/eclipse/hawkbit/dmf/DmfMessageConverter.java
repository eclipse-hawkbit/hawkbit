/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf;

import java.util.Arrays;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.json.JsonMapper;

// - since spring-amqp 4.0.4 not all packages are assumed trusted for type converter (only java.land and java.util)
// so e need to add hawkbit DMF model package (and eventual extension packages) as trusted
// - also (again since spring-amqp 4.0.4) the conversion from empty payload fail (which probably is fine since it is JSON)
// however, (for backward compatibility, e.g. THING_REMOVED doesn't define payload and could be empty byte[]) we assume that
// empty payload is empty byte[] and not try to convert it to Object (which fail since it is not JSON)
public class DmfMessageConverter extends JacksonJsonMessageConverter {

    private static final String DMF_JSON_MODEL_PACKAGE = "org.eclipse.hawkbit.dmf.json.model";

    /**
     * Constructor unsing default {@link JsonMapper}, i.e. <code>new JsonMapper()</code>
     *
     * @param trustedPackagesExt {@link DmfMessageConverter} always trust {@link #DMF_JSON_MODEL_PACKAGE}. If any additional packages
     *         shall be trusted provided here
     */
    public DmfMessageConverter(final String... trustedPackagesExt) {
        this(new JsonMapper(), trustedPackagesExt);
    }

    /**
     * Constructor with specified {@link JsonMapper}
     *
     * @param jsonMapper the {@link JsonMapper} to use for conversion
     * @param trustedPackagesExt {@link DmfMessageConverter} always trust {@link #DMF_JSON_MODEL_PACKAGE}. If any additional packages
     *         shall be trusted provided here
     */
    public DmfMessageConverter(final JsonMapper jsonMapper, final String... trustedPackagesExt) {
        super(
                jsonMapper,
                trustedPackagesExt == null || trustedPackagesExt.length == 0
                        ? new String[] { DMF_JSON_MODEL_PACKAGE }
                        : Stream.concat(Stream.of(DMF_JSON_MODEL_PACKAGE), Arrays.stream(trustedPackagesExt))
                                .distinct()
                                .toArray(String[]::new));
    }

    @Override
    public @NonNull Object fromMessage(@NonNull final Message message, final @Nullable Object conversionHint) {
        // default converter tries to convert empty body payload to Object (since rabbit 4.0.4)
        // which probably is correct since it has to be JSON - however, in this case we assume - empty byte[]
        if (message.getBody().length == 0) {
            return message.getBody();
        } else {
            return super.fromMessage(message, conversionHint);
        }
    }
}