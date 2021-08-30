package org.apereo.cas.config;

import org.apereo.cas.authentication.CasSSLContext;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.cosmosdb.CosmosDbObjectFactory;
import org.apereo.cas.services.CosmosDbServiceRegistry;
import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryListener;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

/**
 * This is {@link CosmosDbServiceRegistryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Configuration(value = "cosmosDbServiceRegistryConfiguration", proxyBeanMethods = true)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CosmosDbServiceRegistryConfiguration {
    @Autowired
    @Qualifier("casSslContext")
    private ObjectProvider<CasSSLContext> casSslContext;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("serviceRegistryListeners")
    private ObjectProvider<Collection<ServiceRegistryListener>> serviceRegistryListeners;

    @ConditionalOnMissingBean(name = "cosmosDbObjectFactory")
    @Bean
    public CosmosDbObjectFactory cosmosDbObjectFactory() {
        return new CosmosDbObjectFactory(casProperties.getServiceRegistry().getCosmosDb());
    }

    @Bean
    @RefreshScope
    public ServiceRegistry cosmosDbServiceRegistry() {
        val cosmosDb = casProperties.getServiceRegistry().getCosmosDb();
        val factory = cosmosDbObjectFactory();
        factory.createDatabaseIfNecessary();
        if (cosmosDb.isDropContainer()) {
            factory.deleteContainer(cosmosDb.getContainer());
            factory.createContainer(cosmosDb.getContainer(), CosmosDbServiceRegistry.PARTITION_KEY);
        }
        return new CosmosDbServiceRegistry(factory, cosmosDb.getContainer(),
            applicationContext, serviceRegistryListeners.getObject());
    }

    @Bean
    @ConditionalOnMissingBean(name = "cosmosDbServiceRegistryExecutionPlanConfigurer")
    @RefreshScope
    @Autowired
    public ServiceRegistryExecutionPlanConfigurer cosmosDbServiceRegistryExecutionPlanConfigurer(
        @Qualifier("cosmosDbServiceRegistry") final ServiceRegistry cosmosDbServiceRegistry) {
        return plan -> plan.registerServiceRegistry(cosmosDbServiceRegistry);
    }

}
