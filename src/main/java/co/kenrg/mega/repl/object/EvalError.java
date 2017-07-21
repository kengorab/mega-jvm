package co.kenrg.mega.repl.object;

import static co.kenrg.mega.repl.object.iface.ObjectType.EVAL_ERROR;

import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class EvalError extends Obj {
    public final String message;

    public EvalError(String message) {
        this.message = message;
    }

    public static EvalError unknownPrefixOperator(String operator, Obj right) {
        return new EvalError(String.format("unknown operator: %s%s", operator, right.getType()));
    }

    public static EvalError unknownInfixOperator(String operator, Obj left, Obj right) {
        return new EvalError(String.format("unknown operator: %s %s %s", left.getType(), operator, right.getType()));
    }

    @Override
    public ObjectType getType() {
        return EVAL_ERROR;
    }

    @Override
    public String inspect() {
        return "Error: " + this.message;
    }
}
