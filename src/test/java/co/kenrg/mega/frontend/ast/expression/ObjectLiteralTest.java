package co.kenrg.mega.frontend.ast.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import co.kenrg.mega.utils.LinkedHashMultimaps;
import org.junit.jupiter.api.Test;

class ObjectLiteralTest {
    private Token lbraceToken = new Token(TokenType.LBRACE, "{");

    @Test
    void testRepr_empty() {
        ObjectLiteral obj = new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of());
        assertEquals("{}", obj.repr(true, 0));
    }

    @Test
    void testRepr_onePair() {
        ObjectLiteral obj = new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
            new Identifier(new Token(TokenType.IDENT, "favNum"), "favNum"),
            new IntegerLiteral(new Token(TokenType.INT, "24"), 24)
        ));
        assertEquals("{ favNum: 24 }", obj.repr(true, 0));
    }

    @Test
    void testRepr_twoPairs() {
        ObjectLiteral obj = new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
            new Identifier(new Token(TokenType.IDENT, "favNum"), "favNum"),
            new IntegerLiteral(new Token(TokenType.INT, "24"), 24)
            ,
            new Identifier(new Token(TokenType.IDENT, "name"), "name"),
            new StringLiteral(new Token(TokenType.STRING, "Ken"), "Ken")
        ));
        assertEquals("{ favNum: 24, name: \"Ken\" }", obj.repr(true, 0));
    }

    @Test
    void testRepr_threePairs_multiLine() {
        ObjectLiteral obj = new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
            new Identifier(new Token(TokenType.IDENT, "favNum"), "favNum"),
            new IntegerLiteral(new Token(TokenType.INT, "24"), 24)
            ,
            new Identifier(new Token(TokenType.IDENT, "age"), "age"),
            new IntegerLiteral(new Token(TokenType.INT, "25"), 25)
            ,
            new Identifier(new Token(TokenType.IDENT, "name"), "name"),
            new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
                new Identifier(new Token(TokenType.IDENT, "first"), "first"),
                new StringLiteral(new Token(TokenType.STRING, "Ken"), "Ken")
                ,
                new Identifier(new Token(TokenType.IDENT, "last"), "last"),
                new StringLiteral(new Token(TokenType.STRING, "Gorab"), "Gorab")
            ))
        ));
        String expected = "" +
            "{\n" +
            "  favNum: 24,\n" +
            "  age: 25,\n" +
            "  name: { first: \"Ken\", last: \"Gorab\" }\n" +
            "}";
        assertEquals(expected, obj.repr(true, 0));
    }

    @Test
    void testRepr_threePairs_multiLine_indented() {
        ObjectLiteral obj = new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
            new Identifier(new Token(TokenType.IDENT, "favNum"), "favNum"),
            new IntegerLiteral(new Token(TokenType.INT, "24"), 24)
            ,
            new Identifier(new Token(TokenType.IDENT, "age"), "age"),
            new IntegerLiteral(new Token(TokenType.INT, "25"), 25)
            ,
            new Identifier(new Token(TokenType.IDENT, "name"), "name"),
            new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
                new Identifier(new Token(TokenType.IDENT, "first"), "first"),
                new StringLiteral(new Token(TokenType.STRING, "Ken"), "Ken")
                ,
                new Identifier(new Token(TokenType.IDENT, "last"), "last"),
                new StringLiteral(new Token(TokenType.STRING, "Gorab"), "Gorab")
            ))
        ));
        String expected = "" +
            "  {\n" +
            "    favNum: 24,\n" +
            "    age: 25,\n" +
            "    name: { first: \"Ken\", last: \"Gorab\" }\n" +
            "  }";
        assertEquals(expected, obj.repr(true, 1));
    }

    @Test
    void testRepr_longerThan80Chars_multiLine() {
        ObjectLiteral obj = new ObjectLiteral(lbraceToken, LinkedHashMultimaps.of(
            new Identifier(new Token(TokenType.IDENT, "city"), "city"),
            new StringLiteral(new Token(TokenType.STRING, "Some super long city name that will cause a newline break"), "Some super long city name that will cause a newline break")
            ,
            new Identifier(new Token(TokenType.IDENT, "state"), "state"),
            new StringLiteral(new Token(TokenType.STRING, "New York"), "New York")
        ));
        String expected = "" +
            "{\n" +
            "  city: \"Some super long city name that will cause a newline break\",\n" +
            "  state: \"New York\"\n" +
            "}";
        assertEquals(expected, obj.repr(true, 0));
    }
}