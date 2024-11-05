/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit;

import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * This resource bundles using specified basenames, to resource loading. This
 * MessageSource implementation supports more than 1 properties file with the
 * same name. All properties files will be merged.
 */
@Slf4j
public class DistributedResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {

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
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        super.setResourceLoader(resourceLoader);
    }

    @Override
    protected PropertiesHolder refreshProperties(final String filename, final PropertiesHolder propHolder) {
        final Properties properties = new Properties();
        long lastModified = -1;
        if (!(resourceLoader instanceof ResourcePatternResolver)) {
            log.warn(
                    "Resource Loader {} doesn't support getting multiple resources. Default properties mechanism will used",
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
            log.warn("Resource with filename " + filename + " couldn't load", ignored);
        }
        return new PropertiesHolder(properties, lastModified);
    }
}