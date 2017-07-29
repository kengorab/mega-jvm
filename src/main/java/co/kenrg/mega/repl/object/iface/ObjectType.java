package co.kenrg.mega.repl.object.iface;

public enum ObjectType {
    INTEGER,
    FLOAT,
    BOOLEAN,
    STRING,

    FUNCTION,
    ARRAY,
    OBJECT,

    NULL,
    EVAL_ERROR;

    public boolean isNumeric() {
        return this == INTEGER || this == FLOAT;
    }
}
