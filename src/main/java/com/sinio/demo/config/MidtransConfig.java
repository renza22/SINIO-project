package com.sinio.demo.config;

import com.midtrans.Config;
import com.midtrans.ConfigFactory;
import com.midtrans.service.MidtransCoreApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Midtrans setup.
 * The SDK still exposes deprecated Config, so we suppress the warning until Midtrans publishes a non-deprecated alternative.
 */
@Configuration
@SuppressWarnings("deprecation")
public class MidtransConfig {

    private static final Logger log = LoggerFactory.getLogger(MidtransConfig.class);

    @Value("${midtrans.server.key}")
    private String serverKey;

    @Value("${midtrans.client.key}")
    private String clientKey;

    @Value("${midtrans.is.production}")
    private boolean isProduction;

    private Config buildConfig() {
        // Config is marked deprecated in SDK 3.2.1, but required for Core/Snap API wiring.
        Config cfg = Config.builder()
            .setServerKey(serverKey)
            .setClientKey(clientKey)
            .setIsProduction(isProduction)
            .build();
        log.info("Midtrans config loaded (env={}): serverKey=****{} clientKey=****{}", isProduction ? "PROD" : "SANDBOX",
            last4(serverKey), last4(clientKey));
        return cfg;
    }

    private String last4(String value) {
        if (value == null || value.length() < 4) {
            return "NULL";
        }
        return value.substring(value.length() - 4);
    }

    @Bean
    public MidtransCoreApi midtransCoreApi() {
        return new ConfigFactory(buildConfig()).getCoreApi();
    }

    @Bean
    public Config midtransSdkConfig() {
        return buildConfig();
    }
}
