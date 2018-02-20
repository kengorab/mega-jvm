package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.typechecking.OperatorTypeChecker.isBooleanOperator;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

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
import co.kenrg.mega.frontend.ast.iface.Exportable;
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
import co.kenrg.mega.frontend.ast.type.BasicTypeExpression;
import co.kenrg.mega.frontend.ast.type.FunctionTypeExpression;
import co.kenrg.mega.frontend.ast.type.ParametrizedTypeExpression;
import co.kenrg.mega.frontend.ast.type.StructTypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpressions;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.typechecking.OperatorTypeChecker.OperatorSignature;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment.Binding;
import co.kenrg.mega.frontend.typechecking.errors.DuplicateExportError;
import co.kenrg.mega.frontend.typechecking.errors.DuplicateTypeError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionArityError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionDuplicateNamedArgumentError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionInvalidNamedArgumentError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionMissingNamedArgumentError;
import co.kenrg.mega.frontend.typechecking.errors.FunctionWithDefaultParamValuesArityError;
import co.kenrg.mega.frontend.typechecking.errors.IllegalOperatorError;
import co.kenrg.mega.frontend.typechecking.errors.MissingParameterTypeAnnotationError;
import co.kenrg.mega.frontend.typechecking.errors.MutabilityError;
import co.kenrg.mega.frontend.typechecking.errors.ParametrizableTypeArityError;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UnindexableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UninvokeableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownExportError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownIdentifierError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownModuleError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownOperatorError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownPropertyError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnparametrizableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnsupportedFeatureError;
import co.kenrg.mega.frontend.typechecking.errors.VisibilityError;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType.Kind;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.ParametrizedMegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import co.kenrg.mega.frontend.typechecking.types.StructType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
    private Function<String, Optional<TypeCheckResult<Module>>> moduleProvider;

    public TypeChecker() {
        this.errors = Lists.newArrayList();
    }

    public void setModuleProvider(Function<String, Optional<TypeCheckResult<Module>>> moduleProvider) {
        this.moduleProvider = moduleProvider;
    }

    public <T extends Node> TypeCheckResult<T> typecheck(T node, TypeEnvironment env) {
        MegaType type = typecheckNode(node, env, null);
        return new TypeCheckResult<>(node, type, this.errors, env);
    }

    private <T extends Node> MegaType typecheckNode(T node, TypeEnvironment env) {
        return this.typecheckNode(node, env, null);
    }

    private <T extends Node> MegaType typecheckNode(T node, TypeEnvironment env, @Nullable MegaType expectedType) {
        // Statements
        if (node instanceof Module) {
            this.typecheckModule((Module) node, env);
        } else if (node instanceof ExpressionStatement) {
            return typecheckNode(((ExpressionStatement) node).expression, env, expectedType);
        } else if (node instanceof ValStatement) {
            this.typecheckValStatement((ValStatement) node, env);
        } else if (node instanceof VarStatement) {
            this.typecheckVarStatement((VarStatement) node, env);
        } else if (node instanceof FunctionDeclarationStatement) {
            this.typecheckFunctionDeclarationStatement((FunctionDeclarationStatement) node, env);
        } else if (node instanceof ForLoopStatement) {
            this.typecheckForLoopStatement((ForLoopStatement) node, env);
        } else if (node instanceof TypeDeclarationStatement) {
            this.typecheckTypeDeclarationStatement((TypeDeclarationStatement) node, env);
        } else if (node instanceof ImportStatement) {
            this.typecheckImportStatement((ImportStatement) node, env);
        }

        // Expressions
        if (node instanceof IntegerLiteral) {
            return this.typecheckLiteralExpression(PrimitiveTypes.INTEGER, expectedType, node);
        } else if (node instanceof FloatLiteral) {
            return this.typecheckLiteralExpression(PrimitiveTypes.FLOAT, expectedType, node);
        } else if (node instanceof BooleanLiteral) {
            return this.typecheckLiteralExpression(PrimitiveTypes.BOOLEAN, expectedType, node);
        } else if (node instanceof StringLiteral) {
            return this.typecheckLiteralExpression(PrimitiveTypes.STRING, expectedType, node);
        } else if (node instanceof ArrayLiteral) {
            return this.typecheckArrayLiteral((ArrayLiteral) node, env, expectedType);
        } else if (node instanceof ObjectLiteral) {
            return this.typecheckObjectLiteral((ObjectLiteral) node, env, expectedType);
        } else if (node instanceof ParenthesizedExpression) {
            return this.typecheckParenthesizedExpression((ParenthesizedExpression) node, env, expectedType);
        } else if (node instanceof PrefixExpression) {
            return this.typecheckPrefixExpression((PrefixExpression) node, env, expectedType);
        } else if (node instanceof InfixExpression) {
            return this.typecheckInfixExpression((InfixExpression) node, env, expectedType);
        } else if (node instanceof IfExpression) {
            return this.typecheckIfExpression((IfExpression) node, env, expectedType);
        } else if (node instanceof BlockExpression) {
            return this.typecheckBlockExpression((BlockExpression) node, env, expectedType);
        } else if (node instanceof Identifier) {
            return this.typecheckIdentifier((Identifier) node, env, expectedType);
        } else if (node instanceof ArrowFunctionExpression) {
            return this.typecheckArrowFunctionExpression((ArrowFunctionExpression) node, env, expectedType);
        } else if (node instanceof CallExpression) {
            return this.typecheckCallExpression((CallExpression) node, env, expectedType);
        } else if (node instanceof IndexExpression) {
            return this.typecheckIndexExpression((IndexExpression) node, env, expectedType);
        } else if (node instanceof AssignmentExpression) {
            return this.typecheckAssignmentExpression((AssignmentExpression) node, env, expectedType);
        } else if (node instanceof AccessorExpression) {
            return this.typecheckAccessorExpression((AccessorExpression) node, env, expectedType);
        } else if (node instanceof RangeExpression) {
            return this.typecheckRangeExpression((RangeExpression) node, env, expectedType);
        }

        return PrimitiveTypes.UNIT;
    }

    private MegaType resolveType(TypeExpression typeExpr, TypeEnvironment typeEnvironment) {
        if (typeExpr instanceof BasicTypeExpression) {
            MegaType type = typeEnvironment.getTypeByName(((BasicTypeExpression) typeExpr).type);
            if (type == null) {
                this.errors.add(new UnknownTypeError(typeExpr.signature(), typeExpr.position));
                return unknownType;
            }
            return type;
        }

        if (typeExpr instanceof ParametrizedTypeExpression) {
            MegaType type = typeEnvironment.getTypeByName(((ParametrizedTypeExpression) typeExpr).type);
            if (type == null) {
                this.errors.add(new UnknownTypeError(typeExpr.signature(), typeExpr.position));
                return unknownType;
            }
            if (!type.isParametrized()) {
                this.errors.add(new UnparametrizableTypeError(type.displayName(), typeExpr.position));
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
                this.errors.add(new ParametrizableTypeArityError(baseType.displayName(), expectedNumTypeArgs, suppliedNumTypeArgs, typeExpr.position));
                return baseType.applyTypeArgs(argTypes.subList(0, expectedNumTypeArgs));
            } else if (expectedNumTypeArgs > suppliedNumTypeArgs) {
                this.errors.add(new ParametrizableTypeArityError(baseType.displayName(), expectedNumTypeArgs, suppliedNumTypeArgs, typeExpr.position));
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
            return FunctionType.ofSignature(paramTypes, returnType);
        }

        if (typeExpr instanceof StructTypeExpression) {
            StructTypeExpression structTypeExpr = (StructTypeExpression) typeExpr;
            LinkedHashMultimap<String, MegaType> propTypes = LinkedHashMultimap.create();
            structTypeExpr.propTypes.forEach((propName, propTypeExpr) -> {
                propTypes.put(propName, resolveType(propTypeExpr, typeEnvironment));
            });
//            List<Pair<String, MegaType>> propTypes = structTypeExpr.propTypes.stream()
//                .map(propType -> Pair.of(propType.getKey(), resolveType(propType.getValue(), typeEnvironment)))
//                .collect(toList());
            return new ObjectType(propTypes);
        }

        return unknownType;
    }

    private void typecheckModule(Module module, TypeEnvironment env) {
        for (Statement statement : module.statements) {
            this.typecheckNode(statement, env);
        }

        for (Statement exportedStmt : module.exports) {
            assert exportedStmt instanceof Exportable;
            Exportable export = (Exportable) exportedStmt;

            String exportName = export.exportName();
            if (module.namedExports.containsKey(exportName)) {
                this.errors.add(new DuplicateExportError(exportName, exportedStmt.getToken().position));
            }
            module.namedExports.put(exportName, exportedStmt);
        }
    }

    @VisibleForTesting
    void typecheckValStatement(ValStatement statement, TypeEnvironment env) {
        typecheckBindingStatement(statement.name, statement.value, true, env);
    }

    private void typecheckVarStatement(VarStatement statement, TypeEnvironment env) {
        typecheckBindingStatement(statement.name, statement.value, false, env);
    }

    private boolean containsInferences(MegaType type) {
        if (type instanceof ParametrizedMegaType) {
            return ((ParametrizedMegaType) type).typeArgs().contains(notInferredType);
        } else if (type instanceof FunctionType) {
            return ((FunctionType) type).paramTypes.contains(notInferredType);
        }
        return false;
    }

    private void typecheckBindingStatement(Identifier name, Expression value, boolean isImmutable, TypeEnvironment env) {
        MegaType expectedType = null;
        if (name.typeAnnotation != null) {
            expectedType = this.resolveType(name.typeAnnotation, env);
        }

        MegaType type = this.typecheckNode(value, env, expectedType);
        if (containsInferences(type)) { // Save expression for later re-evaluation of types
            env.addBindingWithType(name.value, type, isImmutable, value);
        } else {
            env.addBindingWithType(name.value, type, isImmutable);
        }
        name.setType(type);
    }

    private void typecheckFunctionDeclarationStatement(FunctionDeclarationStatement statement, TypeEnvironment env) {
        // TODO: Only allow function declarations at top-level; typechecking should fail if env.parent != null
        TypeEnvironment childEnv = env.createChildEnvironment();

        for (Parameter parameter : statement.parameters) {
            if (parameter.ident.typeAnnotation == null) {
                this.errors.add(new MissingParameterTypeAnnotationError(parameter.ident.value, parameter.ident.token.position));
            } else {
                MegaType annotatedType = this.resolveType(parameter.ident.typeAnnotation, env);
                MegaType paramType;
                if (parameter.hasDefaultValue()) {
                    paramType = this.typecheckNode(parameter.defaultValue, env, annotatedType);
                } else {
                    paramType = annotatedType;
                }
                childEnv.addBindingWithType(parameter.ident.value, paramType, true);
                parameter.ident.setType(paramType);
            }
        }

        MegaType declaredReturnType = statement.typeAnnotation != null
            ? env.getTypeByName(statement.typeAnnotation)
            : null;
        MegaType returnType = typecheckNode(statement.body, childEnv, declaredReturnType);

        if (statement.typeAnnotation != null) {
            if (declaredReturnType == null) {
                this.errors.add(new UnknownTypeError(statement.typeAnnotation, statement.token.position));
            } else {
                if (!declaredReturnType.isEquivalentTo(returnType)) {
                    this.errors.add(new TypeMismatchError(declaredReturnType, returnType, statement.body.getToken().position));
                }
                env.addBindingWithType(statement.name.value, new FunctionType(statement.parameters, declaredReturnType, Kind.METHOD), true);
            }
        } else {
            env.addBindingWithType(statement.name.value, new FunctionType(statement.parameters, returnType, Kind.METHOD), true);
        }
    }

    private void typecheckForLoopStatement(ForLoopStatement statement, TypeEnvironment env) {
        TypeEnvironment childEnv = env.createChildEnvironment();
        String iterator = statement.iterator.value;

        MegaType iterateeType = typecheckNode(statement.iteratee, env);
        ArrayType arrayAnyType = new ArrayType(PrimitiveTypes.ANY);
        if (!arrayAnyType.isEquivalentTo(iterateeType)) {
            this.errors.add(new TypeMismatchError(arrayAnyType, iterateeType, statement.iteratee.getToken().position));
            childEnv.addBindingWithType(iterator, unknownType, true);
        } else {
            MegaType iteratorType = ((ArrayType) iterateeType).typeArg;
            childEnv.addBindingWithType(iterator, iteratorType, true);
            statement.iterator.setType(iteratorType);
        }

        typecheckNode(statement.block, childEnv);
    }

    private void typecheckTypeDeclarationStatement(TypeDeclarationStatement statement, TypeEnvironment env) {
        String typeName = statement.typeName.value;
        MegaType type = resolveType(statement.typeExpr, env);
        if (type instanceof ObjectType) {
            LinkedHashMultimap<String, MegaType> properties = type.getProperties();
            type = new StructType(typeName, properties);

            List<Identifier> params = properties.entries().stream()
                .map(prop -> {
                    String propName = prop.getKey();
                    MegaType propType = prop.getValue();

                    TypeExpression typeExpr = TypeExpressions.fromType(propType);
                    return new Identifier(Token.ident(propName, null), propName, typeExpr, propType);
                })
                .collect(toList());
            env.addBindingWithType(typeName, FunctionType.constructor(params, type), true);
        }

        switch (env.addType(typeName, type)) {
            case E_DUPLICATE:
                this.errors.add(new DuplicateTypeError(typeName, statement.token.position));
                break;
            case NO_ERROR:
            default:
                // No action needed, type has been saved
        }
    }

    private void typecheckImportStatement(ImportStatement node, TypeEnvironment env) {
        if (this.moduleProvider == null) {
            throw new IllegalStateException("Module provider function not set on TypeChecker");
        }

        Optional<TypeCheckResult<Module>> targetModuleResultOpt = this.moduleProvider.apply(node.targetModule.value);
        if (!targetModuleResultOpt.isPresent()) {
            this.errors.add(new UnknownModuleError(node.targetModule.value, node.targetModule.token.position));

            for (Identifier _import : node.imports) {
                String importName = _import.value;
                env.addBindingWithType(importName, unknownType, true);
            }
            return;
        }
        TypeCheckResult<Module> targetModuleResult = targetModuleResultOpt.get();

        Module module = targetModuleResult.node;
        TypeEnvironment moduleTypeEnv = targetModuleResult.typeEnvironment;

        for (Identifier _import : node.imports) {
            String importName = _import.value;

            MegaType importType;
            Binding binding = moduleTypeEnv.getBinding(importName);
            if (binding == null) {
                this.errors.add(new UnknownExportError(node.targetModule.value, importName, _import.token.position));
                importType = unknownType;
            } else {
                if (!module.namedExports.containsKey(importName)) {
                    this.errors.add(new VisibilityError(node.targetModule.value, importName, _import.token.position));
                }
                importType = binding.type;
            }
            env.addBindingWithType(importName, importType, true);
        }
    }

    @VisibleForTesting
    MegaType typecheckLiteralExpression(MegaType literalType, @Nullable MegaType expectedType, Node node) {
        if (expectedType == null) {
            node.setType(literalType);
            return literalType;
        }

        if (!expectedType.isEquivalentTo(literalType)) {
            this.errors.add(new TypeMismatchError(expectedType, literalType, node.getToken().position));
        }
        node.setType(expectedType);
        return expectedType;
    }

    @VisibleForTesting
    MegaType typecheckArrayLiteral(ArrayLiteral array, TypeEnvironment env, @Nullable MegaType expectedType) {
        if (array.elements.isEmpty()) {
            MegaType type;
            if (expectedType != null) {
                type = expectedType;
            } else {
                type = new ArrayType(PrimitiveTypes.NOTHING);
            }
            array.setType(type);
            return type;
        }

        List<Expression> elements = array.elements;

        MegaType expectedTypeArg = (expectedType != null && expectedType instanceof ArrayType)
            ? ((ArrayType) expectedType).typeArg
            : null;

        MegaType type = expectedTypeArg;  // The type arg is the expectedType arg if passed, else the type of elem 0
        Pair<MegaType, Position> firstMismatch = null;  // Used to detect if elems differ from first elem type, when no expected passed
        for (Expression elem : elements) {
            MegaType elemType = typecheckNode(elem, env, expectedTypeArg);
            if (type == null) {
                type = elemType;
            }
            if (!elemType.equals(type)) {
                firstMismatch = Pair.of(elemType, elem.getToken().position);
            }
        }

        ArrayType arrayType = new ArrayType(type);
        if (expectedType == null) {
            // Only need to check firstMismatch if no expectedType passed
            if (firstMismatch != null) {
                this.errors.add(new TypeMismatchError(type, firstMismatch.getLeft(), firstMismatch.getRight()));
            }
            array.setType(arrayType);
            return arrayType;
        }

        if (!expectedType.isEquivalentTo(arrayType)) {
            this.errors.add(new TypeMismatchError(expectedType, arrayType, array.token.position));
        }

        array.setType(arrayType);
        return arrayType;
    }

    @VisibleForTesting
    MegaType typecheckParenthesizedExpression(ParenthesizedExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType type = this.typecheckNode(expr.expr, env, expectedType);
        expr.setType(type);
        return type;
    }

    @VisibleForTesting
    MegaType typecheckObjectLiteral(ObjectLiteral object, TypeEnvironment env, @Nullable MegaType expectedType) {
        final LinkedHashMultimap<String, MegaType> objectPropertyTypes = LinkedHashMultimap.create();

        if (expectedType != null && (expectedType instanceof StructType || expectedType instanceof ObjectType)) {
            if (expectedType instanceof StructType) {
                this.errors.add(new UnsupportedFeatureError("Object literals cannot be coerced to struct types", object.token.position));
                object.setType(unknownType);
                return unknownType;
            }

            Map<String, MegaType> expectedPairs = ((ObjectType) expectedType).properties.entries().stream()
                .collect(toMap(Entry::getKey, Entry::getValue));

            object.pairs.forEach((ident, expr) -> {
                MegaType expectedPairType = expectedPairs.get(ident.value);
                objectPropertyTypes.put(ident.value, typecheckNode(expr, env, expectedPairType));
            });

//            objectPropertyTypes = object.pairs.stream()
//                .map(pair -> {
//                    MegaType expectedPairType = expectedPairs.get(pair.getKey().value);
//                    return Pair.of(pair.getKey().value, typecheckNode(pair.getValue(), env, expectedPairType));
//                })
//                .collect(toList());
        } else {
            object.pairs.forEach((ident, expr) -> {
                objectPropertyTypes.put(ident.value, typecheckNode(expr, env));
            });
//            objectPropertyTypes = object.pairs.stream()
//                .map(pair -> Pair.of(pair.getKey().value, typecheckNode(pair.getValue(), env)))
//                .collect(toList());
        }

        ObjectType type = new ObjectType(objectPropertyTypes);
        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(type)) {
                this.errors.add(new TypeMismatchError(expectedType, type, object.token.position));
            }
            object.setType(expectedType);
            return expectedType;
        }
        object.setType(type);
        return type;
    }

    @VisibleForTesting
    MegaType typecheckPrefixExpression(PrefixExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        switch (expr.operator) {
            case "!":
                if (expectedType != null && !expectedType.equals(PrimitiveTypes.BOOLEAN)) {
                    this.errors.add(new TypeMismatchError(expectedType, PrimitiveTypes.BOOLEAN, expr.token.position));
                }
                expr.setType(PrimitiveTypes.BOOLEAN);
                return PrimitiveTypes.BOOLEAN;
            case "-": {
                typecheckNode(expr.expression, env);
                MegaType exprType = expr.expression.getType();
                if (expectedType != null) {
                    if (!expectedType.isEquivalentTo(exprType)) {
                        this.errors.add(new TypeMismatchError(expectedType, exprType, expr.token.position));
                    }
                } else {
                    if (!PrimitiveTypes.NUMBER.isEquivalentTo(exprType)) {
                        this.errors.add(new TypeMismatchError(PrimitiveTypes.NUMBER, exprType, expr.token.position));
                        expr.setType(unknownType);
                        return unknownType;
                    }
                }
                expr.setType(exprType);
                return exprType;
            }
            default:
                expr.setType(unknownType);
                return unknownType;
        }
    }

    @VisibleForTesting
    MegaType typecheckInfixExpression(InfixExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType leftType = typecheckNode(expr.left, env);
        MegaType rightType = typecheckNode(expr.right, env);
        if (leftType == null || rightType == null) {
            expr.setType(unknownType);
            return unknownType;
        }

        // If no assumptions can be made about the operand types, bail early
        // TODO: Be smarter about this, e.g. if lType is <NotInferred> but the operator is < and rType is Int, then we should be able to infer that lType is also Int
        if (leftType.equals(unknownType) || rightType.equals(unknownType) ||
            leftType.equals(notInferredType) || rightType.equals(notInferredType)) {
            expr.setType(unknownType);
            return unknownType;
        }

        List<OperatorSignature> opSignatures = OperatorTypeChecker.signaturesForOperator(expr.operator);
        if (opSignatures == null) {
            this.errors.add(new UnknownOperatorError(expr.operator, expr.token.position));
            expr.setType(unknownType);
            return unknownType;
        }

        List<OperatorSignature> possibleOperators = opSignatures.stream()
            .filter(signature -> signature.lType.isEquivalentTo(leftType) && signature.rType.isEquivalentTo(rightType))
            .collect(toList());

        if (possibleOperators.isEmpty()) {
            this.errors.add(new IllegalOperatorError(expr.operator, leftType, rightType, expr.token.position));
            MegaType type = isBooleanOperator(expr.operator) ? PrimitiveTypes.BOOLEAN : unknownType;
            expr.setType(type);
            return type;
        }

        OperatorSignature operator = possibleOperators.get(0);
        MegaType type = operator.returnType;

        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(type)) {
                this.errors.add(new TypeMismatchError(expectedType, type, expr.token.position));
            }
            expr.setType(expectedType);
            return expectedType;
        }

        expr.setType(type);
        return type;
    }

    @VisibleForTesting
    MegaType typecheckIfExpression(IfExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        typecheckNode(expr.condition, env, PrimitiveTypes.BOOLEAN);

        MegaType thenBlockType = typecheckNode(expr.thenExpr, env, expectedType);

        if (expr.elseExpr != null) {
            MegaType elseBlockType = typecheckNode(expr.elseExpr, env, expectedType);
            if (expectedType == null) {
                if (!thenBlockType.isEquivalentTo(elseBlockType)) {
                    this.errors.add(new TypeMismatchError(thenBlockType, elseBlockType, expr.elseExpr.getToken().position));
                }
                expr.setType(thenBlockType);
                return thenBlockType;
            }
            expr.setType(expectedType);
            return expectedType;
        } else {
            if (expectedType != null && !PrimitiveTypes.UNIT.isEquivalentTo(expectedType)) {
                this.errors.add(new TypeMismatchError(expectedType, PrimitiveTypes.UNIT, expr.thenExpr.getToken().position));
                expr.setType(expectedType);
                return expectedType;
            }
            expr.setType(PrimitiveTypes.UNIT);
            return PrimitiveTypes.UNIT;
        }
    }

    // BlockExpressions are only accessible via if-expressions and arrow/plain function bodies there's no way to create
    // a BlockExpression on its own.
    private MegaType typecheckBlockExpression(BlockExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        TypeEnvironment childEnv = env.createChildEnvironment();
        List<Statement> statements = expr.statements;

        // An empty block-expr should have type Unit
        MegaType blockType = PrimitiveTypes.UNIT;

        for (int i = 0; i < statements.size(); i++) {
            MegaType type = typecheckNode(statements.get(i), childEnv);
            if (i == statements.size() - 1) {
                blockType = type;
            }
        }
        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(blockType)) {
                Statement lastStatement = statements.get(statements.size() - 1);
                this.errors.add(new TypeMismatchError(expectedType, blockType, lastStatement.getToken().position));
            }
            expr.setType(expectedType);
            return expectedType;
        }
        expr.setType(blockType);
        return blockType;
    }

    @VisibleForTesting
    MegaType typecheckIdentifier(Identifier identifier, TypeEnvironment env, @Nullable MegaType expectedType) {
        Binding binding = env.getBinding(identifier.value);
        MegaType identifierType = (binding == null)
            ? null
            : (binding.expression != null) ? typecheckNode(binding.expression, env, expectedType) : binding.type;
        if (expectedType != null) {
            if (expectedType.isEquivalentTo(identifierType)) {
                identifier.setType(expectedType);
                return expectedType;
            } else {
                this.errors.add(new TypeMismatchError(expectedType, identifierType, identifier.token.position));
            }
        }
        MegaType type = identifierType == null ? unknownType : identifierType;
        identifier.setType(type);
        return type;
    }

    @VisibleForTesting
    MegaType typecheckArrowFunctionExpression(ArrowFunctionExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        Map<String, Binding> capturedBindings = Maps.newHashMap();
        TypeEnvironment childEnv = env.createChildEnvironment();
        childEnv.setOnAccessBindingFromOuterScope(capturedBindings::put);

        int numParams = expr.parameters.size();
        List<MegaType> expectedParamTypes = (expectedType != null && expectedType instanceof FunctionType)
            ? ((FunctionType) expectedType).paramTypes
            : Collections.nCopies(numParams, null);

        for (int i = 0; i < numParams; i++) {
            Parameter parameter = expr.parameters.get(i);
            MegaType expectedParamType = expectedParamTypes.get(i);

            if (parameter.hasDefaultValue()) {
                this.errors.add(new UnsupportedFeatureError("Arrow functions cannot have default-valued parameters", parameter.ident.token.position));
            }

            MegaType paramType;
            if (parameter.ident.typeAnnotation == null) {
                if (expectedParamType != null) {
                    paramType = expectedParamType;
                } else {
                    paramType = notInferredType;
                }
            } else {
                paramType = this.resolveType(parameter.ident.typeAnnotation, env);
            }
            childEnv.addBindingWithType(parameter.ident.value, paramType, true);
            parameter.ident.setType(paramType);
        }

        MegaType returnType = typecheckNode(expr.body, childEnv);
        FunctionType functionType = new FunctionType(expr.parameters, returnType, capturedBindings, Kind.ARROW_FN);
        if (expectedType != null && !expectedType.isEquivalentTo(functionType)) {
            this.errors.add(new TypeMismatchError(expectedType, functionType, expr.token.position));
        }
        expr.setType(functionType);
        return functionType;
    }

    @VisibleForTesting
    MegaType typecheckCallExpression(CallExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        if (expr instanceof CallExpression.UnnamedArgs) {
            return this.typecheckUnnamedArgsCallExpression((CallExpression.UnnamedArgs) expr, env, expectedType);
        } else if (expr instanceof CallExpression.NamedArgs) {
            return this.typecheckNamedArgsCallExpression((CallExpression.NamedArgs) expr, env, expectedType);
        } else {
            throw new IllegalStateException("No other possible subclass of CallExpression: " + expr.getClass());
        }
    }

    private MegaType typecheckUnnamedArgsCallExpression(CallExpression.UnnamedArgs expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType targetType = typecheckNode(expr.target, env);
        if (!(targetType instanceof FunctionType)) {
            this.errors.add(new UninvokeableTypeError(targetType, expr.target.getToken().position));
            expr.setType(unknownType);
            return unknownType;
        }
        FunctionType funcType = (FunctionType) targetType;
        if (funcType.containsParamsWithDefaultValues()) {
            int minNumParams = (int) funcType.parameters.stream().filter(param -> !param.hasDefaultValue()).count();
            if (expr.arguments.size() < minNumParams) {
                this.errors.add(new FunctionWithDefaultParamValuesArityError(minNumParams, expr.arguments.size(), expr.token.position));
                expr.setType(funcType.returnType);
                return funcType.returnType;
            }
        } else if (funcType.paramTypes.size() != expr.arguments.size()) {
            this.errors.add(new FunctionArityError(funcType.paramTypes.size(), expr.arguments.size(), expr.token.position));
            expr.setType(funcType.returnType);
            return funcType.returnType;
        }

        if (this.containsInferences(funcType)) {
            // If the target contains inferences, make two typechecking passes over it, since we know the param types...
            List<MegaType> paramTypes = expr.arguments.stream()
                .map(arg -> typecheckNode(arg, env))
                .collect(toList());
            FunctionType expectedFuncType = FunctionType.ofSignature(paramTypes, expectedType);
            funcType = (FunctionType) typecheckNode(expr.target, env, expectedFuncType);
        } else {
            // Otherwise, typecheck the passed params with expected param types from non-inferred function type
            for (int i = 0; i < expr.arguments.size(); i++) {
                Expression arg = expr.arguments.get(i);
                MegaType expectedParamType = funcType.paramTypes.get(i);
                typecheckNode(arg, env, expectedParamType);
            }
        }

        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(funcType.returnType)) {
                this.errors.add(new TypeMismatchError(expectedType, funcType.returnType, expr.token.position));
            }
            expr.setType(expectedType);
            return expectedType;
        }
        MegaType type = (funcType.returnType == null) ? unknownType : funcType.returnType;
        expr.setType(type);
        return type;
    }

    private MegaType typecheckNamedArgsCallExpression(CallExpression.NamedArgs expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType targetType = typecheckNode(expr.target, env);
        if (!(targetType instanceof FunctionType)) {
            this.errors.add(new UninvokeableTypeError(targetType, expr.target.getToken().position));
            expr.setType(unknownType);
            return unknownType;
        }
        FunctionType funcType = (FunctionType) targetType;
        assert funcType.parameters != null;

        if (funcType.kind != Kind.METHOD) {
            this.errors.add(new UnsupportedFeatureError("Named argument invocation of arrow functions", expr.token.position));
            expr.setType(funcType.returnType);
            return funcType.returnType;
        }

        // The following 3 blocks act as an alternative to checking the arity:
        // 1. Verify that the named arguments in the expr match the names/types in the function declaration
        Map<String, Parameter> expectedParams = funcType.parameters.stream()
            .collect(toMap(param -> param.ident.value, Function.identity()));
        Map<String, Identifier> encounteredNamedArgs = Maps.newHashMap();
        for (Pair<Identifier, Expression> argument : expr.namedParamArguments) {
            Identifier argIdent = argument.getKey();
            String argName = argIdent.value;
            if (!expectedParams.containsKey(argName)) {
                this.errors.add(new FunctionInvalidNamedArgumentError(argName, argIdent.token.position));
                expr.setType(funcType.returnType);
                return funcType.returnType;
            }
            if (encounteredNamedArgs.containsKey(argName)) {
                this.errors.add(new FunctionDuplicateNamedArgumentError(argName, argIdent.token.position));
                expr.setType(funcType.returnType);
                return funcType.returnType;
            } else {
                encounteredNamedArgs.put(argName, argIdent);
            }
        }

        // 2. If the function type contains inferences (params whose type prior to Calling is <NotInferred>) typecheck
        // the expression for each named param (and set the named param's Identifier's type to be that type), and
        // re-typecheck the expression target, now that the types of the parameters are provided by the Call.
        if (this.containsInferences(funcType)) {
            // If the target contains inferences, make two typechecking passes over it, since we know the param types...
            List<Parameter> namedParamArguments = expr.namedParamArguments.stream()
                .map(arg -> {
                    MegaType argExprType = typecheckNode(arg.getValue(), env);
                    arg.getLeft().setType(argExprType);
                    return new Parameter(arg.getLeft());
                })
                .collect(toList());
            FunctionType expectedFuncType = new FunctionType(namedParamArguments, expectedType, funcType.capturedBindings, funcType.kind);
            funcType = (FunctionType) typecheckNode(expr.target, env, expectedFuncType);
            assert funcType.parameters != null;
        } else {
            // Otherwise, typecheck the passed params with expected param types from non-inferred function type
            for (Pair<Identifier, Expression> argument : expr.namedParamArguments) {
                String argName = argument.getKey().value;
                MegaType expectedArgType = expectedParams.get(argName).getType();
                typecheckNode(argument.getValue(), env, expectedArgType);
            }
        }

        // 3. Verify that the named arguments in the function declaration are all present in the expr
        Map<String, Expression> actualParams = expr.namedParamArguments.stream()
            .collect(toMap(arg -> arg.getKey().value, Pair::getValue));
        for (Parameter parameter : funcType.parameters) {
            String argName = parameter.ident.value;
            if (!actualParams.containsKey(argName) && !parameter.hasDefaultValue()) {
                this.errors.add(new FunctionMissingNamedArgumentError(argName, expr.token.position));
                expr.setType(funcType.returnType);
                return funcType.returnType;
            }
        }

        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(funcType.returnType)) {
                this.errors.add(new TypeMismatchError(expectedType, funcType.returnType, expr.token.position));
            }
            expr.setType(expectedType);
            return expectedType;
        }
        MegaType type = (funcType.returnType == null) ? unknownType : funcType.returnType;
        expr.setType(type);
        return type;
    }

    @VisibleForTesting
    MegaType typecheckIndexExpression(IndexExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        MegaType targetType = typecheckNode(expr.target, env);
        if (!(new ArrayType(PrimitiveTypes.ANY)).isEquivalentTo(targetType)) {
            this.errors.add(new UnindexableTypeError(targetType, expr.target.getToken().position));
            expr.setType(unknownType);
            return unknownType;
        } else {
            ArrayType arrayType = (ArrayType) targetType;
            typecheckNode(expr.index, env, PrimitiveTypes.INTEGER);

            if (expectedType != null) {
                if (!expectedType.isEquivalentTo(arrayType.typeArg)) {
                    this.errors.add(new TypeMismatchError(expectedType, arrayType.typeArg, expr.token.position));
                }
                expr.setType(expectedType);
                return expectedType;
            }
            expr.setType(arrayType.typeArg);
            return arrayType.typeArg;
        }
    }

    @VisibleForTesting
    MegaType typecheckAssignmentExpression(AssignmentExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        if (expectedType != null && !expectedType.equals(PrimitiveTypes.UNIT)) {
            this.errors.add(new TypeMismatchError(expectedType, PrimitiveTypes.UNIT, expr.token.position));
            expr.setType(PrimitiveTypes.UNIT);
            return PrimitiveTypes.UNIT;
        }

        String bindingName = expr.name.value;

        Binding binding = env.getBinding(bindingName);
        MegaType type = (binding != null)
            ? binding.type
            : null;
        MegaType rightType = typecheckNode(expr.right, env, type);

        switch (env.setTypeForBinding(bindingName, rightType)) {
            case E_IMMUTABLE:
                this.errors.add(new MutabilityError(bindingName, expr.name.token.position));
                break;
            case E_NOBINDING:
                this.errors.add(new UnknownIdentifierError(bindingName, expr.name.token.position));
                break;
            case E_DUPLICATE:
            case NO_ERROR:
            default:
                // These cases are unimportant, for the purposes of typechecking
                break;
        }
        expr.setType(PrimitiveTypes.UNIT);
        return PrimitiveTypes.UNIT;
    }

    @VisibleForTesting
    MegaType typecheckAccessorExpression(AccessorExpression node, TypeEnvironment env, @Nullable MegaType expectedType) {
        Expression target = node.target;
        MegaType targetType = typecheckNode(target, env);
        LinkedHashMultimap<String, MegaType> properties = targetType.getProperties();
        // TODO: I wonder if some API like targetType.hasPropMatching(String name, MegaType type) would work? This way is currently too inefficient, I think

        String propName = node.property.value;
        List<MegaType> propTypePossibilities = Lists.newArrayList();
        properties.forEach((_propName, propType) -> {
            if (_propName.equals(propName)) {
                propTypePossibilities.add(propType);
            }
        });
//        for (Entry<String, MegaType> property : properties.entries()) {
//            if (property.getKey().equals(propName)) {
//                propTypePossibilities.add(property.getRight());
//            }
//        }

        if (propTypePossibilities.size() == 0) {
            this.errors.add(new UnknownPropertyError(propName, node.property.token.position));
            MegaType type = expectedType == null ? unknownType : expectedType;
            node.setType(type);
            return type;
        }

        MegaType type = null;
        if (expectedType != null) {
            for (MegaType possibility : propTypePossibilities) {
                if (possibility.isEquivalentTo(expectedType)) {
                    type = possibility;
                    break;
                }
            }

            if (type == null) {
                for (MegaType possibility : propTypePossibilities) {
                    this.errors.add(new TypeMismatchError(expectedType, possibility, node.property.token.position));
                }
                type = expectedType;
            }
        } else {
            type = propTypePossibilities.get(0); // TODO: Fix this - the typechecker methods should probably return a Collection of possible types, and downstream methods can determine which one to use?
        }

        node.setType(type);
        return type;
    }

    @VisibleForTesting
    MegaType typecheckRangeExpression(RangeExpression expr, TypeEnvironment env, @Nullable MegaType expectedType) {
        typecheckNode(expr.leftBound, env, PrimitiveTypes.INTEGER);
        typecheckNode(expr.rightBound, env, PrimitiveTypes.INTEGER);

        ArrayType arrayType = new ArrayType(PrimitiveTypes.INTEGER);
        if (expectedType != null) {
            if (!expectedType.isEquivalentTo(arrayType)) {
                this.errors.add(new TypeMismatchError(expectedType, arrayType, expr.token.position));
            }
            expr.setType(expectedType);
            return expectedType;
        }
        expr.setType(arrayType);
        return arrayType;
    }
}
