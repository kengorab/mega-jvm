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
                Triple.of("val someStr = \"string 2\"", "someStr", "string 2")
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
                Triple.of("val someBool = !false", "someBool", true)
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
