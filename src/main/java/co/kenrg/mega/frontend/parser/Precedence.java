package co.kenrg.mega.frontend.parser;

import co.kenrg.mega.frontend.token.TokenType;

enum Precedence {
    LOWEST,
    EQUALS,
    ARROW,
    LESSGREATER,
    SUM,
    PRODUCT,
    PREFIX,
    CALL,
    INDEX;

    public static Precedence forTokenType(TokenType tokenType) {
        switch (tokenType) {
            case EQ:
            case NEQ:
            case ASSIGN:
                return EQUALS;
            case LANGLE:
            case RANGLE:
            case LTE:
            case GTE:
                return LESSGREATER;
            case LBRACK:
                return INDEX;
            case PLUS:
            case MINUS:
                return SUM;
            case SLASH:
            case STAR:
                return PRODUCT;
            case ARROW:
                return ARROW;
            case LPAREN:
                return CALL;
            default:
                return LOWEST;
        }
    }
}


