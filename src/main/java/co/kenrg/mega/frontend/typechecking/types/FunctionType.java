package co.kenrg.mega.frontend.typechecking.types;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.joining;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.TypeEnvironment.Binding;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import mega.lang.functions.Function0;
import mega.lang.functions.Function1;
import mega.lang.functions.Function2;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

public class FunctionType extends MegaType {
    public final List<MegaType> paramTypes;
    @Nullable public final MegaType returnType;
    public Map<String, Binding> capturedBindings;

    public FunctionType(List<MegaType> paramTypes, @Nullable MegaType returnType, Map<String, Binding> capturedBindings) {
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.capturedBindings = capturedBindings;
    }

    public FunctionType(List<MegaType> paramTypes, @Nullable MegaType returnType) {
        this(paramTypes, returnType, Maps.newHashMap());
    }

    @VisibleForTesting
    public FunctionType(MegaType... typeArgs) {
        List<MegaType> typeArgsList = Arrays.asList(typeArgs);
        this.paramTypes = typeArgsList.subList(0, typeArgsList.size() - 1);
        this.returnType = typeArgsList.get(typeArgsList.size() - 1);
        this.capturedBindings = Maps.newHashMap();
    }

    public int arity() {
        return this.paramTypes.size();
    }

    @Override
    public Class typeClass() {
        int arity = this.arity();
        switch (arity) {
            case 0:
                return Function0.class;
            case 1:
                return Function1.class;
            case 2:
                return Function2.class;
            default:
                throw new IllegalStateException("No class for function of arity: " + arity);
        }
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

        // Do NOT check isLambda; there should be no difference in equivalence in type if one is a lambda and one isn't
        return paramTypesEq && returnTypesEq;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
