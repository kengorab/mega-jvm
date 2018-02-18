package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.assertStaticBindingOnClassEquals;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadPrivateStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticVariableFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class StaticVariableTests {

    //    @BeforeAll
    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

    @TestFactory
    List<DynamicTest> testValVsVarDeclarations() {
        List<Triple<String, String, Boolean>> testCases = Lists.newArrayList(
            Triple.of("val someInt = 123", "someInt", true),
            Triple.of("var someInt = 123", "someInt", false)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                boolean isImmutable = testCase.getRight();

                String name = String.format("Compiling `%s` should result in the static %svariable `%s`", input, isImmutable ? "final " : "", bindingName);
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertFinalityOnStaticBinding(className, bindingName, isImmutable);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testExportedVsNonExportedValAndVarDeclarations() {
        List<Triple<String, String, Boolean>> testCases = Lists.newArrayList(
            Triple.of("export val someInt1 = 123", "someInt1", true),
            Triple.of("val someInt2 = 123", "someInt2", false),
            Triple.of("export var someInt3 = 123", "someInt3", true),
            Triple.of("var someInt4 = 123", "someInt4", false)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                boolean isExported = testCase.getRight();

                String name = String.format("Compiling `%s` should result in the static %s variable `%s`", input, isExported ? "public" : "private", bindingName);
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertVisibilityOnStaticBinding(className, bindingName, isExported);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testLiteralDeclarations() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            Triple.of("val someInt = 123", "someInt", 123),
            Triple.of("val someFloat = 12.345", "someFloat", 12.345F),
            Triple.of("val someBool = true", "someBool", true),
            Triple.of("val someBool = false", "someBool", false),
            Triple.of("val someStr = 'string 1'", "someStr", "string 1"),
            Triple.of("val someStr = \"string 2\"", "someStr", "string 2"),
            Triple.of("val someMap = { name: 'Ken', age: 26 }", "someMap", new HashMap<String, Object>() {{
                put("name", "Ken");
                put("age", 26);
            }})
        );

        return testCases.stream()
            .flatMap(testCase -> {
                String valInput = testCase.getLeft();
                String varInput = valInput.replace("val", "var");

                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String valName = "Compiling `" + valInput + "` should result in the static variable `" + bindingName + "` = " + val;
                String varName = "Compiling `" + varInput + "` should result in the static variable `" + bindingName + "` = " + val;
                return Stream.of(
                    dynamicTest(valName, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(valInput);
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, bindingName, val, true);
                    }),
                    dynamicTest(varName, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(varInput);
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, bindingName, val, true);
                    })
                );
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testArrayLiteralDeclarations() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            // Basic arrays
            Triple.of("val someIntArray = [123, 456, 789]", "someIntArray", new Integer[]{123, 456, 789}),
            Triple.of("val someFloatArray = [12.3, 45.6, 78.9]", "someFloatArray", new Float[]{12.3F, 45.6F, 78.9F}),
            Triple.of("val someBoolArray = [true, false, false, true]", "someBoolArray", new Boolean[]{true, false, false, true}),
            Triple.of("val someStrArray = ['asdf', 'qwer', 'zxcv']", "someStrArray", new String[]{"asdf", "qwer", "zxcv"}),

            // Nested arrays
            Triple.of("val someIntArrayArray = [[0, 0], [1, 0], [0, 1]]", "someIntArrayArray", new Integer[][]{{0, 0}, {1, 0}, {0, 1}}),
            Triple.of("val someFloatArrayArray = [[0.0, 0.0], [1.0, 0.0], [0.0, 1.0]]", "someFloatArrayArray", new Float[][]{{0.0F, 0.0F}, {1.0F, 0.0F}, {0.0F, 1.0F}}),
            Triple.of("val someBoolArrayArray = [[true, false], [false, true], [true, true]]", "someBoolArrayArray", new Boolean[][]{{true, false}, {false, true}, {true, true}}),
            Triple.of("val someStrArrayArray = [['a', 'A'], ['b', 'B'], ['c', 'C']]", "someStrArrayArray", new String[][]{{"a", "A"}, {"b", "B"}, {"c", "C"}})
        );

        return testCases.stream()
            .flatMap(testCase -> {
                String valInput = testCase.getLeft();
                String varInput = valInput.replace("val", "var");

                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String valName = "Compiling `" + valInput + "` should result in the static variable `" + bindingName + "` = " + val;
                String varName = "Compiling `" + varInput + "` should result in the static variable `" + bindingName + "` = " + val;
                return Stream.of(
                    dynamicTest(valName, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(valInput);
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, bindingName, val, true);
                    }),
                    dynamicTest(varName, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(varInput);
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, bindingName, val, true);
                    })
                );
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testArrayIndexExpressions() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            Triple.of("val someInt = [1, 2, 3][0]", "someInt", 1),
            Triple.of("val someBool = [true, false, true][1]", "someBool", false),
            Triple.of("val someFloat = [1.23, 4.56, 7.89][2]", "someFloat", 7.89F),
            Triple.of("val someString = ['abc', 'def', 'ghi'][1]", "someString", "def"),

            Triple.of("val idx = 0; val someString = ['abc', 'def', 'ghi'][idx]", "someString", "abc"),
            Triple.of("val idx = 0; val strings = ['a', 'b', 'c']; val someString = strings[idx]", "someString", "a")
//                Triple.of("val idx = 6; val strings = ['a', 'b', 'c']; val someOptString = strings[idx]", "someString", Optional.none()) // TODO: Make this work
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String name = "Compiling `" + input + "` should result in the static variable `" + bindingName + "` = " + val;
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertStaticBindingOnClassEquals(className, bindingName, val, true);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testPrefixExpressions() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            Triple.of("val someInt = -123", "someInt", -123),
            Triple.of("val someFloat = -12.345", "someFloat", -12.345F),
            Triple.of("val someBool = !true", "someBool", false),
            Triple.of("val someBool = !false", "someBool", true)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String name = "Compiling `" + input + "` should result in the static variable `" + bindingName + "` = " + val;
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertStaticBindingOnClassEquals(className, bindingName, val, true);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testInfixExpressions() {
        List<Triple<String, String, Object>> numericalTestCases = Lists.newArrayList(
            // Addition
            Triple.of("val someInt = 123 + 456", "someInt", 579),
            Triple.of("val someInt = 123 + -456", "someInt", -333),
            Triple.of("val someFloat = 2.5 + 1", "someFloat", 3.5F),
            Triple.of("val someFloat = 1 + 2.5", "someFloat", 3.5F),
            Triple.of("val someFloat = 1.5 + 2.5", "someFloat", 4.0F),

            // Subtraction
            Triple.of("val someInt = 123 - 456", "someInt", -333),
            Triple.of("val someInt = 123 - -456", "someInt", 579),
            Triple.of("val someFloat = 2.5 - 1", "someFloat", 1.5F),
            Triple.of("val someFloat = 1 - 2.5", "someFloat", -1.5F),
            Triple.of("val someFloat = 1.5 - 2.5", "someFloat", -1.0F),

            // Multiplication
            Triple.of("val someInt = 2 * 3", "someInt", 6),
            Triple.of("val someInt = 2 * -3", "someInt", -6),
            Triple.of("val someFloat = 2.5 * 1.5", "someFloat", 3.75F),
            Triple.of("val someFloat = 1.5 * -2.5", "someFloat", -3.75F),
            Triple.of("val someFloat = 4 * 2.5", "someFloat", 10.0F),

            // Division
            Triple.of("val someInt = 2 / 3", "someInt", 0.66667),
            Triple.of("val someInt = 2 / -3", "someInt", -0.66667),
            Triple.of("val someInt = 4 / 2", "someInt", 2),
            Triple.of("val someFloat = 4 / 2.0", "someFloat", 2.0F),
            Triple.of("val someFloat = 1.5 / -2", "someFloat", -0.75F),
            Triple.of("val someFloat = 4 / 1.25", "someFloat", 3.2F)
        );

        List<Triple<String, String, Object>> booleanTestCases = Lists.newArrayList(
            // Conditional AND (&&)
            Triple.of("val someBool = true && false", "someBool", false),
            Triple.of("val someBool = true && !false", "someBool", true),
            Triple.of("val someBool = !true && !false", "someBool", false),
            Triple.of("val someBool = false && false", "someBool", false),

            // Conditional OR (||)
            Triple.of("val someBool = true || false", "someBool", true),
            Triple.of("val someBool = true || !false", "someBool", true),
            Triple.of("val someBool = !true || !false", "someBool", true),
            Triple.of("val someBool = false || false", "someBool", false)
        );

        List<Triple<String, String, Object>> stringTestCases = Lists.newArrayList(
            // String concatenation
            Triple.of("val someStr = 'abc' + 'def'", "someStr", "abcdef"),
            Triple.of("val someStr = 'abc' + 'def' + 'ghi'", "someStr", "abcdefghi"),
            Triple.of("val someStr = 'abc' + 123", "someStr", "abc123"),
            Triple.of("val someStr = 12.3 + 'abc' + 123", "someStr", "12.3abc123"),
            Triple.of("val someStr = [1, 2, 3] + 'abc'", "someStr", "[1, 2, 3]abc"),
            Triple.of("val someStr = if true { val x = [1, 2, 3]; x + 'abc' } else { '' }", "someStr", "[1, 2, 3]abc"),
            Triple.of("val someStr = if true { val x = [[1, 2], [3, 4]]; x + 'abc' } else { '' }", "someStr", "[[1, 2], [3, 4]]abc"),

            // String repetition
            Triple.of("val someStr = 'asdf' * 4", "someStr", "asdfasdfasdfasdf"),
            Triple.of("val someStr = 2 * 'qwer'", "someStr", "qwerqwer"),
            Triple.of("val someStr = 'qwer' * 2 * 2", "someStr", "qwerqwerqwerqwer"),
            Triple.of("val someStr = 2 * 'qwer' * 2", "someStr", "qwerqwerqwerqwer")
        );

        List<Triple<String, String, Object>> comparisonTestCases = Lists.newArrayList(
            // Integer comparison (<, <=, >, >=, ==, !=)
            Triple.of("val someBool = 123 < 456", "someBool", true),
            Triple.of("val someBool = 123 <= 456", "someBool", true),
            Triple.of("val someBool = 123 > 456", "someBool", false),
            Triple.of("val someBool = 123 >= 456", "someBool", false),
            Triple.of("val someBool = 123 == 456", "someBool", false),
            Triple.of("val someBool = 123 != 456", "someBool", true),

            // Float comparison (<, <=, >, >=, ==, !=)
            Triple.of("val someBool = 12.3 < 45.6", "someBool", true),
            Triple.of("val someBool = 12 < 45.6", "someBool", true),
            Triple.of("val someBool = 12.3 < 45", "someBool", true),
            Triple.of("val someBool = 12.3 <= 45.6", "someBool", true),
            Triple.of("val someBool = 12 <= 45.6", "someBool", true),
            Triple.of("val someBool = 12.3 <= 45", "someBool", true),
            Triple.of("val someBool = 12.3 > 45.6", "someBool", false),
            Triple.of("val someBool = 12 > 45.6", "someBool", false),
            Triple.of("val someBool = 12.3 > 45", "someBool", false),
            Triple.of("val someBool = 12.3 >= 45.6", "someBool", false),
            Triple.of("val someBool = 12 >= 45.6", "someBool", false),
            Triple.of("val someBool = 12.3 >= 45", "someBool", false),
            Triple.of("val someBool = 12.3 == 45.6", "someBool", false),
            Triple.of("val someBool = 12 == 45.6", "someBool", false),
            Triple.of("val someBool = 12.3 == 45", "someBool", false),
            Triple.of("val someBool = 12.0 == 12", "someBool", true),
            Triple.of("val someBool = 12.3 != 45.6", "someBool", true),
            Triple.of("val someBool = 12 != 45.6", "someBool", true),
            Triple.of("val someBool = 12.3 != 45", "someBool", true),
            Triple.of("val someBool = 12.0 != 12", "someBool", false),

            // Boolean comparison (<, <=, >, >=, ==, !=)
            Triple.of("val someBool = true < false", "someBool", false),
            Triple.of("val someBool = true <= false", "someBool", false),
            Triple.of("val someBool = true > false", "someBool", true),
            Triple.of("val someBool = true >= false", "someBool", true),
            Triple.of("val someBool = true == false", "someBool", false),
            Triple.of("val someBool = true != false", "someBool", true),

            // Comparable (just Strings, at the moment) comparison (<, <=, >, >=, ==, !=)
            Triple.of("val someBool = 'a' < 'b'", "someBool", true),
            Triple.of("val someBool = 'ab' <= 'ab'", "someBool", true),
            Triple.of("val someBool = 'abc' > 'ABC'", "someBool", true),
            Triple.of("val someBool = 'abc' >= 'ABC'", "someBool", true),
            Triple.of("val someBool = 'abc' == 'abc'", "someBool", true),
            Triple.of("val someBool = 'abc' != 'abc'", "someBool", false)
            //Triple.of("val someBool = 'abc' == 1", "someBool", false), // TODO: Make this work
        );

        List<Triple<String, String, Object>> testCases = Lists.newArrayList();
        testCases.addAll(numericalTestCases);
        testCases.addAll(booleanTestCases);
        testCases.addAll(stringTestCases);
        testCases.addAll(comparisonTestCases);

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String name = "Compiling `" + input + "` should result in the static variable `" + bindingName + "` = " + val;
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertStaticBindingOnClassEquals(className, bindingName, val, true);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testIfExpressions() { // TODO: Testing if's without else's
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
//                Triple.of("val someInt = if 1 < 2 { 123 }", "someInt", 123) // TODO: This should probably be an optional type (someInt: Int?)
            Triple.of("val someInt = if 1 < 2 { 123 } else { -123 }", "someInt", 123),
            Triple.of("val someInt = if 1 < 2 { if 1 < 2 { 123 } else { 456 } } else { -123 }", "someInt", 123),
            Triple.of("val someInt = if 1 < 2 { val x = if 1 < 2 { 123 } else { 456 }; x } else { -123 }", "someInt", 123),
            Triple.of("val someInt = if 2 < 1 { 123 } else { -123 }", "someInt", -123),

            // TODO: Clean this up when function compilation exists. Right now there's no way to compile multiple statements in a single scope aside from an if-expr.
            Triple.of("val someInt = if 1 < 2 { for x in [1, 2, 3] { val a = x + 1 }; 123 } else { -123 }", "someInt", 123),
            Triple.of("val someInt = if 1 < 2 { val e = 3; for x in [1, 2, 3] { val a = x + 1 }; val f = 123; f } else { -123 }", "someInt", 123),
            Triple.of("val someInt = if 1 < 2 { var e = 3; e = 123; e } else { -123 }", "someInt", 123)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String name = "Compiling `" + input + "` should result in the static variable `" + bindingName + "` = " + val;
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertStaticBindingOnClassEquals(className, bindingName, val, true);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testRangeExpressions() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            Triple.of("val someRange = 1..4", "someRange", new Integer[]{1, 2, 3}),
            Triple.of("val lBound = 3; val rBound = 7; val someRange = lBound..rBound", "someRange", new Integer[]{3, 4, 5, 6})
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String bindingName = testCase.getMiddle();
                Object val = testCase.getRight();

                String name = "Compiling `" + input + "` should result in the static variable `" + bindingName + "` = " + val;
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertStaticBindingOnClassEquals(className, bindingName, val, true);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testArrowFunctionExpressions() {
        class TestCase {
            private final String input;
            private final String bindingName;
            private final Object[] args;
            private final Object result;

            private TestCase(String input, String bindingName, Object[] args, Object result) {
                this.input = input;
                this.bindingName = bindingName;
                this.args = args;
                this.result = result;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("val a = true; val someFn = () => a", "someFn", new Object[]{}, true),
            new TestCase("val someFn = () => true", "someFn", new Object[]{}, true),
            new TestCase("val someFn = (i: Int) => i + '!'", "someFn", new Object[]{1}, "1!"),
            new TestCase("val someFn = (i: Int) => if i > 3 { i + 0.14 } else { i - 0.14 }", "someFn", new Object[]{1}, 0.86F),
            new TestCase("val someFn = (a: Int, b: Int) => a + b", "someFn", new Object[]{1, 2}, 3),
            new TestCase("val someFn = (a: Int, b: Bool) => if a > 3 { b } else { !b }", "someFn", new Object[]{2, true}, false)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.input;
                String bindingName = testCase.bindingName;
                Object[] args = testCase.args;
                Object res = testCase.result;

                String name = "Compiling `" + input + "` should result in the static variable `" + bindingName + " which evaluates to " + res + " when passed " + Arrays.toString(args);
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(input);
                    String className = result.className;

                    assertPrivateStaticBindingOnClassIsLambdaAndEvaluatesTo(className, bindingName, args, res);
                });
            })
            .collect(toList());
    }

    private void assertFinalityOnStaticBinding(String className, String fieldName, boolean expectedFinal) {
        Field field = loadStaticVariableFromClass(className, fieldName);
        if (expectedFinal) {
            assertTrue(Modifier.isFinal(field.getModifiers()), "Field " + fieldName + " is immutable and should be final");
        } else {
            assertFalse(Modifier.isFinal(field.getModifiers()), "Field " + fieldName + " is mutable and should not be final");
        }
    }

    private void assertVisibilityOnStaticBinding(String className, String fieldName, boolean expectedVisible) {
        Field field = loadStaticVariableFromClass(className, fieldName);
        if (expectedVisible) {
            assertTrue(Modifier.isPublic(field.getModifiers()), "Field " + fieldName + " is exported and should be public");
        } else {
            assertTrue(Modifier.isPrivate(field.getModifiers()), "Field " + fieldName + " is not exported and should be private");
        }
    }

    private void assertPrivateStaticBindingOnClassIsLambdaAndEvaluatesTo(String className, String staticFieldName, Object[] args, Object res) {
        try {
            Object variable = loadPrivateStaticValueFromClass(className, staticFieldName);
            Method invokeMethod = Arrays.stream(variable.getClass().getMethods())
                .filter(method -> method.getName().equals("invoke"))
                .collect(toList())
                .get(0);
            String message = "The result of invoking the lambda with " + Arrays.toString(args) + " should be " + res;
            assertEquals(res, invokeMethod.invoke(variable, args), message);
        } catch (IllegalAccessException | InvocationTargetException e) {
            fail("Functions with arity " + args.length + " have not been handled yet", e);
        }
    }
}
