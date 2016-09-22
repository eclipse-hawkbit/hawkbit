/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.event.bulk;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the bulk remote events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public class RemoteBulkEntityEventTest extends AbstractJpaIntegrationTest {

    // @Test
    // @Description("Verifies that the target tag entities reloading by remote
    // events works")
    // public void reloadTargetTagBulkEntityByRemoteEvent() {
    // final List<TargetTag> targetTags = tagManagement.createTargetTags(
    // Arrays.asList(entityFactory.generateTargetTag("tag1"),
    // entityFactory.generateTargetTag("tag2")));
    //
    // final List<Long> ids = targetTags.stream().map(tag ->
    // tag.getId()).collect(Collectors.toList());
    //
    // final TargetTag targetTag = targetTags.get(0);
    // final TargetTagCreatedBulkEvent bulkEvent = new
    // TargetTagCreatedBulkEvent(targetTag.getTenant(), ids,
    // targetTag.getClass(), "node");
    // assertThat(bulkEvent.getEntities(),
    // Matchers.containsInAnyOrder(targetTags.toArray()));
    //
    // }
    //
    // @Test
    // @Description("Verifies that the ds tag entity reloading by remote events
    // works")
    // public void reloadDsTagBulkEntityByRemoteEvent() {
    // final List<DistributionSetTag> dsTags =
    // tagManagement.createDistributionSetTags(Arrays.asList(
    // entityFactory.generateDistributionSetTag("tag1"),
    // entityFactory.generateDistributionSetTag("tag2")));
    // final DistributionSetTag dsTag = dsTags.get(0);
    //
    // final List<Long> ids = dsTags.stream().map(tag ->
    // tag.getId()).collect(Collectors.toList());
    //
    // final DistributionSetTagCreatedBulkEvent createdBulkEvent = new
    // DistributionSetTagCreatedBulkEvent(
    // dsTag.getTenant(), ids, dsTag.getClass(), "Node");
    // assertThat(createdBulkEvent.getEntities(),
    // Matchers.containsInAnyOrder(dsTags));
    //
    // }

}
