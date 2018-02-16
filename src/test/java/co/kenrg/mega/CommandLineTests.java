package co.kenrg.mega;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class CommandLineTests {

    @Nested
    class HelpSubcommandTests {

        @TestFactory
        List<DynamicTest> printsFullUsageInfo() {
            List<String> testCases = Lists.newArrayList("help", "-h");
            return testCases.stream()
                .map(testCase -> {
                    String name = String.format("Running `mega %s` results in full usage info", testCase);

                    return dynamicTest(name, () -> {
                        String output = run(testCase);
                        String expected = "" +
                            "usage: mega\n" +
                            "Mega has a couple of subcommands\n" +
                            "  help              Displays this help information. You can also\n" +
                            "                    run help on a specific command.\n" +
                            "                        e.g. `mega help repl`\n" +
                            "                        Equivalent to `mega repl -h`\n" +
                            "\n" +
                            "  repl              Starts up the Mega REPL (Read Eval Print Loop)\n" +
                            "\n" +
                            "  run               Executes the Mega file passed as an argument\n" +
                            "\n" +
                            "  compile           Compiles the Mega file to JVM bytecode\n";
                        assertEquals(expected, output);
                    });
                })
                .collect(toList());
        }

        @Test
        void argumentIsHelp_printsHelpUsageInfo() throws ParseException {
            String output = run("help", "help");
            String expected = "" +
                "usage: mega help\n" +
                "Displays help information about various subcommands\n\n";
            assertEquals(expected, output);
        }

        @Test
        void argumentIsRepl_printsReplUsageInfo() throws ParseException {
            String output = run("help", "repl");
            String expected = "" +
                "usage: mega repl\n" +
                "Start up the Mega REPL (Read Eval Print Loop)\n" +
                "  -h,--help               Displays this help information, for the repl\n" +
                "                          subcommand\n" +
                "  -l,--load-file <arg>    Loads a REPL, with the given file evaluated\n";
            assertEquals(expected, output);
        }

        @Test
        void argumentIsRun_printsRunUsageInfo() throws ParseException {
            String output = run("help", "run");
            String expected = "" +
                "usage: mega run [filename]\n" +
                "Evaluates the Mega file passed as an argument\n" +
                "  -h,--help    Displays this help information, for the run subcommand\n";
            assertEquals(expected, output);
        }

        @Test
        void argumentIsCompile_printsCompileUsageInfo() throws ParseException {
            String output = run("help", "compile");
            String expected = "" +
                "usage: mega compile [filename]\n" +
                "Compile the Mega file passed as an argument to JVM class files\n" +
                "  -h,--help             Displays this help information, for the compile\n" +
                "                        subcommand\n" +
                "  -o,--out-dir <arg>    Directory where compiled class files should be written\n" +
                "                        (defaults to current directory)\n";
            assertEquals(expected, output);
        }
    }

    private String run(String... args) throws ParseException {
        OutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        Main.handleSubcommand(args, ps);
        return os.toString();
    }
}