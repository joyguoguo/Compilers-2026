package framework.project2;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;

/**
 * Project 2 TODO.
 *
 * ANTLR's DefaultErrorStrategy is still responsible for the actual recovery
 * algorithm. Do NOT reimplement recoverInline(), single-token insertion, or
 * single-token deletion.
 *
 * Your task is to adapt ANTLR's missing-token and unwanted-token reports to
 * ErrorReporter using the line-number rules defined in the Project 2 spec.
 */
public final class Project2ErrorStrategy extends DefaultErrorStrategy {

    private final ErrorReporter reporter;

    public Project2ErrorStrategy(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    protected void reportMissingToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        IntervalSet expecting = getExpectedTokens(recognizer);
        Token current = recognizer.getCurrentToken();

        // The specified malformed inputs have one expected token at each
        // insertion point.  getMinElement() gives that symbolic token type
        // without parsing ANTLR's human-readable error message.
        reporter.reportMissing(
                expecting.getMinElement(),
                Math.max(0, current.getLine() - 1));
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        Token current = recognizer.getCurrentToken();
        reporter.reportExtraneous(
                current.getType(),
                Math.max(0, current.getLine() - 1));
    }
}
