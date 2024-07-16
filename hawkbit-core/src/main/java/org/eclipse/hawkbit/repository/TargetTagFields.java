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
 * Additionally, here were added fields for Target in order
 * filtering over target fields also.
 */
public enum TargetTagFields implements FieldNameProvider {
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
  COLOUR(TagFields.COLOUR.getFieldName());

  private final String fieldName;

  TargetTagFields(final String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public String getFieldName() {
    return fieldName;
  }
}