package co.kenrg.mega.repl.object;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class BooleanObj extends Obj {
    public final static BooleanObj TRUE = new BooleanObj(true);
    public final static BooleanObj FALSE = new BooleanObj(false);

    public final boolean value;

    public BooleanObj(boolean value) {
        this.value = value;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.BOOLEAN;
    }

    @Override
    public String inspect() {
        return String.valueOf(this.value);
    }
}
