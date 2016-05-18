/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.Arrays;

/**
 * 
 * Available token details.
 * 
 *
 *
 */
public class TokenDescription {

    /** Literal token values. */
    private static final String[] TOKEN_IMAGE = { "<EOF>", "\" \"", "\"\\t\"", "<ALPHA>", "<UNRESERVED_STR>",
            "<SINGLE_QUOTED_STR>", "<DOUBLE_QUOTED_STR>", "<AND>", "<OR>", "\"(\"", "\")\"", "<==>|<!=>", ">=|<=", };

    public String[] getTokenImage() {
        return Arrays.copyOf(TOKEN_IMAGE, TOKEN_IMAGE.length);
    }

}
