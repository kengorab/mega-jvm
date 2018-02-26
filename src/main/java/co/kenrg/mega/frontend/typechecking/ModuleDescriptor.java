package co.kenrg.mega.frontend.typechecking;

import static java.util.stream.Collectors.joining;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ModuleDescriptor {
    public final String moduleName;
    public final List<String> packageParts;
    public final String filepath;

    private ModuleDescriptor(String moduleName, List<String> packageParts) {
        this.moduleName = moduleName;
        this.packageParts = packageParts;

        this.filepath = this.packageParts.stream()
            .collect(joining("/", "", "/"))
            + this.moduleName + ".meg";
    }

    public static ModuleDescriptor fromRaw(String name) {
        String fullyQualifiedModuleName = name
            .replaceAll("\\.meg$", "")
            .replace('.', '/');

        List<String> packageParts = Lists.newArrayList();
        String[] parts = fullyQualifiedModuleName.split("/");
        String moduleName = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                moduleName = part;
            } else {
                packageParts.add(part);
            }
        }

        return new ModuleDescriptor(moduleName, packageParts);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
