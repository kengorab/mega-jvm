package co.kenrg.mega.frontend.token;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Token {
    public final TokenType type;
    public final String literal;

    public Token(TokenType type, char literal) {
        this.type = type;
        this.literal = Character.toString(literal);
    }

    public Token(TokenType type, String literal) {
        this.type = type;
        this.literal = literal;
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
