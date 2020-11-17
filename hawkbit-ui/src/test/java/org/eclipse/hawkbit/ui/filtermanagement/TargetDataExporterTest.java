/**
 * Copyright (c) 2020 devolo AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetBuilder;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TargetDataExporterTest {

    private static final String SEPARATOR = ";";

    private static final String[] HEADER = {
            "Name", SEPARATOR,
            "Description", SEPARATOR,
            "Status", SEPARATOR,
            "Created By", SEPARATOR,
            "Created Date", SEPARATOR,
            "Modified By", SEPARATOR,
            "Modified Date"
    };
    

    private static final StringBuilder line1 = new StringBuilder();

    private static final List<Target> targetList = new ArrayList<>();

    private static final StringBuilder expected = new StringBuilder();

    @Before
    public void setUp(){
        TargetBuilder builder = new JpaTargetBuilder();
        Target t1 = builder.create()
                .controllerId("0123")
                .name("TestTarget1")
                .description("TestTargetDescription1")
                .status(TargetUpdateStatus.PENDING)
                .securityToken("321")
                .build();
        ((AbstractJpaBaseEntity)t1).setCreatedBy("User1");
        ((AbstractJpaBaseEntity)t1).setCreatedAt(0L);
        ((AbstractJpaBaseEntity)t1).setLastModifiedBy("User2");
        ((AbstractJpaBaseEntity)t1).setLastModifiedAt(100L);

        line1.append("\"0123\"")
                .append(SEPARATOR)
                .append("\"TestTargetDescription1\"")
                .append(SEPARATOR)
                .append("\"PENDING\"")
                .append(SEPARATOR)
                .append("\"User1\"")
                .append(SEPARATOR)
                .append("\"" + toReadableDate(0L) + "\"")
                .append(SEPARATOR)
                .append("\"User2\"")
                .append(SEPARATOR)
                .append("\"" + toReadableDate(100L) + "\"");

        targetList.add(t1);

        Arrays.asList(HEADER).forEach(expected::append);
        expected.append('\n');
        expected.append(line1).append('\n');
    }

    @Test
    public void CSVExportTest(){
        StringBuilder builder = TargetDataExporter.toCSV(targetList);
        assertEquals(builder.toString(), expected.toString());
    }

    private static String toReadableDate(long millis){
        DateFormat simpleDate = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
        Date date = new Date(millis);
        return simpleDate.format(date);
    }
}