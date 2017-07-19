package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.token.TokenType.FALSE;
import static co.kenrg.mega.frontend.token.TokenType.FLOAT;
import static co.kenrg.mega.frontend.token.TokenType.IDENT;
import static co.kenrg.mega.frontend.token.TokenType.INT;
import static co.kenrg.mega.frontend.token.TokenType.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
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

    private void assertIdentifier(Expression expr, String identName) {
        assertEquals(expr, new Identifier(new Token(IDENT, identName), identName));
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
            new TestCase("false != 1 * 3 > 1", "(false != ((1 * 3) > 1))"),

            new TestCase("1 + (2 + 3) + 4", "((1 + (2 + 3)) + 4)"),
            new TestCase("(1 + 2) * 3", "((1 + 2) * 3)"),
            new TestCase("-(5 + 5)", "(-(5 + 5))"),
            new TestCase("!(true == true)", "(!(true == true))")
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

    @Test
    public void testIfExpression() {
        String input = "if x < y { x }";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof IfExpression);
        IfExpression ifExpression = (IfExpression) statement.expression;

        InfixExpression condition = (InfixExpression) ifExpression.condition;
        assertEquals("<", condition.operator);
        assertEquals(condition.left, new Identifier(new Token(IDENT, "x"), "x"));
        assertEquals(condition.right, new Identifier(new Token(IDENT, "y"), "y"));

        BlockExpression thenBlock = (BlockExpression) ifExpression.thenExpr;
        Identifier ident = (Identifier) ((ExpressionStatement) thenBlock.statements.get(0)).expression;
        assertEquals(ident, new Identifier(new Token(IDENT, "x"), "x"));

        assertNull(ifExpression.elseExpr);
    }

    @Test
    public void testIfElseExpression() {
        String input = "if x < y { x } else { y }";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof IfExpression);
        IfExpression ifExpression = (IfExpression) statement.expression;

        InfixExpression condition = (InfixExpression) ifExpression.condition;
        assertEquals("<", condition.operator);
        assertEquals(condition.left, new Identifier(new Token(IDENT, "x"), "x"));
        assertEquals(condition.right, new Identifier(new Token(IDENT, "y"), "y"));

        BlockExpression thenBlock = (BlockExpression) ifExpression.thenExpr;
        Identifier thenExpr = (Identifier) ((ExpressionStatement) thenBlock.statements.get(0)).expression;
        assertEquals(thenExpr, new Identifier(new Token(IDENT, "x"), "x"));

        BlockExpression elseBlock = (BlockExpression) ifExpression.elseExpr;
        Identifier elseExpr = (Identifier) ((ExpressionStatement) elseBlock.statements.get(0)).expression;
        assertEquals(elseExpr, new Identifier(new Token(IDENT, "y"), "y"));
    }

    @Test
    public void testIfExpression_nestedIfElse() {
        String input = "" +
            "if x < y { \n" +
            "  if x > 0 {\n" +
            "    0" +
            "  } else {\n" +
            "    y\n" +
            "  }\n" +
            "}";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof IfExpression);
        IfExpression ifExpression = (IfExpression) statement.expression;

        InfixExpression condition1 = (InfixExpression) ifExpression.condition;
        assertEquals("<", condition1.operator);
        assertEquals(condition1.left, new Identifier(new Token(IDENT, "x"), "x"));
        assertEquals(condition1.right, new Identifier(new Token(IDENT, "y"), "y"));

        BlockExpression thenBlock1 = (BlockExpression) ifExpression.thenExpr;
        IfExpression nestedIfExpr = (IfExpression) ((ExpressionStatement) thenBlock1.statements.get(0)).expression;
        assertNull(ifExpression.elseExpr);

        InfixExpression condition2 = (InfixExpression) nestedIfExpr.condition;
        assertEquals(">", condition2.operator);
        assertEquals(condition2.left, new Identifier(new Token(IDENT, "x"), "x"));
        assertLiteralExpression(condition2.right, 0);

        BlockExpression thenBlock2 = (BlockExpression) nestedIfExpr.thenExpr;
        IntegerLiteral thenExpr = (IntegerLiteral) ((ExpressionStatement) thenBlock2.statements.get(0)).expression;
        assertLiteralExpression(thenExpr, 0);

        BlockExpression elseBlock = (BlockExpression) nestedIfExpr.elseExpr;
        Identifier elseExpr = (Identifier) ((ExpressionStatement) elseBlock.statements.get(0)).expression;
        assertEquals(elseExpr, new Identifier(new Token(IDENT, "y"), "y"));
    }

    @TestFactory
    public List<DynamicTest> testArrowFunction() {
        class TestCase {
            public final String input;
            public final List<String> params;

            public TestCase(String input, List<String> params) {
                this.input = input;
                this.params = params;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("(a, b) => { 24 }", Lists.newArrayList("a", "b")),
            new TestCase("(a) => { 24 }", Lists.newArrayList("a")),
            new TestCase("() => { 24 }", Lists.newArrayList()),
            new TestCase("() => 24", Lists.newArrayList()),
            new TestCase("a => { 24 }", Lists.newArrayList("a")),
            new TestCase("a => 24", Lists.newArrayList("a"))
        );

        return testCases.stream()
            .map(testCase -> {

                String name = String.format("Correctly parses arrow function '%s'", testCase.input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(testCase.input);
                    assertTrue(statement.expression instanceof ArrowFunctionExpression);
                    ArrowFunctionExpression expr = (ArrowFunctionExpression) statement.expression;

                    for (int i = 0; i < testCase.params.size(); i++) {
                        assertIdentifier(expr.parameters.get(i), testCase.params.get(i));
                    }

                    Expression body = expr.body;
                    if (body instanceof BlockExpression) {
                        BlockExpression block = (BlockExpression) body;

                        assertEquals(1, block.statements.size());
                        assertLiteralExpression(((ExpressionStatement) block.statements.get(0)).expression, 24);
                    } else {
                        assertLiteralExpression(body, 24);
                    }
                });
            })
            .collect(toList());

    }

    @Test
    public void testArrowFunction_errors() {
        String input = "1 => { 24 }";
        Parser p = new Parser(new Lexer(input));
        p.parseModule();
        assertTrue(p.errors.size() != 0);
    }
}