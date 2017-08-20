package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import java.util.List;

public class FunctionType extends MegaType {
    public final List<MegaType> paramTypes;
    public final MegaType returnType;

    public FunctionType(List<MegaType> paramTypes, MegaType returnType) {
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    @Override
    public String displayName() {
        String params;
        if (this.paramTypes.size() == 1) {
            params = this.paramTypes.get(0).signature();
        } else {
            String paramList = this.paramTypes.stream()
                .map(MegaType::signature)
                .collect(joining(", "));
            params = String.format("(%s)", paramList);
        }

        return String.format("%s => %s", params, this.returnType.signature());
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        return other instanceof FunctionType
            && ((FunctionType) other).paramTypes.equals(this.paramTypes)
            && ((FunctionType) other).returnType.equals(this.returnType);
    }
}