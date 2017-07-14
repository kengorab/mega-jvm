package co.kenrg.mega.token;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public enum TokenType {
    ILLEGAL("ILLEGAL"),
    EOF("EOF"),

    IDENT("IDENT"),
    INT("INT"),
    FLOAT("FLOAT"),

    ASSIGN("="),
    PLUS("+"),

    COMMA(","),
    SEMICOLON(";"),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),

    FUNCTION("FUNCTION"),
    LET("LET");

    public final String literal;

    TokenType(String literal) {
        this.literal = literal;
    }

    private static Map<String, TokenType> KEYWORDS = ImmutableMap.of(
            "fn", FUNCTION,
            "let", LET
    );

    public static TokenType lookupIdent(String ident) {
        return KEYWORDS.getOrDefault(ident, IDENT);
    }
}
