package me.checkers;

// Android imports
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.util.Log;  // for commenting

// Java imports
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public boolean firstPlayerTurn;
    public ArrayList<Coordinates> coords = new ArrayList<>();
    public Tile[][] Board = new Tile[8][8];
    public boolean pieceHasBeenSelected = false;
    public Coordinates lastPos = null;
    public Coordinates clickedPosition = new Coordinates(0,0);
    public TextView game_over;
    public TextView[][] DisplayBoard = new TextView[8][8];
    public TextView[][] DisplayBoardBackground = new TextView[8][8];
    public int numberOfMoves;
    public Piece lastSelectedPiece;
    public static boolean redTurn;
    public static boolean isOnline;
    public List<Coordinates> lastCaptures;

    ArrayList<Piece> redPieces;
    ArrayList<Piece> whitePieces;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //create instance and set view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //receive intent - online or offline play
        Intent intent = getIntent();
        isOnline = intent.getBooleanExtra(MainMenu.EXTRA_BOOLEAN, false);
        Log.d("Check online", "Value is: " + isOnline);

        //initialise values
        redPieces = new ArrayList<>();
        whitePieces = new ArrayList<>();
        lastCaptures = new ArrayList<>();
        initialiseBoard();
        lastSelectedPiece = null;
        firstPlayerTurn = true;

        //initialise FireBase - online only
        // initialiseOnlinePlay();

    }

    private void initialiseOnlinePlay(){

    }

    private void sendBoardToOpponent(){
        
    }

    private void waitAndRetrieve(){

    }

    private void convertBoardIntoID(){

    }

    private void initialiseBoard(){
        for (int i = 0; i < 12; i++) {
            redPieces.add(new Piece(true));
            whitePieces.add(new Piece(false));
        }

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Board[x][y] = new Tile(null);
            }
        }

        for (Piece piece : whitePieces) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 8; x += 2) {
                    if (y % 2 == 0) {
                        try {
                            Board[x + 1][y].setPiece(piece);
                        } catch (ArrayIndexOutOfBoundsException ignored) { }
                    } else {
                        Board[x][y].setPiece(piece);
                    }
                }
            }
        }

        for (Piece piece : redPieces) {
            for (int y = 5; y < 8; y++) {
                for (int x = 0; x < 8; x += 2) {
                    if (y % 2 == 0) {
                        try {
                            Board[x + 1][y].setPiece(piece);
                        } catch (ArrayIndexOutOfBoundsException ignored) { }
                    } else {
                        Board[x][y].setPiece(piece);
                    }
                }
            }
        }

        Resources r = MainActivity.this.getResources();
        String name = getPackageName();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int idName = r.getIdentifier("R" + x + "" + y, "id", name);
                int idBackName = r.getIdentifier("R0" + x + "" + y, "id", name);
                DisplayBoard[x][y] = findViewById(idName);
                DisplayBoardBackground[x][y] = findViewById(idBackName);
            }
        }

        numberOfMoves = 0;
        pieceHasBeenSelected = false;
        if(!isOnline) {
            firstPlayerTurn = true;
        }
        setBoard();
    }

    // Draws pieces for the foreground board
    private void setBoard(){
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece piece = Board[x][y].getPiece();

                if (Board[x][y].getPiece() != null) {
                    if (piece.isRed()) {
                        DisplayBoard[x][y].setBackgroundResource(R.drawable.rpawn);
                    } else {
                        DisplayBoard[x][y].setBackgroundResource(R.drawable.wpawn);
                    }
                } else{
                    DisplayBoard[x][y].setBackgroundResource(0);
                }
            }
        }
    }

    // Checks location of click and allows for
    // movement of pieces if permitted
    @Override
    public void onClick(View view){
        Resources r = getResources();
        String name = getPackageName();
        boolean found = false;

        int tileID = view.getId();
        for (int x = 0; x < 8 && !found; x++) {
            for (int y = 0; y < 8; y++) {
                int idName = r.getIdentifier("R" + x + "" + y, "id", name);
                if(tileID == idName){
                    clickedPosition.setX(x);
                    clickedPosition.setY(y);
                    found = true;
                    break;
                }
            }
        }

        Piece selectedPiece = Board[clickedPosition.getX()][clickedPosition.getY()].getPiece();

        if(!pieceHasBeenSelected){
            if(selectedPiece == null
                    || selectedPiece.isRed() && !firstPlayerTurn
                    || !selectedPiece.isRed() && firstPlayerTurn){
                return;
            }
            lastCaptures = allowedMoves(selectedPiece, clickedPosition.getX(), clickedPosition.getY());
            lastSelectedPiece = selectedPiece;
            lastPos = new Coordinates(clickedPosition.getX(), clickedPosition.getY());
            pieceHasBeenSelected = true;
        }
        else {
            int x = clickedPosition.getX();
            int y = clickedPosition.getY();
            int redColor = Color.RED;
            int blueColor = Color.BLUE;

            ColorDrawable background =
                    (ColorDrawable) DisplayBoardBackground[x][y].getBackground();

            if(selectedPiece != null ||
                    (background.getColor() != redColor
                    && background.getColor() != blueColor)){
                clearAllowedMoves();
                pieceHasBeenSelected = false;
            }
            else{
                // movement
                if(background.getColor() == redColor) {
                    int lx = lastPos.getX();
                    int ly = lastPos.getY();
                    Board[x][y].setPiece(lastSelectedPiece);
                    Board[lx][ly].removePiece();
                    redPieces.add(lastSelectedPiece);
                    clearAllowedMoves();
                    setBoard();
                    pieceHasBeenSelected = false;
                    firstPlayerTurn = !firstPlayerTurn;
                }
                // capturing
                else{
                    while(lastCaptures.isEmpty()) {
                        int lx = lastPos.getX();
                        int ly = lastPos.getY();
                        Board[x][y].setPiece(lastSelectedPiece);
                        Board[lx][ly].removePiece();

                        if (lx < x && ly > y) {
                            Board[x - 1][y + 1].removePiece();
                        } else if (lx > x && ly < y) {
                            Board[x + 1][y - 1].removePiece();
                        } else if (lx > x && ly > y) {
                            Board[x + 1][y + 1].removePiece();
                        } else if (lx < x && ly < y) {
                            Board[x - 1][y - 1].removePiece();
                        }

                        redPieces.add(lastSelectedPiece);


                        clearAllowedMoves();
                        setBoard();

                        lastCaptures = allowedMoves(lastSelectedPiece, x, y);
                    }
                    pieceHasBeenSelected = false;
                }
            }
        }
    }

    //todo
    public List<Coordinates> furtherCaptures(){
        return new ArrayList<>();
    }

    public List<Coordinates> allowedMoves(Piece piece, int px, int py){
        if(!piece.isKing() && piece.isRed()){
            //mandatory capturing
            List<Coordinates> captures = canCapture(piece, px, py);
            if(!captures.isEmpty()){
                for(Coordinates coord : captures){
                    DisplayBoardBackground[coord.getX()][coord.getY()]
                            .setBackgroundResource(R.color.colorBoardCaptureSelect);
                }
                //todo: test capture return
                return captures;
            }
            //no capturing
            else {
                try {
                    if (Board[px + 1][py - 1].getPiece() == null) {
                        DisplayBoardBackground[px + 1][py - 1]
                                .setBackgroundResource(R.color.colorBoardSelected);
                    }
                } catch (IndexOutOfBoundsException ignored) { }
                try {
                    if (Board[px - 1][py - 1].getPiece() == null) {
                        DisplayBoardBackground[px - 1][py - 1]
                                .setBackgroundResource(R.color.colorBoardSelected);
                    }
                } catch (IndexOutOfBoundsException ignored) { }
            }
        }
        if(!piece.isKing() && !piece.isRed()){
            List<Coordinates> captures = canCapture(piece, px, py);
            if(!captures.isEmpty()){
                for(Coordinates coord : captures){
                    DisplayBoardBackground[coord.getX()][coord.getY()]
                            .setBackgroundResource(R.color.colorBoardCaptureSelect);
                }
            }
            else {
                try {
                    if (Board[px + 1][py + 1].getPiece() == null) {
                        DisplayBoardBackground[px + 1][py + 1]
                                .setBackgroundResource(R.color.colorBoardSelected);
                    }
                } catch (IndexOutOfBoundsException ignored) { }
                try {
                    if (Board[px - 1][py + 1].getPiece() == null) {
                        DisplayBoardBackground[px - 1][py + 1]
                                .setBackgroundResource(R.color.colorBoardSelected);
                    }
                } catch (IndexOutOfBoundsException ignored) { }
            }
        }
        return new ArrayList<>(); //empty return in all other cases
    }

    public List<Coordinates> canCapture(Piece piece, int px, int py){
        ArrayList<Coordinates> result = new ArrayList<>();
        if(piece.isRed() && !piece.isKing()) {
            try {
                if (Board[px - 1][py - 1].hasPiece()
                        && !Board[px - 1][py - 1].getPiece().isRed()
                        && !Board[px - 2][py - 2].hasPiece()) {
                    result.add(new Coordinates(px - 2, py - 2));
                }
            }
            catch (IndexOutOfBoundsException ignored) {}
            try{
                if (Board[px + 1][py - 1].getPiece() != null
                        && !Board[px + 1][py - 1].getPiece().isRed()
                        && !Board[px + 2][py - 2].hasPiece()) {
                    result.add(new Coordinates(px + 2, py - 2));
                }
            }
            catch (IndexOutOfBoundsException ignored) {}
        }
        else if(!piece.isRed() && !piece.isKing()) {
            try {
                if (Board[px - 1][py + 1].hasPiece()
                        && Board[px - 1][py + 1].getPiece().isRed()
                        && !Board[px - 2][py + 2].hasPiece()) {
                    result.add(new Coordinates(px - 2, py + 2));
                }
            }
            catch (IndexOutOfBoundsException ignored) {}
            try{
                if (Board[px + 1][py + 1].getPiece() != null
                        && Board[px + 1][py + 1].getPiece().isRed()
                        && !Board[px + 2][py + 2].hasPiece()) {
                    result.add(new Coordinates(px + 2, py + 2));
                }
            }
            catch (IndexOutOfBoundsException ignored) {}
        }
        return result;
    }

    public void clearAllowedMoves(){
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if((x+y)%2==0)
                    DisplayBoardBackground[x][y].setBackgroundResource(R.color.colorBoardBuff);
                else DisplayBoardBackground[x][y].setBackgroundResource(R.color.colorBoardGreen);
            }
        }
    }
}
