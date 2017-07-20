package co.kenrg.mega.repl.object.iface;

public enum ObjectType {
    INTEGER,
    FLOAT,
    BOOLEAN,

    NULL;

    public boolean isNumeric() {
        return this == INTEGER || this == FLOAT;
    }
}
