/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.dstable.DistributionBeanQuery;
import org.eclipse.hawkbit.ui.management.event.BulkUploadPopupEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.state.TargetBulkUpload;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.tokenfield.TokenField;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Bulk target upload layout.
 * 
 *
 *
 */
@SpringComponent
@UIScope
public class TargetBulkUpdateWindowLayout extends CustomComponent {

    @Autowired
    private I18N i18n;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private TargetBulkTokenTags targetBulkTokenTags;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private transient DeploymentManagement deploymentManagement;

    @Autowired
    private transient UiProperties uiproperties;

    private static final long serialVersionUID = -6659290471705262389L;
    private VerticalLayout tokenVerticalLayout;
    private TextArea descTextArea;
    private ComboBox dsNamecomboBox;
    private BulkUploadHandler bulkUploader;
    private VerticalLayout mainLayout;
    private ProgressBar progressBar;
    private Label targetsCountLabel;
    private Link linkToSystemConfigHelp;
    private Window bulkUploadWindow;
    private Label windowCaption;
    private Button minimizeButton;
    private Button closeButton;

    /**
     * Initialize the Add Update Window Component for Target.
     */
    public void init() {
        createRequiredComponents();
        buildLayout();
        setImmediate(true);
        setCompositionRoot(mainLayout);
    }

    protected void onStartOfUpload() {
        final TargetBulkUpload targetBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload();
        targetBulkUpload.setDsNameAndVersion((DistributionSetIdName) dsNamecomboBox.getValue());
        targetBulkUpload.setDescription(descTextArea.getValue());
        targetBulkUpload.setProgressBarCurrentValue(0F);
        targetBulkUpload.setFailedUploadCount(0L);
        targetBulkUpload.setSucessfulUploadCount(0L);
        closeButton.setEnabled(false);
        minimizeButton.setEnabled(true);
    }

    protected void setProgressBarValue(final Float value) {
        progressBar.setValue(value);
        progressBar.setVisible(true);
    }

    private void createRequiredComponents() {
        tokenVerticalLayout = getTokenFieldLayout();
        dsNamecomboBox = getDsComboField();
        descTextArea = getDescriptionTextArea();
        progressBar = creatreProgressBar();
        targetsCountLabel = getStatusCountLabel();
        bulkUploader = getBulkUploadHandler();
        linkToSystemConfigHelp = SPUIComponentProvider
                .getHelpLink(uiproperties.getLinks().getDocumentation().getDeploymentView());
        windowCaption = new Label(i18n.get("caption.bulk.upload.targets"));
        minimizeButton = getMinimizeButton();
        closeButton = getCloseButton();
    }

    private ProgressBar creatreProgressBar() {
        final ProgressBar progressBarIndicator = new ProgressBar(0F);
        progressBarIndicator.addStyleName("bulk-upload-label");
        progressBarIndicator.setSizeFull();
        return progressBarIndicator;
    }

    private Button getCloseButton() {
        final Button closeBtn = SPUIComponentProvider.getButton(UIComponentIdProvider.BULK_UPLOAD_CLOSE_BUTTON_ID, "",
                "", "", true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        closeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        closeBtn.addClickListener(event -> closePopup());
        return closeBtn;
    }

    private Button getMinimizeButton() {
        final Button minimizeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.BULK_UPLOAD_MINIMIZE_BUTTON_ID, "", "", "", true, FontAwesome.MINUS,
                SPUIButtonStyleSmallNoBorder.class);
        minimizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        minimizeBtn.addClickListener(event -> minimizeWindow());
        minimizeBtn.setEnabled(false);
        return minimizeBtn;
    }

    private BulkUploadHandler getBulkUploadHandler() {
        final BulkUploadHandler bulkUploadHandler = new BulkUploadHandler(this, targetManagement, managementUIState,
                deploymentManagement, i18n, UI.getCurrent());
        bulkUploadHandler.buildLayout();
        bulkUploadHandler.addStyleName(SPUIStyleDefinitions.BULK_UPLOAD_BUTTON);
        return bulkUploadHandler;
    }

    private Label getStatusCountLabel() {
        final Label countLabel = new Label();
        countLabel.setImmediate(true);
        countLabel.addStyleName("bulk-upload-label");
        countLabel.setVisible(false);
        countLabel.setCaptionAsHtml(true);
        countLabel.setId(UIComponentIdProvider.BULK_UPLOAD_COUNT);
        return countLabel;
    }

    private TextArea getDescriptionTextArea() {
        final TextArea description = new TextAreaBuilder().caption(i18n.get("textfield.description"))
                .style("text-area-style").prompt(i18n.get("textfield.description")).immediate(true)
                .id(UIComponentIdProvider.BULK_UPLOAD_DESC).buildTextComponent();
        description.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        description.setWidth("100%");
        return description;
    }

    private ComboBox getDsComboField() {
        final Container container = createContainer();
        final ComboBox dsComboBox = SPUIComponentProvider.getComboBox(i18n.get("bulkupload.ds.name"), "", null, null,
                false, "", i18n.get("bulkupload.ds.name"));
        dsComboBox.setSizeUndefined();
        dsComboBox.addStyleName(SPUIDefinitions.BULK_UPLOD_DS_COMBO_STYLE);
        dsComboBox.setImmediate(true);
        dsComboBox.setFilteringMode(FilteringMode.STARTSWITH);
        dsComboBox.setPageLength(7);
        dsComboBox.setContainerDataSource(container);
        dsComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME_VERSION);
        dsComboBox.setId(UIComponentIdProvider.BULK_UPLOAD_DS_COMBO);
        dsComboBox.setWidth("100%");
        return dsComboBox;
    }

    private VerticalLayout getTokenFieldLayout() {
        final TokenField tokenField = targetBulkTokenTags.getTokenField();
        final VerticalLayout tokenLayout = SPUIComponentProvider.getDetailTabLayout();
        tokenLayout.addStyleName("bulk-target-tags-layout");
        tokenLayout.addComponent(tokenField);
        tokenLayout.setSpacing(false);
        tokenLayout.setMargin(false);
        tokenLayout.setSizeFull();
        tokenLayout.setHeight("100px");
        tokenLayout.setId(UIComponentIdProvider.BULK_UPLOAD_TAG);
        return tokenLayout;
    }

    private void closePopup() {
        clearPreviousSessionData();
        bulkUploadWindow.close();
        eventBus.publish(this, BulkUploadPopupEvent.CLOSED);
    }

    /**
     * @return
     */
    private Container createContainer() {

        final Map<String, Object> queryConfiguration = Maps.newHashMapWithExpectedSize(2);

        final List<String> list = new ArrayList<>();
        queryConfiguration.put(SPUIDefinitions.FILTER_BY_NO_TAG,
                managementUIState.getDistributionTableFilters().isNoTagSelected());

        if (!managementUIState.getDistributionTableFilters().getDistSetTags().isEmpty()) {
            list.addAll(managementUIState.getDistributionTableFilters().getDistSetTags());
        }
        queryConfiguration.put(SPUIDefinitions.FILTER_BY_TAG, list);

        final BeanQueryFactory<DistributionBeanQuery> distributionQF = new BeanQueryFactory<>(
                DistributionBeanQuery.class);
        distributionQF.setQueryConfiguration(queryConfiguration);
        final LazyQueryContainer distributionContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_DIST_ID_NAME),
                distributionQF);

        return distributionContainer;

    }

    private void buildLayout() {
        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();
        mainLayout.setWidth("200px");

        final HorizontalLayout captionLayout = new HorizontalLayout();
        captionLayout.setSizeFull();
        captionLayout.addComponents(windowCaption, minimizeButton, closeButton);
        captionLayout.setExpandRatio(windowCaption, 1.0F);
        captionLayout.addStyleName("v-window-header");

        final HorizontalLayout uploaderLayout = new HorizontalLayout();
        uploaderLayout.addComponent(bulkUploader);
        uploaderLayout.addComponent(linkToSystemConfigHelp);
        uploaderLayout.setComponentAlignment(linkToSystemConfigHelp, Alignment.BOTTOM_RIGHT);
        uploaderLayout.setExpandRatio(bulkUploader, 1.0F);
        uploaderLayout.setSizeFull();
        mainLayout.addComponents(captionLayout, dsNamecomboBox, descTextArea, tokenVerticalLayout, descTextArea,
                progressBar, targetsCountLabel, uploaderLayout);
    }

    /**
     * Reset the values in popup.
     */
    public void resetComponents() {
        dsNamecomboBox.clear();
        descTextArea.clear();
        targetBulkTokenTags.getTokenField().clear();
        targetBulkTokenTags.populateContainer();
        progressBar.setValue(0F);
        progressBar.setVisible(false);
        managementUIState.getTargetTableFilters().getBulkUpload().setProgressBarCurrentValue(0F);
        targetsCountLabel.setVisible(false);
    }

    private void clearPreviousSessionData() {
        final TargetBulkUpload targetBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload();
        targetBulkUpload.setDescription(null);
        targetBulkUpload.setDsNameAndVersion(null);
        targetBulkUpload.setFailedUploadCount(0L);
        targetBulkUpload.setSucessfulUploadCount(0L);
        targetBulkUpload.getAssignedTagNames().clear();
        targetBulkUpload.getTargetsCreated().clear();
    }

    /**
     * Restore the target bulk upload layout field values.
     */
    public void restoreComponentsValue() {
        targetBulkTokenTags.populateContainer();
        final TargetBulkUpload targetBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload();
        progressBar.setValue(targetBulkUpload.getProgressBarCurrentValue());
        dsNamecomboBox.setValue(targetBulkUpload.getDsNameAndVersion());
        descTextArea.setValue(targetBulkUpload.getDescription());
        targetBulkTokenTags.addAlreadySelectedTags();
        if (targetBulkUpload.getSucessfulUploadCount() > 0 || targetBulkUpload.getFailedUploadCount() > 0) {
            targetsCountLabel.setVisible(true);
            targetsCountLabel.setCaption(getFormattedCountLabelValue(targetBulkUpload.getSucessfulUploadCount(),
                    targetBulkUpload.getFailedUploadCount()));
        }
        if (targetBulkUpload.getProgressBarCurrentValue() < 1) {
            bulkUploader.getUpload().setEnabled(false);
        }
    }

    /**
     * Actions once bulk upload is completed.
     */
    public void onUploadCompletion() {
        final TargetBulkUpload targetBulkUpload = managementUIState.getTargetTableFilters().getBulkUpload();

        final String targetCountLabel = getFormattedCountLabelValue(targetBulkUpload.getSucessfulUploadCount(),
                targetBulkUpload.getFailedUploadCount());
        getTargetsCountLabel().setVisible(true);
        getTargetsCountLabel().setCaption(targetCountLabel);

        getBulkUploader().getUpload().setEnabled(true);

        closeButton.setEnabled(true);
        minimizeButton.setEnabled(false);
    }

    private static String getFormattedCountLabelValue(final long succussfulUploadCount, final long failedUploadCount) {
        return new StringBuilder().append("Successful :").append(succussfulUploadCount)
                .append("<font color=RED> Failed :").append(failedUploadCount).append("</font>").toString();
    }

    /**
     * create and return window
     * 
     * @return Window window
     */
    public Window getWindow() {
        managementUIState.setBulkUploadWindowMinimised(false);

        bulkUploadWindow = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption("").content(this)
                .buildWindow();
        bulkUploadWindow.addStyleName("bulk-upload-window");
        bulkUploadWindow.setImmediate(true);
        if (isNoBulkUploadInProgress()) {
            bulkUploader.getUpload().setEnabled(true);
        }
        return bulkUploadWindow;
    }

    private boolean isNoBulkUploadInProgress() {
        final float progressBarCurrentValue = managementUIState.getTargetTableFilters().getBulkUpload()
                .getProgressBarCurrentValue();
        return Math.abs(progressBarCurrentValue - 0) < 0.00001 || Math.abs(progressBarCurrentValue - 1) < 0.00001;
    }

    private void minimizeWindow() {
        bulkUploadWindow.close();
        managementUIState.setBulkUploadWindowMinimised(true);
        eventBus.publish(this, BulkUploadPopupEvent.MINIMIZED);
    }

    /**
     * @return the descTextArea
     */
    public TextArea getDescTextArea() {
        return descTextArea;
    }

    /**
     * @return the dsNamecomboBox
     */
    public ComboBox getDsNamecomboBox() {
        return dsNamecomboBox;
    }

    /**
     * @return the progressBar
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * @return the targetBulkTokenTags
     */
    public TargetBulkTokenTags getTargetBulkTokenTags() {
        return targetBulkTokenTags;
    }

    /**
     * @return the targetsCountLabel
     */
    public Label getTargetsCountLabel() {
        return targetsCountLabel;
    }

    /**
     * @return the eventBus
     */
    public EventBus.SessionEventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return the bulkUploader
     */
    public BulkUploadHandler getBulkUploader() {
        return bulkUploader;
    }

}
