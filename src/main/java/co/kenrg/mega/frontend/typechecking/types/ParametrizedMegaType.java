package co.kenrg.mega.frontend.typechecking.types;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.joining;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public abstract class ParametrizedMegaType extends MegaType {
    public List<MegaType> typeArgs;

    public ParametrizedMegaType(List<MegaType> typeArgs) {
        this.typeArgs = typeArgs;
    }

    public abstract int numTypeArgs();

    public abstract ParametrizedMegaType applyTypeArgs(List<MegaType> typeArgs);

    public List<MegaType> typeArgs() {
        return this.typeArgs;
    }

    @Override
    public boolean isParametrized() {
        return true;
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        if (!(other instanceof ParametrizedMegaType)) {
            return false;
        }
        ParametrizedMegaType otherType = (ParametrizedMegaType) other;

        Boolean typeArgsEq = zip(this.typeArgs().stream(), otherType.typeArgs().stream(), Pair::of)
            .map(pair -> pair.getLeft().isEquivalentTo(pair.getRight()))
            .reduce(true, Boolean::logicalAnd);

        return otherType.displayName().equals(other.displayName()) && typeArgsEq;
    }

    public String signature() {
        String typeArgsStr = typeArgs().stream()
            .map(MegaType::signature)
            .collect(joining(", ", "[", "]"));
        return String.format("%s%s", displayName(), typeArgsStr);
    }
}
