package co.kenrg.mega.frontend.token;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Token {
    public final TokenType type;
    public final String literal;
    @Nullable public Position position;

    public Token(TokenType type, char literal) {
        this.type = type;
        this.literal = Character.toString(literal);
        this.position = null;
    }

    public Token(TokenType type, char literal, @Nullable Position position) {
        this.type = type;
        this.literal = Character.toString(literal);
        this.position = position;
    }

    public Token(TokenType type, String literal) {
        this.type = type;
        this.literal = literal;
        this.position = null;
    }

    public Token(TokenType type, String literal, @Nullable Position position) {
        this.type = type;
        this.literal = literal;
        this.position = position;
    }

    public static Token illegal(char literal, Position position) {
        return new Token(TokenType.ILLEGAL, literal, position);
    }

    public static Token eof(Position position) {
        return new Token(TokenType.EOF, "", position);
    }

    public static Token ident(String literal, Position position) {
        return new Token(TokenType.IDENT, literal, position);
    }

    public static Token _int(String literal, Position position) {
        return new Token(TokenType.INT, literal, position);
    }

    public static Token _float(String literal, Position position) {
        return new Token(TokenType.FLOAT, literal, position);
    }

    public static Token string(String literal, Position position) {
        return new Token(TokenType.STRING, literal, position);
    }

    public static Token assign(Position position) {
        return new Token(TokenType.ASSIGN, '=', position);
    }

    public static Token bang(Position position) {
        return new Token(TokenType.BANG, '!', position);
    }

    public static Token eq(Position position) {
        return new Token(TokenType.EQ, "==", position);
    }

    public static Token neq(Position position) {
        return new Token(TokenType.NEQ, "!=", position);
    }

    public static Token gte(Position position) {
        return new Token(TokenType.GTE, ">=", position);
    }

    public static Token lte(Position position) {
        return new Token(TokenType.LTE, "<=", position);
    }

    public static Token and(Position position) {
        return new Token(TokenType.AND, "&&", position);
    }

    public static Token or(Position position) {
        return new Token(TokenType.OR, "||", position);
    }

    public static Token dotdot(Position position) {
        return new Token(TokenType.DOTDOT, "..", position);
    }

    public static Token arrow(Position position) {
        return new Token(TokenType.ARROW, "=>", position);
    }

    public static Token plus(Position position) {
        return new Token(TokenType.PLUS, '+', position);
    }

    public static Token minus(Position position) {
        return new Token(TokenType.MINUS, '-', position);
    }

    public static Token slash(Position position) {
        return new Token(TokenType.SLASH, '/', position);
    }

    public static Token star(Position position) {
        return new Token(TokenType.STAR, '*', position);
    }

    public static Token semicolon(Position position) {
        return new Token(TokenType.SEMICOLON, ';', position);
    }

    public static Token comma(Position position) {
        return new Token(TokenType.COMMA, ',', position);
    }

    public static Token dot(Position position) {
        return new Token(TokenType.DOT, '.', position);
    }

    public static Token colon(Position position) {
        return new Token(TokenType.COLON, ':', position);
    }

    public static Token lparen(Position position) {
        return new Token(TokenType.LPAREN, '(', position);
    }

    public static Token rparen(Position position) {
        return new Token(TokenType.RPAREN, ')', position);
    }

    public static Token lbrace(Position position) {
        return new Token(TokenType.LBRACE, '{', position);
    }

    public static Token rbrace(Position position) {
        return new Token(TokenType.RBRACE, '}', position);
    }

    public static Token langle(Position position) {
        return new Token(TokenType.LANGLE, '<', position);
    }

    public static Token rangle(Position position) {
        return new Token(TokenType.RANGLE, '>', position);
    }

    public static Token lbrack(Position position) {
        return new Token(TokenType.LBRACK, '[', position);
    }

    public static Token rbrack(Position position) {
        return new Token(TokenType.RBRACK, ']', position);
    }

    public static Token function(Position position) {
        return new Token(TokenType.FUNCTION, "func", position);
    }

    public static Token _true(Position position) {
        return new Token(TokenType.TRUE, "true", position);
    }

    public static Token _false(Position position) {
        return new Token(TokenType.FALSE, "false", position);
    }

    public static Token _if(Position position) {
        return new Token(TokenType.IF, "if", position);
    }

    public static Token _else(Position position) {
        return new Token(TokenType.ELSE, "else", position);
    }

    public static Token val(Position position) {
        return new Token(TokenType.VAL, "val", position);
    }

    public static Token var(Position position) {
        return new Token(TokenType.VAR, "var", position);
    }

    public static Token _for(Position position) {
        return new Token(TokenType.FOR, "for", position);
    }

    public static Token in(Position position) {
        return new Token(TokenType.IN, "in", position);
    }

    public static Token type(Position position) {
        return new Token(TokenType.TYPE, "type", position);
    }

    public static Token export(Position position) {
        return new Token(TokenType.EXPORT, "export", position);
    }

    public static Token _import(Position position) {
        return new Token(TokenType.IMPORT, "import", position);
    }

    public static Token from(Position position) {
        return new Token(TokenType.FROM, "from", position);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
