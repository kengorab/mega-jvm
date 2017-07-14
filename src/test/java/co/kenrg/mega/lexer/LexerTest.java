package co.kenrg.mega.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import co.kenrg.mega.token.Token;
import co.kenrg.mega.token.TokenType;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

class LexerTest {

    @Test
    public void testNextToken_singleCharSymbols() {
        String input = "=+(){},;";

        List<Token> expectedTokens = Lists.newArrayList(
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.PLUS, "+"),
                new Token(TokenType.LPAREN, "("),
                new Token(TokenType.RPAREN, ")"),
                new Token(TokenType.LBRACE, "{"),
                new Token(TokenType.RBRACE, "}"),
                new Token(TokenType.COMMA, ","),
                new Token(TokenType.SEMICOLON, ";")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_integers() {
        String input = "1 55 155";

        List<Token> expectedTokens = Lists.newArrayList(
                new Token(TokenType.INT, "1"),
                new Token(TokenType.INT, "55"),
                new Token(TokenType.INT, "155")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_floats() {
        String input = "1.0 5.5 0.155 1. 0.003";

        List<Token> expectedTokens = Lists.newArrayList(
                new Token(TokenType.FLOAT, "1.0"),
                new Token(TokenType.FLOAT, "5.5"),
                new Token(TokenType.FLOAT, "0.155"),
                new Token(TokenType.INT, "1"),
                new Token(TokenType.FLOAT, "0.003")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_idents() {
        String input = "someVar foo bar fooBar";

        List<Token> expectedTokens = Lists.newArrayList(
                new Token(TokenType.IDENT, "someVar"),
                new Token(TokenType.IDENT, "foo"),
                new Token(TokenType.IDENT, "bar"),
                new Token(TokenType.IDENT, "fooBar")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_keywords() {
        String input = "let fn";

        List<Token> expectedTokens = Lists.newArrayList(
                new Token(TokenType.LET, "let"),
                new Token(TokenType.FUNCTION, "fn")
        );
        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_skipsWhitespaceAndNewlines() {
        String input = "let five = 5\n" +
                "let ten = 10";
        List<Token> expectedTokens = Lists.newArrayList(
                new Token(TokenType.LET, "let"),
                new Token(TokenType.IDENT, "five"),
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.INT, "5"),
                new Token(TokenType.LET, "let"),
                new Token(TokenType.IDENT, "ten"),
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.INT, "10")
        );
        assertTokensForInput(expectedTokens, input);
    }

    private void assertTokensForInput(List<Token> expectedTokens, String input) {
        Lexer l = new Lexer(input);
        for (Token expectedToken : expectedTokens) {
            Token token = l.nextToken();
            assertEquals(expectedToken, token);
        }
    }
}