package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseExpressionStatement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.AssignmentExpression;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
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
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.parser.ParserTestUtils;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.token.TokenType;
import co.kenrg.mega.frontend.typechecking.errors.FunctionArityError;
import co.kenrg.mega.frontend.typechecking.errors.IllegalOperatorError;
import co.kenrg.mega.frontend.typechecking.errors.MutabilityError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UnindexableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UninvokeableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownIdentifierError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownOperatorError;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import co.kenrg.mega.frontend.typechecking.types.StructType;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * This test suite tests typechecking at a pretty low level. The tests typically aren't concerned about the varying
 * different types themselves, but rather that the logic of each typechecking function is sound. It verifies that
 * expectedTypes are handled properly, that errors are thrown, and that the TypeEnvironment is modified correctly.
 * <p>
 * Compared to {@link TypeCheckerTest}, these tests are more towards the "unit test" side of the spectrum.
 */
class TypeCheckerExpectedTypeTest {
    private final Function<MegaType, ArrayType> arrayOf = ArrayType::new;
    private TypeChecker typeChecker;
    private TypeEnvironment env;

    private <T extends Expression> T parseExpression(String input, Class<T> exprClass) {
        ExpressionStatement expressionStatement = parseExpressionStatement(input);
        assertTrue(exprClass.isAssignableFrom(expressionStatement.expression.getClass()));
        return (T) expressionStatement.expression;
    }

    private <T extends Statement> T parseStatement(String input, Class<T> stmtClass) {
        Statement statement = ParserTestUtils.parseStatement(input);
        assertTrue(stmtClass.isAssignableFrom(statement.getClass()));
        return (T) statement;
    }

    @BeforeEach
    void setUp() {
        typeChecker = new TypeChecker();
        env = new TypeEnvironment();
    }

    @Nested
    class LiteralExpressionTests {

        @Test
        void noExpectedTypePassed_returnsLiteralTypePassed() {
            IntegerLiteral intLiteral = new IntegerLiteral(new Token(TokenType.INT, "4"), 4);
            MegaType type = typeChecker.typecheckLiteralExpression(PrimitiveTypes.STRING, null, intLiteral);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedTypePassed_literalTypeAssignableToExpected_returnsExpected() {
            StringLiteral stringLiteral = new StringLiteral(new Token(TokenType.STRING, "abcd"), "abcd");
            MegaType type = typeChecker.typecheckLiteralExpression(PrimitiveTypes.STRING, PrimitiveTypes.STRING, stringLiteral);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedTypePassed_literalTypeNotAssignableToExpected_returnsExpected_hasMismatchError() {
            StringLiteral stringLiteral = new StringLiteral(new Token(TokenType.STRING, "abcd"), "abcd");
            MegaType type = typeChecker.typecheckLiteralExpression(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, stringLiteral);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, null)),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }
    }

    @Nested
    class ArrayLiteralExpressionTests {

        @Test
        void noExpectedTypePassed_returnsInferredType() {
            ArrayLiteral array = parseExpression("[1, 2, 3]", ArrayLiteral.class);
            MegaType arrayType = typeChecker.typecheckArrayLiteral(array, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), arrayType);
        }

        @Test
        void noExpectedTypePassed_infersTypeByFirstElement_hasMismatchError() {
            ArrayLiteral array = parseExpression("[1, 'str', 3]", ArrayLiteral.class);
            MegaType arrayType = typeChecker.typecheckArrayLiteral(array, env, null);

            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 5))),
                typeChecker.errors
            );
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), arrayType);
        }

        @Test
        void expectedTypePassed_elementsMatchTypeArg_returnsExpectedType() {
            ArrayLiteral array = parseExpression("[1, 2, 3]", ArrayLiteral.class);
            MegaType arrayType = typeChecker.typecheckArrayLiteral(array, env, arrayOf.apply(PrimitiveTypes.INTEGER));

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), arrayType);
        }

        @Test
        void expectedTypePassed_arrayIsEmpty_returnsExpectedType() {
            ArrayLiteral array = parseExpression("[]", ArrayLiteral.class);
            MegaType arrayType = typeChecker.typecheckArrayLiteral(array, env, arrayOf.apply(PrimitiveTypes.INTEGER));

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), arrayType);
        }

        @Test
        void expectedTypePassed_elementsDontMatchTypeArg_returnsExpectedType_hasMismatchError() {
            ArrayLiteral array = parseExpression("['abc', 2, 3]", ArrayLiteral.class);
            MegaType arrayType = typeChecker.typecheckArrayLiteral(array, env, arrayOf.apply(PrimitiveTypes.INTEGER));

            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 2))),
                typeChecker.errors
            );
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), arrayType);
        }
    }

    @Nested
    class ParenthesizedExpressionTests {
        // Tests don't need to be exhaustive; typechecking of parenthesized exprs defers to typechecking the inner expr
        @Test
        void noExpectedTypePassed_returnsInferredType() {
            ParenthesizedExpression expr = parseExpression("('abc' * 4)", ParenthesizedExpression.class);
            MegaType exprType = typeChecker.typecheckParenthesizedExpression(expr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.STRING, exprType);
            assertEquals(PrimitiveTypes.STRING, expr.expr.getType());
        }
    }

    @Nested
    class ObjectLiteralTests {

        @Test
        void noExpectedTypePassed_returnsInferredType() {
            ObjectLiteral object = parseExpression("{ name: 'asdf', age: 3 }", ObjectLiteral.class);
            MegaType objectType = typeChecker.typecheckObjectLiteral(object, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(
                new ObjectType(Lists.newArrayList(
                    Pair.of("name", PrimitiveTypes.STRING),
                    Pair.of("age", PrimitiveTypes.INTEGER)
                )),
                objectType
            );
        }

        @Test
        void expectedTypePassed_expectedTypeIsStruct_matchesStructOfExpectedType_returnsExpectedType() {
            ObjectLiteral object = parseExpression("{ name: 'asdf', age: 3 }", ObjectLiteral.class);

            StructType personType = new StructType("Person", Lists.newArrayList(
                Pair.of("name", PrimitiveTypes.STRING),
                Pair.of("age", PrimitiveTypes.INTEGER)
            ));
            env.addType("Person", personType);
            MegaType objectType = typeChecker.typecheckObjectLiteral(object, env, personType);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(personType, objectType);
        }

        @Test
        void expectedTypePassed_expectedTypeIsNotStruct_returnsExpectedType_hasMismatchError() {
            ObjectLiteral object = parseExpression("{ name: 'asdf', age: 3 }", ObjectLiteral.class);

            ArrayType arrayType = arrayOf.apply(PrimitiveTypes.INTEGER);
            MegaType objectType = typeChecker.typecheckObjectLiteral(object, env, arrayType);

            assertEquals(
                Lists.newArrayList(new TypeMismatchError(
                    arrayType,
                    new ObjectType(Lists.newArrayList(
                        Pair.of("name", PrimitiveTypes.STRING),
                        Pair.of("age", PrimitiveTypes.INTEGER)
                    )),
                    Position.at(1, 1)
                )),
                typeChecker.errors
            );
            assertEquals(arrayType, objectType);
        }
    }

    @Nested
    class IdentifierTests {

        @Test
        void noExpectedTypePassed_returnsSavedBindingType() {
            Identifier identifier = parseExpression("a", Identifier.class);
            env.addBindingWithType("a", PrimitiveTypes.STRING, true);
            MegaType identType = typeChecker.typecheckIdentifier(identifier, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.STRING, identType);
        }

        @Test
        void noExpectedTypePassed_noSavedBindingType_returnsUnknownType() {
            Identifier identifier = parseExpression("a", Identifier.class);
            MegaType identType = typeChecker.typecheckIdentifier(identifier, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(TypeChecker.unknownType, identType);
        }

        @Test
        void expectedTypePassed_expectedTypeDoesntMatch_returnsActualType_hasMismatchError() {
            Identifier identifier = parseExpression("a", Identifier.class);
            env.addBindingWithType("a", PrimitiveTypes.STRING, true);
            MegaType identType = typeChecker.typecheckIdentifier(identifier, env, PrimitiveTypes.INTEGER);

            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, identType);
        }

        @Test
        void expectedTypePassed_actualTypeHasInferences_inferredTypeMatchesExpected_returnsExpected() {
            typeChecker.typecheckValStatement(parseStatement("val identity = a => a", ValStatement.class), env);

            Identifier identifier = parseExpression("identity", Identifier.class);
            FunctionType expectedType = new FunctionType(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER);
            MegaType identType = typeChecker.typecheckIdentifier(identifier, env, expectedType);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(expectedType, identType);
        }
    }

    @Nested
    class ArrowFunctionExpressionTests {

        @Test
        void noExpectedTypePassed_paramsHaveTypeAnnotations_returnsType() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a: String, b: Int) => a + b", ArrowFunctionExpression.class);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(new FunctionType(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING), arrowFuncType);
        }

        @Test
        void noExpectedTypePassed_paramsHaveTypeAnnotations_bodyIsBlockExpression_returnsType() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a: String, b: Int) => { a + b }", ArrowFunctionExpression.class);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(new FunctionType(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING), arrowFuncType);
        }

        @Test
        void noExpectedTypePassed_paramsHaveMissingTypeAnnotations_returnsTypeWithInferences() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a: Int, b) => a + b", ArrowFunctionExpression.class);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(new FunctionType(PrimitiveTypes.INTEGER, TypeChecker.notInferredType, TypeChecker.unknownType), arrowFuncType);
        }

        @Test
        void noExpectedTypePassed_paramsHaveNoTypeAnnotations_returnsTypeWithInferences() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a, b) => a + b", ArrowFunctionExpression.class);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(new FunctionType(TypeChecker.notInferredType, TypeChecker.notInferredType, TypeChecker.unknownType), arrowFuncType);
        }

        @Test
        void expectedTypePassed_paramsHaveTypeAnnotations_typeMatchesExpected_returnsType() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a: String, b: Int) => a + b", ArrowFunctionExpression.class);
            FunctionType funcType = new FunctionType(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, funcType);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(funcType, arrowFuncType);
        }

        @Test
        void expectedTypePassed_paramsHaveMissingTypeAnnotations_typeMatchesExpected_returnsResolvedType() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a: String, b) => a + b", ArrowFunctionExpression.class);
            FunctionType funcType = new FunctionType(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, funcType);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(funcType, arrowFuncType);
        }

        @Test
        void expectedTypePassed_paramsHaveMissingTypeAnnotations_typeDoesntMatchExpected_returnsResolvedType_hasMismatchError() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a: String, b) => a + b", ArrowFunctionExpression.class);
            FunctionType expectedFuncType = new FunctionType(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, expectedFuncType);

            FunctionType actualFuncType = new FunctionType(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(expectedFuncType, actualFuncType, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(actualFuncType, arrowFuncType);
        }

        @Test
        void expectedTypePassed_paramsHaveNoTypeAnnotations_returnsResolvedType() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a, b) => a + b", ArrowFunctionExpression.class);
            FunctionType funcType = new FunctionType(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, funcType);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(funcType, arrowFuncType);
        }

        @Test
        void expectedTypePassed_paramsHaveNoTypeAnnotations_errorTypecheckingBodyWithExpectedParamTypes_returnsResolvedType() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("(a, b) => a * b", ArrowFunctionExpression.class);
            FunctionType funcType = new FunctionType(PrimitiveTypes.FLOAT, PrimitiveTypes.STRING, PrimitiveTypes.STRING);
            MegaType arrowFuncType = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, funcType);

            assertEquals(
                Lists.newArrayList(
                    new IllegalOperatorError("*", PrimitiveTypes.FLOAT, PrimitiveTypes.STRING, Position.at(1, 13)),
                    new TypeMismatchError(
                        new FunctionType(PrimitiveTypes.FLOAT, PrimitiveTypes.STRING, PrimitiveTypes.STRING),
                        new FunctionType(PrimitiveTypes.FLOAT, PrimitiveTypes.STRING, TypeChecker.unknownType),
                        Position.at(1, 1)
                    )
                ),
                typeChecker.errors
            );
            assertEquals(
                new FunctionType(PrimitiveTypes.FLOAT, PrimitiveTypes.STRING, TypeChecker.unknownType),
                arrowFuncType
            );
        }
    }

    @Nested
    class PrefixExpressionTests {

        @Test
        void bangOperator_noExpectedTypePassed_returnsBoolean() {
            PrefixExpression prefixExpr = parseExpression("!true", PrefixExpression.class);
            MegaType type = typeChecker.typecheckPrefixExpression(prefixExpr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.BOOLEAN, type);
        }

        @Test
        void bangOperator_expectedTypePassed_expectBoolean_returnsBoolean() {
            PrefixExpression prefixExpr = parseExpression("!true", PrefixExpression.class);
            MegaType type = typeChecker.typecheckPrefixExpression(prefixExpr, env, PrimitiveTypes.BOOLEAN);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.BOOLEAN, type);
        }

        @Test
        void bangOperator_expectedTypePassed_expectNonBoolean_returnsBoolean_hasMismatchError() {
            PrefixExpression prefixExpr = parseExpression("!true", PrefixExpression.class);
            MegaType type = typeChecker.typecheckPrefixExpression(prefixExpr, env, arrayOf.apply(PrimitiveTypes.STRING));

            assertEquals(
                Lists.newArrayList(new TypeMismatchError(arrayOf.apply(PrimitiveTypes.STRING), PrimitiveTypes.BOOLEAN, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.BOOLEAN, type);
        }

        @Test
        void minusOperator_noExpectedTypePassed_returnsInferredType() {
            PrefixExpression prefixExpr = parseExpression("-1.5", PrefixExpression.class);
            MegaType type = typeChecker.typecheckPrefixExpression(prefixExpr, env, null);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.FLOAT, type);
        }

        @Test
        void minusOperator_expectedTypePassed_expectInteger_returnsExpectedType() {
            PrefixExpression prefixExpr = parseExpression("-4", PrefixExpression.class);
            MegaType type = typeChecker.typecheckPrefixExpression(prefixExpr, env, PrimitiveTypes.INTEGER);

            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void minusOperator_expectedTypePassed_expectWrongType_returnsExpectedType_hasMismatchError() {
            PrefixExpression prefixExpr = parseExpression("-1.4", PrefixExpression.class);
            MegaType type = typeChecker.typecheckPrefixExpression(prefixExpr, env, PrimitiveTypes.INTEGER);

            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.FLOAT, type);
        }
    }

    @Nested
    class InfixExpressionTests {

        @Test
        void noExpectedTypePassed_leftTypeIsUnknown_returnsUnknown() {
            InfixExpression infixExpr = parseExpression("a + 1", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedTypePassed_rightTypeIsUnknown_returnsUnknown() {
            InfixExpression infixExpr = parseExpression("1 + a", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedTypePassed_leftTypeIsNotInferred_returnsUnknown() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("a => a + 1", ArrowFunctionExpression.class);
            MegaType type = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            FunctionType funcType = (FunctionType) type;
            assertEquals(TypeChecker.unknownType, funcType.returnType);
        }

        @Test
        void noExpectedTypePassed_rightTypeIsNotInferred_returnsUnknown() {
            ArrowFunctionExpression arrowFuncExpr = parseExpression("a => 1 + a", ArrowFunctionExpression.class);
            MegaType type = typeChecker.typecheckArrowFunctionExpression(arrowFuncExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            FunctionType funcType = (FunctionType) type;
            assertEquals(TypeChecker.unknownType, funcType.returnType);
        }

        @Test
        @Disabled("Having unknown operators is currently unsupported at the parser level")
        void noExpectedTypePassed_unknownOperator_returnsUnknown_hasUnknownOperatorError() {
            InfixExpression infixExpr = parseExpression("1 & 4", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, null);
            assertEquals(
                Lists.newArrayList(new UnknownOperatorError("&", Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedTypePassed_incorrectTypesForOperator_operatorIsNonBoolean_returnsUnknown_hasIllegalOperatorError() {
            InfixExpression infixExpr = parseExpression("3.4 * 'abc'", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, null);
            assertEquals(
                Lists.newArrayList(new IllegalOperatorError("*", PrimitiveTypes.FLOAT, PrimitiveTypes.STRING, Position.at(1, 5))),
                typeChecker.errors
            );
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedTypePassed_incorrectTypesForOperator_operatorNonBoolean_returnsBoolean_hasIllegalOperatorError() {
            InfixExpression infixExpr = parseExpression("'abc' <= 'def'", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, null);
            assertEquals(
                Lists.newArrayList(new IllegalOperatorError("<=", PrimitiveTypes.STRING, PrimitiveTypes.STRING, Position.at(1, 7))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.BOOLEAN, type);
        }

        @Test
        void noExpectedTypePassed_returnsOperatorResultType() {
            InfixExpression infixExpr = parseExpression("1 + 5.2", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.FLOAT, type);
        }

        @Test
        void expectedTypePassed_resultTypeDoesntMatchExpected_returnsExpectedType_hasMismatchError() {
            InfixExpression infixExpr = parseExpression("1 + 5.2", InfixExpression.class);
            MegaType type = typeChecker.typecheckInfixExpression(infixExpr, env, PrimitiveTypes.INTEGER);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, Position.at(1, 3))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }
    }

    @Nested
    class IfExpressionTests {

        @Test
        void noExpectedTypePassed_conditionIsNotBoolean_returnsInferredType_hasMismatchError() {
            IfExpression ifExpr = parseExpression("if 123 { 'abc' } else { 'def' }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER, Position.at(1, 4))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void noExpectedTypePassed_noElse_returnsUnitType() {
            IfExpression ifExpr = parseExpression("if true { 'abc' }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.UNIT, type);
        }

        @Test
        void noExpectedTypePassed_elseTypeMatchesThen_returnsInferredType() {
            IfExpression ifExpr = parseExpression("if true { 'abc' } else { 'def' }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void noExpectedTypePassed_elseTypeDoesntMatchThen_returnsThenType_hasMismatchError() {
            IfExpression ifExpr = parseExpression("if true { 'abc' } else { 123 }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 24))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedTypePassed_noElse_expectedIsNotUnit_returnsExpectedType_hasMismatchError() {
            IfExpression ifExpr = parseExpression("if true { 'abc' }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, PrimitiveTypes.STRING);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.UNIT, Position.at(1, 9))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedTypePassed_elseDoesntMatchExpected_returnsExpectedType_hasMismatchError() {
            IfExpression ifExpr = parseExpression("if true { 'abc' } else { 123 }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, PrimitiveTypes.STRING);
            assertEquals(
                Lists.newArrayList(
                    new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 26))
                ),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedTypePassed_thenDoesntMatchExpected_returnsExpectedType_hasMismatchError() {
            IfExpression ifExpr = parseExpression("if true { 'abc' } else { 123 }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, PrimitiveTypes.INTEGER);
            assertEquals(
                Lists.newArrayList(
                    new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 11))
                ),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void expectedTypePassed_neitherThenNorElseMatchExpected_returnsExpectedType_has2MismatchErrors() {
            IfExpression ifExpr = parseExpression("if true { 'abc' } else { 123 }", IfExpression.class);
            MegaType type = typeChecker.typecheckIfExpression(ifExpr, env, arrayOf.apply(PrimitiveTypes.FLOAT));
            assertEquals(
                Lists.newArrayList(
                    new TypeMismatchError(arrayOf.apply(PrimitiveTypes.FLOAT), PrimitiveTypes.STRING, Position.at(1, 11)),
                    new TypeMismatchError(arrayOf.apply(PrimitiveTypes.FLOAT), PrimitiveTypes.INTEGER, Position.at(1, 26))
                ),
                typeChecker.errors
            );
            assertEquals(arrayOf.apply(PrimitiveTypes.FLOAT), type);
        }
    }

    @Nested
    class CallExpressionTests {

        @Test
        void noExpectedType_targetIsNotAFunction_returnsUnknown_hasUninvokeableTypeError() {
            CallExpression callExpr = parseExpression("[1, 2](4)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(
                Lists.newArrayList(new UninvokeableTypeError(arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedType_moreParamsThanFunctionArgs_returnsReturnType_hasFunctionArityError() {
            CallExpression callExpr = parseExpression("((a: Int) => a)(1, 2)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(
                Lists.newArrayList(new FunctionArityError(1, 2, Position.at(1, 16))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_fewerParamsThanFunctionArgs_returnsReturnType_hasFunctionArityError() {
            CallExpression callExpr = parseExpression("((a: Int, b: Int) => a + b)(1)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(
                Lists.newArrayList(new FunctionArityError(2, 1, Position.at(1, 28))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_paramTypesDontMatch_returnsReturnType_hasFunctionTypeError() {
            CallExpression callExpr = parseExpression("((a: Int, b: Int) => a + b)(1, 'a')", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 32))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_arrowFunctionPassedThatRequireInference_inferredTypeMatchesParamType_returnsReturnType() {
            ValStatement valStmt = parseStatement("val call = (fn: Int => Int, a: Int) => fn(a)", ValStatement.class);
            typeChecker.typecheckValStatement(valStmt, env);

            CallExpression callExpr = parseExpression("call(a => a, 1)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_arrowFunctionPassedThatRequireInference_inferredTypeDoesntMatchParamType_returnsReturnType_hasMismatchError() {
            ValStatement valStmt = parseStatement("val call = (fn: Int => Int, a: Int) => fn(a)", ValStatement.class);
            typeChecker.typecheckValStatement(valStmt, env);

            CallExpression callExpr = parseExpression("call(a => a + '!', 1)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(
                    new FunctionType(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER),
                    new FunctionType(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
                    Position.at(1, 6)
                )),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_identifierPassedThatRequiresInference_inferredTypeMatchesParamType_returnsReturnType() {
            typeChecker.typecheckValStatement(parseStatement("val call = (fn: Int => Int, a: Int) => fn(a)", ValStatement.class), env);
            typeChecker.typecheckValStatement(parseStatement("val identity = a => a", ValStatement.class), env);

            CallExpression callExpr = parseExpression("call(identity, 1)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_targetRequiresInference_targetTypeMatchesInference_returnsReturnType() {
            CallExpression callExpr = parseExpression("(a => a)(4)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void noExpectedType_targetRequiresInference_targetTypeDoesntMatchInference_returnsUnknown() {
            typeChecker.typecheckValStatement(parseStatement("val halve = a => a / 2", ValStatement.class), env);

            CallExpression callExpr = parseExpression("halve('asdf')", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(
                Lists.newArrayList(new IllegalOperatorError("/", PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 20))),
                typeChecker.errors
            );
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedType_returnsReturnType() {
            CallExpression callExpr = parseExpression("((a: Int, b: Int) => a + b)(1, 2)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, null);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void expectedType_expectedTypeDoesntMatchReturnType_returnsExpected_hasMismatchError() {
            CallExpression callExpr = parseExpression("((a: Int, b: Int) => a + b)(1, 2)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, PrimitiveTypes.STRING);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 28))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedType_expectedTypeMatchesReturnType_returnsExpected() {
            CallExpression callExpr = parseExpression("((a: Int, b: Int) => a + b)(1, 2)", CallExpression.class);
            MegaType type = typeChecker.typecheckCallExpression(callExpr, env, PrimitiveTypes.INTEGER);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.INTEGER, type);
        }
    }

    @Nested
    class IndexExpressionTests {

        @Test
        void noExpectedType_targetIsNotArray_returnsUnknownType_hasUnindexableTypeError() {
            IndexExpression indexExpr = parseExpression("'asdf'[0]", IndexExpression.class);
            MegaType type = typeChecker.typecheckIndexExpression(indexExpr, env, null);
            assertEquals(
                Lists.newArrayList(new UnindexableTypeError(PrimitiveTypes.STRING, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedType_targetIsIdent_targetIsNotArray_returnsUnknownType_hasUnindexableTypeError() {
            IndexExpression indexExpr = parseExpression("arr[0]", IndexExpression.class);
            env.addBindingWithType("arr", PrimitiveTypes.STRING, true);
            MegaType type = typeChecker.typecheckIndexExpression(indexExpr, env, null);
            assertEquals(
                Lists.newArrayList(new UnindexableTypeError(PrimitiveTypes.STRING, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(TypeChecker.unknownType, type);
        }

        @Test
        void noExpectedType_indexIsNotInteger_returnsArrayTypeArg_hasMismatchError() {
            IndexExpression indexExpr = parseExpression("[1, 2]['ab']", IndexExpression.class);
            MegaType type = typeChecker.typecheckIndexExpression(indexExpr, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 8))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.INTEGER, type);
        }

        @Test
        void expectedType_arrayTypeArgDoesntMatchExpected_returnsExpected_hasMismatchError() {
            IndexExpression indexExpr = parseExpression("[1, 2][1]", IndexExpression.class);
            MegaType type = typeChecker.typecheckIndexExpression(indexExpr, env, PrimitiveTypes.STRING);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 7))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.STRING, type);
        }

        @Test
        void expectedType_arrayTypeArgMatchesExpected_returnsExpected() {
            IndexExpression indexExpr = parseExpression("['abc', 'def'][1]", IndexExpression.class);
            MegaType type = typeChecker.typecheckIndexExpression(indexExpr, env, PrimitiveTypes.STRING);
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(PrimitiveTypes.STRING, type);
        }
    }

    @Nested
    class AssignmentExpressionTests {

        @Test
        void noExpectedType_assignmentTypeDoesntMatchAssigneeType_returnsUnit_hasMismatchError() {
            AssignmentExpression assignmentExpr = parseExpression("a = 4", AssignmentExpression.class);
            env.addBindingWithType("a", PrimitiveTypes.STRING, false);
            MegaType type = typeChecker.typecheckAssignmentExpression(assignmentExpr, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 5))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.UNIT, type);
        }

        @Test
        void noExpectedType_assigneeDoesntExist_returnsUnit_hasUnknownIdentifierError() {
            AssignmentExpression assignmentExpr = parseExpression("a = 4 + 4.3", AssignmentExpression.class);
            MegaType type = typeChecker.typecheckAssignmentExpression(assignmentExpr, env, null);
            assertEquals(
                Lists.newArrayList(new UnknownIdentifierError("a", Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.UNIT, type);
        }

        @Test
        void noExpectedType_assigneeIsImmutable_returnsUnit_hasMutabilityError() {
            AssignmentExpression assignmentExpr = parseExpression("a = 'abcd' + 'bcd'", AssignmentExpression.class);
            env.addBindingWithType("a", PrimitiveTypes.STRING, true);
            MegaType type = typeChecker.typecheckAssignmentExpression(assignmentExpr, env, null);
            assertEquals(
                Lists.newArrayList(new MutabilityError("a", Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.UNIT, type);
        }

        @Test
        void expectedType_expectedTypeIsNotUnit_returnsUnit_hasMismatchError() {
            AssignmentExpression assignmentExpr = parseExpression("a = 4", AssignmentExpression.class);
            MegaType type = typeChecker.typecheckAssignmentExpression(assignmentExpr, env, PrimitiveTypes.INTEGER);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.UNIT, Position.at(1, 3))),
                typeChecker.errors
            );
            assertEquals(PrimitiveTypes.UNIT, type);
        }
    }

    @Nested
    class RangeExpressionTests {

        @Test
        void noExpectedType_leftBoundIsNotInteger_returnsIntArray_hasMismatchError() {
            RangeExpression rangeExpression = parseExpression("'a'..4", RangeExpression.class);
            MegaType type = typeChecker.typecheckRangeExpression(rangeExpression, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 1))),
                typeChecker.errors
            );
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), type);
        }

        @Test
        void noExpectedType_rightBoundIsNotInteger_returnsIntArray_hasMismatchError() {
            RangeExpression rangeExpression = parseExpression("1..1.3", RangeExpression.class);
            MegaType type = typeChecker.typecheckRangeExpression(rangeExpression, env, null);
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, Position.at(1, 4))),
                typeChecker.errors
            );
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), type);
        }

        @Test
        void expectedType_expectedTypeIsNotIntArray_returnsExpectedType_hasMismatchError() {
            RangeExpression rangeExpression = parseExpression("1..3", RangeExpression.class);
            MegaType type = typeChecker.typecheckRangeExpression(rangeExpression, env, arrayOf.apply(PrimitiveTypes.FLOAT));
            assertEquals(
                Lists.newArrayList(new TypeMismatchError(arrayOf.apply(PrimitiveTypes.FLOAT), arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 2))),
                typeChecker.errors
            );
            assertEquals(arrayOf.apply(PrimitiveTypes.FLOAT), type);
        }

        @Test
        void expectedType_expectedTypeIsIntArray_returnsExpectedType() {
            RangeExpression rangeExpression = parseExpression("1..3", RangeExpression.class);
            MegaType type = typeChecker.typecheckRangeExpression(rangeExpression, env, arrayOf.apply(PrimitiveTypes.INTEGER));
            assertEquals(0, typeChecker.errors.size(), "There should be no errors");
            assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), type);
        }
    }
}