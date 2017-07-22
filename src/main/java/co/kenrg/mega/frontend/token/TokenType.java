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

    PLUS("+"),
    MINUS("-"),
    SLASH("/"),
    STAR("*"),

    COMMA(","),
    SEMICOLON(";"),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LANGLE("<"),
    RANGLE(">"),

    ARROW("=>"),

    FUNCTION("FUNCTION"),
    TRUE("TRUE"),
    FALSE("FALSE"),
    IF("IF"),
    ELSE("ELSE"),
    LET("LET");

    public final String literal;

    TokenType(String literal) {
        this.literal = literal;
    }

    private static Map<String, TokenType> KEYWORDS = ImmutableMap.<String, TokenType>builder()
            .put("func", FUNCTION)
            .put("let", LET)
            .put("true", TRUE)
            .put("false", FALSE)
            .put("if", IF)
            .put("else", ELSE)
            .build();

    public static TokenType lookupIdent(String ident) {
        return KEYWORDS.getOrDefault(ident, IDENT);
    }
}
