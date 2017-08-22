package co.kenrg.mega.frontend.ast.statement;

import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.token.Token;

public class TypeDeclarationStatement extends Statement {
    public final Token token;
    public final Identifier typeName;
    public final TypeExpression typeExpr;

    public TypeDeclarationStatement(Token token, Identifier typeName, TypeExpression typeExpr) {
        this.token = token;
        this.typeName = typeName;
        this.typeExpr = typeExpr;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format("type %s = %s", this.typeName.value, this.typeExpr.signature());
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
