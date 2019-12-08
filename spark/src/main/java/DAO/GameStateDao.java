package DAO;

import DTO.CardDto;
import DTO.GameStateDto;
import Server.MainServer;

public class GameStateDao {
    private static GameStateDao gameStateDaoObject;

    private GameStateDao() {};

    public static GameStateDao getInstance() {
        if (gameStateDaoObject == null) {
            gameStateDaoObject = new GameStateDao();
        }
        return gameStateDaoObject;
    }

    public boolean flipCard(int gameId, String playerAttemptingToFlipCard, int x, int y) {
        GameStateDto game = MainServer.getGameById(gameId);
        if (game != null && game.currentPlayersTurn.equals(playerAttemptingToFlipCard)) {
            // If no other card has been flipped before, simply flip this card over
            if (game.cardFlipped == null) {
                if (game.gameBoard.getCard(x,y) != null) {
                    game.gameBoard.getCard(x,y).isRevealed = true;
                    game.cardFlipped = game.gameBoard.getCard(x,y);
                }
            }
            // If a card has been flipped before, then we need to check if this new card bring flipped matches it
            else {
                if (game.gameBoard.getCard(x,y) != null) {
                    // If the new card being flipped matches the already flipped card
                    if (game.gameBoard.getCard(x,y).cardId == game.cardFlipped.cardId) {
                        game.gameBoard.getCard(game.cardFlipped.x, game.cardFlipped.y).isPaired = true;
                        game.gameBoard.getCard(game.cardFlipped.x, game.cardFlipped.y).isRevealed = true;
                        game.cardFlipped = null;
                        game.gameBoard.getCard(x,y).isPaired = true;
                        game.gameBoard.getCard(x,y).isRevealed = true;

                        updatePlayerScore(gameId, game.currentPlayersTurn);
                        game.numPairsLeft--;

                        //TODO ????
                        checkForGameOver(gameId);
                    }
                    // If the new card being flipped does not match the already flipped card
                    else {
                        game.gameBoard.getCard(game.cardFlipped.x, game.cardFlipped.y).isPaired = false;
                        game.gameBoard.getCard(game.cardFlipped.x, game.cardFlipped.y).isRevealed = false;
                        game.cardFlipped = null;
                        game.gameBoard.getCard(x,y).isPaired = false;
                        game.gameBoard.getCard(x,y).isRevealed = false;

                        changeTurns(gameId);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isPair(CardDto cardOne, CardDto cardTwo) {
        return (cardOne.cardId == cardTwo.cardId && cardOne != null && cardTwo != null);
    }

    private boolean checkForGameOver(int gameId) {
        GameStateDto game = MainServer.getGameById(gameId);
        if (game != null && game.numPairsLeft <= 0) {
            game.gameIsOver = true;
            return true;
        }
        return false;
    }

    private boolean changeTurns(int gameId) {
        GameStateDto game = MainServer.getGameById(gameId);
        if (game != null) {
            if (game.currentPlayersTurn == game.playerOne._id) {
                game.currentPlayersTurn = game.playerTwo._id;
                return true;
            }
            else if (game.currentPlayersTurn == game.playerTwo._id) {
                game.currentPlayersTurn = game.playerOne._id;
                return true;
            }
        }
        return false;
    }

    private boolean updatePlayerScore(int gameId, String playerId) {
        GameStateDto game = MainServer.getGameById(gameId);
        if (game != null) {
            if (game.playerOne._id == playerId) {
                game.playerOneScore = game.playerOneScore + 1;
                return true;
            }
            else if (game.playerTwo._id == playerId) {
                game.playerTwoScore = game.playerOneScore + 1;
                return true;
            }
        }
        return false;
    }
}
