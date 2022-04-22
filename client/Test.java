package client;

import data.ChatroomListResponse;
import data.ChatroomResponse;
import data.ICentralUserOperations;
import data.Response;
import util.Logger;
import util.RMIAccess;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

/**
 * Testing file designed to test basic operations between the client and the central server.
 * All output is printed to console for manual verification.
 */
public class Test {

    /**
     * Runs tests on basic operations between the client and the central server
     *
     * @param serverInfo contains information used to contact central server
     * @throws RemoteException if there is an error contacting the central server
     * @throws NotBoundException if the central server user operations interface cannot be found
     */
    public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {
        RMIAccess<ICentralUserOperations> centralServerAccessor = new RMIAccess<>(serverInfo.getCentralHost(),
                serverInfo.getCentralPort(),
                "ICentralUserOperations");

        Logger.writeMessageToLog("Starting test...");

        System.out.println();

        System.out.println("Checking register user : SUCCESS");
        Response r = centralServerAccessor.getAccess().registerUser("sample_user", "sample_password");
        System.out.println(r.getStatus());
        System.out.println(r.getMessage());

        System.out.println();

        System.out.println("Checking duplicate register user : FAIL");
        Response r1 = centralServerAccessor.getAccess().registerUser("sample_user", "sample_password");
        System.out.println(r1.getStatus());
        System.out.println(r1.getMessage());

        System.out.println();

        System.out.println("Checking colon in register user username : FAIL");
        Response r2 = centralServerAccessor.getAccess().registerUser("sample:user", "sample_password");
        System.out.println(r2.getStatus());
        System.out.println(r2.getMessage());

        System.out.println();

        System.out.println("Checking colon in register user password : FAIL");
        Response r3 = centralServerAccessor.getAccess().registerUser("sample_user2", "sample:password");
        System.out.println(r3.getStatus());
        System.out.println(r3.getMessage());

        System.out.println();

        System.out.println("Logging in user : SUCCESS");
        Response r4 = centralServerAccessor.getAccess().login("sample_user", "sample_password");
        System.out.println(r4.getStatus());
        System.out.println(r4.getMessage());

        System.out.println();

        System.out.println("Logging in non-existent user : FAIL");
        Response r5 = centralServerAccessor.getAccess().login("sample_user2", "sample_password");
        System.out.println(r5.getStatus());
        System.out.println(r5.getMessage());

        System.out.println();

        System.out.println("Creating new chatroom : SUCCESS");
        Response r6 = centralServerAccessor.getAccess().createChatroom("sample_chatroom", "sample_user");
        System.out.println(r6.getStatus());
        System.out.println(r6.getMessage());

        System.out.println();

        System.out.println("Verifying chatroom details");
        ChatroomResponse r7 = centralServerAccessor.getAccess().getChatroom("sample_chatroom");
        System.out.println(r7.getStatus());
        System.out.println(r7.getName());
        System.out.println(r7.getAddress());
        System.out.println(r7.getRegistryPort());
        System.out.println(r7.getTcpPort());

        System.out.println();

        System.out.println("Creating second new chatroom : SUCCESS");
        Response r8 = centralServerAccessor.getAccess().createChatroom("sample_chatroom2", "sample_user");
        System.out.println(r8.getStatus());
        System.out.println(r8.getMessage());

        System.out.println();

        System.out.println("Verifying second chatroom details");
        ChatroomResponse r9 = centralServerAccessor.getAccess().getChatroom("sample_chatroom2");
        System.out.println(r9.getStatus());
        System.out.println(r9.getName());
        System.out.println(r9.getAddress());
        System.out.println(r9.getRegistryPort());
        System.out.println(r9.getTcpPort());

        System.out.println();

        System.out.println("Getting list of chatroom names");
        ChatroomListResponse r10 = centralServerAccessor.getAccess().listChatrooms();
        for (String name : r10.getChatroomNames()) {
            System.out.println(name);
        }

        System.out.println();

        System.out.println("Deleting second chatroom : SUCCESS");
        Response r11 = centralServerAccessor.getAccess().deleteChatroom("sample_chatroom2", "sample_user", "sample_password");
        System.out.println(r11.getStatus());
        System.out.println(r11.getMessage());

        System.out.println();

        System.out.println("Getting modified list of chatroom names : MISSING SECOND CHATROOM");
        ChatroomListResponse r12 = centralServerAccessor.getAccess().listChatrooms();
        for (String name : r12.getChatroomNames()) {
            System.out.println(name);
        }

        System.out.println();

        System.out.println("Deleting non-existent chatroom : FAIL");
        Response r13 = centralServerAccessor.getAccess().deleteChatroom("sample_chatroom2", "sample_user", "sample_password");
        System.out.println(r13.getStatus());
        System.out.println(r13.getMessage());

        System.out.println();

        System.out.println("Delete chatroom with bad username : FAIL");
        Response r14 = centralServerAccessor.getAccess().deleteChatroom("sample_chatroom", "bad_user", "sample_password");
        System.out.println(r14.getStatus());
        System.out.println(r14.getMessage());

        System.out.println();

        System.out.println("Delete chatroom with bad password : FAIL");
        Response r15 = centralServerAccessor.getAccess().deleteChatroom("sample_chatroom", "sample_user", "bad_password");
        System.out.println(r15.getStatus());
        System.out.println(r15.getMessage());

        System.out.println();

        System.out.println("Creating duplicate chatroom : FAIL");
        ChatroomResponse r16 = centralServerAccessor.getAccess().createChatroom("sample_chatroom", "sample_user");
        System.out.println(r16.getStatus());
        System.out.println(r16.getMessage());

        System.out.println();

        System.out.println("Getting non-existent chatroom : FAIL");
        ChatroomResponse r17 = centralServerAccessor.getAccess().getChatroom("sample_chatroom2");
        System.out.println(r17.getStatus());
        System.out.println(r17.getMessage());

        System.out.println();

        System.out.println("Creating chatroom with \":\" in name : FAIL");
        ChatroomResponse r18 = centralServerAccessor.getAccess().createChatroom("sample:chatroom", "sample_user");
        System.out.println(r18.getStatus());
        System.out.println(r18.getMessage());

        System.out.println();

        System.out.println("Stress-testing 2PC on registerUser");
        List<Thread> threadList = new LinkedList<>();
        for (int i = 0; i < 5; ++i) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        centralServerAccessor.getAccess().registerUser("sample_user3", "sample_password");
                    } catch (RemoteException | NotBoundException e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
            t.start();
            threadList.add(t);
        }

        for (Thread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.println();

        System.out.println("Stress-testing 2PC on createChatroom");

        threadList.clear();
        for (int i = 0; i < 5; ++i) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        centralServerAccessor.getAccess().createChatroom("chatroom3", "sample_user");
                    } catch (RemoteException | NotBoundException e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
            t.start();
            threadList.add(t);
        }

        for (Thread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.println();

        System.out.println("Stress-testing 2PC on deleteChatroom");

        threadList.clear();
        for (int i = 0; i < 5; ++i) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        centralServerAccessor.getAccess().deleteChatroom("chatroom3", "sample_user", "sample_password");
                    } catch (RemoteException | NotBoundException e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
            t.start();
            threadList.add(t);
        }

        for (Thread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        Logger.writeMessageToLog("Finished testing");

    }
}
