package co.kenrg.mega.frontend.token;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public enum TokenType {
    ILLEGAL("ILLEGAL"),
    EOF("EOF"),

    IDENT("IDENT"),
    INT("INT"),
    FLOAT("FLOAT"),
    STRING("STRING"),

    ASSIGN("="),
    BANG("!"),
    EQ("=="),
    NEQ("!="),
    GTE(">="),
    LTE("<="),
    AND("&&"),
    OR("||"),
    DOTDOT(".."),
    ARROW("=>"),

    PLUS("+"),
    MINUS("-"),
    SLASH("/"),
    STAR("*"),

    COMMA(","),
    SEMICOLON(";"),
    COLON(":"),
    DOT("."),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LANGLE("<"),
    RANGLE(">"),
    LBRACK("["),
    RBRACK("]"),

    // Keywords
    FUNCTION("FUNCTION"),
    TRUE("TRUE"),
    FALSE("FALSE"),
    IF("IF"),
    ELSE("ELSE"),
    VAL("VAL"),
    VAR("VAR"),
    FOR("FOR"),
    IN("IN"),
    TYPE("TYPE"),
    EXPORT("EXPORT");

    public final String literal;

    TokenType(String literal) {
        this.literal = literal;
    }

    private static Map<String, TokenType> KEYWORDS = ImmutableMap.<String, TokenType>builder()
        .put("func", FUNCTION)
        .put("val", VAL)
        .put("var", VAR)
        .put("true", TRUE)
        .put("false", FALSE)
        .put("if", IF)
        .put("else", ELSE)
        .put("for", FOR)
        .put("in", IN)
        .put("type", TYPE)
        .put("export", EXPORT)
        .build();

    public static TokenType lookupIdent(String ident) {
        return KEYWORDS.getOrDefault(ident, IDENT);
    }
}
