package com.socialnetwork.mc_account.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class JacksonConfig {

    @Bean
    public Module localDateModule() {
        SimpleModule module = new SimpleModule();

        module.addDeserializer(
                LocalDate.class,
                new LocalDateDeserializer()
        );

        return module;
    }
}
