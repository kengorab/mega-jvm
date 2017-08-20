package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseExpressionStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatement;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class TypeCheckerTestUtils {
    public static MegaType testTypecheckExpression(String input) {
        TypeCheckResult typeCheckResult = testTypecheckExpressionAndGetResult(input);
        assertTrue(typeCheckResult.errors.isEmpty(), "There should be no typechecking errors");
        return typeCheckResult.node.type;
    }

    public static TypeCheckResult testTypecheckExpressionAndGetResult(String input) {
        TypeEnvironment env = new TypeEnvironment();
        ExpressionStatement expressionStatement = parseExpressionStatement(input);
        TypeChecker typeChecker = new TypeChecker();
        TypeCheckResult<ExpressionStatement> typecheckResult = typeChecker.typecheck(expressionStatement, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            typecheckResult.errors.forEach(e -> System.out.println("  " + e.message()));
        }
        return typecheckResult;
    }

    public static MegaType testTypecheckStatement(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckStatement(input, env);
    }

    public static MegaType testTypecheckStatement(String input, TypeEnvironment env) {
        TypeCheckResult typecheckResult = testTypecheckStatementAndGetResult(input, env);
        assertTrue(typecheckResult.errors.isEmpty(), "There should be no typechecking errors");
        return typecheckResult.node.type;
    }

    public static TypeCheckResult testTypecheckStatementAndGetResult(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckStatementAndGetResult(input, env);
    }

    public static TypeCheckResult testTypecheckStatementAndGetResult(String input, TypeEnvironment env) {
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
