package co.kenrg.mega.repl;

import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

public class Repl {

    public static void start() {
        LineReader reader = LineReaderBuilder.builder().build();
        String prompt = ">> ";

        while (true) {
            try {
                String line = reader.readLine(prompt);
                Lexer lexer = new Lexer(line);

                Token token = lexer.nextToken();
                while (token.type != TokenType.EOF) {
                    System.out.println(token.literal);
                    token = lexer.nextToken();
                }
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println("Bye for now!");
                return;
            }
        }
    }
}
