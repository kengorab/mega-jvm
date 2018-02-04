package co.kenrg.mega.frontend.typechecking.types;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment.Binding;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mega.lang.functions.Function0;
import mega.lang.functions.Function1;
import mega.lang.functions.Function2;
import mega.lang.functions.Function3;
import mega.lang.functions.Function4;
import mega.lang.functions.Function5;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

public class FunctionType extends MegaType {
    public enum Kind {
        ARROW_FN,
        METHOD,
        AMBIGUOUS
    }

    public final List<MegaType> paramTypes;
    public List<Parameter> parameters;
    @Nullable public final MegaType returnType;
    public Map<String, Binding> capturedBindings;
    public final boolean isConstructor;
    public final Kind kind;

    private FunctionType(List<MegaType> paramTypes, List<Parameter> parameters, @Nullable MegaType returnType, Map<String, Binding> capturedBindings, boolean isConstructor, Kind kind) {
        this.paramTypes = paramTypes;
        this.parameters = parameters;
        this.returnType = returnType;
        this.capturedBindings = capturedBindings;
        this.isConstructor = isConstructor;
        this.kind = kind;
    }

    public FunctionType(List<Parameter> params, @Nullable MegaType returnType, Map<String, Binding> capturedBindings, Kind kind) {
        this(params.stream().map(p -> p.ident.getType()).collect(toList()), params, returnType, capturedBindings, false, kind);
    }

    public FunctionType(List<Identifier> params, @Nullable MegaType returnType, Kind kind) {
        this(params.stream().map(Identifier::getType).collect(toList()), params.stream().map(Parameter::new).collect(toList()), returnType, Maps.newHashMap(), false, kind);
    }

    @VisibleForTesting
    public FunctionType(int _unused, List<Parameter> params, @Nullable MegaType returnType, Kind kind) {
        this(params, returnType, Maps.newHashMap(), kind);
    }

    public static FunctionType ofSignature(List<MegaType> paramTypes, @Nullable MegaType returnType) {
        return new FunctionType(paramTypes, Lists.newArrayList(), returnType, Maps.newHashMap(), false, Kind.AMBIGUOUS);
    }

    public static FunctionType constructor(List<Identifier> params, @Nullable MegaType returnType) {
        return new FunctionType(params.stream().map(Identifier::getType).collect(toList()), params.stream().map(Parameter::new).collect(toList()), returnType, Maps.newHashMap(), true, Kind.METHOD);
    }

    public int arity() {
        return this.paramTypes.size();
    }

    public List<Entry<String, Binding>> getCapturedBindings() {
        return Lists.newArrayList(this.capturedBindings.entrySet());
    }

    public boolean containsParamsWithDefaultValues() {
        for (Parameter parameter : this.parameters) {
            if (parameter.hasDefaultValue()) {
                return true;
            }
        }
        return false;
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
            case 3:
                return Function3.class;
            case 4:
                return Function4.class;
            case 5:
                return Function5.class;
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
