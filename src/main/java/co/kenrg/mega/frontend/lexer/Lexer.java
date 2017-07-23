package co.kenrg.mega.frontend.lexer;

import static co.kenrg.mega.frontend.token.TokenType.ARROW;
import static co.kenrg.mega.frontend.token.TokenType.ASSIGN;
import static co.kenrg.mega.frontend.token.TokenType.BANG;
import static co.kenrg.mega.frontend.token.TokenType.COMMA;
import static co.kenrg.mega.frontend.token.TokenType.EOF;
import static co.kenrg.mega.frontend.token.TokenType.EQ;
import static co.kenrg.mega.frontend.token.TokenType.FLOAT;
import static co.kenrg.mega.frontend.token.TokenType.GTE;
import static co.kenrg.mega.frontend.token.TokenType.ILLEGAL;
import static co.kenrg.mega.frontend.token.TokenType.INT;
import static co.kenrg.mega.frontend.token.TokenType.LANGLE;
import static co.kenrg.mega.frontend.token.TokenType.LBRACE;
import static co.kenrg.mega.frontend.token.TokenType.LBRACK;
import static co.kenrg.mega.frontend.token.TokenType.LPAREN;
import static co.kenrg.mega.frontend.token.TokenType.LTE;
import static co.kenrg.mega.frontend.token.TokenType.MINUS;
import static co.kenrg.mega.frontend.token.TokenType.NEQ;
import static co.kenrg.mega.frontend.token.TokenType.PLUS;
import static co.kenrg.mega.frontend.token.TokenType.RANGLE;
import static co.kenrg.mega.frontend.token.TokenType.RBRACE;
import static co.kenrg.mega.frontend.token.TokenType.RBRACK;
import static co.kenrg.mega.frontend.token.TokenType.RPAREN;
import static co.kenrg.mega.frontend.token.TokenType.SEMICOLON;
import static co.kenrg.mega.frontend.token.TokenType.SLASH;
import static co.kenrg.mega.frontend.token.TokenType.STAR;
import static co.kenrg.mega.frontend.token.TokenType.STRING;
import static java.lang.Character.isDigit;

import java.util.Map;

import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;

public class Lexer {
    private final String input;
    private int position;
    private int readPosition;
    private char ch;

    private Map<Character, Character> escapes = ImmutableMap.<Character, Character>builder()
        .put('0', '\0')
        .put('n', '\n')
        .put('r', '\r')
        .put('f', '\f')
        .put('t', '\t')
        .build();

    public Lexer(String input) {
        this.input = input;
        this.readChar();    // Initialize lexer by reading first character
    }

    private void readChar() {
        if (this.readPosition >= input.length()) {
            this.ch = 0;
        } else {
            this.ch = this.input.charAt(readPosition);
        }

        this.position = this.readPosition;
        this.readPosition++;
    }

    private char peekChar() {
        if (this.readPosition >= input.length()) {
            return 0;
        }

        return this.input.charAt(this.readPosition);
    }

    public Pair<Token, SyntaxError> nextToken() {
        Token token;
        SyntaxError error = null;

        this.skipWhitespace();

        switch (this.ch) {
            case '=':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(EQ, "==");
                } else if (peekChar() == '>') {
                    this.readChar();
                    token = new Token(ARROW, "=>");
                } else {
                    token = new Token(ASSIGN, this.ch);
                }
                break;
            case '!':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(NEQ, "!=");
                } else {
                    token = new Token(BANG, this.ch);
                }
                break;
            case '+':
                token = new Token(PLUS, this.ch);
                break;
            case '-':
                token = new Token(MINUS, this.ch);
                break;
            case '/':
                token = new Token(SLASH, this.ch);
                break;
            case '*':
                token = new Token(STAR, this.ch);
                break;
            case ';':
                token = new Token(SEMICOLON, this.ch);
                break;
            case ',':
                token = new Token(COMMA, this.ch);
                break;
            case '(':
                token = new Token(LPAREN, this.ch);
                break;
            case ')':
                token = new Token(RPAREN, this.ch);
                break;
            case '{':
                token = new Token(LBRACE, this.ch);
                break;
            case '}':
                token = new Token(RBRACE, this.ch);
                break;
            case '[':
                token = new Token(LBRACK, this.ch);
                break;
            case ']':
                token = new Token(RBRACK, this.ch);
                break;
            case '<':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(LTE, "<=");
                } else {
                    token = new Token(LANGLE, this.ch);
                }
                break;
            case '>':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(GTE, ">=");
                } else {
                    token = new Token(RANGLE, this.ch);
                }
                break;
            case '"':
            case '\'':
                Pair<String, SyntaxError> str = this.readString();
                if (str.getRight() != null) {
                    token = new Token(ILLEGAL, this.ch);
                    error = str.getRight();
                } else {
                    token = new Token(STRING, str.getLeft());
                }
                break;
            case 0:
                token = new Token(EOF, "");
                break;
            default:
                if (Character.isLetter(this.ch)) {
                    String ident = this.readIdentifier();
                    return Pair.of(new Token(TokenType.lookupIdent(ident), ident), null);
                } else if (isDigit(this.ch)) {
                    String number = this.readNumber();
                    if (number.endsWith(".")) {
                        number = number.replace(".", "");
                    }
                    TokenType type = number.contains(".") ? FLOAT : INT;
                    return Pair.of(new Token(type, number), null);
                } else {
                    token = new Token(ILLEGAL, this.ch);
                }
        }

        this.readChar();
        return Pair.of(token, error);
    }

    private String readNumber() {
        boolean isDecimal = false;
        int position = this.position;

        if (isDigit(this.ch)) {
            this.readChar();
        }

        while (isDigit(this.ch) || this.ch == '.') {
            char ch = this.ch;
            if (ch == '.') {
                if (isDecimal) {
                    return this.input.substring(position, this.position);
                }

                char peekCh = this.peekChar();
                if (!isDigit(peekCh)) {
                    this.readChar();
                    return this.input.substring(position, this.position);
                }

                isDecimal = true;
            }

            this.readChar();
        }

        return this.input.substring(position, this.position);
    }

    private String readIdentifier() {
        int position = this.position;

        if (Character.isLetter(this.ch)) {
            this.readChar();
        }

        while (Character.isLetter(this.ch) || Character.isDigit(this.ch)) {
            this.readChar();
        }
        return this.input.substring(position, this.position);
    }

    private Pair<String, SyntaxError> readString() {
        StringBuilder sb = new StringBuilder();
        char quote = this.ch;

        while (true) {
            this.readChar();
            char ch = this.ch;
            if (ch == quote) {
                break;
            }
            if (ch == '\\') {
                char peekCh = this.peekChar();
                switch (peekCh) {
                    case '"':
                    case '\'':
                    case '\\':
                    case '$':
                        this.readChar();
                        ch = this.ch;
                        break;
                    case '0':
                    case 'n':
                    case 'r':
                    case 'f':
                    case 't':
                        this.readChar();
                        ch = escapes.get(this.ch);
                        break;
                    case 'u':
                        this.readChar();

                        int base = 16;
                        int value = 0;
                        for (int length = 4; length > 0; length--) {
                            this.readChar();
                            if (this.ch == '"') {
                                SyntaxError error = new SyntaxError("Invalid unicode value");
                                return Pair.of("", error);
                            }
                            if (this.ch == 0) {
                                SyntaxError error = new SyntaxError(String.format("Expected %c, saw EOF", quote));
                                return Pair.of("", error);
                            }

                            int digitVal = digitValue(this.ch);
                            if (digitVal >= base) {
                                SyntaxError error = new SyntaxError("Invalid unicode value");
                                return Pair.of("", error);
                            }

                            value = value * base + digitVal;
                        }
                        ch = (char) value;
                        break;
                    default:
                        break;
                }
            }
            if (ch == 0) {
                SyntaxError error = new SyntaxError(String.format("Expected %c, saw EOF", quote));
                return Pair.of("", error);
            }

            sb.append(ch);
        }

        return Pair.of(sb.toString(), null);
    }

    private int digitValue(char ch) {
        if ('0' <= ch && ch <= '9') return ch - '0';
        if ('a' <= ch && ch <= 'f') return ch - 'a' + 10;
        if ('A' <= ch && ch <= 'F') return ch - 'A' + 10;
        return 16; // Larger than any legal digit value
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(this.ch)) {
            this.readChar();
        }
    }
}
