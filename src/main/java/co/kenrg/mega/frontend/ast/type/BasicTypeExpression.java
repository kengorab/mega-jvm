package co.kenrg.mega.frontend.ast.type;

import co.kenrg.mega.frontend.token.Position;

public class BasicTypeExpression extends TypeExpression {
    public final String type;

    public BasicTypeExpression(String type, Position position) {
        super(position);
        this.type = type;
    }

    @Override
    public String signature() {
        return this.type;
    }
}
