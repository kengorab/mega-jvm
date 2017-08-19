package co.kenrg.mega.frontend.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;

public class ParserTestUtils {
    public static ExpressionStatement parseExpressionStatement(String input) {
        Statement statement = parseStatement(input);
        assertTrue(statement instanceof ExpressionStatement);
        return (ExpressionStatement) statement;
    }

    public static Statement parseStatement(String input) {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l);
        Module module = p.parseModule();

        int numErrs = p.errors.size();
        if (numErrs != 0) {
            System.out.println("Parser errors:");
            for (SyntaxError error : p.errors) {
                System.out.println("  " + error.message);
            }
        }
        assertEquals(0, p.errors.size(), "There should be 0 parser errors");

        return module.statements.get(0);
    }
}