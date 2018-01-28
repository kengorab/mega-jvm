package co.kenrg.mega.frontend.ast.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

class ArrowFunctionExpressionTest {

    private Token lparenToken = new Token(TokenType.LPAREN, "(");
    private Token lbraceToken = new Token(TokenType.LBRACE, "{");

    private Token identifierToken(String name) {
        return new Token(TokenType.IDENT, name);
    }

    private Identifier identifier(String name) {
        return new Identifier(new Token(TokenType.IDENT, name), name);
    }

    @Test
    void testRepr_noParams() {
        ArrowFunctionExpression arrowFunction = new ArrowFunctionExpression(
            lparenToken,
            Lists.newArrayList(),
            new BlockExpression(
                lbraceToken,
                Lists.newArrayList(
                    new ExpressionStatement(identifierToken("a"), identifier("a"))
                )
            )
        );
        String expected = "() => { a }";
        assertEquals(expected, arrowFunction.repr(false, 0));
    }

    @Test
    void testRepr_oneParam() {
        ArrowFunctionExpression arrowFunction = new ArrowFunctionExpression(
            lparenToken,
            Lists.newArrayList(
                new Parameter(identifier("a"))
            ),
            new BlockExpression(
                lbraceToken,
                Lists.newArrayList(
                    new ExpressionStatement(identifierToken("a"), identifier("a"))
                )
            )
        );
        String expected = "a => { a }";
        assertEquals(expected, arrowFunction.repr(false, 0));
    }

    @Test
    void testRepr_multipleParameters() {
        ArrowFunctionExpression arrowFunction = new ArrowFunctionExpression(
            lparenToken,
            Lists.newArrayList(
                new Parameter(identifier("a")),
                new Parameter(identifier("b"))
            ),
            new BlockExpression(
                lbraceToken,
                Lists.newArrayList(
                    new ExpressionStatement(identifierToken("a"), identifier("a"))
                )
            )
        );
        String expected = "(a, b) => { a }";
        assertEquals(expected, arrowFunction.repr(false, 0));
    }

    @Test
    void testRepr_multipleParameters_bothHaveDefaultValues() {
        ArrowFunctionExpression arrowFunction = new ArrowFunctionExpression(
            lparenToken,
            Lists.newArrayList(
                new Parameter(identifier("a"), new IntegerLiteral(Token._int("1", null), 1)),
                new Parameter(identifier("b"), new StringLiteral(Token.string("abc", null), "abc"))
            ),
            new BlockExpression(
                lbraceToken,
                Lists.newArrayList(
                    new ExpressionStatement(identifierToken("a"), identifier("a"))
                )
            )
        );
        String expected = "(a = 1, b = \"abc\") => { a }";
        assertEquals(expected, arrowFunction.repr(false, 0));
    }

    @Test
    void testRepr_multipleParameters_onlyLastHasDefaultValue() {
        ArrowFunctionExpression arrowFunction = new ArrowFunctionExpression(
            lparenToken,
            Lists.newArrayList(
                new Parameter(identifier("a")),
                new Parameter(identifier("b"), new IntegerLiteral(Token._int("1", null), 1))
            ),
            new BlockExpression(
                lbraceToken,
                Lists.newArrayList(
                    new ExpressionStatement(identifierToken("a"), identifier("a"))
                )
            )
        );
        String expected = "(a, b = 1) => { a }";
        assertEquals(expected, arrowFunction.repr(false, 0));
    }

    @Test
    void testRepr_multipleParameters_singleExpressionBody() {
        ArrowFunctionExpression arrowFunction = new ArrowFunctionExpression(
            lparenToken,
            Lists.newArrayList(
                new Parameter(identifier("a")),
                new Parameter(identifier("b"))
            ),
            new InfixExpression(
                new Token(TokenType.PLUS, "+"),
                "+",
                identifier("a"),
                identifier("b")
            )
        );
        String expected = "(a, b) => a + b";
        assertEquals(expected, arrowFunction.repr(false, 0));
    }
}
