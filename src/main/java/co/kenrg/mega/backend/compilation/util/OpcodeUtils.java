package co.kenrg.mega.backend.compilation.util;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;

public class OpcodeUtils {
    public static int returnInsn(MegaType type) {
        if (type == PrimitiveTypes.INTEGER) {
            return IRETURN;
        } else if (type == PrimitiveTypes.BOOLEAN) {
            return IRETURN;
        } else if (type == PrimitiveTypes.FLOAT) {
            return FRETURN;
        } else {
            return ARETURN;
        }
    }

    public static int loadInsn(MegaType type) {
        if (type == PrimitiveTypes.INTEGER) {
            return ILOAD;
        } else if (type == PrimitiveTypes.BOOLEAN) {
            return ILOAD;
        } else if (type == PrimitiveTypes.FLOAT) {
            return FLOAD;
        } else {
            return ALOAD;
        }
    }

    public static int storeInsn(MegaType type) {
        if (type == PrimitiveTypes.INTEGER) {
            return ISTORE;
        } else if (type == PrimitiveTypes.BOOLEAN) {
            return ISTORE;
        } else if (type == PrimitiveTypes.FLOAT) {
            return FSTORE;
        } else {
            return ASTORE;
        }
    }
}
