package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.CompilerTestUtils.cleanupClassFiles;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.loadStaticValueFromClass;
import static co.kenrg.mega.backend.compilation.CompilerTestUtils.parseTypecheckAndCompileInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import co.kenrg.mega.backend.compilation.CompilerTestUtils.TestCompilationResult;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompilerTest {
    private List<Path> classFiles;

    @BeforeEach
    public void setup() {
        classFiles = Lists.newArrayList();
    }

    @AfterEach
    public void teardown() {
        cleanupClassFiles(classFiles);
    }

    @Test
    public void testStaticIntegerDeclarations() {
        String input = "val someInt1 = 123";// val someInt2 = -123456"; // TODO: Uncomment these test pieces after prefix expressions work
        TestCompilationResult result = parseTypecheckAndCompileInput(input);
        classFiles = result.classFiles;
        String className = result.className;

        int someInt1 = (int) loadStaticValueFromClass(className, "someInt1");
        assertEquals(123, someInt1, "The static value read off the generated class should be as expected");

//        int someInt2 = (int) loadStaticValueFromClass(className, "someInt2");
//        assertEquals(-123456, someInt2, "The static value read off the generated class should be as expected");
    }

    @Test
    public void testStaticFloatDeclarations() {
        String input = "val someFloat1 = 12.345";//; val someFloat2 = -12.345";
        TestCompilationResult result = parseTypecheckAndCompileInput(input);
        classFiles = result.classFiles;
        String className = result.className;

        float someFloat1 = (float) loadStaticValueFromClass(className, "someFloat1");
        float diff1 = (float) (someFloat1 - 12.345);
        assertTrue(diff1 < 0.0000005, "The static value read off the generated class should be as (close enough to) expected");

//        float someFloat2 = (float) loadStaticValueFromClass(className, "someFloat2");
//        float diff2 = (float) (someFloat2 - 12.345);
//        assertTrue(diff2 < 0.0000005, "The static value read off the generated class should be as (close enough to) expected");
    }

    @Test
    public void testStaticBooleanDeclarations() {
        String input = "val someBool1 = true; val someBool2 = false";
        TestCompilationResult result = parseTypecheckAndCompileInput(input);
        classFiles = result.classFiles;
        String className = result.className;

        boolean someBool1 = (boolean) loadStaticValueFromClass(className, "someBool1");
        assertEquals(true, someBool1, "The static value read off the generated class should be as expected");

        boolean someBool2 = (boolean) loadStaticValueFromClass(className, "someBool2");
        assertEquals(false, someBool2, "The static value read off the generated class should be as expected");
    }

    @Test
    public void testStaticStringDeclarations() {
        String input = "val someStr1 = 'string 1'; val someStr2 = \"string 2\"";
        TestCompilationResult result = parseTypecheckAndCompileInput(input);
        classFiles = result.classFiles;
        String className = result.className;

        String someStr1 = (String) loadStaticValueFromClass(className, "someStr1");
        assertEquals("string 1", someStr1, "The static value read off the generated class should be as expected");

        String someStr2 = (String) loadStaticValueFromClass(className, "someStr2");
        assertEquals("string 2", someStr2, "The static value read off the generated class should be as expected");
    }
}