/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * This resource bundles using specified basenames, to resource loading. This
 * MessageSource implementation supports more than 1 properties file with the
 * same name. All properties files will be merged.
 */
public class DistributedResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {
    // Exception squid:S2387 - Follows our upper case convention
    @SuppressWarnings({ "squid:S2387" })
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedResourceBundleMessageSource.class);
    private static final String PROPERTIES_SUFFIX = ".properties";
    private ResourceLoader resourceLoader;

    /**
     * Constructor to set Defaults. Default base name is classpath*:/messages.
     * So all messages_ will be found.
     */
    public DistributedResourceBundleMessageSource() {
        setBasename("classpath*:/messages");
        setDefaultEncoding("UTF-8");
        setUseCodeAsDefaultMessage(true);
    }

    @Override
    protected PropertiesHolder refreshProperties(final String filename, final PropertiesHolder propHolder) {
        final Properties properties = new Properties();
        long lastModified = -1;
        if (!(resourceLoader instanceof ResourcePatternResolver)) {
            LOGGER.warn(
                    "Resource Loader {} doensn't support getting multiple resources. Default properties mechanism will used",
                    resourceLoader.getClass().getName());
            return super.refreshProperties(filename, propHolder);
        }

        try {
            final Resource[] resources = ((ResourcePatternResolver) resourceLoader)
                    .getResources(filename + PROPERTIES_SUFFIX);
            for (final Resource resource : resources) {
                final String sourcePath = resource.getURI().toString().replace(PROPERTIES_SUFFIX, "");
                final PropertiesHolder holder = super.refreshProperties(sourcePath, propHolder);
                properties.putAll(holder.getProperties());
                if (lastModified < resource.lastModified()) {
                    lastModified = resource.lastModified();
                }
            }
        } catch (final IOException ignored) {
            LOGGER.warn("Resource with filname " + filename + " couldn't load", ignored);
        }
        return new PropertiesHolder(properties, lastModified);
    }

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        super.setResourceLoader(resourceLoader);
    }

}
