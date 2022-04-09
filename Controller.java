import java.rmi.RemoteException;
import java.util.List;

public class Controller implements ControllerInterface{
  
  private final Appendable out;
  private final Scanner scan;
  private Readable in;

  
  public Controller(Readable in, Appendable out) {
    if (in == null || out == null) {
      throw new IllegalArgumentException("Readable and Appendable can't be null");
    }
    this.out = out;
    this.in = in;
    scan = new Scanner(in);
  }

  @Override
  public void showOperations() {
    
    try {
    out.append("Please select which action would you like to perform\n");

    out.append("1.New User\n");
    out.append("2.Existing User\n");
    option = scan.nextInt();
    if(option == 1) {
      Response registerUser(String username, String password) throws RemoteException;
      break;
    } else if (option.equals(2)) {
      Response login(String username, String password) throws RemoteException;
      break;
    }

    //on sucessfull login or register

    out.append("1.List of chatRooms available\n");
    out.append("2.Create Chat room\n");
    out.append("3 Get existing chat Room\n");
    out.append("4 Delete Chat room\\\n");
    out.append("5.Reestablish chat room\n");
    option = scan.nextInt();

    if (option.equals(1)) {

      List<String> listChatrooms() throws RemoteException;
      break;
    } else if (option.equals(2)) {
      ChatroomResponse createChatroom(String chatroomName, String username) throws RemoteException;
      break;
    } else if (option.equals(3)) {
      ChatroomResponse getChatroom(String chatroomName) throws RemoteException;
      break;
    } else if (option.equals(4)) {
      Response deleteChatroom(String chatroomName, String username, String password) throws RemoteException;
      break;
    } else if (option.equals(5)) {
      ChatroomResponse reestablishChatroom(String chatroomName, String username) throws RemoteException;
      break;
    }

    } catch (IOException e) {

      throw new IllegalArgumentException("Append failed", e);
    }

  }
