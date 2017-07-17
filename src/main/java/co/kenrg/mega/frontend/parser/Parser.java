package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.parser.Precedence.LOWEST;
import static co.kenrg.mega.frontend.parser.Precedence.PREFIX;

import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Parser {
    public final List<SyntaxError> errors = Lists.newArrayList();

    private final Map<TokenType, PrefixParseFunction> prefixParseFns = Maps.newHashMap();
    private final Map<TokenType, InfixParseFunction> infixParseFns = Maps.newHashMap();
    private final Lexer lexer;

    private Token curTok;
    private Token peekTok;

    public Parser(Lexer lexer) {
        this.lexer = lexer;

        // Read 2 tokens, so curTok and peekTok are both set
        this.nextToken();
        this.nextToken();

        // Register prefix parser functions
        this.registerPrefix(TokenType.IDENT, this::parseIdentifier);
        this.registerPrefix(TokenType.INT, this::parseIntegerLiteral);
        this.registerPrefix(TokenType.FLOAT, this::parseFloatLiteral);
        this.registerPrefix(TokenType.TRUE, this::parseBooleanLiteral);
        this.registerPrefix(TokenType.FALSE, this::parseBooleanLiteral);
        this.registerPrefix(TokenType.BANG, this::parsePrefixExpression);
        this.registerPrefix(TokenType.MINUS, this::parsePrefixExpression);
        this.registerPrefix(TokenType.LPAREN, this::parseParenExpression);

        // Register infix parser functions
        this.registerInfix(TokenType.PLUS, this::parseInfixExpression);
        this.registerInfix(TokenType.MINUS, this::parseInfixExpression);
        this.registerInfix(TokenType.SLASH, this::parseInfixExpression);
        this.registerInfix(TokenType.STAR, this::parseInfixExpression);
        this.registerInfix(TokenType.EQ, this::parseInfixExpression);
        this.registerInfix(TokenType.NEQ, this::parseInfixExpression);
        this.registerInfix(TokenType.LANGLE, this::parseInfixExpression);
        this.registerInfix(TokenType.LTE, this::parseInfixExpression);
        this.registerInfix(TokenType.RANGLE, this::parseInfixExpression);
        this.registerInfix(TokenType.GTE, this::parseInfixExpression);
    }

    private void addParserError(String message) {
        this.errors.add(new SyntaxError(message));
    }

    private void nextToken() {
        this.curTok = this.peekTok;
        this.peekTok = this.lexer.nextToken();
    }

    private boolean curTokenIs(TokenType tokenType) {
        return this.curTok.type == tokenType;
    }

    private Precedence curPrecedence() {
        return Precedence.forTokenType(this.curTok.type);
    }

    private boolean peekTokenIs(TokenType tokenType) {
        return this.peekTok.type == tokenType;
    }

    private Precedence peekPrecedence() {
        return Precedence.forTokenType(this.peekTok.type);
    }

    private boolean expectPeek(TokenType tokenType) {
        if (this.peekTokenIs(tokenType)) {
            this.nextToken();
            return true;
        } else {
            this.addParserError(String.format("Expected %s, saw %s", tokenType, this.peekTok.type));
            return false;
        }
    }

    private void registerPrefix(TokenType tokenType, PrefixParseFunction fn) {
        this.prefixParseFns.put(tokenType, fn);
    }

    private void registerInfix(TokenType tokenType, InfixParseFunction fn) {
        this.infixParseFns.put(tokenType, fn);
    }

    public Module parseModule() {
        List<Statement> statements = Lists.newArrayList();

        while (this.curTok.type != TokenType.EOF) {
            Statement stmt = this.parseStatement();
            if (stmt != null) {
                statements.add(stmt);
            }
            this.nextToken();
        }

        return new Module(statements);
    }

    private Statement parseStatement() {
        switch (this.curTok.type) {
            case LET:
                return this.parseLetStatement();
            default:
                return this.parseExpressionStatement();
        }
    }

    // let <ident> = <expr>
    private Statement parseLetStatement() {
        Token t = this.curTok;

        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier name = new Identifier(this.curTok, this.curTok.literal);

        if (!this.expectPeek(TokenType.ASSIGN)) {
            return null;
        }
        this.nextToken();

        Expression expression = this.parseExpression(LOWEST);

        while (!this.curTokenIs(TokenType.EOF)) {
            this.nextToken();
        }

        return new LetStatement(t, name, expression);
    }

    private Statement parseExpressionStatement() {
        Token t = this.curTok;

        Expression expr = this.parseExpression(Precedence.LOWEST);

        if (this.peekTokenIs(TokenType.SEMICOLON)) {
            this.nextToken();
        }

        return new ExpressionStatement(t, expr);
    }

    private Expression parseExpression(Precedence precedence) {
        PrefixParseFunction prefixFn = this.prefixParseFns.get(this.curTok.type);
        if (prefixFn == null) {
            this.addParserError(String.format("Unexpected '%s'", this.curTok.type));
            return null;
        }

        Expression leftExpr = prefixFn.get();

        while (!this.peekTokenIs(TokenType.SEMICOLON) && precedence.ordinal() < this.peekPrecedence().ordinal()) {
            InfixParseFunction infixFn = this.infixParseFns.get(this.peekTok.type);
            if (infixFn == null) {
                return leftExpr;
            }

            this.nextToken();

            // "Reduce" together all infix parsing functions
            leftExpr = infixFn.apply(leftExpr);
        }

        return leftExpr;
    }

    private Expression parseIdentifier() {
        return new Identifier(this.curTok, this.curTok.literal);
    }

    private Expression parseIntegerLiteral() {
        Token t = this.curTok;

        int value = Integer.parseInt(this.curTok.literal);
        return new IntegerLiteral(t, value);
    }

    private Expression parseFloatLiteral() {
        Token t = this.curTok;

        float value = Float.parseFloat(this.curTok.literal);
        return new FloatLiteral(t, value);
    }

    private Expression parseBooleanLiteral() {
        return new BooleanLiteral(this.curTok, this.curTok.type == TokenType.TRUE);
    }

    private Expression parsePrefixExpression() {
        Token operator = this.curTok;

        this.nextToken();
        Expression right = this.parseExpression(PREFIX);
        return new PrefixExpression(operator, operator.literal, right);
    }

    private Expression parseInfixExpression(Expression leftExpr) {
        Token operator = this.curTok;

        Precedence curPrecedence = this.curPrecedence();
        this.nextToken();
        Expression rightExpr = this.parseExpression(curPrecedence);

        return new InfixExpression(operator, operator.literal, leftExpr, rightExpr);
    }

    private Expression parseParenExpression() {
        this.nextToken();   // Skip '('

        Expression expr = this.parseExpression(LOWEST);

        if (!this.expectPeek(TokenType.RPAREN)) {
            return null;
        }

        return expr;
    }
}
