/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.documentation;

import java.util.List;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

/**
 * {@link ResponseFieldsSnippet} with public constructor
 */
public class DocumenationResponseFieldsSnippet extends ResponseFieldsSnippet {
    public DocumenationResponseFieldsSnippet(final List<FieldDescriptor> descriptors) {
        super(descriptors);
    }

}
