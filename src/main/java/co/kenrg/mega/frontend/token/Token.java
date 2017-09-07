package co.kenrg.mega.frontend.token;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Token {
    public final TokenType type;
    public final String literal;
    @Nullable public final Position position;

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
