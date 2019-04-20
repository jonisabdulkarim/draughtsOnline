package me.checkers;

public class Piece {
    private boolean isRed;
    private boolean isKing;

    Piece(boolean isRed){
        this.isRed = isRed;
        isKing = false;
    }

    public boolean isRed(){
        return isRed;
    }

    public boolean isKing(){
        return isKing;
    }

    public void setKing(Boolean b){
        isKing = b;
    }
}
