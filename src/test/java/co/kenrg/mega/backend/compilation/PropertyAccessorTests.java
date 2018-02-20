package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadPrivateStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class PropertyAccessorTests {

    @BeforeAll
//    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

    @TestFactory
    List<DynamicTest> testCustomTypePropertyAccessors() {
        class TestCase {
            private final String input;
            private final Object expectedValue;
            private final String valName;

            public TestCase(String input, Object expectedValue, String valName) {
                this.input = input;
                this.expectedValue = expectedValue;
                this.valName = valName;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "type Person = { name: String, age: Int }\n" +
                    "val p = Person(name: 'Ken', age: 26)\n" +
                    "val n = p.name",
                "Ken",
                "n"
            ),
            new TestCase(
                "type Person = { name: String, age: Int }\n" +
                    "val p = Person(name: 'Ken', age: 26)\n" +
                    "val a = p.age",
                26,
                "a"
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "Compiling and evaluating `%s` should result in the binding %s, whose value is `%s`",
                    testCase.input, testCase.valName, testCase.expectedValue
                );

                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(testCase.input);
                    Object val = loadPrivateStaticValueFromClass(result.className, testCase.valName);

                    assertEquals(testCase.expectedValue, val);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testBuiltinTypePropertyAccessors() {
        class TestCase {
            private final String input;
            private final Object expectedValue;
            private final String valName;

            public TestCase(String input, Object expectedValue, String valName) {
                this.input = input;
                this.expectedValue = expectedValue;
                this.valName = valName;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "val s = 'substring'.substring(1)",
                "ubstring",
                "s"
            ),
            new TestCase(
                "val i = 'substring'.length().floatValue().intValue()",
                9,
                "i"
            ),
            new TestCase(
                "val b = true.toString()",
                "true",
                "b"
            )
//            new TestCase( // TODO: Uncomment this when typechecking/compilation of polymorphic methods is supported
//                "val s = 'substring'.substring(2, 5)",
//                "str",
//                "s"
//            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "Compiling and evaluating `%s` should result in the binding %s, whose value is `%s`",
                    testCase.input, testCase.valName, testCase.expectedValue
                );

                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(testCase.input);
                    Object val = loadPrivateStaticValueFromClass(result.className, testCase.valName);

                    assertEquals(testCase.expectedValue, val);
                });
            })
            .collect(toList());
    }
}
