/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Software module table layout.
 * 
 *
 * 
 */
@SpringComponent
@ViewScope
public class SoftwareModuleTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 6464291374980641235L;

    @Autowired
    private SoftwareModuleDetails softwareModuleDetails;

    @Autowired
    private SoftwareModuleTableHeader smTableHeader;

    @Autowired
    private SoftwareModuleTable smTable;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    void init() {
        super.init(smTableHeader, smTable, softwareModuleDetails);
    }
}
