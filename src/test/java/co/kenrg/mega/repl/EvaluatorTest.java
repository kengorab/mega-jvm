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
            Pair.of("-17", -17)
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
            Pair.of("-0.3", -0.3f)
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
            Pair.of("false", false)
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