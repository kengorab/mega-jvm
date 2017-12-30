package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticVariableFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;

class CompilerTest {

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
        List<DynamicTest> testPrefixExpressions() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("val someInt = -123", "someInt", -123),
                Triple.of("val someFloat = -12.345", "someFloat", -12.345F),
                Triple.of("val someBool = !true", "someBool", false),
                Triple.of("val someBool = !false", "someBool", true)
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
        List<DynamicTest> testIfExpressions() { // TODO: Testing if's without else's
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
//                Triple.of("val someInt = if 1 < 2 { 123 }", "someInt", 123) // TODO: This should probably be an optional type (someInt: Int?)
                Triple.of("val someInt = if 1 < 2 { 123 } else { -123 }", "someInt", 123),
                Triple.of("val someInt = if 1 < 2 { if 1 < 2 { 123 } else { 456 } } else { -123 }", "someInt", 123),
                Triple.of("val someInt = if 1 < 2 { val x = if 1 < 2 { 123 } else { 456 }; x } else { -123 }", "someInt", 123),
                Triple.of("val someInt = if 2 < 1 { 123 } else { -123 }", "someInt", -123)
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
//            } else if (value instanceof Integer[]) {
//                Integer[] variable = (Integer[]) loadStaticValueFromClass(className, staticFieldName);
//                assertArrayEquals((Integer[]) value, variable, "The static value read off the generated class should be as expected");
            } else if (value instanceof Float) {
                float variable = (float) loadStaticValueFromClass(className, staticFieldName);
                assertEquals(value, variable, "The static value read off the generated class should be as expected");
//            } else if (value instanceof Float[]) {
//                Float[] variable = (Float[]) loadStaticValueFromClass(className, staticFieldName);
//                assertArrayEquals((Float[]) value, variable, "The static value read off the generated class should be as expected");
            } else if (value instanceof Boolean) {
                boolean variable = (boolean) loadStaticValueFromClass(className, staticFieldName);
                assertEquals(value, variable, "The static value read off the generated class should be as expected");
//            } else if (value instanceof Boolean[]) {
//                Boolean[] variable = (Boolean[]) loadStaticValueFromClass(className, staticFieldName);
//                assertArrayEquals((Boolean[]) value, variable, "The static value read off the generated class should be as expected");
            } else if (value instanceof String) {
                String variable = (String) loadStaticValueFromClass(className, staticFieldName);
                assertEquals(value, variable, "The static value read off the generated class should be as expected");
//            } else if (value instanceof String[]) {
//                String[] variable = (String[]) loadStaticValueFromClass(className, staticFieldName);
//                assertArrayEquals((String[]) value, variable, "The static value read off the generated class should be as expected");
            } else if (value instanceof Object[]) {
                Object[] variable = (Object[]) loadStaticValueFromClass(className, staticFieldName);
                assertArrayEquals((Object[]) value, variable, "The static value read off the generated class should be as expected");
            }
        }
    }
}
