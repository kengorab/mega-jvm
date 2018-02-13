package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnknownModuleError extends TypeCheckerError {
    public final String module;

    public UnknownModuleError(String module, Position position) {
        super(position);
        this.module = module;
    }

    @Override
    public String message() {
        return String.format("Module %s does not exist", this.module);
    }
}
