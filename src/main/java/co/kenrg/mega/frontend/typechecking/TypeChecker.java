package co.kenrg.mega.frontend.typechecking;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.ObjectLiteral;
import co.kenrg.mega.frontend.ast.expression.ParenthesizedExpression;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ForLoopStatement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.typechecking.errors.FunctionArityError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionTypeError;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UninvokeableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownTypeError;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

public class TypeChecker {
    static final MegaType unknownType = new MegaType() {
        @Override
        public String displayName() {
            return "<unknown>";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return false;
        }
    };

    private final List<TypeCheckerError> errors;

    public TypeChecker() {
        this.errors = Lists.newArrayList();
    }

    public <T extends Node> TypeCheckResult<T> typecheck(T node, TypeEnvironment env) {
        TypedNode<T> typedNode = typecheckNode(node, env);
        return new TypeCheckResult<>(typedNode, this.errors);
    }

    private <T extends Node> TypedNode<T> typecheckNode(T node, TypeEnvironment env) {
        MegaType type = PrimitiveTypes.UNIT;

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
        } else if (node instanceof FunctionDeclarationStatement) {
            this.typecheckFunctionDeclarationStatement((FunctionDeclarationStatement) node, env);
        } else if (node instanceof ForLoopStatement) {
            this.typecheckForLoopStatement((ForLoopStatement) node, env);
        }

        if (node instanceof IntegerLiteral) {
            type = PrimitiveTypes.INTEGER;
        } else if (node instanceof FloatLiteral) {
            type = PrimitiveTypes.FLOAT;
        } else if (node instanceof BooleanLiteral) {
            type = PrimitiveTypes.BOOLEAN;
        } else if (node instanceof StringLiteral) {
            type = PrimitiveTypes.STRING;
        } else if (node instanceof ArrayLiteral) {
            type = this.typecheckArrayLiteral((ArrayLiteral) node, env);
        } else if (node instanceof ObjectLiteral) {
            type = this.typecheckObjectLiteral((ObjectLiteral) node, env);
        } else if (node instanceof ParenthesizedExpression) {
            type = this.typecheckNode(((ParenthesizedExpression) node).expr, env).type;
        } else if (node instanceof PrefixExpression) {
            type = this.typecheckPrefixExpression((PrefixExpression) node, env);
        } else if (node instanceof InfixExpression) {
            type = this.typecheckInfixExpression((InfixExpression) node, env);
        } else if (node instanceof IfExpression) {
            type = this.typecheckIfExpression((IfExpression) node, env);
        } else if (node instanceof BlockExpression) {
            type = this.typecheckBlockExpression((BlockExpression) node, env);
        } else if (node instanceof Identifier) {
            type = this.typecheckIdentifier((Identifier) node, env);
        } else if (node instanceof ArrowFunctionExpression) {
            type = this.typecheckArrowFunctionExpression((ArrowFunctionExpression) node, env);
        } else if (node instanceof CallExpression) {
            type = this.typecheckCallExpression((CallExpression) node, env);
        }

        return new TypedNode<>(node, type);
    }

    private void typecheckStatements(List<Statement> statements, TypeEnvironment env) {
        for (Statement statement : statements) {
            //TODO: Record errors...
            this.typecheckNode(statement, env);
        }
    }

    private void typecheckLetStatement(LetStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueTypeResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        MegaType type = unknownType;

        if (statement.name.typeAnnotation != null) {
            Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(statement.name.typeAnnotation);
            if (declaredTypeOpt.isPresent()) {
                MegaType declaredType = declaredTypeOpt.get();
                if (declaredType.equals(valueTypeResult.type)) {
                    type = declaredType;
                } else {
                    this.errors.add(new TypeMismatchError(declaredType, valueTypeResult.type));
                }
            } else {
                this.errors.add(new UnknownTypeError(statement.name.typeAnnotation));
            }
        } else {
            type = valueTypeResult.type;
        }

        env.add(name, type, true);
    }

    private void typecheckVarStatement(VarStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueTypeResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        MegaType type = unknownType;

        if (statement.name.typeAnnotation != null) {
            Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(statement.name.typeAnnotation);
            if (declaredTypeOpt.isPresent()) {
                MegaType declaredType = declaredTypeOpt.get();
                if (declaredType.equals(valueTypeResult.type)) {
                    type = declaredType;
                } else {
                    this.errors.add(new TypeMismatchError(declaredType, valueTypeResult.type));
                }
            } else {
                this.errors.add(new UnknownTypeError(statement.name.typeAnnotation));
            }
        } else {
            type = valueTypeResult.type;
        }

        env.add(name, type, false);
    }

    private void typecheckFunctionDeclarationStatement(FunctionDeclarationStatement statement, TypeEnvironment env) {
        List<MegaType> paramTypes = Lists.newArrayListWithExpectedSize(statement.parameters.size());

        for (Identifier parameter : statement.parameters) {
            if (parameter.typeAnnotation == null) {
                this.errors.add(new TypeCheckerError() {
                    @Override
                    public String message() {
                        return String.format("Missing type annotation on parameter: %s", parameter.value);
                    }
                });
            } else {
                Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(parameter.typeAnnotation);
                if (declaredTypeOpt.isPresent()) {
                    MegaType type = declaredTypeOpt.get();
                    paramTypes.add(type);
                    env.add(parameter.value, type, true);
                } else {
                    this.errors.add(new UnknownTypeError(parameter.typeAnnotation));
                }
            }
        }

        MegaType returnType = typecheckNode(statement.body, env).type;

        if (statement.typeAnnotation != null) {
            Optional<MegaType> declaredReturnTypeOpt = PrimitiveTypes.byDisplayName(statement.typeAnnotation);
            if (declaredReturnTypeOpt.isPresent()) {
                MegaType declaredReturnType = declaredReturnTypeOpt.get();
                if (!declaredReturnType.isEquivalentTo(returnType)) {
                    this.errors.add(new TypeMismatchError(declaredReturnType, returnType));
                }
                env.add(statement.name.value, new FunctionType(paramTypes, declaredReturnType), true);
            } else {
                this.errors.add(new UnknownTypeError(statement.typeAnnotation));
            }
        }

        env.add(statement.name.value, new FunctionType(paramTypes, returnType), true);
    }

    private void typecheckForLoopStatement(ForLoopStatement statement, TypeEnvironment env) {
        String iterator = statement.iterator.value;

        MegaType iterateeType = typecheckNode(statement.iteratee, env).type;
        ArrayType arrayAnyType = new ArrayType(PrimitiveTypes.ANY);
        if (!arrayAnyType.isEquivalentTo(iterateeType)) {
            this.errors.add(new TypeMismatchError(arrayAnyType, iterateeType));
            env.add(iterator, unknownType, true);
        } else {
            env.add(iterator, ((ArrayType) iterateeType).typeArg, true);
        }

        typecheckNode(statement.block, env);
    }

    private MegaType typecheckArrayLiteral(ArrayLiteral array, TypeEnvironment env) {
        if (array.elements.isEmpty()) {
            return new ArrayType(PrimitiveTypes.NOTHING);
        }
        List<Expression> elements = array.elements;

        MegaType type = null;
        MegaType firstMismatch = null;
        for (Expression elem : elements) {
            TypedNode<Expression> elemTypeResult = typecheckNode(elem, env);
            if (type == null) {
                type = elemTypeResult.type;
            }
            if (!elemTypeResult.type.equals(type)) {
                firstMismatch = elemTypeResult.type;
            }
        }

        if (firstMismatch != null) {
            this.errors.add(new TypeMismatchError(type, firstMismatch));
        }

        return new ArrayType(type);
    }

    private MegaType typecheckObjectLiteral(ObjectLiteral object, TypeEnvironment env) {
        Map<String, MegaType> objectPropertyTypes = object.pairs.entrySet().stream()
            .collect(toMap(
                entry -> entry.getKey().value,
                entry -> typecheckNode(entry.getValue(), env).type
            ));
        return new ObjectType(objectPropertyTypes);
    }

    private MegaType typecheckPrefixExpression(PrefixExpression expr, TypeEnvironment env) {
        switch (expr.operator) {
            case "!":
                return PrimitiveTypes.BOOLEAN;
            case "-": {
                MegaType exprType = typecheckNode(expr.expression, env).type;
                if (PrimitiveTypes.NUMBER.isEquivalentTo(exprType)) {
                    return exprType;
                } else {
                    this.errors.add(new TypeMismatchError(PrimitiveTypes.NUMBER, exprType));
                    return unknownType;
                }
            }
            default:
                return unknownType;
        }
    }

    private MegaType typecheckInfixExpression(InfixExpression expr, TypeEnvironment env) {
        MegaType leftType = typecheckNode(expr.left, env).type;
        MegaType rightType = typecheckNode(expr.right, env).type;

        switch (expr.operator) {
            //TODO: Implement lexing/parsing of boolean and/or (&&, ||) infix operators
            case "&&":
            case "||":
                if (!PrimitiveTypes.BOOLEAN.isEquivalentTo(leftType)) {
                    this.errors.add(new TypeMismatchError(PrimitiveTypes.BOOLEAN, leftType));
                }
                if (!PrimitiveTypes.BOOLEAN.isEquivalentTo(rightType)) {
                    this.errors.add(new TypeMismatchError(PrimitiveTypes.BOOLEAN, rightType));
                }
                return PrimitiveTypes.BOOLEAN;
            case "<":
            case ">":
            case "<=":
            case ">=":
                if (!PrimitiveTypes.NUMBER.isEquivalentTo(leftType)) {
                    this.errors.add(new TypeMismatchError(PrimitiveTypes.NUMBER, leftType));
                }
                if (!PrimitiveTypes.NUMBER.isEquivalentTo(rightType)) {
                    this.errors.add(new TypeMismatchError(PrimitiveTypes.NUMBER, rightType));
                }
                return PrimitiveTypes.BOOLEAN;
            case "==":
            case "!=":
                return PrimitiveTypes.BOOLEAN;
            default:
                if (PrimitiveTypes.NUMBER.isEquivalentTo(leftType) && PrimitiveTypes.NUMBER.isEquivalentTo(rightType)) {
                    if (PrimitiveTypes.INTEGER.isEquivalentTo(leftType) && PrimitiveTypes.INTEGER.isEquivalentTo(rightType)) {
                        return PrimitiveTypes.INTEGER;
                    } else {
                        return PrimitiveTypes.FLOAT;
                    }
                }

                if (PrimitiveTypes.STRING.isEquivalentTo(leftType) || PrimitiveTypes.STRING.isEquivalentTo(rightType)) {
                    switch (expr.operator) {
                        case "+":
                            return PrimitiveTypes.STRING;
                        case "*":
                            if (PrimitiveTypes.STRING.isEquivalentTo(leftType)) {
                                if (!PrimitiveTypes.INTEGER.isEquivalentTo(rightType)) {
                                    this.errors.add(new TypeMismatchError(PrimitiveTypes.INTEGER, rightType));
                                }
                            }
                            if (PrimitiveTypes.STRING.isEquivalentTo(rightType)) {
                                if (!PrimitiveTypes.INTEGER.isEquivalentTo(leftType)) {
                                    this.errors.add(new TypeMismatchError(PrimitiveTypes.INTEGER, leftType));
                                }
                            }
                            return PrimitiveTypes.STRING;
                        default:
                            // Unknown String operator
                            return unknownType;
                    }
                }

                // Unknown operator, or incoming types are already <unknown>
                return unknownType;
        }
    }

    private MegaType typecheckIfExpression(IfExpression expr, TypeEnvironment env) {
        MegaType conditionType = typecheckNode(expr.condition, env).type;
        if (!PrimitiveTypes.BOOLEAN.isEquivalentTo(conditionType)) {
            this.errors.add(new TypeMismatchError(PrimitiveTypes.BOOLEAN, conditionType));
        }

        MegaType thenBlockType = typecheckNode(expr.thenExpr, env).type;

        if (expr.elseExpr != null) {
            MegaType elseBlockType = typecheckNode(expr.elseExpr, env).type;
            if (!thenBlockType.isEquivalentTo(elseBlockType)) {
                this.errors.add(new TypeMismatchError(thenBlockType, elseBlockType));
            }
            return thenBlockType;
        } else {
            return PrimitiveTypes.UNIT;
        }
    }

    private MegaType typecheckBlockExpression(BlockExpression expr, TypeEnvironment env) {
        List<Statement> statements = expr.statements;

        // An empty block-expr should have type Unit
        MegaType blockType = PrimitiveTypes.UNIT;

        for (int i = 0; i < statements.size(); i++) {
            MegaType type = typecheckNode(statements.get(i), env).type;
            if (i == statements.size() - 1) {
                blockType = type;
            }
        }
        return blockType;
    }

    private MegaType typecheckIdentifier(Identifier identifier, TypeEnvironment env) {
        MegaType identifierType = env.get(identifier.value);
        return identifierType == null ? unknownType : identifierType;
    }

    private MegaType typecheckArrowFunctionExpression(ArrowFunctionExpression expr, TypeEnvironment env) {
        List<MegaType> paramTypes = Lists.newArrayListWithExpectedSize(expr.parameters.size());

        for (Identifier parameter : expr.parameters) {
            if (parameter.typeAnnotation == null) {
                this.errors.add(new TypeCheckerError() {
                    @Override
                    public String message() {
                        return String.format("Missing type annotation on parameter: %s", parameter.value);
                    }
                });
            } else {
                Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(parameter.typeAnnotation);
                if (declaredTypeOpt.isPresent()) {
                    MegaType type = declaredTypeOpt.get();
                    paramTypes.add(type);
                    env.add(parameter.value, type, true);
                } else {
                    this.errors.add(new UnknownTypeError(parameter.typeAnnotation));
                }
            }
        }

        MegaType returnType = typecheckNode(expr.body, env).type;
        return new FunctionType(paramTypes, returnType);
    }

    private MegaType typecheckCallExpression(CallExpression expr, TypeEnvironment env) {
        MegaType targetType = typecheckNode(expr.target, env).type;
        if (!(targetType instanceof FunctionType)) {
            this.errors.add(new UninvokeableTypeError(targetType));
            return unknownType;
        }
        FunctionType funcType = (FunctionType) targetType;

        if (funcType.paramTypes.size() != expr.arguments.size()) {
            this.errors.add(new FunctionArityError(funcType.paramTypes.size(), expr.arguments.size()));
        }

        List<MegaType> providedTypes = expr.arguments.stream().map(arg -> typecheckNode(arg, env).type).collect(toList());

        zip(funcType.paramTypes.stream(), providedTypes.stream(), Pair::of)
            .map(pair -> !pair.getLeft().isEquivalentTo(pair.getRight()))
            .filter(Predicate.isEqual(true))
            .findFirst()
            .ifPresent(isMismatch -> this.errors.add(new FunctionTypeError(funcType.paramTypes, providedTypes)));

        return funcType.returnType;
    }
}
