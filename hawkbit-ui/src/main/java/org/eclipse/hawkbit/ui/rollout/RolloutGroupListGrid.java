package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.eventbus.event.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.distributionbar.renderers.LinkRenderer;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;

@SpringComponent
@ViewScope
public class RolloutGroupListGrid extends AbstractSimpleGrid {
    private static final long serialVersionUID = 4060904914954370524L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient SpPermissionChecker permissionChecker;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUPS) {
            ((LazyQueryContainer) getContainerDataSource()).refresh();
        }
    }

    /**
     * 
     * Handles the RolloutGroupChangeEvent to refresh the item in the table.
     * 
     * 
     * @param rolloutGroupChangeEvent
     *            the event which contains the rollout group which has been
     *            change
     */
    @SuppressWarnings("unchecked")
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final RolloutGroupChangeEvent rolloutGroupChangeEvent) {
        if (rolloutUIState.isShowRolloutGroups()) {
            final RolloutGroup rolloutGroup = rolloutGroupManagement
                    .findRolloutGroupWithDetailedStatus(rolloutGroupChangeEvent.getRolloutGroupId());
            final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
            final Item item = rolloutContainer.getItem(rolloutGroup.getId());
            if (item != null) {
                item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).setValue(rolloutGroup.getStatus());
                item.getItemProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS)
                        .setValue(rolloutGroup.getTotalTargetCountStatus());

            }
        }
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutGroupBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutGroupBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGroupGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_ID, String.class, null, false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutGroupStatus.class, null,
                false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE,
                String.class, null, false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD, String.class,
                null, false, false);

        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD, String.class, null,
                false, false);

        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                false);

        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null,
                false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS,
                TotalTargetCountStatus.class, null, false, false);

    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_NAME).setMaximumWidth(150);

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMinimumWidth(95);

        getColumn(SPUILabelDefinitions.VAR_STATUS).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(75);

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setExpandRatio(2);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(400);

        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE).setMinimumWidth(80);

        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD).setMinimumWidth(100);

        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD).setMinimumWidth(140);

        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_DESC).setExpandRatio(0);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setHeaderCaption(i18n.get("header.name"));
        getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.get("header.status"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS)
                .setHeaderCaption(i18n.get("header.detail.status"));
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE)
                .setHeaderCaption(i18n.get("header.rolloutgroup.installed.percentage"));
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD)
                .setHeaderCaption(i18n.get("header.rolloutgroup.threshold.error"));
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD)
                .setHeaderCaption(i18n.get("header.rolloutgroup.threshold"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setHeaderCaption(i18n.get("header.createdBy"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
        getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.get("header.description"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setHeaderCaption(i18n.get("header.total.targets"));
    }

    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.ROLLOUT_GROUP_LIST_TABLE_ID;
    }

    @Override
    protected void setColumnProperties() {
        List<Object> columnList = new ArrayList<>();
        columnList.add(SPUILabelDefinitions.VAR_NAME);
        columnList.add(SPUILabelDefinitions.VAR_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS);
        columnList.add(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE);
        columnList.add(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD);
        columnList.add(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnList.add(SPUILabelDefinitions.VAR_DESC);
        setColumnOrder(columnList.toArray());

        alignColumns();
    }

    private void alignColumns() {
        setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 5573570647129792429L;

            @Override
            public String getStyle(final CellReference cellReference) {
                String[] coulmnNames = { SPUILabelDefinitions.VAR_STATUS,
                        SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS };
                if (Arrays.asList(coulmnNames).contains(cellReference.getPropertyId())) {
                    return "centeralign";
                }
                return null;
            }
        });
    }

    @Override
    protected void addColumnRenderes() {
        addDetailStatusColumn();
        addStatusCoulmn();
        if (permissionChecker.hasRolloutTargetsReadPermission()) {
            getColumn(SPUILabelDefinitions.VAR_NAME)
                    .setRenderer(new LinkRenderer(event -> onClickOfRolloutGroupName(event)));
        }
    }

    private void onClickOfRolloutGroupName(RendererClickEvent event) {
        rolloutUIState
                .setRolloutGroup(rolloutGroupManagement.findRolloutGroupWithDetailedStatus((Long) event.getItemId()));
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS);
    }

    private void addStatusCoulmn() {
        getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlRenderer(),
                new Converter<String, RolloutGroupStatus>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public RolloutGroupStatus convertToModel(final String value,
                            final Class<? extends RolloutGroupStatus> targetType, final Locale locale) {
                        return null;
                    }

                    @Override
                    public String convertToPresentation(final RolloutGroupStatus value,
                            final Class<? extends String> targetType, final Locale locale) {
                        String result = null;
                        switch (value) {
                        case FINISHED:
                            result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                            break;
                        case SCHEDULED:
                            result = "<div class=\"statusIconBlue\">" + FontAwesome.BULLSEYE.getHtml() + "</div>";
                            break;
                        case RUNNING:
                            result = "<div class=\"statusIconYellow\"><span  title=\"xxx\">" + FontAwesome.ADJUST.getHtml() + "</span></div>";
                            break;
                        case READY:
                            result = "<div class=\"statusIconLightBlue\"> <span title=\"xxx\">"
                                    + FontAwesome.DOT_CIRCLE_O.getHtml() + "</span></div>";
                            break;
                        case ERROR:
                            result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml()
                                    + "</div>";
                            break;
                        default:
                            break;
                        }
                        return result;
                    }

                    @Override
                    public Class<RolloutGroupStatus> getModelType() {
                        return RolloutGroupStatus.class;
                    }

                    @Override
                    public Class<String> getPresentationType() {
                        return String.class;
                    }
                });
    }

    private void addDetailStatusColumn() {
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(
                new org.eclipse.hawkbit.ui.distributionbar.renderers.StringDistributionBarRenderer(),
                new Converter<String, TotalTargetCountStatus>() {
                    private static final long serialVersionUID = 2660476405836705932L;

                    @Override
                    public TotalTargetCountStatus convertToModel(String value,
                            Class<? extends TotalTargetCountStatus> targetType, Locale locale)
                                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                        return null;
                    }

                    @Override
                    public String convertToPresentation(TotalTargetCountStatus value,
                            Class<? extends String> targetType, Locale locale)
                                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                        return HawkbitCommonUtil.getFormattedString(value.getStatusTotalCountMap());
                    }

                    @Override
                    public Class<TotalTargetCountStatus> getModelType() {
                        return TotalTargetCountStatus.class;
                    }

                    @Override
                    public Class<String> getPresentationType() {
                        return String.class;
                    }
                });
    }

    @Override
    protected void setHiddenColumns() {
        List<Object> columnsToBeHidden = new ArrayList<>();
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_DESC);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_ID);
        for (Object propertyId : columnsToBeHidden) {
            getColumn(propertyId).setHidden(true);
        }
    }

}
