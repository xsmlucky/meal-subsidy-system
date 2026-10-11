package com.mealsubsidy.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATE = "yyyy-MM-dd";
    private static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.simpleDateFormat(DATE_TIME);
            builder.serializers(
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE)),
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME))
            );
        };
    }
}
