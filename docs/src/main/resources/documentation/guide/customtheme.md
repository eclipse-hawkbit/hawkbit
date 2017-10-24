---
layout: documentation
title: Clustering
---

{% include base.html %}

# Custom Theme

This guide provides details about using and creating themes that control the visual look of Eclipse _hawkBit_ Management UI. Theme customization is done using Sass, which is an extension of CSS (Cascading Style Sheets).

The mechanism described below is the rather simple case by customizing the theme by means of configuring a set of variables as defined by the _hawkBit_ default theme. A full customization be means of copying the _hawkBit_ theme and customize it completely is not described here but of course possible.

# Example App

An example application with customized theme can be found [here](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-custom-theme-example).

# Overview
Vaadin separates the appearance of the user interface from its logic using themes. Themes can include Sass or CSS style sheets, custom HTML layouts, and any necessary graphics.

Theme resources can also be accessed from application code as ThemeResource objects. Custom themes are placed under the src/main/resources/VAADIN/themes/ folder of the application.

This location is fixed -- the VAADIN folder contains static resources that are served by the Vaadin servlet. The servlet augments the files stored in the folder by resources found from corresponding VAADIN folders contained in JARs in the class path.The base theme and all the corresponding custom themes are placed under the above mentioned folder.

If a new custom theme has to be created, it should always be placed under the src/main/resources/VAADIN/themes/ folder.

Every custom theme should always refer the base theme and customization can be done by the use of variables mentioned in the XXXXvariable.scss. For details of the creation of the custom theme please refer the next section.

# Procedure to create a theme
- Create a new folder **"XXXtheme"** (significant to the new theme) under src/main/resources/VAADIN/themes/ folder.
- Create a folder named as **"customstyles"**.
- Create a **XXXvariables.scss** file under **customstyles** folder, putting all the variables inside the same file. For more details about the variables we recommend to take a look at the [hawkBit defaults](https://github.com/eclipse/hawkbit/blob/master/hawkbit-ui/src/main/resources/VAADIN/themes/hawkbit/customstyles/hawkbitvariables.scss).
- Create **styles.scss** file under the **"XXXtheme"** folder.
- Any images should be placed under the sub folder **"images"** folder.
- Within the **_styles.scss_** file, import **XXXvariables.scss** and the base theme (hawkbit theme as mentioned in previous chapter **Overview** . Please find below the syntax:

  ```
  @import "../hawkbit/customstyles/hawkbitvariables";
  @import "customstyles/examplevariables";
  @import "../hawkbit/hawkbittheme";
  @import "addons";

  .exampletheme {
    @include addons;
    @include hawkbittheme;
  }
  ```

- Finally the structure should be as in the [example app](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-custom-theme-example/src/main/resources/VAADIN).

# Procedure to add a custom footer
- Any footer can be added by creating  "footer.html" in **src/main/resources --> VAADIN -- themes --> {XXXtheme} --> layouts** folder. An example can be found [here](https://github.com/eclipse/hawkbit/blob/master/hawkbit-ui/src/main/resources/VAADIN/themes/hawkbit/layouts/footer.html).