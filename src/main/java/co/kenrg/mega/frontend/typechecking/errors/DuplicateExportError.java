package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class DuplicateExportError extends TypeCheckerError {
    public final String exportName;

    public DuplicateExportError(String exportName, Position position) {
        super(position);
        this.exportName = exportName;
    }

    @Override
    public String message() {
        return String.format("Duplicate export: %s already exported from this context", this.exportName);
    }
}
