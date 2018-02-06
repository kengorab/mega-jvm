package co.kenrg.mega.backend.compilation.scope;

import javax.annotation.Nullable;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FocusedMethod {
    public final MethodVisitor writer;

    // TODO: Are these labels useful?
    @Nullable public final Label start;
    @Nullable public final Label end;

    public FocusedMethod(MethodVisitor writer, @Nullable Label start, @Nullable Label end) {
        this.writer = writer;
        this.start = start;
        this.end = end;
    }
}
