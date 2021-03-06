package co.kenrg.mega.backend.evaluation.object;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;

public class NullObj extends Obj {
    public static final NullObj NULL = new NullObj();

    @Override
    public ObjectType getType() {
        return ObjectType.NULL;
    }

    @Override
    public String inspect(int indentLevel) {
        return "nil";
    }
}
