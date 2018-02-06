package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

public class CallExpression extends Expression {

    @Override
    public String repr(boolean debug, int indentLevel) {
        throw new NotImplementedException("This method should never be invoked on the CallExpression superclass; defer to un/named args subclasses");
    }

    @Override
    public Token getToken() {
        throw new NotImplementedException("This method should never be invoked on the CallExpression superclass; defer to un/named args subclasses");
    }

    public Expression getTarget() {
        throw new NotImplementedException("This method should never be invoked on the CallExpression superclass; defer to un/named args subclasses");
    }

    public static class UnnamedArgs extends CallExpression {
        public final Token token;
        public final Expression target; // The invokee
        public final List<Expression> arguments;

        public UnnamedArgs(Token token, Expression target, List<Expression> arguments) {
            this.token = token;
            this.target = target;
            this.arguments = arguments;
        }

        @Override
        public Expression getTarget() {
            return this.target;
        }

        @Override
        public String repr(boolean debug, int indentLevel) {
            return String.format(
                "%s(%s)",
                this.target.repr(debug, indentLevel),
                this.arguments.stream()
                    .map(arg -> arg.repr(debug, indentLevel))
                    .collect(joining(", "))
            );

        }

        @Override
        public Token getToken() {
            return this.token;
        }
    }

    public static class NamedArgs extends CallExpression {
        public final Token token;
        public final Expression target; // The invokee
        public final List<Pair<Identifier, Expression>> namedParamArguments;
        public final boolean hasNamedArguments;

        public NamedArgs(Token token, Expression target, List<Pair<Identifier, Expression>> namedParamArguments) {
            this.token = token;
            this.target = target;
            this.namedParamArguments = namedParamArguments;
            this.hasNamedArguments = true;
        }

        @Override
        public Expression getTarget() {
            return this.target;
        }

        @Override
        public String repr(boolean debug, int indentLevel) {
            return String.format(
                "%s(%s)",
                this.target.repr(debug, indentLevel),
                this.namedParamArguments.stream()
                    .map(arg -> String.format("%s: %s", arg.getKey().value, arg.getValue().repr(debug, indentLevel)))
                    .collect(joining(", "))
            );
        }

        @Override
        public Token getToken() {
            return this.token;
        }
    }
}
