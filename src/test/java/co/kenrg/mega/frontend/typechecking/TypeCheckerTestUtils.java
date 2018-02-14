package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseExpressionStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseModule;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

class TypeCheckerTestUtils {
    static MegaType testTypecheckExpression(String input) {
        TypeCheckResult typeCheckResult = testTypecheckExpressionAndGetResult(input);
        assertTrue(typeCheckResult.errors.isEmpty(), "There should be no typechecking errors");
        assertEquals(typeCheckResult.type, typeCheckResult.node.getType(), "The result type should be set on the node");
        return typeCheckResult.type;
    }

    static MegaType testTypecheckExpression(String input, TypeEnvironment env) {
        TypeCheckResult typeCheckResult = testTypecheckExpressionAndGetResult(input, env);
        assertTrue(typeCheckResult.errors.isEmpty(), "There should be no typechecking errors");
        assertEquals(typeCheckResult.type, typeCheckResult.node.getType(), "The result type should be set on the node");
        return typeCheckResult.type;
    }

    static TypeCheckResult testTypecheckModuleAndGetResult(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckModuleAndGetResult(input, env);
    }

    static TypeCheckResult testTypecheckModuleAndGetResult(String input, Function<String, String> moduleProvider) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckModuleAndGetResult(input, env, moduleProvider);
    }

    static TypeCheckResult testTypecheckModuleAndGetResult(String input, TypeEnvironment env, Function<String, String> moduleProvider) {
        Module module = parseModule(input);

        TypeChecker typeChecker = new TypeChecker();
        typeChecker.setModuleProvider(moduleName -> {
            String moduleContents = moduleProvider.apply(moduleName);
            if (moduleContents == null) {
                return null;
            }

            Module m = parseModule(moduleContents);
            TypeEnvironment e = new TypeEnvironment();
            TypeChecker tc = new TypeChecker();
            return tc.typecheck(m, e);
        });
        TypeCheckResult<Module> typecheckResult = typeChecker.typecheck(module, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            typecheckResult.errors.forEach(e -> System.out.println("  " + e.message()));
        }
        return typecheckResult;
    }

    static TypeCheckResult testTypecheckModuleAndGetResult(String input, TypeEnvironment env) {
        Module module = parseModule(input);
        TypeChecker typeChecker = new TypeChecker();
        TypeCheckResult<Module> typecheckResult = typeChecker.typecheck(module, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            typecheckResult.errors.forEach(e -> System.out.println("  " + e.message()));
        }
        return typecheckResult;
    }

    static TypeCheckResult testTypecheckExpressionAndGetResult(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckExpressionAndGetResult(input, env);
    }

    static TypeCheckResult testTypecheckExpressionAndGetResult(String input, TypeEnvironment env) {
        ExpressionStatement expressionStatement = parseExpressionStatement(input);
        TypeChecker typeChecker = new TypeChecker();
        TypeCheckResult<ExpressionStatement> typecheckResult = typeChecker.typecheck(expressionStatement, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            typecheckResult.errors.forEach(e -> System.out.println("  " + e.message()));
        }
        return typecheckResult;
    }

    static MegaType testTypecheckStatement(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckStatement(input, env);
    }

    static MegaType testTypecheckStatement(String input, TypeEnvironment env) {
        TypeCheckResult typecheckResult = testTypecheckStatementAndGetResult(input, env);
        assertTrue(typecheckResult.errors.isEmpty(), "There should be no typechecking errors");
        return typecheckResult.type;
    }

    static TypeCheckResult testTypecheckStatementAndGetResult(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckStatementAndGetResult(input, env);
    }

    static TypeCheckResult testTypecheckStatementAndGetResult(String input, TypeEnvironment env) {
        Statement statement = parseStatement(input);
        TypeChecker typeChecker = new TypeChecker();
        TypeCheckResult<Statement> typecheckResult = typeChecker.typecheck(statement, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            typecheckResult.errors.forEach(e -> System.out.println("  " + e.message()));
        }
        return typecheckResult;
    }
}
