package co.kenrg.mega.frontend.ast.statement;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;

public class ImportStatement extends Statement {
    public final Token token;
    public final List<Identifier> imports;
    public final StringLiteral targetModule;

    public ImportStatement(Token token, List<Identifier> imports, StringLiteral targetModule) {
        this.token = token;
        this.imports = imports;
        this.targetModule = targetModule;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String imports = this.imports.stream().map(ident -> ident.value).collect(joining(", "));
        return String.format("import %s from \"%s\"", imports, this.targetModule.value);
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
