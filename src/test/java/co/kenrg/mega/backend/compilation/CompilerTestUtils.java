package co.kenrg.mega.backend.compilation;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import co.kenrg.mega.frontend.typechecking.TypeChecker;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

class CompilerTestUtils {
    private static String outputDir = CompilerTestUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    public static class TestCompilationResult {
        final String className;
        final List<Path> classFiles;
        final TypeCheckResult<Module> typeCheckResult;

        TestCompilationResult(String className, List<Path> classFiles, TypeCheckResult<Module> typeCheckResult) {
            this.className = className;
            this.classFiles = classFiles;
            this.typeCheckResult = typeCheckResult;
        }
    }

    public static class TestFailureException extends RuntimeException {
        TestFailureException(Throwable cause) {
            super("Failing test due to exception", cause);
        }

        TestFailureException(String message) {
            super("Failing test: " + message);
        }
    }

    static boolean requireNoParseOrTypecheckErrors = false;

    static TestCompilationResult parseTypecheckAndCompileInput(String input, Function<String, TypeCheckResult<Module>> typedModuleProvider) {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l);
        Module module = p.parseModule();

        if (requireNoParseOrTypecheckErrors && !p.errors.isEmpty()) {
            System.out.println("Encountered parser errors: ");
            for (SyntaxError error : p.errors) {
                System.out.println(String.format("  (%d, %d): %s", error.position.line, error.position.col, error.message));
            }
            throw new TestFailureException("Test failed due to parser errors");
        }

        TypeChecker typeChecker = new TypeChecker();
        typeChecker.setModuleProvider(moduleName -> Optional.of(typedModuleProvider.apply(moduleName)));
        TypeEnvironment typeEnv = new TypeEnvironment();
        TypeCheckResult<Module> typecheckResult = typeChecker.typecheck(module, typeEnv);

        if (requireNoParseOrTypecheckErrors && !typecheckResult.errors.isEmpty()) {
            System.out.println("Encountered typechecking errors: ");
            for (TypeCheckerError error : typecheckResult.errors) {
                System.out.println(String.format("  (%d, %d): %s", error.position.line, error.position.col, error.message()));
            }
            throw new TestFailureException("Test failed due to typechecking errors");
        }

        String className = StringUtils.capitalize(RandomStringUtils.randomAlphabetic(16));
        Compiler compiler = new Compiler(className, typeEnv);
        compiler.setTypedModuleProvider(typedModuleProvider);
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

            return new TestCompilationResult(className, classFiles, typecheckResult);
        } catch (IOException e) {
            throw new TestFailureException(e);
        }
    }

    static TestCompilationResult parseTypecheckAndCompileInput(String input) {
        return parseTypecheckAndCompileInput(input, moduleName -> null);
    }

    static void deleteGeneratedClassFiles() {
        try {
            Files.list(Paths.get(outputDir))
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new TestFailureException(e);
                    }
                });
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

    static Field loadStaticVariableFromClass(String className, String fieldName) {
        try {
            return loadClass(className).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new TestFailureException(e);
        }
    }

    static List<Method> loadStaticMethodsFromClass(String className, String methodName) {
        return Arrays.stream(loadClass(className).getDeclaredMethods())
            .filter(method -> method.getName().equals(methodName))
            .collect(toList());
    }

    static Object loadStaticValueFromClass(String className, String fieldName) {
        try {
            return loadStaticVariableFromClass(className, fieldName).get(null);
        } catch (IllegalAccessException e) {
            throw new TestFailureException(e);
        }
    }

    static Object loadPrivateStaticValueFromClass(String className, String fieldName) {
        try {
            Field field = loadStaticVariableFromClass(className, fieldName);

            if (!Modifier.isPrivate(field.getModifiers())) {
                throw new TestFailureException("Field " + fieldName + " on class " + className + " was not private");
            }

            field.setAccessible(true);
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new TestFailureException(e);
        }
    }

    static void assertStaticBindingOnClassEquals(String className, String staticFieldName, Object value, boolean isPrivate) {
        BiFunction<String, String, Object> valueGetter = isPrivate
            ? CompilerTestUtils::loadPrivateStaticValueFromClass
            : CompilerTestUtils::loadStaticValueFromClass;

        if (value instanceof Integer) {
            int variable = (int) valueGetter.apply(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof Float) {
            float variable = (float) valueGetter.apply(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof Boolean) {
            boolean variable = (boolean) valueGetter.apply(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof String) {
            String variable = (String) valueGetter.apply(className, staticFieldName);
            assertEquals(value, variable, "The static value read off the generated class should be as expected");
        } else if (value instanceof Object[]) {
            Object[] variable = (Object[]) valueGetter.apply(className, staticFieldName);
            assertArrayEquals((Object[]) value, variable, "The static value read off the generated class should be as expected");
        }
    }

    static Class getInnerClass(String outerClassName, String innerClassName) {
        try {
            Class<?>[] innerClasses = CompilerTestUtils.class.getClassLoader().loadClass(outerClassName).getDeclaredClasses();
            List<Class<?>> classesMatchingName = Arrays.stream(innerClasses)
                .filter(clazz -> clazz.getSimpleName().equals(innerClassName))
                .collect(toList());
            if (classesMatchingName.size() == 0) {
                throw new TestFailureException("No classes found for name: " + innerClassName);
            }
            if (classesMatchingName.size() > 1) {
                throw new TestFailureException("More than 1 class found for name: " + innerClassName);
            }
            return classesMatchingName.get(0);
        } catch (ClassNotFoundException e) {
            throw new TestFailureException(e);
        }
    }
}
