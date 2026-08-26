package com.example.cliai.agent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BLUEPRINT Step 7: Structured Output – JSON Schema derived from the UnitConversion record
 * via {@link BeanOutputConverter#getJsonSchema()} and validated parsing of the model answer.
 */
class UnitConversionStructuredOutputTest {

    @Test
    void jsonSchemaShouldDescribeValueAndUnitAsRequired() {
        BeanOutputConverter<UnitConversion> converter = new BeanOutputConverter<>(UnitConversion.class);

        String schema = converter.getJsonSchema();

        assertThat(schema).contains("\"value\"").contains("\"unit\"");
        assertThat(schema).contains("required");
    }

    @Test
    void shouldParseModelJsonIntoUnitConversion() {
        BeanOutputConverter<UnitConversion> converter = new BeanOutputConverter<>(UnitConversion.class);

        UnitConversion conversion = converter.convert("{\"value\": 62.14, \"unit\": \"miles\"}");

        assertThat(conversion.value()).isEqualTo(62.14);
        assertThat(conversion.unit()).isEqualTo("miles");
    }

    @Test
    void schemaShouldMatchBlueprintVerifyShape() {
        // BLUEPRINT verify: `Convert 100 km` returns {"value":62.14,"unit":"miles"} validated
        BeanOutputConverter<UnitConversion> converter = new BeanOutputConverter<>(UnitConversion.class);
        UnitConversion conversion = converter.convert("{\"value\":62.14,\"unit\":\"miles\"}");

        assertThat(new UnitConversion(conversion.value(), conversion.unit()))
            .isEqualTo(new UnitConversion(62.14, "miles"));
    }
}
