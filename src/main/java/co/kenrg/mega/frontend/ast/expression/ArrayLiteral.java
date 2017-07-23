package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;
import com.google.common.base.Strings;

public class ArrayLiteral extends Expression {
    public final Token token;
    public final List<Expression> elements;

    public ArrayLiteral(Token token, List<Expression> elements) {
        this.token = token;
        this.elements = elements;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String indentation = Strings.repeat("  ", indentLevel + 1);
        String elements = this.elements.stream()
            .map(el -> el.repr(debug, indentLevel))
            .collect(joining(",\n" + indentation));
        return String.format("[\n%s%s]", indentation, elements);
    }

    @Override
    public Token getToken() {
        return null;
    }
}
