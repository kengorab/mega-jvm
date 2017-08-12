package co.kenrg.mega.frontend.ast.statement;

import static java.util.stream.Collectors.joining;

import javax.annotation.Nullable;
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
    public final @Nullable String typeAnnotation;

    public FunctionDeclarationStatement(Token token, Identifier name, List<Identifier> parameters, BlockExpression body) {
        this.token = token;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.typeAnnotation = null;
    }

    public FunctionDeclarationStatement(Token token, Identifier name, List<Identifier> parameters, BlockExpression body, String typeAnnotation) {
        this.token = token;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.typeAnnotation = typeAnnotation;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String params = this.parameters.stream()
            .map(param -> param.repr(debug, indentLevel))
            .collect(joining(", "));

        return String.format(
            "func %s(%s)%s %s",
            this.name.value,
            params,
            this.typeAnnotation == null ? "" : String.format(": %s", this.typeAnnotation),
            this.body.repr(debug, indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
