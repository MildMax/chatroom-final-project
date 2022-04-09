
public class Driver {
  /**
   * main method to take comandline arguments.
   * @param args commandline arguments.
   */ 

  public static void main(String[] args) {  

    Readable input = new InputStreamReader(System.in);
    Appendable output = System.out;
    ControllerInterface controller = new Controller(input, output);   
    controller.showOperations();
 
    
  }

}