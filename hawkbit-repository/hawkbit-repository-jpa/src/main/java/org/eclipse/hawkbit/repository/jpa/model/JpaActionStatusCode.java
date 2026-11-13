/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.EnumMap;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.jpa.utils.MapAttributeConverter;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;

/**
 * JPA implementation of an {@link ActionStatusCode}.
 */
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "sp_action_status_code")
public class JpaActionStatusCode extends AbstractJpaNamedEntity implements ActionStatusCode {

    @Column(name = "code", nullable = false, updatable = false)
    private int code;

    @Column(name = "severity", nullable = false)
    @Convert(converter = SeverityConverter.class)
    private Severity severity;

    @Converter
    public static class SeverityConverter extends MapAttributeConverter<Severity, Integer> {

        public SeverityConverter() {
            super(new EnumMap<>(Severity.class) {

                {
                    put(Severity.TRACE, 0);
                    put(Severity.DEBUG, 1);
                    put(Severity.INFO, 2);
                    put(Severity.WARN, 3);
                    put(Severity.ERROR, 4);
                }
            }, null);
        }
    }
}
