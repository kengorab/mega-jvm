package co.kenrg.mega.frontend.lexer;

import static co.kenrg.mega.frontend.token.TokenType.FLOAT;
import static co.kenrg.mega.frontend.token.TokenType.INT;
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
                    token = Token.eq(pos);
                } else if (peekChar() == '>') {
                    this.readChar();
                    token = Token.arrow(pos);
                } else {
                    token = Token.assign(pos);
                }
                break;
            case '!':
                if (peekChar() == '=') {
                    this.readChar();
                    token = Token.neq(pos);
                } else {
                    token = Token.bang(pos);
                }
                break;
            case '&':
                if (peekChar() == '&') {
                    this.readChar();
                    token = Token.and(pos);
                } else {
                    token = Token.illegal(this.ch, pos);
                }
                break;
            case '|':
                if (peekChar() == '|') {
                    this.readChar();
                    token = Token.or(pos);
                } else {
                    token = Token.illegal(this.ch, pos);
                }
                break;
            case '+':
                token = Token.plus(pos);
                break;
            case '-':
                token = Token.minus(pos);
                break;
            case '/':
                token = Token.slash(pos);
                break;
            case '*':
                token = Token.star(pos);
                break;
            case ';':
                token = Token.semicolon(pos);
                break;
//            case '_':
//                token = Token.underscore(pos);
//                break;
            case ',':
                token = Token.comma(pos);
                break;
            case '.':
                if (peekChar() == '.') {
                    this.readChar();
                    token = Token.dotdot(pos);
                } else {
                    // This is unused (for now)
                    token = Token.dot(pos);
                }
                break;
            case ':':
                token = Token.colon(pos);
                break;
            case '(':
                token = Token.lparen(pos);
                break;
            case ')':
                token = Token.rparen(pos);
                break;
            case '{':
                token = Token.lbrace(pos);
                break;
            case '}':
                token = Token.rbrace(pos);
                break;
            case '[':
                token = Token.lbrack(pos);
                break;
            case ']':
                token = Token.rbrack(pos);
                break;
            case '<':
                if (peekChar() == '=') {
                    this.readChar();
                    token = Token.lte(pos);
                } else {
                    token = Token.langle(pos);
                }
                break;
            case '>':
                if (peekChar() == '=') {
                    this.readChar();
                    token = Token.gte(pos);
                } else {
                    token = Token.rangle(pos);
                }
                break;
            case '"':
            case '\'':
                int strStartCol = this.col;
                Pair<String, SyntaxError> str = this.readString();
                pos = Position.at(this.line, strStartCol);
                if (str.getRight() != null) {
                    token = Token.illegal(this.ch, pos);
                    error = str.getRight();
                } else {
                    token = Token.string(str.getLeft(), pos);
                }
                break;
            case 0:
                token = Token.eof(pos);
                break;
            default:
                if (Character.isLetter(this.ch) || this.ch == '_') {
                    String ident = this.readIdentifier();
                    return Pair.of(new Token(TokenType.lookupIdent(ident), ident, pos), null);
                } else if (isDigit(this.ch)) {
                    Pair<String, TokenType> number = this.readNumber();
                    return Pair.of(new Token(number.getRight(), number.getLeft(), pos), null);
                } else {
                    token = Token.illegal(this.ch, pos);
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

        if (Character.isLetter(this.ch) || this.ch == '_') {
            this.readChar();
        }

        while (Character.isLetter(this.ch) || Character.isDigit(this.ch) || this.ch == '_') {
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
                                SyntaxError error = new SyntaxError("Invalid unicode value", Position.at(this.line, this.col));
                                return Pair.of("", error);
                            }
                            if (this.ch == 0) {
                                SyntaxError error = new SyntaxError(String.format("Expected %c, saw EOF", quote), Position.at(this.line, this.col));
                                return Pair.of("", error);
                            }

                            int digitVal = digitValue(this.ch);
                            if (digitVal >= base) {
                                SyntaxError error = new SyntaxError("Invalid unicode value", Position.at(this.line, this.col));
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
                SyntaxError error = new SyntaxError(String.format("Expected %c, saw EOF", quote), Position.at(this.line, this.col));
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
