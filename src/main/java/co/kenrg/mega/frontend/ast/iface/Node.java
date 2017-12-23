package co.kenrg.mega.frontend.ast.iface;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

public interface Node {
    String repr(boolean debug, int indentLevel);

    Token getToken();

    /**
     * @return The type of this Node. This field will be null up until after the typechecking pass.
     */
    @Nullable
    MegaType getType();

    /**
     * Should be called during the typechecking pass to set the type of a Node.
     *
     * @param type The type of this Node
     */
    void setType(MegaType type);
}

