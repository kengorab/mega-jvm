package co.kenrg.mega.frontend.typechecking.types;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.joining;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class FunctionType extends MegaType {
    public final List<MegaType> paramTypes;
    @Nullable public final MegaType returnType;

    public FunctionType(List<MegaType> paramTypes, @Nullable MegaType returnType) {
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    public FunctionType(MegaType... typeArgs) {
        List<MegaType> _typeArgs = Arrays.asList(typeArgs);
        this.paramTypes = _typeArgs.subList(0, _typeArgs.size() - 1);
        this.returnType = _typeArgs.get(_typeArgs.size() - 1);
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

        String returnType = (this.returnType == null)
            ? "<Unknown>"   //TODO: Fix leaky <Unknown> abstraction
            : this.returnType.signature();
        return String.format("%s => %s", params, returnType);
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        if (!(other instanceof FunctionType)) {
            return false;
        }
        FunctionType otherType = (FunctionType) other;

        Boolean paramTypesEq = zip(this.paramTypes.stream(), otherType.paramTypes.stream(), Pair::of)
            .map(pair -> pair.getLeft().isEquivalentTo(pair.getRight()))
            .reduce(true, Boolean::logicalAnd);
        boolean returnTypesEq = (this.returnType == null) || this.returnType.isEquivalentTo(otherType.returnType);

        return paramTypesEq && returnTypesEq;
    }
}
