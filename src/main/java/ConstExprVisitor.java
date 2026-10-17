import framework.project2.ConstExprResult;
import generated.SplcBaseVisitor;
import generated.SplcParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstExprVisitor extends SplcBaseVisitor<Integer> {

    private final List<ConstExprResult> results = new ArrayList<>();

    public final List<ConstExprResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    protected final void addResult(String expression, int value) {
        results.add(new ConstExprResult(expression, value));
    }

    @Override
    public Integer visitLocalVarDef(SplcParser.LocalVarDefContext ctx) {
        // Only the complete initializer of a local definition is considered.
        // In particular, do not visit the expression tree independently: that
        // would incorrectly record constexpr subexpressions of a non-constexpr
        // initializer.
        if (ctx.ASSIGN() == null || ctx.expression() == null) {
            return null;
        }

        Integer value = evaluate(ctx.expression());
        if (value != null) {
            addResult(ctx.expression().getText(), value);
        }
        return null;
    }

    /**
     * Evaluate exactly the constexpr forms defined by Project 2.
     * A null result means that the expression is not a constexpr.
     */
    private Integer evaluate(SplcParser.ExpressionContext ctx) {
        if (ctx.Number() != null) {
            return Integer.parseInt(ctx.Number().getText());
        }

        // Identifiers used as function names are not constexpr.  This check
        // also rejects a bare identifier and keeps calls separate from the
        // parenthesized-expression case below.  It must come first because a
        // zero-argument call such as foo() has LPAREN but no argumentList.
        if (ctx.Identifier() != null) {
            return null;
        }

        // A parenthesized expression has one child expression and no operator.
        if (ctx.LPAREN() != null && ctx.argumentList() == null) {
            return evaluate(ctx.expression(0));
        }

        if (ctx.expression().size() == 1) {
            if (ctx.PLUS() != null) {
                return evaluate(ctx.expression(0));
            }
            if (ctx.MINUS() != null) {
                Integer operand = evaluate(ctx.expression(0));
                return operand == null ? null : -operand;
            }

            // NOT is deliberately not part of the constexpr definition.
            return null;
        }

        if (ctx.expression().size() != 2) {
            return null;
        }

        // Only arithmetic binary operators are constexpr.  This also rejects
        // indexing, member access, comparisons, logical operators, power, and
        // assignment expressions.
        if (ctx.STAR() == null && ctx.DIV() == null && ctx.MOD() == null
                && ctx.PLUS() == null && ctx.MINUS() == null) {
            return null;
        }

        Integer left = evaluate(ctx.expression(0));
        Integer right = evaluate(ctx.expression(1));
        if (left == null || right == null) {
            return null;
        }

        if (ctx.STAR() != null) {
            return left * right;
        }
        if (ctx.DIV() != null) {
            return left / right;
        }
        if (ctx.MOD() != null) {
            return left % right;
        }
        if (ctx.PLUS() != null) {
            return left + right;
        }
        return left - right;
    }
}
