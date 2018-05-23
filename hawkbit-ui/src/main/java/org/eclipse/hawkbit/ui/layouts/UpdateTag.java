/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.layouts;

/**
 * Interface which is implemented in tags and types layout classes for updating
 * a tag or type.
 */
@FunctionalInterface
public interface UpdateTag {

    /**
     * Provides the update tag or type dialog with the tag or type information
     *
     * @param selectedTagName
     *            the name of the selected tag
     */
    void setTagDetails(final String selectedTagName);

}
