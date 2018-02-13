package co.kenrg.mega.frontend.ast;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ImportStatement;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Maps;

public class Module implements Node {
    public final List<Statement> statements;
    public final List<ImportStatement> imports;
    public final List<Statement> exports;

    public Map<String, Statement> namedExports = Maps.newHashMap(); // Entries inserted during typechecking

    public Module(List<Statement> statements, List<ImportStatement> imports, List<Statement> exports) {
        this.statements = statements;
        this.exports = exports;
        this.imports = imports;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        StringBuilder sb = new StringBuilder();

        for (Statement statement : statements) {
            sb.append(statement.repr(debug, indentLevel));
        }

        return sb.toString();
    }

    @Override
    public Token getToken() {
        return null;
    }

    @Nullable
    @Override
    public MegaType getType() {
        return PrimitiveTypes.UNIT;
    }

    @Override
    public void setType(MegaType type) {
        // This has no effect, since a Module's type is always Unit.
    }
}
