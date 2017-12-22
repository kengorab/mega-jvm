package co.kenrg.mega.repl;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.backend.evaluation.evaluator.Evaluator;
import co.kenrg.mega.backend.evaluation.object.ArrayObj;
import co.kenrg.mega.backend.evaluation.object.BooleanObj;
import co.kenrg.mega.backend.evaluation.object.EvalError;
import co.kenrg.mega.backend.evaluation.object.FloatObj;
import co.kenrg.mega.backend.evaluation.object.IntegerObj;
import co.kenrg.mega.backend.evaluation.object.NullObj;
import co.kenrg.mega.backend.evaluation.object.ObjectObj;
import co.kenrg.mega.backend.evaluation.object.StringObj;
import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class EvaluatorTest {

    public Obj testEval(String input) {
        Environment env = new Environment();
        Parser p = new Parser(new Lexer(input));
        Module module = p.parseModule();

        // Note the missing typecheck pass; much of what's tested in this suite isn't possible to reach when
        // a typechecking pass is made. It's still worthwhile to test, though, I think.

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
    public List<DynamicTest> testEvalStringLiteral() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("\"hello\"", "hello"),
            Pair.of("\"hello \\u1215!\"", "hello ሕ!"),
            Pair.of("\"Meet me at\n the \\uCAFE?\"", "Meet me at\n the 쫾?"),

            // Test concatenation
            Pair.of("\"string 1\" + \" \" + \"string 2\"", "string 1 string 2"),
            Pair.of("\"string \" + 1", "string 1"),
            Pair.of("\"string \" + 2.0", "string 2.0"),
            Pair.of("\"string \" + true", "string true"),
            Pair.of("1 + \" string\"", "1 string"),
            Pair.of("2.0 + \" string\"", "2.0 string"),
            Pair.of("false + \" string\"", "false string"),

            Pair.of("3 * \"str\"", "strstrstr"),
            Pair.of("\"abc\" * 2", "abcabc"),

            // Test single-quote strings too
            Pair.of("'hello'", "hello"),
            Pair.of("'hello \\u1215!'", "hello ሕ!"),
            Pair.of("'Meet me at\n the \\uCAFE?'", "Meet me at\n the 쫾?"),

            Pair.of("val a = 24; \"$a hrs\"", "24 hrs"),
            Pair.of("val a = 24; \"${a} hrs\"", "24 hrs"),
            Pair.of("val a = [24]; \"${a[0]} hrs\"", "24 hrs"),
            Pair.of("val a = [24]; \"$a[0] hrs\"", "[24][0] hrs")
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%s'", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new StringObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    @Disabled
    public List<DynamicTest> testEvalStringLiteral_withInterpolations() {
        List<Pair<String, String>> testCases = Lists.newArrayList(
            Pair.of("val a = 24; \"$a hrs\"", "24 hrs"),
            Pair.of("val a = 24; \"${a} hrs\"", "24 hrs"),
            Pair.of("val a = [24]; \"${a[0]} hrs\"", "24 hrs"),
            Pair.of("val a = [24]; \"$a[0] hrs\"", "[24][0] hrs"),
            Pair.of("val a = \"24\"; \"$a hrs\"", "24 hrs")
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%s'", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(new StringObj(testCase.getValue()), result);
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
            Pair.of("1 <= 2", true),
            Pair.of("1 > 2", false),
            Pair.of("1 >= 2", false),
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
                    assertEquals(BooleanObj.of(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @TestFactory
    public List<DynamicTest> testEvalBooleanInfixExpression_shortCircuiting() {
        List<Pair<String, Boolean>> testCases = Lists.newArrayList(
            Pair.of("func retTrue() { true }; func retFalseWithEvalError() { 5 + true; false }; retTrue() || retFalse()", true),
            Pair.of("func retTrueWithEvalError() { 5 + true; true }; func retFalse() { false }; retFalse() && retTrue()", false)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format("'%s' should evaluate to '%s', short-circuiting and throwing no error", testCase.getKey(), testCase.getValue());
                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    assertEquals(BooleanObj.of(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }

    @Test
    public void testEvalArrayLiteral() {
        String input = "[1 + 1, 2 * 2, 3 + 3]";
        Obj result = testEval(input);
        assertEquals(
            new ArrayObj(Lists.newArrayList(
                new IntegerObj(2),
                new IntegerObj(4),
                new IntegerObj(6)
            )),
            result
        );
    }

    @Test
    public void testEvalObjectLiteral() {
        String input = "{ prop1: \"hello\", prop2: \"world\", prop3: 1234 }";
        Obj result = testEval(input);
        assertEquals(
            new ObjectObj(ImmutableMap.of(
                "prop1", new StringObj("hello"),
                "prop2", new StringObj("world"),
                "prop3", new IntegerObj(1234)
            )),
            result
        );
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
                    assertEquals(BooleanObj.of(testCase.getValue()), result);
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
            Pair.of("5.0 * \"abc\"", "unknown operator: FLOAT * STRING"),
            Pair.of("\"abc\" * 5.0", "unknown operator: STRING * FLOAT"),
            Pair.of("-true", "unknown operator: -BOOLEAN"),
            Pair.of("true + false", "unknown operator: BOOLEAN + BOOLEAN"),
            Pair.of("5; true * 2; 5;", "unknown operator: BOOLEAN * INTEGER"),
            Pair.of("if 10 > 1 { true + false; 5 }", "unknown operator: BOOLEAN + BOOLEAN"),
            Pair.of("if 10 > 1 { if 10 > 1 { true + false } }", "unknown operator: BOOLEAN + BOOLEAN"),

            Pair.of("foobar", "unknown identifier: foobar"),
            Pair.of("\"$a\"", "unknown identifier: a"),

            Pair.of("1()", "cannot invoke 1 as a function: incompatible type INTEGER"),
            Pair.of("[]()", "cannot invoke [] as a function: incompatible type ARRAY"),
            Pair.of("{}()", "cannot invoke {} as a function: incompatible type OBJECT"),

            Pair.of("val s = \"asdf\"; val s = 3", "duplicate binding: s already defined in this context"),

            Pair.of("val a = \"asdf\"; a = \"qwer\"", "cannot reassign to immutable binding: a"),
            Pair.of("b = \"qwer\"", "unknown identifier: b"),

            Pair.of("var s = \"asdf\"; var s = 3", "duplicate binding: s already defined in this context"),
            Pair.of("val s = \"asdf\"; var s = 3", "duplicate binding: s already defined in this context"),
            Pair.of("val s = \"asdf\"; val s = 3", "duplicate binding: s already defined in this context"),
            Pair.of("func abc(x) { x }; val abc = 3", "duplicate binding: abc already defined in this context"),
            Pair.of("val abc = 3; func abc(x) { x }", "duplicate binding: abc already defined in this context")
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
    public List<DynamicTest> testValStatementBinding() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("val a = 5; a;", 5),
            Pair.of("val a = 5 * 5; a", 25),
            Pair.of("val a = 5; val b = a; b;", 5),
            Pair.of("val a = 5; val b = a; val c = a + b + 5; c;", 15)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%d'",
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

    @TestFactory
    public List<DynamicTest> testVarStatementBinding() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("var a = 5; a;", 5),
            Pair.of("var a = 5 * 5; a", 25),
            Pair.of("var a = 5; var b = a; b;", 5),
            Pair.of("var a = 5; var b = a; var c = a + b + 5; c;", 15)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%d'",
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

    @TestFactory
    public List<DynamicTest> testAssignmentExpression() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("var a = 5; a = 6; a", 6),
            Pair.of("var a = 5; a = a * 5; a", 25),
            Pair.of("val a = 5; var b = a + 1; b + a;", 11)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%d'",
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

    @TestFactory
    public List<DynamicTest> testFunctionApplication() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("val identity = x => x; identity(5);", 5),
            Pair.of("val identity = (x) => x; identity(5);", 5),
            Pair.of("val add = (a, b) => a + b; add(add(1, 2), add(1, 1))", 5),
            Pair.of("((a, b) => a + b)(1 + 1, 4 - 1)", 5),

            Pair.of("func sum(a, b) { a + b }; sum(4, 1)", 5)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%d'",
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

    @TestFactory
    public List<DynamicTest> testFunctionClosures() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("val adder = x => n => x + n; val addOne = adder(1); addOne(4)", 5),
            Pair.of("val a = 2; val addA = x => x + a; addA(3)", 5)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%d'",
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

    @TestFactory
    public List<DynamicTest> testArrayIndexing() {
        List<Pair<String, Integer>> testCases = Lists.newArrayList(
            Pair.of("[1, 2, 3][0]", 1),
            Pair.of("[1, 2, 3][1]", 2),
            Pair.of("[1, 2, 3][2]", 3),
            Pair.of("[1, 2, 3][3]", null),
            Pair.of("[1, 2, 3][-1]", null),

            Pair.of("val i = 0; [1, 2, 3][i]", 1),
            Pair.of("val arr = [1, 2, 3]; arr[arr[0]]", 2)
        );

        return testCases.stream()
            .map(testCase -> {
                String name = String.format(
                    "'%s' should evaluate to '%d'",
                    testCase.getKey(),
                    testCase.getValue()
                );

                return dynamicTest(name, () -> {
                    Obj result = testEval(testCase.getKey());
                    Obj expected = testCase.getValue() == null
                        ? NullObj.NULL
                        : new IntegerObj(testCase.getValue());
                    assertEquals(expected, result);
                });
            })
            .collect(toList());
    }

    @Test
    public void testForInLoop() {
        String input = "" +
            "val arr = [1, 2, 3]\n" +
            "var a = 1\n" +
            "for x in arr {\n" +
            "  a = a * x\n" +
            "}\n" +
            "a";
        Obj result = testEval(input);
        assertEquals(new IntegerObj(6), result);

        String input2 = "" +
            "var a = 1\n" +
            "for x in 1..4 {\n" +
            "  a = a * x\n" +
            "}\n" +
            "a";
        Obj result2 = testEval(input2);
        assertEquals(new IntegerObj(6), result2);
    }

    @TestFactory
    public List<DynamicTest> testRangeExpression() {
        List<Pair<String, List<Integer>>> testCases = Lists.newArrayList(
            Pair.of("1..1", Lists.newArrayList()),
            Pair.of("5..2", Lists.newArrayList()),

            Pair.of("1..2", Lists.newArrayList(1)),
            Pair.of("1..3", Lists.newArrayList(1, 2)),
            Pair.of("1..3", Lists.newArrayList(1, 2)),

            Pair.of("val x = 1; x..3", Lists.newArrayList(1, 2)),
            Pair.of("val x = 1; (x - 1)..3", Lists.newArrayList(0, 1, 2)),
            Pair.of("val x = 1; (x)..4-1", Lists.newArrayList(1, 2)),

            Pair.of("(1..5)[0]..(4..7)[1]", Lists.newArrayList(1, 2, 3, 4))
        );

        return testCases.stream()
            .map(testCase -> {
                String input = testCase.getKey();
                List<Integer> expectedValues = testCase.getValue();
                String name = String.format("'%s' should evaluate to '%s'", input, expectedValues);
                return dynamicTest(name, () -> {
                    Obj result = testEval(input);
                    List<IntegerObj> expectedVals = expectedValues.stream().map(IntegerObj::new).collect(toList());
                    assertEquals(expectedVals, ((ArrayObj) result).elems);
                });
            })
            .collect(toList());
    }
}