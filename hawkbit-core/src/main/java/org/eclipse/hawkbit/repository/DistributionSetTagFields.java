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
public enum DistributionSetTagFields implements RsqlQueryField {

  ID(TagFields.ID.getJpaEntityFieldName()),
  NAME(TagFields.NAME.getJpaEntityFieldName()),
  DESCRIPTION(TagFields.DESCRIPTION.getJpaEntityFieldName()),
  COLOUR(TagFields.COLOUR.getJpaEntityFieldName()),
  DISTRIBUTIONSET("assignedToDistributionSet",
      DistributionSetFields.ID.getJpaEntityFieldName(), DistributionSetFields.NAME.getJpaEntityFieldName());

  private final String jpaEntityFieldName;
  private final List<String> subEntityAttributes;

  DistributionSetTagFields(final String jpaEntityFieldName) {
    this.jpaEntityFieldName = jpaEntityFieldName;
    this.subEntityAttributes = Collections.emptyList();
  }

  DistributionSetTagFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
    this.jpaEntityFieldName = jpaEntityFieldName;
    this.subEntityAttributes = List.of(subEntityAttributes);
  }
}