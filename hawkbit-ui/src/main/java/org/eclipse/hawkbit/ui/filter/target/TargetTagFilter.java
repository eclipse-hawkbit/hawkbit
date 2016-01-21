/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.filter.target;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.filter.FilterExpression;
import org.springframework.util.CollectionUtils;

/**
 *
 *
 *
 */
public class TargetTagFilter implements FilterExpression {

    private final Target target;
    private final Collection<String> tags;
    private final boolean noTag;

    /**
     * @param target
     *            the target to check the filter against
     * @param tags
     *            the tags to check the target against it
     * @param noTag
     *            {@code true} indicates that targets which have no tags should
     *            not be filtered, otherwise {@code false}
     */
    public TargetTagFilter(final Target target, final Collection<String> tags, final boolean noTag) {
        this.target = target;
        this.tags = tags;
        this.noTag = noTag;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.filter.FilterExpression#evaluate()
     */
    @Override
    public boolean doFilter() {
        final List<String> targetTags = target.getTags().stream().map(targetTag -> targetTag.getName())
                .collect(Collectors.toList());
        if (targetTags.isEmpty() || (noTag && targetTags.isEmpty())) {
            return false;
        }
        return !CollectionUtils.containsAny(targetTags, tags);
    }
}
