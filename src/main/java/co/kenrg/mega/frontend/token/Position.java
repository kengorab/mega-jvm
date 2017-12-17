package co.kenrg.mega.frontend.token;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Position {
    public final int line;
    public final int col;

    private Position(int line, int col) {
        this.line = line;
        this.col = col;
    }

    public static Position at(int line, int col) {
        return new Position(line, col);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", this.line, this.col);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
