package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.hawkbit.repository.SpPermissionChecker;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus.SessionEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

public class DistributionSetMetadatadetailslayout extends Table{
    
    private static final long serialVersionUID = 2913758299611837718L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetMetadatadetailslayout.class);

    private static final String METADATA_KEY = "Key";
    
    private static final String VIEW ="view";

    private SpPermissionChecker permissionChecker;

    private I18N i18n;

   /**
     * Initialize software module table- to be displayed in details layout.
     * 
     * @param i18n
     *            I18N
     * @param isUnassignSoftModAllowed
     *            boolean flag to check for unassign functionality allowed for
     *            the view.
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param permissionChecker
     *            SpPermissionChecker
     * @param eventBus
     *            SessionEventBus
     * @param manageDistUIState
     *            ManageDistUIState
     */
    public void init(final I18N i18n, final SpPermissionChecker permissionChecker) {
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        createDSMetadataTable();
        addCustomGeneratedColumns();
    }

    private void createDSMetadataTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getSwModuleContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addDSMetadataTableHeader();
        setSizeFull(); // check if this style is required
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
    }

    private IndexedContainer getSwModuleContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(METADATA_KEY, String.class, "");
        setColumnExpandRatio(METADATA_KEY, 0.7f);
        setColumnAlignment(METADATA_KEY, Align.LEFT);
        
        if (permissionChecker.hasUpdateDistributionPermission()) {
            container.addContainerProperty(VIEW, Label.class, "");
            setColumnExpandRatio(VIEW, 0.2F);
            setColumnAlignment(VIEW, Align.RIGHT);
        }
        return container;
    }

    private void addDSMetadataTableHeader() {
        setColumnHeader(METADATA_KEY, i18n.get("label.dist.details.key"));
    }

    /**
     * Populate software module table.
     * 
     * @param distributionSet
     */
    public void populateDSMetadata(final DistributionSet distributionSet) {
        removeAllItems();
        List<DistributionSetMetadata> dsMetadtaList = new ArrayList<>();
        for(int i=1;i<=5;i++){
            DistributionSetMetadata dsMetadata = new DistributionSetMetadata();
            dsMetadata.setKey("ReleaseNote AAA" +i);
            dsMetadata.setValue("ReleaseNote AAA sample data"+i);
            dsMetadtaList.add(dsMetadata);
       }
        if (null != distributionSet) {            
           /* final List<DistributionSetMetadata> dsMetadataList = distributionSet.getMetadata();*/
            final List<DistributionSetMetadata> dsMetadataList = dsMetadtaList;
            if (null != dsMetadataList && !dsMetadataList.isEmpty()) {
                dsMetadataList.forEach(dsMetadata -> setDSMetadataProperties(dsMetadata));
            }
         }

    }
    
    private void setDSMetadataProperties(final DistributionSetMetadata dsMetadata){
        final Item item = getContainerDataSource().addItem(dsMetadata.getKey());
        item.getItemProperty(METADATA_KEY).setValue(dsMetadata.getKey());
        if (permissionChecker.hasUpdateDistributionPermission()) {
            item.getItemProperty(VIEW).setValue(HawkbitCommonUtil.getFormatedLabel("View"));
        }
        
    }
    
    protected void addCustomGeneratedColumns() {       
        addGeneratedColumn(VIEW,
                (source, itemId, columnId) -> customMetadataDetailButton((String) itemId));
    }

    private Button customMetadataDetailButton(final String itemId) {
        final Item row1 = getItem(itemId);
        final String metadataKey = (String) row1.getItemProperty(METADATA_KEY).getValue();

        final Button viewIcon = SPUIComponentProvider.getButton(getDetailLinkId(metadataKey), VIEW,
                "View DistributionSet Metadata details", null, false, null, SPUIButtonStyleSmallNoBorder.class);
        viewIcon.setData(metadataKey);
        viewIcon.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");
        //viewIcon.addClickListener(event -> onClickOfDetailButton(event));
        return viewIcon;
    }
    
    private static String getDetailLinkId(final String name) {
        return new StringBuilder(SPUIComponetIdProvider.DS_METADATA_DETAIL_LINK).append('.').append(name)
                .toString();
    }
}
