package co.kenrg.mega.frontend.typechecking.types;

import java.util.List;

import com.google.common.collect.Lists;

public class ArrayType extends MegaType {
    private final MegaType typeArg;

    public ArrayType(MegaType typeArg) {
        this.typeArg = typeArg;
    }

    @Override
    public String displayName() {
        return "Array";
    }

    @Override
    public List<MegaType> typeArgs() {
        return Lists.newArrayList(this.typeArg);
    }
}
