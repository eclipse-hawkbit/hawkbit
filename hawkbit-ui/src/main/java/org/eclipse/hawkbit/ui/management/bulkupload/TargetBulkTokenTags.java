/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.bulkupload;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TagToProxyTagMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.util.CollectionUtils;

/**
 * Target tag layout in bulk upload popup.
 *
 */
public class TargetBulkTokenTags extends AbstractTagToken<ProxyTarget> {
    private final TargetTagManagement tagManagement;

    private final TagToProxyTagMapper<TargetTag> tagMapper;

    TargetBulkTokenTags(final CommonUiDependencies uiDependencies, final TargetTagManagement tagManagement) {
        super(uiDependencies);

        this.tagManagement = tagManagement;
        this.tagMapper = new TagToProxyTagMapper<>();
    }

    @Override
    public void assignTag(final ProxyTag tagData) {
        tagPanelLayout.setAssignedTag(tagData);
    }

    @Override
    public void unassignTag(final ProxyTag tagData) {
        tagPanelLayout.removeAssignedTag(tagData);
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasCreateTargetPermission();
    }

    @Override
    protected List<ProxyTag> getAllTags() {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(tagManagement::findAll).stream()
                .map(tag -> new ProxyTag(tag.getId(), tag.getName(), tag.getColour())).collect(Collectors.toList());
    }

    @Override
    protected List<ProxyTag> getAssignedTags() {
        // this view doesn't belong to a specific target, so the current
        // selected target in the target table is ignored and therefore there
        // are no assigned tags
        return Collections.emptyList();
    }

    /**
     * @return List of assigned tags
     */
    public List<ProxyTag> getSelectedTagsForAssignment() {
        return tagPanelLayout.getAssignedTags();
    }

    @Override
    protected List<ProxyTag> getTagsById(final Collection<Long> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return Collections.emptyList();
        }

        return tagManagement.get(entityIds).stream().map(tagMapper::map).collect(Collectors.toList());
    }
}
