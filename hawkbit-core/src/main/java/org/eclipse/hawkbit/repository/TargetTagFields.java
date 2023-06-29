/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Tag model which can be used in the REST API e.g.
 * for sorting etc.
 * Additionally here were added fields for Target in order
 * filtering over target fields also.
 */
public enum TargetTagFields implements FieldNameProvider {
  /**
   * The id field.
   */
  ID("id"),

  /**
   * The name field.
   */
  NAME("name"),
  /**
   * The description field.
   */
  DESCRIPTION("description"),
  /**
   * The controllerId field.
   */
  COLOUR("colour"),

  /**
   * Target fields
   */
  TARGET("assignedToTargets",
      TargetFields.ID.getFieldName(), TargetFields.NAME.getFieldName());

  private final String fieldName;

  private final List<String> subEntityAttributes;

  private TargetTagFields(final String fieldName) {
    this.fieldName = fieldName;
    this.subEntityAttributes = Collections.emptyList();
  }

  private TargetTagFields(final String fieldName, final String... subEntityAttributes) {
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