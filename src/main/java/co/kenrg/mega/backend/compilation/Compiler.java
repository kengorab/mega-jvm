package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.compileArrowFunction;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionWithClosureCompiler.compileArrowFunctionWithClosure;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionWithClosureCompiler.getInitMethodDesc;
import static co.kenrg.mega.backend.compilation.subcompilers.BooleanInfixExpressionCompiler.compileComparisonExpression;
import static co.kenrg.mega.backend.compilation.subcompilers.BooleanInfixExpressionCompiler.compileConditionalAndExpression;
import static co.kenrg.mega.backend.compilation.subcompilers.BooleanInfixExpressionCompiler.compileConditionalOrExpression;
import static co.kenrg.mega.backend.compilation.subcompilers.CallExpressionCompiler.compileInvocation;
import static co.kenrg.mega.backend.compilation.subcompilers.MethodProxyCompiler.compileFuncProxy;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileBoxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileUnboxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.StaticMethodReferenceCompiler.compileMethodReference;
import static co.kenrg.mega.backend.compilation.subcompilers.StringInfixExpressionCompiler.compileStringConcatenation;
import static co.kenrg.mega.backend.compilation.subcompilers.StringInfixExpressionCompiler.compileStringRepetition;
import static co.kenrg.mega.backend.compilation.subcompilers.TypeDeclarationStatementCompiler.compileTypeDeclaration;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.loadInsn;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.returnInsn;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.storeInsn;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.F_CHOP;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import co.kenrg.mega.backend.compilation.scope.Binding;
import co.kenrg.mega.backend.compilation.scope.BindingTypes;
import co.kenrg.mega.backend.compilation.scope.FocusedMethod;
import co.kenrg.mega.backend.compilation.scope.Scope;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.AccessorExpression;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.AssignmentExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.IndexExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.ObjectLiteral;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.expression.ParenthesizedExpression;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.RangeExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ForLoopStatement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ImportStatement;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType.Kind;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class Compiler {
    private final TypeEnvironment typeEnv;
    private final List<String> errors = Lists.newArrayList();
    private final String className;

    public final ClassWriter cw;
    public final MethodVisitor clinitWriter;

    public Scope scope;

    private List<Pair<String, byte[]>> innerClasses = Lists.newArrayList();
    private Function<String, TypeCheckResult<Module>> typedModuleProvider;

    public Compiler(String className, TypeEnvironment typeEnv) {
        this(className, null, "java/lang/Object", null, typeEnv);

        // TODO: Flesh out the <init> method writer a bit, as needed
        MethodVisitor initWriter = this.cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        initWriter.visitCode();
        initWriter.visitVarInsn(ALOAD, 0);
        initWriter.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initWriter.visitInsn(RETURN);
        initWriter.visitMaxs(1, 1);
        initWriter.visitEnd();
    }

    public Compiler(String className, String signature, String superName, String[] interfaces, TypeEnvironment typeEnv) {
        this.className = className;
        this.typeEnv = typeEnv;

        this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.cw.visit(V1_6, ACC_PUBLIC, className, signature, superName, interfaces);

        // Note: When creating a Compiler specifically for compiling a class w/ a designated superclass, the assumption
        // is that the consumer will handle writing the <init> method visitor.

        this.clinitWriter = this.cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        this.scope = new Scope(new FocusedMethod(this.clinitWriter, null, null));

        // Initialize <clinit> method writer for class. This method writer will be used to initialize all the static
        // values for the class (namespace).
        clinitWriter.visitCode();
    }

    public void setTypedModuleProvider(Function<String, TypeCheckResult<Module>> typedModuleProvider) {
        this.typedModuleProvider = typedModuleProvider;
    }

    public <T extends Node> List<Pair<String, byte[]>> compile(T node) {
        compileNode(node);
        this.clinitWriter.visitInsn(RETURN);
        this.clinitWriter.visitMaxs(-1, -1);
        this.clinitWriter.visitEnd();

        return results();
    }

    public List<Pair<String, byte[]>> results() { // Used publicly by sub-compilers
        this.cw.visitEnd();
        innerClasses.add(Pair.of(this.className, this.cw.toByteArray()));
        return innerClasses;
    }

    public <T extends Node> void compileNode(T node) {
        this.scope.context.pushContext(node);

        // Statements
        if (node instanceof Module) {
            this.compileStatements(((Module) node).statements);
        } else if (node instanceof ExpressionStatement) {
            this.compileNode(((ExpressionStatement) node).expression);
        } else if (node instanceof ValStatement) {
            this.compileValStatement((ValStatement) node);
        } else if (node instanceof VarStatement) {
            this.compileVarStatement((VarStatement) node);
        } else if (node instanceof ForLoopStatement) {
            this.compileForLoopStatement((ForLoopStatement) node);
        } else if (node instanceof FunctionDeclarationStatement) {
            this.compileFunctionDeclarationStatement((FunctionDeclarationStatement) node);
        } else if (node instanceof TypeDeclarationStatement) {
            this.compileTypeDeclarationStatement((TypeDeclarationStatement) node);
        } else if (node instanceof ImportStatement) {
            this.compileImportStatement((ImportStatement) node);
        }

        // Expressions
        if (node instanceof IntegerLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof FloatLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof BooleanLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof StringLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof ArrayLiteral) {
            this.compileArrayLiteral((ArrayLiteral) node);
        } else if (node instanceof ObjectLiteral) {
            this.compileObjectLiteral((ObjectLiteral) node);
        } else if (node instanceof ParenthesizedExpression) {
            this.compileNode(((ParenthesizedExpression) node).expr);
        } else if (node instanceof PrefixExpression) {
            this.compilePrefixExpression((PrefixExpression) node);
        } else if (node instanceof InfixExpression) {
            this.compileInfixExpression((InfixExpression) node);
        } else if (node instanceof IfExpression) {
            this.compileIfExpression((IfExpression) node);
        } else if (node instanceof Identifier) {
            this.compileIdentifier((Identifier) node);
        } else if (node instanceof AssignmentExpression) {
            this.compileAssignmentExpression((AssignmentExpression) node);
        } else if (node instanceof RangeExpression) {
            this.compileRangeExpression((RangeExpression) node);
        } else if (node instanceof IndexExpression) {
            this.compileIndexExpression((IndexExpression) node);
        } else if (node instanceof ArrowFunctionExpression) {
            this.compileArrowFunctionExpression((ArrowFunctionExpression) node);
        } else if (node instanceof CallExpression) {
            this.compileCallExpression((CallExpression) node);
        } else if (node instanceof AccessorExpression) {
            this.compileAccessorExpression((AccessorExpression) node);
        }

        this.scope.context.popContext();
    }

    private void compileStatements(List<Statement> statements) {
        for (Statement statement : statements) {
            this.compileNode(statement);
        }
    }

    //***************************************************************
    //************              Statements               ************
    //***************************************************************

    private void compileValStatement(ValStatement stmt) {
        compileBinding(stmt.name.value, stmt.value.getType(), stmt.isExported, false, () -> compileNode(stmt.value));
    }

    private void compileVarStatement(VarStatement stmt) {
        compileBinding(stmt.name.value, stmt.value.getType(), stmt.isExported, true, () -> compileNode(stmt.value));
    }

    private void compileBinding(String bindingName, MegaType bindingType, boolean isExported, boolean isMutable, Runnable onCompileNode) {
        assert bindingType != null; // Should have been set during typechecking pass
        String jvmDescriptor = jvmDescriptor(bindingType, false);

        if (this.scope.isRoot()) {
            int access = ACC_STATIC; // At root scope, declare binding as static in the class
            if (!isMutable) {
                access = access | ACC_FINAL;
            }
            if (isExported) {
                access = access | ACC_PUBLIC;
            } else {
                access = access | ACC_PRIVATE;
            }
            cw.visitField(access, bindingName, jvmDescriptor, null, null);
            onCompileNode.run();
            clinitWriter.visitFieldInsn(PUTSTATIC, className, bindingName, jvmDescriptor);

            this.scope.addBinding(bindingName, bindingType, this.className, BindingTypes.STATIC, isMutable, isExported);
            return;
        }

        int index = this.scope.nextLocalVariableIndex();
        onCompileNode.run();
        this.scope.focusedMethod.writer.visitVarInsn(storeInsn(bindingType), index);
        this.scope.addBinding(bindingName, bindingType, this.className, BindingTypes.LOCAL, isMutable, isExported);
    }

    private void compileForLoopStatement(ForLoopStatement node) {
        String tag = RandomStringUtils.randomAlphanumeric(6); // Tag to uniquely id synthesized loop variables

        compileNode(node.iteratee);
        int iterateeIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ASTORE, iterateeIndex);
        this.scope.addBinding("$$for_loop_iteratee_" + tag, node.iteratee.getType(), this.className, BindingTypes.LOCAL, false);

        int iterateeLengthIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ALOAD, iterateeIndex);
        this.scope.focusedMethod.writer.visitInsn(ARRAYLENGTH);
        this.scope.focusedMethod.writer.visitVarInsn(ISTORE, iterateeLengthIndex);
        this.scope.addBinding("$$for_loop_iteratee_length_" + tag, PrimitiveTypes.INTEGER, this.className, BindingTypes.LOCAL, false);

        int iteratorIndexIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitInsn(ICONST_0);
        this.scope.focusedMethod.writer.visitVarInsn(ISTORE, iteratorIndexIndex);
        this.scope.addBinding("$$for_loop_iterator_idx_" + tag, PrimitiveTypes.INTEGER, this.className, BindingTypes.LOCAL, false);

        Label loopStart = new Label();
        Label loopEnd = new Label();

        this.scope.focusedMethod.writer.visitLabel(loopStart);
        Object[] localsSignatures = this.scope.getLocalsSignatures();
        this.scope.focusedMethod.writer.visitFrame(F_FULL, localsSignatures.length, localsSignatures, 0, null);
        this.scope.focusedMethod.writer.visitVarInsn(ILOAD, iteratorIndexIndex);
        this.scope.focusedMethod.writer.visitVarInsn(ILOAD, iterateeLengthIndex);
        this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPGE, loopEnd);

        String iteratorName = node.iterator.value;
        int iteratorIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ALOAD, iterateeIndex);
        this.scope.focusedMethod.writer.visitVarInsn(ILOAD, iteratorIndexIndex);
        ArrayType iterateeType = (ArrayType) node.iteratee.getType();
        assert iterateeType != null; // Should be populated by typechecking pass
        if (iterateeType.typeArg == PrimitiveTypes.INTEGER) {
            this.scope.focusedMethod.writer.visitInsn(AALOAD);
            compileUnboxPrimitiveType(PrimitiveTypes.INTEGER, this.scope.focusedMethod.writer);
            this.scope.focusedMethod.writer.visitVarInsn(ISTORE, iteratorIndex);
        }

        this.scope.addBinding(iteratorName, node.iterator.getType(), this.className, BindingTypes.LOCAL, false);

        compileBlockExpression(node.block);

        this.scope.focusedMethod.writer.visitIincInsn(iteratorIndexIndex, 1);

        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, loopStart);

        this.scope.focusedMethod.writer.visitLabel(loopEnd);
        this.scope.focusedMethod.writer.visitFrame(F_CHOP, 3, null, 0, null);
    }

    private void compileFunctionDeclarationStatement(FunctionDeclarationStatement node) {
        String methodName = node.name.value;
        TypeEnvironment.Binding methodBinding = this.typeEnv.getBinding(methodName);
        assert methodBinding != null; // If it's null, then it was never properly defined
        FunctionType fnType = (FunctionType) methodBinding.type;
        String funcDesc = jvmMethodDescriptor(fnType, false);

        int access = ACC_STATIC | ACC_FINAL;
        if (node.isExported) {
            access = access | ACC_PUBLIC;
        } else {
            access = access | ACC_PRIVATE;
        }
        MethodVisitor methodWriter = this.cw.visitMethod(access, methodName, funcDesc, null, null);

        Scope origScope = this.scope;
        this.scope = this.scope.createChild(new FocusedMethod(methodWriter, null, null));
        for (Parameter param : node.parameters) {
            this.scope.addBinding(param.ident.value, param.getType(), this.className, BindingTypes.LOCAL, false);
        }
        methodWriter.visitCode();

        if (node.body instanceof BlockExpression) {
            compileStatements(((BlockExpression) node.body).statements);
        } else {
            compileNode(node.body);
        }
        methodWriter.visitInsn(returnInsn(fnType.returnType));

        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
        this.scope = origScope;

        this.scope.addBinding(methodName, fnType, this.className, BindingTypes.METHOD, false, node.isExported);

        String methodAccessProxyName = methodName + "$access";

        if (!node.isExported) {
            int proxyAccess = ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC;
            MethodVisitor methodAccessProxyWriter = this.cw.visitMethod(proxyAccess, methodAccessProxyName, funcDesc, null, null);
            methodAccessProxyWriter.visitCode();

            for (int i = 0; i < fnType.paramTypes.size(); i++) {
                MegaType paramType = fnType.paramTypes.get(i);

                methodAccessProxyWriter.visitVarInsn(loadInsn(paramType), i);
            }
            methodAccessProxyWriter.visitMethodInsn(INVOKESTATIC, this.className, methodName, funcDesc, false);
            methodAccessProxyWriter.visitInsn(returnInsn(fnType.returnType));
            methodAccessProxyWriter.visitMaxs(fnType.arity(), 0);
            methodAccessProxyWriter.visitEnd();
        } else {
            String lambdaName = methodName + "$ref";
            String methodRefClassName = this.className + "$" + lambdaName;
            if (this.innerClasses.stream().noneMatch(innerClass -> innerClass.getLeft().equals(methodRefClassName))) {
                int methodRefAccess = ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_SYNTHETIC;
                this.cw.visitInnerClass(methodRefClassName, this.className, lambdaName, methodRefAccess);
                // TODO: This will only work for static method references at the moment; make this work for non-static method references
                List<Pair<String, byte[]>> generatedClasses = compileMethodReference(
                    this.className,
                    lambdaName,
                    methodRefClassName,
                    fnType,
                    methodName,
                    this.typeEnv,
                    this.scope.context,
                    methodRefAccess
                );
                this.innerClasses.addAll(generatedClasses);
            }
        }

        if (fnType.containsParamsWithDefaultValues()) {
            String proxiedMethodName = node.isExported ? methodName : methodAccessProxyName;
            compileFuncProxy(this.className, this.cw, fnType, methodName, proxiedMethodName, scope, (n, scope) -> {
                Scope s = this.scope; // Preserve original scope
                this.scope = scope;
                compileNode(n);
                this.scope = s;
            });
        }
    }

    private void compileTypeDeclarationStatement(TypeDeclarationStatement node) {
        String innerClassName = this.className + "$" + node.typeName.value;

        int access = ACC_STATIC | ACC_FINAL;
        if (node.isExported) {
            access = access | ACC_PUBLIC;
        } else {
            access = access | ACC_PRIVATE;
        }
        this.cw.visitInnerClass(innerClassName, this.className, node.typeName.value, access);
        List<Pair<String, byte[]>> generatedClasses = compileTypeDeclaration(this.className, innerClassName, node, this.typeEnv, access);
        innerClasses.addAll(generatedClasses);
    }

    private void compileImportStatement(ImportStatement node) {
        String targetModuleName = node.targetModule.value;
        if (this.typedModuleProvider == null) {
            throw new IllegalStateException("Module provider function not set on Compiler");
        }
        TypeCheckResult<Module> module = this.typedModuleProvider.apply(targetModuleName);

        for (Identifier _import : node.imports) {
            String importName = _import.value;

            // If the module is not present, then assume it's a java class being imported from
            if (module == null) {
                String className = targetModuleName.replace('.', '/');
                MegaType importType = _import.getType();
                BindingTypes bindingType;
                if (importType instanceof FunctionType && ((FunctionType) importType).kind == Kind.METHOD) {
                    bindingType = BindingTypes.METHOD;
                } else {
                    bindingType = BindingTypes.STATIC;
                }
                this.scope.addBinding(importName, importType, className, bindingType, false, false);
                continue;
            }

            TypeEnvironment.Binding typeBinding = module.typeEnvironment.getBinding(importName);
            assert typeBinding != null;
            MegaType importType = typeBinding.type;

            BindingTypes bindingType = BindingTypes.STATIC;
            if (importType instanceof FunctionType) {
                if (((FunctionType) importType).kind == Kind.METHOD) {
                    bindingType = BindingTypes.METHOD;
                }
            }
            this.scope.addBinding(importName, typeBinding.type, targetModuleName, bindingType, false, false);
        }
    }

    //***************************************************************
    //************              Expressions              ************
    //***************************************************************

    private void compileLiteral(Node node) {
        if (node instanceof IntegerLiteral) {
            int value = ((IntegerLiteral) node).value;
            if (0 <= value && value <= 5) {
                this.scope.focusedMethod.writer.visitInsn(value + ICONST_0);
            } else {
                this.scope.focusedMethod.writer.visitLdcInsn(value);
            }
        } else if (node instanceof FloatLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((FloatLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            int value = ((BooleanLiteral) node).value ? ICONST_1 : ICONST_0;
            this.scope.focusedMethod.writer.visitInsn(value);
        } else if (node instanceof StringLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((StringLiteral) node).value);
        }
    }

    private void compileArrayLiteral(ArrayLiteral node) {
        ArrayType type = (ArrayType) node.getType();
        assert type != null; // Should have been populated in typechecking pass
        MegaType elType = type.typeArg;
        String elTypeDescriptor = jvmDescriptor(elType, true);

        this.scope.focusedMethod.writer.visitLdcInsn(node.elements.size());
        this.scope.focusedMethod.writer.visitTypeInsn(ANEWARRAY, elTypeDescriptor);
        for (int i = 0; i < node.elements.size(); i++) {
            this.scope.focusedMethod.writer.visitInsn(DUP);
            this.scope.focusedMethod.writer.visitLdcInsn(i);
            Expression element = node.elements.get(i);
            compileNode(element);

            if (isPrimitive(elType)) {
                compileBoxPrimitiveType(elType, this.scope.focusedMethod.writer);
            }
            this.scope.focusedMethod.writer.visitInsn(AASTORE);
        }
    }

    private void compileObjectLiteral(ObjectLiteral node) {
        this.scope.focusedMethod.writer.visitTypeInsn(NEW, getInternalName(HashMap.class));
        this.scope.focusedMethod.writer.visitInsn(DUP);
        this.scope.focusedMethod.writer.visitMethodInsn(INVOKESPECIAL, getInternalName(HashMap.class), "<init>", "()V", false);
        String tag = RandomStringUtils.randomAlphanumeric(6); // Tag to uniquely id map
        this.scope.addBinding("$$map_" + tag, null, this.className, BindingTypes.LOCAL, false);
        int mapIdx = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ASTORE, mapIdx);

        for (Entry<Identifier, Expression> pair : node.pairs.entries()) {
            this.scope.focusedMethod.writer.visitVarInsn(ALOAD, mapIdx);
            this.scope.focusedMethod.writer.visitLdcInsn(pair.getKey().value);

            Expression value = pair.getValue();
            compileNode(value);
            MegaType valueType = value.getType();
            if (isPrimitive(valueType)) {
                compileBoxPrimitiveType(valueType, this.scope.focusedMethod.writer);
            }

            String putDesc = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
            this.scope.focusedMethod.writer.visitMethodInsn(INVOKEINTERFACE, getInternalName(Map.class), "put", putDesc, true);
            this.scope.focusedMethod.writer.visitInsn(POP);
        }

        this.scope.focusedMethod.writer.visitVarInsn(ALOAD, mapIdx);
    }

    private void compileIndexExpression(IndexExpression node) {
        compileNode(node.target);
        compileNode(node.index);

        ArrayType arrayType = (ArrayType) node.target.getType();
        assert arrayType != null; // Should have been populated in typechecking pass
        MegaType arrayElType = arrayType.typeArg;
        assert arrayElType != null; // Should have been populated in typechecking pass

        this.scope.focusedMethod.writer.visitInsn(AALOAD);
        if (isPrimitive(arrayElType)) {
            compileUnboxPrimitiveType(arrayElType, this.scope.focusedMethod.writer);
        }
    }

    private void compilePrefixExpression(PrefixExpression node) {
        switch (node.operator) {
            case "-": {
                compileNode(node.expression);
                if (node.expression.getType() == PrimitiveTypes.INTEGER) {
                    this.scope.focusedMethod.writer.visitInsn(INEG);
                } else if (node.expression.getType() == PrimitiveTypes.FLOAT) {
                    this.scope.focusedMethod.writer.visitInsn(FNEG);
                } else {
                    this.errors.add("Cannot apply numeric negation to type: " + node.expression.getType());
                }
                return;
            }
            case "!": {
                Label negationEndLabel = new Label();
                Label negationFalseLabel = new Label();

                compileNode(node.expression);
                this.scope.focusedMethod.writer.visitJumpInsn(IFNE, negationFalseLabel);
                this.scope.focusedMethod.writer.visitInsn(ICONST_1);
                this.scope.focusedMethod.writer.visitJumpInsn(GOTO, negationEndLabel);

                this.scope.focusedMethod.writer.visitLabel(negationFalseLabel);
                this.scope.focusedMethod.writer.visitInsn(ICONST_0);

                this.scope.focusedMethod.writer.visitLabel(negationEndLabel);
                return;
            }
            default:
                this.errors.add("Unrecognized unary operator: " + node.operator);
        }
    }

    private Map<String, Map<MegaType, Integer>> infixOperatorOpcodes = ImmutableMap.of(
        "+", ImmutableMap.of(
            PrimitiveTypes.INTEGER, IADD,
            PrimitiveTypes.FLOAT, FADD
        ),
        "-", ImmutableMap.of(
            PrimitiveTypes.INTEGER, ISUB,
            PrimitiveTypes.FLOAT, FSUB
        ),
        "*", ImmutableMap.of(
            PrimitiveTypes.INTEGER, IMUL,
            PrimitiveTypes.FLOAT, FMUL
        ),
        "/", ImmutableMap.of(
            PrimitiveTypes.INTEGER, IDIV,
            PrimitiveTypes.FLOAT, FDIV
        )
    );

    private void compileInfixExpression(InfixExpression node) {
        MegaType type = node.getType();
        assert type != null; // Should have been populated during typechecking pass

        if (type.isEquivalentTo(PrimitiveTypes.STRING)) {
            switch (node.operator) {
                case "+": {
                    compileStringConcatenation(node, this.scope, this::compileNode);
                    return;
                }
                case "*": {
                    compileStringRepetition(node, this.scope, this::compileNode);
                    return;
                }
            }
        }

        if (type == PrimitiveTypes.INTEGER) {
            compileNode(node.left);
            compileNode(node.right);

            Integer opcode = infixOperatorOpcodes.get(node.operator).get(type);
            this.scope.focusedMethod.writer.visitInsn(opcode);
        } else if (type == PrimitiveTypes.FLOAT) {
            assert node.left.getType() != null;
            compileNode(node.left);
            if (!node.left.getType().isEquivalentTo(PrimitiveTypes.FLOAT)) {
                this.scope.focusedMethod.writer.visitInsn(I2F);
            }

            assert node.right.getType() != null;
            compileNode(node.right);
            if (!node.right.getType().isEquivalentTo(PrimitiveTypes.FLOAT)) {
                this.scope.focusedMethod.writer.visitInsn(I2F);
            }

            Integer opcode = infixOperatorOpcodes.get(node.operator).get(type);
            this.scope.focusedMethod.writer.visitInsn(opcode);
        } else if (type == PrimitiveTypes.BOOLEAN) {
            switch (node.operator) {
                case "&&":
                    compileConditionalAndExpression(node, this.scope, this::compileNode);
                    break;
                case "||":
                    compileConditionalOrExpression(node, this.scope, this::compileNode);
                    break;
                default:
                    compileComparisonExpression(node, this.scope, this::compileNode);
                    break;
            }
        }
    }

    private void compileIfExpression(IfExpression node) {
        Label elseBlockLabel = new Label();
        Label endLabel = new Label();

        compileNode(node.condition);

        boolean hasElse = node.elseExpr != null;
        Label condFalseLabel = hasElse ? elseBlockLabel : endLabel;
        this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        compileBlockExpression(node.thenExpr);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        if (hasElse) {
            this.scope.focusedMethod.writer.visitLabel(elseBlockLabel);
            Object[] localsSignatures = this.scope.getLocalsSignatures();
            this.scope.focusedMethod.writer.visitFrame(F_FULL, localsSignatures.length, localsSignatures, 0, null);

            compileBlockExpression(node.elseExpr);
        }

        this.scope.focusedMethod.writer.visitLabel(endLabel);
        this.scope.focusedMethod.writer.visitFrame(F_SAME, 0, null, 0, null);
    }

    private void compileBlockExpression(BlockExpression node) {
        Scope origScope = this.scope;
        this.scope = this.scope.createChild();

        compileStatements(node.statements);
        this.scope = origScope;
    }

    private void compileIdentifier(Identifier node) {
        String identName = node.value;
        Binding binding = this.scope.getBinding(identName);
        if (binding == null) {
            System.out.printf("Expected identifier %s to be present, but it wasn't", identName);
            return;
        }

        MegaType type = binding.type;
        assert type != null; // Should be filled in by the typechecking pass

        loadIdentifier(identName, binding);
    }

    private void loadIdentifier(String identName, Binding binding) {
        if (binding.bindingType == BindingTypes.METHOD) {
            String lambdaName = identName + "$ref";
            String methodRefClassName = binding.ownerModule + "$" + lambdaName;
            if (binding.ownerModule.equals(this.className)) {
                // Don't recreate the inner class twice
                if (this.innerClasses.stream().noneMatch(innerClass -> innerClass.getLeft().equals(methodRefClassName))) {
                    int access = ACC_FINAL | ACC_STATIC | ACC_SYNTHETIC;
                    if (binding.isExported) {
                        access = access | ACC_PUBLIC;
                    } else {
                        access = access | ACC_PRIVATE;
                    }
                    this.cw.visitInnerClass(methodRefClassName, this.className, lambdaName, access);
                    // TODO: This will only work for static method references at the moment; make this work for non-static method references
                    List<Pair<String, byte[]>> generatedClasses = compileMethodReference(
                        this.className,
                        lambdaName,
                        methodRefClassName,
                        (FunctionType) binding.type,
                        binding.isExported ? binding.name : binding.name + "$access",
                        this.typeEnv,
                        this.scope.context,
                        access
                    );
                    this.innerClasses.addAll(generatedClasses);
                }
            }

            this.scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, methodRefClassName, "INSTANCE", "L" + methodRefClassName + ";");
            return;
        }

        MegaType type = binding.type;

        if (binding.bindingType == BindingTypes.STATIC) {
            this.scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, binding.ownerModule, identName, jvmDescriptor(type, false));
            return;
        }

        this.scope.focusedMethod.writer.visitVarInsn(loadInsn(type), binding.index);
    }

    private void compileAssignmentExpression(AssignmentExpression node) {
        String identName = node.name.value;
        Binding binding = this.scope.getBinding(identName);
        if (binding == null) {
            System.out.printf("Expected identifier %s to be present, but it wasn't", identName);
            return;
        }
        if (!binding.isMutable) {
            System.out.printf("Expected identifier %s to be mutable, but it wasn't", identName);
            return;
        }

        compileNode(node.right);

        MegaType type = binding.type;
        assert type != null; // Should be filled in by the typechecking pass
        this.scope.focusedMethod.writer.visitVarInsn(storeInsn(type), binding.index);
    }

    private void compileRangeExpression(RangeExpression node) {
        compileNode(node.leftBound);
        compileNode(node.rightBound);
        this.scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, StdLib.Ranges, "of", "(II)[Ljava/lang/Integer;", false);
    }

    private void compileArrowFunctionExpression(ArrowFunctionExpression node) {
        String lambdaName = this.scope.context.getLambdaName();
        this.scope.context.incLambdaCountOfPreviousContext();

        String innerClassName = this.className + "$" + lambdaName;
        int access = ACC_FINAL | ACC_STATIC | ACC_SYNTHETIC;
        this.cw.visitInnerClass(innerClassName, this.className, lambdaName, access);

        FunctionType fnType = (FunctionType) node.getType();
        assert fnType != null; // Should be populated by typechecking pass

        List<Entry<String, TypeEnvironment.Binding>> capturedBindings = fnType.getCapturedBindings();
        boolean closesOverBindings = !capturedBindings.isEmpty();

        List<Pair<String, byte[]>> generatedClasses = closesOverBindings
            ? compileArrowFunctionWithClosure(this.className, lambdaName, innerClassName, node, this.typeEnv, this.scope.context, access)
            : compileArrowFunction(this.className, lambdaName, innerClassName, node, this.typeEnv, this.scope.context, access);
        innerClasses.addAll(generatedClasses);

        if (closesOverBindings) {
            this.scope.focusedMethod.writer.visitTypeInsn(NEW, innerClassName);
            this.scope.focusedMethod.writer.visitInsn(DUP);

            for (Entry<String, TypeEnvironment.Binding> capturedBinding : capturedBindings) {
                Binding binding = this.scope.getBinding(capturedBinding.getKey());
                assert binding != null; // If binding is not present, there's a bigger problem
                loadIdentifier(capturedBinding.getKey(), binding);

                String initMethodDesc = getInitMethodDesc(capturedBindings);
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKESPECIAL, innerClassName, "<init>", initMethodDesc, false);
            }
        } else {
            this.scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, innerClassName, "INSTANCE", "L" + innerClassName + ";");
        }
    }

    private void compileCallExpression(CallExpression node) {
        compileInvocation(node, this.scope, this::compileNode);
    }

    private void compileAccessorExpression(AccessorExpression node) {
        compileNode(node.target);
        String propName = node.property.value;
        String getterName = String.format("get%s", StringUtils.capitalize(propName));

        MegaType targetType = node.target.getType();
        assert targetType != null;

        MegaType type = node.getType();
        assert type != null;
        String getterDesc = String.format("()%s", jvmDescriptor(type, false));
        this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, getInternalName(targetType), getterName, getterDesc, false);
    }
}
