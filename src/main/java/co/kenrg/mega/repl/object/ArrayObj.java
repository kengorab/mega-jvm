package co.kenrg.mega.repl.object;

import static co.kenrg.mega.repl.object.iface.ObjectType.ARRAY;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Stream;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

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
    public String inspect() {
        Stream<String> elemsStream = this.elems.stream().map(Obj::inspect);
        String elems = elemsStream.collect(joining(", "));
        return String.format("[%s]", elems);
    }
}
