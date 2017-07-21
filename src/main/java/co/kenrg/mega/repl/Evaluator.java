package co.kenrg.mega.repl;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
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
import co.kenrg.mega.repl.object.iface.ObjectType;

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
            return nativeBoolToBoolObj(((BooleanLiteral) node).value);
        } else if (node instanceof PrefixExpression) {
            return evalPrefixExpression((PrefixExpression) node);
        } else if (node instanceof InfixExpression) {
            return evalInfixExpression((InfixExpression) node);
        } else if (node instanceof IfExpression) {
            return evalIfExpression((IfExpression) node);
        } else if (node instanceof BlockExpression) {
            return evalBlockExpression((BlockExpression) node);
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

    private static BooleanObj nativeBoolToBoolObj(boolean b) {
        if (b) {
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

    private static Obj evalInfixExpression(InfixExpression expr) {
        Obj leftResult = eval(expr.left);
        Obj rightResult = eval(expr.right);

        if (expr.operator.equals("==")) {
            return evalEqualsExpression(leftResult, rightResult, false);
        } else if (expr.operator.equals("!=")) {
            return evalEqualsExpression(leftResult, rightResult, true);
        }

        ObjectType leftType = leftResult.getType();
        ObjectType rightType = rightResult.getType();

        if (leftType.isNumeric() && rightType.isNumeric()) {
            return evalNumericInfixExpression(expr.operator, leftResult, rightResult);
        }

        return null;
    }

    private static Obj evalEqualsExpression(Obj left, Obj right, boolean negate) {
        boolean eq = left.equals(right);

        if (negate) {
            if (eq) {
                return BooleanObj.FALSE;
            } else {
                return BooleanObj.TRUE;
            }
        } else {
            return nativeBoolToBoolObj(eq);
        }
    }

    private static Obj evalNumericInfixExpression(String operator, Obj left, Obj right) {
        ObjectType leftType = left.getType();
        ObjectType rightType = right.getType();

        if (leftType == ObjectType.FLOAT || rightType == ObjectType.FLOAT) {
            float lval;
            if (leftType == ObjectType.INTEGER) {
                lval = (float) ((IntegerObj) left).value;
            } else {
                lval = ((FloatObj) left).value;
            }

            float rval;
            if (rightType == ObjectType.INTEGER) {
                rval = (float) ((IntegerObj) right).value;
            } else {
                rval = ((FloatObj) right).value;
            }
            return evalFloatInfixExpression(operator, lval, rval);
        } else {
            int lval = ((IntegerObj) left).value;
            int rval = ((IntegerObj) right).value;
            return evalIntegerInfixExpression(operator, lval, rval);
        }
    }

    private static Obj evalIntegerInfixExpression(String operator, int lval, int rval) {
        switch (operator) {
            case "+":
                return new IntegerObj(lval + rval);
            case "-":
                return new IntegerObj(lval - rval);
            case "*":
                return new IntegerObj(lval * rval);
            case "/":
                return new IntegerObj(lval / rval);
            case "<":
                return new BooleanObj(lval < rval);
            case ">":
                return new BooleanObj(lval > rval);
            default:
                return NullObj.NULL;
        }
    }

    private static Obj evalFloatInfixExpression(String operator, float lval, float rval) {
        switch (operator) {
            case "+":
                return new FloatObj(lval + rval);
            case "-":
                return new FloatObj(lval - rval);
            case "*":
                return new FloatObj(lval * rval);
            case "/":
                return new FloatObj(lval / rval);
            case "<":
                return new BooleanObj(lval < rval);
            case ">":
                return new BooleanObj(lval > rval);
            default:
                return NullObj.NULL;
        }
    }

    private static Obj evalBlockExpression(BlockExpression expression) {
        Obj result = NullObj.NULL;
        for (Statement statement : expression.statements) {
            result = eval(statement);
        }
        return result;
    }

    private static boolean isTruthy(Obj obj) {
        if (obj.equals(NullObj.NULL) || obj.equals(BooleanObj.FALSE)) {
            return false;
        } else {
            return true;
        }
    }

    private static Obj evalIfExpression(IfExpression expression) {
        Obj condition = eval(expression.condition);
        if (isTruthy(condition)) {
            return eval(expression.thenExpr);
        } else if (expression.condition != null) {
            return eval(expression.elseExpr);
        } else {
            return NullObj.NULL;
        }
    }
}
