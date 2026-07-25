package MineOpsBackend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // This custom ObjectMapper bean bypasses Spring Boot's Jackson autoconfiguration
            // entirely, so Jackson's own default (true) applies unless disabled here explicitly.
            // The frontend intentionally sends a few extra identity fields (actorEmail, actorName,
            // etc.) on some requests that the backend ignores in favor of deriving identity from
            // the authenticated JWT principal — those extra fields must not cause a 400.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
