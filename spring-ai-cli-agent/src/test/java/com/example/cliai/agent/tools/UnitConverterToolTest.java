package com.example.cliai.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitConverterToolTest {

    private UnitConverterTool converter;

    @BeforeEach
    void setUp() {
        converter = new UnitConverterTool();
    }

    @Test
    void shouldConvertKmToMiles() {
        String result = converter.convert(100, "km", "miles");
        assertThat(result).contains("62.1371").contains("miles");
    }

    @Test
    void shouldConvertMilesToKm() {
        String result = converter.convert(62.1371, "miles", "km");
        assertThat(result).contains("km");
    }

    @Test
    void shouldConvertKgToLbs() {
        String result = converter.convert(10, "kg", "lbs");
        assertThat(result).contains("22.0462").contains("lbs");
    }

    @Test
    void shouldConvertLbsToKg() {
        String result = converter.convert(22.0462, "lbs", "kg");
        assertThat(result).contains("kg");
    }

    @Test
    void shouldConvertCelsiusToFahrenheit() {
        String result = converter.convert(100, "celsius", "fahrenheit");
        assertThat(result).contains("212.0").contains("°F");
    }

    @Test
    void shouldConvertFahrenheitToCelsius() {
        String result = converter.convert(212, "fahrenheit", "celsius");
        assertThat(result).contains("100.0").contains("°C");
    }

    @Test
    void shouldConvertLitersToGallons() {
        String result = converter.convert(10, "liters", "gallons");
        assertThat(result).contains("2.64172").contains("gallons");
    }

    @Test
    void shouldConvertGallonsToLiters() {
        String result = converter.convert(10, "gallons", "liters");
        assertThat(result).contains("37.8541").contains("liters");
    }

    @Test
    void shouldHandleZeroValue() {
        String result = converter.convert(0, "km", "miles");
        assertThat(result).contains("0.0").contains("miles");
    }

    @Test
    void shouldHandleNegativeValue() {
        String result = converter.convert(-40, "celsius", "fahrenheit");
        assertThat(result).contains("-40.0").contains("°F");
    }

    @Test
    void shouldReturnUnsupportedForUnknownConversion() {
        String result = converter.convert(10, "km", "kg");
        assertThat(result).contains("Unsupported conversion");
    }

    @Test
    void shouldHandleCaseInsensitiveUnits() {
        String result = converter.convert(100, "KM", "MILES");
        assertThat(result).contains("62.1371").contains("miles");
    }
}
