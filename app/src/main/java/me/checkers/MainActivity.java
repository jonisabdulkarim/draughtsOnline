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

// Firebase imports
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

// Java utilities imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public boolean firstPlayerTurn;
    public boolean pieceHasBeenSelected;
    public int numberOfMoves;
    public Coordinates lastPos;
    public TextView game_over;
    public Piece lastSelectedPiece;

    public Tile[][] Board = new Tile[8][8];
    public List<List<Integer>> BoardId = new ArrayList<>();
    public Coordinates clickedPosition = new Coordinates(0,0);
    public TextView[][] DisplayBoard = new TextView[8][8];
    public TextView[][] DisplayBoardBackground = new TextView[8][8];

    public static boolean isRed;
    public static boolean isOnline;
    public static String roomId;
    public static Map<String, Object> roomData;
    public static int[][] miniBoard;

    ArrayList<Piece> redPieces;
    ArrayList<Piece> whitePieces;

    public final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // This method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //create instance and set view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //receive intent - online or offline play
        Intent intent = getIntent();
        isOnline = intent.getBooleanExtra(MainMenu.EXTRA_BOOLEAN, false);

        //initialise values
        redPieces = new ArrayList<>();
        whitePieces = new ArrayList<>();
        initialiseBoard();
        firstPlayerTurn = true;
        isRed = true;

        lastSelectedPiece = null;
        lastPos = null;
        pieceHasBeenSelected = false;
        roomId = null;
        roomData = null;

        game_over = findViewById(R.id.game_over);
        game_over.setVisibility(View.INVISIBLE);

        //initialise Firebase - online only
        if(isOnline) {
            initialiseOnlinePlay();
            writeBoardId();
        }
    }

    private void initialiseOnlinePlay(){
        checkForRooms(new MyCallback() {
            @Override
            public void onCallback(Map value) {
                if(roomId == null) {
                    Log.d("noRoom", "true");
                    Map<String, Object> newRoomInfo = new HashMap<>();
                    newRoomInfo.put("status", "half");
                    Map<String, Object> boardInfo = new HashMap<>();

                    writeBoardId();
                    for (int x = 0; x < 8; x++) {
                        boardInfo.put("row" + x, BoardId.get(x));
                    }

                    DocumentReference ref = db.collection("room").document();
                    ref.set(newRoomInfo);
                    roomId = ref.getId();

                    db.collection("room").document(roomId)
                            .collection("board").document("lastboard")
                            .set(boardInfo);
                    isRed = true;
                }
                else {
                    DocumentReference ref = db.collection("room").document(roomId);
                    ref.update("status", "full");
                    isRed = false;
                    listenForChanges();
                }
            }
        });
    }

    private void checkForRooms(final MyCallback myCallback){
        db.collection("room").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        for(DocumentSnapshot doc : docs) {
                            if ( Objects.equals(doc.getData().get("status"), "half")) {
                                roomId = doc.getId();
                                roomData = doc.getData();
                                myCallback.onCallback(null);
                            }
                        }
                        myCallback.onCallback(null);
                    }
                });
    }

    private void sendBoardToOpponent(){
        Map<String, Object> boardInfo = new HashMap<>();
        for (int x = 0; x < 8; x++) {
            boardInfo.put("row" + x, BoardId.get(x));
        }

        db.collection("room").document(roomId)
                .collection("board").document("lastboard")
                .update(boardInfo);
    }

    private void listenForChanges(){
        listenerForChanges(new MyCallback() {
            @Override
            public void onCallback(Map value) {
                miniBoard = new int[8][8];

                // Log.d("newboard", "this is the keyset: " + newBoard.keySet());
                for(int x = 0; x < 8 ; x++) {
                    List<Long> row = (List<Long>) value.get("row" + x);
                    for(int y = 0; y < 8; y++){
                        miniBoard[x][y] = Integer.parseInt(String.valueOf(row.get(y)));
                    }
                }

                BoardIdReversor();
                firstPlayerTurn = !firstPlayerTurn;
            }
        });
    }

    private void listenerForChanges(final MyCallback myCallback){
        DocumentReference ref = db.collection("room").document(roomId)
                .collection("board").document("lastboard");

        ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                @Nullable FirebaseFirestoreException e) {
                if(e != null){
                    Log.w("Error", "Listen failed.", e);
                    return;
                }
                if(documentSnapshot != null && documentSnapshot.exists()){
                    myCallback.onCallback(documentSnapshot.getData());
                    // Log.d("ListenSucc", "Current data: " + documentSnapshot.getData());
                } else {
                    Log.d("emptyData", "Current data: null");
                }
            }
        });
    }

    //converts board with integers
    //0 = empty tile
    //1 = red man     2 = red king
    //3 = white man   4 = white king
    private void writeBoardId(){
        //reset boardId
        BoardId = new ArrayList<List<Integer>>();
        for(int i = 0; i < 8; i++){
            BoardId.add(new ArrayList<Integer>());
        }

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if(Board[x][y].hasPiece()) {
                    if (Board[x][y].getPiece().isRed() && !Board[x][y].getPiece().isKing()) {
                        BoardId.get(x).add(1);
                    } else if (Board[x][y].getPiece().isRed() && Board[x][y].getPiece().isKing()) {
                        //BoardId[x][y] = 2;
                        BoardId.get(x).add(2);
                    } else if (!Board[x][y].getPiece().isRed() && !Board[x][y].getPiece().isKing()) {
                        //BoardId[x][y] = 3;
                        BoardId.get(x).add(3);
                    } else{
                        //BoardId[x][y] = 4;
                        BoardId.get(x).add(4);
                    }
                } else{
                    //BoardId[x][y] = 0;
                    BoardId.get(x).add(0);
                }
            }
        }
    }

    private void BoardIdReversor(){
        for(int x = 0; x < 8; x++){
            for( int y = 0; y < 8; y++){
                switch(miniBoard[x][y]){
                    case 0:
                        Board[x][y].removePiece();
                        break;
                    case 1:
                        Board[x][y].setPiece(new Piece(true));
                        break;
                    case 2:
                        Board[x][y].setPiece(new Piece(true));
                        Board[x][y].getPiece().setKing(true);
                        break;
                    case 3:
                        Board[x][y].setPiece(new Piece(false));
                        break;
                    case 4:
                        Board[x][y].setPiece(new Piece(false));
                        Board[x][y].getPiece().setKing(true);
                        break;
                }
            }
        }
        setBoard();
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

        Iterator<Piece> whiteIt = whitePieces.iterator();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 8; x += 2) {
                if (y % 2 == 0) {
                    try {
                        Board[x + 1][y].setPiece(whiteIt.next());
                    } catch (ArrayIndexOutOfBoundsException ignored) { }
                } else {
                    Board[x][y].setPiece(whiteIt.next());
                }
            }
        }

        Iterator<Piece> redIt = redPieces.iterator();
        for (int y = 5; y < 8; y++) {
            for (int x = 0; x < 8; x += 2) {
                if (y % 2 == 0) {
                    try {
                        Board[x + 1][y].setPiece(redIt.next());
                    } catch (ArrayIndexOutOfBoundsException ignored) { }
                } else {
                    Board[x][y].setPiece(redIt.next());
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
                    if (piece.isRed() && !piece.isKing()) {
                        DisplayBoard[x][y].setBackgroundResource(R.drawable.rpawn);
                    }
                    else if(piece.isRed() && piece.isKing()){
                        DisplayBoard[x][y].setBackgroundResource(R.drawable.rking);
                    }
                    else if(!piece.isRed() && piece.isKing()){
                        DisplayBoard[x][y].setBackgroundResource(R.drawable.wking);
                    }
                    else{
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
    public void onClick(View view) {
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
            if((selectedPiece == null
                    || selectedPiece.isRed() && !firstPlayerTurn
                    || !selectedPiece.isRed() && firstPlayerTurn)
                    && !isOnline){
                return;
            }

            Map<Piece, Coordinates> canAnyCapture = canAnyCapture();
            if(!canAnyCapture.isEmpty()) {
                boolean capturingPiece = false;
                for(Coordinates coordinates : canAnyCapture.values()){
                    if(coordinates.getX() == clickedPosition.getX()
                            && coordinates.getY() == clickedPosition.getY()){
                        capturingPiece = true;
                    }
                }
                if(!capturingPiece){
                    return;
                }
            }

            allowedMoves(selectedPiece, clickedPosition.getX(), clickedPosition.getY());
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
                    if(isOnline){
                        writeBoardId();
                        sendBoardToOpponent();
                        listenForChanges();
                    }
                }
                // capturing
                else{

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

                    if(canCapture(lastSelectedPiece, x, y).isEmpty()){
                        pieceHasBeenSelected = false;
                        firstPlayerTurn = !firstPlayerTurn;
                        if(isOnline){
                            writeBoardId();
                            sendBoardToOpponent();
                            listenForChanges();
                        }
                    }
                    else{
                        allowedMoves(lastSelectedPiece, x, y);
                        lastPos = new Coordinates(x, y);
                    }
                }
                // after move/capture - check for king
                if(lastSelectedPiece.isRed() && y == 0){
                    lastSelectedPiece.setKing(true);
                    setBoard();
                }
                else if(!lastSelectedPiece.isRed() && y == 7){
                    lastSelectedPiece.setKing(true);
                    setBoard();
                }
            }
        }

        char status = checkForWin();
        if(status != '0'){
            game_over.setVisibility(View.VISIBLE);
        }
    }

    public void allowedMoves(Piece piece, int px, int py){
        if(!piece.isKing() && piece.isRed()){
            //mandatory capturing
            List<Coordinates> captures = canCapture(piece, px, py);
            if(!captures.isEmpty()){
                for(Coordinates coord : captures){
                    DisplayBoardBackground[coord.getX()][coord.getY()]
                            .setBackgroundResource(R.color.colorBoardCaptureSelect);
                }
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
        if(piece.isKing() && piece.isRed()){
            //mandatory capturing
            List<Coordinates> captures = canCapture(piece, px, py);
            if(!captures.isEmpty()){
                for(Coordinates coord : captures){
                    DisplayBoardBackground[coord.getX()][coord.getY()]
                            .setBackgroundResource(R.color.colorBoardCaptureSelect);
                }
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
        if(piece.isKing() && !piece.isRed()){
            //mandatory capturing
            List<Coordinates> captures = canCapture(piece, px, py);
            if(!captures.isEmpty()){
                for(Coordinates coord : captures){
                    DisplayBoardBackground[coord.getX()][coord.getY()]
                            .setBackgroundResource(R.color.colorBoardCaptureSelect);
                }
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
    }

    public Map<Piece, Coordinates> canAnyCapture(){
        Map<Piece, Coordinates> map = new HashMap<>();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if(firstPlayerTurn
                        && Board[x][y].getPiece() != null
                        && Board[x][y].getPiece().isRed()){
                    List<Coordinates> res = new ArrayList<>(canCapture(Board[x][y].getPiece(), x, y));
                    if(!res.isEmpty()) {
                        map.put(Board[x][y].getPiece(), new Coordinates(x, y));
                    }
                }
                else if(!firstPlayerTurn
                        && Board[x][y].getPiece() != null
                        && !Board[x][y].getPiece().isRed()){
                    List<Coordinates> res = new ArrayList<>(canCapture(Board[x][y].getPiece(), x, y));
                    if(!res.isEmpty()) {
                        map.put(Board[x][y].getPiece(), new Coordinates(x, y));
                    }
                }
            }
        }

        return map;
    }

    public List<Coordinates> canCapture(Piece piece, int px, int py){
        ArrayList<Coordinates> result = new ArrayList<>();
        if(piece.isRed()) {
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
            if(piece.isKing()){
                try {
                    if (Board[px - 1][py + 1].hasPiece()
                            && !Board[px - 1][py + 1].getPiece().isRed()
                            && !Board[px - 2][py + 2].hasPiece()) {
                        result.add(new Coordinates(px - 2, py + 2));
                    }
                }
                catch (IndexOutOfBoundsException ignored) {}
                try{
                    if (Board[px + 1][py + 1].getPiece() != null
                            && !Board[px + 1][py + 1].getPiece().isRed()
                            && !Board[px + 2][py + 2].hasPiece()) {
                        result.add(new Coordinates(px + 2, py + 2));
                    }
                }
                catch (IndexOutOfBoundsException ignored) {}
            }
        }
        else if(!piece.isRed()) {
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
            if(piece.isKing()){
                try {
                    if (Board[px - 1][py - 1].hasPiece()
                            && Board[px - 1][py - 1].getPiece().isRed()
                            && !Board[px - 2][py - 2].hasPiece()) {
                        result.add(new Coordinates(px - 2, py - 2));
                    }
                }
                catch (IndexOutOfBoundsException ignored) {}
                try{
                    if (Board[px + 1][py - 1].getPiece() != null
                            && Board[px + 1][py - 1].getPiece().isRed()
                            && !Board[px + 2][py - 2].hasPiece()) {
                        result.add(new Coordinates(px + 2, py - 2));
                    }
                }
                catch (IndexOutOfBoundsException ignored) {}
            }
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

    public char checkForWin(){
        int redPieceCount = 0;
        int whitePieceCount = 0;
        for (Tile[] tiles : Board) {
            for (Tile tile : tiles) {
                if (tile.hasPiece()) {
                    if (tile.getPiece().isRed()) {
                        redPieceCount++;
                    } else if (!tile.getPiece().isRed()) {
                        whitePieceCount++;
                    }
                }
            }
        }

        // WIN status
        // No win = 0
        // Red win = 1
        // White win = 2
        if(redPieceCount == 0){
            Log.d("whiwin", "true");
            return '2';
        }
        else if(whitePieceCount == 0){
            Log.d("redwin", "true");
            return '1';
        }
        return '0';
    }
}
