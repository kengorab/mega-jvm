package co.kenrg.mega.repl;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.lexer.Lexer;
import co.kenrg.mega.frontend.parser.Parser;
import co.kenrg.mega.repl.object.BooleanObj;
import co.kenrg.mega.repl.object.FloatObj;
import co.kenrg.mega.repl.object.IntegerObj;
import co.kenrg.mega.repl.object.iface.Obj;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class EvaluatorTest {

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
                    Parser p = new Parser(new Lexer(testCase.getKey()));
                    Module module = p.parseModule();

                    Obj result = Evaluator.eval(module);
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
                    Parser p = new Parser(new Lexer(testCase.getKey()));
                    Module module = p.parseModule();

                    Obj result = Evaluator.eval(module);
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
                    Parser p = new Parser(new Lexer(testCase.getKey()));
                    Module module = p.parseModule();

                    Obj result = Evaluator.eval(module);
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
                    Parser p = new Parser(new Lexer(testCase.getKey()));
                    Module module = p.parseModule();

                    Obj result = Evaluator.eval(module);
                    assertEquals(new BooleanObj(testCase.getValue()), result);
                });
            })
            .collect(toList());
    }
}