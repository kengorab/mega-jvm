package co.kenrg.mega.frontend.typechecking;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import co.kenrg.mega.frontend.ast.Module;
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
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.ast.type.BasicTypeExpression;
import co.kenrg.mega.frontend.ast.type.FunctionTypeExpression;
import co.kenrg.mega.frontend.ast.type.ParametrizedTypeExpression;
import co.kenrg.mega.frontend.ast.type.StructTypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.typechecking.errors.DuplicateTypeError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionArityError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionTypeError;
import co.kenrg.mega.frontend.typechecking.errors.MutabilityError;
import co.kenrg.mega.frontend.typechecking.errors.ParametrizableTypeArityError;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UnindexableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UninvokeableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownIdentifierError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnparametrizableTypeError;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.ParametrizedMegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import co.kenrg.mega.frontend.typechecking.types.StructType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

public class TypeChecker {
    static final MegaType unknownType = new MegaType() {
        @Override
        public String displayName() {
            return "<Unknown>";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return false;
        }
    };

    static final MegaType notInferredType = new MegaType() {
        @Override
        public String displayName() {
            return "<NotInferred>";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return false;
        }
    };

    final List<TypeCheckerError> errors;

    public TypeChecker() {
        this.errors = Lists.newArrayList();
    }

    public <T extends Node> TypeCheckResult<T> typecheck(T node, TypeEnvironment env) {
        TypedNode<T> typedNode = typecheckNode(node, env, null);
        return new TypeCheckResult<>(typedNode, this.errors);
    }

    private <T extends Node> TypedNode<T> typecheckNode(T node, TypeEnvironment env) {
        return this.typecheckNode(node, env, null);
    }

    private <T extends Node> TypedNode<T> typecheckNode(T node, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType type = PrimitiveTypes.UNIT;

        // Statements
        if (node instanceof Module) {
            this.typecheckStatements(((Module) node).statements, env);
        } else if (node instanceof ExpressionStatement) {
            TypedNode<Expression> typedNode = typecheckNode(((ExpressionStatement) node).expression, env, expectedType);
            return new TypedNode<>(node, typedNode.type);
        } else if (node instanceof LetStatement) {
            this.typecheckLetStatement((LetStatement) node, env);
        } else if (node instanceof VarStatement) {
            this.typecheckVarStatement((VarStatement) node, env);
        } else if (node instanceof FunctionDeclarationStatement) {
            this.typecheckFunctionDeclarationStatement((FunctionDeclarationStatement) node, env);
        } else if (node instanceof ForLoopStatement) {
            this.typecheckForLoopStatement((ForLoopStatement) node, env);
        } else if (node instanceof TypeDeclarationStatement) {
            this.typecheckTypeDeclarationStatement((TypeDeclarationStatement) node, env);
        }

        if (node instanceof IntegerLiteral) {
            type = this.typecheckLiteralExpression(PrimitiveTypes.INTEGER, expectedType);
        } else if (node instanceof FloatLiteral) {
            type = this.typecheckLiteralExpression(PrimitiveTypes.FLOAT, expectedType);
        } else if (node instanceof BooleanLiteral) {
            type = this.typecheckLiteralExpression(PrimitiveTypes.BOOLEAN, expectedType);
        } else if (node instanceof StringLiteral) {
            type = this.typecheckLiteralExpression(PrimitiveTypes.STRING, expectedType);
        } else if (node instanceof ArrayLiteral) {
            type = this.typecheckArrayLiteral((ArrayLiteral) node, env, expectedType);
        } else if (node instanceof ObjectLiteral) {
            type = this.typecheckObjectLiteral((ObjectLiteral) node, env, expectedType);
        } else if (node instanceof ParenthesizedExpression) {
            type = this.typecheckNode(((ParenthesizedExpression) node).expr, env, expectedType).type;
        } else if (node instanceof PrefixExpression) {
            type = this.typecheckPrefixExpression((PrefixExpression) node, env);
        } else if (node instanceof InfixExpression) {
            type = this.typecheckInfixExpression((InfixExpression) node, env);
        } else if (node instanceof IfExpression) {
            type = this.typecheckIfExpression((IfExpression) node, env);
        } else if (node instanceof BlockExpression) {
            type = this.typecheckBlockExpression((BlockExpression) node, env);
        } else if (node instanceof Identifier) {
            type = this.typecheckIdentifier((Identifier) node, env, expectedType);
        } else if (node instanceof ArrowFunctionExpression) {
            type = this.typecheckArrowFunctionExpression((ArrowFunctionExpression) node, env, expectedType);
        } else if (node instanceof CallExpression) {
            type = this.typecheckCallExpression((CallExpression) node, env);
        } else if (node instanceof IndexExpression) {
            type = this.typecheckIndexExpression((IndexExpression) node, env);
        } else if (node instanceof AssignmentExpression) {
            type = this.typecheckAssignmentExpression((AssignmentExpression) node, env);
        } else if (node instanceof RangeExpression) {
            type = this.typecheckRangeExpression((RangeExpression) node, env);
        }

        return new TypedNode<>(node, type);
    }

    private MegaType resolveType(TypeExpression typeExpr, TypeEnvironment typeEnvironment) {
        if (typeExpr instanceof BasicTypeExpression) {
            MegaType type = typeEnvironment.getTypeByName(((BasicTypeExpression) typeExpr).type);
            if (type == null) {
                this.errors.add(new UnknownTypeError(typeExpr.signature()));
                return unknownType;
            }
            return type;
        }

        if (typeExpr instanceof ParametrizedTypeExpression) {
            MegaType type = typeEnvironment.getTypeByName(((ParametrizedTypeExpression) typeExpr).type);
            if (type == null) {
                this.errors.add(new UnknownTypeError(typeExpr.signature()));
                return unknownType;
            }
            if (!type.isParametrized()) {
                this.errors.add(new UnparametrizableTypeError(type.displayName()));
                return type;
            }
            ParametrizedMegaType baseType = (ParametrizedMegaType) type;

            List<TypeExpression> typeArgs = ((ParametrizedTypeExpression) typeExpr).typeArgs;
            List<MegaType> argTypes = typeArgs.stream()
                .map(typeArg -> resolveType(typeArg, typeEnvironment))
                .collect(toList());

            int expectedNumTypeArgs = baseType.numTypeArgs();
            int suppliedNumTypeArgs = argTypes.size();
            if (expectedNumTypeArgs < suppliedNumTypeArgs) {
                this.errors.add(new ParametrizableTypeArityError(baseType.displayName(), expectedNumTypeArgs, suppliedNumTypeArgs));
                return baseType.applyTypeArgs(argTypes.subList(0, expectedNumTypeArgs));
            } else if (expectedNumTypeArgs > suppliedNumTypeArgs) {
                this.errors.add(new ParametrizableTypeArityError(baseType.displayName(), expectedNumTypeArgs, suppliedNumTypeArgs));
                for (int i = suppliedNumTypeArgs; i < expectedNumTypeArgs; i++) {
                    argTypes.add(unknownType);
                }
                return baseType.applyTypeArgs(argTypes);
            }

            return baseType.applyTypeArgs(argTypes);
        }

        if (typeExpr instanceof FunctionTypeExpression) {
            FunctionTypeExpression funcTypeExpr = (FunctionTypeExpression) typeExpr;
            List<MegaType> paramTypes = funcTypeExpr.paramTypes.stream()
                .map(paramType -> resolveType(paramType, typeEnvironment))
                .collect(toList());
            MegaType returnType = resolveType(funcTypeExpr.returnType, typeEnvironment);
            return new FunctionType(paramTypes, returnType);
        }

        if (typeExpr instanceof StructTypeExpression) {
            StructTypeExpression structTypeExpr = (StructTypeExpression) typeExpr;
            Map<String, MegaType> propTypes = structTypeExpr.propTypes.entrySet().stream().collect(toMap(
                Entry::getKey,
                entry -> resolveType(entry.getValue(), typeEnvironment)
            ));
            return new ObjectType(propTypes);
        }

        return unknownType;
    }

    private void typecheckStatements(List<Statement> statements, TypeEnvironment env) {
        for (Statement statement : statements) {
            this.typecheckNode(statement, env);
        }
    }

    private void typecheckLetStatement(LetStatement statement, TypeEnvironment env) {
        typecheckBindingStatement(statement.name, statement.value, true, env);
    }

    private void typecheckVarStatement(VarStatement statement, TypeEnvironment env) {
        typecheckBindingStatement(statement.name, statement.value, false, env);
    }

    private void typecheckBindingStatement(Identifier name, Expression value, boolean isImmutable, TypeEnvironment env) {
        MegaType expectedType = null;
        if (name.typeAnnotation != null) {
            expectedType = this.resolveType(name.typeAnnotation, env);
        }

        MegaType type = this.typecheckNode(value, env, expectedType).type;
        env.addBindingWithType(name.value, type, isImmutable);
    }

    private void typecheckFunctionDeclarationStatement(FunctionDeclarationStatement statement, TypeEnvironment env) {
        TypeEnvironment childEnv = env.createChildEnvironment();
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
                MegaType type = this.resolveType(parameter.typeAnnotation, env);
                paramTypes.add(type);
                childEnv.addBindingWithType(parameter.value, type, true);
            }
        }

        //TODO: Pass expected type here when function declarations' typeAnnotation is actually a TypeExpression
        MegaType returnType = typecheckNode(statement.body, childEnv).type;

        if (statement.typeAnnotation != null) {
            MegaType declaredReturnType = env.getTypeByName(statement.typeAnnotation);
            if (declaredReturnType == null) {
                this.errors.add(new UnknownTypeError(statement.typeAnnotation));
            } else {
                if (!declaredReturnType.isEquivalentTo(returnType)) {
                    this.errors.add(new TypeMismatchError(declaredReturnType, returnType));
                }
                env.addBindingWithType(statement.name.value, new FunctionType(paramTypes, declaredReturnType), true);
            }
        }

        env.addBindingWithType(statement.name.value, new FunctionType(paramTypes, returnType), true);
    }

    private void typecheckForLoopStatement(ForLoopStatement statement, TypeEnvironment env) {
        TypeEnvironment childEnv = env.createChildEnvironment();
        String iterator = statement.iterator.value;

        MegaType iterateeType = typecheckNode(statement.iteratee, env).type;
        ArrayType arrayAnyType = new ArrayType(PrimitiveTypes.ANY);
        if (!arrayAnyType.isEquivalentTo(iterateeType)) {
            this.errors.add(new TypeMismatchError(arrayAnyType, iterateeType));
            childEnv.addBindingWithType(iterator, unknownType, true);
        } else {
            childEnv.addBindingWithType(iterator, ((ArrayType) iterateeType).typeArg, true);
        }

        typecheckNode(statement.block, childEnv);
    }

    private void typecheckTypeDeclarationStatement(TypeDeclarationStatement statement, TypeEnvironment env) {
        String typeName = statement.typeName.value;
        MegaType type = resolveType(statement.typeExpr, env);
        if (type instanceof ObjectType) {
            type = new StructType(typeName, ((ObjectType) type).properties);
        }

        switch (env.addType(typeName, type)) {
            case E_DUPLICATE:
                this.errors.add(new DuplicateTypeError(typeName));
                break;
            case NO_ERROR:
            default:
                // No action needed, type has been saved
        }
    }

    @VisibleForTesting
    MegaType typecheckLiteralExpression(MegaType literalType, @Nullable MegaType expectedType) {
        if (expectedType == null) {
            return literalType;
        }

        if (!expectedType.isEquivalentTo(literalType)) {
            this.errors.add(new TypeMismatchError(expectedType, literalType));
        }
        return expectedType;
    }

    @VisibleForTesting
    MegaType typecheckArrayLiteral(ArrayLiteral array, TypeEnvironment env, @Nullable MegaType expectedType) {
        if (array.elements.isEmpty()) {
            return new ArrayType(PrimitiveTypes.NOTHING);
        }
        List<Expression> elements = array.elements;

        MegaType expectedTypeArg = (expectedType != null && expectedType instanceof ArrayType)
            ? ((ArrayType) expectedType).typeArg
            : null;

        MegaType type = expectedTypeArg;  // The type arg is the expectedType arg if passed, else the type of elem 0
        MegaType firstMismatch = null;  // Used to detect if elems differ from first elem type, when no expected passed
        for (Expression elem : elements) {
            TypedNode<Expression> elemTypeResult = typecheckNode(elem, env, expectedTypeArg);
            if (type == null) {
                type = elemTypeResult.type;
            }
            if (!elemTypeResult.type.equals(type)) {
                firstMismatch = elemTypeResult.type;
            }
        }

        ArrayType arrayType = new ArrayType(type);
        if (expectedType == null) {
            // Only need to check firstMismatch if no expectedType passed
            if (firstMismatch != null) {
                this.errors.add(new TypeMismatchError(type, firstMismatch));
            }
            return arrayType;
        }

        if (!expectedType.isEquivalentTo(arrayType)) {
            this.errors.add(new TypeMismatchError(expectedType, arrayType));
        }

        return arrayType;
    }

    @VisibleForTesting
    MegaType typecheckObjectLiteral(ObjectLiteral object, TypeEnvironment env, @Nullable MegaType expectedType) {
        Map<String, MegaType> objectPropertyTypes = object.pairs.entrySet().stream()
            .collect(toMap(
                entry -> entry.getKey().value,
                entry -> typecheckNode(entry.getValue(), env).type
            ));
        ObjectType type = new ObjectType(objectPropertyTypes);
        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(type)) {
                this.errors.add(new TypeMismatchError(expectedType, type));
            }
            return expectedType;
        }
        return type;
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
        TypeEnvironment childEnv = env.createChildEnvironment();
        List<Statement> statements = expr.statements;

        // An empty block-expr should have type Unit
        MegaType blockType = PrimitiveTypes.UNIT;

        for (int i = 0; i < statements.size(); i++) {
            MegaType type = typecheckNode(statements.get(i), childEnv).type;
            if (i == statements.size() - 1) {
                blockType = type;
            }
        }
        return blockType;
    }

    @VisibleForTesting
    MegaType typecheckIdentifier(Identifier identifier, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType identifierType = env.getTypeForBinding(identifier.value);
        if (expectedType != null) {
            if (expectedType.isEquivalentTo(identifierType)) {
                return expectedType;
            } else {
                this.errors.add(new TypeMismatchError(expectedType, identifierType));
            }
        }
        return identifierType == null ? unknownType : identifierType;
    }

    @VisibleForTesting
    MegaType typecheckArrowFunctionExpression(ArrowFunctionExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        TypeEnvironment childEnv = env.createChildEnvironment();
        int numParams = expr.parameters.size();
        List<MegaType> paramTypes = Lists.newArrayListWithExpectedSize(numParams);

        List<MegaType> expectedParamTypes = (expectedType != null && expectedType instanceof FunctionType)
            ? ((FunctionType) expectedType).paramTypes
            : Collections.nCopies(numParams, null);

        for (int i = 0; i < numParams; i++) {
            Identifier parameter = expr.parameters.get(i);
            MegaType expectedParamType = expectedParamTypes.get(i);

            MegaType paramType;
            if (parameter.typeAnnotation == null) {
                if (expectedParamType != null) {
                    paramType = expectedParamType;
                } else {
                    paramType = notInferredType;
                }
            } else {
                paramType = this.resolveType(parameter.typeAnnotation, env);
            }
            paramTypes.add(paramType);
            childEnv.addBindingWithType(parameter.value, paramType, true);
        }

        MegaType returnType = typecheckNode(expr.body, childEnv).type;
        FunctionType functionType = new FunctionType(paramTypes, returnType);
        if (expectedType != null && !expectedType.isEquivalentTo(functionType)) {
            this.errors.add(new TypeMismatchError(expectedType, functionType));
        }
        return functionType;
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

    private MegaType typecheckIndexExpression(IndexExpression expr, TypeEnvironment env) {
        MegaType targetType = typecheckNode(expr.target, env).type;
        if (!new ArrayType(PrimitiveTypes.ANY).isEquivalentTo(targetType)) {
            this.errors.add(new UnindexableTypeError(targetType));
            return unknownType;
        } else {
            ArrayType arrayType = (ArrayType) targetType;

            MegaType indexType = typecheckNode(expr.index, env).type;
            if (!PrimitiveTypes.INTEGER.isEquivalentTo(indexType)) {
                this.errors.add(new TypeMismatchError(PrimitiveTypes.INTEGER, indexType));
            }

            return arrayType.typeArg;
        }
    }

    private MegaType typecheckAssignmentExpression(AssignmentExpression expr, TypeEnvironment env) {
        String bindingName = expr.name.value;
        MegaType rightType = typecheckNode(expr.right, env).type;

        MegaType type = env.getTypeForBinding(bindingName);

        if (type != null && !type.isEquivalentTo(rightType)) {
            this.errors.add(new TypeMismatchError(type, rightType));
        }

        switch (env.setTypeForBinding(bindingName, rightType)) {
            case E_IMMUTABLE:
                this.errors.add(new MutabilityError(bindingName));
            case E_NOBINDING:
                this.errors.add(new UnknownIdentifierError(bindingName));
            case E_DUPLICATE:
                // This should not happen
            case NO_ERROR:
            default:
                return PrimitiveTypes.UNIT;
        }
    }

    private MegaType typecheckRangeExpression(RangeExpression expr, TypeEnvironment env) {
        MegaType leftBoundType = typecheckNode(expr.leftBound, env).type;
        if (!PrimitiveTypes.INTEGER.isEquivalentTo(leftBoundType)) {
            this.errors.add(new TypeMismatchError(PrimitiveTypes.INTEGER, leftBoundType));
        }

        MegaType rightBoundType = typecheckNode(expr.rightBound, env).type;
        if (!PrimitiveTypes.INTEGER.isEquivalentTo(rightBoundType)) {
            this.errors.add(new TypeMismatchError(PrimitiveTypes.INTEGER, rightBoundType));
        }

        return new ArrayType(PrimitiveTypes.INTEGER);
    }
}
