package co.kenrg.mega.frontend.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import org.junit.jupiter.api.Test;

class AstTest {

    @Test
    public void testRepr_valStatement() {
        String input = "val  x    =  abc";
        Parser p = new Parser(new Lexer(input));

        String repr = p.parseModule().repr(true, 0);
        assertEquals(
            "val x = abc",
            repr
        );
    }
}