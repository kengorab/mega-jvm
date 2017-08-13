package co.kenrg.mega.frontend.typechecking;

import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseExpressionStatement;
import static co.kenrg.mega.frontend.parser.ParserTestUtils.parseStatement;

import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.typechecking.TypeChecker.TypecheckResult;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class TypeCheckerTestUtils {
    public static ObjectType testTypecheckExpression(String input) {
        TypeEnvironment env = new TypeEnvironment();
        ExpressionStatement expressionStatement = parseExpressionStatement(input);
        TypeChecker typeChecker = new TypeChecker();
        TypecheckResult<ExpressionStatement> typecheckResult = typeChecker.typecheck(expressionStatement, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            for (TypeError error : typecheckResult.errors) {
                System.out.println("  Expected " + error.expected.displayName + ", got " + error.actual.displayName);
            }
        }
        return typecheckResult.node.type;
    }

    public static ObjectType testTypecheckStatement(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckStatement(input, env);
    }

    public static ObjectType testTypecheckStatement(String input, TypeEnvironment env) {
        TypecheckResult typecheckResult = testTypecheckStatementAndGetResult(input, env);
        return typecheckResult.node.type;
    }

    public static TypecheckResult testTypecheckStatementAndGetResult(String input) {
        TypeEnvironment env = new TypeEnvironment();
        return testTypecheckStatementAndGetResult(input, env);
    }

    public static TypecheckResult testTypecheckStatementAndGetResult(String input, TypeEnvironment env) {
        Statement statement = parseStatement(input);
        TypeChecker typeChecker = new TypeChecker();
        TypecheckResult<Statement> typecheckResult = typeChecker.typecheck(statement, env);

        if (typecheckResult.hasErrors()) {
            System.out.println("Typechecker errors:");
            for (TypeError error : typecheckResult.errors) {
                System.out.println("  Expected " + error.expected.displayName + ", got " + error.actual.displayName);
            }
        }
        return typecheckResult;
    }
}
