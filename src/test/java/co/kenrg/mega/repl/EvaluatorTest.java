package co.kenrg.mega.repl;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.repl.evaluator.Environment;
import co.kenrg.mega.repl.evaluator.Evaluator;
import co.kenrg.mega.repl.object.BooleanObj;
import co.kenrg.mega.repl.object.EvalError;
import co.kenrg.mega.repl.object.FloatObj;
import co.kenrg.mega.repl.object.IntegerObj;
import co.kenrg.mega.repl.object.NullObj;
import co.kenrg.mega.repl.object.iface.Obj;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class EvaluatorTest {

    public Obj testEval(String input) {
        Environment env = new Environment();
        Parser p = new Parser(new Lexer(input));
        Module module = p.parseModule();

        return Evaluator.eval(module, env);
    }

    @TestFactory
    public List<DynamicTest> testEvalIntegerLiteral() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("5", 5),
            Pair.of("10.", 10),
            Pair.of("-17", -17),
            Pair.of("5 + 5 - 10", 0),
            Pair.of("2 * 2 - 2", 2),
            Pair.of("-50 + 100 + -50", 0),
            Pair.of("5 + 2 * 10", 25),
            Pair.of("50 / 2 * 2 + 10", 60),
            Pair.of("2 * (5 + 10) * 2", 60),
            Pair.of("(5 + 10 * 2 + 15 / 3) * 2 + -10", 50)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%d'", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new IntegerObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testEvalFloatLiteral() {
        List<Pair<String, Float>> testCases = Lists.newArrayList(
            Pair.of("5.0", 5.0f),
            Pair.of("1.03", 1.03f),
            Pair.of("0.123", 0.123f),
            Pair.of("-0.3", -0.3f),
            Pair.of("-50.0 + 100.0 + -50.0", 0f),
            Pair.of("5.1 + 2.0 * 10.0", 25.1f),
            Pair.of("50.0 / 2.0 * 2.0 + 10.12", 60.12f),
            Pair.of("2.0 * (5.0 + 10.0) * 2.0", 60.0f),
            Pair.of("(5.0 + 10.0 * 2.0 + 15.0 / 3.0) * 2.0 + -10.0", 50.0f),

            Pair.of("5.1 + 5", 10.1f),
            Pair.of("5.1 + 5 - 10.1", 0f),
            Pair.of("50.0 / 2.0 * 2.0 + 10", 60f)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%f'", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new FloatObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testEvalBooleanLiteral() {
        List<Pair<String, Boolean>> testCases = Lists.newArrayList(
            Pair.of("true", true),
            Pair.of("false", false),
            Pair.of("1 < 2", true),
            Pair.of("1 > 2", false),
            Pair.of("1 < 1", false),
            Pair.of("1 > 1", false),
            Pair.of("1 == 1", true),
            Pair.of("1 != 1", false),
            Pair.of("1 == 2", false),
            Pair.of("1 != 2", true),
            Pair.of("true == true", true),
            Pair.of("false == false", true),
            Pair.of("true == false", false),
            Pair.of("true != false", true),
            Pair.of("false != true", true),
            Pair.of("(1 < 2) == true", true),
            Pair.of("(1 < 2) == false", false),
            Pair.of("(1 > 2) == true", false),
            Pair.of("(1 > 2) == false", true)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%b'", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new BooleanObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testBangOperator() {
        List<Pair<String, Boolean>> testCases = Lists.newArrayList(
            Pair.of("!true", false),
            Pair.of("!false", true),
            Pair.of("!5", false),
            Pair.of("!!true", true),
            Pair.of("!!false", false),
            Pair.of("!!5", true)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%b'", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new BooleanObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testIfElseExpressions() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("if true { 10 }", 10),
            Pair.of("if false { 10 }", null),
            Pair.of("if 1 { 10 }", 10),
            Pair.of("if 1 > 2 { 10 }", null),
            Pair.of("if 1 < 2 { 10 }", 10),
            Pair.of("if 1 < 2 { 10 } else { 20 }", 10),
            Pair.of("if 1 > 2 { 10 } else { 20 }", 20)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%s'",
                    testCase.getKey(),
                    testCase.getValue() == null ? "nil" : testCase.getValue().toString()
                );

                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    if (testCase.getValue() == null) {
                        assertEquals(NullObj.NULL, result);
                    } else {
                        assertEquals(new IntegerObj(testCase.getValue()), result);
                    }
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testErrorReporting() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("5 + true;", "unknown operator: INTEGER + BOOLEAN"),
            Pair.of("5 + true; 5;", "unknown operator: INTEGER + BOOLEAN"),
            Pair.of("-true", "unknown operator: -BOOLEAN"),
            Pair.of("true + false", "unknown operator: BOOLEAN + BOOLEAN"),
            Pair.of("5; true * 2; 5;", "unknown operator: BOOLEAN * INTEGER"),
            Pair.of("if 10 > 1 { true + false; 5 }", "unknown operator: BOOLEAN + BOOLEAN"),
            Pair.of("if 10 > 1 { if 10 > 1 { true + false } }", "unknown operator: BOOLEAN + BOOLEAN"),

            Pair.of("foobar", "unknown identifier: foobar")
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should have error '%s'",
                    testCase.getKey(),
                    testCase.getValue()
                );

                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertTrue(result instanceof EvalError);
                    assertEquals(testCase.getValue(), ((EvalError) result).message);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testLetStatementBinding() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("let a = 5; a;", 5),
            Pair.of("let a = 5 * 5; a", 25),
            Pair.of("let a = 5; let b = a; b;", 5),
            Pair.of("let a = 5; let b = a; let c = a + b + 5; c;", 15)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should have error '%s'",
                    testCase.getKey(),
                    testCase.getValue()
                );

                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new IntegerObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }
}