/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.hawkbit.repository.model.Tag;

import com.google.common.base.MoreObjects;

/**
 * Tag details. Represents the ui data for {@link Tag} entity.
 */
public class TagData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final Long id;

    private final String color;

    /**
     * Tag data constructor.
     *
     * @param id
     *            the id of the {@link Tag}
     * @param name
     *            the name of the {@link Tag}
     * @param color
     *            the color of the {@link Tag}
     */
    public TagData(final Long id, final String name, final String color) {
        this.color = color;
        this.id = id;
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the color
     */
    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TagData other = (TagData) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.name, other.name)
                && Objects.equals(this.color, other.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", id).add("name", name).add("color", color).toString();
    }
}
