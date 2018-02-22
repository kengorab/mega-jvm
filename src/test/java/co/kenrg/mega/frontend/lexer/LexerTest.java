package co.kenrg.mega.frontend.lexer;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.IntStream;

import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class LexerTest {

    @Test
    void testNextToken_singleCharSymbols() {
        String input = "( ) { } [ ] , ; : . \n" +
            "+ - / * = ! < >";

        List<Token> expectedTokens = Lists.newArrayList(
            Token.lparen(Position.at(1, 1)),
            Token.rparen(Position.at(1, 3)),
            Token.lbrace(Position.at(1, 5)),
            Token.rbrace(Position.at(1, 7)),
            Token.lbrack(Position.at(1, 9)),
            Token.rbrack(Position.at(1, 11)),
            Token.comma(Position.at(1, 13)),
            Token.semicolon(Position.at(1, 15)),
            Token.colon(Position.at(1, 17)),
            Token.dot(Position.at(1, 19)),
            Token.plus(Position.at(2, 1)),
            Token.minus(Position.at(2, 3)),
            Token.slash(Position.at(2, 5)),
            Token.star(Position.at(2, 7)),
            Token.assign(Position.at(2, 9)),
            Token.bang(Position.at(2, 11)),
            Token.langle(Position.at(2, 13)),
            Token.rangle(Position.at(2, 15))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    void testNextToken_multiCharSymbols() {
        String input = "== != <= >= => .. && ||";

        List<Token> expectedTokens = Lists.newArrayList(
            Token.eq(Position.at(1, 1)),
            Token.neq(Position.at(1, 4)),
            Token.lte(Position.at(1, 7)),
            Token.gte(Position.at(1, 10)),
            Token.arrow(Position.at(1, 13)),
            Token.dotdot(Position.at(1, 16)),
            Token.and(Position.at(1, 19)),
            Token.or(Position.at(1, 22))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    void testNextToken_integers() {
        String input = "1 55 155 1.";

        List<Token> expectedTokens = Lists.newArrayList(
            Token._int("1", Position.at(1, 1)),
            Token._int("55", Position.at(1, 3)),
            Token._int("155", Position.at(1, 6)),
            Token._int("1", Position.at(1, 10))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    void testNextToken_floats() {
        String input = "1.0 5.5 0.155 0.003";

        List<Token> expectedTokens = Lists.newArrayList(
            Token._float("1.0", Position.at(1, 1)),
            Token._float("5.5", Position.at(1, 5)),
            Token._float("0.155", Position.at(1, 9)),
            Token._float("0.003", Position.at(1, 15))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    void testNextToken_booleans() {
        String input = "true false";

        List<Token> expectedTokens = Lists.newArrayList(
            Token._true(Position.at(1, 1)),
            Token._false(Position.at(1, 6))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @TestFactory
    List<DynamicTest> testNextToken_strings() {
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
                        Token.string(testCase, Position.at(1, 1))
                    );

                    assertTokensForInput(expectedTokens, input);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testNextToken_stringsWithEscapes() {
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
                        Token.string(testCase.getRight(), Position.at(1, 1))
                    );

                    assertTokensForInput(expectedTokens, input);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testNextToken_stringsWithSyntaxErrors() {
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
    void testNextToken_identifiers() {
        String input = "someVar foo bar fooBar ab1 ABC_DEF _leadingUnderscore";

        List<Token> expectedTokens = Lists.newArrayList(
            Token.ident("someVar", Position.at(1, 1)),
            Token.ident("foo", Position.at(1, 9)),
            Token.ident("bar", Position.at(1, 13)),
            Token.ident("fooBar", Position.at(1, 17)),
            Token.ident("ab1", Position.at(1, 24)),
            Token.ident("ABC_DEF", Position.at(1, 28)),
            Token.ident("_leadingUnderscore", Position.at(1, 36))
        );

        assertTokensForInput(expectedTokens, input);
    }

    @Test
    void testNextToken_keywords() {
        String input = "val var func if else for in type export import from";

        List<Token> expectedTokens = Lists.newArrayList(
            Token.val(Position.at(1, 1)),
            Token.var(Position.at(1, 5)),
            Token.function(Position.at(1, 9)),
            Token._if(Position.at(1, 14)),
            Token._else(Position.at(1, 17)),
            Token._for(Position.at(1, 22)),
            Token.in(Position.at(1, 26)),
            Token.type(Position.at(1, 29)),
            Token.export(Position.at(1, 34)),
            Token._import(Position.at(1, 41)),
            Token.from(Position.at(1, 48))
        );
        assertTokensForInput(expectedTokens, input);
    }

    @Test
    void testNextToken_skipsWhitespaceAndNewlines() {
        String input = "val five = 5\n" +
            "val ten = 10";
        List<Token> expectedTokens = Lists.newArrayList(
            Token.val(Position.at(1, 1)),
            Token.ident("five", Position.at(1, 5)),
            Token.assign(Position.at(1, 10)),
            Token._int("5", Position.at(1, 12)),
            Token.val(Position.at(2, 1)),
            Token.ident("ten", Position.at(2, 5)),
            Token.assign(Position.at(2, 9)),
            Token._int("10", Position.at(2, 11))
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