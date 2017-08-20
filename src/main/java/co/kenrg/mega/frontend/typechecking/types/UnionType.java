package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;

public class UnionType extends MegaType {
    public final List<MegaType> components;

    public UnionType(List<MegaType> components) {
        this.components = components;
    }

    public UnionType(MegaType... components) {
        this.components = Arrays.asList(components);
    }

    @Override
    public String displayName() {
        return this.components.stream()
            .map(MegaType::signature)
            .collect(joining(" | "));
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        return this.components.contains(other);
    }
}
