package client;

// Java program to create a blank text field with a
// given initial text and given number of columns
import chatserver.Chatroom;
import data.*;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class Chat extends JFrame implements ActionListener {

    private static JTextArea textDisplay;
    private static JTextArea textEntry;
    private static JFrame frame;

    private static String username;
    private static String chatroomName;
    private static String hostname;
    private static int tcpPort;
    private static int rmiPort;
    private static Object chatWait;
    private static RMIAccess<IChatroomUserOperations> chatroomAccessor;
    private static RMIAccess<ICentralUserOperations> centralServer;

    // default constructor
    public Chat(String username, String chatroomName, String hostname, int tcpPort, int rmiPort, RMIAccess<ICentralUserOperations> centralServer, Object chatWait){
        Chat.username = username;
        Chat.chatroomName = chatroomName;
        Chat.hostname = hostname;
        Chat.tcpPort = tcpPort;
        Chat.rmiPort = rmiPort;
        Chat.centralServer = centralServer;
        Chat.chatWait = chatWait;
    }

    // main class
    public void start()
    {
        // create a new frame to store text field and button
        frame = new JFrame("textfield");

        // create a panel to add buttons and textfield
        JPanel display = new JPanel(new BorderLayout());

        // set layout

        // set text layout
        JPanel mainText = new JPanel();
        textDisplay = new JTextArea();
        textDisplay.setLineWrap(true);
        textDisplay.setEditable(false);
        textDisplay.setColumns(65);
        textDisplay.setRows(25);
        textDisplay.setBorder(BorderFactory.createLineBorder(Color.black));
        JScrollPane mainScroll = new JScrollPane(textDisplay);
        mainScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainText.add(mainScroll, BorderLayout.CENTER);
        textDisplay.setText("System >> Welcome to the chatroom! Please be civil.");

        display.add(mainText, BorderLayout.NORTH);

        // set text entry layout
        JPanel entry = new JPanel();
        entry.setBorder(new EmptyBorder(0, 12, 0, 0));
        textEntry = new JTextArea();
        textEntry.setLineWrap(true);
        textEntry.setColumns(55);
        textEntry.setRows(3);
        JScrollPane entryScroll = new JScrollPane(textEntry);
        entryScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        entryScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        entryScroll.setBorder(BorderFactory.createLineBorder(Color.black));
        entry.add(entryScroll, BorderLayout.CENTER);

        display.add(entry, BorderLayout.WEST);

        // set submit button
        JPanel submitButton = new JPanel();

        // create a object of the text class
        JButton b = new JButton("submit");

        b.addActionListener(this);
        b.setPreferredSize(new Dimension(75, 40));

        submitButton.setBorder(new EmptyBorder(4, 0, 0, 12));
        submitButton.add(b, BorderLayout.CENTER);
        display.add(submitButton, BorderLayout.EAST);

        // add panel to frame
        frame.add(display);
        frame.setSize(700, 520);
        frame.setResizable(false);
        frame.setVisible(true);

        Socket s = Chat.establishSocket();

        // start receiving messages from server
        Thread receiveThread = new Thread(new ReceiveThread(s));
        receiveThread.start();

        // set up accessor
        Chat.chatroomAccessor = new RMIAccess<>(Chat.hostname, Chat.rmiPort, "IChatroomUserOperations");
        try {
            Chat.chatroomAccessor.getAccess().joinChatroom(Chat.chatroomName, Chat.username);
        } catch (RemoteException | NotBoundException e) {
            Logger.writeErrorToLog("Unable to signal join chatroom message");
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    Chat.chatroomAccessor.getAccess().leaveChatroom(Chat.chatroomName, Chat.username);
                } catch (RemoteException | NotBoundException err) {
                    Logger.writeErrorToLog("Unable to access chatroom server for leave operation");
                }
                receiveThread.interrupt();
                try {
                    s.close();
                } catch (IOException ioException) {
                    Logger.writeErrorToLog("Unable to close receive socket for chat");
                }
                synchronized (Chat.chatWait) {
                    chatWait.notify();
                }
                super.windowClosing(e);
            }
        });
    }

    // if the button is pressed
    // use this to send messages to the server
    @Override
    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        if (s.equals("submit")) {

            // send message to server via RMI accessor
            try {
                chatroomAccessor.getAccess().chat(chatroomName, username, textEntry.getText());
                // set the text of field to blank
                textEntry.setText("");
            } catch (RemoteException | NotBoundException err) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "There was an error contacting the chat server: \"%s\"",
                        err.getMessage()
                ));
            }


        }
    }

    static class ReceiveThread implements Runnable {

        private Socket receiveSocket;

        ReceiveThread(Socket s) {
            this.receiveSocket = s;
        }

        @Override
        public void run() {

            BufferedReader socketReader = null;
            try {
                socketReader = new BufferedReader(new InputStreamReader(this.receiveSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String message;
            try {
                Logger.writeMessageToLog("startin");
                while ((message = socketReader.readLine()) != null) {
                    Logger.writeMessageToLog("firing");

                    // write display to the main part of the window
                    textDisplay.setText(textDisplay.getText() + "\n" + message);
                }
            }
            catch (IOException e) {
                Logger.writeErrorToLog("Lost connection with chatroom server; reestablishing connection...");
                try {
                    ChatroomResponse r = centralServer.getAccess().reestablishChatroom(Chat.chatroomName, Chat.username);
                    if (r.getStatus() == ResponseStatus.FAIL) {
                        // exit out of this class
                        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                        return;
                    }

                    Chat.hostname = r.getAddress();
                    Chat.rmiPort = r.getRegistryPort();
                    Chat.tcpPort = r.getTcpPort();

                    Chat.chatroomAccessor = new RMIAccess<>(Chat.hostname, Chat.rmiPort, "IChatroomUserOperations");
                    this.receiveSocket = Chat.establishSocket();

                    if (this.receiveSocket == null) {
                        throw new IOException("Unable to establish new receive socket");
                    }

                } catch (NotBoundException | IOException err) {
                    Logger.writeErrorToLog("Unable to reestablish chatroom central server; closing window...");
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                    return;
                }
                Logger.writeMessageToLog("Reestablished connection to chatroom server");
            }

        }
    }

    private static Socket establishSocket() {
        Socket s = null;
        try {
            s = new Socket(Chat.hostname, Chat.tcpPort);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            Logger.writeMessageToLog("about to send");
            out.println(Chat.chatroomName + ":" + Chat.username);
            Logger.writeMessageToLog("sent");
            String response = in.readLine();

            Logger.writeMessageToLog(response);

            if (response.compareTo("success") != 0) {
                Logger.writeErrorToLog("Failed to establish connection with Chatroom server");
                return null;
            }

        } catch (IOException e) {
            Logger.writeErrorToLog("Unable to establish connection with Chatroom server");
            return null;
        }
        return s;
    }
}