package co.kenrg.mega.repl.object;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class NullObj extends Obj {
    public static final NullObj NULL = new NullObj();

    @Override
    public ObjectType getType() {
        return ObjectType.NULL;
    }

    @Override
    public String inspect() {
        return "nil";
    }
}
