package co.kenrg.mega.frontend.ast.statement;

import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;

public class ForLoopStatement extends Statement {
    public final Token token;
    public final Identifier iterator;
    public final Expression iteratee;
    public final BlockExpression block;

    public ForLoopStatement(Token token, Identifier iterator, Expression iteratee, BlockExpression block) {
        this.token = token;
        this.iterator = iterator;
        this.iteratee = iteratee;
        this.block = block;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format(
            "for %s in %s %s",
            this.iterator.value,
            this.iteratee.repr(debug, indentLevel),
            this.block.repr(debug, indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
