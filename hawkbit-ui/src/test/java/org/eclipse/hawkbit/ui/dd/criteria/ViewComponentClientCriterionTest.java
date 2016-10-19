/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.criteria;

import java.io.StringWriter;

import org.junit.Test;

import com.vaadin.server.JsonPaintTarget;
import com.vaadin.server.PaintException;

public class ViewComponentClientCriterionTest {

    @Test
    public void testPaintContent() throws PaintException {
        String[] validDropTargetIdPrefixes = new String[] { "dropTarget1.", "dropTarget2." };
        String[] validDropAreasIds = new String[] { "dropTarget1.comp", "dropTarget2.comp" };

        ServerViewComponentClientCriterion cut = new ServerViewComponentClientCriterion("dragSource.", validDropTargetIdPrefixes,
                validDropAreasIds);

        StringWriter writer = new StringWriter();
        JsonPaintTarget paintTarget = new JsonPaintTarget(null, writer, false);

        cut.paint(paintTarget);
        paintTarget.close();

        System.out.println(writer.toString());
    }

}
