package co.kenrg.mega.repl.object.iface;

import java.util.List;

import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.repl.evaluator.Environment;

public interface InvokeableObj {
    List<Identifier> getParams();
    Environment getEnvironment();
    Expression getBody();
}
