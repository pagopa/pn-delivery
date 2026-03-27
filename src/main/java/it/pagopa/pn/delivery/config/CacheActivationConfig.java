package it.pagopa.pn.delivery.config;


import com.github.benmanes.caffeine.cache.Caffeine;
import it.pagopa.pn.commons.configs.cache.PnCacheConfiguration;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class CacheActivationConfig extends PnCacheConfiguration {
    @Primary
    @Bean(name = {"infoPaCacheManager"})
    public CacheManager infoPaCacheManager(PnDeliveryConfigs cfg) {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        com.github.benmanes.caffeine.cache.Cache<Object, Object> infoPaConfig =
                Caffeine.newBuilder()
                        .expireAfterWrite(cfg.getInfoPaCacheDuration())
                        .maximumSize(10000)
                        .build();

        manager.registerCustomCache("infoPa", infoPaConfig);
        return manager;
    }
}
