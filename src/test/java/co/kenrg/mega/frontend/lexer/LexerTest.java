package co.kenrg.mega.frontend.lexer;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.IntStream;

import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.token.Position;
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
        String input = "( ) { } [ ] , ; : . \n" +
            "+ - / * = ! < >";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.LPAREN, "(", Position.at(1, 1)),
            new Token(TokenType.RPAREN, ")", Position.at(1, 3)),
            new Token(TokenType.LBRACE, "{", Position.at(1, 5)),
            new Token(TokenType.RBRACE, "}", Position.at(1, 7)),
            new Token(TokenType.LBRACK, "[", Position.at(1, 9)),
            new Token(TokenType.RBRACK, "]", Position.at(1, 11)),
            new Token(TokenType.COMMA, ",", Position.at(1, 13)),
            new Token(TokenType.SEMICOLON, ";", Position.at(1, 15)),
            new Token(TokenType.COLON, ":", Position.at(1, 17)),
            new Token(TokenType.DOT, ".", Position.at(1, 19)),
            new Token(TokenType.PLUS, "+", Position.at(2, 1)),
            new Token(TokenType.MINUS, "-", Position.at(2, 3)),
            new Token(TokenType.SLASH, "/", Position.at(2, 5)),
            new Token(TokenType.STAR, "*", Position.at(2, 7)),
            new Token(TokenType.ASSIGN, "=", Position.at(2, 9)),
            new Token(TokenType.BANG, "!", Position.at(2, 11)),
            new Token(TokenType.LANGLE, "<", Position.at(2, 13)),
            new Token(TokenType.RANGLE, ">", Position.at(2, 15))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_multiCharSymbols() {
        String input = "== != <= >= => .. && ||";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.EQ, "==", Position.at(1, 1)),
            new Token(TokenType.NEQ, "!=", Position.at(1, 4)),
            new Token(TokenType.LTE, "<=", Position.at(1, 7)),
            new Token(TokenType.GTE, ">=", Position.at(1, 10)),
            new Token(TokenType.ARROW, "=>", Position.at(1, 13)),
            new Token(TokenType.DOTDOT, "..", Position.at(1, 16)),
            new Token(TokenType.AND, "&&", Position.at(1, 19)),
            new Token(TokenType.OR, "||", Position.at(1, 22))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_integers() {
        String input = "1 55 155 1.";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.INT, "1", Position.at(1, 1)),
            new Token(TokenType.INT, "55", Position.at(1, 3)),
            new Token(TokenType.INT, "155", Position.at(1, 6)),
            new Token(TokenType.INT, "1", Position.at(1, 10))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_floats() {
        String input = "1.0 5.5 0.155 0.003";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.FLOAT, "1.0", Position.at(1, 1)),
            new Token(TokenType.FLOAT, "5.5", Position.at(1, 5)),
            new Token(TokenType.FLOAT, "0.155", Position.at(1, 9)),
            new Token(TokenType.FLOAT, "0.003", Position.at(1, 15))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_booleans() {
        String input = "true false";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.TRUE, "true", Position.at(1, 1)),
            new Token(TokenType.FALSE, "false", Position.at(1, 6))
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
                        new Token(TokenType.STRING, testCase, Position.at(1, 1))
                    );

                    assertTokensForInput(expectedTokens, input);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testNextToken_stringsWithEscapes() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("\\\"", "\""),
            Pair.of("\\$", "$"),
            Pair.of("\\n", "\n"),
            Pair.of("\\r", "\r"),
            Pair.of("\\t", "\t"),
            Pair.of("\\f", "\f"),
            Pair.of("\\u1215", "ሕ"),
            Pair.of("\\u09A3", "ণ"),
            Pair.of("\\uCAFE", "쫾"),
            Pair.of("\\uBABE", "몾")
        );

        return testCases.stream()
            .map(testCase -> {
                String input = "\"" + testCase.getLeft() + "\"";
                String name = String.format("'%s' should lex to a string token, with literal '%s'", input, testCase);
                return dynamicTest(name, () -> {
                    List<Token> expectedTokens = Lists.newArrayList(
                        new Token(TokenType.STRING, testCase.getRight(), Position.at(1, 1))
                    );

                    assertTokensForInput(expectedTokens, input);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testNextToken_stringsWithSyntaxErrors() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("\"hello world", "Expected \", saw EOF"),
            Pair.of("\"\\u378\"", "Invalid unicode value"),
            Pair.of("\"\\uA37Q\"", "Invalid unicode value")
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String errorMessage = testCase.getRight();

                String name = String.format("Lexing '%s' should raise error with message: '%s'", input, errorMessage);
                return dynamicTest(name, () -> {
                    Lexer l = new Lexer(input);
                    Pair<Token, SyntaxError> t = l.nextToken();
                    assertEquals(errorMessage, t.getRight().message);
                });
            })
            .collect(toList());
    }

    @Test
    public void testNextToken_identifiers() {
        String input = "someVar foo bar fooBar ab1";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.IDENT, "someVar", Position.at(1, 1)),
            new Token(TokenType.IDENT, "foo", Position.at(1, 9)),
            new Token(TokenType.IDENT, "bar", Position.at(1, 13)),
            new Token(TokenType.IDENT, "fooBar", Position.at(1, 17)),
            new Token(TokenType.IDENT, "ab1", Position.at(1, 24))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_keywords() {
        String input = "val var func if else for in";

        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.VAL, "val", Position.at(1, 1)),
            new Token(TokenType.VAR, "var", Position.at(1, 5)),
            new Token(TokenType.FUNCTION, "func", Position.at(1, 9)),
            new Token(TokenType.IF, "if", Position.at(1, 14)),
            new Token(TokenType.ELSE, "else", Position.at(1, 17)),
            new Token(TokenType.FOR, "for", Position.at(1, 22)),
            new Token(TokenType.IN, "in", Position.at(1, 26))
        );
        assertTokensForInput(expectedTokens, input);
    }

    @Test
    public void testNextToken_skipsWhitespaceAndNewlines() {
        String input = "val five = 5\n" +
            "val ten = 10";
        List<Token> expectedTokens = Lists.newArrayList(
            new Token(TokenType.VAL, "val", Position.at(1, 1)),
            new Token(TokenType.IDENT, "five", Position.at(1, 5)),
            new Token(TokenType.ASSIGN, "=", Position.at(1, 10)),
            new Token(TokenType.INT, "5", Position.at(1, 12)),
            new Token(TokenType.VAL, "val", Position.at(2, 1)),
            new Token(TokenType.IDENT, "ten", Position.at(2, 5)),
            new Token(TokenType.ASSIGN, "=", Position.at(2, 9)),
            new Token(TokenType.INT, "10", Position.at(2, 11))
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