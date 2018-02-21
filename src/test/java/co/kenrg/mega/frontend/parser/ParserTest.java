package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseExpressionStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatementAndGetErrors;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatementAndGetModule;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatementAndGetWarnings;
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
import co.kenrg.mega.frontend.ast.expression.AccessorExpression;
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
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.expression.ParenthesizedExpression;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.RangeExpression;
import co.kenrg.mega.frontend.ast.expression.StringInterpolationExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Exportable;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ForLoopStatement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ImportStatement;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.ast.type.BasicTypeExpression;
import co.kenrg.mega.frontend.ast.type.FunctionTypeExpression;
import co.kenrg.mega.frontend.ast.type.ParametrizedTypeExpression;
import co.kenrg.mega.frontend.ast.type.StructTypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.utils.LinkedHashMultimaps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ParserTest {

    @TestFactory
    List<DynamicTest> testValStatements() {
        List<Pair<String, String>> tests = Lists.newArrayList(
            Pair.of("val x = 4", "x"),
            Pair.of("val y = 10", "y"),
            Pair.of("val foobar = 12.45", "foobar")
        );

        return tests.stream()
            .map(testCase -> {
                    String valStmt = testCase.getLeft();
                    String ident = testCase.getRight();

                    String testName = String.format("The val-stmt `%s` should have ident `%s`", valStmt, ident);
                    return dynamicTest(testName, () -> {
                        Statement statement = parseStatement(valStmt);
                        assertTrue(statement instanceof ValStatement);

                        assertEquals(ident, ((ValStatement) statement).name.value);
                    });
                }
            )
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypeAnnotations_identifiers() {
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

        Function<Statement, Identifier> getValStmtIdent = s -> ((ValStatement) s).name;
        Function<Statement, Identifier> getVarStmtIdent = s -> ((VarStatement) s).name;
        Function<Integer, Function<Statement, Identifier>> getFuncStmtParamIdent = i -> s -> ((FunctionDeclarationStatement) s).parameters.get(i).ident;
        Function<Integer, Function<Statement, Identifier>> getArrowFuncExprParamIdent = i -> s -> ((ArrowFunctionExpression) ((ExpressionStatement) s).expression).parameters.get(i).ident;
        List<TestCase> tests = Lists.newArrayList(
            new TestCase(
                "val x: Int = 4",
                "x",
                new BasicTypeExpression("Int", Position.at(1, 8)),
                getValStmtIdent
            ),
            new TestCase(
                "val s: String = \"asdf\"",
                "s",
                new BasicTypeExpression("String", Position.at(1, 8)),
                getValStmtIdent
            ),
            new TestCase(
                "val s: Array[String] = [\"asdf\"]",
                "s",
                new ParametrizedTypeExpression("Array", Lists.newArrayList(new BasicTypeExpression("String", Position.at(1, 14))), Position.at(1, 8)),
                getValStmtIdent
            ),
            new TestCase(
                "val s: Array[Array[String]] = [[\"asdf\"]]",
                "s",
                new ParametrizedTypeExpression("Array", Lists.newArrayList(new ParametrizedTypeExpression("Array", Lists.newArrayList(new BasicTypeExpression("String", Position.at(1, 20))), Position.at(1, 14))), Position.at(1, 8)),
                getValStmtIdent
            ),
            new TestCase(
                "val s: SomeType[A, B] = [[\"asdf\"]]",
                "s",
                new ParametrizedTypeExpression("SomeType", Lists.newArrayList(
                    new BasicTypeExpression("A", Position.at(1, 17)),
                    new BasicTypeExpression("B", Position.at(1, 20))
                ), Position.at(1, 8)),
                getValStmtIdent
            ),

            new TestCase("var x: Int = 4", "x", new BasicTypeExpression("Int", Position.at(1, 8)), getVarStmtIdent),
            new TestCase("var s: String = \"asdf\"", "s", new BasicTypeExpression("String", Position.at(1, 8)), getVarStmtIdent),
            new TestCase("var p: { name: String } = { name: \"asdf\" }", "p", new StructTypeExpression(LinkedHashMultimaps.of("name", new BasicTypeExpression("String", Position.at(1, 16))), Position.at(1, 8)), getVarStmtIdent),

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
            ),
            new TestCase(
                "(a: () => String) => a()",
                "a",
                new FunctionTypeExpression(
                    Lists.newArrayList(),
                    new BasicTypeExpression("String", Position.at(1, 11)),
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
    void testTypeAnnotations_functionDeclarationReturnType() {
        String input = "func sum(a: Int, b: Int): Int { a + b }";
        Statement statement = parseStatement(input);
        FunctionDeclarationStatement functionDeclarationStatement = (FunctionDeclarationStatement) statement;
        assertEquals("Int", functionDeclarationStatement.typeAnnotation);
    }

    @Test
    void testTypeAnnotations_structTypeExpression() {
        String input = "val person: { name: String } = { name: 'Ken' }";
        Pair<Statement, List<SyntaxError>> result = parseStatementAndGetErrors(input);
        ValStatement valStatement = (ValStatement) result.getLeft();

        StructTypeExpression expected = new StructTypeExpression(
            LinkedHashMultimaps.of(
                "name", new BasicTypeExpression("String", Position.at(1, 21))
            ),
            Position.at(1, 13)
        );
        assertEquals(expected, valStatement.name.typeAnnotation);
    }

    @TestFactory
    List<DynamicTest> testTypeAnnotations_structTypeExpression_typeAnnotationIsTooVerbose_addWarning() {
        List<String> testCases = Lists.newArrayList(
            "val person: { firstName: String, lastName: String, age: Int } = { name: 'Ken' }",
            "val person: { f: Int, l: Int, a: Int } = { name: 'Ken' }",
            "val person: { a: { b: Int } } = { name: 'Ken' }"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("Parsing `%s` should result in a warning, due to length of type signature", testCase);

                return dynamicTest(name, () -> {
                    Pair<Statement, List<SyntaxError>> result = parseStatementAndGetWarnings(testCase);
                    List<SyntaxError> warnings = result.getRight();

                    List<SyntaxError> expected = Lists.newArrayList(
                        new SyntaxError("Type signature is a bit too verbose, consider defining as a separate type?", Position.at(1, 13))
                    );

                    assertEquals(expected, warnings);
                });
            })
            .collect(toList());
    }

    @Test
    void testValStatement_syntaxErrors() {
        String input = "val x 4";
        Parser parser = new Parser(new Lexer(input));
        parser.parseModule();

        assertEquals(1, parser.errors.size());
    }

    @TestFactory
    List<DynamicTest> testVarStatements() {
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
    void testVarStatement_syntaxErrors() {
        String input = "var x 4";
        Parser parser = new Parser(new Lexer(input));
        parser.parseModule();

        assertEquals(1, parser.errors.size());
    }

    @TestFactory
    List<DynamicTest> testAssignmentExpression() {
        class TestCase {
            private final String input;
            private final String identName;
            private final Object value;
            private final Position position;

            private TestCase(String input, String identName, Object value, Position position) {
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
    List<DynamicTest> testParenthesizedExpressions() {
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
    void testFunctionDeclarationStatement() {
        String input = "func add(a, b) { a + b }";
        Parser parser = new Parser(new Lexer(input));
        Module module = parser.parseModule();
        assertEquals(0, parser.errors.size());
        FunctionDeclarationStatement actual = (FunctionDeclarationStatement) module.statements.get(0);
        FunctionDeclarationStatement expected = new FunctionDeclarationStatement(
            Token.function(Position.at(1, 1)),
            new Identifier(Token.ident("add", Position.at(1, 6)), "add"),
            Lists.newArrayList(
                new Parameter(new Identifier(Token.ident("a", Position.at(1, 10)), "a")),
                new Parameter(new Identifier(Token.ident("b", Position.at(1, 13)), "b"))
            ),
            new BlockExpression(
                Token.lbrace(Position.at(1, 16)),
                Lists.newArrayList(
                    new ExpressionStatement(
                        Token.ident("a", Position.at(1, 18)),
                        new InfixExpression(
                            Token.plus(Position.at(1, 20)),
                            "+",
                            new Identifier(Token.ident("a", Position.at(1, 18)), "a"),
                            new Identifier(Token.ident("b", Position.at(1, 22)), "b")
                        )
                    )
                )
            )
        );
        assertEquals(expected, actual);
    }

    @Test
    void testFunctionDeclarationStatement_typedAndDefaultValuedParams() {
        String input = "func add(a, b = 4, c: Int = 12) { a + b }";
        Parser parser = new Parser(new Lexer(input));
        Module module = parser.parseModule();
        assertEquals(0, parser.errors.size());
        FunctionDeclarationStatement actual = (FunctionDeclarationStatement) module.statements.get(0);

        FunctionDeclarationStatement expected = new FunctionDeclarationStatement(
            Token.function(Position.at(1, 1)),
            new Identifier(Token.ident("add", Position.at(1, 6)), "add"),
            Lists.newArrayList(
                new Parameter(
                    new Identifier(Token.ident("a", Position.at(1, 10)), "a")
                ),
                new Parameter(
                    new Identifier(Token.ident("b", Position.at(1, 13)), "b"),
                    new IntegerLiteral(Token._int("4", Position.at(1, 17)), 4)
                ),
                new Parameter(
                    new Identifier(Token.ident("c", Position.at(1, 20)), "c", new BasicTypeExpression("Int", Position.at(1, 23))),
                    new IntegerLiteral(Token._int("12", Position.at(1, 29)), 12)
                )
            ),
            new BlockExpression(
                Token.lbrace(Position.at(1, 33)),
                Lists.newArrayList(
                    new ExpressionStatement(
                        Token.ident("a", Position.at(1, 35)),
                        new InfixExpression(
                            Token.plus(Position.at(1, 37)),
                            "+",
                            new Identifier(Token.ident("a", Position.at(1, 35)), "a"),
                            new Identifier(Token.ident("b", Position.at(1, 39)), "b")
                        )
                    )
                )
            )
        );
        assertEquals(expected, actual);
    }

    @Test
    void testFunctionDeclarationStatement_bodyIsSingleValueExpression() {
        String input = "func add(a, b) = a + b";
        Parser parser = new Parser(new Lexer(input));
        Module module = parser.parseModule();
        assertEquals(0, parser.errors.size());
        FunctionDeclarationStatement actual = (FunctionDeclarationStatement) module.statements.get(0);
        FunctionDeclarationStatement expected = new FunctionDeclarationStatement(
            Token.function(Position.at(1, 1)),
            new Identifier(Token.ident("add", Position.at(1, 6)), "add"),
            Lists.newArrayList(
                new Parameter(new Identifier(Token.ident("a", Position.at(1, 10)), "a")),
                new Parameter(new Identifier(Token.ident("b", Position.at(1, 13)), "b"))
            ),
            new InfixExpression(
                Token.plus(Position.at(1, 20)),
                "+",
                new Identifier(Token.ident("a", Position.at(1, 18)), "a"),
                new Identifier(Token.ident("b", Position.at(1, 22)), "b")
            )
        );
        assertEquals(expected, actual);
    }

    @Test
    void testFunctionDeclarationStatement_bodyIsSingleValueBlockExpression_raisesWarning() {
        String input = "func add(a, b) = { a + b }";
        Parser parser = new Parser(new Lexer(input));
        Module module = parser.parseModule();
        assertEquals(0, parser.errors.size());
        FunctionDeclarationStatement actual = (FunctionDeclarationStatement) module.statements.get(0);
        FunctionDeclarationStatement expected = new FunctionDeclarationStatement(
            Token.function(Position.at(1, 1)),
            new Identifier(Token.ident("add", Position.at(1, 6)), "add"),
            Lists.newArrayList(
                new Parameter(new Identifier(Token.ident("a", Position.at(1, 10)), "a")),
                new Parameter(new Identifier(Token.ident("b", Position.at(1, 13)), "b"))
            ),
            new BlockExpression(
                Token.lbrace(Position.at(1, 18)),
                Lists.newArrayList(
                    new ExpressionStatement(
                        Token.ident("a", Position.at(1, 20)),
                        new InfixExpression(
                            Token.plus(Position.at(1, 22)),
                            "+",
                            new Identifier(Token.ident("a", Position.at(1, 20)), "a"),
                            new Identifier(Token.ident("b", Position.at(1, 24)), "b")
                        )
                    )
                )
            )
        );
        assertEquals(expected, actual);

        List<SyntaxError> expectedWarnings = Lists.newArrayList(
            new SyntaxError("Unnecessary equals sign; a function whose single-expression body is a block is pointless", Position.at(1, 16))
        );
        assertEquals(expectedWarnings, parser.warnings);
    }

    @Test
    void testParseSingleImportStatement() {
        String input = "import a from \"co.kenrg.some-package.Module\"";
        Module module = parseStatementAndGetModule(input);
        Statement statement = module.statements.get(0);
        assertTrue(statement instanceof ImportStatement);
        ImportStatement importStmt = (ImportStatement) statement;

        ImportStatement expected = new ImportStatement(
            Token._import(Position.at(1, 1)),
            Lists.newArrayList(
                new Identifier(
                    Token.ident("a", Position.at(1, 8)),
                    "a"
                )
            ),
            new StringLiteral(
                Token.string("co.kenrg.some-package.Module", Position.at(1, 15)),
                "co.kenrg.some-package.Module"
            )
        );
        assertEquals(expected, importStmt);

        assertEquals(Lists.newArrayList(expected), module.imports);
    }

    @Test
    void testImportStatement() {
        String input = "import a, bc, def from 'co.kenrg.some-package.Module'";
        Module module = parseStatementAndGetModule(input);
        Statement statement = module.statements.get(0);
        assertTrue(statement instanceof ImportStatement);
        ImportStatement importStmt = (ImportStatement) statement;

        ImportStatement expected = new ImportStatement(
            Token._import(Position.at(1, 1)),
            Lists.newArrayList(
                new Identifier(
                    Token.ident("a", Position.at(1, 8)),
                    "a"
                ),
                new Identifier(
                    Token.ident("bc", Position.at(1, 11)),
                    "bc"
                ),
                new Identifier(
                    Token.ident("def", Position.at(1, 15)),
                    "def"
                )
            ),
            new StringLiteral(
                Token.string("co.kenrg.some-package.Module", Position.at(1, 24)),
                "co.kenrg.some-package.Module"
            )
        );
        assertEquals(expected, importStmt);

        assertEquals(Lists.newArrayList(expected), module.imports);
    }

    @Test
    void testImportStatement_errors() {
        String input = "import from 'co.kenrg.some-package.Module'";

        Parser p = new Parser(new Lexer(input));
        p.parseModule();
        assertTrue(p.errors.get(0).message.contains("Invalid imported name: 'from'"));
    }

    @Test
    void testIdentifierExpression() {
        String input = "foobar;";

        ExpressionStatement statement = parseExpressionStatement(input);
        Expression identExpr = statement.expression;

        assertEquals(identExpr, new Identifier(Token.ident("foobar", Position.at(1, 1)), "foobar"));
    }

    private void assertLiteralExpression(Expression expr, Object expectedValue, Position position) {
        if (expectedValue instanceof Integer) {
            Integer value = (Integer) expectedValue;
            assertEquals(
                new IntegerLiteral(Token._int(String.valueOf(value), position), value),
                expr
            );
        } else if (expectedValue instanceof Float) {
            Float value = (Float) expectedValue;
            assertEquals(
                new FloatLiteral(Token._float(String.valueOf(value), position), value),
                expr
            );
        } else if (expectedValue instanceof Boolean) {
            Boolean value = (Boolean) expectedValue;
            Token token = value
                ? Token._true(position)
                : Token._false(position);
            assertEquals(
                new BooleanLiteral(token, value),
                expr
            );
        } else if (expectedValue instanceof String) {
            String value = (String) expectedValue;
            assertEquals(
                new StringLiteral(Token.string(value, position), value),
                expr
            );
        } else {
            fail("Cannot assert literal expr equivalence for type: " + expectedValue.getClass().getName());
        }
    }

    private void assertIdentifier(Expression expr, String identName, Position position) {
        assertEquals(expr, new Identifier(Token.ident(identName, position), identName));
    }

    @TestFactory
    List<DynamicTest> testIntegerLiteralExpression() {
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
    List<DynamicTest> testFloatLiteralExpression() {
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
    List<DynamicTest> testBooleanLiteralExpression() {
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
    List<DynamicTest> testStringLiteralExpression() {
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
    List<DynamicTest> testStringInterpolationExpression() {
        //TODO: The positions of interpolated expressions should be with respect to the string, not with respect to itself.
        // This is an artifact of how I'm (kind of jankily) supporting these nested expressions...
        List<Pair<String, Map<String, Expression>>> testCases = Lists.newArrayList(
            Pair.of("\"$a bc\"", ImmutableMap.of(
                "$a", new Identifier(Token.ident("a", Position.at(1, 1)), "a")
            )),
            Pair.of("\"$a $bc\"", ImmutableMap.of(
                "$a", new Identifier(Token.ident("a", Position.at(1, 1)), "a"),
                "$bc", new Identifier(Token.ident("bc", Position.at(1, 1)), "bc")
            )),
            Pair.of("\"$a ${bc}\"", ImmutableMap.of(
                "$a", new Identifier(Token.ident("a", Position.at(1, 1)), "a"),
                "${bc}", new Identifier(Token.ident("bc", Position.at(1, 1)), "bc")
            )),
            Pair.of("\"${a} ${bc}\"", ImmutableMap.of(
                "${a}", new Identifier(Token.ident("a", Position.at(1, 1)), "a"),
                "${bc}", new Identifier(Token.ident("bc", Position.at(1, 1)), "bc")
            )),
            Pair.of("\"1 + 1 = ${1 + 1}\"", ImmutableMap.of(
                "${1 + 1}", new InfixExpression(
                    Token.plus(Position.at(1, 3)),
                    "+",
                    new IntegerLiteral(Token._int("1", Position.at(1, 1)), 1),
                    new IntegerLiteral(Token._int("1", Position.at(1, 5)), 1)
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
    List<DynamicTest> testArrayLiteralExpression_elementsAreLiterals() {
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
    List<DynamicTest> testObjectLiteralExpression() {
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
                    List<Entry<Identifier, Expression>> elems = Lists.newArrayList(expr.pairs.entries());
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
    List<DynamicTest> testPrefixExpressions() {
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
    List<DynamicTest> testInfixExpressions() {
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
            new TestCase("5 != 5", "!=", 5, 5, Position.at(1, 1), Position.at(1, 6)),

            new TestCase("true && true", "&&", true, true, Position.at(1, 1), Position.at(1, 9)),
            new TestCase("false || true", "||", false, true, Position.at(1, 1), Position.at(1, 10))
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
    List<DynamicTest> testOperatorPrecedence() {
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
    void testIfExpression() {
        String input = "if x < y { x }";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof IfExpression);
        IfExpression ifExpression = (IfExpression) statement.expression;

        InfixExpression condition = (InfixExpression) ifExpression.condition;
        assertEquals("<", condition.operator);
        assertEquals(condition.left, new Identifier(Token.ident("x", Position.at(1, 4)), "x"));
        assertEquals(condition.right, new Identifier(Token.ident("y", Position.at(1, 8)), "y"));

        BlockExpression thenBlock = ifExpression.thenExpr;
        Identifier ident = (Identifier) ((ExpressionStatement) thenBlock.statements.get(0)).expression;
        assertEquals(ident, new Identifier(Token.ident("x", Position.at(1, 12)), "x"));

        assertNull(ifExpression.elseExpr);
    }

    @Test
    void testIfElseExpression() {
        String input = "if x < y { x } else { y }";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof IfExpression);
        IfExpression ifExpression = (IfExpression) statement.expression;

        InfixExpression condition = (InfixExpression) ifExpression.condition;
        assertEquals("<", condition.operator);
        assertEquals(condition.left, new Identifier(Token.ident("x", Position.at(1, 4)), "x"));
        assertEquals(condition.right, new Identifier(Token.ident("y", Position.at(1, 8)), "y"));

        BlockExpression thenBlock = ifExpression.thenExpr;
        Identifier thenExpr = (Identifier) ((ExpressionStatement) thenBlock.statements.get(0)).expression;
        assertEquals(thenExpr, new Identifier(Token.ident("x", Position.at(1, 12)), "x"));

        BlockExpression elseBlock = ifExpression.elseExpr;
        Identifier elseExpr = (Identifier) ((ExpressionStatement) elseBlock.statements.get(0)).expression;
        assertEquals(elseExpr, new Identifier(Token.ident("y", Position.at(1, 23)), "y"));
    }

    @Test
    void testIfExpression_nestedIfElse() {
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
        assertEquals(condition1.left, new Identifier(Token.ident("x", Position.at(1, 4)), "x"));
        assertEquals(condition1.right, new Identifier(Token.ident("y", Position.at(1, 8)), "y"));

        BlockExpression thenBlock1 = ifExpression.thenExpr;
        IfExpression nestedIfExpr = (IfExpression) ((ExpressionStatement) thenBlock1.statements.get(0)).expression;
        assertNull(ifExpression.elseExpr);

        InfixExpression condition2 = (InfixExpression) nestedIfExpr.condition;
        assertEquals(">", condition2.operator);
        assertEquals(condition2.left, new Identifier(Token.ident("x", Position.at(2, 6)), "x"));
        assertLiteralExpression(condition2.right, 0, Position.at(2, 10));

        BlockExpression thenBlock2 = nestedIfExpr.thenExpr;
        IntegerLiteral thenExpr = (IntegerLiteral) ((ExpressionStatement) thenBlock2.statements.get(0)).expression;
        assertLiteralExpression(thenExpr, 0, Position.at(3, 5));

        BlockExpression elseBlock = nestedIfExpr.elseExpr;
        Identifier elseExpr = (Identifier) ((ExpressionStatement) elseBlock.statements.get(0)).expression;
        assertEquals(elseExpr, new Identifier(Token.ident("y", Position.at(5, 5)), "y"));
    }

    @TestFactory
    List<DynamicTest> testArrowFunction() {
        class TestCase {
            public final String input;
            private final List<String> params;
            private final Position bodyPosition;

            private TestCase(String input, List<String> params, Position bodyPosition) {
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
                            .map(param -> param.ident.value)
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

    @TestFactory
    List<DynamicTest> testArrowFunction_withDefaultParamValues() {
        class TestCase {
            public final String input;
            private final List<Parameter> params;
            private final Position bodyPosition;

            private TestCase(String input, List<Parameter> params, Position bodyPosition) {
                this.input = input;
                this.params = params;
                this.bodyPosition = bodyPosition;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "(a = 1, b = 14) => { 24 }",
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(Token.ident("a", Position.at(1, 2)), "a", null),
                        new IntegerLiteral(Token._int("1", Position.at(1, 6)), 1)
                    ),
                    new Parameter(
                        new Identifier(Token.ident("b", Position.at(1, 9)), "b", null),
                        new IntegerLiteral(Token._int("14", Position.at(1, 13)), 14)
                    )
                ),
                Position.at(1, 22)
            ),
            new TestCase(
                "(a, b = 14) => { 24 }",
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(Token.ident("a", Position.at(1, 2)), "a", null)
                    ),
                    new Parameter(
                        new Identifier(Token.ident("b", Position.at(1, 5)), "b", null),
                        new IntegerLiteral(Token._int("14", Position.at(1, 9)), 14)
                    )
                ),
                Position.at(1, 18)
            ),
            new TestCase(
                "(a, b: Int = 14) => { 24 }",
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(Token.ident("a", Position.at(1, 2)), "a", null)
                    ),
                    new Parameter(
                        new Identifier(
                            Token.ident("b", Position.at(1, 5)),
                            "b",
                            new BasicTypeExpression("Int", Position.at(1, 8))
                        ),
                        new IntegerLiteral(Token._int("14", Position.at(1, 14)), 14)
                    )
                ),
                Position.at(1, 23)
            ),
            new TestCase(
                "(a = 'asdf') => { 24 }",
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 2)),
                            "a",
                            null
                        ),
                        new StringLiteral(Token.string("asdf", Position.at(1, 6)), "asdf")
                    )
                ),
                Position.at(1, 19)
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("Correctly parses arrow function '%s'", testCase.input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(testCase.input);
                    assertTrue(statement.expression instanceof ArrowFunctionExpression);
                    ArrowFunctionExpression expr = (ArrowFunctionExpression) statement.expression;

                    assertEquals(testCase.params, expr.parameters);

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
    void testArrowFunction_returnsAnotherArrowFunction() {
        String input = "a => b => a + b";
        ExpressionStatement statement = parseExpressionStatement(input);
        assertTrue(statement.expression instanceof ArrowFunctionExpression);
        ArrowFunctionExpression expr = (ArrowFunctionExpression) statement.expression;

        assertIdentifier(expr.parameters.get(0).ident, "a", Position.at(1, 1));

        Expression body = expr.body;
        assertTrue(body instanceof ArrowFunctionExpression);
        ArrowFunctionExpression arrowFunc = (ArrowFunctionExpression) body;

        assertEquals(1, arrowFunc.parameters.size());
        assertIdentifier(arrowFunc.parameters.get(0).ident, "b", Position.at(1, 6));

        Expression arrowFuncBody = arrowFunc.body;
        assertEquals(
            arrowFuncBody,
            new InfixExpression(
                Token.plus(Position.at(1, 13)),
                "+",
                new Identifier(Token.ident("a", Position.at(1, 11)), "a"),
                new Identifier(Token.ident("b", Position.at(1, 15)), "b")
            )
        );
    }

    @TestFactory
    List<DynamicTest> testArrowFunction_errors() {
        List<String> testCases = Lists.newArrayList(
            "1 => { 24 }",
            "a = 4 => { 24 }",
            "(a = 4, b) => { 24 }"
        );

        return testCases.stream()
            .map(testCase ->
                dynamicTest("Parsing " + testCase + " should result in errors", () -> {
                    Parser p = new Parser(new Lexer(testCase));
                    p.parseModule();
                    assertTrue(p.errors.size() != 0);
                })
            )
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testCallExpression_nonNamedArguments() {
        class TestCase {
            public final String input;
            private final String targetRepr;
            private final List<String> argReprs;

            private TestCase(String input, String targetRepr, List<String> argReprs) {
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

                String name = String.format("'%s', should be an unnamed-args CallExpression, applying %s to '%s'", input, argReprs, targetRepr);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertTrue(statement.expression instanceof CallExpression.UnnamedArgs);
                    CallExpression.UnnamedArgs expr = (CallExpression.UnnamedArgs) statement.expression;

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
    List<DynamicTest> testCallExpression_namedArguments() {
        class TestCase {
            public final String input;
            private final String targetRepr;
            private final List<Pair<String, String>> argReprs;

            private TestCase(String input, String targetRepr, List<Pair<String, String>> argReprs) {
                this.input = input;
                this.targetRepr = targetRepr;
                this.argReprs = argReprs;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase("add(a: 1, b: 2)", "add", Lists.newArrayList(
                Pair.of("a", "1"),
                Pair.of("b", "2")
            )),
            new TestCase("add(abc: a + 1, xyz: 2 * 2)", "add", Lists.newArrayList(
                Pair.of("abc", "(a + 1)"),
                Pair.of("xyz", "(2 * 2)")
            )),
            new TestCase("(a => a + 1)(a: 2)", "(a => (a + 1))", Lists.newArrayList(
                Pair.of("a", "2")
            )),
            new TestCase("map(coll: arr, fn: a => a)", "map", Lists.newArrayList(
                Pair.of("coll", "arr"),
                Pair.of("fn", "a => a")
            ))
        );

        return tests.stream()
            .map(testCase -> {
                String input = testCase.input;
                String targetRepr = testCase.targetRepr;
                List<Pair<String, String>> argReprs = testCase.argReprs;

                String name = String.format("'%s', should be a named-args CallExpression, applying %s to '%s'", input, argReprs, targetRepr);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertTrue(statement.expression instanceof CallExpression.NamedArgs);
                    CallExpression.NamedArgs expr = (CallExpression.NamedArgs) statement.expression;

                    assertEquals(targetRepr, expr.target.repr(true, 0));
                    assertEquals(
                        argReprs,
                        expr.namedParamArguments.stream()
                            .map(arg -> Pair.of(arg.getKey().value, arg.getValue().repr(true, 0)))
                            .collect(toList())
                    );
                });
            })
            .collect(toList());
    }

    @Test
    void testCallExpression_noArgs_isUnnamedArgsCallExpression() {
        ExpressionStatement statement = parseExpressionStatement("noArgs()");
        assertTrue(statement.expression instanceof CallExpression.UnnamedArgs);
    }

    @TestFactory
    List<DynamicTest> testIndexExpression() {
        class TestCase {
            public final String input;
            private final String targetRepr;
            private final String index;

            private TestCase(String input, String targetRepr, String index) {
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
    void testForInLoop() {
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
        assertEquals(new IntegerLiteral(Token._int("1", Position.at(1, 20)), 1), body.right);
    }

    @TestFactory
    List<DynamicTest> testTypeDeclarationStatement() {
        class TestCase {
            public final String input;
            private final String typeName;
            private final TypeExpression typeExpr;

            private TestCase(String input, String typeName, TypeExpression typeExpr) {
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
    List<DynamicTest> testExportKeyword() {
        List<String> tests = Lists.newArrayList(
            "export func abc() { 1 + 1 }",
            "export val a = 'asdf'",
            "export var b = abc() + def()",
            "export type Person = { name: String }"
        );

        return tests.stream()
            .map(testCase -> {
                String name = String.format("'%s', should be parsed to a Statement, with exported = true", testCase);
                return dynamicTest(name, () -> {
                    Exportable stmt = (Exportable) parseStatement(testCase);
                    assertTrue(stmt.isExported());
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testExportKeyword_errors() {
        List<String> tests = Lists.newArrayList(
            "export for x in [1..2] { x }",
            "export 1",
            "export 1.0",
            "export true",
            "export false",
            "export 'asdf'",
            "export !false",
            "export -12",
            "export ('asdf')",
            "export if 1 < 2 { 'a' } else { 'b' }",
            "export [1, 2, 3, 4]",
            "export { a: 1, b: 'asdf' }",
            "export 1 + 1",
            "export a => a + 1",
            "export abcd(1)",
            "export arr[1]",
            "export person = { name: 'Sam' }",
            "export 12..24"
        );

        return tests.stream()
            .map(testCase -> {
                String name = String.format("'%s', should be parsed to a Statement, with exported = true", testCase);
                return dynamicTest(name, () -> {
                    Parser p = new Parser(new Lexer(testCase));
                    p.parseModule();
                    assertEquals(1, p.errors.size(), "There should be one error message");
                    assertTrue(p.errors.get(0).message.contains("Expected one of [VAL, VAR, FUNCTION, TYPE], saw"));
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testAccessorExpression() {
        class TestCase {
            public final String input;
            private final Expression expr;

            private TestCase(String input, Expression expr) {
                this.input = input;
                this.expr = expr;
            }
        }

        List<TestCase> tests = Lists.newArrayList(
            new TestCase(
                "a.b",
                new AccessorExpression(
                    Token.dot(Position.at(1, 2)),
                    new Identifier(
                        Token.ident("a", Position.at(1, 1)),
                        "a",
                        null
                    ),
                    new Identifier(
                        Token.ident("b", Position.at(1, 3)),
                        "b",
                        null
                    )
                )
            ),
            new TestCase(
                "() => a.b + b.c",
                new ArrowFunctionExpression(
                    Token.lparen(Position.at(1, 1)),
                    Lists.newArrayList(),
                    new InfixExpression(
                        Token.plus(Position.at(1, 11)),
                        "+",
                        new AccessorExpression(
                            Token.dot(Position.at(1, 8)),
                            new Identifier(
                                Token.ident("a", Position.at(1, 7)),
                                "a",
                                null
                            ),
                            new Identifier(
                                Token.ident("b", Position.at(1, 9)),
                                "b",
                                null
                            )
                        ),
                        new AccessorExpression(
                            Token.dot(Position.at(1, 14)),
                            new Identifier(
                                Token.ident("b", Position.at(1, 13)),
                                "b",
                                null
                            ),
                            new Identifier(
                                Token.ident("c", Position.at(1, 15)),
                                "c",
                                null
                            )
                        )
                    )
                )
            ),
            new TestCase(
                "a.b.c",
                new AccessorExpression(
                    Token.dot(Position.at(1, 4)),
                    new AccessorExpression(
                        Token.dot(Position.at(1, 2)),
                        new Identifier(
                            Token.ident("a", Position.at(1, 1)),
                            "a",
                            null
                        ),
                        new Identifier(
                            Token.ident("b", Position.at(1, 3)),
                            "b",
                            null
                        )
                    ),
                    new Identifier(
                        Token.ident("c", Position.at(1, 5)),
                        "c",
                        null
                    )
                )
            ),
            new TestCase(
                "'a'.b().c[0].d",
                new AccessorExpression(
                    Token.dot(Position.at(1, 13)),
                    new IndexExpression(
                        Token.lbrack(Position.at(1, 10)),
                        new AccessorExpression(
                            Token.dot(Position.at(1, 8)),
                            new CallExpression.UnnamedArgs(
                                Token.lparen(Position.at(1, 6)),
                                new AccessorExpression(
                                    Token.dot(Position.at(1, 4)),
                                    new StringLiteral(
                                        Token.string("a", Position.at(1, 1)),
                                        "a"
                                    ),
                                    new Identifier(
                                        Token.ident("b", Position.at(1, 5)),
                                        "b",
                                        null
                                    )
                                ),
                                Lists.newArrayList()
                            ),
                            new Identifier(
                                Token.ident("c", Position.at(1, 9)),
                                "c",
                                null
                            )
                        ),
                        new IntegerLiteral(
                            Token._int("0", Position.at(1, 11)),
                            0
                        )
                    ),
                    new Identifier(
                        Token.ident("d", Position.at(1, 14)),
                        "d",
                        null
                    )
                )
            )
        );

        return tests.stream()
            .map(testCase -> {
                String input = testCase.input;
                Expression expectedExpr = testCase.expr;

                String name = String.format("'%s', should be AccessorExpression", input);
                return dynamicTest(name, () -> {
                    ExpressionStatement statement = parseExpressionStatement(input);
                    assertEquals(expectedExpr, statement.expression);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testRangeExpression() {
        class TestCase {
            public final String input;
            private final String lRepr;
            private final String rRepr;

            private TestCase(String input, String lRepr, String rRepr) {
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