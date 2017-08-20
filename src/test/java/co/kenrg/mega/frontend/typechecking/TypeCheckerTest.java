package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckExpression;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckExpressionAndGetResult;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckStatement;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckStatementAndGetResult;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import co.kenrg.mega.frontend.typechecking.types.UnionType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class TypeCheckerTest {

    @TestFactory
    public List<DynamicTest> testTypecheckIntegerLiteral() {
        List<String> testCases = Lists.newArrayList(
            "5",
            "10."
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Int", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.INTEGER, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckFloatLiteral() {
        List<String> testCases = Lists.newArrayList(
            "5.0",
            "1.03",
            "0.123"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Float", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.FLOAT, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckBooleanLiteral() {
        List<String> testCases = Lists.newArrayList(
            "true", "false"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Bool", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.BOOLEAN, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckStringLiteral() {
        List<String> testCases = Lists.newArrayList(
            "\"hello\"",
            "\"hello \\u1215!\"",
            "\"Meet me at\n the \\uCAFE?\""
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to String", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.STRING, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckBindingDeclarationStatements_letAndVar() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("let s = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("let s: String = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("let i = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("let i: Int = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("let f = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("let f: Float = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("let b = true", "b", PrimitiveTypes.BOOLEAN),
            Triple.of("let b: Bool = false", "b", PrimitiveTypes.BOOLEAN),

            Triple.of("var s = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("var s: String = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("var i = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("var i: Int = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("var f = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("var f: Float = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("var b = true", "b", PrimitiveTypes.BOOLEAN),
            Triple.of("var b: Bool = false", "b", PrimitiveTypes.BOOLEAN)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should typecheck to %s", testCase.getLeft(), testCase.getRight());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    MegaType result = testTypecheckStatement(testCase.getLeft(), env);
                    assertEquals(PrimitiveTypes.UNIT, result);

                    MegaType bindingType = env.get(testCase.getMiddle());
                    assertEquals(testCase.getRight(), bindingType);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckBindingDeclarationStatements_errors() {
        List<Triple<String, MegaType, MegaType>> testCases = Lists.newArrayList(
            Triple.of("let s: String = 123", PrimitiveTypes.STRING, PrimitiveTypes.INTEGER),
            Triple.of("let i: Int = \"asdf\"", PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
            Triple.of("let f: Float = 123", PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER),
            Triple.of("let b: Bool = 123", PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER),

            Triple.of("var s: String = 123", PrimitiveTypes.STRING, PrimitiveTypes.INTEGER),
            Triple.of("var i: Int = \"asdf\"", PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
            Triple.of("var f: Float = 123", PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER),
            Triple.of("var b: Bool = 123", PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckStatementAndGetResult(testCase.getLeft());
                    assertEquals(PrimitiveTypes.UNIT, result.node.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.getMiddle(), testCase.getRight()),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @Test
    public void testTypecheckEmptyArray_arrayOfNothing() {
        MegaType type = testTypecheckExpression("[]");
        assertEquals("Array<Nothing>", type.signature());
        assertEquals(Lists.newArrayList(PrimitiveTypes.NOTHING), type.typeArgs());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckArrayWithTypeMismatches_errors() {
        class TestCase {
            public final String input;
            public final MegaType arrayType;
            public final MegaType elementType;
            public final MegaType erroneousType;

            public TestCase(String input, MegaType arrayType, MegaType elementType, MegaType erroneousType) {
                this.input = input;
                this.arrayType = arrayType;
                this.elementType = elementType;
                this.erroneousType = erroneousType;
            }
        }
        ArrayType intArray = new ArrayType(PrimitiveTypes.INTEGER);
        ArrayType strArray = new ArrayType(PrimitiveTypes.STRING);

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("[1, \"a\"]", intArray, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
            new TestCase("[1, 2, \"a\"]", intArray, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
            new TestCase("[1, \"a\", \"b\"]", intArray, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
            new TestCase("[\"a\", \"b\", 3]", strArray, PrimitiveTypes.STRING, PrimitiveTypes.INTEGER),

            // Test typechecking of arrays of arrays of disparate types
            new TestCase("[[\"a\", \"b\"], 3]", new ArrayType(strArray), new ArrayType(PrimitiveTypes.STRING), PrimitiveTypes.INTEGER),
            new TestCase("[[\"a\", \"b\"], [\"c\", 4]]", new ArrayType(strArray), PrimitiveTypes.STRING, PrimitiveTypes.INTEGER),
            new TestCase("[[\"a\", \"b\"], [3, 4]]", new ArrayType(strArray), new ArrayType(PrimitiveTypes.STRING), new ArrayType(PrimitiveTypes.INTEGER))
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.input);
                    assertEquals(testCase.arrayType, result.node.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.elementType, testCase.erroneousType),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckArray_differentTypes() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("[1, 2, 3]", PrimitiveTypes.INTEGER),
            Pair.of("[1.2, 2.2, 3.2]", PrimitiveTypes.FLOAT),
            Pair.of("[true, false, true]", PrimitiveTypes.BOOLEAN),
            Pair.of("[\"asdf\", \"qwer\", \"zxcv\"]", PrimitiveTypes.STRING),

            Pair.of("[[1, 2], [3]]", new ArrayType(PrimitiveTypes.INTEGER)),
            Pair.of("[[1.2, 2.2], [3.2]]", new ArrayType(PrimitiveTypes.FLOAT)),
            Pair.of("[[true, false], [true]]", new ArrayType(PrimitiveTypes.BOOLEAN)),
            Pair.of("[[\"asdf\", \"qwer\"], [\"zxcv\"]]", new ArrayType(PrimitiveTypes.STRING))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(new ArrayType(type), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckObjectLiteral() {
        List<Pair<String, Map<String, MegaType>>> testCases = Lists.newArrayList(
            Pair.of("{ }", ImmutableMap.of()),

            Pair.of("{ a: 1 }", ImmutableMap.of("a", PrimitiveTypes.INTEGER)),
            Pair.of("{ a: 1.2 }", ImmutableMap.of("a", PrimitiveTypes.FLOAT)),
            Pair.of("{ a: true }", ImmutableMap.of("a", PrimitiveTypes.BOOLEAN)),
            Pair.of("{ a: \"a\" }", ImmutableMap.of("a", PrimitiveTypes.STRING)),

            Pair.of("{ a: [1] }", ImmutableMap.of("a", new ArrayType(PrimitiveTypes.INTEGER))),
            Pair.of("{ a: [1.2] }", ImmutableMap.of("a", new ArrayType(PrimitiveTypes.FLOAT))),
            Pair.of("{ a: [true] }", ImmutableMap.of("a", new ArrayType(PrimitiveTypes.BOOLEAN))),
            Pair.of("{ a: [\"a\"] }", ImmutableMap.of("a", new ArrayType(PrimitiveTypes.STRING))),

            Pair.of("{ a: 1, b: 2 }", ImmutableMap.of(
                "a", PrimitiveTypes.INTEGER,
                "b", PrimitiveTypes.INTEGER
            )),
            Pair.of("{ a: 1, b: 2.2 }", ImmutableMap.of(
                "a", PrimitiveTypes.INTEGER,
                "b", PrimitiveTypes.FLOAT
            )),
            Pair.of("{ a: \"asdf\", b: 2.2 }", ImmutableMap.of(
                "a", PrimitiveTypes.STRING,
                "b", PrimitiveTypes.FLOAT
            )),
            Pair.of("{ a: true, b: [2.2] }", ImmutableMap.of(
                "a", PrimitiveTypes.BOOLEAN,
                "b", new ArrayType(PrimitiveTypes.FLOAT)
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                Map<String, MegaType> objProps = testCase.getRight();

                String name = String.format("'%s' should typecheck to an Object with appropriate props", input);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(new ObjectType(objProps), result);
                });
            })
            .collect(toList());
    }

    @Test
    public void testTypecheckObjectLiteral_nestedObjects() {
        String input = "" +
            "{\n" +
            "  a: { a1: \"asdf\", a2: false },\n" +
            "  b: [1, 2, 3, 4]\n" +
            "}";
        MegaType type = testTypecheckExpression(input);
        MegaType expected = new ObjectType(ImmutableMap.of(
            "a", new ObjectType(ImmutableMap.of(
                "a1", PrimitiveTypes.STRING,
                "a2", PrimitiveTypes.BOOLEAN
            )),
            "b", new ArrayType(PrimitiveTypes.INTEGER)
        ));
        assertEquals(expected, type);
    }

    @TestFactory
    public List<DynamicTest> testTypecheckPrefixOperator() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("!1", PrimitiveTypes.BOOLEAN),
            Pair.of("!1.3", PrimitiveTypes.BOOLEAN),
            Pair.of("!true", PrimitiveTypes.BOOLEAN),
            Pair.of("!\"asdf\"", PrimitiveTypes.BOOLEAN),
            Pair.of("![1, 2, 3]", PrimitiveTypes.BOOLEAN),
            Pair.of("!{ a: 1 }", PrimitiveTypes.BOOLEAN),
            Pair.of("!!\"asdf\"", PrimitiveTypes.BOOLEAN),

            Pair.of("-1", PrimitiveTypes.INTEGER),
            Pair.of("-1.3", PrimitiveTypes.FLOAT)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckPrefixOperator_dash_errors() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("-\"asdf\"", PrimitiveTypes.STRING),
            Pair.of("-true", PrimitiveTypes.BOOLEAN),
            Pair.of("-[1, 2, 3]", new ArrayType(PrimitiveTypes.INTEGER)),
            Pair.of("-{ a: 1 }", new ObjectType(ImmutableMap.of("a", PrimitiveTypes.INTEGER)))
        );

        MegaType numericType = new UnionType(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT);

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should fail to typecheck", input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(input);
                    assertEquals(TypeChecker.unknownType, result.node.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(numericType, type),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckInfixOperator() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            //TODO: Implement lexing/parsing of boolean and/or (&&, ||) infix operators
            // Boolean and/or
            // Pair.of("true && false", PrimitiveTypes.BOOLEAN),
            // Pair.of("true || false", PrimitiveTypes.BOOLEAN),

            // Inequalities
            Pair.of("1 < 2", PrimitiveTypes.BOOLEAN),
            Pair.of("1.2 > 3", PrimitiveTypes.BOOLEAN),
            Pair.of("1 <= 2", PrimitiveTypes.BOOLEAN),
            Pair.of("1.2 >= 3", PrimitiveTypes.BOOLEAN),

            // Equalities
            Pair.of("1 == 1", PrimitiveTypes.BOOLEAN),
            Pair.of("1.3 == 1", PrimitiveTypes.BOOLEAN),
            Pair.of("'asdf' == 'qwer'", PrimitiveTypes.BOOLEAN),
            Pair.of("[1, 2, 3] == [2, 3, 4]", PrimitiveTypes.BOOLEAN),
            Pair.of("{ a: 1, b: 2 } == { b: 1, c: 2 }", PrimitiveTypes.BOOLEAN),
            Pair.of("1 != 1", PrimitiveTypes.BOOLEAN),
            Pair.of("1.3 != 1", PrimitiveTypes.BOOLEAN),
            Pair.of("'asdf' != 'qwer'", PrimitiveTypes.BOOLEAN),
            Pair.of("[1, 2, 3] != [2, 3, 4]", PrimitiveTypes.BOOLEAN),
            Pair.of("{ a: 1, b: 2 } != { b: 1, c: 2 }", PrimitiveTypes.BOOLEAN),

            // Mathematical operations
            Pair.of("1 + 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 + 3", PrimitiveTypes.FLOAT),
            Pair.of("1 + 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 + 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1 - 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 - 3", PrimitiveTypes.FLOAT),
            Pair.of("1 - 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 - 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1 * 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 * 3", PrimitiveTypes.FLOAT),
            Pair.of("1 * 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 * 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1 / 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 / 3", PrimitiveTypes.FLOAT),
            Pair.of("1 / 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 / 3.2", PrimitiveTypes.FLOAT),

            // String concatenation
            Pair.of("'asdf' + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("3 + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("3.3 + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("[3, 4] + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("'asdf' + 3", PrimitiveTypes.STRING),
            Pair.of("'asdf' + 3.3", PrimitiveTypes.STRING),
            Pair.of("'asdf' + [3, 4]", PrimitiveTypes.STRING),

            // String repetition
            Pair.of("'asdf' * 3", PrimitiveTypes.STRING),
            Pair.of("3 * 'asdf'", PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckInfixOperator_errors() {
        class TestCase {
            public final String input;
            public final MegaType expected;
            public final MegaType actual;
            public final MegaType overallType;

            public TestCase(String input, MegaType expected, MegaType actual, MegaType overallType) {
                this.input = input;
                this.expected = expected;
                this.actual = actual;
                this.overallType = overallType;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            //TODO: Implement error testing of boolean and/or
            // Boolean and/or
            // Pair.of("true && false", PrimitiveTypes.BOOLEAN),
            // Pair.of("true || false", PrimitiveTypes.BOOLEAN),

            // Inequalities
            new TestCase("\"a\" < 3", PrimitiveTypes.NUMBER, PrimitiveTypes.STRING, PrimitiveTypes.BOOLEAN),
            new TestCase("3 > \"a\"", PrimitiveTypes.NUMBER, PrimitiveTypes.STRING, PrimitiveTypes.BOOLEAN),
            new TestCase("[3, 4] <= 3", PrimitiveTypes.NUMBER, new ArrayType(PrimitiveTypes.INTEGER), PrimitiveTypes.BOOLEAN),
            new TestCase("3 >= [3, 4]", PrimitiveTypes.NUMBER, new ArrayType(PrimitiveTypes.INTEGER), PrimitiveTypes.BOOLEAN),

            new TestCase("'asdf' * 1.3", PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.STRING),
            new TestCase("'asdf' * [1.3]", PrimitiveTypes.INTEGER, new ArrayType(PrimitiveTypes.FLOAT), PrimitiveTypes.STRING),
            new TestCase("'asdf' * [1, 2]", PrimitiveTypes.INTEGER, new ArrayType(PrimitiveTypes.INTEGER), PrimitiveTypes.STRING),
            new TestCase("'asdf' * 'asdf'", PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.input);
                    assertEquals(testCase.overallType, result.node.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.expected, testCase.actual),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckIfExpression() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            // When given no else-block, if-expr is typed to Unit
            Pair.of("if true { 1 + 2 }", PrimitiveTypes.UNIT),
            Pair.of("if true { \"asdf\" + 2 }", PrimitiveTypes.UNIT),
            Pair.of("if true { let a = 1; 1 + 2 }", PrimitiveTypes.UNIT),

            Pair.of("if true { 1 } else { 2 }", PrimitiveTypes.INTEGER),
            Pair.of("if true { 1.2 } else { 2.3 }", PrimitiveTypes.FLOAT),
            Pair.of("if true { \"asdf\" } else { \"qwer\" }", PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckIfExpression_errors() {
        class TestCase {
            public final String input;
            public final MegaType expected;
            public final MegaType actual;
            public final MegaType overallType;

            public TestCase(String input, MegaType expected, MegaType actual, MegaType overallType) {
                this.input = input;
                this.expected = expected;
                this.actual = actual;
                this.overallType = overallType;
            }
        }
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("if 1 + 2 { 1 } else { 2 }", PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER),
            new TestCase("if true { 1.2 } else { 2 }", PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT),
            new TestCase("if true { 1 } else { 2.1 }", PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.input);
                    // Even though typechecking fails, there should still be some kind of overall type returned, even if it's <unknown>.
                    assertEquals(testCase.overallType, result.node.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.expected, testCase.actual),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }
}