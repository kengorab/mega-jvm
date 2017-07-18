package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;
import com.google.common.base.Strings;

public class BlockExpression extends Expression {
    public final Token token;
    public final List<Statement> statements;

    public BlockExpression(Token token, List<Statement> statements) {
        this.token = token;
        this.statements = statements;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String indentation = Strings.repeat("  ", indentLevel + 1);
        String exprReprs = this.statements.stream()
            .map(expr -> indentation + expr.repr(debug, indentLevel + 1))
            .collect(joining("\n"));
        return String.format(
            "{\n%s\n%s}",
            exprReprs,
            Strings.repeat("  ", indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
