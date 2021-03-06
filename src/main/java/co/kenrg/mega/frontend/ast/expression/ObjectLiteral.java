package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map.Entry;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;
import com.google.common.base.Strings;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;

public class ObjectLiteral extends Expression {
    public final Token token;
    public final LinkedHashMultimap<Identifier, Expression> pairs;

    public ObjectLiteral(Token token, LinkedHashMultimap<Identifier, Expression> pairs) {
        this.token = token;
        this.pairs = pairs;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        if (this.pairs.isEmpty()) {
            return "{}";
        }

        List<String> pairs = Lists.newArrayList();
        for (Entry<Identifier, Expression> entry : this.pairs.entries()) {
            pairs.add(String.format(
                "%s: %s",
                entry.getKey().repr(debug, indentLevel),
                entry.getValue().repr(debug, indentLevel + 1)
            ));
        }

        String pairsOnOneLine = pairs.stream().collect(joining(", "));
        if (pairsOnOneLine.length() <= 76 && pairs.size() < 3) { // Counting {, }, and two spaces
            return String.format("{ %s }", pairsOnOneLine);
        }

        String indentation = Strings.repeat("  ", indentLevel + 1);

        return String.format(
            "%s{\n%s%s\n%s}",
            Strings.repeat("  ", indentLevel),
            indentation,
            pairs.stream().collect(joining(",\n" + indentation)),
            Strings.repeat("  ", indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
