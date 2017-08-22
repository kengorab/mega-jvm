package co.kenrg.mega.frontend.typechecking.types;

import java.util.List;

import com.google.common.collect.Lists;

public class ArrayType extends ParametrizedMegaType {
    public final MegaType typeArg;

    public ArrayType(MegaType typeArg) {
        super(Lists.newArrayList(typeArg));
        this.typeArg = typeArg;
    }

    @Override
    public int numTypeArgs() {
        return 1;
    }

    @Override
    public String displayName() {
        return "Array";
    }

    @Override
    public ParametrizedMegaType applyTypeArgs(List<MegaType> typeArgs) {
        //TODO: Fix this; parametrized types shouldn't require a dummy instance of them, from which all instances will be cloned
        return new ArrayType(typeArgs.get(0));
    }

    @Override
    public List<MegaType> typeArgs() {
        return Lists.newArrayList(this.typeArg);
    }
}
