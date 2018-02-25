package co.kenrg.mega.frontend.ast.statement;

import static java.util.stream.Collectors.joining;

import javax.annotation.Nullable;
import java.util.List;

import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.iface.Exportable;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;

public class FunctionDeclarationStatement extends Statement implements Exportable {
    public final Token token;
    public final Identifier name;
    public final List<Parameter> parameters;
    public final Expression body;
    private final boolean hasSingleExpressionBody;
    public final @Nullable String typeAnnotation;
    public final boolean isExported;

    public FunctionDeclarationStatement(Token token, Identifier name, List<Parameter> parameters, Expression body, @Nullable String typeAnnotation, boolean isExported) {
        this.token = token;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.hasSingleExpressionBody = body instanceof BlockExpression;
        this.typeAnnotation = typeAnnotation;
        this.isExported = isExported;
    }

    public FunctionDeclarationStatement(Token token, Identifier name, List<Parameter> parameters, Expression body) {
        this(token, name, parameters, body, null, false);
    }

    public FunctionDeclarationStatement(Token token, Identifier name, List<Parameter> parameters, Expression body, boolean isExported) {
        this(token, name, parameters, body, null, isExported);
    }

    public static FunctionDeclarationStatement mock(String name, List<Parameter> parameters, boolean isExported) {
        Identifier ident = new Identifier(null, name, null);
        return new FunctionDeclarationStatement(null, ident, parameters, null, isExported);
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String params = this.parameters.stream()
            .map(param -> param.repr(debug, indentLevel))
            .collect(joining(", "));

        return String.format(
            "func %s(%s)%s%s%s",
            this.name.value,
            params,
            this.typeAnnotation == null ? "" : String.format(": %s", this.typeAnnotation),
            this.hasSingleExpressionBody ? " = " : " ",
            this.body.repr(debug, indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }

    @Override
    public boolean isExported() {
        return this.isExported;
    }

    @Override
    public String exportName() {
        return this.name.value;
    }
}
