package co.kenrg.mega.repl;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import co.kenrg.mega.frontend.typechecking.TypeChecker;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.repl.evaluator.Environment;
import co.kenrg.mega.repl.evaluator.Evaluator;
import co.kenrg.mega.repl.object.NullObj;
import co.kenrg.mega.repl.object.iface.Obj;
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
                readEvalPrint(line, env, typeEnv);
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println("Bye for now!");
                return;
            }
        }
    }

    public static void readEvalPrint(String code, Environment env, TypeEnvironment typeEnv) {
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer);
        Module module = parser.parseModule();

        if (!parser.errors.isEmpty()) {
            System.out.println("Syntax errors:");
            for (SyntaxError error : parser.errors) {
                System.out.println("  " + error.message);
            }

            return;
        }

        TypeChecker typeChecker = new TypeChecker();
        TypeCheckResult<Module> typecheckResult = typeChecker.typecheck(module, typeEnv);

        if (typecheckResult.hasErrors()) {
            System.out.println("Type errors:");
            for (TypeCheckerError error : typecheckResult.errors) {
                System.out.println("  " + error.message());
            }

            return;
        }

        Obj evaluated = Evaluator.eval(module, env);
        if (evaluated != null && !evaluated.equals(NullObj.NULL)) {
            System.out.println(evaluated.inspect(0));
        }
    }
}
