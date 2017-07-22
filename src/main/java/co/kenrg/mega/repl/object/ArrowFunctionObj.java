package co.kenrg.mega.repl.object;

import static co.kenrg.mega.repl.object.iface.ObjectType.FUNCTION;

import java.util.List;

import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.repl.evaluator.Environment;
import co.kenrg.mega.repl.object.iface.InvokeableObj;
import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class ArrowFunctionObj extends Obj implements InvokeableObj {
    public final ArrowFunctionExpression function;
    public final Environment env;

    public ArrowFunctionObj(ArrowFunctionExpression function, Environment env) {
        this.function = function;
        this.env = env;
    }

    @Override
    public List<Identifier> getParams() {
        return this.function.parameters;
    }

    @Override
    public Environment getEnvironment() {
        return this.env;
    }

    @Override
    public Expression getBody() {
        return this.function.body;
    }

    @Override
    public ObjectType getType() {
        return FUNCTION;
    }

    @Override
    public String inspect() {
        return this.function.repr(false, 0);
    }
}
