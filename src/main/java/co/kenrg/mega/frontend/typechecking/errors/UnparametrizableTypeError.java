package co.kenrg.mega.frontend.typechecking.errors;

public class UnparametrizableTypeError extends TypeCheckerError {
    public final String typeName;

    public UnparametrizableTypeError(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String message() {
        return String.format("Type %s does not accept type arguments", this.typeName);
    }
}
