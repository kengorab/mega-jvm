package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.assertStaticBindingOnClassEquals;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.deleteGeneratedClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ImportTests {

    @BeforeAll
//    @AfterAll
    static void cleanup() {
        deleteGeneratedClassFiles();
    }

    @Test
    void testCompileImports_val() {
        String module1 = "export val hello = 'hello'";
        Function<String, String> getModule2 = (module1Name) -> "" +
            "import hello from '" + module1Name + "'" +
            "export val helloWorld = hello + ' world!'";
        String bindingName = "helloWorld";
        String expectedValue = "hello world!";

        TestCompilationResult result1 = parseTypecheckAndCompileInput(module1);
        String module1ClassName = result1.className;
        TypeCheckResult<Module> typeCheckResult1 = result1.typeCheckResult;

        String module2 = getModule2.apply(module1ClassName);
        TestCompilationResult result2 = parseTypecheckAndCompileInput(module2, moduleName -> {
            if (moduleName.equals(module1ClassName)) {
                return typeCheckResult1;
            } else {
                return null;
            }
        });

        assertStaticBindingOnClassEquals(result2.className, bindingName, expectedValue, false);
    }

    @Test
    void testCompileImports_methodReference() {
        String module1 = "export func sayHello() { 'hello' }";
        Function<String, String> getModule2 = (module1Name) -> "" +
            "import sayHello from '" + module1Name + "'" +
            "func shout(fn: () => String) { fn() + '!' }" +
            "export val shoutedHello = shout(sayHello)";
        String bindingName = "shoutedHello";
        String expectedValue = "hello!";

        TestCompilationResult result1 = parseTypecheckAndCompileInput(module1);
        String module1ClassName = result1.className;
        TypeCheckResult<Module> typeCheckResult1 = result1.typeCheckResult;

        String module2 = getModule2.apply(module1ClassName);
        TestCompilationResult result2 = parseTypecheckAndCompileInput(module2, moduleName -> {
            if (moduleName.equals(module1ClassName)) {
                return typeCheckResult1;
            } else {
                return null;
            }
        });

        assertStaticBindingOnClassEquals(result2.className, bindingName, expectedValue, false);
    }

    @Test
    void testCompileImports_methodImport() {
        String module1 = "export func sayHello() { 'hello' }";
        Function<String, String> getModule2 = (module1Name) -> "" +
            "import sayHello from '" + module1Name + "'" +
            "export val helloWorld = sayHello() + ' world!'";
        String bindingName = "helloWorld";
        String expectedValue = "hello world!";

        TestCompilationResult result1 = parseTypecheckAndCompileInput(module1);
        String module1ClassName = result1.className;
        TypeCheckResult<Module> typeCheckResult1 = result1.typeCheckResult;

        String module2 = getModule2.apply(module1ClassName);
        TestCompilationResult result2 = parseTypecheckAndCompileInput(module2, moduleName -> {
            if (moduleName.equals(module1ClassName)) {
                return typeCheckResult1;
            } else {
                return null;
            }
        });

        assertStaticBindingOnClassEquals(result2.className, bindingName, expectedValue, false);
    }

    @Test
    void testCompileImports_constructImportedType() {
        String module1 = "export type Person = { name: String }";
        Function<String, String> getModule2 = (module1Name) -> "" +
            "import Person from '" + module1Name + "'" +
            "export val person = Person(name: 'Ken')";
        String bindingName = "person";
        String expectedToStringValue = "Person { name: \"Ken\" }";

        TestCompilationResult result1 = parseTypecheckAndCompileInput(module1);
        String module1ClassName = result1.className;
        TypeCheckResult<Module> typeCheckResult1 = result1.typeCheckResult;

        String module2 = getModule2.apply(module1ClassName);
        TestCompilationResult result2 = parseTypecheckAndCompileInput(module2, moduleName -> {
            if (moduleName.equals(module1ClassName)) {
                return typeCheckResult1;
            } else {
                return null;
            }
        });

        Object person = loadStaticValueFromClass(result2.className, bindingName);
        assertEquals(expectedToStringValue, person.toString());
    }

    @Test
    void testCompileImports_importedStaticMethodFromJavaClass() {
        String input = "" +
            "import logicalAnd from 'java.lang.Boolean'" +
            "val b = logicalAnd(true, false)";
        TestCompilationResult result = parseTypecheckAndCompileInput(input, moduleName -> null);

        assertStaticBindingOnClassEquals(result.className, "b", false, true);
    }
}
