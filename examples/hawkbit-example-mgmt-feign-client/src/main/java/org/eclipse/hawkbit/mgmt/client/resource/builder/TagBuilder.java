/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.resource.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;

/**
 * Builder pattern for building {@link MgmtTagRequestBodyPut}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class TagBuilder {

    private String name;
    private String description;
    private String color;

    /**
     * @param name
     *            the name of the tag
     * @return the builder itself
     */
    public TagBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param description
     *            the description of the tag
     * @return the builder itself
     */
    public TagBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @param color
     *            the colour of the tag
     * @return the builder itself
     */
    public TagBuilder color(final String color) {
        this.color = color;
        return this;
    }

    /**
     * Builds a list with a single entry of {@link MgmtTagRequestBodyPut} which
     * can directly be used in the RESTful-API.
     * 
     * @return a single entry list of {@link MgmtTagRequestBodyPut}
     */
    public List<MgmtTagRequestBodyPut> build() {
        return Arrays.asList(doBuild(name));
    }

    /**
     * Builds a list of multiple {@link MgmtTagRequestBodyPut} to create
     * multiple tags at once. An increasing number will be added to the name of
     * the tag. The color and description will remain the same.
     * 
     * @param count
     *            the amount of distribution sets body which should be created
     * @return a list of {@link MgmtTagRequestBodyPut}
     */
    public List<MgmtTagRequestBodyPut> buildAsList(final int count) {
        final List<MgmtTagRequestBodyPut> bodyList = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(name + index));
        }

        return bodyList;
    }

    private MgmtTagRequestBodyPut doBuild(final String prefixName) {
        final MgmtTagRequestBodyPut body = new MgmtTagRequestBodyPut();
        body.setName(prefixName);
        body.setDescription(description);
        body.setColour(color);
        return body;
    }

}
