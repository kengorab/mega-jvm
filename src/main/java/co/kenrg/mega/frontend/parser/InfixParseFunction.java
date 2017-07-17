package co.kenrg.mega.frontend.parser;

import java.util.function.Function;

import co.kenrg.mega.frontend.ast.iface.Expression;

public interface InfixParseFunction extends Function<Expression, Expression> {
}
