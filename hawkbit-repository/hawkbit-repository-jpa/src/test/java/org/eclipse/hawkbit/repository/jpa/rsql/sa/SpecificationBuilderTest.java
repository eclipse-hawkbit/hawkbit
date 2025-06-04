/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql.sa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.rsql.RsqlConfigHolder.RsqlToSpecBuilder.G3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLToSQL;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlParser;
import org.eclipse.hawkbit.repository.jpa.rsql.SpecificationBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.jpa.vendor.Database;

@SuppressWarnings("java:S5961") // complex check because the matter is very complex
@DataJpaTest(properties = {
        "logging.level.org.eclipse.hawkbit.repository.jpa.rsql=DEBUG"
}, excludeAutoConfiguration = { FlywayAutoConfiguration.class } )
@EnableAutoConfiguration
@Slf4j
class SpecificationBuilderTest {

    private static final Database DATABASE = Database.H2;

    @Autowired
    private SubRepository subRepository;
    @Autowired
    private RootRepository rootRepository;
    @Autowired
    private EntityManager entityManager;

    private final SpecificationBuilder<Root> builder = new SpecificationBuilder<>(null, false, DATABASE);

    @Test
    void singularStringAttribute() {
        final Root root = rootRepository.save(new Root().setStrValue("rootX"));
        final Root root2 = rootRepository.save(new Root().setStrValue("rootX"));
        final Root root3 = rootRepository.save(new Root().setStrValue("rootY"));
        final Root root4 = rootRepository.save(new Root().setStrValue("rootY"));
        final Root root5 = rootRepository.save(new Root()); // null

        assertThat(filter("strValue==rootX")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("strValue==nostr")).isEmpty();
        assertThat(filter("strValue=is=null")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("strValue!=rootX")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("strValue!=nostr")).hasSize(5);
        assertThat(filter("strValue=not=null")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("strValue<rootY")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("strValue<=rootY")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("strValue>rootX")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("strValue>=rootX")).hasSize(4);
        assertThat(filter("strValue=in=rootX")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("strValue=in=(rootX, rootY)")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("strValue=in=(rootZ, rootT)")).isEmpty();
        assertThat(filter("strValue=out=rootX")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("strValue=out=(rootX, rootY)")).hasSize(1).containsExactlyInAnyOrder(root5);
        // wildcard, like
        assertThat(filter("strValue==root*")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("strValue==*tX")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("strValue!=root*")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("strValue!=*tX")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
    }

    @Test
    void singularIntAttribute() {
        final Root root = rootRepository.save(new Root().setIntValue(0));
        final Root root2 = rootRepository.save(new Root().setIntValue(0));
        final Root root3 = rootRepository.save(new Root().setIntValue(1));
        final Root root4 = rootRepository.save(new Root().setIntValue(1));

        assertThat(filter("intValue==0")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("intValue==2")).isEmpty();
        assertThat(filter("intValue!=0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("intValue!=2")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("intValue<1")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("intValue<=1")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("intValue>0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("intValue>=0")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("intValue=in=0")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("intValue=in=(0, 1)")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("intValue=out=0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("intValue=out=(0, 1)")).isEmpty();
    }

    @Test
    void singularEntityAttribute() {
        final Sub sub = subRepository.save(new Sub().setStrValue("subX").setIntValue(0));
        final Sub sub2 = subRepository.save(new Sub().setStrValue("subY").setIntValue(1));
        final Root root = rootRepository.save(new Root().setSubEntity(sub));
        final Root root2 = rootRepository.save(new Root().setSubEntity(sub));
        final Root root3 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root4 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root5 = rootRepository.save(new Root()); // no sub set

        // by sub entity string
        assertThat(filter("subEntity.strValue==subX")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.strValue==nostr")).isEmpty();
        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it is
        assertThat(filter("subEntity.strValue!=subX")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.strValue!=nostr")).hasSize(5);
        assertThat(filter("subEntity.strValue<subY")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.strValue<=subY")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("subEntity.strValue>subX")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.strValue>=subX")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("subEntity.strValue=in=subX")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.strValue=in=(subX, subY)")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("subEntity.strValue=in=(subZ, subT)")).isEmpty();
        assertThat(filter("subEntity.strValue=out=subX")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.strValue=out=(subX, subY)")).hasSize(1).containsExactlyInAnyOrder(root5);
        // wildcard, like
        assertThat(filter("subEntity.strValue==sub*")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("subEntity.strValue==*bX")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.strValue!=sub*")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("subEntity.strValue!=*bX")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);

        // by sub entity int
        assertThat(filter("subEntity.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.intValue==2")).isEmpty();
        assertThat(filter("subEntity.intValue!=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.intValue!=2")).hasSize(5);
        assertThat(filter("subEntity.intValue<1")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.intValue<=1")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("subEntity.intValue>0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.intValue>=0")).hasSize(4);
        assertThat(filter("subEntity.intValue=in=0")).hasSize(2).containsExactlyInAnyOrder(root, root2);
        assertThat(filter("subEntity.intValue=in=(0, 1)")).hasSize(4).containsExactlyInAnyOrder(root, root2, root3, root4);
        assertThat(filter("subEntity.intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("subEntity.intValue=out=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.intValue=out=(0, 1)")).hasSize(1).containsExactlyInAnyOrder(root5);
    }

    @Test
    void pluralSubSetAttribute() {
        final Sub sub = subRepository.save(new Sub().setStrValue("subX").setIntValue(0));
        final Sub sub2 = subRepository.save(new Sub().setStrValue("subY").setIntValue(1));
        final Sub sub3 = subRepository.save(new Sub().setStrValue("subY").setIntValue(0));
        final Root root = rootRepository.save(new Root().setSubSet(Set.of(sub)));
        final Root root2 = rootRepository.save(new Root().setSubSet(Set.of(sub2)));
        final Root root3 = rootRepository.save(new Root().setSubSet(Set.of(sub3)));
        final Root root4 = rootRepository.save(new Root().setSubSet(Set.of(sub, sub2)));
        final Root root5 = rootRepository.save(new Root().setSubSet(Set.of(sub, sub3)));
        final Root root6 = rootRepository.save(new Root()); // no sub set

        // by sub entity string
        assertThat(filter("subSet.strValue==subX")).hasSize(3).containsExactlyInAnyOrder(root, root4, root5);
        assertThat(filter("subSet.strValue==nostr")).isEmpty();
        assertThat(filter("subSet.strValue!=subX")).hasSize(3).containsExactlyInAnyOrder(root2, root3, root6);
        assertThat(filter("subSet.strValue!=nostr")).hasSize(6);
        assertThat(filter("subSet.strValue<subY")).hasSize(3).containsExactlyInAnyOrder(root, root4, root5);
        assertThat(filter("subSet.strValue<=subY")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue>subX")).hasSize(4).containsExactlyInAnyOrder(root2, root3, root4, root5);
        assertThat(filter("subSet.strValue>=subX")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue=in=subX")).hasSize(3).containsExactlyInAnyOrder(root, root4, root5);
        assertThat(filter("subSet.strValue=in=(subX, subY)"))
                .hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue=in=(subZ, subT)")).isEmpty();
        assertThat(filter("subSet.strValue=out=subX")).hasSize(3).containsExactlyInAnyOrder(root2, root3, root6);
        assertThat(filter("subSet.strValue=out=(subX, subY)")).hasSize(1).containsExactlyInAnyOrder(root6);
        // wildcard, like
        assertThat(filter("subSet.strValue==sub*")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue==*bX")).hasSize(3).containsExactlyInAnyOrder(root, root4, root5);
        assertThat(filter("subSet.strValue!=sub*")).hasSize(1).containsExactlyInAnyOrder(root6);
        assertThat(filter("subSet.strValue!=*bX")).hasSize(3).containsExactlyInAnyOrder(root2, root3, root6);

        // by sub entity int
        assertThat(filter("subSet.intValue==0")).hasSize(4).containsExactlyInAnyOrder(root, root3, root4, root5);
        assertThat(filter("subSet.intValue==2")).isEmpty();
        assertThat(filter("subSet.intValue!=0")).hasSize(2).containsExactlyInAnyOrder(root2, root6);
        assertThat(filter("subSet.intValue!=2")).hasSize(6);
        assertThat(filter("subSet.intValue<1")).hasSize(4).containsExactlyInAnyOrder(root, root3, root4, root5);
        assertThat(filter("subSet.intValue<=1")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue>0")).hasSize(2).containsExactlyInAnyOrder(root2, root4);
        assertThat(filter("subSet.intValue>=0")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue=in=0")).hasSize(4).containsExactlyInAnyOrder(root, root3, root4, root5);
        assertThat(filter("subSet.intValue=in=(0, 1)")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("subSet.intValue=out=0")).hasSize(2).containsExactlyInAnyOrder(root2, root6);
        assertThat(filter("subSet.intValue=out=(0, 1)")).hasSize(1).containsExactlyInAnyOrder(root6);
    }

    @Test
    void pluralSubMapAttribute() {
        final Root root = rootRepository.save(new Root().setSubMap(Map.of("x", "rootX", "y", "rootY")));
        final Root root2 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootX", "y", "rootX")));
        final Root root3 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootY", "y", "rootY")));
        final Root root4 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootY", "y", "rootX")));
        final Root root5 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootX")));
        final Root root6 = rootRepository.save(new Root()); // no sub map

        assertThat(filter("subMap.x==rootX")).hasSize(3).containsExactlyInAnyOrder(root, root2, root5);
        assertThat(filter("subMap.x==nostr")).isEmpty();
        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it's not
        assertThat(filter("subMap.x!=rootX")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x!=nostr")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subMap.x<rootY")).hasSize(3).containsExactlyInAnyOrder(root, root2, root5);
        assertThat(filter("subMap.x<=rootY")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subMap.x>rootX")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x>=rootX")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subMap.x=in=rootX")).hasSize(3).containsExactlyInAnyOrder(root, root2, root5);
        assertThat(filter("subMap.x=in=(rootX, rootY)")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subMap.x=in=(rootZ, rootT)")).isEmpty();
        assertThat(filter("subMap.x=out=rootX")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x=out=(rootX, rootY)")).isEmpty();
        // wildcard, like
        assertThat(filter("subMap.x==root*")).hasSize(5).containsExactlyInAnyOrder(root, root2, root3, root4, root5);
        assertThat(filter("subMap.x==*tX")).hasSize(3).containsExactlyInAnyOrder(root, root2, root5);
        assertThat(filter("subMap.x!=root*")).isEmpty();
        assertThat(filter("subMap.x!=*tX")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
    }

    private List<Root> filter(final String rsql) {
        // reference / auto filter (using elements and reflection)
        final ReferenceMatcher matcher = ReferenceMatcher.ofRsql(rsql);
        final List<Root> refResult = StreamSupport.stream(rootRepository.findAll().spliterator(), false).filter(matcher::match).toList();
        final List<Root> result = rootRepository.findAll(builder.specification(RsqlParser.parse(rsql)));
        // auto check with reference result
        try {
            assertThat(result).containsExactlyInAnyOrder(refResult.toArray(Root[]::new));
        } catch (final AssertionError e) {
            log.error(
                    "Fail to get expected result for RSQL: {} with SQL query: {}",
                    rsql, new RSQLToSQL(entityManager).toSQL(Root.class, null, rsql, G3),
                    e);
            throw e;
        }
        return result;
    }

    @SpringBootConfiguration
    static class Config {}
}