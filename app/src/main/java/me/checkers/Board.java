package me.checkers;

import java.util.ArrayList;
import java.util.HashMap;

public class Board {

    private static final int DEFAULT_BOARD_DIMENSION = 8;

    private final Tile[][] board;

    public Board(int width, int length) {
        board = new Tile[width][length];
    }

    public Board() {
        this(DEFAULT_BOARD_DIMENSION, DEFAULT_BOARD_DIMENSION);
    }

    /**
     * Returns piece at the tile specified
     * @param coordinates = coordinates of tile
     * @return piece object, if present, else null
     */
    public Piece get(Coordinates coordinates) {
        Tile tile = getTile(coordinates);
        return tile.getPiece();
    }

    /**
     * Places piece at a specific tile
     * @param coordinates = coordinates of tile
     * @param piece = piece to place at tile
     */
    public void place(Coordinates coordinates, Piece piece) {
        Tile tile = getTile(coordinates);
        tile.setPiece(piece);
    }


    /**
     * Remove piece at specified tile. No effect if
     * a piece is not present.
     * @param coordinates = coordinate of tile
     */
    public void remove(Coordinates coordinates) {
        Tile tile = getTile(coordinates);
        tile.removePiece();
    }

    /**
     * Returns a boolean value, indicating if a piece
     * is present at this tile
     * @param coordinates = coordinate of tile
     * @return true, if piece is present, else false
     */
    public boolean contains(Coordinates coordinates) {
        Tile tile = getTile(coordinates);
        return tile.hasPiece();
    }

    /**
     * Upgrades a piece to king
     * @param coordinates = coordinate of tile
     * @throws NullPointerException if piece doesn't exist
     */
    public void upgrade(Coordinates coordinates) {
        Tile tile = getTile(coordinates);
        tile.getPiece().setKing(true);
    }

    /**
     * Remove all pieces from the board
     */
    public void clear() {
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                getTile(x, y).removePiece();
            }
        }
    }

    /**
     * Produces an integer representation of the board for
     * use in online play. The HashMap is necessary because
     * Google Firebase does NOT support 2D arrays natively.
     * Instead, the array rows are placed in a HashMap. The
     * row position is indicated in the key string value.
     * @return a hash map with the row indicated in its key
     * and actual piece indicated in its value
     */
    public HashMap<String, ArrayList<Integer>> convert() {
        HashMap<String, ArrayList<Integer>> map = new HashMap<>();

        for (int x = 0; x < 8; x++) {
            ArrayList<Integer> arrayOfRow = new ArrayList<>();
            for (int y = 0; y < 8; y++) {
                arrayOfRow.add(convertTile(x, y));
            }
            map.put("row" + x, arrayOfRow);
        }

        return map;
    }

    /**
     * Private helper method for convert(). This method finds the
     * relevant tile and, based on its piece, returns an integer
     * representing the piece in that tile:
     * 0 = empty tile
     * 1 = red man     2 = red king
     * 3 = white man   4 = white king
     * @param x = X coordinate of tile
     * @param y = Y coordinate of tile
     * @return an integer representing the piece in that tile
     */
    private int convertTile(int x, int y) {
        Tile tile = getTile(x, y);
        if(tile.hasPiece()) {
            // red man
            if (tile.getPiece().isRed() && !tile.getPiece().isKing()) {
                return 1;
            }
            // red king
            else if (tile.getPiece().isRed() && tile.getPiece().isKing()) {
                return 2;
            }
            // white man
            else if (!tile.getPiece().isRed() && !tile.getPiece().isKing()) {
                return 3;
            }
            // white king
            else if (!tile.getPiece().isRed() && tile.getPiece().isKing()){
                return 4;
            }
            else {
                throw new IllegalStateException("Invalid piece status");
            }
        } else {
            return 0;
        }
    }

    /**
     * Private helper method used to obtain tile from
     * specified coordinates. Prevents code repetition.
     * @param coordinates = coordinates of tile
     * @return tile object specified by coordinates
     */
    private Tile getTile(Coordinates coordinates) {
        return getTile(coordinates.getX(), coordinates.getY());
    }

    /**
     * Private helper method to access specified tile.
     * @param x = X coordinate of tile
     * @param y = Y coordinate of tile
     * @return tile object specified by coordinates
     */
    private Tile getTile(int x, int y) {
        return board[x][y];
    }

}
