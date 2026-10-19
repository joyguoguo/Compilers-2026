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

        SplcParser.ExpressionContext initializer = ctx.expression();
        if (isConstExpr(initializer)) {
            int value = evaluate(initializer);
            addResult(initializer.getText(), value);
        }
        return null;
    }

    /**
     * Check whether an expression is a constexpr according to Project 2.
     * This method only checks the expression form; it does not calculate a
     * value.  Keeping the check separate makes it clear that a non-constexpr
     * initializer is never partially recorded.
     */
    private boolean isConstExpr(SplcParser.ExpressionContext ctx) {
        if (ctx.Number() != null) {
            return true;
        }

        // A direct identifier may be a variable, a function name, or a member
        // name.  None of these expression forms is a constexpr.
        if (ctx.Identifier() != null) {
            return false;
        }

        // Parentheses do not change whether the enclosed expression is a
        // constexpr.  A function call also has LPAREN, so argumentList must be
        // checked before treating this as a parenthesized expression.
        if (ctx.LPAREN() != null && ctx.argumentList() == null) {
            return isConstExpr(ctx.expression(0));
        }

        int expressionCount = ctx.expression().size();

        // The only allowed unary operators are unary plus and unary minus.
        if (expressionCount == 1) {
            if (ctx.PLUS() != null) {
                return isConstExpr(ctx.expression(0));
            }
            if (ctx.MINUS() != null) {
                return isConstExpr(ctx.expression(0));
            }

            // Logical NOT and other one-child forms are not constexpr.
            return false;
        }

        // Only the five arithmetic binary operators are allowed.  This
        // intentionally rejects indexing, member access, power, comparison,
        // logical, and assignment expressions.
        if (expressionCount == 2 && isArithmeticOperator(ctx)) {
            return isConstExpr(ctx.expression(0))
                    && isConstExpr(ctx.expression(1));
        }

        return false;
    }

    private boolean isArithmeticOperator(SplcParser.ExpressionContext ctx) {
        return ctx.STAR() != null
                || ctx.DIV() != null
                || ctx.MOD() != null
                || ctx.PLUS() != null
                || ctx.MINUS() != null;
    }

    /**
     * Evaluate an expression that has already been accepted by isConstExpr.
     */
    private int evaluate(SplcParser.ExpressionContext ctx) {
        if (ctx.Number() != null) {
            return Integer.parseInt(ctx.Number().getText());
        }

        if (ctx.LPAREN() != null && ctx.argumentList() == null) {
            return evaluate(ctx.expression(0));
        }

        if (ctx.expression().size() == 1) {
            int operand = evaluate(ctx.expression(0));

            if (ctx.PLUS() != null) {
                return operand;
            }
            if (ctx.MINUS() != null) {
                return -operand;
            }
        }

        int left = evaluate(ctx.expression(0));
        int right = evaluate(ctx.expression(1));

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
        if (ctx.MINUS() != null) {
            return left - right;
        }

        // This should be unreachable because evaluate is called only after
        // isConstExpr returned true.
        throw new IllegalArgumentException("Not a constexpr expression");
    }
}
