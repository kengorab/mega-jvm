package co.kenrg.mega.backend.evaluation.object;

import static co.kenrg.mega.backend.evaluation.object.iface.ObjectType.FUNCTION;
import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.backend.evaluation.object.iface.InvokeableObj;
import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.iface.Expression;

public class FunctionObj extends Obj implements InvokeableObj {
    public final String name;
    public final List<Parameter> params;
    public final Expression body;
    public final Environment env;

    public FunctionObj(String name, List<Parameter> params, Expression body, Environment env) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.env = env;
    }

    @Override
    public List<Parameter> getParams() {
        return this.params;
    }

    @Override
    public Environment getEnvironment() {
        return this.env;
    }

    @Override
    public Expression getBody() {
        return this.body;
    }

    @Override
    public ObjectType getType() {
        return FUNCTION;
    }

    @Override
    public String inspect(int indentLevel) {
        return String.format(
            "func %s(%s) %s",
            this.name,
            this.params.stream()
                .map(param -> param.repr(false, indentLevel))
                .collect(joining(", ")),
            this.body.repr(false, indentLevel)
        );
    }
}
