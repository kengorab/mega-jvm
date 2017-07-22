package co.kenrg.mega.frontend.lexer;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.IntStream;

import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class LexerTest {

    @Test
    public void testNextToken_singleCharSymbols() {
        String input = "( ) { } , ; \n" +
            "+ - / * = ! < >";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.LPAREN, "("),
            new Token(TokenType.RPAREN, ")"),
            new Token(TokenType.LBRACE, "{"),
            new Token(TokenType.RBRACE, "}"),
            new Token(TokenType.COMMA, ","),
            new Token(TokenType.SEMICOLON, ";"),
            new Token(TokenType.PLUS, "+"),
            new Token(TokenType.MINUS, "-"),
            new Token(TokenType.SLASH, "/"),
            new Token(TokenType.STAR, "*"),
            new Token(TokenType.ASSIGN, "="),
            new Token(TokenType.BANG, "!"),
            new Token(TokenType.LANGLE, "<"),
            new Token(TokenType.RANGLE, ">")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_multiCharSymbols() {
        String input = "== != <= >= =>";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.EQ, "=="),
            new Token(TokenType.NEQ, "!="),
            new Token(TokenType.LTE, "<="),
            new Token(TokenType.GTE, ">="),
            new Token(TokenType.ARROW, "=>")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_integers() {
        String input = "1 55 155 1.";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.INT, "1"),
            new Token(TokenType.INT, "55"),
            new Token(TokenType.INT, "155"),
            new Token(TokenType.INT, "1")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_floats() {
        String input = "1.0 5.5 0.155 0.003";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.FLOAT, "1.0"),
            new Token(TokenType.FLOAT, "5.5"),
            new Token(TokenType.FLOAT, "0.155"),
            new Token(TokenType.FLOAT, "0.003")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_booleans() {
        String input = "true false";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.TRUE, "true"),
            new Token(TokenType.FALSE, "false")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @TestFactory
    public List<DynamicTest> testNextToken_strings() {
        List<String> manualTestCases = Lists.newArrayList(
            "hello world",
            "123",
            "咊 ⧻",
            "🙌💯"
        );

        return Streams
            .concat(
                IntStream.range(0, 9).mapToObj(i -> RandomStringUtils.random(24)),
                manualTestCases.stream()
            )
            .map(testCase -> {
                String input = "\"" + testCase + "\"";
                String name = String.format("'%s' should lex to a string token, with literal '%s'", input, testCase);
                return dynamicTest(name, () -> {
                    List<Token> expectedTokens = Lists.newArrayList(
                        new Token(TokenType.STRING, testCase)
                    );

                    assertTokensForInput(expectedTokens, input);
                });
            })
            .collect(toList());
    }

//    @TestFactory
//    public List<DynamicTest> testNextToken_stringsWithEscapes() {
//        List<Pair<String, String>> testCases = Lists.newArrayList(
//            Pair.of("\\'", ""),
//            Pair.of("123", ""),
//            Pair.of("咊 ⧻", ""),
//            Pair.of("🙌💯", "")
//        );
//
//        return testCases.stream()
//            .map(testCase -> {
//                String input = "\"" + testCase + "\"";
//                String name = String.format("'%s' should lex to a string token, with literal '%s'", input, testCase);
//                return dynamicTest(name, () -> {
//                    List<Token> expectedTokens = Lists.newArrayList(
//                        new Token(TokenType.STRING, testCase)
//                    );
//
//                    assertTokensForInput(expectedTokens, input);
//                });
//            })
//            .collect(toList());
//    }

    @Test
    public void testNextToken_stringsWithMissingClosingQuote_syntaxError() {
        String input = "\"hello world";

        Lexer l = new Lexer(input);
        Pair<Token, SyntaxError> t = l.nextToken();
        assertEquals("Expected \", saw EOF", t.getRight().message);
    }

    @Test
    public void testNextToken_identifiers() {
        String input = "someVar foo bar fooBar ab1";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.IDENT, "someVar"),
            new Token(TokenType.IDENT, "foo"),
            new Token(TokenType.IDENT, "bar"),
            new Token(TokenType.IDENT, "fooBar"),
            new Token(TokenType.IDENT, "ab1")
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_keywords() {
        String input = "let func if else";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.LET, "let"),
            new Token(TokenType.FUNCTION, "func"),
            new Token(TokenType.IF, "if"),
            new Token(TokenType.ELSE, "else")
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
            Token token = l.nextToken().getLeft();
            assertEquals(expectedToken, token);
        }
    }
}