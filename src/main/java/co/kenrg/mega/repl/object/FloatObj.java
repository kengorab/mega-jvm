package co.kenrg.mega.repl.object;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class FloatObj extends Obj {
    public final float value;

    public FloatObj(float value) {
        this.value = value;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.FLOAT;
    }

    @Override
    public String inspect() {
        return String.valueOf(this.value);
    }
}
