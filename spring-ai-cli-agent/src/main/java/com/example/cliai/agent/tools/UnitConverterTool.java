package com.example.cliai.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class UnitConverterTool {

    @Tool(description = "Convert between units. Supports: km/miles, kg/lbs, celsius/fahrenheit, liters/gallons")
    String convert(
            @ToolParam(description = "The value to convert") double value,
            @ToolParam(description = "Source unit (e.g., km, miles, kg, lbs, celsius, fahrenheit)") String from,
            @ToolParam(description = "Target unit (e.g., km, miles, kg, lbs, celsius, fahrenheit)") String to) {

        return switch (from.toLowerCase() + "->" + to.toLowerCase()) {
            case "km->miles" -> value * 0.621371 + " miles";
            case "miles->km" -> value * 1.60934 + " km";
            case "kg->lbs" -> value * 2.20462 + " lbs";
            case "lbs->kg" -> value / 2.20462 + " kg";
            case "celsius->fahrenheit" -> (value * 9/5 + 32) + " °F";
            case "fahrenheit->celsius" -> (value - 32) * 5/9 + " °C";
            case "liters->gallons" -> value * 0.264172 + " gallons";
            case "gallons->liters" -> value * 3.78541 + " liters";
            default -> "Unsupported conversion: " + from + " to " + to;
        };
    }
}