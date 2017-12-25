package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.cleanupClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.Path;
import java.util.List;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;

class CompilerTest {

    @Nested
    class TestStaticVariables {

        @TestFactory
        List<DynamicTest> testLiteralDeclarations() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                Triple.of("val someInt = 123", "someInt", 123),
                Triple.of("val someFloat = 12.345", "someFloat", 12.345F),
                Triple.of("val someBool = true", "someBool", true),
                Triple.of("val someBool = false", "someBool", false),
                Triple.of("val someStr = 'string 1'", "someStr", "string 1"),
                Triple.of("val someStr = \"string 2\"", "someStr", "string 2"),

                Triple.of("var someInt = 123", "someInt", 123),
                Triple.of("var someFloat = 12.345", "someFloat", 12.345F),
                Triple.of("var someBool = true", "someBool", true),
                Triple.of("var someBool = false", "someBool", false),
                Triple.of("var someStr = 'string 1'", "someStr", "string 1"),
                Triple.of("var someStr = \"string 2\"", "someStr", "string 2")
            );

            return testCases.stream()
                .map(testCase -> {
                    String input = testCase.getLeft();
                    String varName = testCase.getMiddle();
                    Object val = testCase.getRight();
                    String name = "Compiling `" + input + "` should result in the static variable `" + varName + "` = " + val;

                    return dynamicTest(name, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(input);
                        List<Path> classFiles = result.classFiles;
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, varName, val);
                        cleanupClassFiles(classFiles);
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
                Triple.of("val someBool = !false", "someBool", true),

                Triple.of("var someInt = -123", "someInt", -123),
                Triple.of("var someFloat = -12.345", "someFloat", -12.345F),
                Triple.of("var someBool = !true", "someBool", false),
                Triple.of("var someBool = !false", "someBool", true)
            );

            return testCases.stream()
                .map(testCase -> {
                    String input = testCase.getLeft();
                    String varName = testCase.getMiddle();
                    Object val = testCase.getRight();
                    String name = "Compiling `" + input + "` should result in the static variable `" + varName + "` = " + val;

                    return dynamicTest(name, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(input);
                        List<Path> classFiles = result.classFiles;
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, varName, val);
                        cleanupClassFiles(classFiles);
                    });
                })
                .collect(toList());
        }

        @TestFactory
        List<DynamicTest> testInfixExpressions() {
            List<Triple<String, String, Object>> testCases = Lists.newArrayList(
                // Addition
                Triple.of("val someInt = 123 + 456", "someInt", 579),
                Triple.of("val someInt = 123 + -456", "someInt", -333),
                Triple.of("val someFloat = 2.5 + 1", "someFloat", 3.5F),
                Triple.of("val someFloat = 1 + 2.5", "someFloat", 3.5F),
                Triple.of("val someFloat = 1.5 + 2.5", "someFloat", 4.0F),

                Triple.of("var someInt = 123 + 456", "someInt", 579),
                Triple.of("var someInt = 123 + -456", "someInt", -333),
                Triple.of("var someFloat = 2.5 + 1", "someFloat", 3.5F),
                Triple.of("var someFloat = 1 + 2.5", "someFloat", 3.5F),
                Triple.of("var someFloat = 1.5 + 2.5", "someFloat", 4.0F),

                // Subtraction
                Triple.of("val someInt = 123 - 456", "someInt", -333),
                Triple.of("val someInt = 123 - -456", "someInt", 579),
                Triple.of("val someFloat = 2.5 - 1", "someFloat", 1.5F),
                Triple.of("val someFloat = 1 - 2.5", "someFloat", -1.5F),
                Triple.of("val someFloat = 1.5 - 2.5", "someFloat", -1.0F),

                Triple.of("var someInt = 123 - 456", "someInt", -333),
                Triple.of("var someInt = 123 - -456", "someInt", 579),
                Triple.of("var someFloat = 2.5 - 1", "someFloat", 1.5F),
                Triple.of("var someFloat = 1 - 2.5", "someFloat", -1.5F),
                Triple.of("var someFloat = 1.5 - 2.5", "someFloat", -1.0F),

                // Multiplication
                Triple.of("val someInt = 2 * 3", "someInt", 6),
                Triple.of("val someInt = 2 * -3", "someInt", -6),
                Triple.of("val someFloat = 2.5 * 1.5", "someFloat", 3.75F),
                Triple.of("val someFloat = 1.5 * -2.5", "someFloat", -3.75F),
                Triple.of("val someFloat = 4 * 2.5", "someFloat", 10.0F),

                Triple.of("var someInt = 2 * 3", "someInt", 6),
                Triple.of("var someInt = 2 * -3", "someInt", -6),
                Triple.of("var someFloat = 2.5 * 1.5", "someFloat", 3.75F),
                Triple.of("var someFloat = 1.5 * -2.5", "someFloat", -3.75F),
                Triple.of("var someFloat = 4 * 2.5", "someFloat", 10.0F),

                // Division
                Triple.of("val someInt = 2 / 3", "someInt", 0.66667),
                Triple.of("val someInt = 2 / -3", "someInt", -0.66667),
                Triple.of("val someInt = 4 / 2", "someInt", 2),
                Triple.of("val someFloat = 4 / 2.0", "someFloat", 2.0F),
                Triple.of("val someFloat = 1.5 / -2", "someFloat", -0.75F),
                Triple.of("val someFloat = 4 / 1.25", "someFloat", 3.2F),

                Triple.of("var someInt = 2 / 3", "someInt", 0.66667),
                Triple.of("var someInt = 2 / -3", "someInt", -0.66667),
                Triple.of("var someInt = 4 / 2", "someInt", 2),
                Triple.of("var someFloat = 4 / 2.0", "someFloat", 2.0F),
                Triple.of("var someFloat = 1.5 / -2", "someFloat", -0.75F),
                Triple.of("var someFloat = 4 / 1.25", "someFloat", 3.2F)
            );

            return testCases.stream()
                .map(testCase -> {
                    String input = testCase.getLeft();
                    String varName = testCase.getMiddle();
                    Object val = testCase.getRight();
                    String name = "Compiling `" + input + "` should result in the static variable `" + varName + "` = " + val;

                    return dynamicTest(name, () -> {
                        TestCompilationResult result = parseTypecheckAndCompileInput(input);
                        List<Path> classFiles = result.classFiles;
                        String className = result.className;

                        assertStaticBindingOnClassEquals(className, varName, val);
                        cleanupClassFiles(classFiles);
                    });
                })
                .collect(toList());
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
            }
        }
    }
}
