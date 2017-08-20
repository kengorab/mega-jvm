package co.kenrg.mega.frontend.ast.type;

public class BasicTypeExpression extends TypeExpression {
    public final String type;

    public BasicTypeExpression(String type) {
        this.type = type;
    }

    @Override
    public String signature() {
        return this.type;
    }
}
