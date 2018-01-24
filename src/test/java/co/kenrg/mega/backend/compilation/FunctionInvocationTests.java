package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class FunctionInvocationTests {

    //    @BeforeAll
    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

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
            ),

            // With named parameters
            Triple.of("val shout = (str: String) => str + '!'; val loudNoises = shout(str: 'shouting')", "loudNoises", "shouting!")
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
            Triple.of("val a = (i: Int) => (s: String) => s; val b = a(3)('abc')", "b", "abc"),
            Triple.of("val a = (i: Int) => (s: String) => (x: Bool) => x; val b = a(3)('abc')(true)", "b", true),

            // With named parameters
            Triple.of("val a = ((i: Int) => i + 1)(i: 2)", "a", 3),
            Triple.of("val a = (i: Int) => (s: String) => s; val b = a(i: 3)(s: 'abc')", "b", "abc"),
            Triple.of("val a = (i: Int) => (s: String) => (x: Bool) => x; val b = a(i: 3)(s: 'abc')(x: true)", "b", true)
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
            ),

            // With named parameters
            Triple.of("" +
                    "func returnFn() { (a: Int) => a + 1 };" +
                    "val two = returnFn()(a: 1);",
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
            ),
            Triple.of("" +
                    "func apply(fn: (Int, String) => String, a: Int, b: String) { fn(a, b) };" +
                    "func repeat(num: Int, str: String) { num * str };" +
                    "val str1 = apply(repeat, 5, 'abc');" +
                    "val str2 = apply(repeat, 4, 'aaa');",
                "str1",
                "abcabcabcabcabc"
            ),

            // With named parameters
            Triple.of("" +
                    "func apply(fn: (Int, String) => String, a: Int, b: String) { fn(a, b) };" +
                    "func repeat(num: Int, str: String) { num * str };" +
                    "val str1 = apply(fn: repeat, a: 5, b: 'abc');" +
                    "val str2 = apply(fn: repeat, a: 4, b: 'aaa');",
                "str1",
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

    @TestFactory
    List<DynamicTest> testInvocationOfArrowFunctionsWithClosures() {
        List<Triple<String, String, Object>> testCases = Lists.newArrayList(
            Triple.of("val a = (i: Int) => (s: String) => s * i; val b = a(3)('abc')", "b", "abcabcabc"),
            Triple.of("" +
                    "func createAdder(a: Int) { (i: Int) => a + i };" + // createAdder closes over a
                    "val adder = createAdder(5);" +
                    "val two = adder(-3)",
                "two",
                2
            ),
            Triple.of("" +
                    "func inc(a: Int) { a + 1 };" +
                    "func apply(fn: Int => Int, a: Int) { fn(a) };" +
                    "func applyInc(i: Int) { apply(inc, i) };" +
                    "val two = applyInc(1)",
                "two",
                2
            ),

            // With named parameters
            Triple.of("" +
                    "func inc(a: Int) { a + 1 };" +
                    "func apply(fn: Int => Int, a: Int) { fn(a) };" +
                    "func applyInc(i: Int) { apply(fn: inc, a: i) };" +
                    "val two = applyInc(i: 1)",
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
}
