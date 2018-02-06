package co.kenrg.mega.backend.compilation.scope;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Objects;

import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.AssignmentExpression;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.ObjectLiteral;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import com.google.common.collect.Lists;

public class Context {
    private class ContextFrame {
        Node node;
        Integer numLambdas;

        ContextFrame(Node node, Integer numLambdas) {
            this.node = node;
            this.numLambdas = numLambdas;
        }
    }

    private List<ContextFrame> contextFrames = Lists.newArrayList();

    public void pushContext(Node node) {
        this.contextFrames.add(new ContextFrame(node, 0));
    }

    public void incLambdaCountOfPreviousContext() {
        int index = this.contextFrames.size() - 2;
        ContextFrame previousFrame = this.contextFrames.get(index);
        previousFrame.numLambdas++;
    }

    public void popContext() {
        this.contextFrames.remove(this.contextFrames.size() - 1);
    }

    public String getLambdaName() {
        return this.contextFrames.stream()
            .map(frame -> {
                Node node = frame.node;

                if (node instanceof ValStatement) {
                    return ((ValStatement) node).name.value;
                } else if (node instanceof VarStatement) {
                    return ((VarStatement) node).name.value;
                } else if (node instanceof FunctionDeclarationStatement) {
                    return ((FunctionDeclarationStatement) node).name.value;
                }

                if (node instanceof ArrayLiteral) {
                    return frame.numLambdas.toString();
                } else if (node instanceof ObjectLiteral) {
                    return frame.numLambdas.toString();
                } else if (node instanceof IfExpression) {
                    return frame.numLambdas.toString();
                } else if (node instanceof AssignmentExpression) {
                    return ((AssignmentExpression) node).name.value;
                } else if (node instanceof CallExpression) {
                    return frame.numLambdas.toString();
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(joining("$"));
    }
}
