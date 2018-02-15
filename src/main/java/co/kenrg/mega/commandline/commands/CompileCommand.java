package co.kenrg.mega.commandline.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.repl.Repl;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;

public class CompileCommand {
    private static Map<String, TypeCheckResult<Module>> typedModuleCache = Maps.newHashMap();
    private static Map<String, TypeCheckResult<Module>> compiledModulesCache = Maps.newHashMap();

    public static TypeCheckResult<Module> compileModule(String moduleFileName, String outputDirectory) {
        String fullyQualifiedModuleName = moduleFileName.replace(".meg", "");
        if (compiledModulesCache.containsKey(fullyQualifiedModuleName)) {
            return compiledModulesCache.get(fullyQualifiedModuleName);
        }

        Optional<TypeCheckResult<Module>> resultOpt = typecheckModule(moduleFileName);
        if (!resultOpt.isPresent()) {
            System.err.println("Could not proceed compilation due to errors");
            System.exit(1);
            return null;
        }
        TypeCheckResult<Module> result = resultOpt.get();
        TypeEnvironment typeEnv = result.typeEnvironment;

        Module module = result.node;

        String moduleName = Paths.get(moduleFileName).getFileName().toString().replace(".meg", "");
        Compiler compiler = new Compiler(moduleName, typeEnv);
        compiler.setTypedModuleProvider(_moduleName -> compileModule(_moduleName, outputDirectory));
        List<Pair<String, byte[]>> classes = compiler.compile(module);
        if (!writeClasses(outputDirectory, classes)) {
            return null;
        }

        compiledModulesCache.put(moduleName, result);
        return result;
    }

    private static Optional<TypeCheckResult<Module>> typecheckModule(String moduleName) {
        if (typedModuleCache.containsKey(moduleName)) {
            return Optional.of(typedModuleCache.get(moduleName));
        } else {
            String code = getFileContents(moduleName);
            TypeEnvironment typeEnv = new TypeEnvironment();
            Optional<TypeCheckResult<Module>> resultOpt = Repl.readAndTypecheck(code, typeEnv, moduleName, CompileCommand::typecheckModule);
            resultOpt.ifPresent(moduleTypeCheckResult -> typedModuleCache.put(moduleName, moduleTypeCheckResult));
            return resultOpt;
        }
    }

    private static String getFileContents(String moduleName) {
        String moduleFilepath = moduleName.endsWith(".meg") ? moduleName : moduleName + ".meg";
        Path filepath = Paths.get(moduleFilepath);
        try {
            byte[] bytes = Files.readAllBytes(filepath);
            return new String(bytes);
        } catch (IOException e) {
            System.err.printf("No such file: %s\n", filepath.toAbsolutePath().toString());
            System.exit(1);
            return null;
        }
    }

    private static boolean writeClasses(String outputDirectory, List<Pair<String, byte[]>> classes) {
        for (Pair<String, byte[]> output : classes) {
            try {
                String name = output.getLeft();
                byte[] bytes = output.getRight();

                File file = Paths.get(outputDirectory, String.format("%s.class", name)).toFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Could not write class files");
                System.exit(1);
                return false;
            }
        }
        return true;
    }
}
