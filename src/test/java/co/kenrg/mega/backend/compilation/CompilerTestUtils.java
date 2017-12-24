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

public class CompilerTestUtils {
    private static String outputDir = CompilerTestUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    public static class TestCompilationResult {
        final String className;
        final List<Path> classFiles;

        public TestCompilationResult(String className, List<Path> classFiles) {
            this.className = className;
            this.classFiles = classFiles;
        }
    }

    public static TestCompilationResult parseTypecheckAndCompileInput(String input) throws IOException {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l);
        Module module = p.parseModule();

        TypeChecker typeChecker = new TypeChecker();
        typeChecker.typecheck(module, new TypeEnvironment());

        String className = RandomStringUtils.randomAlphabetic(16);
        Compiler compiler = new Compiler(className);
        List<Pair<String, byte[]>> generatedClasses = compiler.compile(module);

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
    }

    public static void cleanupClassFiles(TestCompilationResult result) throws IOException {
        for (Path path : result.classFiles) {
            Files.deleteIfExists(path);
        }
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return CompilerTestUtils.class.getClassLoader().loadClass(className);
    }
}
