package co.kenrg.mega.backend.evaluation.object;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;

public class BooleanObj extends Obj {
    public final static BooleanObj TRUE = new BooleanObj(true);
    public final static BooleanObj FALSE = new BooleanObj(false);

    public final boolean value;

    // Use the static initializer BooleanObj.of(boolean)
    private BooleanObj(boolean value) {
        this.value = value;
    }

    public static BooleanObj of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.BOOLEAN;
    }

    @Override
    public String inspect(int indentLevel) {
        return String.valueOf(this.value);
    }
}
