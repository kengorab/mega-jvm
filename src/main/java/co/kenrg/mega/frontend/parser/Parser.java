package co.kenrg.mega.frontend.parser;

import static co.kenrg.mega.frontend.parser.Precedence.LOWEST;
import static co.kenrg.mega.frontend.parser.Precedence.PREFIX;
import static java.util.stream.Collectors.toMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.AccessorExpression;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.AssignmentExpression;
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
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.expression.ParenthesizedExpression;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.RangeExpression;
import co.kenrg.mega.frontend.ast.expression.StringInterpolationExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Exportable;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ForLoopStatement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ImportStatement;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.ast.type.BasicTypeExpression;
import co.kenrg.mega.frontend.ast.type.FunctionTypeExpression;
import co.kenrg.mega.frontend.ast.type.ParametrizedTypeExpression;
import co.kenrg.mega.frontend.ast.type.StructTypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mega.lang.collections.Arrays;
import org.apache.commons.lang3.tuple.Pair;

public class Parser {
    public final List<SyntaxError> errors = Lists.newArrayList();
    public final List<SyntaxError> warnings = Lists.newArrayList();

    private final Map<TokenType, PrefixParseFunction> prefixParseFns = Maps.newHashMap();
    private final Map<TokenType, InfixParseFunction> infixParseFns = Maps.newHashMap();
    private final Lexer lexer;

    private Token prevTok;
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
        this.registerInfix(TokenType.AND, this::parseInfixExpression);
        this.registerInfix(TokenType.OR, this::parseInfixExpression);
        this.registerInfix(TokenType.ARROW, this::parseSingleParamArrowFunctionExpression);
        this.registerInfix(TokenType.LPAREN, this::parseCallExpression);
        this.registerInfix(TokenType.LBRACK, this::parseIndexExpression);
        this.registerInfix(TokenType.ASSIGN, this::parseAssignmentExpression);
        this.registerInfix(TokenType.DOT, this::parseAccessorExpression);
        this.registerInfix(TokenType.DOTDOT, this::parseRangeExpression);
    }

    private void addParserError(String message, Position position) {
        this.errors.add(new SyntaxError(message, position));
    }

    private void addParserWarning(String message, Position position) {
        this.warnings.add(new SyntaxError(message, position));
    }

    private void nextToken() {
        this.prevTok = this.curTok;
        this.curTok = this.peekTok;
        this.peekTok = this.peekAheadTok;

        // Since peekTok is null on initialization, allow for setting peekAheadTok when peekTok is null, since this
        // method will be called 3 times on initialization to saturate all the lookahead tokens.
        if (this.peekTok == null || this.peekTok.type != TokenType.EOF) {
            Pair<Token, SyntaxError> token = this.lexer.nextToken();
            if (token.getRight() != null) {
                this.addParserError(token.getRight().message, token.getLeft().position);
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

    private boolean expectPeek(TokenType... tokenTypes) {
        String tokenTypesErrPart = tokenTypes.length == 1
            ? tokenTypes[0].toString()
            : "one of " + Arrays.toString(tokenTypes);

        for (TokenType tokenType : tokenTypes) {
            if (this.peekTokenIs(tokenType)) {
                this.nextToken();
                return true;
            }
        }

        this.addParserError(String.format("Expected %s, saw %s", tokenTypesErrPart, this.peekTok.type), this.peekTok.position);
        return false;
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
        List<ImportStatement> imports = Lists.newArrayList();
        List<Statement> exports = Lists.newArrayList();

        while (this.curTok.type != TokenType.EOF) {
            Statement stmt = this.parseStatement();
            if (stmt != null) {
                statements.add(stmt);
            }

            if (stmt instanceof ImportStatement) {
                imports.add((ImportStatement) stmt);
            }

            if (stmt instanceof Exportable) {
                if (((Exportable) stmt).isExported()) {
                    exports.add(stmt);
                }
            }
            this.nextToken();
        }

        return new Module(statements, imports, exports);
    }

    private Statement parseStatement() {
        boolean isExported = false;
        if (this.curTokenIs(TokenType.EXPORT)) {
            isExported = true;
            if (!this.expectPeek(TokenType.VAL, TokenType.VAR, TokenType.FUNCTION, TokenType.TYPE)) {
                return null;
            }
        }

        switch (this.curTok.type) {
            case VAL:
                return this.parseValStatement(isExported);
            case VAR:
                return this.parseVarStatement(isExported);
            case FUNCTION:
                return this.parseFunctionDeclarationStatement(isExported);
            case FOR:
                return this.parseForInLoopStatement();
            case TYPE:
                return this.parseTypeDeclarationStatement(isExported);
            case IMPORT:
                return this.parseImportStatement();
            default:
                return this.parseExpressionStatement();
        }
    }

    // [export] val <ident> = <expr>
    private Statement parseValStatement(boolean isExported) {
        Token t = this.curTok;  // The 'val' token

        Pair<Identifier, Expression> binding = this.parseBinding();
        if (binding == null) {
            return null;
        }
        return new ValStatement(t, binding.getLeft(), binding.getRight(), isExported);
    }

    // [export] var <ident> = <expr>
    private Statement parseVarStatement(boolean isExported) {
        Token t = this.curTok;  // The 'var' token

        Pair<Identifier, Expression> binding = this.parseBinding();
        if (binding == null) {
            return null;
        }
        return new VarStatement(t, binding.getLeft(), binding.getRight(), isExported);
    }

    private Pair<Identifier, Expression> parseBinding() {
        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier name = this.parsePossiblyTypeAnnotatedIdentifier();

        if (!this.expectPeek(TokenType.ASSIGN)) {
            return null;
        }
        this.nextToken();

        Expression expression = this.parseExpression(LOWEST);

        if (this.peekTokenIs(TokenType.SEMICOLON)) {
            this.nextToken();
        }
        return Pair.of(name, expression);
    }

    // <ident>[: <type>]
    private Identifier parsePossiblyTypeAnnotatedIdentifier() {
        Token t = this.curTok;
        String ident = this.curTok.literal;

        if (!this.peekTokenIs(TokenType.COLON)) {
            return new Identifier(t, ident);
        }
        this.nextToken();
        this.nextToken();

        TypeExpression type = this.parseTypeExpression();
        return new Identifier(t, ident, type);
    }

    @Nullable
    private TypeExpression parseTypeExpression() {
        // Type annotations can take one of 6 forms:
        // 1. A single type:                            Int
        // 2. A type w/ type arg(s):                    Array[Int] or Map[String, Int]
        // 3. A function with 1 param (no parens):      Int => String
        // 4. A function with 1 param (w/ parens):      (Int) => String
        // 5. A function with many params:              (Int, String) => String
        // 6. A map describing the structure:           { num: Int, func: (Int, Int) => Int }

        // Handle cases which begin with an identifier (1, 2, 3)
        Token startToken = this.curTok;
        if (this.curTokenIs(TokenType.IDENT)) {
            String baseType = startToken.literal;

            if (this.peekTokenIs(TokenType.LBRACK)) {
                this.nextToken();
                this.nextToken();

                List<TypeExpression> typeArgs = Lists.newArrayList();
                typeArgs.add(this.parseTypeExpression());

                while (this.peekTokenIs(TokenType.COMMA)) {
                    this.nextToken();   // Consume ','
                    this.nextToken();

                    typeArgs.add(this.parseTypeExpression());
                }

                if (!expectPeek(TokenType.RBRACK)) {
                    return null;
                }
                return new ParametrizedTypeExpression(baseType, typeArgs, startToken.position);
            } else if (this.peekTokenIs(TokenType.ARROW)) {
                this.nextToken();
                this.nextToken();

                TypeExpression returnTypeExpr = this.parseTypeExpression();
                return new FunctionTypeExpression(
                    Lists.newArrayList(new BasicTypeExpression(baseType, startToken.position)),
                    returnTypeExpr,
                    startToken.position
                );
            }

            return new BasicTypeExpression(baseType, this.curTok.position);
        }

        // Handle cases which begin with a left paren (4, 5)
        if (this.curTokenIs(TokenType.LPAREN)) {
            this.nextToken();

            List<TypeExpression> typeArgs = Lists.newArrayList();
            if (this.curTokenIs(TokenType.RPAREN)) {
                this.nextToken();
                this.nextToken();
            } else {
                typeArgs.add(this.parseTypeExpression());

                while (this.peekTokenIs(TokenType.COMMA)) {
                    this.nextToken();   // Consume ','
                    this.nextToken();

                    typeArgs.add(this.parseTypeExpression());
                }

                if (!expectPeek(TokenType.RPAREN)) {
                    return null;
                }
                if (!expectPeek(TokenType.ARROW)) {
                    return null;
                }
                this.nextToken();
            }

            TypeExpression returnType = this.parseTypeExpression();
            return new FunctionTypeExpression(typeArgs, returnType, startToken.position);
        }

        if (this.curTokenIs(TokenType.LBRACE)) {
            this.nextToken();

            LinkedHashMultimap<String, TypeExpression> propTypes = LinkedHashMultimap.create();

            Identifier prop = (Identifier) this.parseIdentifier();

            if (!expectPeek(TokenType.COLON)) {
                return null;
            }
            this.nextToken();

            propTypes.put(prop.value, this.parseTypeExpression());

            while (this.peekTokenIs(TokenType.COMMA)) {
                this.nextToken();   // Consume ','
                this.nextToken();

                String propName = ((Identifier) this.parseIdentifier()).value;

                if (!expectPeek(TokenType.COLON)) {
                    return null;
                }
                this.nextToken();

                propTypes.put(propName, this.parseTypeExpression());
            }

            if (!expectPeek(TokenType.RBRACE)) {
                return null;
            }

            StructTypeExpression structTypeExpr = new StructTypeExpression(propTypes, startToken.position);
            if (structTypeExpr.isTooUnwieldy()) {
                this.addParserWarning("Type signature is a bit too verbose, consider defining as a separate type?", startToken.position);
            }
            return structTypeExpr;
        }

        return null;
    }

    // [export] func <name>([<param> [, <param>]*]) [{ <stmts> } | <expr>]
    private Statement parseFunctionDeclarationStatement(boolean isExported) {
        Token t = this.curTok;  // The 'func' token

        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier name = new Identifier(this.curTok, this.curTok.literal);

        if (!this.expectPeek(TokenType.LPAREN)) {
            return null;
        }

        // The parseFunctionParameters method assumes that the leading '(' token has already been consumed
        this.nextToken();
        List<Parameter> params = this.parseFunctionParameters();

        String typeAnnotation = null;
        if (this.peekTokenIs(TokenType.COLON)) {
            this.nextToken();
            this.nextToken();
            //TODO: This should support all type annotations!!
            typeAnnotation = this.curTok.literal;
        }

        if (!this.expectPeek(TokenType.LBRACE, TokenType.ASSIGN)) {
            return null;
        }

        Expression body;
        if (this.curTokenIs(TokenType.LBRACE)) {
            body = this.parseBlockExpression();
        } else if (this.curTokenIs(TokenType.ASSIGN)) {
            this.nextToken(); // Skip '='
            if (this.curTokenIs(TokenType.LBRACE)) {
                this.addParserWarning("Unnecessary equals sign; a function whose single-expression body is a block is pointless", this.prevTok.position);
                body = this.parseBlockExpression();
            } else {
                body = this.parseExpression(LOWEST);
            }
        } else {
            throw new IllegalStateException("There shouldn't be any other possibilities for a function body");
        }

        if (typeAnnotation != null) {
            return new FunctionDeclarationStatement(t, name, params, body, typeAnnotation, isExported);
        }
        return new FunctionDeclarationStatement(t, name, params, body, isExported);
    }

    // for <ident> in <expr> { <stmts> }
    private Statement parseForInLoopStatement() {
        Token t = this.curTok;  // The 'for' token

        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier iterator = new Identifier(this.curTok, this.curTok.literal);

        if (!this.expectPeek(TokenType.IN)) {
            return null;
        }
        this.nextToken();

        Expression iteratee = this.parseExpression(LOWEST);

        if (!this.expectPeek(TokenType.LBRACE)) {
            return null;
        }

        BlockExpression block = (BlockExpression) this.parseBlockExpression();
        return new ForLoopStatement(t, iterator, iteratee, block);
    }

    // [export] type <ident> = <type_expr>
    private Statement parseTypeDeclarationStatement(boolean isExported) {
        Token t = this.curTok;  // The 'type' token

        if (!this.expectPeek(TokenType.IDENT)) {
            return null;
        }

        Identifier typeName = new Identifier(this.curTok, this.curTok.literal);

        if (!this.expectPeek(TokenType.ASSIGN)) {
            return null;
        }
        this.nextToken();

        TypeExpression typeExpr = this.parseTypeExpression();

        return new TypeDeclarationStatement(t, typeName, typeExpr, isExported);
    }

    // import <ident>[, <ident>] from <module_str>
    private Statement parseImportStatement() {
        Token t = this.curTok; // The 'import' token

        if (this.peekTokenIs(TokenType.FROM)) {
            this.addParserError("Invalid imported name: 'from'", this.peekTok.position);
            return null;
        }

        List<Expression> exprs = this.parseExpressionList(TokenType.FROM);
        if (exprs == null) {
            return null;
        }
        List<Identifier> imports = Lists.newArrayList();
        for (Expression expr : exprs) {
            imports.add((Identifier) expr);
        }

        if (!expectPeek(TokenType.STRING)) {
            return null;
        }
        StringLiteral targetModule = (StringLiteral) this.parseStringLiteral();

        return new ImportStatement(t, imports, targetModule);
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
            this.addParserError(String.format("Unexpected '%s'", this.curTok.type), this.curTok.position);
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
    private Expression parseArrayLiteral() {
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

    // { [<ident>: <expr> [,<ident>: <expr>]*] }
    private Expression parseObjectLiteral() {
        Token t = this.curTok;
        LinkedHashMultimap<Identifier, Expression> pairs = LinkedHashMultimap.create();
        List<Pair<Identifier, Expression>> namedExpressionPairs = this.parseNamedExpressionPairs(TokenType.RBRACE);
        if (namedExpressionPairs == null) {
            return null;
        }

        namedExpressionPairs.forEach(pair -> pairs.put(pair.getKey(), pair.getValue()));

        if (!this.expectPeek(TokenType.RBRACE)) {
            return null;
        }

        return new ObjectLiteral(t, pairs);
    }

    private List<Pair<Identifier, Expression>> parseNamedExpressionPairs(TokenType endToken) {
        List<Pair<Identifier, Expression>> pairs = Lists.newArrayList();
        while (!this.peekTokenIs(endToken)) {
            this.nextToken();
            Identifier key = (Identifier) this.parseIdentifier();

            if (!this.expectPeek(TokenType.COLON)) {
                return null;
            }
            this.nextToken();

            Expression value = this.parseExpression(LOWEST);
            pairs.add(Pair.of(key, value));

            if (!this.peekTokenIs(endToken) && !this.expectPeek(TokenType.COMMA)) {
                return null;
            }
        }
        return pairs;
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
        Token t = this.curTok;  // The '(' token
        this.nextToken();   // Consume '('

        // When presented with a '(' token, parse an arrow function under the following conditions:
        //   - Next 2 tokens are ')' and '=>'               Arrow fn w/ no-args
        //   - Next 2 tokens are <ident> and ','            Arrow fn w/ multiple, type-annotation-free args
        //   - Next 2 tokens are <ident> and ':'            Arrow fn w/ at least 1 type-annotated arg
        //   - Next 2 tokens are <ident> and '='            Arrow fn w/ at least 1 arg, which has a default value
        //   - Next 3 tokens are <ident>, ')', and '=>'     Arrow fn with single arg
        if (this.curTokenIs(TokenType.RPAREN) && this.peekTokenIs(TokenType.ARROW) ||
            this.curTokenIs(TokenType.IDENT) && this.peekTokenIs(TokenType.COMMA) ||
            this.curTokenIs(TokenType.IDENT) && this.peekTokenIs(TokenType.COLON) ||
            this.curTokenIs(TokenType.IDENT) && this.peekTokenIs(TokenType.ASSIGN) ||
            this.curTokenIs(TokenType.IDENT) && this.peekTokenIs(TokenType.RPAREN) && this.peekAheadTokenIs(TokenType.ARROW)) {
            return this.parseArrowFunctionExpression();
        }

        Expression expr = this.parseExpression(LOWEST);

        if (!this.expectPeek(TokenType.RPAREN)) {
            return null;
        }

        return new ParenthesizedExpression(t, expr);
    }

    // if <condition> <expr> [else <expr>]
    private Expression parseIfExpression() {
        Token t = this.curTok;  // The 'if' token
        this.nextToken();   // Skip 'if'

        Expression condition = this.parseExpression(LOWEST);

        if (!this.expectPeek(TokenType.LBRACE)) {
            return null;
        }

        BlockExpression thenBlock = (BlockExpression) this.parseBlockExpression();

        BlockExpression elseBlock = null;
        if (this.peekTokenIs(TokenType.ELSE)) {
            this.nextToken();   // Skip 'else'

            if (!this.expectPeek(TokenType.LBRACE)) {
                return null;
            }

            elseBlock = (BlockExpression) this.parseBlockExpression();
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
        // The '(' token was consumed prior to entering this method, in order to simplify the parsing of parenthesized
        // and arrow function expressions.
        Token t = this.prevTok;
        List<Parameter> parameters = this.parseFunctionParameters();

        if (!this.expectPeek(TokenType.ARROW)) {
            return null;
        }
        this.nextToken();   // Consume '=>'

        Expression body = this.parseBlockOrSingleExpression();
        return new ArrowFunctionExpression(t, parameters, body);
    }

    // <param> => <expr>
    private Expression parseSingleParamArrowFunctionExpression(Expression leftExpr) {
        Token token = leftExpr.getToken();
        if (token.type != TokenType.IDENT) {
            addParserError(String.format("Expected %s, saw %s", TokenType.IDENT, token.type), token.position);
            return null;
        }
        Identifier param = (Identifier) leftExpr;
        if (this.peekTokenIs(TokenType.ASSIGN)) {
            return null;
        }

        this.nextToken();   // Skip '=>'

        Expression body = this.parseBlockOrSingleExpression();
        return new ArrowFunctionExpression(token, Lists.newArrayList(new Parameter(param)), body);
    }

    // ([<param>[: <type_ident>] [= <expr>] [, <param>[: <type_ident>] [= <expr>]]*])
    private List<Parameter> parseFunctionParameters() {
        List<Parameter> params = Lists.newArrayList();
        // This method assumes that the leading '(' has already been consumed. This is a consequence of how parenthesized
        // expressions and arrow function expressions are parsed.
        if (this.curTokenIs(TokenType.RPAREN)) {
            return params;
        }

        boolean hasDefaultParamValues = false;

        Identifier param1 = this.parsePossiblyTypeAnnotatedIdentifier();
        if (this.peekTokenIs(TokenType.ASSIGN)) {
            hasDefaultParamValues = true;
            this.nextToken();
            this.nextToken();
            Expression defaultValueExpr = this.parseExpression(LOWEST);
            params.add(new Parameter(param1, defaultValueExpr));
        } else {
            params.add(new Parameter(param1));
        }

        while (this.peekTokenIs(TokenType.COMMA)) {
            this.nextToken();   // Consume ','
            this.nextToken();

            Identifier param = this.parsePossiblyTypeAnnotatedIdentifier();
            if (hasDefaultParamValues) { // Once a param in a list has a default value, subsequent ones need to as well
                if (!this.expectPeek(TokenType.ASSIGN)) {
                    return null;
                }
                this.nextToken();
                Expression defaultValueExpr = this.parseExpression(LOWEST);
                params.add(new Parameter(param, defaultValueExpr));
            } else {
                if (this.peekTokenIs(TokenType.ASSIGN)) {
                    hasDefaultParamValues = true;
                    this.nextToken();
                    this.nextToken();
                    Expression defaultValueExpr = this.parseExpression(LOWEST);
                    params.add(new Parameter(param, defaultValueExpr));
                } else {
                    params.add(new Parameter(param));
                }
            }
        }

        if (!this.expectPeek(TokenType.RPAREN)) {
            return null;
        }
        return params;
    }

    // Unnamed args: <expr>([<expr> [, <expr>]*])
    // Named args: <expr>([<ident>: <expr> [, <ident>: <expr>]*])
    private Expression parseCallExpression(Expression leftExpr) {
        Token t = this.curTok;  // The '(' token
        boolean hasNamedArgs = this.peekAheadTokenIs(TokenType.COLON);
        if (hasNamedArgs) {
            List<Pair<Identifier, Expression>> namedArgs = this.parseNamedExpressionPairs(TokenType.RPAREN);
            CallExpression.NamedArgs callExpression = new CallExpression.NamedArgs(t, leftExpr, namedArgs);
            if (!this.expectPeek(TokenType.RPAREN)) {
                return null;
            }
            return callExpression;
        } else {
            return new CallExpression.UnnamedArgs(t, leftExpr, this.parseExpressionList(TokenType.RPAREN));
        }
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

    // <ident> = <expr>
    private Expression parseAssignmentExpression(Expression leftExpr) {
        Token t = this.curTok;  // The '=' token
        this.nextToken();   // Consume '='

        if (!(leftExpr instanceof Identifier)) {
            this.addParserError(String.format("Expected %s, saw %s", TokenType.IDENT, this.peekTok.type), this.peekTok.position);
            return null;
        }
        Identifier name = (Identifier) leftExpr;

        Expression right = this.parseExpression(LOWEST);
        return new AssignmentExpression(t, name, right);
    }

    // <expr>.<ident>
    private Expression parseAccessorExpression(Expression leftExpr) {
        Token t = this.curTok;  // The '.' token
        this.nextToken();   // Consume '.'

        Identifier identifier = (Identifier) this.parseIdentifier();
        return new AccessorExpression(t, leftExpr, identifier);
    }

    // <expr>..<expr>
    private Expression parseRangeExpression(Expression leftExpr) {
        Token t = this.curTok;  // The '..' token
        this.nextToken();   // Consume '..'

        Expression rightExpr = this.parseExpression(LOWEST);
        return new RangeExpression(t, leftExpr, rightExpr);
    }
}
