package co.kenrg.mega.backend.compilation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.frontend.typechecking.TypeChecker;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

class CompilerTestUtils {
    private static String outputDir = CompilerTestUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    public static class TestCompilationResult {
        final String className;
        final List<Path> classFiles;

        TestCompilationResult(String className, List<Path> classFiles) {
            this.className = className;
            this.classFiles = classFiles;
        }
    }

    public static class TestFailureException extends RuntimeException {
        TestFailureException(Throwable cause) {
            super("Failing test due to exception", cause);
        }
    }

    static TestCompilationResult parseTypecheckAndCompileInput(String input) {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l);
        Module module = p.parseModule();

        TypeChecker typeChecker = new TypeChecker();
        typeChecker.typecheck(module, new TypeEnvironment());

        String className = RandomStringUtils.randomAlphabetic(16);
        Compiler compiler = new Compiler(className);
        List<Pair<String, byte[]>> generatedClasses = compiler.compile(module);

        try {
            List<Path> classFiles = Lists.newArrayList();
            for (Pair<String, byte[]> generatedClass : generatedClasses) {
                String name = generatedClass.getLeft();
                byte[] bytes = generatedClass.getRight();

                Path path = Paths.get(outputDir, String.format("%s.class", name));
                classFiles.add(path);

                File file = path.toFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
            }

            return new TestCompilationResult(className, classFiles);
        } catch (IOException e) {
            throw new TestFailureException(e);
        }
    }

    static void cleanupClassFiles(TestCompilationResult result) {
        try {
            for (Path path : result.classFiles) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new TestFailureException(e);
        }
    }

    static Class<?> loadClass(String className) {
        try {
            return CompilerTestUtils.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new TestFailureException(e);
        }
    }

    static Object loadStaticValueFromClass(String className, String fieldName) {
        try {
            return loadClass(className).getField(fieldName).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new TestFailureException(e);
        }
    }
}
