package qirkat;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;

import static qirkat.PieceColor.*;
import static qirkat.Move.*;

/** A Qirkat board.   The squares are labeled by column (a char value between
 *  'a' and 'e') and row (a char value between '1' and '5'.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (with row 0 being the bottom row)
 *  counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Mariel Aquino
 */
class Board extends Observable {
    /** A data structure to store the PieceColor objects. */
    private PieceColor[] playBoard;
    /** Where we store past moves for undo. */
    private Stack<PieceColor[]> undoStore = new Stack<>();
    /** Array of booleans for right movement. */
    private boolean[] cantRightMove = new boolean[5 * 5];
    /** Array of booleans for left movement. */
    private boolean[] cantLeftMove = new boolean[5 * 5];

    /** A new, cleared board at the start of the game. */
    Board() {

        playBoard = new PieceColor[5 * 5];
        for (int i = 0; i < playBoard.length; i += 1) {
            playBoard[i] = WHITE;
        }
        clear();
    }

    /**
     * A copy of B.
     */
    Board(Board b) {
        internalCopy(b);
    }

    /**
     * Return a constant view of me (allows any access method, but no
     * method that modifies it).
     */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions. Lol
     */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        cantRightMove = new boolean[5 * 5];
        cantLeftMove = new boolean[5 * 5];

        for (int i = 0; i < 10; i += 1) {
            playBoard[i] = WHITE;
        }

        playBoard[13] = WHITE;
        playBoard[14] = WHITE;
        playBoard[12] = EMPTY;
        playBoard[11] = BLACK;
        playBoard[10] = BLACK;

        for (int i = 15; i < 5 * 5; i += 1) {
            playBoard[i] = BLACK;
        }

        setChanged();
        notifyObservers();
    }

    /**
     * Copy B into me.
     */
    void copy(Board b) {
        internalCopy(b);
    }

    /**
     * Copy B into me.
     */
    private void internalCopy(Board b) {
        this.playBoard = new PieceColor[5 * 5];
        for (int i = 0; i < b.playBoard.length; i += 1) {
            this.playBoard[i] = b.playBoard[i];
        }
        this._gameOver = b._gameOver;
        this._whoseMove = b._whoseMove;
        for (int i = 0; i < this.cantLeftMove.length; i += 1) {
            this.cantLeftMove[i] = b.cantLeftMove[i];
        }
        for (int i = 0; i < this.cantRightMove.length; i += 1) {
            this.cantRightMove[i] = b.cantRightMove[i];
        }
        this.undoStore = new Stack<PieceColor[]>();
        setChanged();
        notifyObservers();
    }

    /**
     * Set my contents as defined by STR.  STR consists of 25 characters,
     * each of which is b, w, or -, optionally interspersed with whitespace.
     * These give the contents of the Board in row-major order, starting
     * with the bottom row (row 1) and left column (column a). All squares
     * are initialized to allow horizontal movement in either direction.
     * NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }


        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b':
            case 'B':
                set(k, BLACK);
                break;
            case 'w':
            case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }
        _whoseMove = nextMove;
        setChanged();
        notifyObservers();
    }

    /**
     * Return true iff the game is over: i.e., if the current player has
     * no moves.
     */
    boolean gameOver() {
        return _gameOver;
    }

    /**
     * Return the current contents of square C R, where 'a' <= C <= 'e',
     * and '1' <= R <= '5'.
     */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /**
     * Return the current contents of the square at linearized index K.
     */
    PieceColor get(int k) {
        assert validSquare(k);
        return playBoard[k];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'e', and
     * '1' <= R <= '5'.
     */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);

    }

    /**
     * Set get(K) to V, where K is the linearized index of a square.
     */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        this.playBoard[k] = v;
    }

    /**
     * Return true iff MOV is legal on the current board.
     */
    boolean legalMove(Move mov) {
        if (gameOver()) {
            return false;
        }
        if (!mov.isJump() && jumpPossible(mov.col0(), mov.row0())) {
            return false;
        }
        if (_whoseMove == WHITE && mov.row0() == '5' && !mov.isJump()) {
            return false;
        }
        if (_whoseMove == BLACK && mov.row0() == '1' && !mov.isJump()) {
            return false;
        }
        if (_whoseMove != playBoard[index(mov.col0(), mov.row0())]) {
            return false;
        }
        if (_whoseMove == WHITE && mov.row0() > mov.row1() && !mov.isJump()) {
            return false;
        }
        if (_whoseMove == BLACK && mov.row1() > mov.row0() && !mov.isJump()) {
            return false;
        }
        if (mov.isRightMove() && cantRightMove[index(mov.col0(), mov.row0())]) {
            return false;
        }
        if (mov.isLeftMove() && cantLeftMove[index(mov.col0(), mov.row0())]) {
            return false;
        }
        return true;
    }



    /**
     * Return a list of all legal moves from the current position.
     */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /**
     * Add all legal moves from the current position to MOVES.
     */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }

    /**
     * Add all legal non-capturing moves from the position
     * with linearized index K to MOVES.
     */
    private void getMoves(ArrayList<Move> moves, int k) {
        if (colToChar(k) != 'a') {
            if (playBoard[k - 1] == EMPTY) {
                if (getLeftCondition(k)) {
                    getMovesLeft(moves, k);
                }
            }
        }
        if (rowToChar(k) != '5') {
            if (playBoard[k + 5] == EMPTY) {
                if (getUpCondition(k)) {
                    getMovesUp(moves, k);
                }
            }
        }
        if (colToChar(k) != 'e') {
            if (playBoard[k + 1] == EMPTY) {
                if (getRightCondition(k)) {
                    getMovesRight(moves, k);
                }
            }
        }
        if (rowToChar(k) != '1') {
            if (playBoard[k - 5] == EMPTY) {
                if (getDownCondition(k)) {
                    getMovesDown(moves, k);
                }
            }
        }
        if (rowToChar(k) != '5' && colToChar(k) != 'e' && k % 2 == 0) {
            if (playBoard[k + 6] == EMPTY) {
                if (getDiagRightUpCond(k)) {
                    getDiagonalRightUp(moves, k);
                }
            }
        }
        if (rowToChar(k) != '5' && colToChar(k) != 'a' && k % 2 == 0) {
            if (playBoard[k + 4] == EMPTY) {
                if (getDiagLeftUpCond(k)) {
                    getDiagonalLeftUp(moves, k);
                }
            }
        }
        if (diagRightDownBool(k)) {
            if (playBoard[k - 4] == EMPTY) {
                if (legalMove(Move.move(colToChar(k), rowToChar(k),
                        colToChar(k - 4), rowToChar(k - 4), null))) {
                    getDiagonalRightDown(moves, k);
                }
            }
        }
        if (diagLeftDownBool(k)) {
            if (playBoard[k - 6] == EMPTY) {
                if (legalMove(Move.move(colToChar(k), rowToChar(k),
                        colToChar(k - 6), rowToChar(k - 6), null))) {
                    getDiagonalLeftDown(moves, k);
                }
            }
        }
    }
    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean diagRightDownBool(int k) {
        return rowToChar(k) != '1' && colToChar(k) != 'e' && k % 2 == 0;
    }
    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean diagLeftDownBool(int k) {
        return rowToChar(k) != '1' && colToChar(k) != 'a' && k % 2 == 0;
    }
    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getMovesLeft(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k - 1), rowToChar(k), null));
    }

    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean getLeftCondition(int k) {
        return legalMove(Move.move(colToChar(k), rowToChar(k),
                colToChar(k - 1), rowToChar(k), null));
    }

    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getMovesUp(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k), rowToChar(k + 5), null));
    }

    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean getUpCondition(int k) {
        return legalMove(Move.move(colToChar(k), rowToChar(k),
                colToChar(k), rowToChar(k + 5), null));
    }

    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getMovesDown(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k), rowToChar(k - 5), null));
    }
    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean getDownCondition(int k) {
        return legalMove(Move.move(colToChar(k), rowToChar(k),
                colToChar(k), rowToChar(k - 5), null));
    }

    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getMovesRight(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k + 1), rowToChar(k), null));
    }

    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean getRightCondition(int k) {
        return legalMove(Move.move(colToChar(k), rowToChar(k),
                colToChar(k + 1), rowToChar(k), null));
    }

    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getDiagonalRightUp(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k + 6), rowToChar(k + 6), null));
    }
    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean getDiagRightUpCond(int k) {
        return legalMove(Move.move(colToChar(k), rowToChar(k),
                colToChar(k + 6), rowToChar(k + 6), null));
    }

    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getDiagonalLeftUp(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k + 4), rowToChar(k + 4), null));
    }
    /** Boolean helper.
     * @param k as the int
     * @return a boolean
     */
    private boolean getDiagLeftUpCond(int k) {
        return legalMove(Move.move(colToChar(k), rowToChar(k),
                colToChar(k + 4), rowToChar(k + 4), null));
    }
    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getDiagonalRightDown(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k), rowToChar(k),
                colToChar(k - 4), rowToChar(k - 4), null));
    }
    /** Move adding helper.
     * @param moves as the arraylist
     * @param k as integer
     */
    private void getDiagonalLeftDown(ArrayList<Move> moves, int k) {
        moves.add(Move.move(colToChar(k),
                rowToChar(k), colToChar(k - 6), rowToChar(k - 6), null));
    }



    /**
     * Take the index, and give me the column.
     * @param k for the integer
     * @return char for modNum
     */
    char colToChar(int k) {
        int modNum = k % 5;
        if (modNum == 0) {
            return 'a';
        } else if (modNum == 1) {
            return 'b';
        } else if (modNum == 2) {
            return 'c';
        } else if (modNum == 3) {
            return 'd';
        } else if (modNum == 4) {
            return 'e';
        } else {
            return 'f';
        }
    }

    /**
     * Take the index, and gives me the row.
     * @param k for the integer
     * @return char for
     */
    private char rowToChar(int k) {
        if (k >= 0 && k <= 4) {
            return '1';
        } else if (k >= 5 && k <= 9) {
            return '2';
        } else if (k >= 10 && k <= 14) {
            return '3';
        } else if (k >= 15 && k <= (10 + 9)) {
            return '4';
        } else {
            return '5';
        }
    }

    /**
     * Add all legal captures from the position with linearized index K
     * to MOVES.
     */
    private void getJumps(ArrayList<Move> moves, int k) {
        if (leftJCondition(k)) {
            addDirectionJumps(moves, k, k - 2);
        }
        if (rightJCondition(k)) {
            addDirectionJumps(moves, k, k + 2);
        }
        if (upJCondition(k)) {
            addDirectionJumps(moves, k, k + 10);
        }
        if (downJCondition(k)) {
            addDirectionJumps(moves, k, k - 10);
        }
        if (diagJRightUpCond(k)) {
            addDirectionJumps(moves, k, k + 12);
        }
        if (diagJRightDownCond(k)) {
            addDirectionJumps(moves, k, k - 8);
        }
        if (diagJLeftUpCond(k)) {
            addDirectionJumps(moves, k, k + 8);
        }
        if (diagJLeftDownCond(k)) {
            addDirectionJumps(moves, k, k - 12);
        }
    }

    /** An adding directional jumps helper.
     * @param moves is an arraylist
     * @param k is an integer
     * @param adj is an adjusted integer
     */
    private void addDirectionJumps(ArrayList<Move> moves, int k, int adj) {
        Move m = Move.move(colToChar(k), rowToChar(k),
                colToChar(adj), rowToChar(adj));
        ArrayList<Move> helper;
        ArrayList<Integer> usedStart = new ArrayList<Integer>();
        usedStart.add(k);
        helper = jumperHelp(adj, usedStart);
        if (helper.size() == 0) {
            moves.add(m);
        }
        for (Move x : helper) {
            moves.add(Move.move(m, x));
        }
    }
    /**
     * A diagonal jump helper method.
     * @param k is an integer
     * @param hasBeen is an arraylist
     * @return an arraylist
     */
    private ArrayList<Move> jumperHelp(int k, ArrayList<Integer> hasBeen) {
        ArrayList<Move> moves = new ArrayList<>();
        if (leftJCondition(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k - 2), rowToChar(k - 2), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k - 2, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        if (rightJCondition(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k + 2), rowToChar(k + 2), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k + 2, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        if (upJCondition(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k + 10), rowToChar(k + 10), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k + 10, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        if (downJCondition(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k - 10), rowToChar(k - 10), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k - 10, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        diagJumperHelp(moves, k, hasBeen);
        return moves;
    }

    /**
     * A diagonal jump helper method.
     * @param moves is an arraylist of moves
     * @param k is an integer
     * @param hasBeen is an arraylist
     * @return an arraylist
     */
    private ArrayList<Move> diagJumperHelp(ArrayList<Move> moves, int k,
                                           ArrayList<Integer> hasBeen) {
        if (diagJRightUpCond(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k + 12), rowToChar(k + 12), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k + 12, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        if (diagJRightDownCond(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k - 8), rowToChar(k - 8), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k - 8, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        if (diagJLeftUpCond(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k + 8), rowToChar(k + 8), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k + 8, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        if (diagJLeftDownCond(k)) {
            Move newFirst = Move.move(colToChar(k), rowToChar(k),
                    colToChar(k - 12), rowToChar(k - 12), null);
            makeMove(newFirst);
            ArrayList<Move> its2 = jumperHelp(k - 12, hasBeen);
            undo();
            if (its2.size() == 0) {
                moves.add(newFirst);
            }
            for (Move m : its2) {
                moves.add(Move.move(newFirst, m));
            }
        }
        return moves;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean upJCondition(int k) {
        char row = rowToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (row == '4' || row == '5') {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k + 5] == opposite
                && playBoard[k + 10] == EMPTY) {
            return true;
        }
        return false;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean downJCondition(int k) {
        char row = rowToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (row == '1' || row == '2') {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k - 5] == opposite
                && playBoard[k - 10] == EMPTY) {
            return true;
        }
        return false;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean leftJCondition(int k) {
        char col = colToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (col == 'a' || col == 'b') {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k - 1] == opposite
                && playBoard[k - 2] == EMPTY) {
            return true;
        }
        return false;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean rightJCondition(int k) {
        char col = colToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (col == 'd' || col == 'e') {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k + 1] == opposite
                && playBoard[k + 2] == EMPTY) {
            return true;
        }
        return false;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean diagJRightUpCond(int k) {
        char row = rowToChar(k);
        char col = colToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (row == '4' || row == '5') {
            return false;
        }
        if (col == 'd' || col == 'e') {
            return false;
        }
        if (k % 2 != 0 && (k + 6) % 2 != 0) {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k + 6] == opposite
                && playBoard[k + 12] == EMPTY) {
            return true;
        }
        return false;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean diagJRightDownCond(int k) {
        char row = rowToChar(k);
        char col = colToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (row == '1' || row == '2') {
            return false;
        }
        if (col == 'd' || col == 'e') {
            return false;
        }
        if (k % 2 != 0 && (k - 4) % 2 != 0) {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k - 4] == opposite
                && playBoard[k - 8] == EMPTY) {
            return true;
        }
        return false;
    }
    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean diagJLeftUpCond(int k) {
        char row = rowToChar(k);
        char col = colToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (row == '4' || row == '5') {
            return false;
        }
        if (col == 'a' || col == 'b') {
            return false;
        }
        if (k % 2 != 0 && (k - 4) % 2 != 0) {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k + 4] == opposite
                && playBoard[k + 8] == EMPTY) {
            return true;
        }
        return false;
    }

    /**
     * Conditional helper method.
     * @param k as the integer
     * @return a boolean for helper
     */
    private boolean diagJLeftDownCond(int k) {
        char row = rowToChar(k);
        char col = colToChar(k);
        PieceColor opposite = _whoseMove.opposite();
        if (row == '1' || row == '2') {
            return false;
        }
        if (col == 'a' || col == 'b') {
            return false;
        }
        if (k % 2 != 0 && (k - 6) % 2 != 0) {
            return false;
        }
        if (playBoard[k] == _whoseMove
                && playBoard[k - 6] == opposite
                && playBoard[k - 12] == EMPTY) {
            return true;
        }
        return false;
    }

    /**
     * Return true iff MOV is a valid jump sequence on the current board.
     * MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     * could be continued and are valid as far as they go.
     */
    boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        return false;
    }

    /**
     * Return true iff a jump is possible for a piece at position C R.
     */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /**
     * Return true iff a jump is possible for a piece at position with
     * linearized index K.
     */
    boolean jumpPossible(int k) {
        char col = colToChar(k);
        char row = rowToChar(k);
        if (playBoard[k] == _whoseMove) {
            PieceColor opposite = playBoard[k].opposite();
            if (leftCondition(col)) {
                if (playBoard[k - 1] == playBoard[k].opposite()
                        && playBoard[k - 2] == EMPTY) {
                    return true;
                }
            }
            if (rightCondition(col)) {
                if (playBoard[k + 1] == opposite
                        && playBoard[k + 2] == EMPTY) {
                    return true;
                }
            }
            if (upCondition(row)) {
                if (playBoard[k + 5] == opposite
                        && playBoard[k + 10] == EMPTY) {
                    return true;
                }
            }
            if (downCondition(row)) {
                if (playBoard[k - 5] == opposite
                        && playBoard[k - 10] == EMPTY) {
                    return true;
                }
            }
            if (upCondition(row) && rightCondition(col)) {
                if (playBoard[k + 6] == opposite
                        && playBoard[k + 12] == EMPTY) {
                    return true;
                }
            }
            if (upCondition(row) && leftCondition(col)) {
                if (playBoard[k + 4] == opposite && playBoard[k + 8] == EMPTY) {
                    return true;
                }
            }
            if (leftCondition(col) && downCondition(row)) {
                if (playBoard[k - 6] == opposite
                        && playBoard[k - 12] == EMPTY) {
                    return true;
                }
            }
            if (rightCondition(col) && downCondition(row)) {
                if (playBoard[k - 4] == opposite
                        && playBoard[k - 8] == EMPTY) {
                    return true;
                }
            }

        }
        return false;
    }
    /** Creating left condition.
     * @param col from index
     * @return boolean
     */
    private boolean leftCondition(char col) {
        return col != 'a' && col != 'b';
    }

    /** Creating right condition.
     * @param col from index
     * @return boolean
     */
    private boolean rightCondition(char col) {
        return col != 'd' && col != 'e';
    }

    /** Creating up condition.
     * @param row from index
     * @return boolean
     */

    private boolean upCondition(char row) {
        return row != '4' && row != '5';
    }

    /** Creating down condition.
     * @param row from index
     * @return boolean
     */

    private boolean downCondition(char row) {
        return row != '1' && row != '2';
    }

    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /** Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     *  Assumes the result is legal. */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /** Make the Move MOV on this Board, assuming it is legal. */
    void makeMove(Move mov) {
        if (!legalMove(mov)) {
            return;
        }
        assert legalMove(mov);
        PieceColor[] boardCopy = new PieceColor[5 * 5];
        for (int i = 0; i < this.playBoard.length; i += 1) {
            boardCopy[i] = this.playBoard[i];
        }
        undoStore.push(boardCopy);

        if (!mov.isJump()) {
            int index1 = index(mov.col0(), mov.row0());
            int index2 = index(mov.col1(), mov.row1());
            if (mov.isLeftMove()) {
                cantRightMove[index2] = true;
                cantRightMove[index1] = false;
            } else if (mov.isRightMove()) {
                cantLeftMove[index2] = true;
                cantLeftMove[index1] = false;
            } else {
                cantLeftMove[index2] = false;
                cantRightMove[index2] = false;
            }
            playBoard[index2] = playBoard[index1];
            playBoard[index1] = EMPTY;

        } else if (mov.isJump()) {
            while (mov != null) {
                int capIn = index(mov.col0(), mov.row0());
                int capIn2 = index(mov.col1(), mov.row1());
                int jumpedIndex = index(mov.jumpedCol(), mov.jumpedRow());
                playBoard[jumpedIndex] = EMPTY;
                playBoard[capIn2] = playBoard[capIn];
                playBoard[capIn] = EMPTY;
                cantLeftMove[capIn2] = false;
                cantRightMove[capIn2] = false;
                mov = mov.jumpTail();


            }
        }
        _whoseMove = _whoseMove.opposite();
        if (!isMove() && !jumpPossible()) {
            _gameOver = true;
        }
        setChanged();
        notifyObservers();
    }

    /** Undo the last move, if any. */
    void undo() {
        this.playBoard = undoStore.pop();
        _whoseMove = _whoseMove.opposite();
        _gameOver = false;
        setChanged();
        notifyObservers();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        for (char j = '5'; j >= '1'; j -= 1) {
            out.format(" ");
            for (char i = 'a'; i <= 'e'; i += 1) {
                int indexed = index(i, j);
                if (playBoard[indexed] == EMPTY) {
                    out.format(" -");
                } else if (playBoard[indexed] == BLACK) {
                    out.format(" b");
                } else if (playBoard[indexed] == WHITE) {
                    out.format(" w");
                }

            }
            if (j != '1') {
                out.format("\n");
            }
        }

        return out.toString();
    }

    /** Return true iff there is a move for the current player. */
    private boolean isMove() {
        return getMoves().size() != 0;
    }


    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Set true when game ends. */
    private boolean _gameOver;

    /** Convenience value giving values of pieces at each ordinal position. */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /** One cannot create arrays of ArrayList<Move>, so we introduce
     *  a specialized private list type for this purpose. */
    private static class MoveList extends ArrayList<Move> {
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Board) {
            Board b = (Board) o;
            return (b.toString().equals(toString())
                    && _whoseMove == b.whoseMove()
                    && _gameOver == b._gameOver);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 1;
    }


    /** A read-only view of a Board. */
    private class ConstantBoard extends Board implements Observer {
        /** A constant view of this Board. */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /** Undo the last move. */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }
}
