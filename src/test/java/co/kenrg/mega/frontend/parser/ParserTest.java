package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.token.TokenType.FALSE;
import static co.kenrg.mega.frontend.token.TokenType.FLOAT;
import static co.kenrg.mega.frontend.token.TokenType.IDENT;
import static co.kenrg.mega.frontend.token.TokenType.INT;
import static co.kenrg.mega.frontend.token.TokenType.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Token;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ParserTest {

    @TestFactory
    public List<DynamicTest> testLetStatements() {
        List<Pair<String, String>> tests = Lists.newArrayList(
            Pair.of("let x = 4", "x"),
            Pair.of("let y = 10", "y"),
            Pair.of("let foobar = 12.45", "foobar")
        );

        return tests.stream()
            .map(testCase -> {
                    String letStmt = testCase.getLeft();
                    String ident = testCase.getRight();

                    String testName = String.format("The let-stmt `%s` should have ident `%s`", letStmt, ident);
                    return dynamicTest(testName, () -> {
                        Lexer lexer = new Lexer(letStmt);
                        Parser parser = new Parser(lexer);
                        Module module = parser.parseModule();

                        assertEquals(1, module.statements.size());

                        Statement statement = module.statements.get(0);
                        assertTrue(statement instanceof LetStatement);

                        assertEquals(ident, ((LetStatement) statement).name.value);
                    });
                }
            )
            .collect(toList());
    }

    @Test
    public void testLetStatement_syntaxErrors() {
        String input = "let x 4";
        Parser parser = new Parser(new Lexer(input));
        parser.parseModule();

        assertEquals(1, parser.errors.size());
    }

    private ExpressionStatement parseExpressionStatement(String input) {
        Parser p = new Parser(new Lexer(input));
        Module module = p.parseModule();
        assertEquals(0, p.errors.size(), "There should be 0 parser errors");

        assertEquals(1, module.statements.size(), "There should be 1 statement parsed");
        Statement statement = module.statements.get(0);
        assertTrue(statement instanceof ExpressionStatement);
        return (ExpressionStatement) statement;
    }

    @Test
    public void testIdentifierExpression() {
        String input = "foobar;";

        ExpressionStatement statement = parseExpressionStatement(input);
        Expression identExpr = statement.expression;

        assertEquals(identExpr, new Identifier(new Token(IDENT, "foobar"), "foobar"));
    }

    private void assertLiteralExpression(Expression expr, Object expectedValue) {
        if (expectedValue instanceof Integer) {
            Integer value = (Integer) expectedValue;
            assertEquals(
                new IntegerLiteral(new Token(INT, String.valueOf(value)), value),
                expr
            );
        } else if (expectedValue instanceof Float) {
            Float value = (Float) expectedValue;
            assertEquals(
                new FloatLiteral(new Token(FLOAT, String.valueOf(value)), value),
                expr
            );
        } else if (expectedValue instanceof Boolean) {
            Boolean value = (Boolean) expectedValue;
            Token token = value
                ? new Token(TRUE, "true")
                : new Token(FALSE, "false");
            assertEquals(
                new BooleanLiteral(token, value),
                expr
            );
        } else {
            fail("Cannot assert literal expr equivalence for type: " + expectedValue.getClass().getName());
        }
    }

    @TestFactory
    public List<DynamicTest> testIntegerLiteralExpression() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("5;", 5),
            Pair.of("15;", 15),
            Pair.of("1.;", 1)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                int value = testCase.getRight();

                String name = String.format("'%s' should parse to '%d'", input, value);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertLiteralExpression(statement.expression, value);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testFloatLiteralExpression() {
        List<Pair<String, Float>> testCases = Lists.newArrayList(
            Pair.of("5.0;", 5.0f),
            Pair.of("0.15;", 0.15f),
            Pair.of("15.03;", 15.03f)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                float value = testCase.getRight();

                String name = String.format("'%s' should parse to '%f'", input, value);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertLiteralExpression(statement.expression, value);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testBooleanLiteralExpression() {
        List<Pair<String, Boolean>> testCases = Lists.newArrayList(
            Pair.of("true", true),
            Pair.of("false", false)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                Boolean value = testCase.getRight();

                String name = String.format("'%s' should parse to '%b'", input, value);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertLiteralExpression(statement.expression, value);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testPrefixExpressions() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            Triple.of("!5", "!", 5),
            Triple.of("-15", "-", 15),

            Triple.of("!true", "!", true),
            Triple.of("!false", "!", false)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String operator = testCase.getMiddle();
                Object value = testCase.getRight();

                String name = String.format("'%s' should parse correctly", input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    Expression expr = statement.expression;

                    assertTrue(expr instanceof PrefixExpression);
                    PrefixExpression prefixExpr = (PrefixExpression) expr;

                    assertEquals(operator, prefixExpr.operator);
                    assertLiteralExpression(prefixExpr.expression, value);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testInfixExpressions() {
        class TestCase {
            private final String input;
            private final String operator;
            private final Object left;
            private final Object right;

            private TestCase(String input, String operator, Object left, Object right) {
                this.input = input;
                this.operator = operator;
                this.left = left;
                this.right = right;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("5 + 5", "+", 5, 5),
            new TestCase("5 - 5", "-", 5, 5),
            new TestCase("5 * 5", "*", 5, 5),
            new TestCase("5 / 5", "/", 5, 5),
            new TestCase("5 < 5", "<", 5, 5),
            new TestCase("5 <= 5", "<=", 5, 5),
            new TestCase("5 > 5", ">", 5, 5),
            new TestCase("5 >= 5", ">=", 5, 5),
            new TestCase("5 == 5", "==", 5, 5),
            new TestCase("5 != 5", "!=", 5, 5)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should parse correctly", testCase.input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(testCase.input);
                    Expression expr = statement.expression;

                    assertTrue(expr instanceof InfixExpression);
                    InfixExpression infixExpr = (InfixExpression) expr;

                    assertEquals(testCase.operator, infixExpr.operator);
                    assertLiteralExpression(infixExpr.left, testCase.left);
                    assertLiteralExpression(infixExpr.right, testCase.right);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testOperatorPrecedence() {
        class TestCase {
            private final String input;
            private final String output;

            private TestCase(String input, String output) {
                this.input = input;
                this.output = output;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("-a * b", "((-a) * b)"),
            new TestCase("!-a", "(!(-a))"),
            new TestCase("a+b+c", "((a + b) + c)"),
            new TestCase("a+b-c", "((a + b) - c)"),
            new TestCase("a*b*c", "((a * b) * c)"),
            new TestCase("a*b/c", "((a * b) / c)"),
            new TestCase("a+b*c", "(a + (b * c))"),
            new TestCase("a+b/c", "(a + (b / c))"),
            new TestCase("a+b*c+d/e-f", "(((a + (b * c)) + (d / e)) - f)"),
            new TestCase("5 > 4 == 3 < 4", "((5 > 4) == (3 < 4))"),
            new TestCase("5 < 4 != 3 < 4", "((5 < 4) != (3 < 4))"),
            new TestCase("1+2+3==5+1", "(((1 + 2) + 3) == (5 + 1))"),

            new TestCase("3 < 5 == true", "((3 < 5) == true)"),
            new TestCase("false != 1 * 3 > 1", "(false != ((1 * 3) > 1))")
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should have the correct representation (precendence accounted-for)", testCase.input);
                return dynamicTest(name, () -> {
                    Parser p = new Parser(new Lexer(testCase.input));
                    Module module = p.parseModule();
                    assertEquals(0, p.errors.size(), "There should be 0 parser errors");

                    assertEquals(testCase.output, module.repr(true, 0));
                });
            })
            .collect(toList());
    }
}