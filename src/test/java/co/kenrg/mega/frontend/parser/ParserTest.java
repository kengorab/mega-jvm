package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.token.TokenType.FALSE;
import static co.kenrg.mega.frontend.token.TokenType.FLOAT;
import static co.kenrg.mega.frontend.token.TokenType.IDENT;
import static co.kenrg.mega.frontend.token.TokenType.INT;
import static co.kenrg.mega.frontend.token.TokenType.PLUS;
import static co.kenrg.mega.frontend.token.TokenType.STRING;
import static co.kenrg.mega.frontend.token.TokenType.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.IndexExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.ObjectLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.StringInterpolationExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Token;
import com.google.common.collect.ImmutableMap;
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

    @Test
    public void testFunctionDeclarationStatement() {
        String input = "func add(a, b) { a + b }";
        Parser parser = new Parser(new Lexer(input));
        Module module = parser.parseModule();
        assertEquals(0, parser.errors.size());
        FunctionDeclarationStatement statement = (FunctionDeclarationStatement) module.statements.get(0);

        assertEquals("add", statement.name.value);
        assertEquals(
            Lists.newArrayList("a", "b"),
            statement.parameters.stream()
                .map(param -> param.value)
                .collect(toList())
        );

        BlockExpression body = statement.body;
        assertEquals(1, body.statements.size());

        assertEquals(
            Lists.newArrayList(
                new ExpressionStatement(
                    new Token(IDENT, "a"),
                    new InfixExpression(
                        new Token(PLUS, "+"),
                        "+",
                        new Identifier(new Token(IDENT, "a"), "a"),
                        new Identifier(new Token(IDENT, "b"), "b")
                    )
                )
            ),
            body.statements
        );
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
        } else if (expectedValue instanceof String) {
            String value = (String) expectedValue;
            assertEquals(
                new StringLiteral(new Token(STRING, value), value),
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
    public List<DynamicTest> testStringLiteralExpression() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("\"hello\"", "hello")
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String value = testCase.getRight();

                String name = String.format("'%s' should parse to '%s'", input, value);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertLiteralExpression(statement.expression, value);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testStringInterpolationExpression() {
        List<Pair<String, Map<String, Expression>>> testCases = Lists.newArrayList(
            Pair.of("\"$a bc\"", ImmutableMap.of(
                "$a", new Identifier(new Token(IDENT, "a"), "a")
            )),
            Pair.of("\"$a $bc\"", ImmutableMap.of(
                "$a", new Identifier(new Token(IDENT, "a"), "a"),
                "$bc", new Identifier(new Token(IDENT, "bc"), "bc")
            )),
            Pair.of("\"$a ${bc}\"", ImmutableMap.of(
                "$a", new Identifier(new Token(IDENT, "a"), "a"),
                "${bc}", new Identifier(new Token(IDENT, "bc"), "bc")
            )),
            Pair.of("\"${a} ${bc}\"", ImmutableMap.of(
                "${a}", new Identifier(new Token(IDENT, "a"), "a"),
                "${bc}", new Identifier(new Token(IDENT, "bc"), "bc")
            )),
            Pair.of("\"1 + 1 = ${1 + 1}\"", ImmutableMap.of(
                "${1 + 1}", new InfixExpression(
                    new Token(PLUS, "+"),
                    "+",
                    new IntegerLiteral(new Token(INT, "1"), 1),
                    new IntegerLiteral(new Token(INT, "1"), 1)
                )
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                Map<String, Expression> interpolatedExprs = testCase.getRight();

                String name = String.format("'%s' should contain proper interpolated expressions", input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    StringInterpolationExpression expr = (StringInterpolationExpression) statement.expression;
                    assertEquals(interpolatedExprs, expr.interpolatedExpressions);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testArrayLiteralExpression_elementsAreLiterals() {
        List<Pair<String, List<Object>>> testCases = Lists.newArrayList(
            Pair.of("[]", Lists.newArrayList()),
            Pair.of("[\"hello\", 1]", Lists.newArrayList("hello", 1)),
            Pair.of("[\"hello\", 1.2, true]", Lists.newArrayList("hello", 1.2f, true))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                List<Object> elements = testCase.getRight();

                String name = String.format("'%s' should parse to an array of elements", input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    ArrayLiteral expr = (ArrayLiteral) statement.expression;
                    assertEquals(elements.size(), expr.elements.size());

                    for (int i = 0; i < elements.size(); i++) {
                        Object elem = elements.get(i);
                        Expression expression = expr.elements.get(i);
                        assertLiteralExpression(expression, elem);
                    }
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testObjectLiteralExpression() {
        List<Pair<String, List<Pair<String, String>>>> testCases = Lists.newArrayList(
            Pair.of("{}", Lists.newArrayList()),
            Pair.of("{prop1:1}", Lists.newArrayList(
                Pair.of("prop1", "1")
            )),
            Pair.of("{prop1:1, prop2:\"two\"}", Lists.newArrayList(
                Pair.of("prop1", "1"),
                Pair.of("prop2", "\"two\"")
            )),
            Pair.of("{prop1:1 + 1, prop2:\"two\" + \"two\"}", Lists.newArrayList(
                Pair.of("prop1", "(1 + 1)"),
                Pair.of("prop2", "(\"two\" + \"two\")")
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                List<Pair<String, String>> elements = testCase.getRight();

                String name = String.format("'%s' should parse to an object literal", input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    ObjectLiteral expr = (ObjectLiteral) statement.expression;
                    assertEquals(elements.size(), expr.pairs.size());

                    // Sorting expected and actual by alphabetical order helps ensure non-flaky tests, since objects'
                    // key/value pairs aren't always ordered the same.
                    List<Entry<Identifier, Expression>> elems = Lists.newArrayList(expr.pairs.entrySet());
                    elems.sort(Comparator.comparing(e -> e.getKey().value));

                    for (int i = 0; i < elems.size(); i++) {
                        Pair<String, String> elem = elements.get(i);
                        Entry<Identifier, Expression> pair = elems.get(i);

                        assertIdentifier(pair.getKey(), elem.getKey());
                        assertEquals(elem.getValue(), pair.getValue().repr(true, 0));
                    }
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
            new TestCase("!(true == true)", "(!(true == true))"),

            new TestCase("add(a, 1 + add(b, 2))", "add(a, (1 + add(b, 2)))"),
            new TestCase("a * [1, 2, 3][b * c] * d", "((a * ([1, 2, 3][(b * c)])) * d)")
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

                    assertEquals(
                        testCase.params,
                        expr.parameters.stream()
                            .map(param -> param.value)
                            .collect(toList())
                    );

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
    public void testArrowFunction_returnsAnotherArrowFunction() {
        String input = "a => b => a + b";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof ArrowFunctionExpression);
        ArrowFunctionExpression expr = (ArrowFunctionExpression) statement.expression;

        assertIdentifier(expr.parameters.get(0), "a");

        Expression body = expr.body;
        assertTrue(body instanceof ArrowFunctionExpression);
        ArrowFunctionExpression arrowFunc = (ArrowFunctionExpression) body;

        assertEquals(1, arrowFunc.parameters.size());
        assertIdentifier(arrowFunc.parameters.get(0), "b");

        Expression arrowFuncBody = arrowFunc.body;
        assertEquals(
            arrowFuncBody,
            new InfixExpression(
                new Token(PLUS, "+"),
                "+",
                new Identifier(new Token(IDENT, "a"), "a"),
                new Identifier(new Token(IDENT, "b"), "b")
            )
        );
    }

    @Test
    public void testArrowFunction_errors() {
        String input = "1 => { 24 }";
        Parser p = new Parser(new Lexer(input));
        p.parseModule();
        assertTrue(p.errors.size() != 0);
    }

    @TestFactory
    public List<DynamicTest> testCallExpression() {
        class TestCase {
            public final String input;
            public final String targetRepr;
            public final List<String> argReprs;

            public TestCase(String input, String targetRepr, List<String> argReprs) {
                this.input = input;
                this.targetRepr = targetRepr;
                this.argReprs = argReprs;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase("add(1, 2)", "add", Lists.newArrayList("1", "2")),
            new TestCase("add(a + 1, 2 * 2)", "add", Lists.newArrayList("(a + 1)", "(2 * 2)")),
            new TestCase("(a => a + 1)(2)", "a => (a + 1)", Lists.newArrayList("2")),
            new TestCase("map(arr, a => a)", "map", Lists.newArrayList("arr", "a => a"))
        );

        return tests.stream()
            .map(testCase -> {
                String input = testCase.input;
                String targetRepr = testCase.targetRepr;
                List<String> argReprs = testCase.argReprs;

                String name = String.format("'%s', should be a CallExpression, applying %s to '%s'", input, argReprs, targetRepr);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertTrue(statement.expression instanceof CallExpression);
                    CallExpression expr = (CallExpression) statement.expression;

                    assertEquals(targetRepr, expr.target.repr(true, 0));
                    assertEquals(
                        argReprs,
                        expr.arguments.stream()
                            .map(arg -> arg.repr(true, 0))
                            .collect(toList())
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testIndexExpression() {
        class TestCase {
            public final String input;
            public final String targetRepr;
            public final String index;

            public TestCase(String input, String targetRepr, String index) {
                this.input = input;
                this.targetRepr = targetRepr;
                this.index = index;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase("arr[1]", "arr", "1"),
            new TestCase("[1, 2, 3][1]", "[1, 2, 3]", "1")
        );

        return tests.stream()
            .map(testCase -> {
                String input = testCase.input;
                String targetRepr = testCase.targetRepr;
                String index = testCase.index;

                String name = String.format("'%s', should be an IndexExpression, indexing into %s via '%s'", input, targetRepr, index);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertTrue(statement.expression instanceof IndexExpression);
                    IndexExpression expr = (IndexExpression) statement.expression;

                    assertEquals(targetRepr, expr.target.repr(true, 0));
                    assertEquals(index, expr.index.repr(true, 0));
                });
            })
            .collect(toList());
    }
}