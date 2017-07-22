package co.kenrg.mega.repl.object;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

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
    public String inspect() {
        return "\"" + this.value + "\"";
    }
}
