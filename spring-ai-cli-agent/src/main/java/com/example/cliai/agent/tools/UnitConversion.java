package com.example.cliai.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured output target (BLUEPRINT Step 7): {@code BeanOutputConverter<UnitConversion>}
 * derives a JSON Schema from this record and parses the model's JSON answer back into it.
 * Verified shape: {@code {"value":62.14,"unit":"miles"}}.
 */
public record UnitConversion(
        @JsonProperty(required = true) double value,
        @JsonProperty(required = true) String unit) {
}
