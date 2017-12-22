package co.kenrg.mega.backend.evaluation.object;

import static co.kenrg.mega.backend.evaluation.object.iface.ObjectType.ARRAY;
import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;
import com.google.common.base.Strings;

public class ArrayObj extends Obj {
    public final List<Obj> elems;

    public ArrayObj(List<Obj> elems) {
        this.elems = elems;
    }

    @Override
    public ObjectType getType() {
        return ARRAY;
    }

    @Override
    public String inspect(int indentLevel) {
        String elemsOnOneLine = this.elems.stream()
            .map(el -> el.inspect(indentLevel))
            .collect(joining(", "));
        if (elemsOnOneLine.length() < 80) {
            return String.format("[%s]", elemsOnOneLine);
        }

        String indentation = Strings.repeat("  ", indentLevel + 1);
        String elemsOnMultipleLines = this.elems.stream()
            .map(el -> el.inspect(indentLevel + 1))
            .collect(joining(",\n" + indentation));
        return String.format(
            "[\n%s%s\n%s]",
            indentation,
            elemsOnMultipleLines,
            Strings.repeat("  ", indentLevel)
        );
    }
}
