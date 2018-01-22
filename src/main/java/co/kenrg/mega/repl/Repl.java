package co.kenrg.mega.repl;

import java.util.Optional;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.backend.evaluation.evaluator.Evaluator;
import co.kenrg.mega.backend.evaluation.object.NullObj;
import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import co.kenrg.mega.frontend.typechecking.TypeChecker;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.repl.commands.Commands;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

public class Repl {

    public static void start(Environment env, TypeEnvironment typeEnv) {
        LineReader reader = LineReaderBuilder.builder()
            .build();
        reader.setOpt(Option.DISABLE_EVENT_EXPANSION);
        String prompt = ">> ";

        while (true) {
            try {
                String line = reader.readLine(prompt);
                if (Commands.shouldHandleLine(line)) {
                    System.out.println(Commands.handleLine(line, typeEnv, env));
                } else {
                    readEvalPrint(line, env, typeEnv);
                }
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println("Bye for now!");
                return;
            }
        }
    }

    public static Optional<Module> readAndTypecheck(String code, TypeEnvironment typeEnv) {
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer);
        Module module = parser.parseModule();

        if (!parser.errors.isEmpty()) {
            System.out.println("Syntax errors:");
            for (SyntaxError error : parser.errors) {
                System.out.println("  " + error.message);
            }

            return Optional.empty();
        }

        TypeChecker typeChecker = new TypeChecker();
        TypeCheckResult<Module> typecheckResult = typeChecker.typecheck(module, typeEnv);

        if (typecheckResult.hasErrors()) {
            System.out.println("Type errors:");
            for (TypeCheckerError error : typecheckResult.errors) {
                System.out.println(String.format("  (%d, %d): %s", error.position.line, error.position.col, error.message()));
            }

            return Optional.empty();
        }
        return Optional.of(module);
    }

    public static void readEvalPrint(String code, Environment env, TypeEnvironment typeEnv) {
        Optional<Module> module = readAndTypecheck(code, typeEnv);
        if (!module.isPresent()) {
            System.out.println("Cannot proceed due to errors.");
            return;
        }

        Obj evaluated = Evaluator.eval(module.get(), env);
        if (evaluated != null && !evaluated.equals(NullObj.NULL)) {
            System.out.println(evaluated.inspect(0));
        }
    }
}
