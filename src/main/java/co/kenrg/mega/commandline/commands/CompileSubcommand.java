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
import co.kenrg.mega.commandline.iface.Subcommand;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.repl.Repl;
import com.google.common.collect.Maps;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.tuple.Pair;

public class CompileSubcommand implements Subcommand {

    @Override
    public String name() {
        return "compile [filename]";
    }

    @Override
    public String desc() {
        return "Compile the Mega file passed as an argument to JVM class files";
    }

    @Override
    public Options opts() {
        return new Options()
            .addOption("h", "help", false, "Displays this help information, for the compile subcommand")
            .addOption("o", "out-dir", true, "Directory where compiled class files should be written (defaults to current directory)");
    }

    @Override
    public boolean execute(CommandLine command) {
        if (command.hasOption('h') || command.getArgList().size() != 1) {
            return false;
        }

        String outputDirectory = System.getProperty("user.dir");
        if (command.hasOption('o')) {
            Path path = Paths.get(command.getOptionValue('o'));
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                    return false;
                }
            }
            outputDirectory = path.toAbsolutePath().toString();
        }

        String fileToCompile = command.getArgList().get(0);
        Path filepath = Paths.get(fileToCompile);
        if (!fileToCompile.endsWith(".meg")) {
            System.err.printf("Invalid file extension for module %s; expected .meg extension\n", filepath.toAbsolutePath().toString());
            System.exit(1);
        }
        compileModule(fileToCompile, outputDirectory);

        return true;
    }

    private static Map<String, TypeCheckResult<Module>> typedModuleCache = Maps.newHashMap();
    private static Map<String, TypeCheckResult<Module>> compiledModulesCache = Maps.newHashMap();

    private static TypeCheckResult<Module> compileModule(String moduleFileName, String outputDirectory) {
        String fullyQualifiedModuleName = moduleFileName.replaceAll("\\.meg$", "");
        if (compiledModulesCache.containsKey(fullyQualifiedModuleName)) {
            return compiledModulesCache.get(fullyQualifiedModuleName);
        }

        Optional<TypeCheckResult<Module>> resultOpt = typecheckModule(moduleFileName);
        if (!resultOpt.isPresent()) {
            // If there is no module returned, assume it's a non-meg module from classpath
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
            Optional<String> code = getFileContents(moduleName);
            if (code.isPresent()) {
                TypeEnvironment typeEnv = new TypeEnvironment();
                Optional<TypeCheckResult<Module>> resultOpt = Repl.readAndTypecheck(code.get(), typeEnv, moduleName, CompileSubcommand::typecheckModule);
                resultOpt.ifPresent(moduleTypeCheckResult -> typedModuleCache.put(moduleName, moduleTypeCheckResult));
                return resultOpt;
            }
            return Optional.empty();
        }
    }

    private static Optional<String> getFileContents(String moduleName) {
        String moduleFilepath = moduleName.endsWith(".meg") ? moduleName : moduleName + ".meg";
        Path filepath = Paths.get(moduleFilepath.replace('.', '/'));
        try {
            byte[] bytes = Files.readAllBytes(filepath);
            return Optional.of(new String(bytes));
        } catch (IOException e) {
            return Optional.empty();
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
