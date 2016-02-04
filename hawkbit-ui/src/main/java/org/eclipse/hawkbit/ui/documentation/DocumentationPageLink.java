/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.documentation;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Link;

/**
 * enum declaration which contains all documentation links to the documenation
 * which can be used to create the links to a specific documentation from a UI
 * view.
 * 
 *
 *
 *
 */
public enum DocumentationPageLink {

    // Root URL to index.html
    ROOT_VIEW(""),

    // userguide/deployment
    DEPLOYMENT_VIEW("deployment.html", DocumentationUtil.USERGUIDE),

    // userguide/distribution
    DISTRIBUTION_VIEW("distribution.html", DocumentationUtil.USERGUIDE),

    // userguide/upload
    UPLOAD_VIEW("upload.html", DocumentationUtil.USERGUIDE),

    // userguide/statistics
    STATISTICS_VIEW("statistics.html", DocumentationUtil.USERGUIDE),

    // userguide
    SYSTEM_CONFIGURATION_VIEW("systemconfiguration.html", DocumentationUtil.USERGUIDE),

    // authentication/security
    TARGET_SECURITY_TOKEN("security.html", DocumentationUtil.DEVELOPERGUIDE, "concepts"),

    // userguide/targetfilter
    TARGET_FILTER_VIEW("targetfilter.html", DocumentationUtil.USERGUIDE),

    // userguide/ROLLOUT
    ROLLOUT_VIEW("rollout.html", DocumentationUtil.USERGUIDE);

    private static final String ROOT_PATH = "../documentation";

    private final String[] path;
    private final String page;

    private DocumentationPageLink(final String page, final String... path) {
        this.path = path;
        this.page = page;
    }

    public String getPath() {
        final StringBuilder builder = new StringBuilder(ROOT_PATH);
        for (final String string : path) {
            builder.append('/').append(string);
        }
        return builder.append('/').append(page).toString();
    }

    public Link getLink() {
        final Link link = new Link("", new ExternalResource(getPath()));
        link.setTargetName("_blank");
        link.setIcon(FontAwesome.QUESTION_CIRCLE);
        link.setDescription("Documentation");
        return link;
    }

}
