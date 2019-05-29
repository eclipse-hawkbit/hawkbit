package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Target Filter")
public class TargetFilterTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests the UI-filter functions")
    public void filterShouldNotFindTargetsForNotExistingTag() {
        assertThat(targetManagement.countByFilters(FilterParams.forTags("X"))).as("Target count is wrong").isEqualTo(0);
    }

    @Test
    @Description("Tests the UI-filter functions")
    public void shouldOnlyFindTaggedTargets() {
        TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("TEST-TAG"));
        Target target1 = targetManagement.create(
                entityFactory.target().create().controllerId("target-1").name("Name1"));
        targetManagement.create(entityFactory.target().create().controllerId("target-2").name("Name2"));
        targetManagement.assignTag(Arrays.asList(target1.getControllerId()), tag.getId());

        assertThat(targetManagement.countByFilters(FilterParams.forTags(tag.getName()))).as("Target count is wrong")
                .isEqualTo(1);
    }

    @Test
    @Description("Tests the UI-filter functions")
    public void shouldApplyAsSoonAsTagsAreProvided() {
        FilterParams filter = FilterParams.forTags("1", "2", "3");
        assertThat(filter.hasTagsFilterActive()).as("Has tags should evaluate to true").isTrue();
    }

    @Test
    @Description("Tests the UI-filter functions")
    public void shouldApplyWhenTagsHaveBeenSelected() {
        FilterParams filter = new FilterParams(null,null,null,null,true,null);
        assertThat(filter.hasTagsFilterActive()).as("Has tags should evaluate to true").isTrue();
    }
}
