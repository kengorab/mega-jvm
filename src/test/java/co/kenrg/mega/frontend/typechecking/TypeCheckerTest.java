package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckExpression;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckExpressionAndGetResult;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckModuleAndGetResult;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckStatement;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckStatementAndGetResult;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.ast.type.BasicTypeExpression;
import co.kenrg.mega.frontend.ast.type.StructTypeExpression;
import co.kenrg.mega.frontend.ast.type.TypeExpressions;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
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
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UnindexableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UninvokeableTypeError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownExportError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownIdentifierError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownModuleError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownTypeError;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * This test suite tests typechecking at a pretty high level. The tests go from a string input down into the types that
 * the parsed AST should be. They also check errors for various different types, and that the TypeEnvironment is
 * modified to include types and bindings that get defined.
 * <p>
 * Compared to {@link TypeCheckerExpectedTypeTest}, these tests are more towards the "functional test" side of the spectrum.
 */
class TypeCheckerTest {
    private final Function<MegaType, ArrayType> arrayOf = ArrayType::new;

    @Test
    void testTypecheckModuleExports() {
        String input = "" +
            "export func abc() { 1 }\n" +
            "func def() { 1 }\n" +

            "export val a = 'asdf'\n" +
            "val b = 'asdf'\n" +

            "export var c = abc()\n" +
            "var d = def()\n" +

            "export type Person = { name: String }\n" +
            "type Person2 = { name: String, age: Int }";

        TypeCheckResult result = testTypecheckModuleAndGetResult(input);
        Module module = (Module) result.node;
        assertFalse(result.hasErrors());

        Map<String, Statement> expectedExports = ImmutableMap.of(
            "abc", new FunctionDeclarationStatement(
                Token.function(Position.at(1, 8)),
                new Identifier(
                    Token.ident("abc", Position.at(1, 13)),
                    "abc",
                    null
                ),
                Lists.newArrayList(),
                new BlockExpression(
                    Token.lbrace(Position.at(1, 19)),
                    Lists.newArrayList(
                        new ExpressionStatement(
                            Token._int("1", Position.at(1, 21)),
                            new IntegerLiteral(
                                Token._int("1", Position.at(1, 21)),
                                1,
                                PrimitiveTypes.INTEGER
                            )
                        )
                    ),
                    PrimitiveTypes.INTEGER
                ),
                true
            ),
            "a", new ValStatement(
                Token.val(Position.at(3, 8)),
                new Identifier(
                    Token.ident("a", Position.at(3, 12)),
                    "a",
                    null,
                    PrimitiveTypes.STRING
                ),
                new StringLiteral(
                    Token.string("asdf", Position.at(3, 16)),
                    "asdf",
                    PrimitiveTypes.STRING
                ),
                true
            ),
            "c", new VarStatement(
                Token.var(Position.at(5, 8)),
                new Identifier(
                    Token.ident("c", Position.at(5, 12)),
                    "c",
                    null,
                    PrimitiveTypes.INTEGER
                ),
                new CallExpression.UnnamedArgs(
                    Token.lparen(Position.at(5, 19)),
                    new Identifier(
                        Token.ident("abc", Position.at(5, 16)),
                        "abc",
                        null,
                        new FunctionType(Lists.newArrayList(), PrimitiveTypes.INTEGER, Kind.METHOD)
                    ),
                    Lists.newArrayList(),
                    PrimitiveTypes.INTEGER
                ),
                true
            ),
            "Person", new TypeDeclarationStatement(
                Token.type(Position.at(7, 8)),
                new Identifier(
                    Token.ident("Person", Position.at(7, 13)),
                    "Person",
                    null
                ),
                new StructTypeExpression(
                    Lists.newArrayList(
                        Pair.of("name", new BasicTypeExpression("String", Position.at(7, 30)))
                    ),
                    Position.at(7, 22)
                ),
                true
            )
        );
        assertEquals(expectedExports, module.namedExports);
    }

    @Test
    void testTypecheckModuleExports_errors() {
        String input = "" +
            "export func abc() { 1 }\n" +
            "export val abc = 'asdf'";

        TypeCheckResult result = testTypecheckModuleAndGetResult(input);
        assertEquals(
            Lists.newArrayList(new DuplicateExportError("abc", Position.at(2, 8))),
            result.errors
        );
    }

    @Test
    void testTypecheckModuleImports_singleImport() {
        String module1Input = "export val a = 1";
        String module2Input = "" +
            "import a from 'co.mega.test.module1'" +
            "val b = a";

        TypeCheckResult result = testTypecheckModuleAndGetResult(module2Input, moduleName -> {
            switch (moduleName) {
                case "co.mega.test.module1":
                    return module1Input;
                default:
                    fail("Unknown module: " + moduleName);
                    return null;
            }
        });

        assertEquals(
            new Binding(PrimitiveTypes.INTEGER, true),
            result.typeEnvironment.getBinding("a")
        );

        assertEquals(
            new Binding(PrimitiveTypes.INTEGER, true),
            result.typeEnvironment.getBinding("b")
        );
    }

    @Test
    void testTypecheckModuleImports_multipleImports() {
        String module1Input = "" +
            "export val a = 1\n" +
            "export func abc(i: Int = 1) { a + i }";
        String module2Input = "" +
            "import a, abc from 'co.mega.test.module1'\n" +
            "val b = abc(a)";

        TypeCheckResult result = testTypecheckModuleAndGetResult(module2Input, moduleName -> {
            switch (moduleName) {
                case "co.mega.test.module1":
                    return module1Input;
                default:
                    fail("Unknown module: " + moduleName);
                    return null;
            }
        });

        // Assert imported bindings added to module's type env
        assertEquals(
            new Binding(PrimitiveTypes.INTEGER, true),
            result.typeEnvironment.getBinding("a")
        );
        assertEquals(
            new Binding(
                new FunctionType(
                    Lists.newArrayList(
                        new Parameter(
                            new Identifier(
                                Token.ident("i", Position.at(2, 17)),
                                "i",
                                new BasicTypeExpression("Int", Position.at(2, 20)),
                                PrimitiveTypes.INTEGER
                            ),
                            new IntegerLiteral(Token._int("1", Position.at(2, 26)), 1, PrimitiveTypes.INTEGER)
                        )
                    ),
                    PrimitiveTypes.INTEGER,
                    Kind.METHOD
                ),
                true
            ),
            result.typeEnvironment.getBinding("abc")
        );

        assertEquals(
            new Binding(PrimitiveTypes.INTEGER, true),
            result.typeEnvironment.getBinding("b")
        );
    }

    @TestFactory
    List<DynamicTest> testTypecheckModuleImports_errors() {
        List<Triple<String, String, TypeCheckerError>> testCases = Lists.newArrayList(
            Triple.of(
                "export val a = 1", // co.mega.test.module1
                "import b from 'co.mega.test.module1'", // co.mega.test.module2
                new UnknownExportError("co.mega.test.module1", "b", Position.at(1, 8))
            ),
            Triple.of(
                "val a = 1", // co.mega.test.module1
                "import a from 'co.mega.test.module1'", // co.mega.test.module2
                new VisibilityError("co.mega.test.module1", "a", Position.at(1, 8))
            ),
            Triple.of(
                "val a = 1", // co.mega.test.module1
                "import a from 'co.mega.test.invalid-module'", // co.mega.test.module2
                new UnknownModuleError("co.mega.test.invalid-module", Position.at(1, 15))
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String module1 = testCase.getLeft();
                String module2 = testCase.getMiddle();
                TypeCheckerError error = testCase.getRight();

                String name = String.format("For module1 (`%s`) and module2 (`%s`), there should be an error", module1, module2);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckModuleAndGetResult(module2, moduleName -> {
                        switch (moduleName) {
                            case "co.mega.test.module1":
                                return module1;
                            default:
                                return null;
                        }
                    });

                    assertEquals(Lists.newArrayList(error), result.errors);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckIntegerLiteral() {
        List<String> testCases = Lists.newArrayList(
            "5",
            "10."
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Int", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.INTEGER, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckFloatLiteral() {
        List<String> testCases = Lists.newArrayList(
            "5.0",
            "1.03",
            "0.123"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Float", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.FLOAT, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckBooleanLiteral() {
        List<String> testCases = Lists.newArrayList(
            "true", "false"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Bool", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.BOOLEAN, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckStringLiteral() {
        List<String> testCases = Lists.newArrayList(
            "\"hello\"",
            "\"hello \\u1215!\"",
            "\"Meet me at\n the \\uCAFE?\""
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to String", testCase);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(testCase);
                    assertEquals(PrimitiveTypes.STRING, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckBindingDeclarationStatements_valAndVar() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("val s = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("val s: String = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("val i = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("val i: Int = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("val f = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("val f: Float = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("val b = true", "b", PrimitiveTypes.BOOLEAN),
            Triple.of("val b: Bool = false", "b", PrimitiveTypes.BOOLEAN),
            Triple.of("val arr: Array[Int] = [1, 2, 3]", "arr", arrayOf.apply(PrimitiveTypes.INTEGER)),
            Triple.of("val sum: (Int, Int) => Int = (a: Int, b: Int) => a + b", "sum", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 31)),
                            "a",
                            new BasicTypeExpression("Int", Position.at(1, 34)),
                            PrimitiveTypes.INTEGER
                        )
                    ),
                    new Parameter(
                        new Identifier(
                            Token.ident("b", Position.at(1, 39)),
                            "b",
                            new BasicTypeExpression("Int", Position.at(1, 42)),
                            PrimitiveTypes.INTEGER
                        )
                    )
                ),
                PrimitiveTypes.INTEGER,
                Kind.ARROW_FN
            )),

            Triple.of("var s = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("var s: String = \"asdf\"", "s", PrimitiveTypes.STRING),
            Triple.of("var i = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("var i: Int = 123", "i", PrimitiveTypes.INTEGER),
            Triple.of("var f = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("var f: Float = 12.34", "f", PrimitiveTypes.FLOAT),
            Triple.of("var b = true", "b", PrimitiveTypes.BOOLEAN),
            Triple.of("var b: Bool = false", "b", PrimitiveTypes.BOOLEAN),
            Triple.of("var arr: Array[Int] = [1, 2, 3]", "arr", arrayOf.apply(PrimitiveTypes.INTEGER)),
            Triple.of("var sum: (Int, Int) => Int = (a: Int, b: Int) => a + b", "sum", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 31)),
                            "a",
                            new BasicTypeExpression("Int", Position.at(1, 34)),
                            PrimitiveTypes.INTEGER
                        )
                    ),
                    new Parameter(
                        new Identifier(
                            Token.ident("b", Position.at(1, 39)),
                            "b",
                            new BasicTypeExpression("Int", Position.at(1, 42)),
                            PrimitiveTypes.INTEGER
                        )
                    )
                ),
                PrimitiveTypes.INTEGER,
                Kind.ARROW_FN
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should typecheck to %s", testCase.getLeft(), testCase.getRight());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    MegaType result = testTypecheckStatement(testCase.getLeft(), env);
                    assertEquals(PrimitiveTypes.UNIT, result);

                    Binding binding = env.getBinding(testCase.getMiddle());
                    assertNotNull(binding);
                    MegaType bindingType = binding.type;
                    assertEquals(testCase.getRight(), bindingType);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckBindingDeclarationStatement_typeIsObjectType() {
        ObjectType personObjType = new ObjectType( Lists.newArrayList(
            Pair.of("name", PrimitiveTypes.STRING),
            Pair.of("age", PrimitiveTypes.INTEGER)
        ));
        ObjectType teamObjType = new ObjectType( Lists.newArrayList(
            Pair.of("manager", personObjType),
            Pair.of("members", arrayOf.apply(personObjType))
        ));

        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("val x: { name: String, age: Int } = { name: 'Ken', age: 25 }", personObjType),
            Pair.of("val x: Array[{ name: String, age: Int }] = [{ name: 'Ken', age: 25 }, { name: 'Meg', age: 24 }]", arrayOf.apply(personObjType)),
            Pair.of("val x: { manager: { name: String, age: Int }, members: Array[{ name: String, age: Int }] } = { manager: { name: 'Ken', age: 25 }, members: [{ name: 'Scott', age: 27 }] }", teamObjType),
            Pair.of("val x: { manager: { name: String, age: Int }, members: Array[{ name: String, age: Int }] } = { manager: { name: 'Ken', age: 25 }, members: []}", teamObjType),

            Pair.of("var x: { name: String, age: Int } = { name: 'Ken', age: 25 }", personObjType),
            Pair.of("var x: Array[{ name: String, age: Int }] = [{ name: 'Ken', age: 25 }, { name: 'Meg', age: 24 }]", arrayOf.apply(personObjType)),
            Pair.of("var x: { manager: { name: String, age: Int }, members: Array[{ name: String, age: Int }] } = { manager: { name: 'Ken', age: 25 }, members: [{ name: 'Scott', age: 27 }] }", teamObjType),
            Pair.of("var x: { manager: { name: String, age: Int }, members: Array[{ name: String, age: Int }] } = { manager: { name: 'Ken', age: 25 }, members: [] }", teamObjType)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should typecheck to %s", testCase.getLeft(), testCase.getRight().signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    MegaType result = testTypecheckStatement(testCase.getLeft(), env);
                    assertEquals(PrimitiveTypes.UNIT, result);

                    Binding binding = env.getBinding("x");
                    assertNotNull(binding);
                    MegaType bindingType = binding.type;
                    assertEquals(testCase.getRight(), bindingType);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckBindingDeclarationStatement_typeIsStructType_noTypeAnnotation_bindingHasCorrectlyGuessedType() {
        ObjectType personObjType = new ObjectType( Lists.newArrayList(
            Pair.of("name", PrimitiveTypes.STRING),
            Pair.of("age", PrimitiveTypes.INTEGER)
        ));
        ObjectType teamObjType = new ObjectType( Lists.newArrayList(
            Pair.of("manager", personObjType),
            Pair.of("members", arrayOf.apply(personObjType))
        ));
        ObjectType teamObjIncompleteType = new ObjectType( Lists.newArrayList(
            Pair.of("manager", personObjType),
            Pair.of("members", arrayOf.apply(PrimitiveTypes.NOTHING))
        ));

        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("val x = { name: 'Ken', age: 25 }", personObjType),
            Pair.of("val x = { manager: { name: 'Ken', age: 26 }, members: [{ name: 'Brian', age: 25 }] }", teamObjType),
            Pair.of("val x = { manager: { name: 'Ken', age: 26 }, members: [] }", teamObjIncompleteType)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should typecheck to %s", testCase.getLeft(), testCase.getRight().signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    MegaType result = testTypecheckStatement(testCase.getLeft(), env);
                    assertEquals(PrimitiveTypes.UNIT, result);

                    Binding binding = env.getBinding("x");
                    assertNotNull(binding);
                    MegaType bindingType = binding.type;
                    assertEquals(testCase.getRight(), bindingType);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckBindingDeclarationStatement_typeIsObjectType_bindingIsMissingFields_errors() {
        ObjectType personObjType = new ObjectType(Lists.newArrayList(
            Pair.of("name", PrimitiveTypes.STRING),
            Pair.of("age", PrimitiveTypes.INTEGER)
        ));
        ObjectType teamObjType = new ObjectType(Lists.newArrayList(
            Pair.of("manager", personObjType),
            Pair.of("members", arrayOf.apply(personObjType))
        ));

        List<Triple<String, MegaType, String>> testCases = Lists.newArrayList(
//            Triple.of(
//                "val x: { name: String, age: Int } = { name: 'Ken' }",
//                personObjType,
//                "(1, 37): Expected { name: String, age: Int }, got { name: String }; missing properties { age: Int }"
//            ),
            Triple.of(
                "val x: { manager: { name: String, age: Int }, members: Array[{ name: String, age: Int }] } = { manager: { name: 'Ken' }, members: [] }",
                teamObjType,
                "(1, 105): Expected { name: String, age: Int }, got { name: String }; missing properties { age: Int }"
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should typecheck to %s", testCase.getLeft(), testCase.getMiddle().signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    TypeCheckResult result = testTypecheckStatementAndGetResult(testCase.getLeft(), env);
                    assertEquals(PrimitiveTypes.UNIT, result.type);

                    Binding binding = env.getBinding("x");
                    assertNotNull(binding);
                    MegaType bindingType = binding.type;
                    assertEquals(testCase.getMiddle(), bindingType);

                    assertTrue(result.hasErrors());
                    assertEquals(1, result.errors.size());
                    assertEquals(testCase.getRight(), result.errors.get(0).toString());
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckBindingDeclarationStatements_errors() {
        List<Triple<String, Pair<MegaType, MegaType>, Position>> testCases = Lists.newArrayList(
            Triple.of("val s: String = 123", Pair.of(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER), Position.at(1, 17)),
            Triple.of("val i: Int = \"asdf\"", Pair.of(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING), Position.at(1, 14)),
            Triple.of("val f: Float = 123", Pair.of(PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER), Position.at(1, 16)),
            Triple.of("val b: Bool = 123", Pair.of(PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER), Position.at(1, 15)),
            Triple.of("val b: Bool = (123)", Pair.of(PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER), Position.at(1, 16)),
            Triple.of("val arr: Array[Int] = ['abc']", Pair.of(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING), Position.at(1, 24)),

            Triple.of("var s: String = 123", Pair.of(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER), Position.at(1, 17)),
            Triple.of("var i: Int = \"asdf\"", Pair.of(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING), Position.at(1, 14)),
            Triple.of("var f: Float = 123", Pair.of(PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER), Position.at(1, 16)),
            Triple.of("var b: Bool = 123", Pair.of(PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER), Position.at(1, 15)),
            Triple.of("var b: Bool = (123)", Pair.of(PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER), Position.at(1, 16)),
            Triple.of("var arr: Array[Int] = ['abc']", Pair.of(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING), Position.at(1, 24))
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckStatementAndGetResult(testCase.getLeft());
                    assertEquals(PrimitiveTypes.UNIT, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.getMiddle().getLeft(), testCase.getMiddle().getRight(), testCase.getRight()),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckFunctionDeclarationStatement() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("func addOne(a: Int): Int { a + 1 }", "addOne", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 13)),
                            "a",
                            new BasicTypeExpression("Int", Position.at(1, 16)),
                            PrimitiveTypes.INTEGER
                        )
                    )
                ),
                PrimitiveTypes.INTEGER,
                Kind.METHOD
            )),
            Triple.of("func addOne(a: Int) { a + 1 }", "addOne", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 13)),
                            "a",
                            new BasicTypeExpression("Int", Position.at(1, 16)),
                            PrimitiveTypes.INTEGER
                        )
                    )
                ),
                PrimitiveTypes.INTEGER,
                Kind.METHOD
            )),
            Triple.of("" +
                    "type Person = { name: String }\n" +
                    "func addOne(p: Person, a: Int) { a + 1 }",
                "addOne",
                new FunctionType(
                    Lists.newArrayList(
                        new Parameter(
                            new Identifier(
                                Token.ident("p", Position.at(2, 13)),
                                "p",
                                new BasicTypeExpression("Person", Position.at(2, 16)),
                                new StructType("Person", Lists.newArrayList(
                                    Pair.of("name", PrimitiveTypes.STRING)
                                ))
                            )
                        ),
                        new Parameter(
                            new Identifier(
                                Token.ident("a", Position.at(2, 24)),
                                "a",
                                new BasicTypeExpression("Int", Position.at(2, 27)),
                                PrimitiveTypes.INTEGER
                            )
                        )
                    ),
                    PrimitiveTypes.INTEGER,
                    Kind.METHOD
                )
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String funcName = testCase.getMiddle();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    testTypecheckModuleAndGetResult(input, env);

                    Binding binding = env.getBinding(funcName);
                    assertNotNull(binding);
                    MegaType funcType = binding.type;
                    assertEquals(type, funcType);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckFunctionDeclaration_errors() {
        List<Pair<String, TypeCheckerError>> testCases = Lists.newArrayList(
            Pair.of(
                "func doSomething(a: Int): Int { a + '!' }",
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 33))
            ),
            Pair.of(
                "func abc(a: Int = 'asdf') { a + 1 }",
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 19))
            ),
            Pair.of(
                "func abc(a = true) { a + 1 }",
                new MissingParameterTypeAnnotationError("a", Position.at(1, 10))
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                TypeCheckerError error = testCase.getRight();

                String name = String.format("'%s' should have typechecking error: %s", input, error.message());
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckStatementAndGetResult(testCase.getLeft());

                    assertTrue(result.hasErrors());
                    assertEquals(Lists.newArrayList(error), result.errors);
                });
            })
            .collect(toList());
    }

    @Test
    void testTypecheckForLoopStatement() {
        String input = "for x in arr { val a: Int = x }";

        TypeEnvironment env = new TypeEnvironment();
        env.addBindingWithType("arr", arrayOf.apply(PrimitiveTypes.INTEGER), true);

        TypeCheckResult result = testTypecheckStatementAndGetResult(input, env);
        assertEquals(PrimitiveTypes.UNIT, result.type);
        assertTrue(result.errors.isEmpty(), "There should be no typechecking errors");
    }

    @TestFactory
    List<DynamicTest> testTypecheckForLoopStatement_errors() {
        List<Triple<String, MegaType, MegaType>> testCases = Lists.newArrayList(
            Triple.of("for x in 123 { }", arrayOf.apply(PrimitiveTypes.ANY), PrimitiveTypes.INTEGER),
            Triple.of("for x in \"asdf\" { }", arrayOf.apply(PrimitiveTypes.ANY), PrimitiveTypes.STRING),
            Triple.of("for x in [1, 2, 3] { val a: Float = x }", PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER)
        );
        Map<String, Position> positions = ImmutableMap.of(
            "for x in 123 { }", Position.at(1, 10),
            "for x in \"asdf\" { }", Position.at(1, 10),
            "for x in [1, 2, 3] { val a: Float = x }", Position.at(1, 37)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckStatementAndGetResult(testCase.getLeft());
                    assertEquals(PrimitiveTypes.UNIT, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.getMiddle(), testCase.getRight(), positions.get(testCase.getLeft())),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckTypeDeclarationStatement_typeAliases() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("type Id = Int", "Id", PrimitiveTypes.INTEGER),
            Triple.of("type Name = String", "Name", PrimitiveTypes.STRING),
            Triple.of("type Names = Array[String]", "Names", arrayOf.apply(PrimitiveTypes.STRING)),
            Triple.of("type Matrix = Array[Array[Int]]", "Matrix", arrayOf.apply(arrayOf.apply(PrimitiveTypes.INTEGER))),
            Triple.of("type UnaryOp = Int => Int", "UnaryOp", FunctionType.ofSignature(Lists.newArrayList(PrimitiveTypes.INTEGER), PrimitiveTypes.INTEGER)),
            Triple.of("type UnaryOp = (Int) => Int", "UnaryOp", FunctionType.ofSignature(Lists.newArrayList(PrimitiveTypes.INTEGER), PrimitiveTypes.INTEGER)),
            Triple.of("type BinOp = (Int, Int) => Int", "BinOp", FunctionType.ofSignature(Lists.newArrayList(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER), PrimitiveTypes.INTEGER)),
            Triple.of("type Person = { name: String, age: Int }", "Person", new StructType("Person", Lists.newArrayList(
                Pair.of("name", PrimitiveTypes.STRING),
                Pair.of("age", PrimitiveTypes.INTEGER)
            )))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String typeName = testCase.getMiddle();
                MegaType expectedType = testCase.getRight();

                String name = String.format("'%s' should typecheck to Unit, and add type named %s to env", input, typeName);
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    MegaType result = testTypecheckStatement(input, env);
                    assertEquals(PrimitiveTypes.UNIT, result);

                    MegaType type = env.getTypeByName(typeName);
                    assertEquals(expectedType, type);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckTypeDeclarationStatement_structType_constructorFunctionDeclared() {
        List<Triple<String, String, List<Pair<String, MegaType>>>> testCases = Lists.newArrayList(
            Triple.of("type Person = { name: String, age: Int }", "Person", Lists.newArrayList(
                Pair.of("name", PrimitiveTypes.STRING),
                Pair.of("age", PrimitiveTypes.INTEGER)
            )),
            Triple.of("type Team = { name: String, members: Array[String] }", "Team", Lists.newArrayList(
                Pair.of("name", PrimitiveTypes.STRING),
                Pair.of("members", arrayOf.apply(PrimitiveTypes.STRING))
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                String typeName = testCase.getMiddle();
                List<Pair<String, MegaType>> props = testCase.getRight();

                String name = String.format("'%s' should typecheck to Unit, and add type named %s to env, with constructor function", input, typeName);
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    MegaType result = testTypecheckStatement(input, env);
                    assertEquals(PrimitiveTypes.UNIT, result);

                    MegaType type = env.getTypeByName(typeName);
                    StructType expectedType = new StructType(typeName, props);
                    assertEquals(expectedType, type);

                    Binding binding = env.getBinding(typeName);
                    Binding expected = new Binding(
                        FunctionType.constructor(
                            props.stream()
                                .map(prop -> new Identifier(
                                    Token.ident(prop.getLeft(), null),
                                    prop.getLeft(),
                                    TypeExpressions.fromType(prop.getRight()),
                                    prop.getRight()
                                ))
                                .collect(toList()),
                            type
                        ),
                        true
                    );
                    assertEquals(expected, binding);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckTypeDeclarationStatement_errors() {
        class TestCase {
            public final String input;
            private final TypeCheckerError typeError;
            private final MegaType savedType;
            private final Map<String, MegaType> environment;

            private TestCase(String input, TypeCheckerError typeError, MegaType savedType, Map<String, MegaType> environment) {
                this.input = input;
                this.typeError = typeError;
                this.savedType = savedType;
                this.environment = environment;
            }
        }
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "type MyType = Identifier",
                new UnknownTypeError("Identifier", Position.at(1, 15)),
                TypeChecker.unknownType,
                ImmutableMap.of()
            ),
            new TestCase(
                "type MyType = Array[Identifier]",
                new UnknownTypeError("Identifier", Position.at(1, 21)),
                arrayOf.apply(TypeChecker.unknownType),
                ImmutableMap.of()
            ),
            new TestCase(
                "type MyType = Identifier => Int",
                new UnknownTypeError("Identifier", Position.at(1, 15)),
                FunctionType.ofSignature(Lists.newArrayList(TypeChecker.unknownType), PrimitiveTypes.INTEGER),
                ImmutableMap.of()
            ),
            new TestCase(
                "type MyType = Int => Identifier",
                new UnknownTypeError("Identifier", Position.at(1, 22)),
                FunctionType.ofSignature(Lists.newArrayList(PrimitiveTypes.INTEGER), TypeChecker.unknownType),
                ImmutableMap.of()
            ),
            new TestCase(
                "type MyType = { name: String, someField: BogusType }",
                new UnknownTypeError("BogusType", Position.at(1, 42)),
                new StructType("MyType", Lists.newArrayList(
                    Pair.of("name", PrimitiveTypes.STRING),
                    Pair.of("someField", TypeChecker.unknownType)
                )),
                ImmutableMap.of()
            ),

            new TestCase(
                "type MyType = String",
                new DuplicateTypeError("MyType", Position.at(1, 1)),
                PrimitiveTypes.INTEGER,
                ImmutableMap.of("MyType", PrimitiveTypes.INTEGER)
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, but should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeEnvironment typeEnv = new TypeEnvironment();
                    testCase.environment.forEach(typeEnv::addType);
                    TypeCheckResult result = testTypecheckStatementAndGetResult(testCase.input, typeEnv);
                    assertEquals(PrimitiveTypes.UNIT, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(testCase.typeError, result.errors.get(0));

                    assertEquals(testCase.savedType, typeEnv.getTypeByName("MyType"));
                });
            })
            .collect(toList());
    }

    @Test
    void testTypecheckEmptyArray_arrayOfNothing() {
        ParametrizedMegaType type = (ParametrizedMegaType) testTypecheckExpression("[]");
        assertEquals("Array[Nothing]", type.signature());
        assertEquals(Lists.newArrayList(PrimitiveTypes.NOTHING), type.typeArgs());
    }

    @TestFactory
    List<DynamicTest> testTypecheckArrayWithTypeMismatches_errors() {
        class TestCase {
            public final String input;
            private final MegaType arrayType;
            private final MegaType elementType;
            private final MegaType erroneousType;
            private final Position errorPos;

            private TestCase(String input, MegaType arrayType, MegaType elementType, MegaType erroneousType, Position errorPos) {
                this.input = input;
                this.arrayType = arrayType;
                this.elementType = elementType;
                this.erroneousType = erroneousType;
                this.errorPos = errorPos;
            }
        }
        ArrayType intArray = arrayOf.apply(PrimitiveTypes.INTEGER);
        ArrayType strArray = arrayOf.apply(PrimitiveTypes.STRING);

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("[1, \"a\"]", intArray, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 5)),
            new TestCase("[1, 2, \"a\"]", intArray, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 8)),
            new TestCase("[1, \"a\", \"b\"]", intArray, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 10)),
            new TestCase("[\"a\", \"b\", 3]", strArray, PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 12)),

            // Test typechecking of arrays of arrays of disparate types
            new TestCase("[[\"a\", \"b\"], 3]", arrayOf.apply(strArray), arrayOf.apply(PrimitiveTypes.STRING), PrimitiveTypes.INTEGER, Position.at(1, 14)),
            new TestCase("[[\"a\", \"b\"], [\"c\", 4]]", arrayOf.apply(strArray), PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 20)),
            new TestCase("[[\"a\", \"b\"], [3, 4]]", arrayOf.apply(strArray), arrayOf.apply(PrimitiveTypes.STRING), arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 14))
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.input);
                    assertEquals(testCase.arrayType, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.elementType, testCase.erroneousType, testCase.errorPos),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckArray_differentTypes() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("[1, 2, 3]", PrimitiveTypes.INTEGER),
            Pair.of("[1.2, 2.2, 3.2]", PrimitiveTypes.FLOAT),
            Pair.of("[true, false, true]", PrimitiveTypes.BOOLEAN),
            Pair.of("[\"asdf\", \"qwer\", \"zxcv\"]", PrimitiveTypes.STRING),

            Pair.of("[[1, 2], [3]]", arrayOf.apply(PrimitiveTypes.INTEGER)),
            Pair.of("[[1.2, 2.2], [3.2]]", arrayOf.apply(PrimitiveTypes.FLOAT)),
            Pair.of("[[true, false], [true]]", arrayOf.apply(PrimitiveTypes.BOOLEAN)),
            Pair.of("[[\"asdf\", \"qwer\"], [\"zxcv\"]]", arrayOf.apply(PrimitiveTypes.STRING))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(arrayOf.apply(type), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckParenthesizedExpression() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("(1)", PrimitiveTypes.INTEGER),
            Pair.of("(1 + 1)", PrimitiveTypes.INTEGER),
            Pair.of("('abc' * 3)", PrimitiveTypes.STRING),
            Pair.of("((i: Int) => i + 1)", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("i", Position.at(1, 3)),
                            "i",
                            new BasicTypeExpression("Int", Position.at(1, 6)),
                            PrimitiveTypes.INTEGER
                        )
                    )
                ),
                PrimitiveTypes.INTEGER,
                Kind.ARROW_FN
            )),
            Pair.of("([true, false])", arrayOf.apply(PrimitiveTypes.BOOLEAN))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck inner expr and set the appropriate types", input);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckObjectLiteral() {
        List<Pair<String, List<Pair<String, MegaType>>>> testCases = Lists.newArrayList(
            Pair.of("{ }", Lists.newArrayList()),

            Pair.of("{ a: 1 }", Lists.newArrayList(Pair.of("a", PrimitiveTypes.INTEGER))),
            Pair.of("{ a: 1.2 }", Lists.newArrayList(Pair.of("a", PrimitiveTypes.FLOAT))),
            Pair.of("{ a: true }", Lists.newArrayList(Pair.of("a", PrimitiveTypes.BOOLEAN))),
            Pair.of("{ a: \"a\" }", Lists.newArrayList(Pair.of("a", PrimitiveTypes.STRING))),

            Pair.of("{ a: [1] }", Lists.newArrayList(Pair.of("a", arrayOf.apply(PrimitiveTypes.INTEGER)))),
            Pair.of("{ a: [1.2] }", Lists.newArrayList(Pair.of("a", arrayOf.apply(PrimitiveTypes.FLOAT)))),
            Pair.of("{ a: [true] }", Lists.newArrayList(Pair.of("a", arrayOf.apply(PrimitiveTypes.BOOLEAN)))),
            Pair.of("{ a: [\"a\"] }", Lists.newArrayList(Pair.of("a", arrayOf.apply(PrimitiveTypes.STRING)))),

            Pair.of("{ a: 1, b: 2 }", Lists.newArrayList(
                Pair.of("a", PrimitiveTypes.INTEGER),
                Pair.of("b", PrimitiveTypes.INTEGER)
            )),
            Pair.of("{ a: 1, b: 2.2 }", Lists.newArrayList(
                Pair.of("a", PrimitiveTypes.INTEGER),
                Pair.of("b", PrimitiveTypes.FLOAT)
            )),
            Pair.of("{ a: \"asdf\", b: 2.2 }", Lists.newArrayList(
                Pair.of("a", PrimitiveTypes.STRING),
                Pair.of("b", PrimitiveTypes.FLOAT)
            )),
            Pair.of("{ a: true, b: [2.2] }", Lists.newArrayList(
                Pair.of("a", PrimitiveTypes.BOOLEAN),
                Pair.of("b", arrayOf.apply(PrimitiveTypes.FLOAT))
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                List<Pair<String, MegaType>> objProps = testCase.getRight();

                String name = String.format("'%s' should typecheck to an Object with appropriate props", input);
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(new ObjectType(objProps), result);
                });
            })
            .collect(toList());
    }

    @Test
    void testTypecheckObjectLiteral_nestedObjects() {
        String input = "" +
            "{\n" +
            "  a: { a1: \"asdf\", a2: false },\n" +
            "  b: [1, 2, 3, 4]\n" +
            "}";
        MegaType type = testTypecheckExpression(input);
        MegaType expected = new ObjectType(Lists.newArrayList(
            Pair.of("a", new ObjectType(Lists.newArrayList(
                Pair.of("a1", PrimitiveTypes.STRING),
                Pair.of("a2", PrimitiveTypes.BOOLEAN)
            ))),
            Pair.of("b", arrayOf.apply(PrimitiveTypes.INTEGER))
        ));
        assertEquals(expected, type);
    }

    @TestFactory
    List<DynamicTest> testTypecheckPrefixOperator() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("!1", PrimitiveTypes.BOOLEAN),
            Pair.of("!1.3", PrimitiveTypes.BOOLEAN),
            Pair.of("!true", PrimitiveTypes.BOOLEAN),
            Pair.of("!\"asdf\"", PrimitiveTypes.BOOLEAN),
            Pair.of("![1, 2, 3]", PrimitiveTypes.BOOLEAN),
            Pair.of("!{ a: 1 }", PrimitiveTypes.BOOLEAN),
            Pair.of("!!\"asdf\"", PrimitiveTypes.BOOLEAN),

            Pair.of("-1", PrimitiveTypes.INTEGER),
            Pair.of("-1.3", PrimitiveTypes.FLOAT)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckPrefixOperator_dash_errors() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("-\"asdf\"", PrimitiveTypes.STRING),
            Pair.of("-true", PrimitiveTypes.BOOLEAN),
            Pair.of("-[1, 2, 3]", arrayOf.apply(PrimitiveTypes.INTEGER)),
            Pair.of("-{ a: 1 }", new ObjectType(Lists.newArrayList(
                Pair.of("a", PrimitiveTypes.INTEGER))
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should fail to typecheck", input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(input);
                    assertEquals(TypeChecker.unknownType, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(PrimitiveTypes.NUMBER, type, Position.at(1, 1)),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckInfixOperator() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            // Boolean and/or
            Pair.of("true && false", PrimitiveTypes.BOOLEAN),
            Pair.of("true || false", PrimitiveTypes.BOOLEAN),

            // Inequalities
            Pair.of("1 < 2", PrimitiveTypes.BOOLEAN),
            Pair.of("1.2 > 3", PrimitiveTypes.BOOLEAN),
            Pair.of("1 <= 2", PrimitiveTypes.BOOLEAN),
            Pair.of("1.2 >= 3", PrimitiveTypes.BOOLEAN),

            // Equalities
            Pair.of("1 == 1", PrimitiveTypes.BOOLEAN),
            Pair.of("1.3 == 1", PrimitiveTypes.BOOLEAN),
            Pair.of("'asdf' == 'qwer'", PrimitiveTypes.BOOLEAN),
            Pair.of("[1, 2, 3] == [2, 3, 4]", PrimitiveTypes.BOOLEAN),
            Pair.of("{ a: 1, b: 2 } == { b: 1, c: 2 }", PrimitiveTypes.BOOLEAN),
            Pair.of("1 != 1", PrimitiveTypes.BOOLEAN),
            Pair.of("1.3 != 1", PrimitiveTypes.BOOLEAN),
            Pair.of("'asdf' != 'qwer'", PrimitiveTypes.BOOLEAN),
            Pair.of("[1, 2, 3] != [2, 3, 4]", PrimitiveTypes.BOOLEAN),
            Pair.of("{ a: 1, b: 2 } != { b: 1, c: 2 }", PrimitiveTypes.BOOLEAN),

            // Mathematical operations
            Pair.of("1 + 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 + 3", PrimitiveTypes.FLOAT),
            Pair.of("1 + 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 + 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1 - 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 - 3", PrimitiveTypes.FLOAT),
            Pair.of("1 - 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 - 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1 * 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 * 3", PrimitiveTypes.FLOAT),
            Pair.of("1 * 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 * 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1 / 3", PrimitiveTypes.INTEGER),
            Pair.of("1.4 / 3", PrimitiveTypes.FLOAT),
            Pair.of("1 / 3.2", PrimitiveTypes.FLOAT),
            Pair.of("1.3 / 3.2", PrimitiveTypes.FLOAT),

            // String concatenation
            Pair.of("'asdf' + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("3 + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("3.3 + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("[3, 4] + 'qwer'", PrimitiveTypes.STRING),
            Pair.of("'asdf' + 3", PrimitiveTypes.STRING),
            Pair.of("'asdf' + 3.3", PrimitiveTypes.STRING),
            Pair.of("'asdf' + [3, 4]", PrimitiveTypes.STRING),

            // String repetition
            Pair.of("'asdf' * 3", PrimitiveTypes.STRING),
            Pair.of("3 * 'asdf'", PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckInfixOperator_errors() {
        class TestCase {
            public final String input;
            private final TypeCheckerError error;
            private final MegaType overallType;

            private TestCase(String input, TypeCheckerError error, MegaType overallType) {
                this.input = input;
                this.error = error;
                this.overallType = overallType;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            // Boolean and/or
            new TestCase(
                "'asdf' && true",
                new IllegalOperatorError("&&", PrimitiveTypes.STRING, PrimitiveTypes.BOOLEAN, Position.at(1, 8)),
                PrimitiveTypes.BOOLEAN
            ),
            new TestCase(
                "false || 123",
                new IllegalOperatorError("||", PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER, Position.at(1, 7)),
                PrimitiveTypes.BOOLEAN
            ),

            // Inequalities
            new TestCase(
                "\"a\" < 3",
                new IllegalOperatorError("<", PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 5)),
                PrimitiveTypes.BOOLEAN
            ),
            new TestCase(
                "3 > \"a\"",
                new IllegalOperatorError(">", PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 3)),
                PrimitiveTypes.BOOLEAN
            ),
            new TestCase(
                "[3, 4] <= 3",
                new IllegalOperatorError("<=", arrayOf.apply(PrimitiveTypes.INTEGER), PrimitiveTypes.INTEGER, Position.at(1, 8)),
                PrimitiveTypes.BOOLEAN
            ),
            new TestCase(
                "3 >= [3, 4]",
                new IllegalOperatorError(">=", PrimitiveTypes.INTEGER, arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 3)),
                PrimitiveTypes.BOOLEAN
            ),

            new TestCase(
                "'asdf' * 1.3",
                new IllegalOperatorError("*", PrimitiveTypes.STRING, PrimitiveTypes.FLOAT, Position.at(1, 8)),
                TypeChecker.unknownType
            ),
            new TestCase(
                "'asdf' * [1.3]",
                new IllegalOperatorError("*", PrimitiveTypes.STRING, arrayOf.apply(PrimitiveTypes.FLOAT), Position.at(1, 8)),
                TypeChecker.unknownType
            ),
            new TestCase(
                "'asdf' * [1, 2]",
                new IllegalOperatorError("*", PrimitiveTypes.STRING, arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 8)),
                TypeChecker.unknownType
            ),
            new TestCase(
                "'asdf' * 'asdf'",
                new IllegalOperatorError("*", PrimitiveTypes.STRING, PrimitiveTypes.STRING, Position.at(1, 8)),
                TypeChecker.unknownType
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.input);
                    assertEquals(testCase.overallType, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(testCase.error, result.errors.get(0));
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckIfExpression() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            // When given no else-block, if-expr is typed to Unit
            Pair.of("if true { 1 + 2 }", PrimitiveTypes.UNIT),
            Pair.of("if true { \"asdf\" + 2 }", PrimitiveTypes.UNIT),
            Pair.of("if true { val a = 1; 1 + 2 }", PrimitiveTypes.UNIT),

            Pair.of("if true { 1 } else { 2 }", PrimitiveTypes.INTEGER),
            Pair.of("if true { 1.2 } else { 2.3 }", PrimitiveTypes.FLOAT),
            Pair.of("if true { \"asdf\" } else { \"qwer\" }", PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckIfExpression_errors() {
        class TestCase {
            public final String input;
            private final MegaType expected;
            private final MegaType actual;
            private final MegaType overallType;
            private final Position errorPos;

            private TestCase(String input, MegaType expected, MegaType actual, MegaType overallType, Position errorPos) {
                this.input = input;
                this.expected = expected;
                this.actual = actual;
                this.overallType = overallType;
                this.errorPos = errorPos;
            }
        }
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("if 1 + 2 { 1 } else { 2 }", PrimitiveTypes.BOOLEAN, PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER, Position.at(1, 6)),
            new TestCase("if true { 1.2 } else { 2 }", PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, Position.at(1, 22)),
            new TestCase("if true { 1 } else { 2.1 }", PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, Position.at(1, 20))
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.input);
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.input);
                    // Even though typechecking fails, there should still be some kind of overall type returned, even if it's <unknown>.
                    assertEquals(testCase.overallType, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeMismatchError(testCase.expected, testCase.actual, testCase.errorPos),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckIdentifier() {
        List<Triple<String, Map<String, MegaType>, MegaType>> testCases = Lists.newArrayList(
            Triple.of("a", ImmutableMap.of("a", PrimitiveTypes.INTEGER), PrimitiveTypes.INTEGER),
            Triple.of("-a", ImmutableMap.of("a", PrimitiveTypes.INTEGER), PrimitiveTypes.INTEGER),
            Triple.of("a + 1", ImmutableMap.of(), TypeChecker.unknownType)
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                Map<String, MegaType> environment = testCase.getMiddle();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    environment.forEach((key, value) -> env.addBindingWithType(key, value, true));

                    MegaType result = testTypecheckExpression(input, env);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckArrowFunction() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("(a: Int) => a + 1", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 2)),
                            "a",
                            new BasicTypeExpression("Int", Position.at(1, 5)),
                            PrimitiveTypes.INTEGER
                        ))
                ),
                PrimitiveTypes.INTEGER,
                Kind.ARROW_FN
            )),
            Pair.of("(a: Int, b: String) => a + b", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("a", Position.at(1, 2)),
                            "a",
                            new BasicTypeExpression("Int", Position.at(1, 5)),
                            PrimitiveTypes.INTEGER
                        )
                    ),
                    new Parameter(
                        new Identifier(
                            Token.ident("b", Position.at(1, 10)),
                            "b",
                            new BasicTypeExpression("String", Position.at(1, 13)),
                            PrimitiveTypes.STRING
                        )
                    )
                ),
                PrimitiveTypes.STRING,
                Kind.ARROW_FN
            )),
            Pair.of("() => 24",
                new FunctionType(
                    Lists.newArrayList(),
                    PrimitiveTypes.INTEGER,
                    Kind.ARROW_FN
                )
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @Test
    void testTypecheckArrowFunction_defaultValuedParameters_raisesError() {
        String input = "(a: String = 'asdf') => a + 1";
        TypeCheckResult result = testTypecheckExpressionAndGetResult(input);

        assertTrue(result.hasErrors());
        List<TypeCheckerError> errors = Lists.newArrayList(
            new UnsupportedFeatureError("Arrow functions cannot have default-valued parameters", Position.at(1, 2))
        );
        assertEquals(errors, result.errors);
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_arrowFunctionInvocation_unnamedArgs() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("((a: Int) => a + 1)(1)", PrimitiveTypes.INTEGER),
            Pair.of("((s: String, a: Int) => a + s)(\"asdf\", 1)", PrimitiveTypes.STRING),
            Pair.of("((s1: String, a: Int, s2: String) => { (s1 + s2) * a })(\"asdf\", 1, \"qwer\")", PrimitiveTypes.STRING),
            Pair.of("((a: String) => (b: String) => a + b)(\"asdf\")(\"qwer\")", PrimitiveTypes.STRING),
            Pair.of("((a: String) => (b: String) => a + b)(\"asdf\")", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("b", Position.at(1, 18)),
                            "b",
                            new BasicTypeExpression("String", Position.at(1, 21)),
                            PrimitiveTypes.STRING
                        )
                    )
                ),
                PrimitiveTypes.STRING,
                ImmutableMap.of("a", new Binding(PrimitiveTypes.STRING, true, null)),
                Kind.ARROW_FN
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_declaredFunctionInvocation_unnamedArgs_defaultParamValues() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("func abc(a: Int = 4) { a + 1 }", "abc(1)", PrimitiveTypes.INTEGER),
            Triple.of("func abc(s: String, a: Int = 4) { a + s }", "abc(\"asdf\")", PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String fnDecl = testCase.getLeft();
                String input = testCase.getMiddle();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment typeEnv = new TypeEnvironment();
                    testTypecheckStatement(fnDecl, typeEnv);
                    MegaType result = testTypecheckExpression(input, typeEnv);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @Test
    void testTypecheckCallExpression_arrowFunctionInvocation_namedArgs_raisesError() {
        String input = "((a: Int) => a + 1)(a: 1)";
        TypeCheckResult result = testTypecheckExpressionAndGetResult(input);

        assertTrue(result.hasErrors());
        List<TypeCheckerError> errors = Lists.newArrayList(
            new UnsupportedFeatureError("Named argument invocation of arrow functions", Position.at(1, 20))
        );
        assertEquals(errors, result.errors);
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_declaredFunctionInvocation_namedArgs() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("func abc(a: Int) { a + 1 }", "abc(a: 1)", PrimitiveTypes.INTEGER),
            Triple.of("func abc(a: Int = 4) { a + 1 }", "abc()", PrimitiveTypes.INTEGER),
            Triple.of("func abc(s: String, a: Int) { a + s }", "abc(a: 1, s: \"asdf\")", PrimitiveTypes.STRING),
            Triple.of("func abc(s1: String, a: Int, s2: String) { (s1 + s2) * a }", "abc(s1: \"asdf\", a: 1, s2: \"qwer\")", PrimitiveTypes.STRING),
            Triple.of("func abc(a: String) { (b: String) => a + b }", "abc(a: \"asdf\")(\"qwer\")", PrimitiveTypes.STRING),
            Triple.of("func abc(a: String) { (b: String) => a + b }", "abc(a: \"asdf\")", new FunctionType(
                Lists.newArrayList(
                    new Parameter(
                        new Identifier(
                            Token.ident("b", Position.at(1, 24)),
                            "b",
                            new BasicTypeExpression("String", Position.at(1, 27)),
                            PrimitiveTypes.STRING
                        )
                    )
                ),
                PrimitiveTypes.STRING,
                ImmutableMap.of("a", new Binding(PrimitiveTypes.STRING, true, null)),
                Kind.ARROW_FN
            ))
        );

        return testCases.stream()
            .map(testCase -> {
                String fnDecl = testCase.getLeft();
                String input = testCase.getMiddle();
                MegaType type = testCase.getRight();

                String name = String.format("'%s', and then '%s' should typecheck to %s", fnDecl, input, type.signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment typeEnv = new TypeEnvironment();
                    testTypecheckStatement(fnDecl, typeEnv);
                    MegaType result = testTypecheckExpression(input, typeEnv);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_declaredFunctionInvocation_namedArgs_defaultParamValues() {
        List<Triple<String, String, MegaType>> testCases = Lists.newArrayList(
            Triple.of("func abc(a: Int = 4) { a + 1 }", "abc(a: 1)", PrimitiveTypes.INTEGER),
            Triple.of("func xyz(a: Int = 4) { a + 1 }", "xyz()", PrimitiveTypes.INTEGER),
            Triple.of("func abc(s: String, a: Int = 4) { a + s }", "abc(s: \"asdf\")", PrimitiveTypes.STRING)
        );

        return testCases.stream()
            .map(testCase -> {
                String fnDecl = testCase.getLeft();
                String input = testCase.getMiddle();
                MegaType type = testCase.getRight();

                String name = String.format("'%s', and then '%s' should typecheck to %s", fnDecl, input, type.signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment typeEnv = new TypeEnvironment();
                    testTypecheckStatement(fnDecl, typeEnv);
                    MegaType result = testTypecheckExpression(input, typeEnv);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_arrowFunctionInvocation_namedArgs_defaultParamValues_errors() {
        List<Triple<String, String, TypeCheckerError>> testCases = Lists.newArrayList(
            Triple.of("func abc(a: Int = 4) { a + 1 }", "abc(a: 1, b: 4)", new FunctionInvalidNamedArgumentError("b", Position.at(1, 11))),
            Triple.of("func abc(a: Int = 4) { a + 1 }", "abc(b: 4)", new FunctionInvalidNamedArgumentError("b", Position.at(1, 5))),
            Triple.of("func abc(s: String, a: Int = 4) { a + s }", "abc()", new FunctionWithDefaultParamValuesArityError(1, 0, Position.at(1, 4)))
        );

        return testCases.stream()
            .map(testCase -> {
                String fnDecl = testCase.getLeft();
                String input = testCase.getMiddle();
                TypeCheckerError error = testCase.getRight();

                String name = String.format("'%s' should typecheck with errors", input);
                return dynamicTest(name, () -> {
                    TypeEnvironment typeEnv = new TypeEnvironment();
                    testTypecheckStatementAndGetResult(fnDecl, typeEnv);
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(input, typeEnv);

                    assertTrue(result.hasErrors());
                    assertEquals(Lists.newArrayList(error), result.errors);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_declaredFunctionInvocation_namedArgs_errors() {
        class TestCase {
            private String fnDecl;
            private String input;
            private List<TypeCheckerError> errors;
            private MegaType type;

            private TestCase(String fnDecl, String input, List<TypeCheckerError> errors, MegaType type) {
                this.fnDecl = fnDecl;
                this.input = input;
                this.errors = errors;
                this.type = type;
            }
        }

        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                "func abc(a: Int) { a + 1 }",
                "abc(a: 'str!')",
                Lists.newArrayList(
                    new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 8))
                ),
                PrimitiveTypes.INTEGER
            ),
            new TestCase(
                "func abc(a: Int) { a + 1 }",
                "abc(a: 'str!', a: 3)",
                Lists.newArrayList(
                    new FunctionDuplicateNamedArgumentError("a", Position.at(1, 16))
                ),
                PrimitiveTypes.INTEGER
            ),
            new TestCase(
                "func abc(a: Int, b: String) { a * b }",
                "abc(a: 1)",
                Lists.newArrayList(
                    new FunctionMissingNamedArgumentError("b", Position.at(1, 4))
                ),
                PrimitiveTypes.STRING
            ),
            new TestCase(
                "func abc(a: Int, b: String) { a * b }",
                "abc(b: 1)",
                Lists.newArrayList(
                    new TypeMismatchError(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, Position.at(1, 8)),
                    new FunctionMissingNamedArgumentError("a", Position.at(1, 4))
                ),
                PrimitiveTypes.STRING
            ),
            new TestCase(
                "func abc(a: Int, b: String) { a * b }",
                "abc(b: 'hello', a: 1, c: 'huh?')",
                Lists.newArrayList(
                    new FunctionInvalidNamedArgumentError("c", Position.at(1, 23))
                ),
                PrimitiveTypes.STRING
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String fnDecl = testCase.fnDecl;
                String input = testCase.input;
                List<TypeCheckerError> errors = testCase.errors;
                MegaType type = testCase.type;

                String name = String.format("'%s' should typecheck to %s, with errors", input, type.signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment typeEnv = new TypeEnvironment();
                    testTypecheckStatement(fnDecl, typeEnv);
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(input, typeEnv);
                    // Even though typechecking fails, there should still be some kind of overall type returned
                    assertEquals(type, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(errors, result.errors);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_constructorFunctionInvocation_namedArgs() {
        class TestCase {
            private final List<String> typeDecls;
            private final List<String> valDecls;
            private final String valName;
            private final MegaType valType;

            private TestCase(List<String> typeDecls, List<String> valDecls, String valName, MegaType valType) {
                this.typeDecls = typeDecls;
                this.valDecls = valDecls;
                this.valName = valName;
                this.valType = valType;
            }
        }
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase(
                Lists.newArrayList(
                    "type Person = { name: String, age: Int }"
                ),
                Lists.newArrayList(
                    "val p = Person(name: 'Ken', age: 26)",
                    "val p = Person(age: 26, name: 'Ke' + 'n')"
                ),
                "p",
                new StructType("Person", Lists.newArrayList(
                    Pair.of("name", PrimitiveTypes.STRING),
                    Pair.of("age", PrimitiveTypes.INTEGER)
                ))
            ),
            new TestCase(
                Lists.newArrayList(
                    "type Person = { name: String, age: Int }",
                    "type Team = { teamName: String, members: Array[Person] }"
                ),
                Lists.newArrayList(
                    "val t = Team(teamName: 'The Best Team', members: [Person(name: 'Ken', age: 26)])",
                    "val t = Team(teamName: 'The Best Team', members: [])",
                    "val t = Team(members: [Person(name: 'Ken', age: 26), Person(age: 25, name: 'Meg')], teamName: 'The' + ' ' + 'Best' + ' ' + 'Team')"
                ),
                "t",
                new StructType("Team", Lists.newArrayList(
                    Pair.of("teamName", PrimitiveTypes.STRING),
                    Pair.of("members", arrayOf.apply(new StructType("Person", Lists.newArrayList(
                        Pair.of("name", PrimitiveTypes.STRING),
                        Pair.of("age", PrimitiveTypes.INTEGER)
                    ))))
                ))
            )
        );

        return testCases.stream()
            .flatMap(testCase -> testCase.valDecls.stream()
                .map(valDecl -> {
                    String name = String.format("'%s' should typecheck to an instance of its type", valDecl);
                    return dynamicTest(name, () -> {
                        TypeEnvironment env = new TypeEnvironment();
                        testCase.typeDecls.forEach(decl -> testTypecheckStatement(decl, env));
                        testTypecheckStatement(valDecl, env);

                        Binding binding = env.getBinding(testCase.valName);
                        assertNotNull(binding);
                        assertEquals(testCase.valType, binding.type);
                    });
                }))
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckCallExpression_errors() {
        List<Triple<String, TypeCheckerError, MegaType>> testCases = Lists.newArrayList(
            // Uninvokeable type errors
            Triple.of("[1, 2, 3](1.3)", new UninvokeableTypeError(arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 1)), TypeChecker.unknownType),
            Triple.of("(1 + 3)(1)", new UninvokeableTypeError(PrimitiveTypes.INTEGER, Position.at(1, 1)), TypeChecker.unknownType),
            Triple.of("\"asdf\"(3)", new UninvokeableTypeError(PrimitiveTypes.STRING, Position.at(1, 1)), TypeChecker.unknownType),

            // Arity errors
            Triple.of("((a: Int) => a + 1)(3, 3)", new FunctionArityError(1, 2, Position.at(1, 20)), PrimitiveTypes.INTEGER),
            Triple.of("(() => 'asdf')(3)", new FunctionArityError(0, 1, Position.at(1, 15)), PrimitiveTypes.STRING),
            Triple.of("((a: Int, b: Int) => a + b)(3)", new FunctionArityError(2, 1, Position.at(1, 28)), PrimitiveTypes.INTEGER),

            // Param type errors
            Triple.of(
                "((a: Int, b: Int) => a + b)(3, 'asdf')",
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 32)),
                PrimitiveTypes.INTEGER
            ),
            Triple.of(
                "((a: Int, b: Int, c: Int) => [a, b])(3, ['asdf'], 3)",
                new TypeMismatchError(PrimitiveTypes.INTEGER, arrayOf.apply(PrimitiveTypes.STRING), Position.at(1, 41)),
                arrayOf.apply(PrimitiveTypes.INTEGER)
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.getLeft());
                    // Even though typechecking fails, there should still be some kind of overall type returned, even if it's <unknown>.
                    assertEquals(testCase.getRight(), result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(testCase.getMiddle(), result.errors.get(0));
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckIndexExpression() {
        List<Pair<String, MegaType>> testCases = Lists.newArrayList(
            Pair.of("[1, 2, 3][0]", PrimitiveTypes.INTEGER),
            Pair.of("[1.2, 2.3, 3.4][1]", PrimitiveTypes.FLOAT),
            Pair.of("[true, false, true][1]", PrimitiveTypes.BOOLEAN),
            Pair.of("['asdf', 'qwer'][1]", PrimitiveTypes.STRING),
            Pair.of("[['asdf', 'qwer'], ['zxcv']][1]", arrayOf.apply(PrimitiveTypes.STRING))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                MegaType type = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, type.signature());
                return dynamicTest(name, () -> {
                    MegaType result = testTypecheckExpression(input);
                    assertEquals(type, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckIndexExpression_errors() {
        List<Triple<String, TypeCheckerError, MegaType>> testCases = Lists.newArrayList(
            Triple.of("[1, 2, 3][1.3]", new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, Position.at(1, 11)), PrimitiveTypes.INTEGER),
            Triple.of("[1, 2, 3]['a']", new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 11)), PrimitiveTypes.INTEGER),
            Triple.of("'abc'[0]", new UnindexableTypeError(PrimitiveTypes.STRING, Position.at(1, 1)), TypeChecker.unknownType)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.getLeft());
                    // Even though typechecking fails, there should still be some kind of overall type returned, even if it's <unknown>.
                    assertEquals(testCase.getRight(), result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(testCase.getMiddle(), result.errors.get(0));
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckAssignmentExpression() {
        List<Pair<String, Map<String, MegaType>>> testCases = Lists.newArrayList(
            Pair.of("a = 1", ImmutableMap.of("a", PrimitiveTypes.INTEGER)),
            Pair.of("a = 1.3", ImmutableMap.of("a", PrimitiveTypes.FLOAT)),
            Pair.of("a = true", ImmutableMap.of("a", PrimitiveTypes.BOOLEAN)),

            Pair.of("a = a + 'hello'", ImmutableMap.of("a", PrimitiveTypes.STRING))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                Map<String, MegaType> environment = testCase.getRight();

                String name = String.format("'%s' should typecheck to Unit", input);
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    environment.forEach((key, value) -> env.addBindingWithType(key, value, false));

                    MegaType result = testTypecheckExpression(input, env);
                    assertEquals(PrimitiveTypes.UNIT, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckAssignmentExpression_errors() {
        List<Triple<String, Map<String, Pair<MegaType, Boolean>>, TypeCheckerError>> testCases = Lists.newArrayList(
            Triple.of(
                "a = 'hello'",
                ImmutableMap.of("a", Pair.of(PrimitiveTypes.INTEGER, false)),
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 5))
            ),
            Triple.of(
                "a = 'world'",
                ImmutableMap.of("a", Pair.of(PrimitiveTypes.STRING, true)),
                new MutabilityError("a", Position.at(1, 1))
            ),
            Triple.of(
                "a = 'world'",
                ImmutableMap.of(),
                new UnknownIdentifierError("a", Position.at(1, 1))
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    testCase.getMiddle().forEach((key, value) -> env.addBindingWithType(key, value.getLeft(), value.getRight()));
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.getLeft(), env);
                    assertEquals(PrimitiveTypes.UNIT, result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(testCase.getRight(), result.errors.get(0));
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckRangeExpression() {
        List<Pair<String, Map<String, MegaType>>> testCases = Lists.newArrayList(
            Pair.of("1..3", ImmutableMap.of()),
            Pair.of("-1..3", ImmutableMap.of()),
            Pair.of("a..c", ImmutableMap.of("a", PrimitiveTypes.INTEGER, "c", PrimitiveTypes.INTEGER))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getLeft();
                Map<String, MegaType> environment = testCase.getRight();

                String name = String.format("'%s' should typecheck to %s", input, arrayOf.apply(PrimitiveTypes.INTEGER).signature());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    environment.forEach((key, value) -> env.addBindingWithType(key, value, false));

                    MegaType result = testTypecheckExpression(input, env);
                    assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    List<DynamicTest> testTypecheckRangeExpression_errors() {
        List<Triple<String, Map<String, MegaType>, TypeCheckerError>> testCases = Lists.newArrayList(
            Triple.of(
                "'a'..'z'",
                ImmutableMap.of(),
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, Position.at(1, 1))
            ),
            Triple.of(
                "1.4..1.9",
                ImmutableMap.of(),
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, Position.at(1, 1))
            ),
            Triple.of(
                "true..false",
                ImmutableMap.of(),
                new TypeMismatchError(PrimitiveTypes.INTEGER, PrimitiveTypes.BOOLEAN, Position.at(1, 1))
            ),
            Triple.of(
                "[1, 2]..[4, 5]",
                ImmutableMap.of(),
                new TypeMismatchError(PrimitiveTypes.INTEGER, arrayOf.apply(PrimitiveTypes.INTEGER), Position.at(1, 1))
            ),
            Triple.of(
                "a..d",
                ImmutableMap.of("a", PrimitiveTypes.INTEGER, "d", new ObjectType(Lists.newArrayList(
                    Pair.of("a", PrimitiveTypes.INTEGER)
                ))),
                new TypeMismatchError(
                    PrimitiveTypes.INTEGER,
                    new ObjectType(Lists.newArrayList(
                        Pair.of("a", PrimitiveTypes.INTEGER)
                    )),
                    Position.at(1, 4)
                )
            ),
            Triple.of(
                "((a: Int) => a)..((b: Int) => b)",
                ImmutableMap.of(),
                new TypeMismatchError(
                    PrimitiveTypes.INTEGER,
                    new FunctionType(
                        Lists.newArrayList(
                            new Parameter(
                                new Identifier(
                                    Token.ident("a", Position.at(1, 3)),
                                    "a",
                                    new BasicTypeExpression("Int", Position.at(1, 6)),
                                    PrimitiveTypes.INTEGER
                                )
                            )
                        ),
                        PrimitiveTypes.INTEGER,
                        Kind.ARROW_FN
                    ),
                    Position.at(1, 2)
                )
            )
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    testCase.getMiddle().forEach((key, value) -> env.addBindingWithType(key, value, true));
                    TypeCheckResult result = testTypecheckExpressionAndGetResult(testCase.getLeft(), env);
                    assertEquals(arrayOf.apply(PrimitiveTypes.INTEGER), result.type);

                    assertTrue(result.hasErrors());
                    assertEquals(testCase.getRight(), result.errors.get(0));
                });
            })
            .collect(toList());
    }
}