/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.eclipse.hawkbit.ui.security.AuthenticatedUser;
import org.eclipse.hawkbit.ui.view.AboutView;
import org.eclipse.hawkbit.ui.view.ConfigView;
import org.eclipse.hawkbit.ui.view.DistributionSetView;
import org.eclipse.hawkbit.ui.view.RolloutView;
import org.eclipse.hawkbit.ui.view.SoftwareModuleView;
import org.eclipse.hawkbit.ui.view.TargetView;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    static final List<Class<? extends Component>> DEFAULT_VIEW_PRIORITY = List.of(TargetView.class, DistributionSetView.class,
            SoftwareModuleView.class, RolloutView.class);
    private final transient AuthenticatedUser authenticatedUser;
    private final AccessAnnotationChecker accessChecker;
    private H2 viewTitle;
    private transient Optional<Class<? extends Component>> defaultView;

    public MainLayout(final AuthenticatedUser authenticatedUser, final AccessAnnotationChecker accessChecker) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        setDrawerOpened(true);
        addHeaderContent();
    }

    @Override
    public void showRouterLayoutContent(final HasElement content) {
        super.showRouterLayoutContent(content);
        viewTitle.setText(
                Optional.ofNullable(getContent())
                        .map(c -> c.getClass().getAnnotation(PageTitle.class))
                        .map(PageTitle::value)
                        .orElse(""));
        if (UI.getCurrent().getActiveViewLocation().getPath().isEmpty()) {
            defaultView.ifPresent(c -> UI.getCurrent().navigate(c));
        }
    }

    private void addHeaderContent() {
        final DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(false, toggle, viewTitle);
    }

    private void addDrawerContent() {
        final H1 appName = new H1("hawkBit UI");
        final HorizontalLayout layout = new HorizontalLayout();
        layout.setPadding(true);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        final Image icon = new Image("images/header_icon.png", "hawkBit icon");
        icon.setMaxHeight(24, Unit.PIXELS);
        icon.setMaxWidth(24, Unit.PIXELS);
        appName.addClassNames(LumoUtility.AlignItems.BASELINE, LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        layout.add(icon, appName);
        final Header header = new Header(layout);

        final Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        final SideNav nav = new SideNav();
        if (accessChecker.hasAccess(TargetView.class)) {
            nav.addItem(new SideNavItem("Targets", TargetView.class, VaadinIcon.FILTER.create()));
        }
        if (accessChecker.hasAccess(RolloutView.class)) {
            nav.addItem(new SideNavItem("Rollouts", RolloutView.class, VaadinIcon.COGS.create()));
        }
        if (accessChecker.hasAccess(DistributionSetView.class)) {
            nav.addItem(new SideNavItem("Distribution Sets", DistributionSetView.class, VaadinIcon.FILE_TREE.create()));
        }
        if (accessChecker.hasAccess(SoftwareModuleView.class)) {
            nav.addItem(new SideNavItem("Software Modules", SoftwareModuleView.class, VaadinIcon.FILE.create()));
        }
        if (accessChecker.hasAccess(ConfigView.class)) {
            nav.addItem(new SideNavItem("Config", ConfigView.class, VaadinIcon.COG.create()));
        }
        if (accessChecker.hasAccess(AboutView.class)) {
            nav.addItem(new SideNavItem("About", AboutView.class, VaadinIcon.INFO_CIRCLE.create()));
        }
        defaultView = DEFAULT_VIEW_PRIORITY.stream().filter(accessChecker::hasAccess).findFirst();
        return nav;
    }

    private Footer createFooter() {
        final Footer layout = new Footer();

        final Optional<String> maybeUser = authenticatedUser.getName();
        if (maybeUser.isPresent()) {
            final String user = maybeUser.get();

            final Avatar avatar = new Avatar(user);

            final MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            final MenuItem userName = userMenu.addItem("");
            final Div div = new Div();
            div.add(avatar);
            div.add(user);
            div.add(new Icon("lumo", "dropdown"));
            div.getElement().getStyle().set("display", "flex");
            div.getElement().getStyle().set("align-items", "center");
            div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
            userName.add(div);
            userName.getSubMenu().addItem("Sign out", e -> authenticatedUser.logout());

            layout.add(userMenu);
        } else {
            final Anchor loginLink = new Anchor("login", "Sign in");
            layout.add(loginLink);
        }

        return layout;
    }
}
