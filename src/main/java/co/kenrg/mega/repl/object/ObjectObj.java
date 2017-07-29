package co.kenrg.mega.repl.object;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

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
    public String inspect() {
        if (this.pairs.isEmpty()) {
            return "{}";
        }

        List<String> pairs = Lists.newArrayList();
        for (Map.Entry<String, Obj> pair : this.pairs.entrySet()) {
            pairs.add(String.format(
                "%s: %s",
                pair.getKey(),
                pair.getValue().inspect()
            ));
        }

        String pairsOnOneLine = pairs.stream().collect(joining(", "));
        if (pairsOnOneLine.length() <= 76 && pairs.size() < 3) { // Counting {, }, and two spaces
            return String.format("{ %s }", pairsOnOneLine);
        }

        String indentation = Strings.repeat("  ", 1);

        return String.format(
            "%s{\n%s%s\n%s}",
            Strings.repeat("  ", 0),
            indentation,
            pairs.stream().collect(joining(",\n" + indentation)),
            Strings.repeat("  ", 0)
        );
    }
}
