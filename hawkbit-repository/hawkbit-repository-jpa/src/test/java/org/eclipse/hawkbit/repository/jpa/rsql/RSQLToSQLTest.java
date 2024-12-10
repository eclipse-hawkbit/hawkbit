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

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.eclipse.hawkbit.repository.RsqlQueryField;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = NONE, properties = {
        "hawkbit.rsql.caseInsensitiveDB=true",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.main.banner-mode=off",
        "logging.level.root=ERROR" })
@ContextConfiguration(classes = { RepositoryApplicationConfiguration.class, TestConfiguration.class })
@Import(TestChannelBinderConfiguration.class)
@Disabled("For manual run only, while playing around with RSQL to SQL")
class RSQLToSQLTest {

    private RSQLToSQL rsqlToSQL;

    @Test
    void print() {
        print(JpaTarget.class, TargetFields.class, "tag==tag1 and tag==tag2");
        print(JpaTarget.class, TargetFields.class, "tag==tag1 or tag==tag2 or tag==tag3");
        print(JpaTarget.class, TargetFields.class, "targettype.key==type1 and metadata.key1==target1-value1");
        print(JpaTarget.class, TargetFields.class, "(tag!=TAG1 or tag !=TAG2)");
    }

    @Test
    void printSameTableMultiJoin() {
        print(JpaTarget.class, TargetFields.class, "installedds.version==1.0.0 or assignedds.version==2.0.0");
    }

    @Test
    void printPG() {
        printFrom(JpaTarget.class, TargetFields.class, "tag!=TAG1 and tag==TAG2");
        printFrom(JpaTarget.class, TargetFields.class, "tag==TAG1 and tag!=TAG2");
    }

    private static String from(final String sql) {
        return sql.substring(sql.indexOf("FROM"));
    }

    @PersistenceContext
    private void setEntityManager(final EntityManager entityManager) {
        rsqlToSQL = new RSQLToSQL(entityManager);
    }

    private <T, A extends Enum<A> & RsqlQueryField> void print(final Class<T> domainClass, final Class<A> fieldsClass, final String rsql) {
        System.out.println(rsql);
        System.out.println("\tlegacy:\n" +
                "\t\t" + rsqlToSQL.toSQL(domainClass, fieldsClass, rsql, true));
        System.out.println("\tG2:\n" +
                "\t\t" + rsqlToSQL.toSQL(domainClass, fieldsClass, rsql, false));
    }

    private <T, A extends Enum<A> & RsqlQueryField> void printFrom(final Class<T> domainClass, final Class<A> fieldsClass, final String rsql) {
        System.out.println(rsql);
        System.out.println("\tlegacy:\n" +
                "\t\t" + from(rsqlToSQL.toSQL(domainClass, fieldsClass, rsql, true)));
        System.out.println("\tG2:\n" +
                "\t\t" + from(rsqlToSQL.toSQL(domainClass, fieldsClass, rsql, false)));
    }
}