import org.json.simple.parser.ParseException;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.*;

public class ChessGame {
    static Long WP = 0L, WN = 0L, WB = 0L, WR = 0L, WQ = 0L, WK = 0L, BP = 0L, BN = 0L, BB = 0L, BR = 0L, BQ = 0L, BK = 0L;
    static Hashtable<String, Long> pieceTables = new Hashtable<>();
    static int countMoves = 0;
    static int mateAtDepth = 0;
    static int lastMateAtDepth = -1;
    public static List<Piece> blackPieces;
    public static List<Piece> whitePieces;
    public static long pushMap = Long.MAX_VALUE;
    public static long captureMap = Long.MAX_VALUE;
    public static int numOfAttackers = 0;
    public static int blackMobility = 0;
    public static int whiteMobility = 0;
    public static int[][] PV;
    public static int[][] tempPV;
    public static int bestMoveEval = 0;

    static List<Piece> copy(List<Piece> l1) {
        List<Piece> res = new ArrayList<>();
        for (Piece p : l1) {
            res.add(p.copy());
        }
        return res;
    }


    public static Spot[][] generate_Board(int row) {
        Spot[][] bo = new Spot[row][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < row; j++) {
                bo[i][j] = new Spot(i, j, null);
            }
        }
        return bo;
    }

    private static boolean isKingStuckInMiddle(King k) {
        if (!k.isCastled) {
            if (k.getColor()) {
                return !GamePanel.wcks && !GamePanel.wcqs;
            } else {
                return !GamePanel.bcks && !GamePanel.bcqs;
            }
        }
        return false;
    }

    public static boolean isEndGame() {
        int score = 0;
        for (Piece p : GamePanel.whitePieces) {
            if (p.type == Type.KING)
                continue;
            score += p.evalPiece(true, false);
        }
        for (Piece p : GamePanel.blackPieces) {
            if (p.type == Type.KING)
                continue;
            score += p.evalPiece(true, false);
        }
        return score < 2600;
    }

    public static int isPawnsIsolated(boolean[] cols) {
        int result = 0;
        for (int i = 0; i < cols.length; i++) {
            if (i == 0 && cols[i]) {
                if (!cols[i + 1])
                    result++;
            } else if (i == 7 && cols[i]) {
                if (!cols[i - 1])
                    result++;
            } else {
                if (cols[i] && !cols[i + 1] && !cols[i - 1])
                    result++;
            }
        }
        return result;
    }

    private static boolean passed(Pawn p, Pawn[] pawns) {
        int i = p.getCol();
        if (p.getColor()) {
            if (i == 0) {
                if (pawns[0] != null && pawns[0].getCol() == i)
                    if (p.getRow() - pawns[0].getRow() > 0)
                        return false;
                if (pawns[1] != null && pawns[1].getCol() == i + 1) {
                    return p.getRow() - pawns[1].getRow() <= 0;
                }
            } else if (i == 7) {
                if (pawns[7] != null && pawns[7].getCol() == i)
                    if (p.getRow() - pawns[7].getRow() > 0)
                        return false;
                if (pawns[6] != null && pawns[6].getCol() == i - 1)
                    return p.getRow() - pawns[6].getRow() <= 0;
            } else {
                if (pawns[i + 1] != null && pawns[i + 1].getCol() == i + 1) {
                    if (p.getRow() - pawns[i + 1].getRow() > 0)
                        return false;
                }
                if (pawns[i] != null && pawns[i].getCol() == i)
                    if (p.getRow() - pawns[i].getRow() > 0)
                        return false;
                if (pawns[i - 1] != null && pawns[i - 1].getCol() == i - 1)
                    return p.getRow() - pawns[i - 1].getRow() <= 0;
            }
        } else {
            if (i == 0) {
                if (pawns[0] != null && pawns[0].getCol() == i)
                    if (pawns[0].getRow() - p.getRow() > 0)
                        return false;
                if (pawns[1] != null && pawns[1].getCol() == i + 1) {
                    return pawns[1].getRow() - p.getRow() <= 0;
                }
            } else if (i == 7) {
                if (pawns[7] != null && pawns[7].getCol() == i)
                    if (pawns[7].getRow() - p.getRow() > 0)
                        return false;
                if (pawns[6] != null && pawns[6].getCol() == i - 1)
                    return pawns[6].getRow() - p.getRow() <= 0;
            } else {
                if (pawns[i + 1] != null && pawns[i + 1].getCol() == i + 1) {
                    if (pawns[i + 1].getRow() - p.getRow() > 0)
                        return false;
                }
                if (pawns[i] != null && pawns[i].getCol() == i)
                    if (pawns[i].getRow() - p.getRow() > 0)
                        return false;
                if (pawns[i - 1] != null && pawns[i - 1].getCol() == i - 1)
                    return pawns[i - 1].getRow() - p.getRow() <= 0;
            }
        }
        return true;
    }

    private static int isFileOpen(int[] files, Spot[][] board, boolean color) {
        boolean semi;
        boolean open;
        boolean isQueen = false;
        int result = 0;
        for (int file : files) {
            open = true;
            semi = false;
            if (file == -1) break;
            for (int i = 0; i < 8; i++) {
                if (board[i][file].isFull()) {
                    if (board[i][file].getPiece().type == Type.QUEEN && board[i][file].getPiece().getColor() != color)
                        isQueen = true;
                    if (board[i][file].getPiece().type == Type.PAWN) {
                        open = false;
                        if (board[i][file].getPiece().getColor() != color)
                            semi = true;
                        else {
                            semi = false;
                            break;
                        }
                    }
                }
            }
            if (open) {
                result += 20;
                if (isQueen)
                    result += 20;
            }
            if (semi) {
                result += 10;
                if (isQueen)
                    result += 20;
            }
        }
        return result;
    }

    public static int isPassedPawn(boolean[] whiteCols, boolean[] blackCols, Pawn[] whitePawns, Pawn[] blackPawns, boolean color) {
        int result = 0;
        int bonus = 25;
        for (int i = 0; i < whiteCols.length; i++) {
            if (i == 0 && whiteCols[i]) {
                int row = (color) ? 7 - whitePawns[i].getRow() : whitePawns[i].getRow();
                if (!blackCols[i] && !blackCols[i + 1])
                    result += row * bonus;
                else if (passed(whitePawns[i], blackPawns))
                    result += row * bonus;
            } else if (i == 7 && whiteCols[i]) {
                int row = (color) ? 7 - whitePawns[i].getRow() : whitePawns[i].getRow();
                if (!blackCols[i] && !blackCols[i - 1])
                    result += row * bonus;
                else if (passed(whitePawns[i], blackPawns))
                    result += row * bonus;
            } else {
                if (whiteCols[i]) {
                    int row = (color) ? 7 - whitePawns[i].getRow() : whitePawns[i].getRow();
                    if (!blackCols[i] && !blackCols[i + 1] && !blackCols[i - 1])
                        result += row * bonus;
                    else if (passed(whitePawns[i], blackPawns))
                        result += row * bonus;
                }
            }
        }

        return result;
    }

    public static int isPawnConnected(boolean[] pawnLocations, Pawn[] pawns, boolean color) {
        int ph = phalanx(pawnLocations, pawns);
        int su = supported(pawnLocations, pawns, color);
        return ph + su;
    }

    private static int supported(boolean[] pawnLocations, Pawn[] pawns, boolean color) {
        int result = 0;
        int bonus = 15;
        if (color) {
            for (int i = 0; i < pawnLocations.length; i++) {
                if (pawnLocations[i]) {
                    if (i == 0 && pawnLocations[i + 1]) {
                        if (pawns[i + 1].getRow() == pawns[i].getRow() + 1)
                            result += bonus;
                    } else if (i == 7 && pawnLocations[i - 1]) {
                        if (pawns[i - 1].getRow() == pawns[i].getRow() + 1)
                            result += bonus;
                    } else if (i > 0 && i < 7) {
                        if (pawnLocations[i + 1]) {
                            if (pawns[i + 1].getRow() == pawns[i].getRow() + 1)
                                result += bonus;
                        }
                        if (pawnLocations[i - 1]) {
                            if (pawns[i - 1].getRow() == pawns[i].getRow() + 1)
                                result += bonus;
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < pawnLocations.length; i++) {
                if (pawnLocations[i]) {
                    if (i == 0 && pawnLocations[i + 1]) {
                        if (pawns[i + 1].getRow() == pawns[i].getRow() - 1)
                            result += bonus;
                    } else if (i == 7 && pawnLocations[i - 1]) {
                        if (pawns[i - 1].getRow() == pawns[i].getRow() - 1)
                            result += bonus;
                    } else if (i > 0 && i < 7) {
                        if (pawnLocations[i + 1]) {
                            if (pawns[i + 1].getRow() == pawns[i].getRow() - 1)
                                result += bonus;
                        }
                        if (pawnLocations[i - 1]) {
                            if (pawns[i - 1].getRow() == pawns[i].getRow() - 1)
                                result += bonus;
                        }
                    }
                }
            }
        }
        return result;
    }

    private static int phalanx(boolean[] pawnLocations, Pawn[] pawns) {
        int result = 0;
        int bonus = 7;
        for (int i = 0; i < pawnLocations.length; i++) {
            if (i == 0 && pawnLocations[i]) {
                if (pawnLocations[i + 1]) {
                    if (pawns[i + 1].getRow() == pawns[i].getRow())
                        result += bonus;
                }
            } else if (i == 7 && pawnLocations[i]) {
                if (pawnLocations[i - 1]) {
                    if (pawns[i - 1].getRow() == pawns[i].getRow())
                        result += bonus;
                }
            } else if (pawnLocations[i]) {
                boolean flag = false;
                if (pawnLocations[i + 1]) {
                    if (pawns[i + 1].getRow() == pawns[i].getRow())
                        result += bonus;
                    else
                        flag = true;
                } else
                    flag = true;
                if (pawnLocations[i - 1] && flag) {
                    if (pawns[i - 1].getRow() == pawns[i].getRow())
                        result += bonus;
                }
            }
        }
        return result;
    }

    public static int[] endGameDistanceCalc(King wk, King bk) {
        int center = 4;
        int distanceBetweenKings = Math.abs(wk.getRow() - bk.getRow()) + Math.abs(wk.getCol() - bk.getCol());
        int whiteDisFromCenter = Math.abs(wk.getRow() - center) + Math.abs(wk.getCol() - center);
        int blackDisFromCenter = Math.abs(bk.getRow() - center) + Math.abs(bk.getCol() - center);
        return new int[]{distanceBetweenKings, whiteDisFromCenter, blackDisFromCenter};
    }

    private static int pawnShield(King k, Spot[][] board) {
        int result = 0;
        if (k.isCastled) {
            if (k.getColor()) {
                if (k.getRow() == 7) {
                    boolean side = k.getCol() >= 4;
                    if (side) {
                        for (int col = 5; col < 8; col++) {
                            boolean empty = false;
                            for (int row = 6; row >= 5; row--) {
                                if (board[row][col].isEmpty() || (board[row][col].isFull() && board[row][col].getPiece().type != Type.PAWN)) {
                                    if (empty)
                                        break;
                                    result += 60;
                                    empty = true;
                                } else if (row == 6)
                                    break;
                                else {
                                    result -= 55;
                                }
                            }
                        }
                    } else {
                        for (int col = 0; col < 3; col++) {
                            boolean empty = false;
                            for (int row = 6; row >= 5; row--) {
                                if (board[row][col].isEmpty() || (board[row][col].isFull() && board[row][col].getPiece().type != Type.PAWN)) {
                                    if (empty)
                                        break;
                                    result += 60;
                                    empty = true;
                                } else if (row == 6)
                                    break;
                                else {
                                    result -= 55;
                                }
                            }
                        }
                    }
                }
            } else {
                if (k.getRow() == 0) {
                    boolean side = k.getCol() >= 4;
                    if (side) {
                        for (int col = 5; col < 8; col++) {
                            boolean empty = false;
                            for (int row = 1; row <= 2; row++) {
                                if (board[row][col].isEmpty() || (board[row][col].isFull() && board[row][col].getPiece().type != Type.PAWN)) {
                                    if (empty)
                                        break;
                                    result += 60;
                                    empty = true;
                                } else if (row == 1)
                                    break;
                                else {
                                    result -= 55;
                                }
                            }
                        }
                    } else {
                        for (int col = 0; col < 3; col++) {
                            boolean empty = false;
                            for (int row = 1; row <= 2; row++) {
                                if (board[row][col].isEmpty() || (board[row][col].isFull() && board[row][col].getPiece().type != Type.PAWN)) {
                                    if (empty)
                                        break;
                                    result += 60;
                                    empty = true;
                                } else if (row == 1)
                                    break;
                                else {
                                    result -= 55;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static boolean kingZone(Integer[] pos, King k) {
        return Math.abs(k.getRow() - pos[0]) < 3 && Math.abs(k.getCol() - pos[1]) < 3;
    }

    private static int[] kingAttackers(King k) {
        HashMap<Type, Integer> pieceValueMap = new HashMap<>();
        pieceValueMap.put(Type.KNIGHT, 20);
        pieceValueMap.put(Type.BISHOP, 20);
        pieceValueMap.put(Type.ROOK, 40);
        pieceValueMap.put(Type.QUEEN, 80);
        int attackersCount = 0;
        int attackersWeight = 0;
        List<Piece> pieces = (!k.getColor()) ? GamePanel.whitePieces : GamePanel.blackPieces;
        for (Piece p : pieces) {
            if (p.type == Type.KING | p.type == Type.PAWN)
                continue;
            for (Integer[] move : p.possibleMoves()) {
                if (kingZone(move, k)) {
                    attackersCount++;
                    attackersWeight += pieceValueMap.get(p.type);
                    break;
                }
            }
        }
        return new int[]{attackersCount, attackersWeight};
    }

    private static int kingSafety(King k) {
        int[] attackWeight = {0, 50, 75, 88, 94, 97, 99, 101, 101, 101, 101, 101, 101, 101, 101, 101};
        int[] attackers = kingAttackers(k);
        int attackersCount = attackers[0];
        int attackersWeight = attackers[1];
        return attackersWeight * attackWeight[attackersCount] / 50;
    }


    public static int evaluation(Spot[][] board, boolean turn) {
        int blackPoints = 0;
        int whitePoints = 0;
        boolean endGame = isEndGame();
        King wk = null;
        King bk = null;
        int wpawnCounter = 0;
        int wknightCounter = 0;
        int wrookCounter = 0;
        int wbishopCounter = 0;
        int whitePieceCounter = 0;
        Pawn[] whitePawns = new Pawn[8];
        boolean[] whitePawnsLocations = new boolean[8];
        int[] whiteRooksLocations = {-1, -1};
        for (Piece p : GamePanel.whitePieces) {
            whitePoints += p.evalPiece(false, endGame);
            if (p.type == Type.KING)
                wk = (King) p;
            if (p.type == Type.KNIGHT) {
                wknightCounter++;
                whitePieceCounter++;
            }
            if (p.type == Type.BISHOP) {
                wbishopCounter++;
                whitePieceCounter++;
            }
            if (p.type == Type.QUEEN) {
                whitePieceCounter++;
                whitePoints -= 10 * countOnes(p.getMoves());
                if (p.isMoved && GamePanel.fullMoves < 10)
                    whitePoints -= 40;
            }
            if (p.type == Type.ROOK) {
                whitePieceCounter++;
                whiteRooksLocations[wrookCounter] = p.getCol();
                wrookCounter++;
            }
            if (p.type == Type.PAWN) {
                whitePawns[p.getCol()] = (Pawn) p;
                wpawnCounter++;
                if (whitePawnsLocations[p.getCol()]) {
                    whitePoints -= 20;
                } else {
                    whitePawnsLocations[p.getCol()] = true;
                }
            }
        }
        int bpawnCounter = 0;
        int bknightCounter = 0;
        int brookCounter = 0;
        int bbishopCounter = 0;
        int blackPieceCounter = 0;
        Pawn[] blackPawns = new Pawn[8];
        boolean[] blackPawnsLocations = new boolean[8];
        int[] blackRooksLocations = {-1, -1};
        for (Piece p : GamePanel.blackPieces) {
            blackPoints += p.evalPiece(false, endGame);
            if (p.type == Type.KING)
                bk = (King) p;
            if (p.type == Type.KNIGHT) {
                bknightCounter++;
                blackPieceCounter++;
            }
            if (p.type == Type.BISHOP) {
                bbishopCounter++;
                blackPieceCounter++;
            }
            if (p.type == Type.QUEEN) {
                blackPieceCounter++;
                blackPoints -= 10 * countOnes(p.getMoves());
                if (p.isMoved && GamePanel.fullMoves < 10)
                    blackPoints -= 40;
            }
            if (p.type == Type.ROOK) {
                blackPieceCounter++;
                blackRooksLocations[brookCounter] = p.getCol();
                brookCounter++;
            }
            if (p.type == Type.PAWN) {
                blackPawns[p.getCol()] = (Pawn) p;
                bpawnCounter++;
                if (blackPawnsLocations[p.getCol()]) {
                    blackPoints -= 20;
                } else {
                    blackPawnsLocations[p.getCol()] = true;
                }
            }
        }
        if (wbishopCounter == 2)
            whitePoints += 40;
        if (bbishopCounter == 2)
            blackPoints += 40;
        blackPoints += brookCounter * (32 - (bpawnCounter + wpawnCounter) * 2);
        whitePoints += wrookCounter * (32 - (bpawnCounter + wpawnCounter) * 2);
        blackPoints -= bknightCounter * (32 - (bpawnCounter + wpawnCounter) * 2);
        whitePoints -= wknightCounter * (32 - (bpawnCounter + wpawnCounter) * 2);

        if (whitePieceCounter > blackPieceCounter) {
            blackPoints -= 28 - (blackPieceCounter + whitePieceCounter) * 2;
        } else if (whitePieceCounter < blackPieceCounter)
            whitePoints -= 28 - (blackPieceCounter + whitePieceCounter) * 2;

        if (isKingStuckInMiddle(wk) && !endGame)
            whitePoints -= 25;
        if (isKingStuckInMiddle(bk) && !endGame)
            blackPoints -= 25;
        if (!wk.isCastled) {
            whitePoints -= 20;
        }
        if (!bk.isCastled) {
            blackPoints -= 20;
        }
        whitePoints += whiteMobility * 10;
        blackPoints += blackMobility * 10;

        whitePoints -= 20 * isPawnsIsolated(whitePawnsLocations);
        blackPoints -= 20 * isPawnsIsolated(blackPawnsLocations);

        whitePoints += isPassedPawn(whitePawnsLocations, blackPawnsLocations, whitePawns, blackPawns, true);
        blackPoints += isPassedPawn(blackPawnsLocations, whitePawnsLocations, blackPawns, whitePawns, false);

        whitePoints += isPawnConnected(whitePawnsLocations, whitePawns, true);
        blackPoints += isPawnConnected(blackPawnsLocations, blackPawns, false);

        whitePoints += isFileOpen(whiteRooksLocations, board, true);
        blackPoints += isFileOpen(blackRooksLocations, board, false);

        whitePoints -= kingSafety(wk);
        blackPoints -= kingSafety(bk);

        whitePoints -= pawnShield(wk, board);
        blackPoints -= pawnShield(bk, board);

        if (turn) whitePoints += 30; else blackPoints += 30;



        int[] disCalc = endGameDistanceCalc(wk, bk);
        int distanceBetweenKings = disCalc[0];
        int whiteKingFromCenter = disCalc[1];
        int blackKingFromCenter = disCalc[2];
        if (endGame) {
            distanceBetweenKings = 200 / distanceBetweenKings;
            if (blackPoints > whitePoints) {
                blackPoints += distanceBetweenKings;
            }
            if (blackPoints < whitePoints) {
                whitePoints += distanceBetweenKings;
            }
            whitePoints += blackKingFromCenter * 20;
            blackPoints += whiteKingFromCenter * 20;
        }
        if (turn) {whitePoints += 20;} else {blackPoints += 20;};

        return blackPoints - whitePoints;
    }

    private static boolean revealed(King k) {
        long wp = pieceTables.get("truePAWN");
        long bp = pieceTables.get("falsePAWN");
        long l;
        if (k.getColor()) {
            l = (wp & Piece.FileMasks8[3]) | (wp & Piece.FileMasks8[4]);
        } else {
            l = (bp & Piece.FileMasks8[3]) | (bp & Piece.FileMasks8[4]);
        }
        return l == 0L;
    }


    public static Integer[] minimax(Spot[][] board, int depth, int alpha, int beta, boolean color) {
        countMoves++;
        long t = SECONDS.between(GamePanel.algStartTime, LocalTime.now());
        if (GamePanel.moveTime == GamePanel.timeBound) {
            if (t >= GamePanel.moveTime)
                return new Integer[]{0, null, null, null, null};
        }
        else {
            if (t >= GamePanel.moveTime && GamePanel.depth > GamePanel.minDepth || t >= GamePanel.timeBound)
                return new Integer[]{0, null, null, null, null};
        }
        if (GamePanel.isTimeOver(color))
            return new Integer[]{0, null, null, null, null};
        if (threefoldRepetition(color))
            return new Integer[]{0, null, null, null, null};
        if (depth == 0)
//	    	return new Integer[] {evaluation(board), null, null, null, null};
            return new Integer[]{searchCaptures(board, alpha, beta, color), null, null, null, null};
        List<Integer[]> sortedMoves = moveOrder(color, depth);
        if (sortedMoves.isEmpty()) {
            boolean check = isInCheck(color);
            if (check) {
                int max = (color) ? Integer.MAX_VALUE - 1 : Integer.MIN_VALUE + 1;
                mateAtDepth = depth;
                if (lastMateAtDepth == -1)
                    lastMateAtDepth = depth;
                return new Integer[]{max, null, null, null, null};
            } else
                return new Integer[]{0, null, null, null, null};

        }
        long transposition = Zobrist.getZobristHash(color, GamePanel.wcks, GamePanel.wcqs, GamePanel.bcks, GamePanel.bcqs);
        int freq = Collections.frequency(GamePanel.positions, transposition);
        boolean toAdd = false;
        if (GamePanel.transpositionTable.containsKey(transposition)) {
            Integer[] res = GamePanel.transpositionTable.get(transposition);
            if (res[5] >= depth && res[6] == freq) {
                return new Integer[]{res[0], res[1], res[2], res[3], res[4]};
            }
        } else
            toAdd = true;
        Integer[] first = sortedMoves.get(0);
        Piece bestP = GamePanel.board[first[0]][first[1]].getPiece();
        int[] bestM = new int[]{first[2], first[3]};
        int maxEval = Integer.MIN_VALUE;
        int minEval = Integer.MAX_VALUE;
        for (Integer[] sortedMove : sortedMoves) {
            Piece p = GamePanel.board[sortedMove[0]][sortedMove[1]].getPiece();
            int[] lastPos = {p.getRow(), p.getCol()};
            int[] move = new int[]{sortedMove[2], sortedMove[3]};
            Piece toRevive = null;
            Piece rook = null;
            int[] lastRookPos = null;
            Piece queen = null;

            // un capture
            if (board[move[0]][move[1]].isFull()) {
                toRevive = board[move[0]][move[1]].getPiece();
            }
            // un en passant
            boolean isPawnPassant = false;
            if (p.type == Type.PAWN) {
                if (board[move[0]][move[1]].isEmpty() && move[1] != p.col) {
                    if (p.color) {
                        toRevive = board[move[0] + 1][move[1]].getPiece();
                    } else {
                        toRevive = board[move[0] - 1][move[1]].getPiece();
                    }
                }
                if (Math.abs(p.getRow() - move[0]) == 2) {
                    isPawnPassant = true;
                    if (color) {
                        GamePanel.enPassant[0] = move[0] + 1;
                    } else {
                        GamePanel.enPassant[0] = move[0] - 1;
                    }
                    GamePanel.enPassant[1] = move[1];
                }
            }
            if (!isPawnPassant) {
                GamePanel.enPassant[0] = 0;
                GamePanel.enPassant[1] = 0;
            }
            // un castle
            if (p.type == Type.KING) {
                int[] m = {p.row, p.col};
                if (move[0] == m[0] && move[1] == m[1] + 2) {
                    rook = board[m[0]][m[1] + 3].getPiece();
                    lastRookPos = new int[2];
                    lastRookPos[0] = rook.getRow();
                    lastRookPos[1] = rook.getCol();

                }
                if (move[0] == m[0] && move[1] == m[1] - 2) {
                    rook = board[m[0]][m[1] - 4].getPiece();
                    lastRookPos = new int[2];
                    lastRookPos[0] = rook.getRow();
                    lastRookPos[1] = rook.getCol();
                }
            }
            p.move(board, move[0], move[1], false);

            if (p.type == Type.PAWN) {
                // un promote
                if (p.color) {
                    if (move[0] == 0) {
                        queen = board[p.row][p.col].getPiece();
                    }
                } else {
                    if (move[0] == 7) {
                        queen = board[p.row][p.col].getPiece();
                    }
                }
            }
            int evaluate = minimax(board, depth - 1, alpha, beta, !color)[0];

            if (rook != null) {
                rook.lastPos = lastRookPos;
            }
            p.undoMove(board, rook, toRevive, queen, lastPos, color);
            GamePanel.isInCheck = GamePanel.tmpIsInCheck;
            if (!color) {
                if (evaluate == maxEval && evaluate == Integer.MAX_VALUE - 1 && mateAtDepth > lastMateAtDepth) {
                    bestP = p;
                    bestM = move;
                    lastMateAtDepth = mateAtDepth;
                }
                if (evaluate > maxEval) {
                    maxEval = evaluate;
                    bestP = p;
                    bestM = move;
                    int[] tmp = {p.getRow(), p.getCol(), move[0], move[1]};
                    tempPV[depth-1] = tmp;
                    if (depth == GamePanel.depth){
                        PV = Arrays.copyOf(tempPV, tempPV.length);
                    }
                }
                alpha = Math.max(alpha, evaluate);
            } else {
                if (evaluate == minEval && evaluate == Integer.MIN_VALUE + 1 && mateAtDepth > lastMateAtDepth) {
                    bestP = p;
                    bestM = move;
                    lastMateAtDepth = mateAtDepth;
                }
                if (evaluate < minEval) {
                    minEval = evaluate;
                    bestP = p;
                    bestM = move;
                    int[] tmp = {p.getRow(), p.getCol(), move[0], move[1]};
                    tempPV[depth-1] = tmp;
                    if (depth == GamePanel.depth) {
                        PV = Arrays.copyOf(tempPV, tempPV.length);
                    }
                }
                beta = Math.min(beta, evaluate);
            }
            if (beta <= alpha)
                break;
        }
        int eval = (color) ? minEval : maxEval;
        if (toAdd) {
            freq = Collections.frequency(GamePanel.positions, transposition);
            GamePanel.transpositionTable.put(transposition, new Integer[]{eval, bestP.getRow(), bestP.getCol(), bestM[0], bestM[1], depth, freq});
        }
        if (bestP.type != Type.PAWN || !GamePanel.board[bestM[0]][bestM[1]].isFull())
            if (GamePanel.halfMoves + 1 == 50)
                return new Integer[]{0, bestP.getRow(), bestP.getCol(), bestM[0], bestM[1]};
        return new Integer[]{eval, bestP.getRow(), bestP.getCol(), bestM[0], bestM[1]};
    }

    public static int searchCaptures(Spot[][] board, int alpha, int beta, boolean color) {
        countMoves++;
//		if (SECONDS.between(GamePanel.algStartTime, LocalTime.now()) >= GamePanel.moveTime)
//			return 0;
        if (GamePanel.isTimeOver(color))
            return 0;
        if (threefoldRepetition(color))
            return 0;
        List<Integer[]> sortedMoves = moveOrder(color, 0);
        if (sortedMoves.isEmpty()) {
            boolean check = isInCheck(color);
            if (check) {
                return (color) ? Integer.MAX_VALUE - 1 : Integer.MIN_VALUE + 1;
            } else
                return 0;

        }
        List<Integer[]> captureMoves = allCaptureMoves(color);
        int maxEval = Integer.MIN_VALUE;
        int minEval = Integer.MAX_VALUE;
        int evaluate = evaluation(board, color);
        if (!color) {
            if (evaluate > maxEval) {
                maxEval = evaluate;
            }
            alpha = Math.max(alpha, evaluate);
            if (beta <= alpha)
                return alpha;
        } else {
            if (evaluate < minEval) {
                minEval = evaluate;
            }
            beta = Math.min(beta, evaluate);
            if (beta <= alpha)
                return beta;
        }
        for (Integer[] sortedMove : captureMoves) {
            Piece p = GamePanel.board[sortedMove[0]][sortedMove[1]].getPiece();
            int[] move = new int[]{sortedMove[2], sortedMove[3]};
            int[] lastPos = {p.getRow(), p.getCol()};
            Piece toRevive = null;
            Piece rook = null;
            int[] lastRookPos = null;
            Piece queen = null;
            // un capture
            if (board[move[0]][move[1]].isFull()) {
                toRevive = board[move[0]][move[1]].getPiece();
            }
            // un en passant
            boolean isPawnPassant = false;
            if (p.type == Type.PAWN) {
                if (board[move[0]][move[1]].isEmpty() && move[1] != p.col) {
                    if (p.color)
                        toRevive = board[move[0] + 1][move[1]].getPiece();
                    else
                        toRevive = board[move[0] - 1][move[1]].getPiece();
                }
                if (Math.abs(p.getRow() - move[0]) == 2) {
                    isPawnPassant = true;
                    if (color) {
                        GamePanel.enPassant[0] = move[0] + 1;
                    } else {
                        GamePanel.enPassant[0] = move[0] - 1;
                    }
                    GamePanel.enPassant[1] = move[1];
                }
            }
            if (!isPawnPassant) {
                GamePanel.enPassant[0] = 0;
                GamePanel.enPassant[1] = 0;
            }
            // un castle
            if (p.type == Type.KING) {
                int[] m = {p.row, p.col};
                if (move[0] == m[0] && move[1] == m[1] + 2) {
                    rook = board[m[0]][m[1] + 3].getPiece();
                    lastRookPos = new int[2];
                    lastRookPos[0] = rook.getRow();
                    lastRookPos[1] = rook.getCol();

                }
                if (move[0] == m[0] && move[1] == m[1] - 2) {
                    rook = board[m[0]][m[1] - 4].getPiece();
                    lastRookPos = new int[2];
                    lastRookPos[0] = rook.getRow();
                    lastRookPos[1] = rook.getCol();
                }
            }
            p.move(board, move[0], move[1], false);

            if (p.type == Type.PAWN) {
                // un promote
                if (p.color) {
                    if (move[0] == 0) {
                        queen = board[p.row][p.col].getPiece();
                    }
                } else {
                    if (move[0] == 7) {
                        queen = board[p.row][p.col].getPiece();
                    }
                }
            }
            evaluate = searchCaptures(board, alpha, beta, !color);

            if (rook != null) {
                rook.lastPos = lastRookPos;
            }
            p.undoMove(board, rook, toRevive, queen, lastPos, color);
            GamePanel.isInCheck = GamePanel.tmpIsInCheck;
            if (!color) {
                if (evaluate > maxEval) {
                    maxEval = evaluate;
                }
                alpha = Math.max(alpha, evaluate);
            } else {
                if (evaluate < minEval) {
                    minEval = evaluate;
                }
                beta = Math.min(beta, evaluate);
            }
            if (beta <= alpha)
                break;
        }

        return (!color) ? maxEval : minEval;
    }

    private static List<Integer[]> allCaptureMoves(boolean color) {
        countMoves++;
        List<Integer[]> captureMoves = new ArrayList<>();
        List<Piece> pieces = (color) ? GamePanel.whitePieces : GamePanel.blackPieces;
        for (Piece p : pieces) {
            long moves = p.getMoves() & Piece.ENEMY_PIECES;
            long j = moves & -moves;
            while (j != 0) {
                int index = Long.numberOfTrailingZeros(j);
                captureMoves.add(new Integer[]{p.getRow(), p.getCol(), index / 8, index % 8});
                moves &= ~j;
                j = moves & -moves;
            }
        }
//        for (Integer[] sortedMove : moves) {
//            Piece p = GamePanel.board[sortedMove[0]][sortedMove[1]].getPiece();
//            Integer[] move = new Integer[]{sortedMove[2], sortedMove[3]};
//            if (board[move[0]][move[1]].isFull()) {
//                captureMoves.add(sortedMove);
//                continue;
//            }
//            int[] lastPos = {p.getRow(), p.getCol()};
//            Piece toRevive = null;
//            Piece rook = null;
//            int[] lastRookPos = null;
//            Piece queen = null;
//            // un capture
//            if (board[move[0]][move[1]].isFull()) {
//                toRevive = board[move[0]][move[1]].getPiece();
//            }
//            // un en passant
//            boolean isPawnPassant = false;
//            if (p.type == Type.PAWN) {
//                if (board[move[0]][move[1]].isEmpty() && move[1] != p.col) {
//                    if (p.color) {
//                        toRevive = board[move[0] + 1][move[1]].getPiece();
//                    } else {
//                        toRevive = board[move[0] - 1][move[1]].getPiece();
//                    }
//                }
//                if (Math.abs(p.getRow() - move[0]) == 2) {
//                    isPawnPassant = true;
//                    if (color) {
//                        GamePanel.enPassant[0] = move[0] + 1;
//                    } else {
//                        GamePanel.enPassant[0] = move[0] - 1;
//                    }
//                    GamePanel.enPassant[1] = move[1];
//                }
//            }
//            if (!isPawnPassant) {
//                GamePanel.enPassant[0] = 0;
//                GamePanel.enPassant[1] = 0;
//            }
//            // un castle
//            if (p.type == Type.KING) {
//                int[] m = {p.row, p.col};
//                if (move[0] == m[0] && move[1] == m[1] + 2) {
//                    rook = board[m[0]][m[1] + 3].getPiece();
//                    lastRookPos = new int[2];
//                    lastRookPos[0] = rook.getRow();
//                    lastRookPos[1] = rook.getCol();
//
//                }
//                if (move[0] == m[0] && move[1] == m[1] - 2) {
//                    rook = board[m[0]][m[1] - 4].getPiece();
//                    lastRookPos = new int[2];
//                    lastRookPos[0] = rook.getRow();
//                    lastRookPos[1] = rook.getCol();
//                }
//            }
//            p.move(board, move[0], move[1], false);
//
//            if (isInCheck(!color)) {
//                captureMoves.add(sortedMove);
//            }
//            if (p.type == Type.PAWN) {
//                // un promote
//                if (p.color) {
//                    if (move[0] == 0) {
//                        queen = board[p.row][p.col].getPiece();
//                    }
//                } else {
//                    if (move[0] == 7) {
//                        queen = board[p.row][p.col].getPiece();
//                    }
//                }
//            }
//
//            if (rook != null) {
//                rook.lastPos = lastRookPos;
//            }
//            p.undoMove(board, rook, toRevive, queen, lastPos, color);
//        }
        return captureMoves;
    }

    public static boolean threefoldRepetition(boolean color) {
        long currPos = Zobrist.getZobristHash(color, GamePanel.wcks, GamePanel.wcqs, GamePanel.bcks, GamePanel.bcqs);
        int occurrences = Collections.frequency(GamePanel.positions, currPos);
        return occurrences > 2;
    }


    public static boolean isStaleMate(boolean color) {
        if (threefoldRepetition(color)) return true;
        if (GamePanel.whitePieces.size() == 1 && GamePanel.blackPieces.size() == 1)
            return true;
        if (GamePanel.whitePieces.size() == 1 && GamePanel.blackPieces.size() == 2) {
            if (GamePanel.blackPieces.get(0).type == Type.KNIGHT || GamePanel.blackPieces.get(1).type == Type.KNIGHT)
                return true;
            if (GamePanel.blackPieces.get(0).type == Type.BISHOP || GamePanel.blackPieces.get(1).type == Type.BISHOP)
                return true;
        }
        if (GamePanel.blackPieces.size() == 1 && GamePanel.whitePieces.size() == 2) {
            if (GamePanel.whitePieces.get(0).type == Type.KNIGHT || GamePanel.whitePieces.get(1).type == Type.KNIGHT)
                return true;
            if (GamePanel.whitePieces.get(0).type == Type.BISHOP || GamePanel.whitePieces.get(1).type == Type.BISHOP)
                return true;
        }
        if (GamePanel.blackPieces.size() == 2 && GamePanel.whitePieces.size() == 2) {
            if (GamePanel.whitePieces.get(0).type == Type.KNIGHT || GamePanel.whitePieces.get(0).type == Type.BISHOP ||
                    GamePanel.whitePieces.get(1).type == Type.KNIGHT || GamePanel.whitePieces.get(1).type == Type.BISHOP) {

                return GamePanel.blackPieces.get(0).type == Type.KNIGHT || GamePanel.blackPieces.get(0).type == Type.BISHOP ||
                        GamePanel.blackPieces.get(1).type == Type.KNIGHT || GamePanel.blackPieces.get(1).type == Type.BISHOP;
            }
        }
        return false;
    }

    public static List<Integer[]> moveOrder(boolean color, int depth) {
        List<Integer[]> moves = getAllMoves(color);
        List<Integer[]> sortedMoves = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (Integer[] moveList : moves) {
            Integer[] pLoc = new Integer[]{moveList[0], moveList[1]};
            Piece p = GamePanel.board[pLoc[0]][pLoc[1]].getPiece();
            Integer[] move = new Integer[]{moveList[2], moveList[3]};
            int score = 0;
            Piece capture;
            if (depth -2 >= 0) {
                if (pLoc[0] == GamePanel.principalVariation[depth-2][0] && pLoc[1] == GamePanel.principalVariation[depth-2][1] &&
                        move[0] == GamePanel.principalVariation[depth-2][2] && move[1] == GamePanel.principalVariation[depth-2][3])
                    score += 3000;
            }

            if (GamePanel.board[move[0]][move[1]].isFull()) {
                capture = GamePanel.board[move[0]][move[1]].getPiece();
                score += 10 * capture.evalPiece(true, false) - p.evalPiece(true, false);
            }
            String value = Long.toBinaryString(GamePanel.threatMap);
            String zeros = "0000000000000000000000000000000000000000000000000000000000000000";
            value = zeros.substring(value.length()) + value;
            int index = 63 - (move[0] * 8 + move[1]);
            if (value.charAt(index) == '1') {
                score -= p.evalPiece(true, false);
            }
            if (p.type == Type.PAWN && p.getColor() && move[0] == 0) {
                score += p.evalPiece(true, false) * 9;
            }
            if (p.type == Type.PAWN && !p.getColor() && move[0] == 7) {
                score += p.evalPiece(true, false) * 9;
            }

            if (scores.size() == 0) {
                scores.add(score);
                sortedMoves.add(moveList);
                continue;
            }
            if (score <= scores.get(scores.size()-1)) {
                scores.add(scores.size(), score);
                sortedMoves.add(sortedMoves.size(), moveList);
                continue;
            }
            if (score >= scores.get(0)) {
                scores.add(0, score);
                sortedMoves.add(0, moveList);
                continue;
            }
            int l = 0, r = scores.size() - 1;
            while (l <= r) {
                int m = l + (r - l) / 2;
                // check if mid is the right place
                if (score >= scores.get(m) && score <= scores.get(m - 1)) {
                    scores.add(m, score);
                    sortedMoves.add(m, moveList);
                    break;
                }
                // if number at mid is too high, ignore left half
                if (score < scores.get(m))
                    l = m + 1;
                    // if number at mid is too low, ignore right half
                else
                    r = m - 1;
            }
//            for (int i = 0; i < scores.size(); i++) {
//                if (score < scores.get(i)) {
//                    if (i == scores.size() - 1) {
//                        scores.add(i, score);
//                        sortedMoves.add(i, moveList);
//                        break;
//                    }
//                }
//                else {
//                    scores.add(i, score);
//                    sortedMoves.add(i, moveList);
//                    break;
//                }
//            }
        }
        return sortedMoves;
    }

    public static List<Integer[]> getAllMoves(boolean color) {
//		List<List<Object>> allMoves = new ArrayList<>();
        List<Integer[]> allMoves = new ArrayList<>();
        updateBoards(color);
        GamePanel.tmpIsInCheck = GamePanel.isInCheck;
        GamePanel.isInCheck = isInCheck(color);
        updateThreats(!color);
        if (color) {
            for (int i = 0; i < GamePanel.whitePieces.size(); i++) {
                Piece p = GamePanel.whitePieces.get(i);
                for (Integer[] move : p.possibleMoves()) {
                    Integer[] m = new Integer[4];
                    m[0] = p.getRow();
                    m[1] = p.getCol();
                    m[2] = move[0];
                    m[3] = move[1];
                    allMoves.add(m);
                }
            }
        } else {
            for (int i = 0; i < GamePanel.blackPieces.size(); i++) {
                Piece p = GamePanel.blackPieces.get(i);
                for (Integer[] move : p.possibleMoves()) {
                    Integer[] m = new Integer[4];
                    m[0] = p.getRow();
                    m[1] = p.getCol();
                    m[2] = move[0];
                    m[3] = move[1];
                    allMoves.add(m);
                }
            }
        }
        return allMoves;
    }

    public static void unPinAll() {
        for (Piece p : GamePanel.whitePieces) {
            p.isPinned = false;
            p.pinnedMoves = 0L;
        }
        for (Piece p : GamePanel.blackPieces) {
            p.isPinned = false;
            p.pinnedMoves = 0L;
        }

    }

    public static boolean isInCheck(boolean color) {
        unPinAll();
        pushMap = 0L;
        captureMap = 0L;
        King k = null;
        if (color) {
            for (Piece p : GamePanel.whitePieces) {
                if (p.type == Type.KING) {
                    k = (King) p;
                    break;
                }
            }
        } else {
            for (Piece p : GamePanel.blackPieces) {
                if (p.type == Type.KING) {
                    k = (King) p;
                    break;
                }
            }
        }
        List<Piece> attackers = new ArrayList<>();
        long tmpList = 0L;
        Spot[][] board = GamePanel.board;
        Piece pinned = null;
        boolean attFound = false;
        int counter = 0;
        long remove = 0L;
        // slider moves
        //Check right
        assert k != null;
        for (int i = k.getCol() + 1; i < 8; i++) {
            if (board[k.getRow()][i].isFull() && board[k.getRow()][i].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[k.getRow()][i].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[k.getRow()][i].isEmpty()) {
                long add = (1L << (k.getRow() * 8 + i));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[k.getRow()][i].getPiece().getColor() != k.getColor()) {
                if ((board[k.getRow()][i].getPiece().type == Type.ROOK || board[k.getRow()][i].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (k.getRow() * 8 + i));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[k.getRow()][i].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        //Check left
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getCol() - 1; i >= 0; i--) {
            if (board[k.getRow()][i].isFull() && board[k.getRow()][i].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[k.getRow()][i].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[k.getRow()][i].getPiece() == null) {
                long add = (1L << (k.getRow() * 8 + i));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[k.getRow()][i].getPiece().getColor() != k.getColor()) {
                long add = (1L << (k.getRow() * 8 + i));
                if ((board[k.getRow()][i].getPiece().type == Type.ROOK || board[k.getRow()][i].getPiece().type == Type.QUEEN)) {
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[k.getRow()][i].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;

        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check down
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() + 1; i < 8; i++) {
            if (board[i][k.getCol()].isFull() && board[i][k.getCol()].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][k.getCol()].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][k.getCol()].getPiece() == null) {
                long add = (1L << (i * 8 + k.getCol()));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][k.getCol()].getPiece().getColor() != k.getColor()) {
                if ((board[i][k.getCol()].getPiece().type == Type.ROOK || board[i][k.getCol()].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + k.getCol()));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][k.getCol()].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;

        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check up
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() - 1; i >= 0; i--) {
            if (board[i][k.getCol()].isFull() && board[i][k.getCol()].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][k.getCol()].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][k.getCol()].getPiece() == null) {
                long add = (1L << (i * 8 + k.getCol()));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][k.getCol()].getPiece().getColor() != k.getColor()) {
                if ((board[i][k.getCol()].getPiece().type == Type.ROOK || board[i][k.getCol()].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + k.getCol()));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][k.getCol()].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        //down right
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() + 1, j = k.getCol() + 1; i < 8 && j < 8; i++, j++) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //up left
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() - 1, j = k.getCol() - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //up right
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() - 1, j = k.getCol() + 1; i >= 0 && j < 8; i--, j++) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //down left
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() + 1, j = k.getCol() - 1; i < 8 && j >= 0; i++, j--) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        // knight moves
        // Check up
        if (k.getRow() - 2 >= 0) {
            if (k.getCol() - 1 >= 0)
                if (board[k.getRow() - 2][k.getCol() - 1].getPiece() != null && board[k.getRow() - 2][k.getCol() - 1].getPiece().color != k.getColor() && board[k.getRow() - 2][k.getCol() - 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 2][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() - 2) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getCol() + 1 < 8)
                if (board[k.getRow() - 2][k.getCol() + 1].getPiece() != null && board[k.getRow() - 2][k.getCol() + 1].getPiece().color != k.getColor() && board[k.getRow() - 2][k.getCol() + 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 2][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() - 2) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check down
        if (k.getRow() + 2 < 8) {
            if (k.getCol() - 1 >= 0)
                if (board[k.getRow() + 2][k.getCol() - 1].getPiece() != null && board[k.getRow() + 2][k.getCol() - 1].getPiece().color != k.getColor() && board[k.getRow() + 2][k.getCol() - 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 2][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() + 2) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getCol() + 1 < 8)
                if (board[k.getRow() + 2][k.getCol() + 1].getPiece() != null && board[k.getRow() + 2][k.getCol() + 1].getPiece().color != k.getColor() && board[k.getRow() + 2][k.getCol() + 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 2][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() + 2) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check right
        if (k.getCol() + 2 < 8) {
            if (k.getRow() - 1 >= 0)
                if (board[k.getRow() - 1][k.getCol() + 2].getPiece() != null && board[k.getRow() - 1][k.getCol() + 2].getPiece().color != k.getColor() && board[k.getRow() - 1][k.getCol() + 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 1][k.getCol() + 2].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() + 2));
                    captureMap |= add;
                }
            if (k.getRow() + 1 < 8)
                if (board[k.getRow() + 1][k.getCol() + 2].getPiece() != null && board[k.getRow() + 1][k.getCol() + 2].getPiece().color != k.getColor() && board[k.getRow() + 1][k.getCol() + 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 1][k.getCol() + 2].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() + 2));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check left
        if (k.getCol() - 2 >= 0) {
            if (k.getRow() - 1 >= 0)
                if (board[k.getRow() - 1][k.getCol() - 2].getPiece() != null && board[k.getRow() - 1][k.getCol() - 2].getPiece().color != k.getColor() && board[k.getRow() - 1][k.getCol() - 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 1][k.getCol() - 2].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() - 2));
                    captureMap |= add;
                }
            if (k.getRow() + 1 < 8)
                if (board[k.getRow() + 1][k.getCol() - 2].getPiece() != null && board[k.getRow() + 1][k.getCol() - 2].getPiece().color != k.getColor() && board[k.getRow() + 1][k.getCol() - 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 1][k.getCol() - 2].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() - 2));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        // pawn moves
        if (k.getColor()) {
            if (k.getRow() - 1 >= 0 && k.getCol() - 1 >= 0)
                if (board[k.getRow() - 1][k.getCol() - 1].isFull() && board[k.getRow() - 1][k.getCol() - 1].getPiece().getColor() != k.getColor() && board[k.getRow() - 1][k.getCol() - 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() - 1][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getRow() - 1 >= 0 && k.getCol() + 1 < 8)
                if (board[k.getRow() - 1][k.getCol() + 1].isFull() && board[k.getRow() - 1][k.getCol() + 1].getPiece().getColor() != k.getColor() && board[k.getRow() - 1][k.getCol() + 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() - 1][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        } else {
            if (k.getRow() + 1 < 8 && k.getCol() - 1 >= 0)
                if (board[k.getRow() + 1][k.getCol() - 1].isFull() && board[k.getRow() + 1][k.getCol() - 1].getPiece().getColor() != k.getColor() && board[k.getRow() + 1][k.getCol() - 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() + 1][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getRow() + 1 < 8 && k.getCol() + 1 < 8)
                if (board[k.getRow() + 1][k.getCol() + 1].isFull() && board[k.getRow() + 1][k.getCol() + 1].getPiece().getColor() != k.getColor() && board[k.getRow() + 1][k.getCol() + 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() + 1][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        ChessGame.numOfAttackers = attackers.size();
        return ChessGame.numOfAttackers > 0;
    }

    private static int countOnes(Long l) {
        String map = Long.toBinaryString(l);
        int result = 0;
        for (int i = 0; i < map.length(); i++) {
            if (map.charAt(i) == '1')
                result++;
        }
        return result;
    }

    public static void updateThreats(boolean color) {
        if (color) {
            GamePanel.threatMap = Piece.unsafeForBlack();
            whiteMobility = countOnes(GamePanel.threatMap);
        } else {
            GamePanel.threatMap = Piece.unsafeForWhite();
            blackMobility = countOnes(GamePanel.threatMap);
        }
    }

    static void updateBoards(Boolean color) {
        long WP = pieceTables.get("truePAWN");
        long WN = pieceTables.get("trueKNIGHT");
        long WB = pieceTables.get("trueBISHOP");
        long WR = pieceTables.get("trueROOK");
        long WQ = pieceTables.get("trueQUEEN");
        long WK = pieceTables.get("trueKING");
        long BP = pieceTables.get("falsePAWN");
        long BN = pieceTables.get("falseKNIGHT");
        long BB = pieceTables.get("falseBISHOP");
        long BR = pieceTables.get("falseROOK");
        long BQ = pieceTables.get("falseQUEEN");
        long BK = pieceTables.get("falseKING");
        if (color) {
            Piece.NOT_MY_PIECES = ~(WP | WN | WB | WR | WQ | WK | BK);
            Piece.ENEMY_PIECES = BP | BN | BB | BR | BQ;
        } else {
            Piece.NOT_MY_PIECES = ~(BP | BN | BB | BR | BQ | BK | WK);
            Piece.ENEMY_PIECES = WP | WN | WB | WR | WQ;
        }
        Piece.OCCUPIED = WP | WN | WB | WR | WQ | WK | BP | BN | BB | BR | BQ | BK;
        Piece.EMPTY = ~Piece.OCCUPIED;
    }


    public static void main(String[] args) throws IOException {
        new GameFrame();
    }

}
