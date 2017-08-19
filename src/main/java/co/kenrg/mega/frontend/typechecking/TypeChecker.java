package co.kenrg.mega.frontend.typechecking;

import java.util.List;
import java.util.Optional;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownTypeError;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;

public class TypeChecker {
    private static final MegaType unknownType = () -> "<unknown>";

    private final List<TypeCheckerError> errors;

    public TypeChecker() {
        this.errors = Lists.newArrayList();
    }

    public <T extends Node> TypeCheckResult<T> typecheck(T node, TypeEnvironment env) {
        TypedNode<T> typedNode = typecheckNode(node, env);
        return new TypeCheckResult<>(typedNode, this.errors);
    }

    private <T extends Node> TypedNode<T> typecheckNode(T node, TypeEnvironment env) {
        // Statements
        if (node instanceof Module) {
            this.typecheckStatements(((Module) node).statements, env);
        } else if (node instanceof ExpressionStatement) {
            TypedNode<Expression> typedNode = typecheckNode(((ExpressionStatement) node).expression, env);
            return new TypedNode<>(node, typedNode.type);
        } else if (node instanceof LetStatement) {
            this.typecheckLetStatement((LetStatement) node, env);
        } else if (node instanceof VarStatement) {
            this.typecheckVarStatement((VarStatement) node, env);
        }

        if (node instanceof IntegerLiteral) {
            return new TypedNode<>(node, PrimitiveTypes.INTEGER);
        } else if (node instanceof FloatLiteral) {
            return new TypedNode<>(node, PrimitiveTypes.FLOAT);
        } else if (node instanceof BooleanLiteral) {
            return new TypedNode<>(node, PrimitiveTypes.BOOLEAN);
        } else if (node instanceof StringLiteral) {
            return new TypedNode<>(node, PrimitiveTypes.STRING);
        }

        return new TypedNode<>(node, PrimitiveTypes.UNIT);
    }

    private void typecheckStatements(List<Statement> statements, TypeEnvironment env) {
        for (Statement statement : statements) {
            //TODO: Record errors...
            this.typecheckNode(statement, env);
        }
    }

    private void typecheckLetStatement(LetStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        MegaType type = unknownType;

        if (statement.name.typeAnnotation != null) {
            Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(statement.name.typeAnnotation);
            if (declaredTypeOpt.isPresent()) {
                MegaType declaredType = declaredTypeOpt.get();
                if (declaredType.equals(valueResult.type)) {
                    type = declaredType;
                } else {
                    this.errors.add(new TypeMismatchError(declaredType, valueResult.type));
                }
            } else {
                this.errors.add(new UnknownTypeError(statement.name.typeAnnotation));
            }
        } else {
            type = valueResult.type;
        }

        env.add(name, type, true);
    }

    private void typecheckVarStatement(VarStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        MegaType type = unknownType;

        if (statement.name.typeAnnotation != null) {
            Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(statement.name.typeAnnotation);
            if (declaredTypeOpt.isPresent()) {
                MegaType declaredType = declaredTypeOpt.get();
                if (declaredType.equals(valueResult.type)) {
                    type = declaredType;
                } else {
                    this.errors.add(new TypeMismatchError(declaredType, valueResult.type));
                }
            } else {
                this.errors.add(new UnknownTypeError(statement.name.typeAnnotation));
            }
        } else {
            type = valueResult.type;
        }

        env.add(name, type, false);
    }
}
