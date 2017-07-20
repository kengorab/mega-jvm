package co.kenrg.mega.frontend.ast;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;

public class Module implements Node {
    public final List<Statement> statements;

    public Module(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        StringBuilder sb = new StringBuilder();

        for (Statement statement : statements) {
            sb.append(statement.repr(debug, indentLevel));
        }

        return sb.toString();
    }

    @Override
    public Token getToken() {
        return null;
    }
}
