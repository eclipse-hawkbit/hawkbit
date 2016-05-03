/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.AbstractColorPicker.Coordinates2Color;

/**
 * Converts 2d-coordinates to a Color.
 * 
 *
 *
 *
 */
public class CoordinatesToColor implements Coordinates2Color {

    private static final long serialVersionUID = 9145071998551210789L;

    @Override
    public Color calculate(final int x, final int y) {
        return calculateHSVColor(x, y);
    }

    @Override
    public int[] calculate(final Color color) {
        final float[] hsv = color.getHSV();
        final int x = Math.round(hsv[0] * 220f);
        int y = 0;
        y = calculateYCoordinateOfColor(hsv);
        return new int[] { x, y };
    }

    private Color calculateHSVColor(final int x, final int y) {
        final float h = x / 220f;
        float s = 1f;
        float v = 1f;
        if (y < 110) {
            s = y / 110f;
        } else if (y > 110) {
            v = 1f - (y - 110f) / 110f;
        }
        return new Color(Color.HSVtoRGB(h, s, v));
    }

    private int calculateYCoordinateOfColor(final float[] hsv) {
        int y;
        // lower half
        /* Assuming hsv[] array value will have in the range of 0 to 1 */
        if (hsv[1] < 1f) {
            y = Math.round(hsv[1] * 110f);
        } else {
            y = Math.round(110f - (hsv[1] + hsv[2]) * 110f);
        }
        return y;
    }

}
