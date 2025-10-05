package de.unistuttgart.iste.meitrex.media_service.config;

import de.unistuttgart.iste.meitrex.common.profanity_filter.ProfanityFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class ProfanityFilterConfiguration {

    @Bean
    public ProfanityFilter getProfanityFilter() throws IOException {
        Resource deResource = new ClassPathResource("profanity/de.txt");
        Resource enResource = new ClassPathResource("profanity/en.txt");
        return new ProfanityFilter(deResource, enResource);
    }
}
