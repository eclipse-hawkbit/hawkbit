/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.util.Objects;

import org.eclipse.hawkbit.repository.model.Tag;

import com.google.common.base.MoreObjects;

/**
 * Proxy for {@link Tag}.
 */
public class ProxyTag extends ProxyFilterButton {

    private static final long serialVersionUID = 1L;

    private boolean isNoTag;

    /**
     * Constructor
     */
    public ProxyTag() {
    }

    /**
     * Constructor for ProxyTag
     *
     * @param id
     *          Tag id
     * @param name
     *          Tag name
     * @param colour
     *          Tag oolour
     */
    public ProxyTag(final Long id, final String name, final String colour) {
        setId(id);
        setName(name);
        setColour(colour);
    }

    /**
     * Flag that indicates if the tag is added.
     *
     * @return <code>true</code> if the tag is added, otherwise
     *         <code>false</code>
     */
    public boolean isNoTag() {
        return isNoTag;
    }

    /**
     * Sets the flag that indicates if there is no tag.
     *
     * @param isNoTag
     *            <code>true</code> if there is no tag, otherwise
     *            <code>false</code>
     */
    public void setNoTag(final boolean isNoTag) {
        this.isNoTag = isNoTag;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyTag other = (ProxyTag) obj;
        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getColour(), other.getColour());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getColour());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", getId()).add("name", getName()).add("color", getColour())
                .toString();
    }
}
