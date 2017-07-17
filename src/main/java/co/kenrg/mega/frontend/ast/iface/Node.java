package co.kenrg.mega.frontend.ast.iface;

import co.kenrg.mega.frontend.token.Token;

public interface Node {
    String repr(boolean debug, int indentLevel);
    Token getToken();
}

