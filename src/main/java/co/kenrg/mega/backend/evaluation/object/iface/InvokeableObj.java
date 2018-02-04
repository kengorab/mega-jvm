package co.kenrg.mega.backend.evaluation.object.iface;

import java.util.List;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.iface.Expression;

public interface InvokeableObj {
    List<Parameter> getParams();
    Environment getEnvironment();
    Expression getBody();
}
