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
import static co.kenrg.mega.frontend.token.TokenType.LPAREN;
import static co.kenrg.mega.frontend.token.TokenType.LTE;
import static co.kenrg.mega.frontend.token.TokenType.MINUS;
import static co.kenrg.mega.frontend.token.TokenType.NEQ;
import static co.kenrg.mega.frontend.token.TokenType.PLUS;
import static co.kenrg.mega.frontend.token.TokenType.RANGLE;
import static co.kenrg.mega.frontend.token.TokenType.RBRACE;
import static co.kenrg.mega.frontend.token.TokenType.RPAREN;
import static co.kenrg.mega.frontend.token.TokenType.SEMICOLON;
import static co.kenrg.mega.frontend.token.TokenType.SLASH;
import static co.kenrg.mega.frontend.token.TokenType.STAR;
import static co.kenrg.mega.frontend.token.TokenType.STRING;
import static java.lang.Character.isDigit;

import co.kenrg.mega.frontend.error.SyntaxError;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import org.apache.commons.lang3.tuple.Pair;

public class Lexer {
    private final String input;
    private int position;
    private int readPosition;
    private char ch;

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
        int position = this.position + 1;   // Skip quote char

        while (true) {
            this.readChar();
            if (this.ch == '"') {
                break;
            }
            if (this.ch == 0) {
                SyntaxError error = new SyntaxError("Expected \", saw EOF");
                return Pair.of("", error);
            }
        }

        return Pair.of(this.input.substring(position, this.position), null);
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(this.ch)) {
            this.readChar();
        }
    }
}
