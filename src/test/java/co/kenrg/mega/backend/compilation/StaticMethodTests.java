package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticMethodsFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import mega.lang.functions.Function1;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class StaticMethodTests {

    //    @BeforeAll
    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

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

                    assertInvokingPrivateStaticMethodOnClassEvaluatesTo(className, bindingName, args, expectedResult);
                });
            })
            .collect(toList());
    }

    private void assertInvokingPrivateStaticMethodOnClassEvaluatesTo(String className, String name, Object[] args, Object res) {
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

        if (!Modifier.isPrivate(method.getModifiers())) {
            fail("Method " + name + " on class " + className + " was not private");
        }

        method.setAccessible(true);

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

