import java.util.*;

public class BoardGeneration {
    public static void initiateStandardChess() {
        String chessBoard[][]={
                {"r","n","b","q","k","b","n","r"},
                {"p","p","p","p","p","p","p","p"},
                {" "," "," "," "," "," "," "," "},
                {" "," "," "," "," "," "," "," "},
                {" "," "," "," "," "," "," "," "},
                {" "," "," "," "," "," "," "," "},
                {"P","P","P","P","P","P","P","P"},
                {"R","N","B","Q","K","B","N","R"}};
        arrayToBitboards(chessBoard);
    }
    public static void initiateChess960() {
        String chessBoard[][]={
            {" "," "," "," "," "," "," "," "},
            {"p","p","p","p","p","p","p","p"},
            {" "," "," "," "," "," "," "," "},
            {" "," "," "," "," "," "," "," "},
            {" "," "," "," "," "," "," "," "},
            {" "," "," "," "," "," "," "," "},
            {"P","P","P","P","P","P","P","P"},
            {" "," "," "," "," "," "," "," "}};
        //step 1:
        int random1=(int)(Math.random()*8);
        chessBoard[0][random1]="b";
        chessBoard[7][random1]="B";
        //step 2:
        int random2=(int)(Math.random()*8);
        while (random2%2==random1%2) {
            random2=(int)(Math.random()*8);
        }
        chessBoard[0][random2]="b";
        chessBoard[7][random2]="B";
        //step 3:
        int random3=(int)(Math.random()*8);
        while (random3==random1 || random3==random2) {
            random3=(int)(Math.random()*8);
        }
        chessBoard[0][random3]="q";
        chessBoard[7][random3]="Q";
        //step 4:
        int random4a=(int)(Math.random()*5);
        int counter=0;
        int loop=0;
        while (counter-1<random4a) {
            if (" ".equals(chessBoard[0][loop])) {counter++;}
            loop++;
        }
        chessBoard[0][loop-1]="n";
        chessBoard[7][loop-1]="N";
        int random4b=(int)(Math.random()*4);
        counter=0;
        loop=0;
        while (counter-1<random4b) {
            if (" ".equals(chessBoard[0][loop])) {counter++;}
            loop++;
        }
        chessBoard[0][loop-1]="n";
        chessBoard[7][loop-1]="N";
        //step 5:
        counter=0;
        while (!" ".equals(chessBoard[0][counter])) {
            counter++;
        }
        chessBoard[0][counter]="r";
        chessBoard[7][counter]="R";
        while (!" ".equals(chessBoard[0][counter])) {
            counter++;
        }
        chessBoard[0][counter]="k";
        chessBoard[7][counter]="K";
        while (!" ".equals(chessBoard[0][counter])) {
            counter++;
        }
        chessBoard[0][counter]="r";
        chessBoard[7][counter]="R";
        arrayToBitboards(chessBoard);
    }
    public static void arrayToBitboards(String[][] chessBoard) {
        String Binary;
        for (int i=0;i<64;i++) {
            Binary="0000000000000000000000000000000000000000000000000000000000000000";
            Binary=Binary.substring(i+1)+"1"+Binary.substring(0, i);
            switch (chessBoard[i/8][i%8]) {
                case "P": ChessGame.WP+=convertStringToBitboard(Binary);
                    break;
                case "N": ChessGame.WN+=convertStringToBitboard(Binary);
                    break;
                case "B": ChessGame.WB+=convertStringToBitboard(Binary);
                    break;
                case "R": ChessGame.WR+=convertStringToBitboard(Binary);
                    break;
                case "Q": ChessGame.WQ+=convertStringToBitboard(Binary);
                    break;
                case "K": ChessGame.WK+=convertStringToBitboard(Binary);
                    break;
                case "p": ChessGame.BP+=convertStringToBitboard(Binary);
                    break;
                case "n": ChessGame.BN+=convertStringToBitboard(Binary);
                    break;
                case "b": ChessGame.BB+=convertStringToBitboard(Binary);
                    break;
                case "r": ChessGame.BR+=convertStringToBitboard(Binary);
                    break;
                case "q": ChessGame.BQ+=convertStringToBitboard(Binary);
                    break;
                case "k": ChessGame.BK+=convertStringToBitboard(Binary);
                    break;
            }
        }
        drawArray();
    }
    public static long convertStringToBitboard(String Binary) {
        if (Binary.charAt(0)=='0') {//not going to be a negative number
            return Long.parseLong(Binary, 2);
        } else {
            return Long.parseLong("1"+Binary.substring(2), 2)*2;
        }
    }
    public static void drawArray() {
        String chessBoard[][]=new String[8][8];
        for (int i=0;i<64;i++) {
            chessBoard[i/8][i%8]=" ";
        }
        for (int i=0;i<64;i++) {
            if (((ChessGame.WP>>i)&1)==1) {chessBoard[i/8][i%8]="P";}
            if (((ChessGame.WN>>i)&1)==1) {chessBoard[i/8][i%8]="N";}
            if (((ChessGame.WB>>i)&1)==1) {chessBoard[i/8][i%8]="B";}
            if (((ChessGame.WR>>i)&1)==1) {chessBoard[i/8][i%8]="R";}
            if (((ChessGame.WQ>>i)&1)==1) {chessBoard[i/8][i%8]="Q";}
            if (((ChessGame.WK>>i)&1)==1) {chessBoard[i/8][i%8]="K";}
            if (((ChessGame.BP>>i)&1)==1) {chessBoard[i/8][i%8]="p";}
            if (((ChessGame.BN>>i)&1)==1) {chessBoard[i/8][i%8]="n";}
            if (((ChessGame.BB>>i)&1)==1) {chessBoard[i/8][i%8]="b";}
            if (((ChessGame.BR>>i)&1)==1) {chessBoard[i/8][i%8]="r";}
            if (((ChessGame.BQ>>i)&1)==1) {chessBoard[i/8][i%8]="q";}
            if (((ChessGame.BK>>i)&1)==1) {chessBoard[i/8][i%8]="k";}
        }
        for (int i=0;i<8;i++) {
            System.out.println(Arrays.toString(chessBoard[i]));
        }
    }
}