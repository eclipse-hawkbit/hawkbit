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

/**
 * Custom identifier for tag .
 * 
 *
 */
public class TagIdName implements Serializable {

    private static final long serialVersionUID = 4150829879670269280L;

    private final String name;

    private final Long id;

    /**
     * Tag with id and name.
     * 
     * @param name
     *            tag name
     * @param id
     *            tag id
     */
    public TagIdName(final String name, final Long id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

}
