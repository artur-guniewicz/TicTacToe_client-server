import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

public class TicTacToeServer
{
    // plansza ma 9 pól k
    // każde pole jest zajęte przez gracza lub puste
    static Player[] board = {
            null, null, null,
            null, null, null,
            null, null, null};

    Player currentPlayer;
    private Player playerX = null;
    private Player playerO = null;
    ServerSocket listener;

    TicTacToeServer()
    {
        try
        {
            listener = new ServerSocket(9999);
            playerX = new Player(listener.accept(), 'X');
            playerO = new Player(listener.accept(), 'O');
            playerX.setOpponent(playerO);
            playerO.setOpponent(playerX);
            currentPlayer = playerX;
            playerX.start();
            playerO.start();
        }

        catch(IOException e)
        {
            e.printStackTrace();
        }
    }


    public boolean hasWinner()
    {
        return
                (board[0] != null && board[0] == board[1] && board[0] == board[2])
                        || (board[3] != null && board[3] == board[4] && board[3] == board[5])
                        || (board[6] != null && board[6] == board[7] && board[6] == board[8])
                        || (board[0] != null && board[0] == board[3] && board[0] == board[6])
                        || (board[1] != null && board[1] == board[4] && board[1] == board[7])
                        || (board[2] != null && board[2] == board[5] && board[2] == board[8])
                        || (board[0] != null && board[0] == board[4] && board[0] == board[8])
                        || (board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }

    // sprawdza, czy wszystkie pola są zajęte
    public boolean boardFilledUp()
    {
        for (Player player : board) // = for (int i = 0; i < board.length; i++)
        {
            if (player == null)
                return false;
        }

        return true;
    }

    // sprawdza, czy gracz wykonał poprawny ruch
    public synchronized boolean legalMove(int location, Player player)
    {
        if (player == currentPlayer && board[location] == null)
        {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        }

        return false;
    }

    // zapis gry
    public void saveGame()
    {
        try
        {
            PrintWriter out = new PrintWriter("zapis.txt");

            for (Player player : board)
            {
                if (player == null)
                    out.println(0);

                else if (player == playerX)
                    out.println(1);

                else
                    out.println(2);
            }

            if(currentPlayer.equals(playerX))
                out.println("X");

            else
                out.println("O");

            out.close();

        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // wczytanie gry
    void loadGame(PrintWriter output) throws IOException
    {
        FileReader in = new FileReader("zapis.txt");
        BufferedReader br = new BufferedReader(in);
        String temp;

        for(int i = 0; i < board.length; i++)
        {
            temp = br.readLine();

            if(temp.equals("0"))
                board[i] = null;

            else if(temp.equals("1"))
                board[i] = playerX;

            else
                board[i] = playerO;
        }

        br.close();
    }

    class Player extends Thread implements Serializable
    {
        char mark;
        char oponentMark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public Player(Socket socket, char mark)
        {
            this.socket = socket;
            this.mark = mark;

            if(mark == 'X')
                oponentMark = 'O';

            else
                oponentMark = 'X';

            try
            {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Oczekiwanie na przeciwnika...");
            }

            catch (IOException e)
            {
                System.out.println("Error: " + e);
            }
        }

        public void setOpponent(Player opponent)
        {
            this.opponent = opponent;
        }

        // wysyła do serwera informację o ruchu przeciewnika i sprawdza, czy jest wygrana
        public void otherPlayerMoved(int location)
        {
            output.println("OPPONENT_MOVED " + location);
            output.println(hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        // wątek uruchamiamy po dołączeniu dwóch graczy
        public void run()
        {
            try
            {
                output.println("Obaj gracze dołączyli");

                if (mark == 'X')
                    output.println("MESSAGE Kolej gracza X");

                while (true)
                {
                    String command = input.readLine();

                    if (command.startsWith("MOVE"))
                    {
                        int location = Integer.parseInt(command.substring(5));

                        if (legalMove(location, this))
                        {
                            output.println("VALID_MOVE");
                            output.println(hasWinner() ? "VICTORY" : boardFilledUp() ? "TIE" : "");
                        }

                        else
                            output.println("MESSAGE ?");
                    }

                    else if (command.startsWith("QUIT"))
                        return;

                    else if (command.startsWith("SAVE"))
                        saveGame();

                    else if (command.startsWith("LOAD"))
                    {
                        loadGame(output);

                        output.println("GAME_LOADED");

                        for (Player player : board)
                        {
                            if (player == null)
                                output.println("0");

                            else if (player == this)
                                output.println(mark);

                            else
                                output.println(oponentMark);
                        }

                        output.flush();

                        if(currentPlayer == this)
                            output.println("Kolej gracza " + mark);

                        else
                            output.println("Kolej gracza " + oponentMark);

                        output.flush();
                    }
                }
            }

            catch (IOException e)
            {
                System.out.println("Error: " + e);
            }

            finally
            {
                try
                {
                    socket.close();
                }

                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void main(String[] args) throws IOException
    {
        System.out.println("Serwer jest uruchomiony! Niech wygra lepszy!");

        TicTacToeServer game = new TicTacToeServer();
    }
}