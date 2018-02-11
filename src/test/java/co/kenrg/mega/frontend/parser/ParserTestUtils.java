package co.kenrg.mega.frontend.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import org.apache.commons.lang3.tuple.Pair;

public class ParserTestUtils {
    public static ExpressionStatement parseExpressionStatement(String input) {
        Statement statement = parseStatement(input);
        assertTrue(statement instanceof ExpressionStatement);
        return (ExpressionStatement) statement;
    }

    public static Module parseModule(String input) {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l);
        return p.parseModule();
    }

    public static Module parseStatementAndGetModule(String input) {
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

        return module;
    }

    public static Statement parseStatement(String input) {
        Module module = parseStatementAndGetModule(input);
        return module.statements.get(0);
    }

    public static Pair<Statement, List<SyntaxError>> parseStatementAndGetErrors(String input) {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l);
        Module module = p.parseModule();

        return Pair.of(module.statements.get(0), p.errors);
    }
}
