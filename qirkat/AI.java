package qirkat;

import java.util.ArrayList;

import static qirkat.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Mariel Aquino
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 3;
    /**
     * A position magnitude indicating a win (for white if positive, black
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI for GAME that will play MYCOLOR.
     */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        _lastFoundMove = null;
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();

        System.out.println(myColor().toString()
                + " moves " + move.toString() + ".");

        return move;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        int bestMovesOne = -INFTY;
        int bestMovesNeg = +INFTY;
        Move best;
        best = null;
        ArrayList<Move> moves = board.getMoves();

        if (depth == 0 || (moves.size() == 0)) {
            int staticResponse = staticScore(board);
            return staticResponse;
        }

        for (Move m : moves) {
            boolean undo = board.legalMove(m);
            board.makeMove(m);
            int response = findMove(board, depth - 1,
                    false, -sense, alpha, beta);
            if (sense == 1) {
                if (response >= bestMovesOne) {
                    bestMovesOne = response;
                    alpha = Math.max(alpha, response);
                    best = m;
                }
            } else {
                if (response <= bestMovesNeg) {
                    bestMovesNeg = response;
                    beta = Math.min(beta, response);
                    best = m;
                }
            }
            if (undo) {
                board.undo();
            }
            if (beta <= alpha) {
                break;
            }
        }
        if (saveMove) {
            _lastFoundMove = best;
        }

        if (sense == 1) {
            return bestMovesOne;
        } else {
            return bestMovesNeg;
        }
    }

    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        int myScore = 0;
        for (int k = 0; k < 5 * 5; k += 1) {
            if (board.get(k).equals(myColor())) {
                myScore += 1;
            }
        }
        return myScore;
    }
}
