package client;

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
    private static final Object reestablishLock = new Object();
    private static Thread chatThread;

    // default constructor
    public Chat(String username, String chatroomName, String hostname, int tcpPort, int rmiPort, RMIAccess<ICentralUserOperations> centralServer, Object chatWait){
        Chat.username = username;
        Chat.chatroomName = chatroomName;
        Chat.hostname = hostname;
        Chat.tcpPort = tcpPort;
        Chat.rmiPort = rmiPort;
        Chat.centralServer = centralServer;
        Chat.chatWait = chatWait;
        Chat.chatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // leave chatroom if shutdown with ctrl-c or other sigterm
                try {
                    Chat.chatroomAccessor.getAccess().leaveChatroom(Chat.chatroomName, Chat.username);
                } catch (RemoteException | NotBoundException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Failed to leave chatroom \"%s\" on sigterm shutdown",
                            Chat.chatroomName
                    ));
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(Chat.chatThread);


    }

    // main class
    public void start()
    {

        Logger.writeMessageToLog("Setting up JSwing window...");

        // create a new frame to store text field and button
        frame = new JFrame(Chat.chatroomName);

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

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Attempting to establish TCP connection with chat server at \"%s:%d\"...",
                Chat.hostname,
                Chat.tcpPort
        ));

        // initialize socket
        Socket s = Chat.establishSocket();

        if (s == null) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Failed to establish TCP connection with chat server at \"%s:%d\"",
                    Chat.hostname,
                    Chat.tcpPort
            ));
            return;
        }

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully established TCP connection with chat server at \"%s:%d\"",
                Chat.hostname,
                Chat.tcpPort

        ));

        Logger.writeMessageToLog("Initiating receive thread for TCP connection...");

        // start receiving messages from server
        Thread receiveThread = new Thread(new ReceiveThread(s));
        receiveThread.start();

        Logger.writeMessageToLog("Successfully started receive thread for TCP connection");

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Attempting to contact chat server RMI interface at \"%s:%d\"",
                Chat.hostname,
                Chat.rmiPort
        ));
        // set up accessor
        Chat.chatroomAccessor = new RMIAccess<>(Chat.hostname, Chat.rmiPort, "IChatroomUserOperations");
        try {
            Chat.chatroomAccessor.getAccess().joinChatroom(Chat.chatroomName, Chat.username);
        } catch (RemoteException | NotBoundException e) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Unable to look up chat server RMI interface at \"%s:%d\"",
                    Chat.hostname,
                    Chat.rmiPort
            ));
            try {
                s.close();
            } catch (IOException err) {
                Logger.writeErrorToLog("Failed to close socket TCP connection");
            }
            return;
        }

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully looked up RMI interface for chat server at \"%s:%d\"",
                Chat.hostname,
                Chat.rmiPort
        ));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    Chat.chatroomAccessor.getAccess().leaveChatroom(Chat.chatroomName, Chat.username);
                } catch (RemoteException | NotBoundException err) {
                    Logger.writeErrorToLog("Unable to access chatroom server for leave operation");
                }
                receiveThread.interrupt();
                Runtime.getRuntime().removeShutdownHook(Chat.chatThread);
                try {
                    s.close();
                } catch (IOException ioException) {
                    Logger.writeErrorToLog("Failed to close socket TCP connection");
                }
                synchronized (Chat.chatWait) {
                    chatWait.notify();
                }

                Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Leaving chatroom \"%s\" at chatroom server \"%s:%d\"",
                        Chat.chatroomName,
                        Chat.hostname,
                        Chat.rmiPort
                ));
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

            String message = textEntry.getText();

            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Attempting to send message \"%s\" to chat server at \"%s:%d\"",
                    message,
                    Chat.hostname,
                    Chat.rmiPort
            ));

            // send message to server via RMI accessor
            try {
                synchronized (Chat.reestablishLock) {
                    chatroomAccessor.getAccess().chat(chatroomName, username, message);
                }
                // set the text of field to blank
                textEntry.setText("");
            } catch (RemoteException | NotBoundException err) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "There was an error sending message \"%s\" to chat server at \"%s:%d\": \"%s\"",
                        message,
                        Chat.hostname,
                        Chat.rmiPort,
                        err.getMessage()
                ));
            }

            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Successfully sent message \"%s\" to chat server at \"%s:%d\"",
                    message,
                    Chat.hostname,
                    Chat.rmiPort
            ));
        }
    }

    static class ReceiveThread implements Runnable {

        private Socket receiveSocket;
        BufferedReader socketReader;

        ReceiveThread(Socket s) {
            this.receiveSocket = s;
        }

        @Override
        public void run() {
            try {
                this.socketReader = new BufferedReader(new InputStreamReader(this.receiveSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true) {
                String message;
                try {
                    while ((message = socketReader.readLine()) != null) {

                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Received message \"%s\" from chat server at \"%s:%d\"",
                                message,
                                Chat.hostname,
                                Chat.tcpPort
                        ));

                        // write display to the main part of the window
                        textDisplay.setText(textDisplay.getText() + "\n" + message);
                    }
                } catch (IOException e) {
                    synchronized (Chat.reestablishLock) {
                        Logger.writeErrorToLog("Lost connection with chatroom server; reestablishing connection...");
                        ChatroomResponse r = null;
                        try {
                            r = centralServer.getAccess().reestablishChatroom(Chat.chatroomName, Chat.username);
                            if (r.getStatus() == ResponseStatus.FAIL) {
                                Logger.writeErrorToLog("Failed to reestablish connection with chatroom");
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
                                Logger.writeErrorToLog("Failed to reestablish TCP connection with chatroom");
                                // exit out of this class
                                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                                return;
                            }

                            this.socketReader = new BufferedReader(new InputStreamReader(this.receiveSocket.getInputStream()));

                        } catch (NotBoundException | IOException err) {
                            Logger.writeErrorToLog("Unable to reestablish chatroom central server; closing window...");
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                            return;
                        }
                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Reestablished connection to chatroom server at \"%s:%d\"",
                                r.getAddress(),
                                r.getRegistryPort()
                        ));
                    }
                }
            }
        }
    }

    private static Socket establishSocket() {
        Socket s = null;
        try {
            s = new Socket(Chat.hostname, Chat.tcpPort);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Sending initial message \"%s:%s\" to chat server at \"%s:%d\"",
                    Chat.chatroomName,
                    Chat.username,
                    Chat.hostname,
                    Chat.tcpPort
            ));

            out.println(Chat.chatroomName + ":" + Chat.username);
            String response = in.readLine();

            if (response.compareTo("success") != 0) {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
        return s;
    }
}