package co.kenrg.mega.frontend.typechecking;

import java.util.List;

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
import co.kenrg.mega.repl.object.iface.ObjectType;
import com.google.common.collect.Lists;

public class TypeChecker {
    public static class TypecheckResult<T extends Node> {
        public final TypedNode<T> node;
        public final List<TypeError> errors;

        public TypecheckResult(TypedNode<T> node, List<TypeError> errors) {
            this.node = node;
            this.errors = errors;
        }

        public boolean hasErrors() {
            return !this.errors.isEmpty();
        }
    }

    private final List<TypeError> errors;

    public TypeChecker() {
        this.errors = Lists.newArrayList();
    }

    public <T extends Node> TypecheckResult<T> typecheck(T node, TypeEnvironment env) {
        TypedNode<T> typedNode = typecheckNode(node, env);
        return new TypecheckResult<>(typedNode, this.errors);
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
            return new TypedNode<>(node, ObjectType.INTEGER);
        } else if (node instanceof FloatLiteral) {
            return new TypedNode<>(node, ObjectType.FLOAT);
        } else if (node instanceof BooleanLiteral) {
            return new TypedNode<>(node, ObjectType.BOOLEAN);
        } else if (node instanceof StringLiteral) {
            return new TypedNode<>(node, ObjectType.STRING);
        }

        return new TypedNode<>(node, ObjectType.UNIT);
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
        ObjectType type;

        if (statement.name.typeAnnotation != null) {
            ObjectType declaredType = ObjectType.byDisplayName(statement.name.typeAnnotation);
            if (valueResult.type != declaredType) {
                this.errors.add(new TypeError(declaredType, valueResult.type));
            }
            type = declaredType;
        } else {
            type = valueResult.type;
        }

        env.add(name, type, true);
    }

    private void typecheckVarStatement(VarStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        ObjectType type;

        if (statement.name.typeAnnotation != null) {
            ObjectType declaredType = ObjectType.byDisplayName(statement.name.typeAnnotation);
            if (valueResult.type != declaredType) {
                this.errors.add(new TypeError(declaredType, valueResult.type));
            }
            type = declaredType;
        } else {
            type = valueResult.type;
        }

        env.add(name, type, false);
    }
}
