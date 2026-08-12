package com.example.cliai.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorToolTest {

    private CalculatorTool calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorTool();
    }

    @Test
    void shouldAddTwoNumbers() {
        assertThat(calculator.calculate("2 + 3")).isEqualTo(5.0);
    }

    @Test
    void shouldSubtractTwoNumbers() {
        assertThat(calculator.calculate("10 - 4")).isEqualTo(6.0);
    }

    @Test
    void shouldMultiplyTwoNumbers() {
        assertThat(calculator.calculate("6 * 7")).isEqualTo(42.0);
    }

    @Test
    void shouldDivideTwoNumbers() {
        assertThat(calculator.calculate("15 / 3")).isEqualTo(5.0);
    }

    @Test
    void shouldHandleParentheses() {
        assertThat(calculator.calculate("(2 + 3) * 4")).isEqualTo(20.0);
    }

    @Test
    void shouldHandleNestedParentheses() {
        assertThat(calculator.calculate("((1 + 2) * (3 + 4))")).isEqualTo(21.0);
    }

    @Test
    void shouldHandleComplexExpression() {
        assertThat(calculator.calculate("(15 * 7) + 23")).isEqualTo(128.0);
    }

    @Test
    void shouldReturnDecimalResult() {
        assertThat(calculator.calculate("10.0 / 3")).isCloseTo(3.333, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void shouldHandleSingleNumber() {
        assertThat(calculator.calculate("42")).isEqualTo(42.0);
    }

    @Test
    void shouldThrowOnDivisionByZero() {
        assertThatThrownBy(() -> calculator.calculate("1 / 0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnInvalidExpression() {
        assertThatThrownBy(() -> calculator.calculate("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnEmptyExpression() {
        assertThatThrownBy(() -> calculator.calculate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
