package co.kenrg.mega.frontend.lexer;

import static co.kenrg.mega.frontend.token.TokenType.AND;
import static co.kenrg.mega.frontend.token.TokenType.ARROW;
import static co.kenrg.mega.frontend.token.TokenType.ASSIGN;
import static co.kenrg.mega.frontend.token.TokenType.BANG;
import static co.kenrg.mega.frontend.token.TokenType.COLON;
import static co.kenrg.mega.frontend.token.TokenType.COMMA;
import static co.kenrg.mega.frontend.token.TokenType.DOT;
import static co.kenrg.mega.frontend.token.TokenType.DOTDOT;
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
import static co.kenrg.mega.frontend.token.TokenType.OR;
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
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;

public class Lexer {
    private final String input;
    private int position;
    private int readPosition;
    private char ch;
    private int line = 1;
    private int col = 0;

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
        this.col++;
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

        Position pos = Position.at(this.line, this.col);

        switch (this.ch) {
            case '=':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(EQ, "==", Position.at(this.line, this.col - 1));
                } else if (peekChar() == '>') {
                    this.readChar();
                    token = new Token(ARROW, "=>", Position.at(this.line, this.col - 1));
                } else {
                    token = new Token(ASSIGN, this.ch, pos);
                }
                break;
            case '!':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(NEQ, "!=", Position.at(this.line, this.col - 1));
                } else {
                    token = new Token(BANG, this.ch, pos);
                }
                break;
            case '&':
                if (peekChar() == '&') {
                    this.readChar();
                    token = new Token(AND, "&&", Position.at(this.line, this.col - 1));
                } else {
                    token = new Token(ILLEGAL, this.ch, pos);
                }
                break;
            case '|':
                if (peekChar() == '|') {
                    this.readChar();
                    token = new Token(OR, "||", Position.at(this.line, this.col - 1));
                } else {
                    token = new Token(ILLEGAL, this.ch, pos);
                }
                break;
            case '+':
                token = new Token(PLUS, this.ch, pos);
                break;
            case '-':
                token = new Token(MINUS, this.ch, pos);
                break;
            case '/':
                token = new Token(SLASH, this.ch, pos);
                break;
            case '*':
                token = new Token(STAR, this.ch, pos);
                break;
            case ';':
                token = new Token(SEMICOLON, this.ch, pos);
                break;
            case ',':
                token = new Token(COMMA, this.ch, pos);
                break;
            case '.':
                if (peekChar() == '.') {
                    this.readChar();
                    token = new Token(DOTDOT, "..", Position.at(this.line, this.col - 1));
                } else {
                    // This is unused (for now)
                    token = new Token(DOT, this.ch, pos);
                }
                break;
            case ':':
                token = new Token(COLON, this.ch, pos);
                break;
            case '(':
                token = new Token(LPAREN, this.ch, pos);
                break;
            case ')':
                token = new Token(RPAREN, this.ch, pos);
                break;
            case '{':
                token = new Token(LBRACE, this.ch, pos);
                break;
            case '}':
                token = new Token(RBRACE, this.ch, pos);
                break;
            case '[':
                token = new Token(LBRACK, this.ch, pos);
                break;
            case ']':
                token = new Token(RBRACK, this.ch, pos);
                break;
            case '<':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(LTE, "<=", Position.at(this.line, this.col - 1));
                } else {
                    token = new Token(LANGLE, this.ch, pos);
                }
                break;
            case '>':
                if (peekChar() == '=') {
                    this.readChar();
                    token = new Token(GTE, ">=", Position.at(this.line, this.col - 1));
                } else {
                    token = new Token(RANGLE, this.ch, pos);
                }
                break;
            case '"':
            case '\'':
                int strStartCol = this.col;
                Pair<String, SyntaxError> str = this.readString();
                pos = Position.at(this.line, strStartCol);
                if (str.getRight() != null) {
                    token = new Token(ILLEGAL, this.ch, pos);
                    error = str.getRight();
                } else {
                    token = new Token(STRING, str.getLeft(), pos);
                }
                break;
            case 0:
                token = new Token(EOF, "", pos);
                break;
            default:
                if (Character.isLetter(this.ch)) {
                    String ident = this.readIdentifier();
                    return Pair.of(new Token(TokenType.lookupIdent(ident), ident, pos), null);
                } else if (isDigit(this.ch)) {
                    Pair<String, TokenType> number = this.readNumber();
                    return Pair.of(new Token(number.getRight(), number.getLeft(), pos), null);
                } else {
                    token = new Token(ILLEGAL, this.ch, pos);
                }
        }

        this.readChar();
        return Pair.of(token, error);
    }

    private Pair<String, TokenType> readNumber() {
        boolean isDecimal = false;
        int position = this.position;

        if (isDigit(this.ch)) {
            this.readChar();
        }

        while (isDigit(this.ch) || this.ch == '.') {
            char ch = this.ch;
            if (ch == '.') {
                if (isDecimal) {
                    return Pair.of(this.input.substring(position, this.position), FLOAT);
                }

                char peekCh = this.peekChar();
                if (peekCh == '.') {
                    // We're in a range operator call; don't consume first dot
                    return Pair.of(this.input.substring(position, this.position), INT);
                } else if (!isDigit(peekCh)) {
                    // We're in a trailing-dot case like `1.`, which should be treated as the integer 1
                    // Consume the trailing dot, but only return the substring containing the number
                    this.readChar();
                    return Pair.of(this.input.substring(position, this.position - 1), INT);
                }

                isDecimal = true;
            }

            this.readChar();
        }

        TokenType type = isDecimal ? FLOAT : INT;
        return Pair.of(this.input.substring(position, this.position), type);
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
            if (this.ch == '\n') {
                this.line++;
                this.col = 0;
            }
            this.readChar();
        }
    }
}
