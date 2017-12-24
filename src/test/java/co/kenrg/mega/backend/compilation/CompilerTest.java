package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.cleanupClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static org.junit.jupiter.api.Assertions.assertEquals;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import org.junit.jupiter.api.Test;

class CompilerTest {

    @Test
    public void testStaticIntegerDeclarations() {
        String input = "val someInt = 123";
        TestCompilationResult result = parseTypecheckAndCompileInput(input);
        String className = result.className;

        int someInt = (int) loadStaticValueFromClass(className, "someInt");
        assertEquals(123, someInt, "The static value read off the generated class should be as expected");

        cleanupClassFiles(result);
    }
}