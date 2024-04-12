import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

public class HangmanClient extends JFrame implements ActionListener {
        private static final String SERVER_ADDRESS = "localhost";
        private static final int SERVER_PORT = 12345;

        private JLabel wordLabel;
        private JLabel attemptsLabel;
        private static JLabel imageLabel;
        private static BufferedImage bufferedImage;
        private static Image image;
        private static ImageIcon imageIcon;
        private int guessesLeft = 6;
        private static JButton[] letterButtons;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private JButton newGameButton;

        public HangmanClient() {

                JPanel leftPanel = new JPanel(new GridLayout(1,1));
                leftPanel.setBackground(Color.PINK);
                leftPanel.setBounds(0,0,500,600);

                JPanel rightPanel = new JPanel(new GridLayout(3,1));
                rightPanel.setBackground(Color.PINK);
                rightPanel.setBounds(500, 0, 500,600);

                imageLabel = new JLabel();
                imageLabel.setBackground(Color.pink);
                imageLabel.setOpaque(true);
                updateImage(guessesLeft);
                leftPanel.add(imageLabel, CENTER_ALIGNMENT);

                wordLabel = new JLabel("Word: ", JLabel.CENTER);
                wordLabel.setFont(new Font("Broadway", Font.BOLD, 40));
                rightPanel.add(wordLabel);

                attemptsLabel = new JLabel("Attempts: ", JLabel.CENTER);
                attemptsLabel.setFont(new Font("Broadway", Font.BOLD, 20));
                rightPanel.add(attemptsLabel);

                JPanel buttonPanel = new JPanel(new GridLayout(4, 7));
                buttonPanel.setBackground(Color.pink);
                letterButtons = new JButton[26];
                for (char c = 'A'; c <= 'Z'; c++) {
                        int index = c - 'A';
                        letterButtons[index] = new JButton(String.valueOf(c));
                        letterButtons[index].setFont(new Font("Broadway", Font.BOLD, 20));
                        letterButtons[index].addActionListener(this);
                        buttonPanel.add(letterButtons[index]);
                }
                rightPanel.add(buttonPanel, BorderLayout.CENTER);

                setTitle("Hangman Game");
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setSize(1000, 627);
                setLayout(null);
                setResizable(false);
                setVisible(true);
                add(leftPanel);
                add(rightPanel);

                connectToServer();
        }

        private void connectToServer() {
                try {
                        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = new PrintWriter(socket.getOutputStream(), true);

                        // Start a new thread to receive messages from the server
                        Thread thread = new Thread(new ServerListener());
                        thread.start();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        @Override
        public void actionPerformed(ActionEvent e) {

                JButton source = (JButton) e.getSource();
                source.setEnabled(false);
                String guessedLetter = source.getText();
                out.println(guessedLetter);
        }

        private class ServerListener implements Runnable {
                @Override
                public void run() {
                        try {
                                while (true) {
                                        String message = in.readLine();
                                        if (message != null) {
                                                if (message.startsWith("WORD: ")) {
                                                        String word = message.substring(6);
                                                        SwingUtilities.invokeLater(() -> wordLabel.setText("Word: " + word));
                                                } else if (message.startsWith("ATTEMPTS: ")) {
                                                        String attempts = message.substring(10);
                                                        checkCorrectness(guessesLeft, Integer.parseInt(attempts));
                                                        guessesLeft = Integer.parseInt(attempts);
                                                        updateImage(guessesLeft);
                                                        SwingUtilities.invokeLater(() -> attemptsLabel.setText("Attempts: " + attempts));
                                                } else if (message.equals("GAME_OVER")) {
                                                        playSound("lost.wav");
                                                        JOptionPane.showMessageDialog(HangmanClient.this, "Game Over! You lost.");
                                                        resetButtons();
                                                } else if (message.equals("YOU_WIN")) {
                                                        playSound("won.wav");
                                                        JOptionPane.showMessageDialog(HangmanClient.this, "Congratulations! You won.");
                                                        resetButtons();
                                                }
                                        }
                                }
                        } catch (IOException e) {
                                e.printStackTrace();
                        } finally {
                                try {
                                        socket.close();
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }
        }

        public void checkCorrectness(int guessesLeft, int updatedGuesses){
                if(guessesLeft==updatedGuesses)
                        playSound("correct.wav");
                if(guessesLeft > updatedGuesses)
                        playSound("wrong.wav");
        }

        public void playSound(String soundFile){
            try{
                File file = new File(soundFile);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            } catch (Exception e) {
                    System.out.println("Error playing sound: " + e.getMessage());;
            }


        }

        public static void updateImage(int strikes){
            try {
                bufferedImage = ImageIO.read(new File("hangman" + strikes + ".png"));
                image = bufferedImage.getScaledInstance(400,500,Image.SCALE_DEFAULT);
                imageIcon = new ImageIcon(image);
                imageLabel.setIcon(imageIcon);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public static void resetButtons(){
                for(int i=0; i < letterButtons.length; i++){
                        letterButtons[i].setEnabled(true);
                }
        }

        public static void main(String[] args) {
                SwingUtilities.invokeLater(HangmanClient::new);
        }
}
