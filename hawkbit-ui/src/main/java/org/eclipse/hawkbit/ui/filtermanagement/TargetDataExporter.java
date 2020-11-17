/**
 * Copyright (c) 2020 devolo AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.model.Target;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to provide formatted Target Data to be used in file exports
 */
public class TargetDataExporter {

    private static final String SEPARATOR = ";";
    private static final String EMPTY_STRING = "";

    private static final String[] HEADER = {
            "Name", SEPARATOR,
            "Description", SEPARATOR,
            "Status", SEPARATOR,
            "Created By", SEPARATOR,
            "Created Date", SEPARATOR,
            "Modified By", SEPARATOR,
            "Modified Date"
    };

    /**
     * Provides list of Targets in CSV format
     * @param targets List of Targets
     * @return StringBuilder containing the CSV-formatted Target data
     */
    public static StringBuilder toCSV(final List<Target> targets)  {
        final StringBuilder builder = new StringBuilder();

        Arrays.asList(HEADER).forEach(builder::append);
        builder.append('\n');

        targets.forEach(target -> assemble(builder, target));

        return builder;
    }

    private static void assemble(StringBuilder builder, Target target){
        builder.append(quote(target.getControllerId()))
                .append(SEPARATOR)
                .append(quote(Optional.ofNullable(target.getDescription()).orElse(EMPTY_STRING)))
                .append(SEPARATOR)
                .append(quote(target.getUpdateStatus().name()))
                .append(SEPARATOR)
                .append(quote(Optional.ofNullable(target.getCreatedBy()).orElse(EMPTY_STRING)))
                .append(SEPARATOR)
                .append(quote(toReadableDate(target.getCreatedAt())))
                .append(SEPARATOR)
                .append(quote(Optional.ofNullable(target.getLastModifiedBy()).orElse(EMPTY_STRING)))
                .append(SEPARATOR)
                .append(quote(toReadableDate(target.getLastModifiedAt())))
                .append('\n');
    }

    private static String toReadableDate(long millis){
        DateFormat simpleDate = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
        Date date = new Date(millis);
        return simpleDate.format(date);
    }

    private static String quote(String input) {
        return new StringBuilder().append("\"").append(input).append("\"").toString();
    }
}
