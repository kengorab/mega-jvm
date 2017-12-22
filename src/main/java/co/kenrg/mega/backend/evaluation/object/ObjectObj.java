package co.kenrg.mega.backend.evaluation.object;

import static java.util.stream.Collectors.joining;

import java.util.Map;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;
import com.google.common.base.Strings;

public class ObjectObj extends Obj {
    public final Map<String, Obj> pairs;

    public ObjectObj(Map<String, Obj> pairs) {
        this.pairs = pairs;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.OBJECT;
    }

    @Override
    public String inspect(int indentLevel) {
        if (this.pairs.isEmpty()) {
            return "{}";
        }

        String pairsOnOneLine = this.pairs.entrySet().stream()
            .map(pair -> String.format("%s: %s", pair.getKey(), pair.getValue().inspect(0)))
            .collect(joining(", "));
        if (pairsOnOneLine.length() <= 76 && pairs.size() < 3) { // Counting {, }, and two spaces
            return String.format("{ %s }", pairsOnOneLine);
        }

        String indentation = Strings.repeat("  ", indentLevel + 1);

        String pairsOnMultipleLines = this.pairs.entrySet().stream()
            .map(pair -> String.format("%s: %s", pair.getKey(), pair.getValue().inspect(indentLevel + 1)))
            .collect(joining(",\n" + indentation));
        return String.format(
            "{\n%s%s\n%s}",
            indentation,
            pairsOnMultipleLines,
            Strings.repeat("  ", indentLevel)
        );
    }
}
