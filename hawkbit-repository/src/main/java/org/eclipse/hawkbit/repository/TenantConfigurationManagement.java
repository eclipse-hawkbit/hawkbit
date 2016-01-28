package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Transactional(readOnly = true)
@Validated
public class TenantConfigurationManagement implements EnvironmentAware {

    private static final TenantConfigurationManagement INSTANCE = new TenantConfigurationManagement();

    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private final ConfigurableConversionService conversionService = new DefaultConversionService();

    private Environment environment;

    public static TenantConfigurationManagement getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten
     * configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     * 
     * @param <T>
     *
     * @param configurationKey
     *            the key of the configuration
     * @param propertyType
     *            the type of the configuration value, e.g. {@code String.class}
     *            , {@code Integer.class}, etc
     * @return the converted configuration value either from the tenant specific
     *         configuration stored or from the fallback default values or
     *         {@code null} in case key has not been configured and not default
     *         value exists
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */
    @Cacheable(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    public <T> TenantConfigurationValue<T> getConfigurationValue(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {

        if (configurationKey.getDataType() != propertyType) {
            throw new IllegalAccessError(String.format("The key %s does not handle values in the type %s.",
                    configurationKey.getKeyName(), propertyType));
        }

        final TenantConfiguration tenantConfiguration = tenantConfigurationRepository
                .findByKey(configurationKey.getKeyName());

        if (tenantConfiguration != null) {
            return TenantConfigurationValue.<T> builder().isGlobal(false).createdBy(tenantConfiguration.getCreatedBy())
                    .createdAt(tenantConfiguration.getCreatedAt())
                    .lastModifiedAt(tenantConfiguration.getLastModifiedAt())
                    .lastModifiedBy(tenantConfiguration.getLastModifiedBy())
                    .value(conversionService.convert(tenantConfiguration.getValue(), propertyType)).build();

        } else if (configurationKey.getDefaultKeyName() != null) {
            final T valueInProperties = environment.getProperty(configurationKey.getDefaultKeyName(), propertyType);

            return TenantConfigurationValue.<T> builder().isGlobal(true).createdBy(null).createdAt(null)
                    .lastModifiedAt(null).lastModifiedBy(null).value(valueInProperties != null ? valueInProperties
                            : conversionService.convert(configurationKey.getDefaultValue(), propertyType))
                    .build();
        }
        return null;
    }

    /**
     * Adds or updates a specific configuration for a specific tenant.
     *
     * @param tenantConf
     *            the tenant configuration object which contains the key and
     *            value of the specific configuration to update
     * @return the added or updated TenantConfiguration
     */
    @CacheEvict(value = "tenantConfiguration", key = "#tenantConf.getKey()")
    @Transactional
    @Modifying
    public void addOrUpdateConfiguration(final TenantConfigurationKey tenantConfkey, final Object value) {

        tenantConfkey.validate(applicationContext, value);

        TenantConfiguration tenantConfiguration = tenantConfigurationRepository.findByKey(tenantConfkey.getKeyName());
        if (tenantConfiguration != null) {
            tenantConfiguration.setValue(value.toString());
        } else {
            tenantConfiguration = new TenantConfiguration(tenantConfkey.getKeyName(), value.toString());
        }
        tenantConfigurationRepository.save(tenantConfiguration);
    }

    /**
     * Deletes a specific configuration for the current tenant.
     *
     * @param configurationKey
     *            the configuration key to be deleted
     */
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    @Transactional
    @Modifying
    public void deleteConfiguration(final TenantConfigurationKey configurationKey) {
        tenantConfigurationRepository.deleteByKey(configurationKey.getKeyName());
    }

    // @Transactional
    // public List<TenantConfiguration> getTenantConfigurations() {
    //
    // return tenantConfigurationRepository.findAll();
    // }

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
}
