package com.example.cliai.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class CalculatorTool {

    @Tool(description = "Evaluate a mathematical expression. Supports +, -, *, /, parentheses. Example: (2 + 3) * 4")
    double calculate(
            @ToolParam(description = "The math expression to evaluate") String expression) {
        try {
            var engine = new javax.script.ScriptEngineManager().getEngineByName("js");
            Object result = engine.eval(expression);
            return result instanceof Number n ? n.doubleValue() : Double.parseDouble(result.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot evaluate: " + expression, e);
        }
    }
}