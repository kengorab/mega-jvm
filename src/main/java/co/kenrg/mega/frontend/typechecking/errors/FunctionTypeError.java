package co.kenrg.mega.frontend.typechecking.errors;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class FunctionTypeError extends TypeCheckerError {
    public final List<MegaType> expectedParamTypes;
    public final List<MegaType> actualParamTypes;

    public FunctionTypeError(List<MegaType> expectedParamTypes, List<MegaType> actualParamTypes) {
        this.expectedParamTypes = expectedParamTypes;
        this.actualParamTypes = actualParamTypes;
    }

    @Override
    public String message() {
        return String.format(
            "Cannot invoke function; expected arguments: %s, got %s",
            expectedParamTypes.stream().map(MegaType::signature).collect(joining(", ", "(", ")")),
            actualParamTypes.stream().map(MegaType::signature).collect(joining(", ", "(", ")"))
        );
    }
}
