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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Tag model which can be used in the REST API e.g.
 * for sorting etc.
 * Additionally here were added fields for DistributionSet in order
 * filtering over distribution set fields also.
 */
public enum DistributionSetTagFields implements FieldNameProvider {
  /**
   * The id field.
   */
  ID(TagFields.ID.getFieldName()),

  /**
   * The name field.
   */
  NAME(TagFields.NAME.getFieldName()),
  /**
   * The description field.
   */
  DESCRIPTION(TagFields.DESCRIPTION.getFieldName()),
  /**
   * The controllerId field.
   */
  COLOUR(TagFields.COLOUR.getFieldName()),

  /**
   * Distribution set fields
   */
  DISTRIBUTIONSET("assignedToDistributionSet",
      DistributionSetFields.ID.getFieldName(), DistributionSetFields.NAME.getFieldName());

  private final String fieldName;

  private final List<String> subEntityAttributes;

  private DistributionSetTagFields(final String fieldName) {
    this.fieldName = fieldName;
    this.subEntityAttributes = Collections.emptyList();
  }

  private DistributionSetTagFields(final String fieldName, final String... subEntityAttributes) {
    this.fieldName = fieldName;
    this.subEntityAttributes = Arrays.asList(subEntityAttributes);
  }

  @Override
  public List<String> getSubEntityAttributes() {
    return Collections.unmodifiableList(subEntityAttributes);
  }

  @Override
  public String getFieldName() {
    return fieldName;
  }
}