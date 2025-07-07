/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.ql;

import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;

import lombok.Data;
import lombok.experimental.Accessors;

@Entity
@Data
@Accessors(chain = true)
class Root {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // singular attributes
    // basic
    private String strValue;
    private int intValue;
    // entity
    @ManyToOne
    private Sub subEntity;
    // set searchable by key
    @ManyToMany(targetEntity = Sub.class)
    @JoinTable(
            name = "subs",
            joinColumns = { @JoinColumn(name = "root") },
            inverseJoinColumns = { @JoinColumn(name = "subs") })
    private Set<Sub> subSet;
    // standard map
    @ElementCollection
    @CollectionTable(
            name = "map",
            joinColumns = { @JoinColumn(name = "root") })
    @MapKeyColumn(name = "map_key", length = 128)
    @Column(name = "map_value", length = 128)
    private Map<String, String> subMap;
}
