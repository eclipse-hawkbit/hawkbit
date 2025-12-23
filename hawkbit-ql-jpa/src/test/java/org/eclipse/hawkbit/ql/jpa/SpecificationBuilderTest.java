/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ql.EntityMatcher;
import org.eclipse.hawkbit.ql.jpa.utils.QlToSql;
import org.eclipse.hawkbit.ql.rsql.RsqlParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

@SuppressWarnings("java:S5961") // complex check because the matter is very complex
@DataJpaTest(properties = "spring.jpa.database=H2")
@EnableAutoConfiguration
@Slf4j
class SpecificationBuilderTest {

    @Autowired
    private SubSubRepository subSubRepository;
    @Autowired
    private SubRepository subRepository;
    @Autowired
    private RootRepository rootRepository;
    @Autowired
    private EntityManager entityManager;

    private final SpecificationBuilder<Root> builder = new SpecificationBuilder<>(false);

    @Test
    void singularStringAttribute() {
        final Root root1 = rootRepository.save(new Root().setStrValue("rootx"));
        final Root root2 = rootRepository.save(new Root().setStrValue("rootx"));
        final Root root3 = rootRepository.save(new Root().setStrValue("rooty"));
        final Root root4 = rootRepository.save(new Root().setStrValue("rooty"));
        final Root root5 = rootRepository.save(new Root()); // null

        assertThat(filter("strValue==rootx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("strValue==nostr")).isEmpty();
        assertThat(filter("strValue=is=null")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("strValue!=rootx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("strValue!=nostr")).hasSize(5);
        assertThat(filter("strValue=not=null")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("strValue<rooty")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("strValue<=rooty")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("strValue>rootx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("strValue>=rootx")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);

        assertThat(filter("strValue=in=rootx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("strValue=in=(rootx, rooty)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("strValue=in=(rootz, roott)")).isEmpty();
        assertThat(filter("strValue=out=rootx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("strValue=out=(rootx, rooty)")).hasSize(1).containsExactlyInAnyOrder(root5);
        // wildcard, like
        assertThat(filter("strValue==root*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("strValue==*tx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("strValue!=root*")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("strValue!=*tx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("strValue==*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("strValue!=*")).hasSize(1).containsExactlyInAnyOrder(root5);

        // null checks
        assertThat(filter("strValue=is=null")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("strValue=not=null")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);

        assertThat(filter("strValue==*tx and strValue==rooty")).isEmpty();
        assertThat(filter("strValue==*tx and strValue!=rootx")).isEmpty();
        assertThat(filter("strValue==*tx and strValue==rootx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("strValue==*tx or strValue==rooty")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("strValue==*tx or strValue!=rootx")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("strValue==*tx or strValue=is=null")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
    }

    @Test
    void singularIntAttribute() {
        final Root root1 = rootRepository.save(new Root().setIntValue(0));
        final Root root2 = rootRepository.save(new Root().setIntValue(0));
        final Root root3 = rootRepository.save(new Root().setIntValue(1));
        final Root root4 = rootRepository.save(new Root().setIntValue(1));

        assertThat(filter("intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("intValue==2")).isEmpty();
        assertThat(filter("intValue!=0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("intValue!=2")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("intValue<1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("intValue<=1")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("intValue>0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("intValue>=0")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("intValue=in=0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("intValue=in=(0, 1)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("intValue=out=0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("intValue=out=(0, 1)")).isEmpty();

        assertThat(filter("intValue==0 and intValue==1")).isEmpty();
        assertThat(filter("intValue==0 and intValue!=1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("intValue==0 and intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("intValue==0 or intValue==1")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("intValue==0 or intValue!=1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
    }

    @Test
    void singularEntityAttribute() {
        final Sub sub1 = subRepository.save(new Sub().setStrValue("subx").setIntValue(0));
        final Sub sub2 = subRepository.save(new Sub().setStrValue("suby").setIntValue(1));
        final Root root1 = rootRepository.save(new Root().setSubEntity(sub1));
        final Root root2 = rootRepository.save(new Root().setSubEntity(sub1));
        final Root root3 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root4 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root5 = rootRepository.save(new Root()); // no sub set

        // by sub entity string
        assertThat(filter("subEntity.strValue==subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue==nostr")).isEmpty();
        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it is
        assertThat(filter("subEntity.strValue!=subx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.strValue!=nostr")).hasSize(5);
        assertThat(filter("subEntity.strValue<suby")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue<=suby")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue>subx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.strValue>=subx")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue=in=subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue=in=(subx, suby)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue=in=(subZ, subT)")).isEmpty();
        assertThat(filter("subEntity.strValue=out=subx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.strValue=out=(subx, suby)")).hasSize(1).containsExactlyInAnyOrder(root5);
        // wildcard, like
        assertThat(filter("subEntity.strValue==sub*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue==*bx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue!=sub*")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("subEntity.strValue!=*bx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.strValue==*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue!=*")).hasSize(1).containsExactlyInAnyOrder(root5);

        // null checks
        assertThat(filter("subEntity.strValue=is=null")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("subEntity.strValue=not=null")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);

        assertThat(filter("subEntity.strValue==*bx and subEntity.strValue==suby")).isEmpty();
        assertThat(filter("subEntity.strValue==*bx and subEntity.strValue!=subx")).isEmpty();
        assertThat(filter("subEntity.strValue==*bx and subEntity.strValue==subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue==*bx or subEntity.strValue==suby"))
                .hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue==*bx or subEntity.strValue!=subx"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subEntity.strValue==*bx or subEntity.strValue=is=null")).hasSize(3)
                .containsExactlyInAnyOrder(root1, root2, root5);

        // by sub entity int
        assertThat(filter("subEntity.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.intValue==2")).isEmpty();
        assertThat(filter("subEntity.intValue!=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.intValue!=2")).hasSize(5);
        assertThat(filter("subEntity.intValue<1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.intValue<=1")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.intValue>0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.intValue>=0")).hasSize(4);
        assertThat(filter("subEntity.intValue=in=0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.intValue=in=(0, 1)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("subEntity.intValue=out=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.intValue=out=(0, 1)")).hasSize(1).containsExactlyInAnyOrder(root5);

        assertThat(filter("subEntity.intValue==0 and subEntity.intValue==1")).isEmpty();
        assertThat(filter("subEntity.intValue==0 and subEntity.intValue!=1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.intValue==0 and subEntity.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.intValue==0 or subEntity.intValue==1")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.intValue==0 or subEntity.intValue!=1")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);

        assertThat(filter("subEntity.strValue==subx and subEntity.intValue==1")).isEmpty();
        assertThat(filter("subEntity.strValue==subx and subEntity.intValue!=1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue==subx and subEntity.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.strValue==subx or subEntity.intValue==1")).hasSize(4)
                .containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.strValue==subx or subEntity.intValue!=1")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
    }

    @Test
    void pluralSubSetAttribute() {
        final Sub sub1 = subRepository.save(new Sub().setStrValue("subx").setIntValue(0));
        final Sub sub2 = subRepository.save(new Sub().setStrValue("suby").setIntValue(1));
        final Sub sub3 = subRepository.save(new Sub().setStrValue("suby").setIntValue(0));
        final Root root1 = rootRepository.save(new Root().setSubSet(Set.of(sub1)));
        final Root root2 = rootRepository.save(new Root().setSubSet(Set.of(sub2)));
        final Root root3 = rootRepository.save(new Root().setSubSet(Set.of(sub3)));
        final Root root4 = rootRepository.save(new Root().setSubSet(Set.of(sub1, sub2)));
        final Root root5 = rootRepository.save(new Root().setSubSet(Set.of(sub1, sub3)));
        final Root root6 = rootRepository.save(new Root()); // no sub set

        // by sub entity string
        assertThat(filter("subSet.strValue==subx")).hasSize(3).containsExactlyInAnyOrder(root1, root4, root5);
        assertThat(filter("subSet.strValue==nostr")).isEmpty();
        assertThat(filter("subSet.strValue!=subx")).hasSize(3).containsExactlyInAnyOrder(root2, root3, root6);
        assertThat(filter("subSet.strValue!=nostr")).hasSize(6);
        assertThat(filter("subSet.strValue<suby")).hasSize(3).containsExactlyInAnyOrder(root1, root4, root5);
        assertThat(filter("subSet.strValue<=suby")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue>subx")).hasSize(4).containsExactlyInAnyOrder(root2, root3, root4, root5);
        assertThat(filter("subSet.strValue>=subx")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue=in=subx")).hasSize(3).containsExactlyInAnyOrder(root1, root4, root5);
        assertThat(filter("subSet.strValue=in=(subx, suby)")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue=in=(subZ, subT)")).isEmpty();
        assertThat(filter("subSet.strValue=out=subx")).hasSize(3).containsExactlyInAnyOrder(root2, root3, root6);
        assertThat(filter("subSet.strValue=out=(subx, suby)")).hasSize(1).containsExactlyInAnyOrder(root6);
        // wildcard, like
        assertThat(filter("subSet.strValue==sub*")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue==*bx")).hasSize(3).containsExactlyInAnyOrder(root1, root4, root5);
        assertThat(filter("subSet.strValue!=sub*")).hasSize(1).containsExactlyInAnyOrder(root6);
        assertThat(filter("subSet.strValue!=*bx")).hasSize(3).containsExactlyInAnyOrder(root2, root3, root6);
        assertThat(filter("subSet.strValue==*")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue!=*")).hasSize(1).containsExactlyInAnyOrder(root6);

        assertThat(filter("subSet.strValue==*bx and subSet.strValue==suby")).hasSize(2).containsExactlyInAnyOrder(root4, root5);
        assertThat(filter("subSet.strValue==*bx and subSet.strValue!=subx")).isEmpty();
        assertThat(filter("subSet.strValue==*bx and subSet.strValue!=suby")).hasSize(1).containsExactlyInAnyOrder(root1);
        assertThat(filter("subSet.strValue==*bx or subSet.strValue==suby"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.strValue==*bx or subSet.strValue!=subx"))
                .hasSize(6).containsExactlyInAnyOrder(root1, root2, root3, root4, root5, root6);

        // by sub entity int
        assertThat(filter("subSet.intValue==0")).hasSize(4).containsExactlyInAnyOrder(root1, root3, root4, root5);
        assertThat(filter("subSet.intValue==2")).isEmpty();
        // the legacy builders has different (and wrong semantic)
        // G1 - has element != x
        // G2 - has element != x or has no elements
        // accompanying G3 it is (as it should be) semantic of set != x - has no element with value x (including nhas no elements)
        assertThat(filter("subSet.intValue!=0")).hasSize(2).containsExactlyInAnyOrder(root2, root6);
        assertThat(filter("subSet.intValue!=2")).hasSize(6);
        assertThat(filter("subSet.intValue<1")).hasSize(4).containsExactlyInAnyOrder(root1, root3, root4, root5);
        assertThat(filter("subSet.intValue<=1")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue>0")).hasSize(2).containsExactlyInAnyOrder(root2, root4);
        assertThat(filter("subSet.intValue>=0")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue=in=0")).hasSize(4).containsExactlyInAnyOrder(root1, root3, root4, root5);
        assertThat(filter("subSet.intValue=in=(0, 1)")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("subSet.intValue=out=0")).hasSize(2).containsExactlyInAnyOrder(root2, root6);
        assertThat(filter("subSet.intValue=out=(0, 1)")).hasSize(1).containsExactlyInAnyOrder(root6);

        assertThat(filter("subSet.intValue==0 and subSet.intValue==0")).hasSize(4).containsExactlyInAnyOrder(root1, root3, root4, root5);
        assertThat(filter("subSet.intValue==0 and subSet.intValue!=1")).hasSize(3).containsExactlyInAnyOrder(root1, root3, root5);
        assertThat(filter("subSet.intValue==0 or subSet.intValue==1"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue==0 or subSet.intValue!=1"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root3, root4, root5, root6);

        assertThat(filter("subSet.intValue==0 and subSet.strValue==suby")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subSet.intValue==0 and subSet.strValue!=subx")).hasSize(1).containsExactlyInAnyOrder(root3);
        assertThat(filter("subSet.intValue==0 and subSet.strValue!=suby")).hasSize(1).containsExactlyInAnyOrder(root1);
        assertThat(filter("subSet.intValue==0 or subSet.strValue==suby"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subSet.intValue==0 or subSet.strValue!=subx"))
                .hasSize(6).containsExactlyInAnyOrder(root1, root2, root3, root4, root5, root6);
    }

    @Test
    void pluralSubMapAttribute() {
        final Root root1 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootx", "y", "rooty")));
        final Root root2 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootx", "y", "rootx")));
        final Root root3 = rootRepository.save(new Root().setSubMap(Map.of("x", "rooty", "y", "rooty")));
        final Root root4 = rootRepository.save(new Root().setSubMap(Map.of("x", "rooty", "y", "rootx")));
        final Root root5 = rootRepository.save(new Root().setSubMap(Map.of("x", "rootx")));
        final Root root6 = rootRepository.save(new Root()); // no sub map

        assertThat(filter("subMap.x==rootx")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
        assertThat(filter("subMap.x==nostr")).isEmpty();
        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it's not
        assertThat(filter("subMap.x!=rootx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x!=nostr")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x<rooty")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
        assertThat(filter("subMap.x<=rooty")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x>rootx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x>=rootx")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x=in=rootx")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
        assertThat(filter("subMap.x=in=(rootx, rooty)")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x=in=(rootz, roott)")).isEmpty();
        assertThat(filter("subMap.x=out=rootx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x=out=(rootx, rooty)")).isEmpty();
        // wildcard, like
        assertThat(filter("subMap.x==root*")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x==*tx")).hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
        assertThat(filter("subMap.x!=root*")).isEmpty();
        assertThat(filter("subMap.x!=*tx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subMap.x==*")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x!=*")).isEmpty();

        // null checks
        assertThat(filter("subMap.x=is=null")).hasSize(1).containsExactlyInAnyOrder(root6);
        assertThat(filter("subMap.x=not=null")).hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);

        assertThat(filter("subMap.x==*tx and subMap.y==rooty")).hasSize(1).containsExactlyInAnyOrder(root1);
        assertThat(filter("subMap.x==*tx and subMap.y!=rootx")).hasSize(1).containsExactlyInAnyOrder(root1);
        assertThat(filter("subMap.x==*tx or subMap.x==rooty"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subMap.x==*tx or subMap.x!=rootx"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
    }

    @Test
    void singularEntitySubSubAttribute() {
        final SubSub subSub1 = subSubRepository.save(new SubSub().setStrValue("subx").setIntValue(0));
        final SubSub subSub2 = subSubRepository.save(new SubSub().setStrValue("suby").setIntValue(1));
        final Sub sub1 = subRepository.save(new Sub().setSubSub(subSub1));
        final Sub sub2 = subRepository.save(new Sub().setSubSub(subSub2));
        final Root root1 = rootRepository.save(new Root().setSubEntity(sub1));
        final Root root2 = rootRepository.save(new Root().setSubEntity(sub1));
        final Root root3 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root4 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root5 = rootRepository.save(new Root()); // no sub set

        // by sub entity string
        assertThat(filter("subEntity.subSub.strValue==subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue==nostr")).isEmpty();
        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it is
        assertThat(filter("subEntity.subSub.strValue!=subx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.strValue!=nostr")).hasSize(5);
        assertThat(filter("subEntity.subSub.strValue<suby")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue<=suby")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue>subx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.subSub.strValue>=subx")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue=in=subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue=in=(subx, suby)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue=in=(subZ, subT)")).isEmpty();
        assertThat(filter("subEntity.subSub.strValue=out=subx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.strValue=out=(subx, suby)")).hasSize(1).containsExactlyInAnyOrder(root5);
        // wildcard, like
        assertThat(filter("subEntity.subSub.strValue==sub*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue==*bx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue!=sub*")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("subEntity.subSub.strValue!=*bx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.strValue==*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue!=*")).hasSize(1).containsExactlyInAnyOrder(root5);

        assertThat(filter("subEntity.subSub.strValue=is=null")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("subEntity.subSub.strValue=not=null")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);

        assertThat(filter("subEntity.subSub.strValue==*bx and subEntity.subSub.strValue==suby")).isEmpty();
        assertThat(filter("subEntity.subSub.strValue==*bx and subEntity.subSub.strValue!=subx")).isEmpty();
        assertThat(filter("subEntity.subSub.strValue==*bx and subEntity.subSub.strValue==subx"))
                .hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue==*bx or subEntity.subSub.strValue==suby"))
                .hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue==*bx or subEntity.subSub.strValue!=subx"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subEntity.subSub.strValue==*bx or subEntity.subSub.strValue=is=null"))
                .hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);

        // by sub entity int
        assertThat(filter("subEntity.subSub.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.intValue==2")).isEmpty();
        assertThat(filter("subEntity.subSub.intValue!=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.intValue!=2")).hasSize(5);
        assertThat(filter("subEntity.subSub.intValue<1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.intValue<=1")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.intValue>0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.subSub.intValue>=0")).hasSize(4);
        assertThat(filter("subEntity.subSub.intValue=in=0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.intValue=in=(0, 1)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("subEntity.subSub.intValue=out=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.intValue=out=(0, 1)")).hasSize(1).containsExactlyInAnyOrder(root5);

        assertThat(filter("subEntity.subSub.intValue==0 and subEntity.subSub.intValue==1")).isEmpty();
        assertThat(filter("subEntity.subSub.intValue==0 and subEntity.subSub.intValue!=1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.intValue==0 and subEntity.subSub.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.intValue==0 or subEntity.subSub.intValue==1")).hasSize(4)
                .containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.intValue==0 or subEntity.subSub.intValue!=1"))
                .hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);

        assertThat(filter("subEntity.subSub.strValue==subx and subEntity.subSub.intValue==1")).isEmpty();
        assertThat(filter("subEntity.subSub.strValue==subx and subEntity.subSub.intValue!=1")).hasSize(2)
                .containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue==subx and subEntity.subSub.intValue==0"))
                .hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.strValue==subx or subEntity.subSub.intValue==1"))
                .hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.strValue==subx or subEntity.subSub.intValue!=1"))
                .hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
    }

    @Test
    void deapSearchSubSubSubSubAttribute() {
        final SubSub subSubSubSub1 = subSubRepository.save(new SubSub().setStrValue("subx").setIntValue(0));
        final SubSub subSubSubSub2 = subSubRepository.save(new SubSub().setStrValue("suby").setIntValue(1));
        final SubSub subSub1 = subSubRepository.save(new SubSub().setStrValue("subx").setIntValue(0).setSubSub(subSubSubSub1));
        final SubSub subSub2 = subSubRepository.save(new SubSub().setStrValue("suby").setIntValue(1).setSubSub(subSubSubSub2));
        final Sub sub1 = subRepository.save(new Sub().setSubSub(subSub1));
        final Sub sub2 = subRepository.save(new Sub().setSubSub(subSub2));
        final Root root1 = rootRepository.save(new Root().setSubEntity(sub1));
        final Root root2 = rootRepository.save(new Root().setSubEntity(sub1));
        final Root root3 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root4 = rootRepository.save(new Root().setSubEntity(sub2));
        final Root root5 = rootRepository.save(new Root()); // no sub set

        // by sub entity string
        assertThat(filter("subEntity.subSub.subSub.strValue==subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue==nostr")).isEmpty();
        // TODO / recheck - when missing entity shall it be included or not in != or =out=? - now it is
        assertThat(filter("subEntity.subSub.subSub.strValue!=subx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.subSub.strValue!=nostr")).hasSize(5);
        assertThat(filter("subEntity.subSub.subSub.strValue<suby")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue<=suby")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue>subx")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue>=subx")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue=in=subx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue=in=(subx, suby)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue=in=(subZ, subT)")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.strValue=out=subx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.subSub.strValue=out=(subx, suby)")).hasSize(1).containsExactlyInAnyOrder(root5);
        // wildcard, like
        assertThat(filter("subEntity.subSub.subSub.strValue==sub*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue==*bx")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue!=sub*")).hasSize(1).containsExactlyInAnyOrder(root5);
        assertThat(filter("subEntity.subSub.subSub.strValue!=*bx")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.subSub.strValue==*")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue!=*")).hasSize(1).containsExactlyInAnyOrder(root5);

        assertThat(filter("subEntity.subSub.subSub.strValue=is=null")).hasSize(1).containsExactlyInAnyOrder(root5);

        assertThat(filter("subEntity.subSub.subSub.strValue==*bx and subEntity.subSub.strValue==suby")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.strValue==*bx and subEntity.subSub.strValue!=subx")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.strValue==*bx and subEntity.subSub.strValue==subx"))
                .hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue==*bx or subEntity.subSub.strValue==suby"))
                .hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue==*bx or subEntity.subSub.strValue!=subx"))
                .hasSize(5).containsExactlyInAnyOrder(root1, root2, root3, root4, root5);
        assertThat(filter("subEntity.subSub.subSub.strValue==*bx or subEntity.subSub.strValue=is=null"))
                .hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);

        // by sub entity int
        assertThat(filter("subEntity.subSub.subSub.intValue==0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.intValue==2")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.intValue!=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.subSub.intValue!=2")).hasSize(5);
        assertThat(filter("subEntity.subSub.subSub.intValue<1")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.intValue<=1")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.intValue>0")).hasSize(2).containsExactlyInAnyOrder(root3, root4);
        assertThat(filter("subEntity.subSub.subSub.intValue>=0")).hasSize(4);
        assertThat(filter("subEntity.subSub.subSub.intValue=in=0")).hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.intValue=in=(0, 1)")).hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.intValue=in=(2, 3)")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.intValue=out=0")).hasSize(3).containsExactlyInAnyOrder(root3, root4, root5);
        assertThat(filter("subEntity.subSub.subSub.intValue=out=(0, 1)")).hasSize(1).containsExactlyInAnyOrder(root5);

        assertThat(filter("subEntity.subSub.subSub.intValue==0 and subEntity.subSub.intValue==1")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.intValue==0 and subEntity.subSub.intValue!=1")).hasSize(2)
                .containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.intValue==0 and subEntity.subSub.intValue==0")).hasSize(2)
                .containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.intValue==0 or subEntity.subSub.intValue==1")).hasSize(4)
                .containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.intValue==0 or subEntity.subSub.subSub.intValue!=1"))
                .hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);

        assertThat(filter("subEntity.subSub.subSub.strValue==subx and subEntity.subSub.intValue==1")).isEmpty();
        assertThat(filter("subEntity.subSub.subSub.strValue==subx and subEntity.subSub.intValue!=1")).hasSize(2)
                .containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue==subx and subEntity.subSub.intValue==0"))
                .hasSize(2).containsExactlyInAnyOrder(root1, root2);
        assertThat(filter("subEntity.subSub.subSub.strValue==subx or subEntity.subSub.intValue==1"))
                .hasSize(4).containsExactlyInAnyOrder(root1, root2, root3, root4);
        assertThat(filter("subEntity.subSub.subSub.strValue==subx or subEntity.subSub.intValue!=1"))
                .hasSize(3).containsExactlyInAnyOrder(root1, root2, root5);
    }

    private List<Root> filter(final String rsql) {
        // reference / auto filter (using elements and reflection)
        final EntityMatcher matcher = EntityMatcher.of(RsqlParser.parse(rsql));
        final List<Root> refResult = StreamSupport.stream(rootRepository.findAll().spliterator(), false).filter(matcher::match).toList();
        final List<Root> result = rootRepository.findAll(getSpecification(rsql));
        // auto check with reference result
        try {
            assertThat(result).containsExactlyInAnyOrder(refResult.toArray(Root[]::new));
        } catch (final AssertionError e) {
            log.error("Fail to get expected result for RSQL: {} with SQL query: {}",
                    rsql, new QlToSql(entityManager).toSQL(Root.class, null, rsql), e);
            throw e;
        }
        return result;
    }

    protected Specification<Root> getSpecification(final String rsql) {
        return builder.specification(RsqlParser.parse(rsql));
    }

    @SpringBootConfiguration
    static class Config {}
}