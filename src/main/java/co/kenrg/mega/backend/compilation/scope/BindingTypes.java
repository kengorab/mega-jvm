package co.kenrg.mega.backend.compilation.scope;

public enum BindingTypes {
    STATIC,
    LOCAL,
    METHOD // Currently, METHOD implies STATIC, since there are no non-static methods
}
