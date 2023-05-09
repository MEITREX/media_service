package de.unistuttgart.iste.gits.media_service.config;

import graphql.scalars.ExtendedScalars;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQlScalarsAndValidationConfiguration {
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {

        ValidationRules validationRules = ValidationRules.newValidationRules()
                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
                .build();

        ValidationSchemaWiring schemaWiring = new ValidationSchemaWiring(validationRules);

        return wiringBuilder -> wiringBuilder
                .directiveWiring(schemaWiring)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Time)
                .scalar(ExtendedScalars.LocalTime)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.Url);
    }
}
