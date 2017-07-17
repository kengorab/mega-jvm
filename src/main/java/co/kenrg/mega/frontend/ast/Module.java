package co.kenrg.mega.frontend.ast;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Statement;

public class Module {
    public final List<Statement> statements;

    public Module(List<Statement> statements) {
        this.statements = statements;
    }


    public String repr(boolean debug, int indentLevel) {
        StringBuilder sb = new StringBuilder();

        for (Statement statement : statements) {
            sb.append(statement.repr(debug, indentLevel));
        }

        return sb.toString();
    }
}
