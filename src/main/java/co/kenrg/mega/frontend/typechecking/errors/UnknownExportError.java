package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnknownExportError extends TypeCheckerError {
    public final String module;
    public final String identName;

    public UnknownExportError(String module, String identName, Position position) {
        super(position);
        this.module = module;
        this.identName = identName;
    }

    @Override
    public String message() {
        return String.format("Module %s has no export %s", this.module, this.identName);
    }
}
