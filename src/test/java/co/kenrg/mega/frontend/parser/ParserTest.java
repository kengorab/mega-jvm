package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseExpressionStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatementAndGetErrors;
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
import java.util.function.Function;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.AssignmentExpression;
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
import co.kenrg.mega.frontend.ast.expression.ParenthesizedExpression;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.RangeExpression;
import co.kenrg.mega.frontend.ast.expression.StringInterpolationExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ForLoopStatement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.ast.type.BasicTypeExpression;
import co.kenrg.mega.frontend.ast.type.FunctionTypeExpression;
import co.kenrg.mega.frontend.ast.type.ParametrizedTypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Position;
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
                        Statement statement = parseStatement(letStmt);
                        assertTrue(statement instanceof LetStatement);

                        assertEquals(ident, ((LetStatement) statement).name.value);
                    });
                }
            )
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypeAnnotations_identifiers() {
        class TestCase {
            private final String input;
            private final String identName;
            private final TypeExpression identType;
            private final Function<Statement, Identifier> getIdentifier;

            private TestCase(String input, String identName, TypeExpression identType, Function<Statement, Identifier> getIdentifier) {
                this.input = input;
                this.identName = identName;
                this.identType = identType;
                this.getIdentifier = getIdentifier;
            }
        }

        Function<Statement, Identifier> getLetStmtIdent = s -> ((LetStatement) s).name;
        Function<Statement, Identifier> getVarStmtIdent = s -> ((VarStatement) s).name;
        Function<Integer, Function<Statement, Identifier>> getFuncStmtParamIdent = i -> s -> ((FunctionDeclarationStatement) s).parameters.get(i);
        Function<Integer, Function<Statement, Identifier>> getArrowFuncExprParamIdent = i -> s -> ((ArrowFunctionExpression) ((ExpressionStatement) s).expression).parameters.get(i);
        List<TestCase> tests = Lists.newArrayList(
            new TestCase(
                "let x: Int = 4",
                "x",
                new BasicTypeExpression("Int", Position.at(1, 8)),
                getLetStmtIdent
            ),
            new TestCase(
                "let s: String = \"asdf\"",
                "s",
                new BasicTypeExpression("String", Position.at(1, 8)),
                getLetStmtIdent
            ),
            new TestCase(
                "let s: Array[String] = [\"asdf\"]",
                "s",
                new ParametrizedTypeExpression("Array", Lists.newArrayList(new BasicTypeExpression("String", Position.at(1, 14))), Position.at(1, 8)),
                getLetStmtIdent
            ),
            new TestCase(
                "let s: Array[Array[String]] = [[\"asdf\"]]",
                "s",
                new ParametrizedTypeExpression("Array", Lists.newArrayList(new ParametrizedTypeExpression("Array", Lists.newArrayList(new BasicTypeExpression("String", Position.at(1, 20))), Position.at(1, 14))), Position.at(1, 8)),
                getLetStmtIdent
            ),
            new TestCase(
                "let s: SomeType[A, B] = [[\"asdf\"]]",
                "s",
                new ParametrizedTypeExpression("SomeType", Lists.newArrayList(
                    new BasicTypeExpression("A", Position.at(1, 17)),
                    new BasicTypeExpression("B", Position.at(1, 20))
                ), Position.at(1, 8)),
                getLetStmtIdent
            ),

            new TestCase("var x: Int = 4", "x", new BasicTypeExpression("Int", Position.at(1, 8)), getVarStmtIdent),
            new TestCase("var s: String = \"asdf\"", "s", new BasicTypeExpression("String", Position.at(1, 8)), getVarStmtIdent),

            new TestCase("func abc(a: Int, b: Int) { a + b }", "a", new BasicTypeExpression("Int", Position.at(1, 13)), getFuncStmtParamIdent.apply(0)),
            new TestCase("func abc(a: Int, b: Int) { a + b }", "b", new BasicTypeExpression("Int", Position.at(1, 21)), getFuncStmtParamIdent.apply(1)),

            new TestCase("(a: String, b: String) => a + b", "a", new BasicTypeExpression("String", Position.at(1, 5)), getArrowFuncExprParamIdent.apply(0)),
            new TestCase("(a: String, b: String) => a + b", "b", new BasicTypeExpression("String", Position.at(1, 16)), getArrowFuncExprParamIdent.apply(1)),
            new TestCase("(a, b: String) => a + b", "a", null, getArrowFuncExprParamIdent.apply(0)),
            new TestCase("(a: String, b) => a + b", "b", null, getArrowFuncExprParamIdent.apply(1)),

            new TestCase(
                "(a: (Int, Int) => String, b: Int, c: Int) => a(b, c)",
                "a",
                new FunctionTypeExpression(
                    Lists.newArrayList(
                        new BasicTypeExpression("Int", Position.at(1, 6)),
                        new BasicTypeExpression("Int", Position.at(1, 11))
                    ),
                    new BasicTypeExpression("String", Position.at(1, 19)),
                    Position.at(1, 5)
                ),
                getArrowFuncExprParamIdent.apply(0)
            ),
            new TestCase(
                "(a: (Int) => String, b: Int) => a(b)",
                "a",
                new FunctionTypeExpression(
                    Lists.newArrayList(
                        new BasicTypeExpression("Int", Position.at(1, 6))
                    ),
                    new BasicTypeExpression("String", Position.at(1, 14)),
                    Position.at(1, 5)
                ),
                getArrowFuncExprParamIdent.apply(0)
            ),
            new TestCase(
                "(a: Int => String, b: Int) => a(b)",
                "a",
                new FunctionTypeExpression(
                    Lists.newArrayList(
                        new BasicTypeExpression("Int", Position.at(1, 5))
                    ),
                    new BasicTypeExpression("String", Position.at(1, 12)),
                    Position.at(1, 5)
                ),
                getArrowFuncExprParamIdent.apply(0)
            )
        );

        return tests.stream()
            .map(testCase -> {
                    String input = testCase.input;
                    Function<Statement, Identifier> getIdentifier = testCase.getIdentifier;
                    String identName = testCase.identName;
                    TypeExpression identType = testCase.identType;

                    String testName = String.format("The identifier `%s` should have type `%s` in %s", identName, identType, input);
                    return dynamicTest(testName, () -> {
                        Statement statement = parseStatement(input);
                        Identifier identifier = getIdentifier.apply(statement);

                        assertEquals(identName, identifier.value);
                        assertEquals(identType, identifier.typeAnnotation);
                    });
                }
            )
            .collect(toList());
    }

    @Test
    public void testTypeAnnotations_functionDeclarationReturnType() {
        String input = "func sum(a: Int, b: Int): Int { a + b }";
        Statement statement = parseStatement(input);
        FunctionDeclarationStatement functionDeclarationStatement = (FunctionDeclarationStatement) statement;
        assertEquals("Int", functionDeclarationStatement.typeAnnotation);
    }

    @Test
    public void testTypeAnnotations_structTypeExpression_syntaxError() {
        String input = "let person: { name: String } = { name: 'Ken' }";
        Pair<Statement, List<SyntaxError>> result = parseStatementAndGetErrors(input);
        LetStatement letStatement = (LetStatement) result.getLeft();
        assertEquals(null, letStatement.name.typeAnnotation);

        assertEquals(
            "Unexpected struct-based type definition",
            result.getRight().get(0).message
        );
    }

    @Test
    public void testLetStatement_syntaxErrors() {
        String input = "let x 4";
        Parser parser = new Parser(new Lexer(input));
        parser.parseModule();

        assertEquals(1, parser.errors.size());
    }

    @TestFactory
    public List<DynamicTest> testVarStatements() {
        List<Pair<String, String>> tests = Lists.newArrayList(
            Pair.of("var x = 4", "x"),
            Pair.of("var y = 10", "y"),
            Pair.of("var foobar = 12.45", "foobar")
        );

        return tests.stream()
            .map(testCase -> {
                    String varStmt = testCase.getLeft();
                    String ident = testCase.getRight();

                    String testName = String.format("The var-stmt `%s` should have ident `%s`", varStmt, ident);
                    return dynamicTest(testName, () -> {
                        Statement statement = parseStatement(varStmt);
                        assertTrue(statement instanceof VarStatement);

                        assertEquals(ident, ((VarStatement) statement).name.value);
                    });
                }
            )
            .collect(toList());
    }

    @Test
    public void testVarStatement_syntaxErrors() {
        String input = "var x 4";
        Parser parser = new Parser(new Lexer(input));
        parser.parseModule();

        assertEquals(1, parser.errors.size());
    }

    @TestFactory
    public List<DynamicTest> testAssignmentExpression() {
        class TestCase {
            private final String input;
            private final String identName;
            private final Object value;
            private final Position position;

            public TestCase(String input, String identName, Object value, Position position) {
                this.input = input;
                this.identName = identName;
                this.value = value;
                this.position = position;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase("x = 4", "x", 4, Position.at(1, 5)),
            new TestCase("y = 10", "y", 10, Position.at(1, 5)),
            new TestCase("foobar = \"str\"", "foobar", "str", Position.at(1, 10))
        );

        return tests.stream()
            .map(testCase -> {
                    String input = testCase.input;
                    String ident = testCase.identName;
                    Object value = testCase.value;
                    Position position = testCase.position;

                    String testName = String.format("The assign-expr `%s` should have ident `%s` and value `%s`", input, ident, value);
                    return dynamicTest(testName, () -> {
                        ExpressionStatement statement = parseExpressionStatement(input);
                        assertTrue(statement.expression instanceof AssignmentExpression);
                        AssignmentExpression assignment = (AssignmentExpression) statement.expression;

                        assertEquals(ident, assignment.name.value);
                        assertLiteralExpression(assignment.right, value, position);
                    });
                }
            )
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testParenthesizedExpressions() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("(5)", "5"),
            Pair.of("(5.3)", "5.3"),
            Pair.of("(\"asdf\")", "\"asdf\""),

            Pair.of("(1 + 3)", "1 + 3"),
            Pair.of("(1 + (3 + 4))", "1 + (3 + 4)"),
            Pair.of("(someFunc(1, 2, 3))", "someFunc(1, 2, 3)"),

            Pair.of("((a, b) => a + b)", "(a, b) => a + b"),
            Pair.of("((a) => a + 1)", "a => a + 1"),
            Pair.of("(a => a + 1)", "a => a + 1")
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String valueRepr = testCase.getRight();

                String name = String.format("'%s' should contain the expression '%s'", input, valueRepr);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertEquals(
                        valueRepr,
                        ((ParenthesizedExpression) statement.expression).expr.repr(false, 0)
                    );
                });
            })
            .collect(toList());
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
                    new Token(IDENT, "a", Position.at(1, 18)),
                    new InfixExpression(
                        new Token(PLUS, "+", Position.at(1, 20)),
                        "+",
                        new Identifier(new Token(IDENT, "a", Position.at(1, 18)), "a"),
                        new Identifier(new Token(IDENT, "b", Position.at(1, 22)), "b")
                    )
                )
            ),
            body.statements
        );
    }

    @Test
    public void testIdentifierExpression() {
        String input = "foobar;";

        ExpressionStatement statement = parseExpressionStatement(input);
        Expression identExpr = statement.expression;

        assertEquals(identExpr, new Identifier(new Token(IDENT, "foobar", Position.at(1, 1)), "foobar"));
    }

    private void assertLiteralExpression(Expression expr, Object expectedValue, Position position) {
        if (expectedValue instanceof Integer) {
            Integer value = (Integer) expectedValue;
            assertEquals(
                new IntegerLiteral(new Token(INT, String.valueOf(value), position), value),
                expr
            );
        } else if (expectedValue instanceof Float) {
            Float value = (Float) expectedValue;
            assertEquals(
                new FloatLiteral(new Token(FLOAT, String.valueOf(value), position), value),
                expr
            );
        } else if (expectedValue instanceof Boolean) {
            Boolean value = (Boolean) expectedValue;
            Token token = value
                ? new Token(TRUE, "true", position)
                : new Token(FALSE, "false", position);
            assertEquals(
                new BooleanLiteral(token, value),
                expr
            );
        } else if (expectedValue instanceof String) {
            String value = (String) expectedValue;
            assertEquals(
                new StringLiteral(new Token(STRING, value, position), value),
                expr
            );
        } else {
            fail("Cannot assert literal expr equivalence for type: " + expectedValue.getClass().getName());
        }
    }

    private void assertIdentifier(Expression expr, String identName, Position position) {
        assertEquals(expr, new Identifier(new Token(IDENT, identName, position), identName));
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
                    assertLiteralExpression(statement.expression, value, Position.at(1, 1));
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
                    assertLiteralExpression(statement.expression, value, Position.at(1, 1));
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
                    assertLiteralExpression(statement.expression, value, Position.at(1, 1));
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
                    assertLiteralExpression(statement.expression, value, Position.at(1, 1));
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testStringInterpolationExpression() {
        //TODO: The positions of interpolated expressions should be with respect to the string, not with respect to itself.
        // This is an artifact of how I'm (kind of jankily) supporting these nested expressions...
        List<Pair<String, Map<String, Expression>>> testCases = Lists.newArrayList(
            Pair.of("\"$a bc\"", ImmutableMap.of(
                "$a", new Identifier(new Token(IDENT, "a", Position.at(1, 1)), "a")
            )),
            Pair.of("\"$a $bc\"", ImmutableMap.of(
                "$a", new Identifier(new Token(IDENT, "a", Position.at(1, 1)), "a"),
                "$bc", new Identifier(new Token(IDENT, "bc", Position.at(1, 1)), "bc")
            )),
            Pair.of("\"$a ${bc}\"", ImmutableMap.of(
                "$a", new Identifier(new Token(IDENT, "a", Position.at(1, 1)), "a"),
                "${bc}", new Identifier(new Token(IDENT, "bc", Position.at(1, 1)), "bc")
            )),
            Pair.of("\"${a} ${bc}\"", ImmutableMap.of(
                "${a}", new Identifier(new Token(IDENT, "a", Position.at(1, 1)), "a"),
                "${bc}", new Identifier(new Token(IDENT, "bc", Position.at(1, 1)), "bc")
            )),
            Pair.of("\"1 + 1 = ${1 + 1}\"", ImmutableMap.of(
                "${1 + 1}", new InfixExpression(
                    new Token(PLUS, "+", Position.at(1, 3)),
                    "+",
                    new IntegerLiteral(new Token(INT, "1", Position.at(1, 1)), 1),
                    new IntegerLiteral(new Token(INT, "1", Position.at(1, 5)), 1)
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
        List<Pair<String, List<Pair<Object, Position>>>> testCases = Lists.newArrayList(
            Pair.of("[]", Lists.newArrayList()),
            Pair.of("[\"hello\", 1]", Lists.newArrayList(
                Pair.of("hello", Position.at(1, 2)),
                Pair.of(1, Position.at(1, 11)
                ))),
            Pair.of("[\"hello\", 1.2, true]", Lists.newArrayList(
                Pair.of("hello", Position.at(1, 2)),
                Pair.of(1.2f, Position.at(1, 11)),
                Pair.of(true, Position.at(1, 16))
                )
            ));

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                List<Pair<Object, Position>> elements = testCase.getRight();

                String name = String.format("'%s' should parse to an array of elements", input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    ArrayLiteral expr = (ArrayLiteral) statement.expression;
                    assertEquals(elements.size(), expr.elements.size());

                    for (int i = 0; i < elements.size(); i++) {
                        Pair<Object, Position> elem = elements.get(i);
                        Expression expression = expr.elements.get(i);
                        assertLiteralExpression(expression, elem.getLeft(), elem.getRight());
                    }
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testObjectLiteralExpression() {
        List<Pair<String, List<Triple<String, Position, String>>>> testCases = Lists.newArrayList(
            Pair.of("{}", Lists.newArrayList()),
            Pair.of("{prop1:1}", Lists.newArrayList(
                Triple.of("prop1", Position.at(1, 2), "1")
            )),
            Pair.of("{prop1:1, prop2:\"two\"}", Lists.newArrayList(
                Triple.of("prop1", Position.at(1, 2), "1"),
                Triple.of("prop2", Position.at(1, 11), "\"two\"")
            )),
            Pair.of("{prop1:1 + 1, prop2:\"two\" + \"two\"}", Lists.newArrayList(
                Triple.of("prop1", Position.at(1, 2), "(1 + 1)"),
                Triple.of("prop2", Position.at(1, 15), "(\"two\" + \"two\")")
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                List<Triple<String, Position, String>> elements = testCase.getRight();

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
                        Triple<String, Position, String> elem = elements.get(i);
                        Entry<Identifier, Expression> pair = elems.get(i);

                        assertIdentifier(pair.getKey(), elem.getLeft(), elem.getMiddle());
                        assertEquals(elem.getRight(), pair.getValue().repr(true, 0));
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
                    assertLiteralExpression(prefixExpr.expression, value, Position.at(1, 2));
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
            private final Position lPos;
            private final Position rPos;

            private TestCase(String input, String operator, Object left, Object right, Position lPos, Position rPos) {
                this.input = input;
                this.operator = operator;
                this.left = left;
                this.right = right;
                this.lPos = lPos;
                this.rPos = rPos;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("5 + 5", "+", 5, 5, Position.at(1, 1), Position.at(1, 5)),
            new TestCase("5 - 5", "-", 5, 5, Position.at(1, 1), Position.at(1, 5)),
            new TestCase("5 * 5", "*", 5, 5, Position.at(1, 1), Position.at(1, 5)),
            new TestCase("5 / 5", "/", 5, 5, Position.at(1, 1), Position.at(1, 5)),
            new TestCase("5 < 5", "<", 5, 5, Position.at(1, 1), Position.at(1, 5)),
            new TestCase("5 <= 5", "<=", 5, 5, Position.at(1, 1), Position.at(1, 6)),
            new TestCase("5 > 5", ">", 5, 5, Position.at(1, 1), Position.at(1, 5)),
            new TestCase("5 >= 5", ">=", 5, 5, Position.at(1, 1), Position.at(1, 6)),
            new TestCase("5 == 5", "==", 5, 5, Position.at(1, 1), Position.at(1, 6)),
            new TestCase("5 != 5", "!=", 5, 5, Position.at(1, 1), Position.at(1, 6))
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
                    assertLiteralExpression(infixExpr.left, testCase.left, testCase.lPos);
                    assertLiteralExpression(infixExpr.right, testCase.right, testCase.rPos);
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

            new TestCase("1 + (2 + 3) + 4", "((1 + ((2 + 3))) + 4)"),
            new TestCase("(1 + 2) * 3", "(((1 + 2)) * 3)"),
            new TestCase("-(5 + 5)", "(-((5 + 5)))"),
            new TestCase("!(true == true)", "(!((true == true)))"),

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
        assertEquals(condition.left, new Identifier(new Token(IDENT, "x", Position.at(1, 4)), "x"));
        assertEquals(condition.right, new Identifier(new Token(IDENT, "y", Position.at(1, 8)), "y"));

        BlockExpression thenBlock = (BlockExpression) ifExpression.thenExpr;
        Identifier ident = (Identifier) ((ExpressionStatement) thenBlock.statements.get(0)).expression;
        assertEquals(ident, new Identifier(new Token(IDENT, "x", Position.at(1, 12)), "x"));

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
        assertEquals(condition.left, new Identifier(new Token(IDENT, "x", Position.at(1, 4)), "x"));
        assertEquals(condition.right, new Identifier(new Token(IDENT, "y", Position.at(1, 8)), "y"));

        BlockExpression thenBlock = (BlockExpression) ifExpression.thenExpr;
        Identifier thenExpr = (Identifier) ((ExpressionStatement) thenBlock.statements.get(0)).expression;
        assertEquals(thenExpr, new Identifier(new Token(IDENT, "x", Position.at(1, 12)), "x"));

        BlockExpression elseBlock = (BlockExpression) ifExpression.elseExpr;
        Identifier elseExpr = (Identifier) ((ExpressionStatement) elseBlock.statements.get(0)).expression;
        assertEquals(elseExpr, new Identifier(new Token(IDENT, "y", Position.at(1, 23)), "y"));
    }

    @Test
    public void testIfExpression_nestedIfElse() {
        String input = "" +
            "if x < y { \n" +
            "  if x > 0 {\n" +
            "    0\n" +
            "  } else {\n" +
            "    y\n" +
            "  }\n" +
            "}";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof IfExpression);
        IfExpression ifExpression = (IfExpression) statement.expression;

        InfixExpression condition1 = (InfixExpression) ifExpression.condition;
        assertEquals("<", condition1.operator);
        assertEquals(condition1.left, new Identifier(new Token(IDENT, "x", Position.at(1, 4)), "x"));
        assertEquals(condition1.right, new Identifier(new Token(IDENT, "y", Position.at(1, 8)), "y"));

        BlockExpression thenBlock1 = (BlockExpression) ifExpression.thenExpr;
        IfExpression nestedIfExpr = (IfExpression) ((ExpressionStatement) thenBlock1.statements.get(0)).expression;
        assertNull(ifExpression.elseExpr);

        InfixExpression condition2 = (InfixExpression) nestedIfExpr.condition;
        assertEquals(">", condition2.operator);
        assertEquals(condition2.left, new Identifier(new Token(IDENT, "x", Position.at(2, 6)), "x"));
        assertLiteralExpression(condition2.right, 0, Position.at(2, 10));

        BlockExpression thenBlock2 = (BlockExpression) nestedIfExpr.thenExpr;
        IntegerLiteral thenExpr = (IntegerLiteral) ((ExpressionStatement) thenBlock2.statements.get(0)).expression;
        assertLiteralExpression(thenExpr, 0, Position.at(3, 5));

        BlockExpression elseBlock = (BlockExpression) nestedIfExpr.elseExpr;
        Identifier elseExpr = (Identifier) ((ExpressionStatement) elseBlock.statements.get(0)).expression;
        assertEquals(elseExpr, new Identifier(new Token(IDENT, "y", Position.at(5, 5)), "y"));
    }

    @TestFactory
    public List<DynamicTest> testArrowFunction() {
        class TestCase {
            public final String input;
            public final List<String> params;
            public final Position bodyPosition;

            public TestCase(String input, List<String> params, Position bodyPosition) {
                this.input = input;
                this.params = params;
                this.bodyPosition = bodyPosition;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("(a, b) => { 24 }", Lists.newArrayList("a", "b"), Position.at(1, 13)),
            new TestCase("(a) => { 24 }", Lists.newArrayList("a"), Position.at(1, 10)),
            new TestCase("() => { 24 }", Lists.newArrayList(), Position.at(1, 9)),
            new TestCase("() => 24", Lists.newArrayList(), Position.at(1, 7)),
            new TestCase("a => { 24 }", Lists.newArrayList("a"), Position.at(1, 8)),
            new TestCase("a => 24", Lists.newArrayList("a"), Position.at(1, 6))
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
                        assertLiteralExpression(((ExpressionStatement) block.statements.get(0)).expression, 24, testCase.bodyPosition);
                    } else {
                        assertLiteralExpression(body, 24, testCase.bodyPosition);
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

        assertIdentifier(expr.parameters.get(0), "a", Position.at(1, 1));

        Expression body = expr.body;
        assertTrue(body instanceof ArrowFunctionExpression);
        ArrowFunctionExpression arrowFunc = (ArrowFunctionExpression) body;

        assertEquals(1, arrowFunc.parameters.size());
        assertIdentifier(arrowFunc.parameters.get(0), "b", Position.at(1, 6));

        Expression arrowFuncBody = arrowFunc.body;
        assertEquals(
            arrowFuncBody,
            new InfixExpression(
                new Token(PLUS, "+", Position.at(1, 13)),
                "+",
                new Identifier(new Token(IDENT, "a", Position.at(1, 11)), "a"),
                new Identifier(new Token(IDENT, "b", Position.at(1, 15)), "b")
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
            new TestCase("(a => a + 1)(2)", "(a => (a + 1))", Lists.newArrayList("2")),
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
            new TestCase("(arr)[1]", "(arr)", "1"),
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

    @Test
    public void testForInLoop() {
        String input = "for x in arr { x + 1 }";

        Statement statement = parseStatement(input);
        assertTrue(statement instanceof ForLoopStatement);

        ForLoopStatement forLoop = (ForLoopStatement) statement;

        assertIdentifier(forLoop.iterator, "x", Position.at(1, 5));
        assertIdentifier(forLoop.iteratee, "arr", Position.at(1, 10));

        assertEquals(1, forLoop.block.statements.size());
        InfixExpression body = (InfixExpression) ((ExpressionStatement) forLoop.block.statements.get(0)).expression;
        assertEquals("+", body.operator);
        assertIdentifier(body.left, "x", Position.at(1, 16));
        assertEquals(new IntegerLiteral(new Token(INT, "1", Position.at(1, 20)), 1), body.right);
    }

    @TestFactory
    public List<DynamicTest> testTypeDeclarationStatement() {
        class TestCase {
            public final String input;
            public final String typeName;
            public final TypeExpression typeExpr;

            public TestCase(String input, String typeName, TypeExpression typeExpr) {
                this.input = input;
                this.typeName = typeName;
                this.typeExpr = typeExpr;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase(
                "type Id = Int",
                "Id",
                new BasicTypeExpression("Int", Position.at(1, 11))
            ),
            new TestCase(
                "type Name = String",
                "Name",
                new BasicTypeExpression("String", Position.at(1, 13))
            ),
            new TestCase(
                "type Names = Array[String]",
                "Names",
                new ParametrizedTypeExpression(
                    "Array",
                    Lists.newArrayList(new BasicTypeExpression("String", Position.at(1, 20))),
                    Position.at(1, 14)
                )
            ),
            new TestCase(
                "type Matrix = Array[Array[Int]]",
                "Matrix",
                new ParametrizedTypeExpression(
                    "Array",
                    Lists.newArrayList(
                        new ParametrizedTypeExpression(
                            "Array",
                            Lists.newArrayList(new BasicTypeExpression("Int", Position.at(1, 27))),
                            Position.at(1, 21)
                        )
                    ),
                    Position.at(1, 15)
                )
            ),
            new TestCase(
                "type UnaryOp = Int => Int",
                "UnaryOp",
                new FunctionTypeExpression(
                    Lists.newArrayList(new BasicTypeExpression("Int", Position.at(1, 16))),
                    new BasicTypeExpression("Int", Position.at(1, 23)),
                    Position.at(1, 16)
                )
            ),
            new TestCase(
                "type UnaryOp = (Int) => Int",
                "UnaryOp",
                new FunctionTypeExpression(
                    Lists.newArrayList(new BasicTypeExpression("Int", Position.at(1, 17))),
                    new BasicTypeExpression("Int", Position.at(1, 25)),
                    Position.at(1, 16)
                )
            ),
            new TestCase(
                "type BinOp = (Int, Int) => Int",
                "BinOp",
                new FunctionTypeExpression(
                    Lists.newArrayList(
                        new BasicTypeExpression("Int", Position.at(1, 15)),
                        new BasicTypeExpression("Int", Position.at(1, 20))
                    ),
                    new BasicTypeExpression("Int", Position.at(1, 28)),
                    Position.at(1, 14)
                )
            )
        );

        return tests.stream()
            .map(testCase -> {
                String input = testCase.input;
                String typeName = testCase.typeName;
                TypeExpression typeExpr = testCase.typeExpr;

                String name = String.format("'%s' should declare type %s as %s", input, typeName, typeExpr.signature());
                return dynamicTest(name, () -> {
                    Statement statement = parseStatement(input);
                    assertTrue(statement instanceof TypeDeclarationStatement);
                    TypeDeclarationStatement typeDecl = (TypeDeclarationStatement) statement;

                    assertEquals(typeName, typeDecl.typeName.value);
                    assertEquals(typeExpr, typeDecl.typeExpr);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testRangeExpression() {
        class TestCase {
            public final String input;
            public final String lRepr;
            public final String rRepr;

            public TestCase(String input, String lRepr, String rRepr) {
                this.input = input;
                this.lRepr = lRepr;
                this.rRepr = rRepr;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase("1..3", "1", "3"),
            new TestCase("x..3", "x", "3"),
            new TestCase("x..3 - 1", "x", "(3 - 1)"),
            new TestCase("x + 1..x - 1", "(x + 1)", "(x - 1)"),
            new TestCase("x + 1..(x..5)[4]", "(x + 1)", "((x..5)[4])")
        );

        return tests.stream()
            .map(testCase -> {
                String input = testCase.input;
                String lRepr = testCase.lRepr;
                String rRepr = testCase.rRepr;

                String name = String.format("'%s', should be RangeExpression, from %s to %s", input, lRepr, rRepr);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertTrue(statement.expression instanceof RangeExpression);
                    RangeExpression expr = (RangeExpression) statement.expression;

                    assertEquals(lRepr, expr.leftBound.repr(true, 0));
                    assertEquals(rRepr, expr.rightBound.repr(true, 0));
                });
            })
            .collect(toList());
    }
}