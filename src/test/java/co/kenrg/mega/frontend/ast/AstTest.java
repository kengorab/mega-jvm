package co.kenrg.mega.frontend.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import org.junit.jupiter.api.Test;

class AstTest {

    @Test
    public void testRepr_letStatement() {
        String input = "let  x    =  abc";
        Parser p = new Parser(new Lexer(input));

        String repr = p.parseModule().repr(true, 0);
        assertEquals(
            "let x = abc",
            repr
        );
    }
}