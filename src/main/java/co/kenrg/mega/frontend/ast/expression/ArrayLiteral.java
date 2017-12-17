package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Stream;

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
        String repr;
        Stream<String> elementsStream = this.elements.stream()
            .map(el -> el.repr(debug, indentLevel));

        String elemsOnOneLine = elementsStream.collect(joining(", "));
        if (elemsOnOneLine.length() < 80) {
            repr = String.format("[%s]", elemsOnOneLine);
        } else {
            String indentation = Strings.repeat("  ", indentLevel + 1);
            String elements = elementsStream.collect(joining(",\n" + indentation));
            repr = String.format("[\n%s%s]", indentation, elements);
        }

        return repr;
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
