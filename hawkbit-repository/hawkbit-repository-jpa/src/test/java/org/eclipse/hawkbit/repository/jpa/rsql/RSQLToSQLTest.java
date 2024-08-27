/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment=NONE, properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.main.banner-mode=off",
        "logging.level.root=ERROR" })
@ContextConfiguration(classes = { RepositoryApplicationConfiguration.class, TestConfiguration.class })
@Import(TestChannelBinderConfiguration.class)
@Disabled("For manual run only, while playing around with RSQL to SQL")
public class RSQLToSQLTest {

    private RSQLToSQL rsqlToSQL;

    @PersistenceContext
    private void setEntityManager(final EntityManager entityManager) {
        rsqlToSQL = new RSQLToSQL(entityManager);
    }

    @Test
    public void print() {
        String rsql = "tag==tag1 or tag==tag2 or tag==tag3";
        System.out.println(rsql + "\n" +
                "\tlegacy:\n" +
                "\t\t" + rsqlToSQL.toSQL(JpaTarget.class, TargetFields.class, rsql, true) + "\n" +
                "\tG2:\n" +
                "\t\t" + rsqlToSQL.toSQL(JpaTarget.class, TargetFields.class, rsql, false));

        rsql = "targettype.key==type1 and metadata.key1==target1-value1";
        System.out.println(rsql + "\n" +
                "\tlegacy:\n" +
                "\t\t" + rsqlToSQL.toSQL(JpaTarget.class, TargetFields.class, rsql, true) + "\n" +
                "\tG2:\n" +
                "\t\t" + rsqlToSQL.toSQL(JpaTarget.class, TargetFields.class, rsql, false));
    }
}