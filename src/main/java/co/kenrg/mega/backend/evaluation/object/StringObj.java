package co.kenrg.mega.backend.evaluation.object;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;

public class StringObj extends Obj {
    public final String value;

    public StringObj(String value) {
        this.value = value;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.STRING;
    }

    @Override
    public String inspect(int indentLevel) {
        return "\"" + this.value + "\"";
    }
}
