package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
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
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

@SpringComponent
@ViewScope
public class RolloutGroupTargetsListGrid extends AbstractSimpleGrid {

    private static final long serialVersionUID = -2244756637458984597L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RolloutUIState rolloutUIState;

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
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS) {
            ((LazyQueryContainer) getContainerDataSource()).refresh();
            eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS_COUNT);
        }
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutGroupTargetsBeanQuery> rolloutgrouBeanQueryFactory = new BeanQueryFactory<>(
                RolloutGroupTargetsBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID),
                rolloutgrouBeanQueryFactory);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGroupTargetGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false,
                true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, Status.class,
                Status.RETRIEVED, false, false);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null,
                false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class,
                null, false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null,
                false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class,
                null, false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false,
                true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER,
                String.class, "", false, true);
    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setExpandRatio(2);
        getColumn(SPUILabelDefinitions.VAR_NAME).setMinimumWidth(200);

        getColumn(SPUILabelDefinitions.VAR_STATUS).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(150);

        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setMaximumWidth(250);

        getColumn(SPUILabelDefinitions.VAR_CREATED_BY).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_CREATED_BY).setMaximumWidth(250);

        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setMaximumWidth(250);

        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setMaximumWidth(250);

        getColumn(SPUILabelDefinitions.VAR_DESC).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_DESC).setMaximumWidth(300);

        getColumn(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER).setExpandRatio(0);
        setFrozenColumnCount(getColumns().size());

    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setHeaderCaption(i18n.get("header.name"));
        getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.get("header.status"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_BY).setHeaderCaption(i18n.get("header.createdBy"));
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
        getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.get("header.description"));
        getColumn(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER).setHeaderCaption(i18n.get("header.assigned.ds"));

    }

    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.ROLLOUT_GROUP_TARGETS_LIST_TABLE_ID;
    }

    @Override
    protected void setColumnProperties() {
        List<Object> columnList = new ArrayList<>();
        columnList.add(SPUILabelDefinitions.VAR_NAME);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_BY);
        columnList.add(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY);
        columnList.add(SPUILabelDefinitions.VAR_DESC);
        columnList.add(SPUILabelDefinitions.VAR_STATUS);
        setColumnOrder(columnList.toArray());
        alignColumns();
    }

    private void alignColumns() {
        setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 5573570647129792429L;

            @Override
            public String getStyle(final CellReference cellReference) {
                if (SPUILabelDefinitions.VAR_STATUS.equals(cellReference.getPropertyId())) {
                    return "centeralign";
                }
                return null;
            }
        });
    }

    @Override
    protected void addColumnRenderes() {
        addStatusCoulmn();
    }

    @Override
    protected void setHiddenColumns() {
        getColumn(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER).setHidden(true);
    }

    private void addStatusCoulmn() {
        getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlLabelRenderer(), new Converter<String, Status>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Status convertToModel(final String value, final Class<? extends Status> targetType,
                    final Locale locale) {
                return null;
            }

            @Override
            public String convertToPresentation(final Status status, final Class<? extends String> targetType,
                    final Locale locale) {
                String result = null;
                if (status == null) {
                    // Actions are not created for targets when rollout's status
                    // is READY and when duplicate assignment is done.
                    // In these cases display a appropriate status with
                    // description
                    return getStatus();
                } else {
                    switch (status) {
                    case FINISHED:
                        result = HawkbitCommonUtil.getStatusLabelDetailsInString(
                                Integer.toString(FontAwesome.CHECK_CIRCLE.getCodepoint()), status.name().toLowerCase(),
                                "statusIconGreen", null);
                        break;
                    case SCHEDULED:
                        result = HawkbitCommonUtil.getStatusLabelDetailsInString(
                                Integer.toString(FontAwesome.BULLSEYE.getCodepoint()), status.name().toLowerCase(),
                                "statusIconBlue", null);
                        break;
                    case RUNNING:
                    case RETRIEVED:
                    case WARNING:
                    case DOWNLOAD:
                        result = HawkbitCommonUtil.getStatusLabelDetailsInString(
                                Integer.toString(FontAwesome.ADJUST.getCodepoint()), status.name().toLowerCase(),
                                "statusIconYellow", null);
                        break;
                    case CANCELING:
                        result = HawkbitCommonUtil.getStatusLabelDetailsInString(
                                Integer.toString(FontAwesome.TIMES_CIRCLE.getCodepoint()), status.name().toLowerCase(),
                                "statusIconPending", null);
                        break;
                    case CANCELED:
                        result = HawkbitCommonUtil.getStatusLabelDetailsInString(
                                Integer.toString(FontAwesome.TIMES_CIRCLE.getCodepoint()), status.name().toLowerCase(),
                                "statusIconGreen", null);
                        break;
                    case ERROR:
                        result = HawkbitCommonUtil.getStatusLabelDetailsInString(
                                Integer.toString(FontAwesome.EXCLAMATION_CIRCLE.getCodepoint()),
                                status.name().toLowerCase(), "statusIconRed", null);
                        break;
                    default:
                        break;
                    }
                    return result;
                }
            }

            @Override
            public Class<Status> getModelType() {
                return Status.class;
            }

            @Override
            public Class<String> getPresentationType() {
                return String.class;
            }
        });
    }

    private String getStatus() {
        final RolloutGroup rolloutGroup = rolloutUIState.getRolloutGroup().isPresent()
                ? rolloutUIState.getRolloutGroup().get() : null;
        if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.READY) {
            return HawkbitCommonUtil.getStatusLabelDetailsInString(
                    Integer.toString(FontAwesome.DOT_CIRCLE_O.getCodepoint()), RolloutGroupStatus.READY.toString().toLowerCase(),
                    "statusIconLightBlue", null);
        } else if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.FINISHED) {
            String ds =  rolloutUIState.getRolloutDistributionSet().isPresent()? rolloutUIState.getRolloutDistributionSet() .get():"";
            ds = ds.replace(":", "-");
            return HawkbitCommonUtil.getStatusLabelDetailsInString(
                    Integer.toString(FontAwesome.MINUS_CIRCLE.getCodepoint()),
                    i18n.get("message.dist.already.assigned", new Object[] { ds }), "statusIconBlue", null);
        }
        return null;
    }
}
