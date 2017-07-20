package co.kenrg.mega.frontend.ast.statement;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;

public class FunctionDeclarationStatement extends Statement {
    public final Token token;
    public final Identifier name;
    public final List<Identifier> parameters;
    public final BlockExpression body;

    public FunctionDeclarationStatement(Token token, Identifier name, List<Identifier> parameters, BlockExpression body) {
        this.token = token;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String params = this.parameters.stream()
            .map(param -> param.repr(debug, indentLevel))
            .collect(joining(", "));

        return String.format(
            "func %s(%s) %s",
            this.name.value,
            params,
            this.body.repr(debug, indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
