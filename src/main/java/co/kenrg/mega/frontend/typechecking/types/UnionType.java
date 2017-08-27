package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class UnionType extends MegaType {
    public final List<MegaType> components;
    @Nullable public final String name;

    public UnionType(MegaType... components) {
        this.name = null;
        this.components = Arrays.asList(components);
    }

    public UnionType(@Nullable String name, MegaType... components) {
        this.name = name;
        this.components = Arrays.asList(components);
    }

    @Override
    public String displayName() {
        if (this.name != null) {
            return name;
        }
        return this.components.stream()
            .map(MegaType::signature)
            .collect(joining(" | "));
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        return this.components.contains(other);
    }
}
