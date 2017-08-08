package co.kenrg.mega.repl.object;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class IntegerObj extends Obj {
    public final int value;

    public IntegerObj(int value) {
        this.value = value;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.INTEGER;
    }

    @Override
    public String inspect(int indentLevel) {
        return String.valueOf(this.value);
    }
}
