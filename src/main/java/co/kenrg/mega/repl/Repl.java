package co.kenrg.mega.repl;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.repl.evaluator.Environment;
import co.kenrg.mega.repl.evaluator.Evaluator;
import co.kenrg.mega.repl.object.iface.Obj;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

public class Repl {

    public static void start() {
        LineReader reader = LineReaderBuilder.builder().build();
        String prompt = ">> ";

        Environment env = new Environment();

        while (true) {
            try {
                String line = reader.readLine(prompt);
                Lexer lexer = new Lexer(line);
                Parser parser = new Parser(lexer);
                Module module = parser.parseModule();

                if (!parser.errors.isEmpty()) {
                    System.out.println("There were errors!!!");
                }

                Obj evaluated = Evaluator.eval(module, env);
                if (evaluated != null) {
                    System.out.println(evaluated.inspect());
                }
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println("Bye for now!");
                return;
            }
        }
    }
}
