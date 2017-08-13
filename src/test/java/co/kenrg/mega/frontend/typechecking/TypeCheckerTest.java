package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckExpression;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckStatement;
import static co.kenrg.mega.frontend.typechecking.TypeCheckerTestUtils.testTypecheckStatementAndGetResult;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.frontend.typechecking.TypeChecker.TypecheckResult;
import co.kenrg.mega.repl.object.iface.ObjectType;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class TypeCheckerTest {

    @TestFactory
    public List<DynamicTest> testTypecheckIntegerLiteral() {
        List<String> testCases = Lists.newArrayList(
            "5",
            "10."
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Int", testCase);
                return dynamicTest(name, () -> {
                    ObjectType result = testTypecheckExpression(testCase);
                    assertEquals(ObjectType.INTEGER, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckFloatLiteral() {
        List<String> testCases = Lists.newArrayList(
            "5.0",
            "1.03",
            "0.123"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Float", testCase);
                return dynamicTest(name, () -> {
                    ObjectType result = testTypecheckExpression(testCase);
                    assertEquals(ObjectType.FLOAT, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckBooleanLiteral() {
        List<String> testCases = Lists.newArrayList(
            "true", "false"
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Bool", testCase);
                return dynamicTest(name, () -> {
                    ObjectType result = testTypecheckExpression(testCase);
                    assertEquals(ObjectType.BOOLEAN, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckStringLiteral() {
        List<String> testCases = Lists.newArrayList(
            "\"hello\"",
            "\"hello \\u1215!\"",
            "\"Meet me at\n the \\uCAFE?\""
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to String", testCase);
                return dynamicTest(name, () -> {
                    ObjectType result = testTypecheckExpression(testCase);
                    assertEquals(ObjectType.STRING, result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckBindingDeclarationStatements_letAndVar() {
        List<Triple<String, String, ObjectType>> testCases = Lists.newArrayList(
            Triple.of("let s = \"asdf\"", "s", ObjectType.STRING),
            Triple.of("let s: String = \"asdf\"", "s", ObjectType.STRING),
            Triple.of("let i = 123", "i", ObjectType.INTEGER),
            Triple.of("let i: Int = 123", "i", ObjectType.INTEGER),
            Triple.of("let f = 12.34", "f", ObjectType.FLOAT),
            Triple.of("let f: Float = 12.34", "f", ObjectType.FLOAT),
            Triple.of("let b = true", "b", ObjectType.BOOLEAN),
            Triple.of("let b: Bool = false", "b", ObjectType.BOOLEAN),

            Triple.of("var s = \"asdf\"", "s", ObjectType.STRING),
            Triple.of("var s: String = \"asdf\"", "s", ObjectType.STRING),
            Triple.of("var i = 123", "i", ObjectType.INTEGER),
            Triple.of("var i: Int = 123", "i", ObjectType.INTEGER),
            Triple.of("var f = 12.34", "f", ObjectType.FLOAT),
            Triple.of("var f: Float = 12.34", "f", ObjectType.FLOAT),
            Triple.of("var b = true", "b", ObjectType.BOOLEAN),
            Triple.of("var b: Bool = false", "b", ObjectType.BOOLEAN)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should typecheck to %s", testCase.getLeft(), testCase.getRight());
                return dynamicTest(name, () -> {
                    TypeEnvironment env = new TypeEnvironment();
                    ObjectType result = testTypecheckStatement(testCase.getLeft(), env);
                    assertEquals(ObjectType.UNIT, result);

                    ObjectType bindingType = env.get(testCase.getMiddle());
                    assertEquals(testCase.getRight(), bindingType);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testTypecheckBindingDeclarationStatements_errors() {
        List<Triple<String, ObjectType, ObjectType>> testCases = Lists.newArrayList(
            Triple.of("let s: String = 123", ObjectType.STRING, ObjectType.INTEGER),
            Triple.of("let i: Int = \"asdf\"", ObjectType.INTEGER, ObjectType.STRING),
            Triple.of("let f: Float = 123", ObjectType.FLOAT, ObjectType.INTEGER),
            Triple.of("let b: Bool = 123", ObjectType.BOOLEAN, ObjectType.INTEGER),

            Triple.of("var s: String = 123", ObjectType.STRING, ObjectType.INTEGER),
            Triple.of("var i: Int = \"asdf\"", ObjectType.INTEGER, ObjectType.STRING),
            Triple.of("var f: Float = 123", ObjectType.FLOAT, ObjectType.INTEGER),
            Triple.of("var b: Bool = 123", ObjectType.BOOLEAN, ObjectType.INTEGER)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should typecheck to Unit, binding should fail to typecheck", testCase.getLeft());
                return dynamicTest(name, () -> {
                    TypecheckResult result = testTypecheckStatementAndGetResult(testCase.getLeft());
                    assertEquals(ObjectType.UNIT, result.node.type);

                    assertTrue(result.hasErrors());
                    assertEquals(
                        new TypeError(testCase.getMiddle(), testCase.getRight()),
                        result.errors.get(0)
                    );
                });
            })
            .collect(toList());
    }
}