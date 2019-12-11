package Server;

import static spark.Spark.*;

import DAO.GameStateServerDao;
import DAO.PlayerMongoDao;
import DataObjects.GameState;
import DataObjects.Player;
import WebSocket.WebSocketHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.util.Pair;
import spark.Request;
import spark.Response;
import org.eclipse.jetty.websocket.api.Session;

import java.util.*;

public class GameServer {
   // List of current players waiting in queue
   static ArrayList<Pair<Player, Session>> queueList = new ArrayList<>();

   //List of current games going on
   static ArrayList<GameState> gameList = new ArrayList<>();
   static int totalGames = 0;

   public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
    port(1234);

    webSocket("/wsLoading", WebSocketHandler.class);

    post("/login", GameServer::logIn);

    post("/register", GameServer::register);

    post("/logout", GameServer::logoutViaHTTP);

    get("/playerInfo", GameServer::playerInfo);

    get("/rankings", GameServer::rankings);
    }

    private static String logIn(Request request, Response response) {
        String username = request.queryMap("username").value();
        String password = request.queryMap("password").value();
        if (request.queryParams().size() == 2 && username != null && password != null) {
            Player receivedPlayer = PlayerMongoDao.getInstance().getPlayerByUsername(username);
            if (receivedPlayer != null) {
                if (receivedPlayer.password.equals(password)) {
                    if (!receivedPlayer.isLoggedIn) {
                        PlayerMongoDao.getInstance().updatePlayerLoggedStatusById(receivedPlayer._id, true);
                        ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Login Success", receivedPlayer._id);
                        return gson.toJson(messageToReturn);
                    } else {
                        ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Login Failed", "User is logged in");
                        return gson.toJson(messageToReturn);
                    }
                } else {
                    ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Login Failed", "Invalid password");
                    return gson.toJson(messageToReturn);
                }
            } else {
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Login Failed", "Invalid username");
                return gson.toJson(messageToReturn);
            }
        } else {
            ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Login Failed", "Invalid query");
            return gson.toJson(messageToReturn);
        }
    }

    private static String register(Request request, Response response) {
        String username = request.queryMap("username").value();
        String password = request.queryMap("password").value();
        if (request.queryParams().size() == 2 && username != null && password != null) {
            String newUserId = PlayerMongoDao.getInstance().addPlayerToDatabase(username, password);
            if (newUserId != null) {
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Register Success", newUserId);
                return gson.toJson(messageToReturn);
            } else {
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Register Failed", "Invalid username");
                return gson.toJson(messageToReturn);
            }
        } else {
            ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Register Failed", "Invalid query");
            return gson.toJson(messageToReturn);
        }
    }

    private static String playerInfo(Request request, Response response) {
        String playerId = request.queryMap("playerId").value();
        if (request.queryParams().size() == 1 && playerId != null) {
            Player playerToReturn = PlayerMongoDao.getInstance().getPlayerById(playerId);
            if (playerToReturn != null) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Player Info Success", gson.toJson(playerToReturn));
                return gson.toJson(messageToReturn);
            } else {
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Player Info Failed", "Invalid ID");
                return gson.toJson(messageToReturn);
            }
        } else {
            ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Player Info Failed", "Invalid query");
            return gson.toJson(messageToReturn);
        }
    }

    private static String logoutViaHTTP(Request request, Response response) {
        String playerId = request.queryMap("playerId").value();
        Player player = PlayerMongoDao.getInstance().getPlayerById(playerId);
        if (player != null) {
            if (player.isLoggedIn) {
                PlayerMongoDao.getInstance().updatePlayerGameStatusById(player._id, false, false);
                PlayerMongoDao.getInstance().updatePlayerLoggedStatusById(player._id, false);
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Logout Success", player._id);
                return gson.toJson(messageToReturn);
            } else {
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Logout Failed", "User is not logged in");
                return gson.toJson(messageToReturn);
            }
        } else {
            ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Logout Failed", "Invalid ID");
            return gson.toJson(messageToReturn);
        }
    }

    private static String rankings(Request request, Response response) {
        ArrayList<Player> allPlayers = PlayerMongoDao.getInstance().getAllPlayers();
        Collections.sort(allPlayers);

        int size = 10;
        if (allPlayers.size() < size) {
            size = allPlayers.size();
        }

        Player[] topPlayers = allPlayers.subList(0, size).toArray(new Player[allPlayers.size()]);
        ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Rankings Success", gson.toJson(topPlayers));
        return gson.toJson(messageToReturn);
    }

    // Helper function to create a game, given two players and their sessions
    private static void createGame(Pair<Player, Session> playerOne, Pair<Player, Session> playerTwo) {
        GameState newGame = new GameState(generateNewGameId(), playerOne.getKey(), playerOne.getValue(),
                playerTwo.getKey(), playerTwo.getValue());

        gameList.add(newGame);
        PlayerMongoDao.getInstance().updatePlayerGameStatusById(playerOne.getKey()._id, false, true);
        PlayerMongoDao.getInstance().updatePlayerGameStatusById(playerTwo.getKey()._id, false, true);
        WebSocketHandler.newGameBroadcast(newGame);
    }

    // Returns an ongoing game from the game list, based on the gameId
    public static GameState getGameById(int gameId) {
      for (GameState game: gameList) {
          if (game.gameId == gameId) {
              return game;
          }
      }
      return null;
    }

    // Removes an ongoing game from the game list, based on the gameId
    public static boolean removeGameById(int gameId) {
        for (GameState game: gameList) {
            if (game.gameId == gameId) {
                PlayerMongoDao.getInstance().updatePlayerGameStatusById(game.playerOne._id, false, false);
                PlayerMongoDao.getInstance().updatePlayerGameStatusById(game.playerTwo._id, false, false);

                /* TODO
                1. Update highscores (on MongoDB)
                2. Notify players that game has ended (via websocket)
                 */

                gameList.remove(game);
                return true;
            }
        }
        return false;
    }

    // Helper function to generate a new gameId, and update the count for the next game gameId
    private static int generateNewGameId() {
      totalGames++;
      return (totalGames);
    }

    public static void processMessage(String message, Session session) {
      ResponseTemplate.Response response = gson.fromJson(message, ResponseTemplate.Response.class);

      String responseType = response.responseType;
      switch (responseType) {
          case "Play Game":
              addPlayerToQueue(response.responseBody, session);
              break;

          case "Flip Card":
              flipCard(response.responseBody);
              break;

          case "Logout":
              logoutViaWebsocket(response.responseBody);
              break;

          case "Disconnected":
              userDisconnected(response.responseBody, session);
              break;
      }
    }

    /* Adds a player to queue, given their ID and session
    Format of response body must be as follows: "playerId"
    ex: "9F1d5q7"
    */
    public static void addPlayerToQueue(String playerId, Session session) {
        Player player = PlayerMongoDao.getInstance().getPlayerById(playerId);
        if (player != null) {
            queueList.add(new Pair<>(player, session));
            PlayerMongoDao.getInstance().updatePlayerGameStatusById(player._id, true, false);

            // After adding a player to the queue, attempt to match them with someone else in the queue
            if (queueList.size() == 2) {
                Pair<Player, Session> playerOne = queueList.remove(0);
                PlayerMongoDao.getInstance().updatePlayerGameStatusById(playerOne.getKey()._id, false, false);
                Pair<Player, Session> playerTwo = queueList.remove(0);
                PlayerMongoDao.getInstance().updatePlayerGameStatusById(playerTwo.getKey()._id, false, false);

                createGame(playerOne, playerTwo);
            }
        }
    }

    /*
    Format of response body must be as follows: "gameId,playerId,x,y"
    ex: "5,9F1d5q7,3,7"
    */
    private static void flipCard(String flipInformation) {
        String[] splitString = flipInformation.split(",");
        if (splitString.length == 4) {
            int gameId = Integer.parseInt(splitString[0]);
            String playerId = splitString[1];
            int cardX = Integer.parseInt(splitString[2]);
            int cardY = Integer.parseInt(splitString[3]);

            GameStateServerDao.getInstance().flipCard(gameId, playerId, cardX, cardY);
        }
    }

    /*
    Format of response body must be as follows: "playerId"
    ex: "9F1d5q7"
    */
    private static String logoutViaWebsocket(String playerId) {
        Player player = PlayerMongoDao.getInstance().getPlayerById(playerId);
        if (player != null) {
            if (player.isLoggedIn) {
                PlayerMongoDao.getInstance().updatePlayerGameStatusById(player._id, false, false);
                PlayerMongoDao.getInstance().updatePlayerLoggedStatusById(player._id, false);
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Logout Success", player._id);
                return gson.toJson(messageToReturn);
            } else {
                ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Logout Failed", "User is not logged in");
                return gson.toJson(messageToReturn);
            }
        } else {
            ResponseTemplate.Response messageToReturn = new ResponseTemplate.Response("Logout Failed", "Invalid ID");
            return gson.toJson(messageToReturn);
        }
    }

    public static void userDisconnected(String userId, Session session) {
      /* TODO
      1. Check if person is in queue, and remove them if so
      2. Check if person is in game, and end game if so
      3. Check if person is logged in, and log them out if so
       */
    }
}
