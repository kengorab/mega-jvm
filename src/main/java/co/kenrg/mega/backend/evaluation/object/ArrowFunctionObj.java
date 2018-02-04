package co.kenrg.mega.backend.evaluation.object;

import static co.kenrg.mega.backend.evaluation.object.iface.ObjectType.FUNCTION;

import java.util.List;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.backend.evaluation.object.iface.InvokeableObj;
import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.iface.Expression;

public class ArrowFunctionObj extends Obj implements InvokeableObj {
    public final ArrowFunctionExpression function;
    public final Environment env;

    public ArrowFunctionObj(ArrowFunctionExpression function, Environment env) {
        this.function = function;
        this.env = env;
    }

    @Override
    public List<Parameter> getParams() {
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
    public String inspect(int indentLevel) {
        return this.function.repr(false, indentLevel);
    }
}
