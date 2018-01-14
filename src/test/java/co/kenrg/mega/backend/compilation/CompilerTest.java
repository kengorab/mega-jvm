package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticMethodsFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticVariableFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.List;
import java.util.stream.Stream;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import mega.lang.functions.Function1;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;

class CompilerTest {

//    @BeforeAll
    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

    @Nested
    class TestStaticVariables {

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
        List<DynamicTest> testLiteralDeclarations() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("val someInt = 123", "someInt", 123),
                Triple.of("val someFloat = 12.345", "someFloat", 12.345F),
                Triple.of("val someBool = true", "someBool", true),
                Triple.of("val someBool = false", "someBool", false),
                Triple.of("val someStr = 'string 1'", "someStr", "string 1"),
                Triple.of("val someStr = \"string 2\"", "someStr", "string 2")
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

                            assertStaticBindingOnClassEquals(className, bindingName, val);
                        }),
                        dynamicTest(varName, () -> {
                            TestCompilationResult result = parseTypecheckAndCompileInput(varInput);
                            String className = result.className;

                            assertStaticBindingOnClassEquals(className, bindingName, val);
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

                            assertStaticBindingOnClassEquals(className, bindingName, val);
                        }),
                        dynamicTest(varName, () -> {
                            TestCompilationResult result = parseTypecheckAndCompileInput(varInput);
                            String className = result.className;

                            assertStaticBindingOnClassEquals(className, bindingName, val);
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
                    });
                })
                .collect(toList());
        }

        @TestFactory
        List<DynamicTest> testArrowFunctionExpressions() {
            class TestCase {
                public final String input;
                public final String bindingName;
                public final Object[] args;
                public final Object result;

                public TestCase(String input, String bindingName, Object[] args, Object result) {
                    this.input = input;
                    this.bindingName = bindingName;
                    this.args = args;
                    this.result = result;
                }
            }

            List<TestCase> testCases = Lists.newArrayList(
//                new TestCase("val a = true; val someFn = () => a", "someFn", true), // TODO: Make closures work
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

                        assertStaticBindingOnClassIsLambdaAndEvaluatesTo(className, bindingName, args, res);
                    });
                })
                .collect(toList());
        }
    }

    @Nested
    class TestStaticMethods {

        @TestFactory
        List<DynamicTest> testFunctionDeclarationExpressions() {
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
                new TestCase("func returnsOne(): Int { 1 }", "returnsOne", new Object[]{}, 1),
                new TestCase("func addsOne(a: Int): Int { a + 1 }", "addsOne", new Object[]{2}, 3),
                new TestCase("func strConcatShout(a: String, b: String): String { a + b + '!' }", "strConcatShout", new Object[]{"Hello ", "world"}, "Hello world!"),
                new TestCase("func addAll(a: Int, b: Int, c: Int) { a + b + c }", "addAll", new Object[]{1, 2, 3}, 6),
                new TestCase("func applyToInt(fn: Int => String, a: Int): String { fn(a) }", "applyToInt",
                    new Object[]{(Function1<Integer, String>) input -> input + "!", 24},
                    "24!"
                )
            );

            return testCases.stream()
                .map(testCase -> {
                    String input = testCase.input;
                    String bindingName = testCase.bindingName;
                    Object[] args = testCase.args;
                    Object expectedResult = testCase.result;

                    String name = String.format("Compiling `%s` should result in the static method `%s`", input, bindingName);
                    return dynamicTest(name, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(input);
                        String className = result.className;

                        assertInvokingStaticMethodOnClassEvaluatesTo(className, bindingName, args, expectedResult);
                    });
                })
                .collect(toList());
        }
    }

    @Nested
    class TestFunctionInvocation {

        @TestFactory
        List<DynamicTest> testInvocationOfStaticArrowFunctions() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("val returnsOne = () => 1; val one = returnsOne()", "one", 1),
                Triple.of("val shout = (str: String) => str + '!'; val loudNoises = shout('shouting')", "loudNoises", "shouting!"),
                Triple.of("" +
                        "val addSpace = (str: String) => str + ' ';" +
                        "val shout = (str: String) => str + '!'; " +
                        "val helloWorld = shout(shout(addSpace('Hello') + 'world'))",
                    "helloWorld",
                    "Hello world!!"
                )
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
                    });
                })
                .collect(toList());
        }

        @TestFactory
        List<DynamicTest> testInvocationOfNestedArrowFunctions() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("" +
                        "val a = if 4 > 3 {" +
                        "  val fn = (i: Int) => i + 1;" +
                        "  fn(1)" +
                        "} else { 4 }",
                    "a",
                    2
                ),
                Triple.of("" +
                        "val apply = (fn: Int => Int, a: Int) => fn(a);" +
                        "val inc = (i: Int) => i + 1;" +
                        "val a = apply(inc, 3)",
                    "a",
                    4
                ),
                Triple.of("" +
                        "val apply = (fn: Int => Int, a: Int) => fn(a);" +
                        "val a = apply(i => i + 1, 3)",
                    "a",
                    4
                ),
                Triple.of("val a = ((i: Int) => i + 1)(2)", "a", 3),
//                Triple.of("val a = (i: Int) => (s: String) => s * i; val b = a(3)('abc')", "b", "abcabcabc"),// TODO: Uncomment when closures work
                Triple.of("val a = (i: Int) => (s: String) => s; val b = a(3)('abc')", "b", "abc"),
                Triple.of("val a = (i: Int) => (s: String) => (x: Bool) => x; val b = a(3)('abc')(true)", "b", true)
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
                    });
                })
                .collect(toList());
        }

        @TestFactory
        List<DynamicTest> testInvocationOfStaticMethods() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("" +
                        "func returnOne() { 1 };" +
                        "val one = returnOne();",
                    "one",
                    1
                ),
                Triple.of("" +
                        "func returnOne() { 1 };" +
                        "func addOne(a: Int) { a + returnOne() };" +
                        "val two = addOne(1);",
                    "two",
                    2
                ),
                Triple.of("" +
                        "func returnFn() { (a: Int) => a + 1 };" +
                        "val two = returnFn()(1);",
                    "two",
                    2
                )
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
                    });
                })
                .collect(toList());
        }

        @TestFactory
        List<DynamicTest> testInvocationOfStaticMethodReferences() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("" +
                        "func invoke(fn: () => Int) { fn() };" +
                        "func returnTwentyFour() { 24 };" +
                        "val twentyFour = invoke(returnTwentyFour);",
                    "twentyFour",
                    24
                ),
                Triple.of("" +
                        "func apply(fn: Int => Int, a: Int) { fn(a) };" +
                        "func addOne(a: Int) { a + 1 };" +
                        "val two = apply(addOne, 1);",
                    "two",
                    2
                ),
                Triple.of("" +
                        "func apply(fn: (Int, String) => String, a: Int, b: String) { fn(a, b) };" +
                        "func repeat(num: Int, str: String) { num * str };" +
                        "val str = apply(repeat, 5, 'abc');",
                    "str",
                    "abcabcabcabcabc"
                )
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

                        assertStaticBindingOnClassEquals(className, bindingName, val);
                    });
                })
                .collect(toList());
        }
    }

    private void assertFinalityOnStaticBinding(String className, String fieldName, boolean expectedFinal) {
        Field field = loadStaticVariableFromClass(className, fieldName);
        if (expectedFinal) {
            assertTrue(Modifier.isFinal(field.getModifiers()), "Field " + fieldName + " is immutable and should be final");
        } else {
            assertFalse(Modifier.isFinal(field.getModifiers()), "Field " + fieldName + " is mutable and should not be final");
        }
    }

    private void assertStaticBindingOnClassEquals(String className, String staticFieldName, Object value) {
        if (value instanceof Integer) {
            int variable = (int) loadStaticValueFromClass(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof Float) {
            float variable = (float) loadStaticValueFromClass(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof Boolean) {
            boolean variable = (boolean) loadStaticValueFromClass(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof String) {
            String variable = (String) loadStaticValueFromClass(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof Object[]) {
            Object[] variable = (Object[]) loadStaticValueFromClass(className, staticFieldName);
            assertArrayEquals((Object[]) value, variable, "The static value read off the generated class should be as expected");
        }
    }

    private void assertStaticBindingOnClassIsLambdaAndEvaluatesTo(String className, String staticFieldName, Object[] args, Object res) {
        try {
            Object variable = loadStaticValueFromClass(className, staticFieldName);
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

    private void assertInvokingStaticMethodOnClassEvaluatesTo(String className, String name, Object[] args, Object res) {
        List<Method> potentialMethods = loadStaticMethodsFromClass(className, name);
        long numMatches = potentialMethods.stream()
            .filter(method -> method.getParameterCount() == args.length)
            .count();
        if (numMatches == 0) {
            fail("No static method on class matching name " + name + " and arity " + args.length);
            return;
        } else if (numMatches > 1) {
            fail("Too many static methods on class matching name " + name + " and arity " + args.length);
            return;
        }

        Method method = potentialMethods.get(0);
        String arguments = Arrays.toString(args);
        try {
            Object result = method.invoke(null, args);
            assertEquals(res, result, "The result of invoking the static method " + name + " with arguments " + arguments + " should result in " + res);
        } catch (IllegalAccessException | InvocationTargetException e) {
            fail("Invoking static method " + name + " with arguments " + arguments + " failed", e);
            e.printStackTrace();
        }
    }
}
