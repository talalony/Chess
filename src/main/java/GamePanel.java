import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;

import static java.time.temporal.ChronoUnit.SECONDS;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class GamePanel extends JPanel implements ActionListener, MouseListener {

    public static String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
//    	public static String startFen = "8/7k/7P/3P2P1/5K2/4B2r/8/8 w - - 0 8";
//public static String startFen = "4r1k1/1QP2pp1/p6p/P7/8/2r1p2P/4K1P1/8 w - - 0 8";
    public static String lastMoveFen = startFen;

    // display
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    static final int screenWidth = 600;
    static final int screenHeight = 600;
    int w = dim.width / 2 - screenWidth / 2;
    int h = dim.height / 2 - screenHeight / 2;
    static final int unitSize = 75;
    static final int delay = 1000 / 300; // 120 fps
    Color white = new Color(238, 238, 210);
    Color black = new Color(118, 150, 86);

    // computer algorithm vars
    static boolean isCompDone = true;
    static int moveTime = 2;
    static int timeBound = 10;
    static int minDepth = 5;
    static LocalTime algStartTime = null;
    static int[] lastCompBestMove = new int[4];
    static int depth = 0;
    static boolean computersColor = false;
    static int[][] principalVariation;
    public static boolean tableBase = true;

    // game vars
    public static int[] enPassant = new int[2];
    public static int[] lastEnPassant = new int[2];
    static boolean isGameEnded = false;
    static boolean turn;
    static boolean perspective = false;
    static boolean isInCheck = false;
    static boolean tmpIsInCheck = false;
    static boolean stalemate = false;
    static int halfMoves = 0;
    static int fullMoves = 0;
    static boolean wcks;
    static boolean wcqs;
    static boolean bcks;
    static boolean bcqs;

    // main window
    boolean inMainWindow;
    private int chooseTime = 5;
    boolean notAdded = true;

    // buttons
    static MyButton resignButton;
    static MyButton changePersButton;
    MyButton resignYesButton;
    MyButton resignNoButton;

    // animation
    JLabel movesBoard;
    JScrollPane scrollPane;
    public static int toScroll = 2;
    static JScrollBar vertical;
    boolean resignLabel = false;
    static boolean animation = false;
    static boolean dragging;
    static int vel = 0;
    static List<Piece> takenBlackPieces;
    static List<Piece> takenWhitePieces;
    static boolean drawGameOver = true;
    private static final int[] startArrow = new int[2];
    private static final List<Integer[]> redSquares = new ArrayList<>();
    public static List<Arrow> arrows = new ArrayList<>();

    // time management
    static int increment = 0;
    static int whiteTime = 0;
    static int blackTime = 0;
    static int whiteSeconds = 0;
    static int whiteMinutes = 0;
    static int blackSeconds = 0;
    static int blackMinutes = 0;
    Timer countDown;

    // operetion vars
    boolean running = false;
    public static Piece currPiece;
    public static Piece castlingRook;
    List<Integer[]> moves;
    public static Integer[] movedFrom;
    public static Integer[] movedTo;
    public static int[] clickedSpot = null;
    public static String chessMoveList;
    public static List<Long> positions = new ArrayList<>();
    static Hashtable<Long, Integer[]> transpositionTable = new Hashtable<>();
    static List<Piece> blackPieces = ChessGame.blackPieces;
    static List<Piece> whitePieces = ChessGame.whitePieces;
    static List<Piece> tblackPieces = ChessGame.blackPieces;
    static List<Piece> twhitePieces = ChessGame.whitePieces;
    public static Clip clip;
    static int first = 20;
    public static long threatMap = 0L;
    Timer timer;
    Random random;
    static Spot[][] board = positionFromFen(startFen);
    static List<Object[]> playerMoveList = new ArrayList<>();
    static int moveListCounter = 0;

    GamePanel(GameFrame frame) {
        random = new Random();
        if (!frame.fullScreen) {
            dim.width = 1280;
            dim.height = 720;
            w = dim.width / 2 - screenWidth / 2;
            h = dim.height / 2 - screenHeight / 2;
        }
        this.setPreferredSize(dim);
        this.setBackground(new Color(32, 32, 32));
        this.setFocusable(true);
        this.addKeyListener(new myKeyAdapter(frame));
        this.setLayout(null);
        addMouseListener(this);
        startGame();
    }

    public void startGame() {
        takenBlackPieces = new ArrayList<>();
        takenWhitePieces = new ArrayList<>();
        chessMoveList = "";
        movedFrom = null;
        movedTo = null;
        ChessGame.updateThreats(turn);
        ChessGame.updateThreats(!turn);
        isInCheck = ChessGame.isInCheck(turn);
        inMainWindow = true;
        ActionListener l = arg0 -> {
            if (!turn)
                blackTime--;
            else
                whiteTime--;
            whiteSeconds = whiteTime % 60;
            whiteMinutes = (whiteTime - whiteSeconds) / 60;
            blackSeconds = blackTime % 60;
            blackMinutes = (blackTime - blackSeconds) / 60;
        };
        countDown = new Timer(1000, l);
        timer = new Timer(delay, this);
        timer.start();
        Zobrist.fillArray();
        ChessGame.updateBoards(turn);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHints(rh);
        if (inMainWindow) {
            drawBoard(g);
            drawPositions(g);
            drawPieces(g);
            drawMainWindow(g);

        } else if (running) {
            handCursor();
            drawBoard(g);

            if (movedFrom != null && movedTo != null) {
                g.setColor(new Color(255, 254, 123, 180));
                if (!perspective) {
                    g.fillRect(movedFrom[1] * unitSize + w, movedFrom[0] * unitSize + h, unitSize, unitSize);
                    g.fillRect(movedTo[1] * unitSize + w, movedTo[0] * unitSize + h, unitSize, unitSize);
                } else {
                    g.fillRect((7 - movedFrom[1]) * unitSize + w, (7 - movedFrom[0]) * unitSize + h, unitSize, unitSize);
                    g.fillRect((7 - movedTo[1]) * unitSize + w, (7 - movedTo[0]) * unitSize + h, unitSize, unitSize);
                }
            }
            if (computersColor) {
                if (whiteMinutes < 1 && increment < 10) {
                    timeBound = moveTime;
                    if (whiteSeconds < 20 && increment == 0)
                        moveTime = timeBound = 1;
                } else
                    timeBound = 10;
            } else {
                if (blackMinutes < 1 && increment < 10) {
                    timeBound = moveTime;
                    if (blackSeconds < 20 && increment == 0)
                        moveTime = timeBound = 1;
                } else
                    timeBound = 10;
            }

            if (moves != null) {
                drawMoves(g, true);
            }

            drawPositions(g);

            drawMovesBoard();
            g.setColor(new Color(188, 20, 20, 200));
            for (Integer[] square : redSquares)
                g.fillRect(w + square[0] * unitSize, h + square[1] * unitSize, unitSize, unitSize);

            drawPieces(g);

            for (Arrow arrow : arrows)
                arrow.drawArrow(g, 15);

            if (animation) {
                animate(g);
            }

            if (dragging) {
                drawDraggedPiece(g);
            }
            if (resignLabel) {
                if (isGameEnded)
                    drawResignLabel(g, "Exit");
                else
                    drawResignLabel(g, "Resign");
            }

            if (!isGameEnded) {
                drawTimers(g);
                drawTakenPieces(g);
            }

            if (isGameEnded && !animation) {
                if (ChessGame.threefoldRepetition(turn)) {
                    isInCheck = false;
                }
                if (isInCheck) {
                    gameOver(g, this);
                } else {
                    tie(g, this);
                }
            }
            if (isTimeOver(turn)) {
                if (!isGameEnded) {
                    if (currPiece == null)
                        currPiece = whitePieces.get(0);
                    clip = currPiece.sound("chessStalemateSound.wav");
                    resignButton.setText("Exit");
                    if (clip != null) {
                        clip.setFramePosition(0);
                        clip.start();
                    }
                }
                isGameEnded = true;
                isInCheck = true;
                moves = null;
                gameOver(g, this);
            }
            if (halfMoves >= 50 && !animation) {
                tie(g, this);
            }
        }
    }

    public void drawBoard(Graphics g) {
        for (int i = 0; i < screenHeight / unitSize; i++) {
            for (int j = 0; j < screenHeight / unitSize; j++) {
                if ((i + j) % 2 == 0)
                    g.setColor(white);
                else
                    g.setColor(black);
                g.fillRect(i * unitSize + w, j * unitSize + h, unitSize, unitSize);
            }
        }
    }

    private void drawPieces(Graphics g) {
        if (!perspective) {
            for (Piece p : twhitePieces) {
                if (p.equals(currPiece) && (dragging || animation))
                    continue;
                if (p.equals(castlingRook) && animation) {
                    g.drawImage(p.getImg(), castlingRook.lastPos[1] * unitSize + 2 + w, castlingRook.lastPos[0] * unitSize + 2 + h, null);
                    continue;
                }
                g.drawImage(p.getImg(), p.getCol() * unitSize + 2 + w, p.getRow() * unitSize + 2 + h, null);
            }
            for (Piece p : tblackPieces) {
                if (p.equals(currPiece) && (dragging || animation))
                    continue;
                if (p.equals(castlingRook) && animation) {
                    g.drawImage(p.getImg(), castlingRook.lastPos[1] * unitSize + 2 + w, castlingRook.lastPos[0] * unitSize + 2 + h, null);
                    continue;
                }
                g.drawImage(p.getImg(), p.getCol() * unitSize + 2 + w, p.getRow() * unitSize + 2 + h, null);
            }
        } else {
            for (Piece p : twhitePieces) {
                if (p.equals(currPiece) && (dragging || animation))
                    continue;
                if (p.equals(castlingRook) && animation) {
                    g.drawImage(p.getImg(), (7 - castlingRook.lastPos[1]) * unitSize + 2 + w, (7 - castlingRook.lastPos[0]) * unitSize + 2 + h, null);
                    continue;
                }
                g.drawImage(p.getImg(), (7 - p.getCol()) * unitSize + 2 + w, (7 - p.getRow()) * unitSize + 2 + h, null);
            }
            for (Piece p : tblackPieces) {
                if (p.equals(currPiece) && (dragging || animation))
                    continue;
                if (p.equals(castlingRook) && animation) {
                    g.drawImage(p.getImg(), (7 - castlingRook.lastPos[1]) * unitSize + 2 + w, (7 - castlingRook.lastPos[0]) * unitSize + 2 + h, null);
                    continue;
                }
                g.drawImage(p.getImg(), (7 - p.getCol()) * unitSize + 2 + w, (7 - p.getRow()) * unitSize + 2 + h, null);
            }
        }
    }

    private void handCursor() {
        PointerInfo a = MouseInfo.getPointerInfo();
        Point b = a.getLocation();
        int globalX = (int) b.getX() - this.getLocationOnScreen().x;
        int globalY = (int) b.getY() - this.getLocationOnScreen().y;
        int y = (globalX - w) / unitSize;
        int x = (globalY - h) / unitSize;
        if (perspective) {
            y = 7 - (globalX - w) / unitSize;
            x = 7 - (globalY - h) / unitSize;
        }

        if (globalX < w || globalX > screenWidth + w - 1 || globalY < h || globalY > screenHeight + h - 1) {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        List<Piece> pieces = (computersColor) ? tblackPieces : twhitePieces;
        boolean hand = false;
        for (Piece p : pieces) {
            if (p.getRow() == x && p.getCol() == y) {
                hand = true;
                break;
            }
        }
        if (dragging || hand && !resignLabel && !isGameEnded) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public void drawPositions(Graphics2D g) {
        g.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        if (perspective) {
            String[] chars = {"h", "g", "f", "e", "d", "c", "b", "a"};
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0)
                    g.setColor(black);
                else
                    g.setColor(white);
                g.drawString("" + (i + 1), 5 + w, i * unitSize + 15 + h);
                if (i % 2 == 0)
                    g.setColor(white);
                else
                    g.setColor(black);
                g.drawString(chars[i], i * unitSize + 60 + w, 595 + h);

            }
        } else {
            String[] chars = {"a", "b", "c", "d", "e", "f", "g", "h"};
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0)
                    g.setColor(black);
                else
                    g.setColor(white);
                g.drawString("" + (8 - i), 5 + w, i * unitSize + 15 + h);
                if (i % 2 == 0)
                    g.setColor(white);
                else
                    g.setColor(black);
                g.drawString(chars[i], i * unitSize + 60 + w, 595 + h);

            }
        }
    }

    private void drawMoves(Graphics2D g, boolean draw) {
        if (!perspective) {
            g.setColor(new Color(255, 254, 123, 180));
            g.fillRect(currPiece.getCol() * unitSize + w, currPiece.getRow() * unitSize + h, unitSize, unitSize);
            if (draw) {
                for (Integer[] move : moves) {
                    g.setStroke(new BasicStroke(2));
                    g.setColor(new Color(50, 50, 50, 100));
                    if (!board[move[0]][move[1]].isFull())
                        g.fillOval(move[1] * unitSize + 25 + w, move[0] * unitSize + 25 + h, unitSize - 50, unitSize - 50);
                    else if (move[0] == enPassant[0] && move[1] == enPassant[1]) {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval(move[1] * unitSize + 3 + w, move[0] * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    } else {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval(move[1] * unitSize + 3 + w, move[0] * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    }
                }
            }
        } else {
            g.setColor(new Color(255, 254, 123, 180));
            g.fillRect((7 - currPiece.getCol()) * unitSize + w, (7 - currPiece.getRow()) * unitSize + h, unitSize, unitSize);
            if (draw) {
                for (Integer[] move : moves) {
                    g.setStroke(new BasicStroke(2));
                    g.setColor(new Color(50, 50, 50, 100));
                    if (!board[move[0]][move[1]].isFull())
                        g.fillOval((7 - move[1]) * unitSize + 25 + w, (7 - move[0]) * unitSize + 25 + h, unitSize - 50, unitSize - 50);
                    else if ((7 - move[0]) == enPassant[0] && (7 - move[1]) == enPassant[1]) {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval((7 - move[1]) * unitSize + 3 + w, (7 - move[0]) * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    } else {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval((7 - move[1]) * unitSize + 3 + w, (7 - move[0]) * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    }
                }
            }
        }
    }

    private void drawDraggedPiece(Graphics2D g) {
        if (isGameEnded) {
            dragging = false;
            return;
        }
        PointerInfo a = MouseInfo.getPointerInfo();
        Point b = a.getLocation();
        int globalX = (int) b.getX() - this.getLocationOnScreen().x;
        int globalY = (int) b.getY() - this.getLocationOnScreen().y;
        if (globalX < w + 20)
            globalX = w + 20;

        if (globalX > screenWidth - 20 + w)
            globalX = screenWidth - 20 + w;

        if (globalY < 20 + h)
            globalY = 20 + h;

        if (globalY > screenHeight - 25 + h)
            globalY = screenHeight - 25 + h;

        int x = (globalX - w) / unitSize;
        int y = (globalY - h) / unitSize;

        g.setColor(new Color(200, 200, 200, 200));
        g.setStroke(new BasicStroke(3));
        g.drawRect(w + x * unitSize, h + y * unitSize, unitSize, unitSize);
        g.drawImage(currPiece.getImg(), globalX - 30, globalY - 30, null);
    }

    private void drawTimers(Graphics2D g) {
        g.setFont(new Font("Arial Black", Font.PLAIN, 40));
        int w = dim.width / 2 + screenWidth / 2;
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D rect = fm.getStringBounds(String.format("%02d:%02d", blackMinutes, blackSeconds), g);
        int x = w + w / 15;
        int y = h / 2 + dim.height / 3;
        if (perspective)
            y = h / 2 + (dim.height / 3) * 2;

        g.setColor(Color.black);
        g.fillRect(x,
                y - fm.getAscent(),
                (int) rect.getWidth() + 20,
                (int) rect.getHeight());

        if (!turn) {
            g.setColor(Color.white);
            if (blackMinutes == 0 && blackSeconds < 30)
                g.setColor(Color.RED);
        } else
            g.setColor(Color.gray);

        g.drawString(String.format("%02d:%02d", blackMinutes, blackSeconds), x + 10, y);
        y = h / 2 + (dim.height / 3) * 2;
        if (perspective)
            y = h / 2 + dim.height / 3;
        g.setColor(Color.black);
        g.fillRect(x,
                y - fm.getAscent(),
                (int) rect.getWidth() + 20,
                (int) rect.getHeight());

        if (turn) {
            g.setColor(Color.white);
            if (whiteMinutes == 0 && whiteSeconds < 30)
                g.setColor(Color.RED);
        } else
            g.setColor(Color.gray);
        g.drawString(String.format("%02d:%02d", whiteMinutes, whiteSeconds), x + 10, y);
    }

    public void drawResignLabel(Graphics2D g, String text) {
        g.setColor(new Color(125, 125, 125, 60));
        g.fillRect(w, h, screenWidth, screenHeight);
        g.setColor(Color.white);
        g.fillRoundRect(150 + w, 175 + h, 300, 250, 30, 35);
        g.setColor(new Color(40, 40, 40));
        g.setFont(new Font("Times New Roman", Font.BOLD, 25));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Do You Want To " + text, (screenWidth - metrics.stringWidth("Do You Want To " + text)) / 2 + w, g.getFont().getSize() + 250);
//		g.drawString("It's a tie!", (screenWidth - metrics.stringWidth("It's a tie!"))/2+w, g.getFont().getSize() + 300);
    }

    private void drawMainWindow(Graphics2D g) {
        int height = (int) dim.getHeight();
        int width = (int) dim.getWidth();
        // choose piece color
        g.setColor(Color.white.darker());
        g.setFont(new Font("Arial Black", Font.PLAIN, 40));
        g.drawString("Play As", width / 15, height / 6);
        g.drawString("Board Color", width / 29, height / 2);
        Image whiteKing = null;
        Image blackKing = null;
        try {
            whiteKing = ImageIO.read(new File(System.getProperty("user.dir") + "\\images\\white_king.png"));
            whiteKing = whiteKing.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            blackKing = ImageIO.read(new File(System.getProperty("user.dir") + "\\images\\black_king.png"));
            blackKing = blackKing.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        g.setColor(new Color(45, 45, 45));
        g.fillRect(w - (w / 5) * 2, height / 5, 50, 50);
        g.fillRect(w - (w / 5) * 4, height / 5, 50, 50);
        g.drawImage(whiteKing, w - (w / 5) * 2 + 5, height / 5 + 3, 40, 40, null);
        g.drawImage(blackKing, w - (w / 5) * 4 + 5, height / 5 + 3, 40, 40, null);

        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(10, 160, 10));
        if (computersColor) {
            g.drawRect(w - (w / 5) * 4 - 3, height / 5 - 3, 55, 55);
        } else {
            g.drawRect(w - (w / 5) * 2 - 3, height / 5 - 3, 55, 55);
        }

        // choose board color
        if (white.equals(new Color(238, 238, 210))) {
            g.drawRect(w - (w / 10) * 9 - 3, height / 2 + height / 19 - 3, 105, 105);
        } else if (white.equals(new Color(240, 217, 181))) {
            g.drawRect(w - (w / 10) * 4 - 3, height / 2 + height / 19 - 3, 105, 105);
        } else if (white.equals(new Color(222, 227, 230))) {
            g.drawRect(w - (w / 10) * 9 - 3, height / 2 + height / 4 - 3, 105, 105);
        } else if (white.equals(new Color(220, 220, 220))) {
            g.drawRect(w - (w / 10) * 4 - 3, height / 2 + height / 4 - 3, 105, 105);
        }
        // green
        g.setColor(new Color(238, 238, 210));
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 19 + 50, 50, 50);
        g.setColor(new Color(118, 150, 86));
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 19 + 50, 50, 50);
        // brown
        g.setColor(new Color(240, 217, 181));
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 19 + 50, 50, 50);
        g.setColor(new Color(181, 136, 99));
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 19 + 50, 50, 50);
        // blue
        g.setColor(new Color(222, 227, 230));
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 4 + 50, 50, 50);
        g.setColor(new Color(140, 162, 173));
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 4 + 50, 50, 50);
        // grey
        g.setColor(new Color(220, 220, 220));
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 4 + 50, 50, 50);
        g.setColor(new Color(171, 171, 171));
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 4 + 50, 50, 50);

        // choose time control
        JSlider minSlider = new JSlider(1, 60, 5) {
            @Override
            public void updateUI() {
                setUI(new CustomSliderUI(this));
            }
        };
        JSlider incrementSlider = new JSlider(0, 60, 0) {
            @Override
            public void updateUI() {
                setUI(new CustomSliderUI(this));
            }
        };
        g.setColor(Color.white.darker());
        g.drawString("Time Control", width / 2 + width / 4, height / 2);
        minSlider.setBounds(width - width / 5, height / 2 + height / 19, 180, 20);
        minSlider.setBackground(new Color(32, 32, 32));
        g.setFont(new Font("Arial Black", Font.PLAIN, 20));
        ChangeListener l = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                chooseTime = minSlider.getValue();
                increment = incrementSlider.getValue();

            }
        };

        g.drawString(String.format("%02d : minutes", chooseTime), width - width / 5 + width / 50, height / 2 + height / 9);
        minSlider.addChangeListener(l);
        g.setFont(new Font("Arial Black", 0, 40));
        g.drawString("Increment", width - width / 4 + width / 40, height / 2 + height / 5);
        incrementSlider.setBounds(width - width / 5, height / 2 + height / 5 + height / 30, 180, 20);
        incrementSlider.setBackground(new Color(32, 32, 32));
        incrementSlider.setMajorTickSpacing(5);
        g.setFont(new Font("Arial Black", Font.PLAIN, 20));
        g.drawString(String.format("%02d : seconds", increment), width - width / 5 + width / 50, height / 2 + height / 4 + height / 20);
        incrementSlider.addChangeListener(l);
        JLabel play = new JLabel("Play", JLabel.CENTER);
        play.setForeground(Color.white.darker());
        play.setVerticalAlignment(JLabel.BOTTOM);
        play.setBounds(width - w / 2 - w / 4, height / 7, 165, 80);
        play.setBackground(new Color(45, 45, 45));
        play.setOpaque(true);
        play.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 3));
        play.setFont(new Font("Arial Black", Font.PLAIN, 60));
        if (notAdded) {
            this.add(play);
            this.add(minSlider);
            this.add(incrementSlider);
            notAdded = false;
        }
    }

    public void drawMovesBoard() {
        String[] arr = chessMoveList.split(" ");
        if (chessMoveList == "")
            arr = new String[0];
        String drawMoveList = "<html>";
        for (int i = 0; i < arr.length; i++) {
            if ((i + 1) % 2 == 1) {
                String space = "&nbsp;";
                String num = (i / 2 + 1)+"";
                String s = new String(new char[3-num.length()]).replace("\0", "*");
                String word = num + ":" + s + arr[i];
                space = new String(new char[12-word.length()]).replace("\0", space);
                word = word.replace("*", "&nbsp;");
                drawMoveList += "&nbsp;" + word + space;
            }
            else
                drawMoveList += arr[i] + "<br/>";
        }
        movesBoard.setText(drawMoveList+"</html>");
        if (toScroll > 0) {
            vertical.setValue(vertical.getMaximum());
            toScroll -= 1;
        }
    }

    private void drawTakenPieces(Graphics2D g) {
        int w = dim.width / 2 + screenWidth / 2;
        int x = w + w / 35;
        int y = dim.height/2 + h + h/5;
        if (perspective)
            y = dim.height/2 - h - h/5;
        int whitePoints = 0;
        Type lastType = Type.PAWN;
        for (Piece p : takenBlackPieces) {
            if (p == takenBlackPieces.get(0))
                lastType = p.type;
            if (p.type != lastType)
                x += 12;
            g.drawImage(p.getSmallImg(), x, y, null);
            x += 12;
            lastType = p.type;
            whitePoints += p.evalPiece(true, false) / 100;
        }
        int sumOfBlackX = x;
        x = w + w / 35;
        y = dim.height/2 - h - h/5;
        if (perspective)
            y = dim.height/2 + h + h/5;
        int blackPoints = 0;
        for (Piece p : takenWhitePieces) {
            if (p == takenWhitePieces.get(0))
                lastType = p.type;
            if (p.type != lastType)
                x += 12;
            g.drawImage(p.getSmallImg(), x, y, null);
            x += 12;
            lastType = p.type;
            blackPoints += p.evalPiece(true, false) / 100;
        }
        int sumOfWhiteX = x;
        g.setFont(new Font("Arial Black", Font.PLAIN, 18));
        g.setColor(Color.white);
        int points = whitePoints - blackPoints;
        if (points > 0) {
            x = sumOfBlackX + 20;
            y = dim.height/2 + h + h/5 + 14 + 8;
            if (perspective)
                y = dim.height/2 - h - h/5 + 14 + 8;
            g.drawString("+" + Math.abs(points), x, y);
        } else if (points < 0) {
            x = sumOfWhiteX + 20;
            y =dim.height/2 - h - h/5 + 14 + 8;
            if (perspective)
                y = dim.height/2 + h + h/5 + 14 + 8;
            g.drawString("+ " + Math.abs(points), x, y);
        }
    }

    public void gameOver(Graphics2D g, GamePanel panel) {
        if (!drawGameOver) return;
        g.setColor(new Color(125, 125, 125, 60));
        g.fillRect(w, h, screenWidth, screenHeight);
        g.setColor(Color.white);
        g.fillRoundRect(150 + w, 175 + h, 300, 250, 30, 35);
        g.setColor(new Color(40, 40, 40));
        g.setFont(new Font("Times New Roman", Font.BOLD, 30));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Game Over", (screenWidth - metrics.stringWidth("Game Over")) / 2 + w, g.getFont().getSize() + 250);
        String won = (!turn) ? "White" : "Black";
        if (won.equals("White") && !chessMoveList.contains("1-0")) {
            chessMoveList += "1-0";
            createYesNoButton("NO", false, w + 170, h + 375, panel, true);
            createYesNoButton("YES", true, w + 390, h + 375, panel, true);
            resignButton.setEnabled(false);
            changePersButton.setEnabled(false);
        } else if (won.equals("Black") && !chessMoveList.contains("0-1")) {
            chessMoveList += "0-1";
            createYesNoButton("NO", false, w + 170, h + 375, panel, true);
            createYesNoButton("YES", true, w + 390, h + 375, panel, true);
            resignButton.setEnabled(false);
            changePersButton.setEnabled(false);
        }
        g.drawString(won + " won", (screenWidth - metrics.stringWidth(won + " won")) / 2 + w, g.getFont().getSize() + 300);


    }

    public void tie(Graphics2D g, GamePanel panel) {
        if (!drawGameOver) return;
        if (!chessMoveList.contains("1/2-1/2")) {
            chessMoveList += "1/2-1/2";
            createYesNoButton("NO", false, w + 170, h + 375, panel, true);
            createYesNoButton("YES", true, w + 390, h + 375, panel, true);
            resignButton.setEnabled(false);
            changePersButton.setEnabled(false);
        }
        g.setColor(new Color(125, 125, 125, 60));
        g.fillRect(w, h, screenWidth, screenHeight);
        g.setColor(Color.white);
        g.fillRoundRect(150 + w, 175 + h, 300, 250, 30, 35);
        g.setColor(new Color(40, 40, 40));
        g.setFont(new Font("Times New Roman", Font.BOLD, 30));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Game Over", (screenWidth - metrics.stringWidth("Game Over")) / 2 + w, g.getFont().getSize() + 250);
        g.drawString("It's a tie!", (screenWidth - metrics.stringWidth("It's a tie!")) / 2 + w, g.getFont().getSize() + 300);

    }

    public static boolean isTimeOver(boolean color) {
        if (color) {
            return whiteMinutes <= 0 && whiteSeconds <= 0;
        } else {
            return blackMinutes <= 0 && blackSeconds <= 0;
        }
    }

    private void backGroundAnimate() {
        if (currPiece.type == Type.KING) {
            if (currPiece.getColor()) {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    castlingRook = board[7][3].getPiece();
                } else if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    castlingRook = board[7][5].getPiece();
                } else
                    castlingRook = null;
            } else {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    castlingRook = board[0][3].getPiece();
                } else if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    castlingRook = board[0][5].getPiece();
                } else
                    castlingRook = null;
            }
        } else
            castlingRook = null;
        int frameCount = 10;
        if (vel < frameCount)
            vel++;
        else {
            vel = 0;
            animation = false;
            if (currPiece.type == Type.KING) {
                if (currPiece.getColor()) {
                    if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                        currPiece = board[7][3].getPiece();
                        movedFrom = new Integer[]{7, 0};
                        movedTo = new Integer[]{7, 3};
                        animation = true;
                    }
                    if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                        currPiece = board[7][5].getPiece();
                        movedFrom = new Integer[]{7, 7};
                        movedTo = new Integer[]{7, 5};
                        animation = true;
                    }
                } else {
                    if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                        currPiece = board[0][3].getPiece();
                        movedFrom = new Integer[]{0, 0};
                        movedTo = new Integer[]{0, 3};
                        animation = true;
                    }
                    if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                        currPiece = board[0][5].getPiece();
                        movedFrom = new Integer[]{0, 7};
                        movedTo = new Integer[]{0, 5};
                        animation = true;
                    }
                }
            }
        }
    }

    public void draggingCastleAnimation() {
        if (currPiece.type == Type.KING) {
            if (currPiece.getColor()) {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    currPiece = board[7][3].getPiece();
                    movedFrom = new Integer[]{7, 0};
                    movedTo = new Integer[]{7, 3};
                    animation = true;
                }
                if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    currPiece = board[7][5].getPiece();
                    movedFrom = new Integer[]{7, 7};
                    movedTo = new Integer[]{7, 5};
                    animation = true;
                }
            } else {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    currPiece = board[0][3].getPiece();
                    movedFrom = new Integer[]{0, 0};
                    movedTo = new Integer[]{0, 3};
                    animation = true;
                }
                if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    currPiece = board[0][5].getPiece();
                    movedFrom = new Integer[]{0, 7};
                    movedTo = new Integer[]{0, 5};
                    animation = true;
                }
            }
        }
    }

    public void animate(Graphics2D g) {
        int dr;
        int dc;
        int anRow;
        int anCol;
        int frameCount = 10;
        if (perspective) {
            dr = (7 - movedTo[0]) * unitSize - (7 - movedFrom[0]) * unitSize;
            dc = (7 - movedTo[1]) * unitSize - (7 - movedFrom[1]) * unitSize;
            anCol = (7 - movedFrom[1]) * unitSize + (dc * vel / frameCount);
            anRow = (7 - movedFrom[0]) * unitSize + (dr * vel / frameCount);
        } else {
            dr = movedTo[0] * unitSize - movedFrom[0] * unitSize;
            dc = movedTo[1] * unitSize - movedFrom[1] * unitSize;
            anCol = movedFrom[1] * unitSize + (dc * vel / frameCount);
            anRow = movedFrom[0] * unitSize + (dr * vel / frameCount);
        }
        g.drawImage(currPiece.getImg(), anCol + 2 + w, anRow + 2 + h, null);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            if (stalemate) {
                if (!isGameEnded) {
                    if (!chessMoveList.contains("#")) {
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 2) + "# ";
                    }
                }
                isGameEnded = true;
                resignButton.setText("Exit");
            }
            if (halfMoves == 50) {
                if (!isGameEnded) {
                    if (!chessMoveList.contains("#")) {
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 2) + "# ";
                        vertical.setValue(vertical.getMaximum());
                    }
                }
                isGameEnded = true;
                resignButton.setText("Exit");
            }
        }

        if (animation) {
            backGroundAnimate();
        } else if (currPiece != null) {
            draggingCastleAnimation();
        }
        if ((!turn && !computersColor || turn && computersColor) && isCompDone && !animation) {
            computerMove();
        }
        repaint();
    }

    private void computerMove() {
        if (!running) return;
        if (chessMoveList.isEmpty() && first != 0) {
            first--;
            return;
        }
        int moveNum = Integer.parseInt(lastMoveFen.split(" ")[5]);
		if (moveNum == 1 && computersColor) {
			openingMoves("e4", computersColor);
			return;
		}
        List<String> moves = new ArrayList<>();
        if (moveNum < 7) {
            try {
                File myObj = new File(System.getProperty("user.dir") + "\\GMGames.txt");
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    if (chessMoveList.length() == 0) {
                        String m = data.split(" ")[0];
                        if (!moves.contains(m))
                            moves.add(m);
                        continue;
                    }
                    String m = data.split(" ")[chessMoveList.split(" ").length];
                    if (!moves.contains(m) && data.startsWith(chessMoveList.substring(0, chessMoveList.length() - 1))) {
                        moves.add(m);
                    }
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
            Random rand = new Random();
            if (!moves.isEmpty()) {
                String randomMove = moves.get(rand.nextInt(moves.size()));
                openingMoves(randomMove, computersColor);
            }
        }
        int compMin = (computersColor) ? whiteMinutes : blackMinutes;
        int compSec = (computersColor) ? whiteSeconds : blackSeconds;
        if (whitePieces.size()+blackPieces.size() <= 7 && !(compMin == 0 && compSec <= 30) && tableBase) {
            isCompDone = false;
            if (!isGameEnded)
                playTableBase();
        }
        else if (moves.isEmpty()) {
            isCompDone = false;
            if (!isGameEnded)
                startThinking();
        }
    }

    public void playTableBase() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                try {
                    runPython(lastMoveFen, computersColor);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void done() {
                isCompDone = true;
                ReadJson sample = new ReadJson();
                String[] res = new String[2];
                try {
                    res = sample.getJson();
                } catch (IOException | ParseException e) {
                    tableBase = false;
                    return;
                }
                if (res[0].equals("losing")) {
                    tableBase = false;
                    return;
                }
                String m = res[1].split("=")[0];
                try {
                    openingMoves(m, computersColor);
                } catch (RuntimeException e) {
                    tableBase = false;
                    return;
                }
                System.out.println(m +" "+res[0]);
            }
        };
        worker.execute();
    }

    public void startThinking() {
        SwingWorker<Integer[], Void> worker = new SwingWorker<Integer[], Void>() {

            @Override
            protected Integer[] doInBackground() {
                lastCompBestMove = new int[4];
                algStartTime = LocalTime.now();
                Integer[] result = new Integer[5];
                for (int depth = 1; depth < 100; depth++) {
                    ChessGame.PV = new int[depth][4];
                    ChessGame.tempPV = new int[depth][4];
                    ChessGame.bestMoveEval = (computersColor) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    GamePanel.depth = depth;
                    Integer[] arr = ChessGame.minimax(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, computersColor);
                    long t = SECONDS.between(algStartTime, LocalTime.now());
                    if (depth > minDepth && t >= moveTime || t >= timeBound) {
                        System.out.println("depth=" + (depth - 1) + ": " + ChessGame.countMoves);
                        return result;
                    }
                    if (isTimeOver(turn))
                        break;
                    else {
                        enPassant[0] = lastEnPassant[0];
                        enPassant[1] = lastEnPassant[1];
                        result[0] = arr[0];
                        result[1] = arr[1];
                        result[2] = arr[2];
                        result[3] = arr[3];
                        result[4] = arr[4];
                        principalVariation = ChessGame.PV;
                        if (result[0] == Integer.MAX_VALUE - 1 && !computersColor)
                            break;
                        if (result[0] == Integer.MIN_VALUE + 1 && computersColor)
                            break;
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                if (isTimeOver(turn))
                    return;
                Integer[] arr = null;
                try {
                    arr = get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                Piece p = null;
                Integer[] compMove = null;
                if (arr != null) {
                    p = board[arr[1]][arr[2]].getPiece();
                    int points = arr[0];
                    compMove = new Integer[]{arr[3], arr[4]};
                }
                if (p != null) {
                    movedFrom = new Integer[]{p.getRow(), p.getCol()};
                    movedTo = compMove;
                    currPiece = p;
//					if (!computersColor) {
//						fullMoves++;
//						halfMoves++;
//					}
                    if (board[compMove[0]][compMove[1]].isFull() || currPiece.type == Type.PAWN) {
                        halfMoves = 0;
                    }
                    chessMoveList += moveToChessNotation(compMove, currPiece) + " ";
                    p.move(board, compMove[0], compMove[1], true);
                    if (computersColor)
                        whiteTime += increment;
                    else
                        blackTime += increment;
                    whiteSeconds = whiteTime % 60;
                    whiteMinutes = (whiteTime - whiteSeconds) / 60;
                    blackSeconds = blackTime % 60;
                    blackMinutes = (blackTime - blackSeconds) / 60;

                    lastMoveFen = CurrentFen();
                    turn = !turn;
                    positions.add(Zobrist.getZobristHash(turn, wcks, wcqs, bcks, bcqs));
                    isInCheck = ChessGame.isInCheck(!p.getColor());
                    stalemate = ChessGame.getAllMoves(!p.getColor()).isEmpty();
                    isGameEnded = ChessGame.isStaleMate(turn);
                    if (isGameEnded)
                        resignButton.setText("Exit");
                    GamePanel.animation = true;
                    if (isInCheck) {
                        clip = p.sound("chessCheckSound.wav");
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 1) + "+ ";
                    }
                    playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, computersColor});
                    moveListCounter++;
                    if (isGameEnded || stalemate) {
                        if (isInCheck)
                            clip = p.sound("chessCheckmateSound.wav");
                        else
                            clip = p.sound("chessStalemateSound.wav");
                    }
                    clip.setFramePosition(0);
                    clip.start();
                }
                isCompDone = true;
                twhitePieces = ChessGame.copy(whitePieces);
                tblackPieces = ChessGame.copy(blackPieces);
                ChessGame.updateThreats(computersColor);
                ChessGame.countMoves = 0;
                tableBase = true;
            }


        };
        worker.execute();
    }

    public static class myKeyAdapter extends KeyAdapter {

        JFrame frame;
        boolean full = true;

        myKeyAdapter(JFrame frame) {
            this.frame = frame;
        }

        static String[] words;

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (!animation && isGameEnded && !drawGameOver) {
                        if (playerMoveList.size() > 1 && moveListCounter > 0) {
                            if (moveListCounter == playerMoveList.size() - 1 && (chessMoveList.contains("0-1") | chessMoveList.contains("1-0") | chessMoveList.contains("1/2"))) {
                                words = chessMoveList.split(" ");
                                String lastWord = chessMoveList.split(" ")[chessMoveList.split(" ").length - 1];
                                chessMoveList = chessMoveList.substring(0, chessMoveList.lastIndexOf("" + lastWord));
                            }
                            moveListCounter--;
                            Object[] last = playerMoveList.get(moveListCounter);
                            movedFrom = (Integer[]) last[1];
                            movedTo = (Integer[]) last[2];
                            board = positionFromFen((String) last[0]);
                            ChessGame.updateBoards((Boolean) last[5]);
                            whiteTime = (int) last[3];
                            blackTime = (int) last[4];
                            String lastWord = chessMoveList.split(" ")[chessMoveList.split(" ").length - 1];
                            if (chessMoveList.lastIndexOf(" " + lastWord) == -1)
                                chessMoveList = chessMoveList.substring(0, chessMoveList.lastIndexOf(lastWord));
                            else
                                chessMoveList = chessMoveList.substring(0, chessMoveList.lastIndexOf(" " + lastWord));
                        }
                        break;
                    }

                case KeyEvent.VK_RIGHT:
                    if (!animation && isGameEnded && !drawGameOver) {
                        if (moveListCounter < playerMoveList.size() - 1) {
                            moveListCounter++;
                            Object[] last = playerMoveList.get(moveListCounter);
                            movedFrom = (Integer[]) last[1];
                            movedTo = (Integer[]) last[2];
                            board = positionFromFen((String) last[0]);
                            whiteTime = (int) last[3];
                            blackTime = (int) last[4];
                            if (moveListCounter == 1) {
                                chessMoveList += words[moveListCounter - 1];
                                vertical.setValue(vertical.getMaximum());
                            }
                            else {
                                chessMoveList += " " + words[moveListCounter - 1];
                            }
                            if (moveListCounter == playerMoveList.size() - 1) {
                                chessMoveList += " " + words[moveListCounter];
                            }
                        }
                        break;
                    }
                case KeyEvent.VK_ENTER:
                    if (e.isAltDown()) {
                        frame.dispose();
                        full = !full;
                        frame.setUndecorated(full);
                        frame.setAlwaysOnTop(full);
                        frame.pack();
                        frame.setVisible(true);
                        frame.setLocationRelativeTo(null);
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    frame.dispose();
                    full = !full;
                    frame.setUndecorated(full);
                    frame.setAlwaysOnTop(full);
                    frame.pack();
                    frame.setVisible(true);
                    frame.setLocationRelativeTo(null);
					break;
            }
        }
    }

    private void startOver() {
        running = false;
        isGameEnded = false;
        stalemate = false;
        dragging = false;
        drawGameOver = true;
        isCompDone = true;
        moves = null;
        board = positionFromFen(startFen);
        notAdded = true;
        takenBlackPieces = new ArrayList<>();
        takenWhitePieces = new ArrayList<>();
        positions.clear();
        transpositionTable.clear();
        playerMoveList.clear();
        moveListCounter = 0;
        chooseTime = 5;
        lastMoveFen = startFen;
        chessMoveList = "";
        movedFrom = null;
        movedTo = null;
        enPassant = new int[2];
        ChessGame.updateThreats(turn);
        ChessGame.updateThreats(!turn);
        isInCheck = ChessGame.isInCheck(turn);
        inMainWindow = true;
        Zobrist.fillArray();
        ChessGame.updateBoards(turn);
    }

    private void createYesNoButton(String str, boolean yes, int x, int y, JPanel panel, boolean over) {
        MyButton button = new MyButton(str);
        if (yes)
            resignYesButton = button;
        else
            resignNoButton = button;
        ActionListener l = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == button) {
                    panel.remove(resignYesButton);
                    panel.remove(resignNoButton);
                    resignButton.setEnabled(true);
                    changePersButton.setEnabled(true);
                    if (yes) {
                        panel.removeAll();
                        startOver();
                    } else {
                        if (over) {
                            drawGameOver = false;
                            resignButton.setEnabled(true);
                            changePersButton.setEnabled(true);
                        }
                    }
                    resignLabel = false;
                }
            }
        };
        button.addActionListener(l);
        button.setFocusable(false);
        button.setHoverBackgroundColor(Color.white);
        button.setPressedBackgroundColor(Color.white);
        button.setForeground(Color.black);
        button.setBackground(Color.white);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFont(new Font("Arial Black", Font.PLAIN, 15));
        button.setBounds(x, y, 35, 20);
        this.add(button);
    }

    private void createPersButton(JPanel panel) {
        changePersButton = new MyButton();
        ActionListener l = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == changePersButton) {
                    perspective = !perspective;
                }
            }
        };
        changePersButton.addActionListener(l);
        changePersButton.setFocusable(false);
        changePersButton.setBackground(new Color(45, 45, 45));
        changePersButton.setHoverBackgroundColor(new Color(60, 60, 60));
        changePersButton.setPressedBackgroundColor(new Color(100, 100, 100));
        changePersButton.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 2));
        changePersButton.setForeground(Color.white);
        changePersButton.setBounds(w / 2 - 50, h + screenHeight - 50, 100, 50);
        try {
            Image img = ImageIO.read(new File(System.getProperty("user.dir") + "\\images\\switch.png"));
            img = img.getScaledInstance(70, 40, Image.SCALE_SMOOTH);
            changePersButton.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            System.out.println(ex);
        }
        this.add(changePersButton);
    }

    private void createResignButton(JPanel panel) {
        resignButton = new MyButton("Resign");
        ActionListener l = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == resignButton) {
                    createYesNoButton("NO", false, w + 170, h + 375, panel, false);
                    createYesNoButton("YES", true, w + 390, h + 375, panel, false);
                    resignButton.setEnabled(false);
                    changePersButton.setEnabled(false);
                    resignLabel = true;
                }
            }
        };
        resignButton.addActionListener(l);
        resignButton.setFocusable(false);
        resignButton.setBackground(new Color(45, 45, 45));
        resignButton.setHoverBackgroundColor(new Color(60, 60, 60));
        resignButton.setPressedBackgroundColor(new Color(100, 100, 100));
        resignButton.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 2));
        resignButton.setForeground(Color.white);
        resignButton.setFont(new Font("Arial Black", Font.PLAIN, 15));
        resignButton.setBounds(w / 2 - 50, h, 100, 50);
        this.add(resignButton);
    }

    private void createMovesBoard() {
        movesBoard = new JLabel();
        movesBoard.setForeground(Color.white);
        movesBoard.setBackground(new Color(45, 45, 45));
        movesBoard.setVerticalAlignment(JLabel.TOP);
        movesBoard.setOpaque(true);
        movesBoard.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        Border boardBorder = BorderFactory.createLineBorder(Color.white.darker(), 1);
        movesBoard.setBorder(boardBorder);
        scrollPane = new JScrollPane(movesBoard);
        vertical = scrollPane.getVerticalScrollBar();
        scrollPane.setPreferredSize(new Dimension(w * 2 / 3, unitSize * 6));
        scrollPane.setBounds(w / 6, h + unitSize, w * 2 / 3, unitSize * 6);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        this.add(scrollPane);
    }

    private void getClickedButtonOnMainWindow(MouseEvent e) {
        int height = (int) dim.getHeight();
        int width = (int) dim.getWidth();
        // piece color buttons
        if (e.getX() > w - (w / 5) * 2 && e.getX() < w - (w / 5) * 2 + 50 && e.getY() > height / 5 && e.getY() < height / 5 + 50) {
            computersColor = false;
            perspective = false;
        }
        if (e.getX() > w - (w / 5) * 4 && e.getX() < w - (w / 5) * 4 + 50 && e.getY() > height / 5 && e.getY() < height / 5 + 50) {
            computersColor = true;
            perspective = true;
        }

        // board color buttons
        // green
        if (e.getX() > w - (w / 10) * 9 && e.getX() < w - (w / 10) * 9 + 100 && e.getY() > height / 2 + height / 19 && e.getY() < height / 2 + height / 19 + 100) {
            white = new Color(238, 238, 210);
            black = new Color(118, 150, 86);
        }
        // brown
        if (e.getX() > w - (w / 10) * 4 && e.getX() < w - (w / 10) * 4 + 100 && e.getY() > height / 2 + height / 19 && e.getY() < height / 2 + height / 19 + 100) {
            white = new Color(240, 217, 181);
            black = new Color(181, 136, 99);
        }
        // blue
        if (e.getX() > w - (w / 10) * 9 && e.getX() < w - (w / 10) * 9 + 100 && e.getY() > height / 2 + height / 4 && e.getY() < height / 2 + height / 4 + 100) {
            white = new Color(222, 227, 230);
            black = new Color(140, 162, 173);
        }
        // grey
        if (e.getX() > w - (w / 10) * 4 && e.getX() < w - (w / 10) * 4 + 100 && e.getY() > height / 2 + height / 4 && e.getY() < height / 2 + height / 4 + 100) {
            white = new Color(220, 220, 220);
            black = new Color(171, 171, 171);
        }

        // play button
        if (e.getX() > width - w / 2 - w / 4 && e.getX() < width - w / 2 - w / 4 + 165 && e.getY() > height / 7 && e.getY() < height / 7 + 80) {
            running = true;
            inMainWindow = false;
            whiteTime = chooseTime * 60;
            blackTime = chooseTime * 60;
            whiteSeconds = whiteTime % 60;
            whiteMinutes = (whiteTime - whiteSeconds) / 60;
            blackSeconds = blackTime % 60;
            blackMinutes = (blackTime - blackSeconds) / 60;
            playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, true});
            this.removeAll();
            createMovesBoard();
            createResignButton(this);
            createPersButton(this);
            countDown.start();
            Clip clip = null;
            try {
                URL url = this.getClass().getResource(("chessOpeningSound.wav"));
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                FloatControl gainControl =
                        (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-12.0f);
                clip.start();
                clip.setFramePosition(0);
            } catch (Exception e1) {
                System.out.println("sound not found!");
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        dragging = false;
        if (!isCompDone || isGameEnded || !running || resignLabel || e.getButton() != MouseEvent.BUTTON1) return;
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }
        if (currPiece != null && movedFrom != null && movedTo != null && (!turn && !computersColor || turn && computersColor) && !animation && isCompDone)
            animation = true;

        if (x < 8 && x > -1 && y < 8 && y > -1) {
            if (isCompDone && board[x][y].isFull() && board[x][y].getPiece().getColor() == turn && !animation) {
                currPiece = board[x][y].getPiece();
                this.moves = currPiece.possibleMoves();

            } else
                moves = null;
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (inMainWindow && e.getButton() == MouseEvent.BUTTON1) {
            getClickedButtonOnMainWindow(e);
        }
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (e.getButton() == MouseEvent.BUTTON3) {
            startArrow[0] = w + (unitSize / 2) + unitSize * y;
            startArrow[1] = h + (unitSize / 2) + unitSize * x;
            return;
        }
        if (!isCompDone || isGameEnded || !running || resignLabel || e.getButton() != MouseEvent.BUTTON1) return;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }
        arrows.clear();
        redSquares.clear();
        clickedSpot = new int[]{x, y};
        if (x < 8 && x > -1 && y < 8 && y > -1)
            if (isCompDone && board[x][y].isFull() && board[x][y].getPiece().getColor() == turn && !animation) {
                currPiece = board[x][y].getPiece();
                dragging = true;
                this.moves = currPiece.possibleMoves();
            }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (e.getButton() == MouseEvent.BUTTON3 && x < 8 && x >= 0 && y < 8 && y >= 0) {
            int endx = w + (unitSize / 2) + (unitSize * y);
            int endy = h + (unitSize / 2) + (unitSize * x);
            Arrow a = new Arrow(startArrow[0], startArrow[1], endx, endy);
            for (Arrow arrow : arrows)
                if (arrow.equals(a)) {
                    arrows.remove(arrow);
                    return;
                }
            if (startArrow[0] != endx || startArrow[1] != endy) {
                arrows.add(a);
            }
            else {
                Integer[] arr = new Integer[]{y, x};
                for (Integer[] square : redSquares) {
                    if (square[0] == arr[0] && square[1] == arr[1]) {
                        redSquares.remove(square);
                        return;
                    }
                }
                redSquares.add(new Integer[]{y, x});
            }
            return;
        }
        arrows.clear();
        redSquares.clear();
        if (!isCompDone || isGameEnded || !running || resignLabel || e.getButton() != MouseEvent.BUTTON1) return;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }
        dragging = false;
        if (moves != null && (!turn && computersColor || turn && !computersColor)) {
            Integer[] m = {x, y};
            for (Integer[] move : moves) {
                if (m[0].equals(move[0]) && m[1].equals(move[1])) {
                    movedFrom = new Integer[]{currPiece.getRow(), currPiece.getCol()};
                    movedTo = move;
//					if (computersColor) {
//						fullMoves++;
//						halfMoves++;
//					}
                    if (board[x][y].isFull() || currPiece.type == Type.PAWN) {
                        halfMoves = 0;
                    }
                    String lastMove = moveToChessNotation(move, currPiece) + " ";
                    chessMoveList += lastMove;
                    currPiece.move(board, x, y, true);
                    if (!computersColor)
                        whiteTime += increment;
                    else
                        blackTime += increment;
                    whiteSeconds = whiteTime % 60;
                    whiteMinutes = (whiteTime - whiteSeconds) / 60;
                    blackSeconds = blackTime % 60;
                    blackMinutes = (blackTime - blackSeconds) / 60;

                    lastMoveFen = CurrentFen();
                    turn = !turn;
                    positions.add(Zobrist.getZobristHash(turn, wcks, wcqs, bcks, bcqs));
                    isInCheck = ChessGame.isInCheck(!currPiece.getColor());
                    stalemate = ChessGame.getAllMoves(!currPiece.getColor()).isEmpty();
                    isGameEnded = ChessGame.isStaleMate(turn);
                    if (isGameEnded)
                        resignButton.setText("Exit");
                    if (isInCheck) {
                        clip = currPiece.sound("chessCheckSound.wav");
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 1) + "+ ";
                    }
                    playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, !computersColor});
                    moveListCounter++;
                    if (isGameEnded || stalemate) {
                        if (isInCheck)
                            clip = currPiece.sound("chessCheckmateSound.wav");
                        else
                            clip = currPiece.sound("chessStalemateSound.wav");
                    }
                    if (clip != null) {
                        clip.setFramePosition(0);
                        clip.start();
                    }
                    twhitePieces = ChessGame.copy(whitePieces);
                    tblackPieces = ChessGame.copy(blackPieces);
                    ChessGame.updateThreats(!computersColor);
                    if (m[0] == clickedSpot[0] && m[1] == clickedSpot[1])
                        animation = true;
                    moves = null;
                    break;
                }
            }
        }
        if (clickedSpot[0] != x || clickedSpot[1] != y)
            moves = null;
    }

    // Load position from fen string
    public static Spot[][] positionFromFen(String fen) {
        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();
        twhitePieces = new ArrayList<>();
        tblackPieces = new ArrayList<>();
        board = ChessGame.generate_Board(8);
        String[] sections = fen.split(" ");
        ChessGame.WP = 0L;
        ChessGame.WN = 0L;
        ChessGame.WB = 0L;
        ChessGame.WR = 0L;
        ChessGame.WQ = 0L;
        ChessGame.WK = 0L;
        ChessGame.BP = 0L;
        ChessGame.BN = 0L;
        ChessGame.BB = 0L;
        ChessGame.BR = 0L;
        ChessGame.BQ = 0L;
        ChessGame.BK = 0L;

        int file = 0;
        int rank = 0;

        for (int i = 0; i < sections[0].length(); i++) {
            char symbol = sections[0].charAt(i);
            if (symbol == '/') {
                file = 0;
                rank++;
            } else {
                if (Character.isDigit(symbol)) {
                    file += Character.getNumericValue(symbol);
                } else {
                    String Binary = "0000000000000000000000000000000000000000000000000000000000000000";
                    Binary = Binary.substring(rank * 8 + file + 1) + "1" + Binary.substring(0, rank * 8 + file);
                    boolean pieceColor = Character.isUpperCase(symbol);
                    Piece piece = null;
                    try {
                        switch (Character.toLowerCase(symbol)) {
                            case 'p':
                                piece = new Pawn(rank, file, pieceColor, 100);
                                if (pieceColor)
                                    ChessGame.WP += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BP += convertStringToBitboard(Binary);
                                break;
                            case 'k':
                                piece = new King(rank, file, pieceColor, 20000);
                                if (pieceColor)
                                    ChessGame.WK += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BK += convertStringToBitboard(Binary);
                                break;
                            case 'n':
                                piece = new Knight(rank, file, pieceColor, 320);
                                if (pieceColor)
                                    ChessGame.WN += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BN += convertStringToBitboard(Binary);
                                break;
                            case 'b':
                                piece = new Bishop(rank, file, pieceColor, 330);
                                if (pieceColor)
                                    ChessGame.WB += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BB += convertStringToBitboard(Binary);
                                break;
                            case 'r':
                                piece = new Rook(rank, file, pieceColor, 500);
                                if (pieceColor)
                                    ChessGame.WR += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BR += convertStringToBitboard(Binary);
                                break;
                            case 'q':
                                piece = new Queen(rank, file, pieceColor, 950);
                                if (pieceColor)
                                    ChessGame.WQ += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BQ += convertStringToBitboard(Binary);
                                break;
                        }
                    } catch (Exception e) {
                        System.out.println("something went wrong!");
                    }
                    if (piece != null) {
                        board[rank][file].assignPiece(piece);
                        if (pieceColor)
                            whitePieces.add(piece);
                        else
                            blackPieces.add(piece);
                    }
                    file++;
                }
            }
        }
        if (sections.length > 1)
            turn = sections[1].equalsIgnoreCase("w");
        else
            turn = true;

        String castlingRights = (sections.length > 2) ? sections[2] : "";

        wcks = castlingRights.contains("K");

        wcqs = castlingRights.contains("Q");

        bcks = castlingRights.contains("k");

        bcqs = castlingRights.contains("q");
        HashMap<Character, Integer> colMapping = new HashMap<>();
        colMapping.put('a', 0);
        colMapping.put('b', 1);
        colMapping.put('c', 2);
        colMapping.put('d', 3);
        colMapping.put('e', 4);
        colMapping.put('f', 5);
        colMapping.put('g', 6);
        colMapping.put('h', 7);
        if (sections.length > 3) {
            if (!sections[3].equals("-")) {
                enPassant[1] = colMapping.get(sections[3].charAt(0));
                enPassant[0] = 8 - Character.getNumericValue(sections[3].charAt(1));

            }
        }

        halfMoves = Integer.parseInt(sections[4]);
        fullMoves = Integer.parseInt(sections[5]);
        twhitePieces = ChessGame.copy(whitePieces);
        tblackPieces = ChessGame.copy(blackPieces);

        boolean c = true;
        Type[] types = {Type.PAWN, Type.KNIGHT, Type.BISHOP, Type.ROOK, Type.QUEEN, Type.KING};
        long[] tables = {ChessGame.WP, ChessGame.WN, ChessGame.WB, ChessGame.WR, ChessGame.WQ, ChessGame.WK, ChessGame.BP, ChessGame.BN, ChessGame.BB, ChessGame.BR, ChessGame.BQ, ChessGame.BK};
        for (int i = 0; i < 12; i++) {
            ChessGame.pieceTables.put("" + c + types[i % 6], tables[i]);
            if (i == 5)
                c = false;
        }
        return board;
    }

    // Get the fen string of the current position
    public static String CurrentFen() {
        StringBuilder fen = new StringBuilder();
        for (int rank = 0; rank < 8; rank++) {
            int numEmptyFiles = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = board[rank][file].getPiece();
                if (piece != null) {
                    if (numEmptyFiles != 0) {
                        fen.append(numEmptyFiles);
                        numEmptyFiles = 0;
                    }
                    boolean isBlack = !piece.getColor();
                    Type pieceType = piece.type;
                    char pieceChar = ' ';
                    switch (pieceType) {
                        case ROOK:
                            pieceChar = 'R';
                            break;
                        case KNIGHT:
                            pieceChar = 'N';
                            break;
                        case BISHOP:
                            pieceChar = 'B';
                            break;
                        case QUEEN:
                            pieceChar = 'Q';
                            break;
                        case KING:
                            pieceChar = 'K';
                            break;
                        case PAWN:
                            pieceChar = 'P';
                            break;
                    }
                    fen.append((isBlack) ? Character.toLowerCase(pieceChar) : pieceChar);
                } else {
                    numEmptyFiles++;
                }

            }
            if (numEmptyFiles != 0) {
                fen.append(numEmptyFiles);
            }
            if (rank != 7) {
                fen.append('/');
            }
        }

        // Side to move
        fen.append(' ');
        fen.append((turn) ? 'b' : 'w');

        // Castling
        fen.append(' ');
        fen.append((wcks) ? "K" : "");
        fen.append((wcqs) ? "Q" : "");
        fen.append((bcks) ? "k" : "");
        fen.append((bcqs) ? "q" : "");
        if (!wcks && !wcqs && !bcks && !bcqs)
            fen.append("-");

        // En-passant
        fen.append(' ');
        HashMap<Integer, String> colMapping = new HashMap<>();
        colMapping.put(0, "a");
        colMapping.put(1, "b");
        colMapping.put(2, "c");
        colMapping.put(3, "d");
        colMapping.put(4, "e");
        colMapping.put(5, "f");
        colMapping.put(6, "g");
        colMapping.put(7, "h");
        boolean added = false;
        if (turn) {
            if (currPiece.type == Type.PAWN)
                if (movedFrom[0] == 6 && movedTo[0] == 4 && currPiece.lastPos[0] == 6 && currPiece.getRow() == 4) {
                    added = true;
                    Pawn pa = (Pawn) currPiece;
                    enPassant[0] = pa.getRow() + 1;
                    enPassant[1] = pa.getCol();
                    fen.append(colMapping.get(pa.getCol()));
                    fen.append(8 - pa.getRow() - 1);
                }
        } else {
            if (currPiece.type == Type.PAWN)
                if (movedFrom[0] == 1 && movedTo[0] == 3 && currPiece.lastPos[0] == 1 && currPiece.getRow() == 3) {
                    added = true;
                    Pawn pa = (Pawn) currPiece;
                    enPassant[0] = pa.getRow() - 1;
                    enPassant[1] = pa.getCol();
                    fen.append(colMapping.get(pa.getCol()));
                    fen.append(8 - pa.getRow() + 1);
                }
        }
        if (!added) {
            fen.append("-");
            enPassant[0] = 0;
            enPassant[1] = 0;
        }
        lastEnPassant[0] = enPassant[0];
        lastEnPassant[1] = enPassant[1];

//		// 50 move counter
        fen.append(' ');
        fen.append(halfMoves);

        // Full-move count (should be one at start, and increase after each move by black)
        fen.append(' ');
        fen.append(fullMoves);
        return fen.toString();
    }

    public static String moveToChessNotation(Integer[] move, Piece p) {
        if (move == null) return "";
        if (p.type == Type.KING) {
            if (p.getColor()) {
                if (p.getRow() == 7 && p.getCol() == 4) {
                    if (move[0] == 7 && move[1] == 6)
                        return "O-O";
                    if (move[0] == 7 && move[1] == 2)
                        return "O-O-O";
                }
            } else {
                if (p.getRow() == 0 && p.getCol() == 4) {
                    if (move[0] == 0 && move[1] == 6)
                        return "O-O";
                    if (move[0] == 0 && move[1] == 2)
                        return "O-O-O";
                }
            }
        }
        StringBuilder result = new StringBuilder();
        HashMap<Type, Character> pieceMapping = new HashMap<>();
        pieceMapping.put(Type.KNIGHT, 'N');
        pieceMapping.put(Type.BISHOP, 'B');
        pieceMapping.put(Type.ROOK, 'R');
        pieceMapping.put(Type.QUEEN, 'Q');
        pieceMapping.put(Type.KING, 'K');
        HashMap<Integer, String> colMapping = new HashMap<>();
        colMapping.put(0, "a");
        colMapping.put(1, "b");
        colMapping.put(2, "c");
        colMapping.put(3, "d");
        colMapping.put(4, "e");
        colMapping.put(5, "f");
        colMapping.put(6, "g");
        colMapping.put(7, "h");
        String row = "" + (8 - move[0]);
        String col = colMapping.get(move[1]);
        if (p.type != Type.PAWN) {
            result.append(pieceMapping.get(p.type));
            long moves = p.getMoves();
            List<Piece> pieces = (p.getColor()) ? whitePieces : blackPieces;
            for (Piece piece : pieces) {
                if (p.type == piece.type && p != piece) {
                    long m = piece.getMoves();
                    long merged = moves & m;
                    String s = Long.toBinaryString(merged);
                    String zeros = "0000000000000000000000000000000000000000000000000000000000000000";
                    s = zeros.substring(s.length()) + s;
                    int index = 63 - (move[0] * 8 + move[1]);
                    if (s.charAt(index) == '1') {
                        result.append(colMapping.get(p.getCol()));
                    }
                }
            }
        }
        if (GamePanel.board[move[0]][move[1]].isFull()) {
            if (p.type == Type.PAWN) {
                result.append(colMapping.get(p.getCol()));
            }
            result.append("x");
        }
        result.append(col).append(row);
        if (p.type == Type.PAWN)
            if (p.getColor() && row.equals("8") || !p.getColor() && row.equals("1")) {
                result.append("=Q");
            }

        return result.toString();
    }

    public static void openingMoves(String move, boolean color) {
        HashMap<Character, Integer> colMapping = new HashMap<>();
        colMapping.put('a', 0);
        colMapping.put('b', 1);
        colMapping.put('c', 2);
        colMapping.put('d', 3);
        colMapping.put('e', 4);
        colMapping.put('f', 5);
        colMapping.put('g', 6);
        colMapping.put('h', 7);

        HashMap<Character, Type> pieceMapping = new HashMap<>();
        pieceMapping.put('N', Type.KNIGHT);
        pieceMapping.put('B', Type.BISHOP);
        pieceMapping.put('R', Type.ROOK);
        pieceMapping.put('Q', Type.QUEEN);
        pieceMapping.put('K', Type.KING);

        Type pieceType = Type.PAWN;
        int row = 0;
        int col = 0;
        int specificCol = -1;
        int specificRow = -1;
        if (move.equals("O-O")) {
            row = (computersColor) ? 7 : 0;
            col = 6;
            pieceType = Type.KING;
        } else if (move.equals("O-O-O")) {
            row = (computersColor) ? 7 : 0;
            col = 2;
            pieceType = Type.KING;
        } else if (move.length() == 2) {
            row = 8 - Character.getNumericValue(move.charAt(1));
            col = colMapping.get(move.charAt(0));
        } else if (move.length() == 3) {
            if (move.charAt(2) == '+') {
                row = 8 - Character.getNumericValue(move.charAt(1));
                col = colMapping.get(move.charAt(0));
            }
            else {
                pieceType = pieceMapping.get(move.charAt(0));
                row = 8 - Character.getNumericValue(move.charAt(2));
                col = colMapping.get(move.charAt(1));
            }
        } else if (move.length() == 4) {
            if (move.charAt(3) == '+' | move.charAt(3) == '#') {
                pieceType = pieceMapping.get(move.charAt(0));
                row = 8 - Character.getNumericValue(move.charAt(2));
                col = colMapping.get(move.charAt(1));
            } else {
                row = 8 - Character.getNumericValue(move.charAt(3));
                col = colMapping.get(move.charAt(2));
                if (Character.isUpperCase(move.charAt(0)))
                    pieceType = pieceMapping.get(move.charAt(0));
                if (Character.isDigit(move.charAt(1))) {
                    specificRow = 8 - Character.getNumericValue(move.charAt(1));
                } else if (move.charAt(1) != 'x') {
                    specificCol = colMapping.get(move.charAt(1));
                }
            }
        } else if (move.length() == 5) {
            if (move.charAt(4) == '+' | move.charAt(4) == '#') {
                row = 8 - Character.getNumericValue(move.charAt(3));
                col = colMapping.get(move.charAt(2));
            } else {
                row = 8 - Character.getNumericValue(move.charAt(4));
                col = colMapping.get(move.charAt(3));
            }
            if (Character.isUpperCase(move.charAt(0)))
                pieceType = pieceMapping.get(move.charAt(0));
            if (Character.isDigit(move.charAt(1))) {
                specificRow = 8 - Character.getNumericValue(move.charAt(1));
            } else if (move.charAt(1) != 'x') {
                specificCol = colMapping.get(move.charAt(1));
            }
        }
        List<Piece> pieces = (color) ? whitePieces : blackPieces;
        for (Piece p : pieces) {
            if (p.type == pieceType) {
                List<Integer[]> moves = p.possibleMoves();
                for (Integer[] m : moves) {
                    if (m[0] == row && m[1] == col) {
                        if ((specificCol == -1 && specificRow == -1) || (specificCol != -1 && p.getCol() == specificCol) || (specificRow != -1 && p.getRow() == specificRow)) {
                            movedFrom = new Integer[]{p.getRow(), p.getCol()};
                            movedTo = new Integer[]{row, col};
                            currPiece = p;
//							if (!computersColor) {
//								fullMoves++;
//								halfMoves++;
//							}
                            if (board[row][col].isFull() || currPiece.type == Type.PAWN) {
                                halfMoves = 0;
                            }
                            String lastMove = move + " ";
                            chessMoveList += lastMove;
                            p.move(board, row, col, true);
                            if (computersColor)
                                whiteTime += increment;
                            else
                                blackTime += increment;
                            lastMoveFen = CurrentFen();
                            turn = !turn;
                            GamePanel.animation = true;
                            positions.add(Zobrist.getZobristHash(turn, wcks, wcqs, bcks, bcqs));
                            isInCheck = ChessGame.isInCheck(!p.getColor());
                            stalemate = ChessGame.getAllMoves(!p.getColor()).isEmpty();
                            isGameEnded = ChessGame.isStaleMate(turn);
                            if (isGameEnded)
                                resignButton.setText("Exit");
                            if (isInCheck) {
                                clip = p.sound("chessCheckSound.wav");
                            }
                            playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, computersColor});
                            moveListCounter++;
                            if (isGameEnded || stalemate) {
                                if (isInCheck)
                                    clip = p.sound("chessCheckmateSound.wav");
                                else
                                    clip = p.sound("chessStalemateSound.wav");
                            }
                            clip.setFramePosition(0);
                            clip.start();
                            isCompDone = true;
                            twhitePieces = ChessGame.copy(whitePieces);
                            tblackPieces = ChessGame.copy(blackPieces);
                            ChessGame.updateThreats(computersColor);
                            ChessGame.countMoves = 0;
                            return;
                        }
                    }
                }
            }
        }
        throw new RuntimeException("Move not found!");
    }


    public static long convertStringToBitboard(String Binary) {
        if (Binary.charAt(0) == '0') {
            return Long.parseLong(Binary, 2);
        } else {
            return Long.parseLong("1" + Binary.substring(2), 2) * 2;
        }
    }

    public void runPython(String fen, boolean color) throws IOException {
        String c = color+"";
        String[] cmd = {
                "python",
                System.getProperty("user.dir") + "\\tableBase.py",
                c,
                fen
        };
        Process p =  Runtime.getRuntime().exec(cmd);
        try {
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}

