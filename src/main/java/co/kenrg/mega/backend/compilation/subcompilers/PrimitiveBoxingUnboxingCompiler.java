package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import org.objectweb.asm.MethodVisitor;

public class PrimitiveBoxingUnboxingCompiler {
    public static void compileUnboxPrimitiveType(MegaType type, MethodVisitor methodWriter) {
        if (type == PrimitiveTypes.INTEGER) {
            methodWriter.visitMethodInsn(INVOKEVIRTUAL, getInternalName(PrimitiveTypes.INTEGER), "intValue", "()I", false);
        } else if (type == PrimitiveTypes.BOOLEAN) {
            methodWriter.visitMethodInsn(INVOKEVIRTUAL, getInternalName(PrimitiveTypes.BOOLEAN), "booleanValue", "()Z", false);
        } else if (type == PrimitiveTypes.FLOAT) {
            methodWriter.visitMethodInsn(INVOKEVIRTUAL, getInternalName(PrimitiveTypes.FLOAT), "floatValue", "()F", false);
        } else {
            throw new IllegalStateException("Type " + type + " is not primitive, cannot box");
        }
    }

    public static void compileBoxPrimitiveType(MegaType type, MethodVisitor methodWriter) {
        String elTypeClass = getInternalName(type);
        String signature = String.format("(%s)L%s;", jvmDescriptor(type, false), elTypeClass);
        methodWriter.visitMethodInsn(INVOKESTATIC, elTypeClass, "valueOf", signature, false);
    }
}
