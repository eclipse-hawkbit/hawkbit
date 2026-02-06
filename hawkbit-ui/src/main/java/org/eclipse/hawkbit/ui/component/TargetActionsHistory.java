/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.ui.component;

import static org.eclipse.hawkbit.ui.view.Constants.STATUS;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.view.TargetView;
import org.eclipse.hawkbit.ui.view.util.Utils;

@Slf4j
public class TargetActionsHistory extends Grid<TargetActionsHistory.ActionStatusEntry> {

    private final transient HawkbitMgmtClient hawkbitClient;
    private transient MgmtTarget target;
    private final TargetView.TargetActionsHistoryLayout.ActionStepsGrid actionStepsGrid;

    public TargetActionsHistory(final HawkbitMgmtClient hawkbitClient, TargetView.TargetActionsHistoryLayout.ActionStepsGrid actionStepsGrid) {
        this.hawkbitClient = hawkbitClient;
        setWidthFull();
        addColumn(new ComponentRenderer<>(ActionStatusEntry::getStatusIcon)).setHeader(STATUS).setAutoWidth(true).setFlexGrow(0);
        addColumn(ActionStatusEntry::getDistributionSetName).setHeader("Distribution Set").setAutoWidth(true);
        addColumn(Utils.localDateTimeRenderer(ActionStatusEntry::getLastModifiedAt))
                .setHeader("Last Modified")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setComparator(ActionStatusEntry::getLastModifiedAt);

        addColumn(new ComponentRenderer<>(ActionStatusEntry::getForceTypeIcon)).setHeader("Type").setAutoWidth(true).setFlexGrow(0);
        addColumn(new ComponentRenderer<>(ActionStatusEntry::getActionsLayout)).setHeader("Actions").setAutoWidth(true).setFlexGrow(0).setFrozenToEnd(true);
        addColumn(new ComponentRenderer<>(ActionStatusEntry::getForceQuitLayout)).setHeader("Force Quit").setAutoWidth(true)
                .setFlexGrow(0).setFrozenToEnd(true);;
        addItemClickListener(e -> actionStepsGrid.setActionId(e.getItem().action.getId()));
        this.actionStepsGrid = actionStepsGrid;
    }

    public void setItem(final MgmtTarget target) {
        this.target = target;
        this.actionStepsGrid.setTarget(target);
    }

    private List<ActionStatusEntry> fetchActions() {
        return hawkbitClient.getTargetRestApi().getActionHistory(target.getControllerId(), null, 0, 30, null)
                .getBody()
                .getContent()
                .stream()
                .map(action -> new ActionStatusEntry(action, () -> setItems(fetchActions())))
                .filter(value -> value.action != null)
                .toList();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        List<ActionStatusEntry> actionStatusEntries = fetchActions();
        setItems(actionStatusEntries);
        actionStatusEntries.stream().findFirst().ifPresentOrElse(e -> {
            // select first action in the list by default
            asSingleSelect().setValue(e);
            actionStepsGrid.setActionId(e.action.getId());
        }, () -> actionStepsGrid.setActionId(null));
    }

    protected class ActionStatusEntry {

        final MgmtAction action;
        final Runnable onUpdate;
        MgmtDistributionSet distributionSet;

        public ActionStatusEntry(final MgmtAction mgmtAction, final Runnable onUpdate) {
            this.action = hawkbitClient.getActionRestApi().getAction(mgmtAction.getId()).getBody();
            this.onUpdate = onUpdate;
            if (action == null) {
                log.error("Unable to fetch the action with id : {}", mgmtAction.getId());
                return;
            }
            this.action.getLink("distributionset").ifPresent(link -> {
                try {
                    Long dsId = Long.parseLong(link.getHref().substring(link.getHref().lastIndexOf("/") + 1));
                    this.distributionSet = hawkbitClient.getDistributionSetRestApi().getDistributionSet(dsId).getBody();
                } catch (NumberFormatException e) {
                    log.error("Error parsing distribution set ID", e);
                }
            });
        }

        private boolean isActive() {
            return action.isActive();
        }

        private boolean isCancelingOrCanceled() {
            return action.getType().equals(MgmtAction.ACTION_CANCEL);
        }

        public Component getStatusIcon() {
            final HorizontalLayout layout = new HorizontalLayout();
            final Icon icon;
            if (isActive()) {
                if (isCancelingOrCanceled()) {
                    icon = Utils.tooltip(VaadinIcon.ADJUST.create(), "Pending Cancellation");
                    icon.setColor("red");
                } else {
                    icon = Utils.tooltip(VaadinIcon.ADJUST.create(), "Pending Update");
                    icon.setColor("orange");
                }// todo getDetailStatus should return an enum from src/main/java/org/eclipse/hawkbit/repository/model/Action.java
            } else if (action.getType().equals(MgmtAction.ACTION_UPDATE) && action.getStatus().equals("finished")) {
                icon = Utils.tooltip(VaadinIcon.CHECK_CIRCLE.create(), "Updated");
                icon.setColor("green");
            } else {
                icon = Utils.tooltip(VaadinIcon.CLOSE_CIRCLE.create(), "Canceled");
                icon.setColor("red");
            }

            icon.addClassNames(LumoUtility.IconSize.SMALL);
            layout.add(icon);
            layout.setWidth(50, Unit.PIXELS);
            layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            return layout;
        }

        public String getDistributionSetName() {
            return Optional.ofNullable(distributionSet).map(d -> d.getName() + ":" + d.getVersion()).orElse(
                    "Distribution Set not found");
        }

        public Long getLastModifiedAt() {
            return action.getLastModifiedAt();
        }

        public Icon getForceTypeIcon() {
            Icon icon = switch (action.getForceType()) {
                case FORCED -> VaadinIcon.BOLT.create();
                case TIMEFORCED -> VaadinIcon.USER_CLOCK.create();
                case SOFT -> VaadinIcon.USER_CHECK.create();
                case DOWNLOAD_ONLY -> VaadinIcon.DOWNLOAD.create();
            };
            return Utils.tooltip(icon, action.getForceType().getName());
        }

        public HorizontalLayout getActionsLayout() {
            final HorizontalLayout actionsLayout = new HorizontalLayout();
            actionsLayout.setSpacing(true);

            final Button cancelButton = Utils.tooltip(new Button(VaadinIcon.CLOSE.create()), "Cancel Action");
            if (isActive() && !isCancelingOrCanceled()) {
                cancelButton.addClickListener(e -> {
                    String message = "Are you sure you want to cancel the action ?";
                    promptForConfirmAction(
                            message, onUpdate,
                            () -> hawkbitClient.getTargetRestApi().cancelAction(target.getControllerId(), action.getId(), false))
                            .open();
                });
            } else {
                cancelButton.setEnabled(false);
            }

            final Button forceButton = Utils.tooltip(new Button(VaadinIcon.BOLT.create()), "Force Action");
            if (isActive() && !isCancelingOrCanceled() && action.getForceType() != MgmtActionType.FORCED) {
                forceButton.addClickListener(e -> {
                    String message = "Are you sure you want to force the action ?";
                    promptForConfirmAction(
                            message, onUpdate, () -> {
                                MgmtActionRequestBodyPut setForced = new MgmtActionRequestBodyPut();
                                setForced.setForceType(MgmtActionType.FORCED);
                                hawkbitClient.getTargetRestApi()
                                        .updateAction(target.getControllerId(), action.getId(), setForced);
                            }
                    ).open();
                });
            } else {
                forceButton.setEnabled(false);
            }

            actionsLayout.add(cancelButton, forceButton);
            return actionsLayout;
        }

        public HorizontalLayout getForceQuitLayout() {
            final HorizontalLayout forceQuitLayout = new HorizontalLayout();
            forceQuitLayout.setSpacing(true);
            forceQuitLayout.setPadding(true);
            forceQuitLayout.setWidthFull();
            forceQuitLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            final Button forceQuitButton = Utils.tooltip(new Button(VaadinIcon.CLOSE.create()), "Force Cancel");
            forceQuitButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);

            if (isActive() && isCancelingOrCanceled()) {
                forceQuitButton.addClickListener(e -> {
                    String message = "Are you sure you want to force cancel the action ?";
                    promptForConfirmAction(
                            message, onUpdate,
                            () -> hawkbitClient.getTargetRestApi().cancelAction(target.getControllerId(), action.getId(), true)).open();
                });
            } else {
                forceQuitButton.setEnabled(false);
            }

            forceQuitLayout.add(forceQuitButton);
            return forceQuitLayout;
        }

        private static ConfirmDialog promptForConfirmAction(String message, Runnable refreshActions, Runnable actionConsumer) {
            final ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Confirm Action");
            dialog.setText(message);

            dialog.setCancelable(true);
            dialog.addCancelListener(event -> dialog.close());

            dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
            dialog.setConfirmText("Confirm");
            dialog.addConfirmListener(event -> {
                actionConsumer.run();
                refreshActions.run();
                dialog.close();
            });
            return dialog;
        }
    }
}
