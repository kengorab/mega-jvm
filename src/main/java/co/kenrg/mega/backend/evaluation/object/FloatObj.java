package co.kenrg.mega.backend.evaluation.object;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;

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
    public String inspect(int indentLevel) {
        return String.valueOf(this.value);
    }
}
