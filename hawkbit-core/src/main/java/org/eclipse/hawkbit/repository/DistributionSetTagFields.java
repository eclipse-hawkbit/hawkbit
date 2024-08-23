/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Tag model which can be used in the REST API e.g.
 * for sorting etc.
 * Additionally, here were added fields for DistributionSet in order
 * filtering over distribution set fields also.
 */
@Getter
public enum DistributionSetTagFields implements FieldNameProvider {

  ID(TagFields.ID.getFieldName()),
  NAME(TagFields.NAME.getFieldName()),
  DESCRIPTION(TagFields.DESCRIPTION.getFieldName()),
  COLOUR(TagFields.COLOUR.getFieldName()),
  DISTRIBUTIONSET("assignedToDistributionSet",
      DistributionSetFields.ID.getFieldName(), DistributionSetFields.NAME.getFieldName());

  private final String fieldName;
  private final List<String> subEntityAttributes;

  DistributionSetTagFields(final String fieldName) {
    this.fieldName = fieldName;
    this.subEntityAttributes = Collections.emptyList();
  }

  DistributionSetTagFields(final String fieldName, final String... subEntityAttributes) {
    this.fieldName = fieldName;
    this.subEntityAttributes = List.of(subEntityAttributes);
  }
}