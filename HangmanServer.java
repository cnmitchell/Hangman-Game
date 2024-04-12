import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HangmanServer {
    private static final int PORT = 12345;
    private static ArrayList<String> words = new ArrayList<>();

    private List<ClientHandler> clients = new ArrayList<>();
    private String word;
    private StringBuilder guessedWord;
    private int remainingAttempts = 6;

    public HangmanServer() {
        readFile();
        word = words.get((int) (Math.random() * words.size()));
        guessedWord = new StringBuilder(word.replaceAll("[A-Z]", "-"));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Hangman Server is running...");
            System.out.println("Word to guess: " + word);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFile() {
        File file = new File("words.txt");
        try {
            Scanner fileScanner = new Scanner(file);
            while(fileScanner.hasNextLine()){
                words.add(fileScanner.nextLine().toUpperCase());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                sendMessage("WORD: " + guessedWord.toString());
                sendMessage("ATTEMPTS: " + remainingAttempts);

                while (true) {
                    String guess = in.readLine();
                    if (guess == null) {
                        break;
                    }
                    processGuess(guess);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    private synchronized void processGuess(String guess) {
        if (word.contains(guess)) {
            for (int i = 0; i < word.length(); i++) {
                if (word.charAt(i) == guess.charAt(0)) {
                    guessedWord.setCharAt(i, guess.charAt(0));
                }
            }
        } else {
            remainingAttempts--;
            if (remainingAttempts <= 0) {
                broadcast("GAME_OVER");
                resetGame();
                return;
            }
        }

        if (guessedWord.toString().equals(word)) {
            broadcast("YOU_WIN");
            resetGame();
        } else {
            broadcast("WORD: " + guessedWord);
            broadcast("ATTEMPTS: " + remainingAttempts);
        }
    }

    private synchronized void resetGame() {
        word = words.get((int) (Math.random() * words.size()));
        System.out.println("Word to guess: " + word);
        guessedWord = new StringBuilder(word.replaceAll("[A-Z]", "-"));
        remainingAttempts = 6;
        broadcast("WORD: " + guessedWord);
        broadcast("ATTEMPTS: " + remainingAttempts);
    }

    public static void main(String[] args) {
        new HangmanServer();
    }
}