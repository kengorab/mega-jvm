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
import co.kenrg.mega.frontend.typechecking.ModuleDescriptor;
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

        compileModule(ModuleDescriptor.fromRaw(fileToCompile), outputDirectory);

        return true;
    }

    private static Map<ModuleDescriptor, TypeCheckResult<Module>> compiledModulesCache = Maps.newHashMap();

    private static TypeCheckResult<Module> compileModule(ModuleDescriptor moduleDescriptor, String outputDirectory) {
        if (compiledModulesCache.containsKey(moduleDescriptor)) {
            return compiledModulesCache.get(moduleDescriptor);
        }

        Optional<TypeCheckResult<Module>> resultOpt = typecheckModule(moduleDescriptor);
        if (!resultOpt.isPresent()) {
            // If there is no module returned, assume it's a non-meg module from classpath
            return null;
        }

        TypeCheckResult<Module> result = resultOpt.get();
        TypeEnvironment typeEnv = result.typeEnvironment;

        Module module = result.node;

        Compiler compiler = new Compiler(moduleDescriptor.moduleName, typeEnv);
        compiler.setTypedModuleProvider(_moduleName -> compileModule(ModuleDescriptor.fromRaw(_moduleName), outputDirectory));
        List<Pair<String, byte[]>> classes = compiler.compile(module);
        if (!writeClasses(outputDirectory, classes)) {
            return null;
        }

        compiledModulesCache.put(moduleDescriptor, result);
        return result;
    }

    private static Map<ModuleDescriptor, Optional<TypeCheckResult<Module>>> typedModuleCache = Maps.newHashMap();

    private static Optional<TypeCheckResult<Module>> typecheckModule(ModuleDescriptor moduleDescriptor) {
        if (typedModuleCache.containsKey(moduleDescriptor)) {
            return typedModuleCache.get(moduleDescriptor);
        } else {
            Optional<String> code = getFileContents(moduleDescriptor);
            if (code.isPresent()) {
                TypeEnvironment typeEnv = new TypeEnvironment();
                Optional<TypeCheckResult<Module>> result = Repl.readAndTypecheck(
                    code.get(),
                    typeEnv,
                    moduleDescriptor.moduleName,
                    _moduleName -> CompileSubcommand.typecheckModule(ModuleDescriptor.fromRaw(_moduleName))
                );
                typedModuleCache.put(moduleDescriptor, result);
                return result;
            }
            return Optional.empty();
        }
    }

    private static Map<String, Optional<String>> fileCache = Maps.newHashMap();

    private static Optional<String> getFileContents(ModuleDescriptor moduleDescriptor) {
        if (fileCache.containsKey(moduleDescriptor.filepath)) {
            return fileCache.get(moduleDescriptor.filepath);
        }

        Path filepath = Paths.get(moduleDescriptor.filepath);
        try {
            byte[] bytes = Files.readAllBytes(filepath);
            String contents = new String(bytes);
            Optional<String> result = Optional.of(contents);
            fileCache.put(moduleDescriptor.filepath, result);
            return result;
        } catch (IOException e) {
            Optional<String> result = Optional.empty();
            fileCache.put(moduleDescriptor.filepath, result);
            return result;
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
