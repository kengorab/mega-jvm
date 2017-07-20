package co.kenrg.mega.repl;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.repl.object.BooleanObj;
import co.kenrg.mega.repl.object.FloatObj;
import co.kenrg.mega.repl.object.IntegerObj;
import co.kenrg.mega.repl.object.NullObj;
import co.kenrg.mega.repl.object.iface.Obj;

public class Evaluator {

    public static Obj eval(Node node) {
        if (node instanceof Module) {
            return evalStatements(((Module) node).statements);
        }

        // Statements
        if (node instanceof ExpressionStatement) {
            return eval(((ExpressionStatement) node).expression);
        }

        // Expressions
        if (node instanceof IntegerLiteral) {
            return new IntegerObj(((IntegerLiteral) node).value);
        } else if (node instanceof FloatLiteral) {
            return new FloatObj(((FloatLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            return nativeBoolToBoolObj((BooleanLiteral) node);
        } else if (node instanceof PrefixExpression) {
            return evalPrefixExpression((PrefixExpression) node);
        } else {
            return NullObj.NULL;
        }
    }

    private static Obj evalStatements(List<Statement> statements) {
        Obj result = null;
        for (Statement statement : statements) {
            result = eval(statement);
        }
        return result;
    }

    private static BooleanObj nativeBoolToBoolObj(BooleanLiteral b) {
        if (b.value) {
            return BooleanObj.TRUE;
        } else {
            return BooleanObj.FALSE;
        }
    }

    private static Obj evalPrefixExpression(PrefixExpression expr) {
        Obj result = eval(expr.expression);

        switch (expr.operator) {
            case "!":
                if (result.equals(BooleanObj.TRUE)) {
                    return BooleanObj.FALSE;
                } else if (result.equals(BooleanObj.FALSE)) {
                    return BooleanObj.TRUE;
                } else if (result.equals(NullObj.NULL)) {
                    return BooleanObj.TRUE;
                } else {
                    return BooleanObj.FALSE;
                }
            case "-":
                switch (result.getType()) {
                    case INTEGER:
                        return new IntegerObj(-((IntegerObj) result).value);
                    case FLOAT:
                        return new FloatObj(-((FloatObj) result).value);
                    default:
                        return NullObj.NULL;
                }
            default:
                return NullObj.NULL;
        }
    }
}
