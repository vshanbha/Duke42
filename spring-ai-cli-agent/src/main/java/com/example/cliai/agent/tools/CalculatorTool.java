package com.example.cliai.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CalculatorTool {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Tool(description = "Evaluate a mathematical expression. Supports +, -, *, /, parentheses. Example: (2 + 3) * 4")
    double calculate(
            @ToolParam(description = "The math expression to evaluate") String expression) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("pi", Math.PI);
            var result = parser.parseExpression(expression).getValue(context);
            return result instanceof Number n ? n.doubleValue() : Double.parseDouble(result.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot evaluate: " + expression, e);
        }
    }
}