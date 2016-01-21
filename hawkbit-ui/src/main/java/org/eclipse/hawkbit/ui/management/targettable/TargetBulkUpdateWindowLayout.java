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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.documentation.DocumentationPageLink;
import org.eclipse.hawkbit.ui.management.dstable.DistributionBeanQuery;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.tokenfield.TokenField;

import com.vaadin.data.Container;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Bulk target upload layout.
 * 
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetBulkUpdateWindowLayout extends CustomComponent implements CloseListener {

    @Autowired
    private I18N i18n;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private transient UINotification uINotification;

    @Autowired
    private TargetBulkTokenTags targetBulkTokenTags;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private transient DeploymentManagement deploymentManagement;

    private static final long serialVersionUID = -6659290471705262389L;
    private VerticalLayout tokenVerticalLayout;
    private TextArea descTextArea;
    private ComboBox dsNamecomboBox;
    private BulkUploadHandler bulkUploader;
    private VerticalLayout mainLayout;
    private ProgressBar progressBar;
    private Label targetsCountLabel;
    private Link linkToSystemConfigHelp;

    /**
     * Initialize the Add Update Window Component for Target.
     */
    public void init() {
        createRequiredComponents();
        buildLayout();
        setCompositionRoot(mainLayout);
    }

    /**
    * 
    */
    private void createRequiredComponents() {

        final TokenField tokenField = targetBulkTokenTags.getTokenField();
        tokenVerticalLayout = SPUIComponentProvider.getDetailTabLayout();
        tokenVerticalLayout.addStyleName("bulk-target-tags-layout");
        tokenVerticalLayout.addComponent(tokenField);
        tokenVerticalLayout.setSpacing(false);
        tokenVerticalLayout.setMargin(false);
        tokenVerticalLayout.setWidth("185px");
        tokenVerticalLayout.setHeight("100px");
        tokenVerticalLayout.setId(SPUIComponetIdProvider.BULK_UPLOAD_TAG);

        final Container container = createContainer();
        dsNamecomboBox = SPUIComponentProvider.getComboBox("", "", null, null, false, "",
                i18n.get("bulkupload.ds.name"));
        dsNamecomboBox.addStyleName(SPUIDefinitions.BULK_UPLOD_DS_COMBO_STYLE);
        dsNamecomboBox.setImmediate(true);
        dsNamecomboBox.setFilteringMode(FilteringMode.STARTSWITH);
        dsNamecomboBox.setPageLength(7);
        dsNamecomboBox.setContainerDataSource(container);
        dsNamecomboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        dsNamecomboBox.setId(SPUIComponetIdProvider.BULK_UPLOAD_DS_COMBO);

        descTextArea = SPUIComponentProvider.getTextArea("text-area-style", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponetIdProvider.BULK_UPLOAD_DESC);
        descTextArea.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        progressBar = new ProgressBar(0.5f);
        progressBar.setWidth(185f, Unit.PIXELS);
        progressBar.addStyleName("bulk-upload-label");

        targetsCountLabel = new Label();
        targetsCountLabel.setImmediate(false);
        targetsCountLabel.addStyleName("bulk-upload-label");
        targetsCountLabel.setVisible(false);
        targetsCountLabel.setId(SPUIComponetIdProvider.BULK_UPLOAD_COUNT);
        bulkUploader = new BulkUploadHandler(this, targetManagement, managementUIState, deploymentManagement,
                uINotification, i18n);
        bulkUploader.buildLayout();
        bulkUploader.addStyleName(SPUIStyleDefinitions.BULK_UPLOAD_BUTTON);

        linkToSystemConfigHelp = DocumentationPageLink.DEPLOYMENT_VIEW.getLink();

    }

    /**
     * @return
     */
    private Container createContainer() {

        final Map<String, Object> queryConfiguration = new HashMap<>();

        final List<String> list = new ArrayList<>();
        queryConfiguration.put(SPUIDefinitions.FILTER_BY_NO_TAG, managementUIState.getDistributionTableFilters()
                .isNoTagSelected());

        if (!managementUIState.getDistributionTableFilters().getDistSetTags().isEmpty()) {
            list.addAll(managementUIState.getDistributionTableFilters().getDistSetTags());
        }
        queryConfiguration.put(SPUIDefinitions.FILTER_BY_TAG, list);

        final BeanQueryFactory<DistributionBeanQuery> distributionQF = new BeanQueryFactory<>(
                DistributionBeanQuery.class);
        distributionQF.setQueryConfiguration(queryConfiguration);
        final LazyQueryContainer distributionContainer = new LazyQueryContainer(new LazyQueryDefinition(true,
                Integer.MAX_VALUE, SPUILabelDefinitions.VAR_DIST_ID_NAME), distributionQF);
        return distributionContainer;

    }

    private void buildLayout() {
        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();
        final HorizontalLayout uploaderLayout = new HorizontalLayout();
        uploaderLayout.addComponent(bulkUploader);
        uploaderLayout.addComponent(linkToSystemConfigHelp);
        uploaderLayout.setComponentAlignment(linkToSystemConfigHelp, Alignment.BOTTOM_RIGHT);
        uploaderLayout.setExpandRatio(bulkUploader, 1.0F);
        uploaderLayout.setSizeFull();
        mainLayout.addComponents(dsNamecomboBox, descTextArea, tokenVerticalLayout, descTextArea, progressBar,
                targetsCountLabel, uploaderLayout);
    }

    /**
     * Reset the values in popup.
     */
    public void resetComponents() {
        dsNamecomboBox.clear();
        descTextArea.clear();
        targetBulkTokenTags.getTokenField().clear();
        targetBulkTokenTags.populateContainer();
        progressBar.setValue(0f);
        progressBar.setVisible(false);
        targetsCountLabel.setVisible(false);

    }

    /**
     * @return
     */
    public Window getWindow() {
        final Window bulkUploadWindow = SPUIComponentProvider.getWindow(i18n.get("caption.bulk.upload.targets"), null,
                SPUIDefinitions.BULK_UPLOAD_WINDOW);
        bulkUploadWindow.setContent(this);
        bulkUploadWindow.addCloseListener(this);
        return bulkUploadWindow;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Window.CloseListener#windowClose(com.vaadin.ui.Window.
     * CloseEvent)
     */
    @Override
    public void windowClose(final CloseEvent e) {
        e.getWindow().close();

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

}
