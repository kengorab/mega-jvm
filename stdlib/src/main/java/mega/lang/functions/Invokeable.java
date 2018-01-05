package mega.lang.functions;

public abstract class Invokeable {
    private final int arity;

    public Invokeable(int arity) {
        this.arity = arity;
    }

    public int arity() {
        return this.arity;
    }
}
