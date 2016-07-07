/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

import de.flapdoodle.embed.mongo.MongodExecutable;

/**
 * Test class that contains embedded MongoDB for the test.
 */
public abstract class AbstractIntegrationTestWithMongoDB extends AbstractIntegrationTest {

    @Autowired
    protected GridFsOperations operations;

    @Autowired
    protected MongodExecutable mongodExecutable;

    @After
    public void cleanCurrentCollection() {
        operations.delete(new Query());
    }

}
