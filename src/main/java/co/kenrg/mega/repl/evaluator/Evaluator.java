package co.kenrg.mega.repl.evaluator;

import static co.kenrg.mega.repl.object.EvalError.functionArityError;
import static co.kenrg.mega.repl.object.EvalError.uninvokeableTypeError;
import static co.kenrg.mega.repl.object.EvalError.unknownIdentifierError;
import static co.kenrg.mega.repl.object.EvalError.unknownInfixOperatorError;
import static co.kenrg.mega.repl.object.EvalError.unknownPrefixOperatorError;
import static co.kenrg.mega.repl.object.EvalError.unsupportedIndexOperationError;
import static co.kenrg.mega.repl.object.EvalError.unsupportedIndexTargetError;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.IndexExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.FunctionDeclarationStatement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.repl.object.ArrayObj;
import co.kenrg.mega.repl.object.ArrowFunctionObj;
import co.kenrg.mega.repl.object.BooleanObj;
import co.kenrg.mega.repl.object.FloatObj;
import co.kenrg.mega.repl.object.FunctionObj;
import co.kenrg.mega.repl.object.IntegerObj;
import co.kenrg.mega.repl.object.NullObj;
import co.kenrg.mega.repl.object.StringObj;
import co.kenrg.mega.repl.object.iface.InvokeableObj;
import co.kenrg.mega.repl.object.iface.Obj;
import co.kenrg.mega.repl.object.iface.ObjectType;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class Evaluator {

    public static Obj eval(Node node, Environment env) {
        if (node instanceof Module) {
            return evalStatements(((Module) node).statements, env);
        }

        // Statements
        if (node instanceof ExpressionStatement) {
            return eval(((ExpressionStatement) node).expression, env);
        } else if (node instanceof LetStatement) {
            return evalLetStatement((LetStatement) node, env);
        } else if (node instanceof FunctionDeclarationStatement) {
            return evalFunctionDeclarationStatement((FunctionDeclarationStatement) node, env);
        }

        // Expressions
        if (node instanceof IntegerLiteral) {
            return new IntegerObj(((IntegerLiteral) node).value);
        } else if (node instanceof FloatLiteral) {
            return new FloatObj(((FloatLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            return nativeBoolToBoolObj(((BooleanLiteral) node).value);
        } else if (node instanceof StringLiteral) {
            return new StringObj(((StringLiteral) node).value);
        } else if (node instanceof ArrayLiteral) {
            return evalArrayLiteral((ArrayLiteral) node, env);
        } else if (node instanceof PrefixExpression) {
            return evalPrefixExpression((PrefixExpression) node, env);
        } else if (node instanceof InfixExpression) {
            return evalInfixExpression((InfixExpression) node, env);
        } else if (node instanceof IfExpression) {
            return evalIfExpression((IfExpression) node, env);
        } else if (node instanceof BlockExpression) {
            return evalBlockExpression((BlockExpression) node, env);
        } else if (node instanceof Identifier) {
            return evalIdentifier((Identifier) node, env);
        } else if (node instanceof ArrowFunctionExpression) {
            return evalArrowFunctionExpression((ArrowFunctionExpression) node, env);
        } else if (node instanceof CallExpression) {
            return evalCallExpression((CallExpression) node, env);
        } else if (node instanceof IndexExpression) {
            return evalIndexExpression((IndexExpression) node, env);
        } else {
            return NullObj.NULL;
        }
    }

    private static Obj evalLetStatement(LetStatement statement, Environment env) {
        Obj value = eval(statement.value, env);
        if (value.isError()) {
            return value;
        }

        env.set(statement.name.value, value);

        return NullObj.NULL;
    }

    private static Obj evalFunctionDeclarationStatement(FunctionDeclarationStatement statement, Environment env) {
        FunctionObj function = new FunctionObj(
            statement.name.value,
            statement.parameters,
            statement.body,
            env
        );
        env.set(statement.name.value, function);
        return NullObj.NULL;
    }

    private static Obj evalStatements(List<Statement> statements, Environment env) {
        Obj result = null;
        for (Statement statement : statements) {
            result = eval(statement, env);
            if (result.isError()) {
                return result;
            }
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

    private static Obj evalIdentifier(Identifier ident, Environment env) {
        Obj value = env.get(ident.value);
        if (value == null) {
            return unknownIdentifierError(ident.value);
        }
        return value;
    }

    private static Obj evalArrayLiteral(ArrayLiteral array, Environment env) {
        List<Obj> elems = Lists.newArrayList();
        for (Expression element : array.elements) {
            Obj elem = eval(element, env);
            if (elem.isError()) {
                return elem;
            }
            elems.add(elem);
        }
        return new ArrayObj(elems);
    }

    private static Obj evalPrefixExpression(PrefixExpression expr, Environment env) {
        Obj result = eval(expr.expression, env);
        if (result.isError()) {
            return result;
        }

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
                        return unknownPrefixOperatorError(expr.operator, result);
                }
            default:
                return unknownPrefixOperatorError(expr.operator, result);
        }
    }

    private static Obj evalInfixExpression(InfixExpression expr, Environment env) {
        Obj leftResult = eval(expr.left, env);
        if (leftResult.isError()) {
            return leftResult;
        }

        Obj rightResult = eval(expr.right, env);
        if (rightResult.isError()) {
            return rightResult;
        }

        if (expr.operator.equals("==")) {
            return evalEqualsExpression(leftResult, rightResult, false);
        } else if (expr.operator.equals("!=")) {
            return evalEqualsExpression(leftResult, rightResult, true);
        }

        ObjectType leftType = leftResult.getType();
        ObjectType rightType = rightResult.getType();

        if (leftType.isNumeric() && rightType.isNumeric()) {
            return evalNumericInfixExpression(expr.operator, leftResult, rightResult);
        } else if (leftType == ObjectType.STRING || rightType == ObjectType.STRING) {
            return evalStringInfixExpression(expr.operator, leftResult, rightResult);
        }

        return unknownInfixOperatorError(expr.operator, leftResult, rightResult);
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

    private static Obj evalStringInfixExpression(String operator, Obj left, Obj right) {
        switch (operator) {
            case "+": {
                StringBuilder concatenation = new StringBuilder();
                if (left.getType() == ObjectType.STRING) {
                    concatenation.append(((StringObj) left).value);
                } else {
                    concatenation.append(left.inspect());
                }

                if (right.getType() == ObjectType.STRING) {
                    concatenation.append(((StringObj) right).value);
                } else {
                    concatenation.append(right.inspect());
                }

                return new StringObj(concatenation.toString());
            }
            case "*": {
                if (left.getType() == ObjectType.STRING) {
                    String str = ((StringObj) left).value;
                    if (right.getType().isNumeric() && right.getType() == ObjectType.INTEGER) {
                        int times = ((IntegerObj) right).value;
                        return new StringObj(Strings.repeat(str, times));
                    } else {
                        return unknownInfixOperatorError("*", left, right);
                    }
                } else if (right.getType() == ObjectType.STRING) {
                    String str = ((StringObj) right).value;
                    if (left.getType().isNumeric() && left.getType() == ObjectType.INTEGER) {
                        int times = ((IntegerObj) left).value;
                        return new StringObj(Strings.repeat(str, times));
                    } else {
                        return unknownInfixOperatorError("*", left, right);
                    }
                } else {
                    return unknownInfixOperatorError("*", left, right);
                }
            }
            default:
                return unknownInfixOperatorError(operator, left, right);
        }
    }

    private static Obj evalNumericInfixExpression(String operator, Obj left, Obj right) {
        ObjectType leftType = left.getType();
        ObjectType rightType = right.getType();

        Obj result;
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
            result = evalFloatInfixExpression(operator, lval, rval);
        } else {
            int lval = ((IntegerObj) left).value;
            int rval = ((IntegerObj) right).value;
            result = evalIntegerInfixExpression(operator, lval, rval);
        }

        if (result == null) {
            return unknownInfixOperatorError(operator, left, right);
        } else {
            return result;
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
            case "<=":
                return new BooleanObj(lval <= rval);
            case ">=":
                return new BooleanObj(lval >= rval);
            default:
                return null;
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
            case "<=":
                return new BooleanObj(lval <= rval);
            case ">=":
                return new BooleanObj(lval >= rval);
            default:
                return null;
        }
    }

    private static Obj evalBlockExpression(BlockExpression expression, Environment env) {
        Obj result = NullObj.NULL;
        for (Statement statement : expression.statements) {
            result = eval(statement, env);
            if (result.isError()) {
                return result;
            }
        }
        return result;
    }

    private static boolean isTruthy(Obj obj) {
        return !(obj.equals(NullObj.NULL) || obj.equals(BooleanObj.FALSE));
    }

    private static Obj evalIfExpression(IfExpression expression, Environment env) {
        Obj condition = eval(expression.condition, env);
        if (condition.isError()) {
            return condition;
        }

        if (isTruthy(condition)) {
            return eval(expression.thenExpr, env);
        } else if (expression.condition != null) {
            return eval(expression.elseExpr, env);
        } else {
            return NullObj.NULL;
        }
    }

    private static Obj evalArrowFunctionExpression(ArrowFunctionExpression expr, Environment env) {
        return new ArrowFunctionObj(expr, env);
    }

    private static Obj evalCallExpression(CallExpression expr, Environment env) {
        Obj result = eval(expr.target, env);
        if (result.isError()) {
            return result;
        }

        if (!(result instanceof InvokeableObj)) {
            return uninvokeableTypeError(result);
        }
        InvokeableObj func = (InvokeableObj) result;

        List<Obj> args = Lists.newArrayList();
        for (Expression argument : expr.arguments) {
            Obj arg = eval(argument, env);
            if (arg.isError()) {
                return arg;
            }
            args.add(arg);
        }

        List<Identifier> funcParams = func.getParams();

        if (args.size() != funcParams.size()) {
            return functionArityError(funcParams.size(), args.size());
        }

        Environment fnEnv = func.getEnvironment().createChildEnvironment();
        for (int i = 0; i < args.size(); i++) {
            Obj arg = args.get(i);
            Identifier param = funcParams.get(i);
            fnEnv.set(param.value, arg);
        }

        return eval(func.getBody(), fnEnv);
    }

    private static Obj evalIndexExpression(IndexExpression expr, Environment env) {
        Obj target = eval(expr.target, env);
        if (target.isError()) {
            return target;
        }
        if (target.getType() != ObjectType.ARRAY) {
            return unsupportedIndexTargetError(target);
        }

        Obj index = eval(expr.index, env);
        if (index.isError()) {
            return index;
        }
        if (index.getType() != ObjectType.INTEGER) {
            return unsupportedIndexOperationError(index);
        }

        ArrayObj array = (ArrayObj) target;
        int indexVal = ((IntegerObj) index).value;
        int maxVal = array.elems.size();

        if (indexVal < 0 || indexVal >= maxVal) {
            return NullObj.NULL;
        } else {
            return array.elems.get(indexVal);
        }
    }

}
