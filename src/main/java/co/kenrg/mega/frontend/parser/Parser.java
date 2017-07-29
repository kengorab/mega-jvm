package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.parser.Precedence.LOWEST;
import static co.kenrg.mega.frontend.parser.Precedence.PREFIX;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.IndexExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.ObjectLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.StringInterpolationExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;

public class Parser {
    public final List<SyntaxError> errors = Lists.newArrayList();

    private final Map<TokenType, PrefixParseFunction> prefixParseFns = Maps.newHashMap();
    private final Map<TokenType, InfixParseFunction> infixParseFns = Maps.newHashMap();
    private final Lexer lexer;

    private Token curTok;
    private Token peekTok;
    private Token peekAheadTok;

    public Parser(Lexer lexer) {
        this.lexer = lexer;

        // Read 3 tokens, so curTok, peekTok, and peekAheadTok are all set
        this.nextToken();
        this.nextToken();
        this.nextToken();

        // Register prefix parser functions
        this.registerPrefix(TokenType.IDENT, this::parseIdentifier);
        this.registerPrefix(TokenType.INT, this::parseIntegerLiteral);
        this.registerPrefix(TokenType.FLOAT, this::parseFloatLiteral);
        this.registerPrefix(TokenType.TRUE, this::parseBooleanLiteral);
        this.registerPrefix(TokenType.FALSE, this::parseBooleanLiteral);
        this.registerPrefix(TokenType.STRING, this::parseStringLiteral);
        this.registerPrefix(TokenType.BANG, this::parsePrefixExpression);
        this.registerPrefix(TokenType.MINUS, this::parsePrefixExpression);
        this.registerPrefix(TokenType.LPAREN, this::parseParenExpression);
        this.registerPrefix(TokenType.IF, this::parseIfExpression);
        this.registerPrefix(TokenType.LBRACK, this::parseArrayLiteral);
        this.registerPrefix(TokenType.LBRACE, this::parseObjectLiteral);

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
        this.registerInfix(TokenType.ARROW, this::parseSingleParamArrowFunctionExpression);
        this.registerInfix(TokenType.LPAREN, this::parseCallExpression);
        this.registerInfix(TokenType.LBRACK, this::parseIndexExpression);
    }

    private void addParserError(String message) {
        this.errors.add(new SyntaxError(message));
    }

    private void nextToken() {
        this.curTok = this.peekTok;
        this.peekTok = this.peekAheadTok;

        // Since peekTok is null on initialization, allow for setting peekAheadTok when peekTok is null, since this
        // method will be called 3 times on initialization to saturate all the lookahead tokens.
        if (this.peekTok == null || this.peekTok.type != TokenType.EOF) {
            Pair<Token, SyntaxError> token = this.lexer.nextToken();
            if (token.getRight() != null) {
                this.addParserError(token.getRight().message);
            }
            this.peekAheadTok = token.getLeft();
        }
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

    private boolean peekAheadTokenIs(TokenType tokenType) {
        return this.peekAheadTok.type == tokenType;
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
            case FUNCTION:
                return this.parseFunctionDeclarationStatement();
            default:
                return this.parseExpressionStatement();
        }
    }

    // let <ident> = <expr>
    private Statement parseLetStatement() {
        Token t = this.curTok;  // The 'let' token

        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier name = new Identifier(this.curTok, this.curTok.literal);

        if (!this.expectPeek(TokenType.ASSIGN)) {
            return null;
        }
        this.nextToken();

        Expression expression = this.parseExpression(LOWEST);

        if (this.peekTokenIs(TokenType.SEMICOLON)) {
            this.nextToken();
        }

        return new LetStatement(t, name, expression);
    }

    // func <name>([<param> [, <param>]*]) { <stmts> }
    private Statement parseFunctionDeclarationStatement() {
        Token t = this.curTok;  // The 'func' token

        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier name = new Identifier(this.curTok, this.curTok.literal);

        if (!this.expectPeek(TokenType.LPAREN)) {
            return null;
        }

        List<Identifier> params = this.parseFunctionParameters();

        if (!this.expectPeek(TokenType.LBRACE)) {
            return null;
        }

        BlockExpression body = (BlockExpression) this.parseBlockExpression();
        return new FunctionDeclarationStatement(t, name, params, body);
    }

    // Wrapper to allow top-level expressions
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

    // <ident>
    private Expression parseIdentifier() {
        return new Identifier(this.curTok, this.curTok.literal);
    }

    // <integer>
    private Expression parseIntegerLiteral() {
        Token t = this.curTok;

        int value = Integer.parseInt(this.curTok.literal);
        return new IntegerLiteral(t, value);
    }

    // <number.number>
    private Expression parseFloatLiteral() {
        Token t = this.curTok;

        float value = Float.parseFloat(this.curTok.literal);
        return new FloatLiteral(t, value);
    }

    // [true|false]
    private Expression parseBooleanLiteral() {
        return new BooleanLiteral(this.curTok, this.curTok.type == TokenType.TRUE);
    }

    // "<chars>"
    private Expression parseStringLiteral() {
        Token t = this.curTok;
        String str = this.curTok.literal;

        Pattern interpolationRegex = Pattern.compile("(?:[^\\\\]|^)\\$(\\{([^}]*)}|(\\w*))");
        Matcher m = interpolationRegex.matcher(str);

        List<String> interpolatedExpressions = Lists.newArrayList();
        while (m.find()) {
            interpolatedExpressions.add(m.group(1));
        }
        if (interpolatedExpressions.isEmpty()) {
            return new StringLiteral(this.curTok, this.curTok.literal);
        }
        return parseStringInterpolationExpression(t, str, interpolatedExpressions);
    }

    private Expression parseStringInterpolationExpression(Token token, String str, List<String> interpolatedExpressions) {
        Map<String, Expression> exprs = interpolatedExpressions.stream()
            .collect(toMap(
                exprStr -> "$" + exprStr,
                exprStr -> {
                    // This is a little janky... There's GOT to be a way to make the regex above do this for me
                    if (exprStr.startsWith("{") && exprStr.endsWith("}")) {
                        exprStr = exprStr.substring(1, exprStr.length() - 1);
                    }

                    Parser p = new Parser(new Lexer(exprStr));
                    return p.parseExpression(LOWEST);
                }
            ));
        return new StringInterpolationExpression(token, str, exprs);
    }

    // [[<expr> [,<expr>]*]*]
    public Expression parseArrayLiteral() {
        return new ArrayLiteral(this.curTok, this.parseExpressionList(TokenType.RBRACK));
    }

    private List<Expression> parseExpressionList(TokenType endToken) {
        List<Expression> expressions = Lists.newArrayList();

        if (this.peekTokenIs(endToken)) {
            this.nextToken();
            return expressions;
        }

        this.nextToken();
        expressions.add(this.parseExpression(LOWEST));

        while (this.peekTokenIs(TokenType.COMMA)) {
            this.nextToken();   // Skip ','
            this.nextToken();
            expressions.add(this.parseExpression(LOWEST));
        }

        if (!this.expectPeek(endToken)) {
            return null;
        }
        return expressions;
    }

    // { [<ident>: <expr>] [,<ident>: <expr>]* }
    public Expression parseObjectLiteral() {
        Token t = this.curTok;
        Map<Identifier, Expression> pairs = Maps.newHashMap();

        while (!this.peekTokenIs(TokenType.RBRACE)) {
            this.nextToken();
            Identifier key = (Identifier) this.parseIdentifier();

            if (!this.expectPeek(TokenType.COLON)) {
                return null;
            }
            this.nextToken();

            Expression value = this.parseExpression(LOWEST);
            pairs.put(key, value);

            if (!this.peekTokenIs(TokenType.RBRACE) && !this.expectPeek(TokenType.COMMA)) {
                return null;
            }
        }

        if (!this.expectPeek(TokenType.RBRACE)) {
            return null;
        }

        return new ObjectLiteral(t, pairs);
    }

    // <operator><expr>
    private Expression parsePrefixExpression() {
        Token operator = this.curTok;

        this.nextToken();
        Expression right = this.parseExpression(PREFIX);
        return new PrefixExpression(operator, operator.literal, right);
    }

    // <expr><operator><expr>
    private Expression parseInfixExpression(Expression leftExpr) {
        Token operator = this.curTok;

        Precedence curPrecedence = this.curPrecedence();
        this.nextToken();
        Expression rightExpr = this.parseExpression(curPrecedence);

        return new InfixExpression(operator, operator.literal, leftExpr, rightExpr);
    }

    // (<expr>)
    private Expression parseParenExpression() {
        if (this.peekTokenIs(TokenType.RPAREN) && this.peekAheadTokenIs(TokenType.ARROW) ||
            this.peekTokenIs(TokenType.IDENT) && this.peekAheadTokenIs(TokenType.COMMA) ||
            this.peekTokenIs(TokenType.IDENT) && this.peekAheadTokenIs(TokenType.RPAREN)) {
            return this.parseArrowFunctionExpression();
        }

        this.nextToken();   // Skip '('
        Expression expr = this.parseExpression(LOWEST);

        if (!this.expectPeek(TokenType.RPAREN)) {
            return null;
        }

        return expr;
    }

    // if <condition> <expr> [else <expr>]
    private Expression parseIfExpression() {
        Token t = this.curTok;  // The 'if' token
        this.nextToken();   // Skip 'if'

        Expression condition = this.parseExpression(LOWEST);

        if (!this.expectPeek(TokenType.LBRACE)) {
            return null;
        }

        Expression thenBlock = this.parseBlockExpression();

        Expression elseBlock = null;
        if (this.peekTokenIs(TokenType.ELSE)) {
            this.nextToken();   // Skip 'else'

            if (!this.expectPeek(TokenType.LBRACE)) {
                return null;
            }

            elseBlock = this.parseBlockExpression();
        }

        return new IfExpression(t, condition, thenBlock, elseBlock);
    }

    // { [<stmt> [<stmt>]*] }
    private Expression parseBlockExpression() {
        Token lBrace = this.curTok;
        this.nextToken();   // Skip '{'

        List<Statement> statements = Lists.newArrayList();
        while (!this.curTokenIs(TokenType.RBRACE) && !this.curTokenIs(TokenType.EOF)) {
            Statement stmt = this.parseStatement();
            if (stmt != null) {
                statements.add(stmt);
            }
            this.nextToken();
        }

        return new BlockExpression(lBrace, statements);
    }

    private Expression parseBlockOrSingleExpression() {
        if (this.curTokenIs(TokenType.LBRACE)) {
            return this.parseBlockExpression();
        } else {
            return this.parseExpression(LOWEST);
        }
    }

    // ([<param> [, <param>]*]) => <expr>
    private Expression parseArrowFunctionExpression() {
        Token t = this.curTok;
        List<Identifier> parameters = this.parseFunctionParameters();

        if (!this.expectPeek(TokenType.ARROW)) {
            return null;
        }
        this.nextToken();   // Consume '=>'

        Expression body = this.parseBlockOrSingleExpression();
        return new ArrowFunctionExpression(t, parameters, body);
    }

    // <param> => <expr>
    private Expression parseSingleParamArrowFunctionExpression(Expression leftExpr) {
        if (leftExpr.getToken().type != TokenType.IDENT) {
            addParserError(String.format("Expected %s, saw %s", TokenType.IDENT, leftExpr.getToken().type));
            return null;
        }
        Identifier param = (Identifier) leftExpr;

        this.nextToken();   // Skip '=>'

        Expression body = this.parseBlockOrSingleExpression();
        return new ArrowFunctionExpression(leftExpr.getToken(), Lists.newArrayList(param), body);
    }

    // ([<param> [, <param>]*])
    private List<Identifier> parseFunctionParameters() {
        List<Identifier> params = Lists.newArrayList();
        if (this.peekTokenIs(TokenType.RPAREN)) {
            this.nextToken();
            return params;
        }

        this.nextToken();

        Identifier param1 = new Identifier(this.curTok, this.curTok.literal);
        params.add(param1);

        while (this.peekTokenIs(TokenType.COMMA)) {
            this.nextToken();   // Consume ','
            this.nextToken();
            Identifier param = new Identifier(this.curTok, this.curTok.literal);
            params.add(param);
        }

        if (!this.expectPeek(TokenType.RPAREN)) {
            return null;
        }
        return params;
    }

    // <expr>([<expr> [, <expr>]*])
    private Expression parseCallExpression(Expression leftExpr) {
        return new CallExpression(this.curTok, leftExpr, this.parseExpressionList(TokenType.RPAREN));
    }

    // <expr>[<expr>]
    private Expression parseIndexExpression(Expression leftExpr) {
        Token t = this.curTok;  // The '[' token
        this.nextToken();   // Consume '['

        Expression index = this.parseExpression(LOWEST);
        if (!this.expectPeek(TokenType.RBRACK)) {
            return null;
        }

        return new IndexExpression(t, leftExpr, index);
    }
}
