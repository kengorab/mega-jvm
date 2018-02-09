package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.getInnerClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadPrivateStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class TypeDeclarationTests {

//    @BeforeAll
    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

    @TestFactory
    List<DynamicTest> testCustomTypeVisibility() {
        String typeName = "Person";
        List<Pair<String, Boolean>> testCases = Lists.newArrayList(
            Pair.of("type Person = { name: String }", false),
            Pair.of("export type Person = { name: String }", true)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "The type defined by `%s` should be %s",
                    testCase.getLeft(), testCase.getRight() ? "public" : "private"
                );
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(testCase.getLeft());
                    Class innerClass = getInnerClass(result.className, typeName);

                    if (testCase.getRight()) {
                        assertTrue(Modifier.isPublic(innerClass.getModifiers()), "The type " + typeName + " should be public");
                    } else {
                        assertTrue(Modifier.isPrivate(innerClass.getModifiers()), "The type " + typeName + " should be private");
                    }
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testCustomTypeToString() {
        class TestCase {
            private final String input;
            private final Object[] args;
            private final String toStringOutput;

            private TestCase(String input, Object[] args, String toStringOutput) {
                this.input = input;
                this.args = args;
                this.toStringOutput = toStringOutput;
            }
        }

        String typeName = "Person";
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "type Person = { name: String }",
                new Object[]{"Ken"},
                "Person { name: \"Ken\" }"
            ),
            new TestCase(
                "type Person = { name: String, age: Int }",
                new Object[]{"Ken", 26},
                "Person { name: \"Ken\", age: 26 }"
            ),
            new TestCase(
                "type Person = { name: String, age: Int, familyMembers: Array[String] }",
                new Object[]{"Ken", 26, new String[]{"Meg"}},
                "Person { name: \"Ken\", age: 26, familyMembers: [\"Meg\"] }"
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "An instance of `%s` invoked with `%s` should have a toString output of `%s`",
                    testCase.input, Arrays.toString(testCase.args), testCase.toStringOutput
                );
                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(testCase.input);
                    Object instance = getInstanceOfInnerClass(result.className, typeName, testCase.args);

                    assertNotNull(instance, "The instance of the type should not be null");
                    assertEquals(testCase.toStringOutput, instance.toString());
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testCustomTypeEquals_andHashCode() {
        class TestCase {
            private final String input;
            private final Object[] args1;
            private final Object[] args2;
            private final boolean areEqual;

            public TestCase(String input, Object[] args1, Object[] args2, boolean areEqual) {
                this.input = input;
                this.args1 = args1;
                this.args2 = args2;
                this.areEqual = areEqual;
            }
        }

        String typeName = "Person";
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "type Person = { name: String }",
                new Object[]{"Ken"}, new Object[]{"Ken"},
                true
            ),
            new TestCase(
                "type Person = { name: String, age: Int }",
                new Object[]{"Ken", 26}, new Object[]{"Ken", 26},
                true
            ),
            new TestCase(
                "type Person = { name: String, age: Int }",
                new Object[]{"Ken", 25}, new Object[]{"Ken", 26},
                false
            ),
            new TestCase(
                "type Person = { name: String, age: Int, familyMembers: Array[String] }",
                new Object[]{"Ken", 26, new String[]{"Meg"}}, new Object[]{"Ken", 26, new String[]{"Meg"}},
                true
            ),
            new TestCase(
                "type Person = { name: String, age: Int, familyMembers: Array[String] }",
                new Object[]{"Ken", 26, new String[]{}}, new Object[]{"Ken", 26, new String[]{"Meg"}},
                false
            )
        );

        return testCases.stream()
            .flatMap(testCase -> {
                String testName_equals = String.format(
                    "An instance of `%s` invoked with `%s` should be %sequal to an instance invoked with `%s`",
                    testCase.input, Arrays.toString(testCase.args1), testCase.areEqual ? "" : "not ", Arrays.toString(testCase.args2)
                );
                String testName_hashCode = String.format(
                    "An instance of `%s` invoked with `%s` should %shave the same hashCode as an instance invoked with `%s`",
                    testCase.input, Arrays.toString(testCase.args1), testCase.areEqual ? "" : "not ", Arrays.toString(testCase.args2)
                );

                TestCompilationResult result = parseTypecheckAndCompileInput(testCase.input);
                Object instance1 = getInstanceOfInnerClass(result.className, typeName, testCase.args1);
                assertNotNull(instance1, "The first instance of the type should not be null");
                Object instance2 = getInstanceOfInnerClass(result.className, typeName, testCase.args2);
                assertNotNull(instance2, "The second instance of the type should not be null");

                return Stream.of(
                    dynamicTest(testName_equals, () -> {
                        if (testCase.areEqual) assertEquals(instance1, instance2);
                        else assertNotEquals(instance1, instance2);
                    }),
                    dynamicTest(testName_hashCode, () -> {
                        if (testCase.areEqual) assertEquals(instance1.hashCode(), instance2.hashCode());
                        else assertNotEquals(instance1.hashCode(), instance2.hashCode());
                    })
                );
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testCustomTypeConstructor() {
        class TestCase {
            private final String input;
            private final String expectedToString;
            private final String valName;

            public TestCase(String input, String expectedToString, String valName) {
                this.input = input;
                this.expectedToString = expectedToString;
                this.valName = valName;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "type Person = { name: String, age: Int }" +
                    "val p = Person(name: 'Ken', age: 26)",
                "Person { name: \"Ken\", age: 26 }",
                "p"
            ),
            new TestCase(
                "type Person = { name: String, age: Int }" +
                    "val p = Person(age: 26, name: 'Ken')",
                "Person { name: \"Ken\", age: 26 }",
                "p"
            ),

            new TestCase(
                "type Person = { name: String, age: Int }" +
                    "type Team = { teamName: String, members: Array[Person] }" +
                    "val p = Person(age: 26, name: 'Ken')" +
                    "val t = Team(teamName: 'The Best Team', members: [p, Person(name: 'Meg', age: 25)])",
                "Team { teamName: \"The Best Team\", members: [Person { name: \"Ken\", age: 26 }, Person { name: \"Meg\", age: 25 }] }",
                "t"
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "Compiling and evaluating `%s` should result in the binding %s, whose toString value is `%s`",
                    testCase.input, testCase.valName, testCase.expectedToString
                );

                return dynamicTest(name, () -> {
                    TestCompilationResult result = parseTypecheckAndCompileInput(testCase.input);
                    Object val = loadPrivateStaticValueFromClass(result.className, testCase.valName);

                    assertEquals(testCase.expectedToString, val.toString());
                });
            })
            .collect(toList());
    }

    private Object getInstanceOfInnerClass(String outerClassName, String innerClassName, Object[] args) {
        Class innerClass = getInnerClass(outerClassName, innerClassName);
        List<Constructor> constructors = Arrays.stream(innerClass.getConstructors())
            .filter(constructor -> constructor.getParameterCount() == args.length)
            .collect(toList());
        if (constructors.size() == 0) {
            fail("No constructors found for class " + innerClassName + " with arity " + args.length);
            return null;
        } else if (constructors.size() > 1) {
            fail("Too many constructors found for class " + innerClassName + " with arity " + args.length);
            return null;
        }

        try {
            return constructors.get(0).newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            fail("Failed to instantiate instance of " + innerClassName, e);
            return null;
        }
    }
}

