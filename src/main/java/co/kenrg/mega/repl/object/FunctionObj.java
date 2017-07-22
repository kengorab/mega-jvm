package co.kenrg.mega.repl.object;

import static co.kenrg.mega.repl.object.iface.ObjectType.FUNCTION;
import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.repl.evaluator.Environment;
import co.kenrg.mega.repl.object.iface.InvokeableObj;
import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class FunctionObj extends Obj implements InvokeableObj {
    public final String name;
    public final List<Identifier> params;
    public final BlockExpression body;
    public final Environment env;

    public FunctionObj(String name, List<Identifier> params, BlockExpression body, Environment env) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.env = env;
    }

    @Override
    public List<Identifier> getParams() {
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
    public String inspect() {
        return String.format(
            "func %s(%s) %s",
            this.name,
            this.params.stream()
                .map(param -> param.repr(false, 0))
                .collect(joining(", ")),
            this.body.repr(false, 0)
        );
    }
}
