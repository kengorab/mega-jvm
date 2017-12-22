package co.kenrg.mega.backend.compilation;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.typechecking.TypedNode;
import com.google.common.collect.Lists;

public class Compiler {

    List<String> errors;

    public Compiler() {
        errors = Lists.newArrayList();
    }

    public <T extends TypedNode> void compile(T typedNode) {
        compileNode(typedNode);
    }

    private <T extends TypedNode> void compileNode(T typedNode) {
        Node node = typedNode.node;

        if (node instanceof Module) {
            this.compileStatements(((Module) node).statements);
        }

    }

    private void compileStatements(List<Statement> statements) {
        for (Statement statement : statements) {
//            this.compileNode(statement);
        }
    }
}
