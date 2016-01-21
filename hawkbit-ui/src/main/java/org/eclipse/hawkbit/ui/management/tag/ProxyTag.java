/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.tag;

import java.io.Serializable;
import java.security.SecureRandom;

/**
 *
 *
 */
public class ProxyTag implements Serializable {

    private static final long serialVersionUID = -9010500141590644093L;

    private TagIdName tagIdName;

    private String name;

    private Long id;

    private String colour;

    private String description;

    /**
     * Proxy tag constructor.
     */
    public ProxyTag() {
        final Integer generatedIntId = new SecureRandom().nextInt(Integer.MAX_VALUE) - Integer.MAX_VALUE;
        tagIdName = new TagIdName(generatedIntId.toString(), null);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public TagIdName getTagIdName() {
        return tagIdName;
    }

    public void setTagIdName(final TagIdName tagIdName) {
        this.tagIdName = tagIdName;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(final String colour) {
        this.colour = colour;
    }

}
