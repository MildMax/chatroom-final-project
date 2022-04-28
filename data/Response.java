package data;

import java.io.Serializable;

/**
 * A generic response object containing the status of an operation and a message describing an operation
 */
public class Response implements Serializable {
  private final ResponseStatus status;
  private final String message;

  /**
   * Creates and instance of the Response object
   *
   * @param status the status of an operation
   * @param message a message describing the result of an operation
   */
  public Response(ResponseStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  /**
   * Gets the status of the operation
   *
   * @return the status of the operation
   */
  public ResponseStatus getStatus() {
    return this.status;
  }

  /**
   * Gets the message describing the result of the operation
   *
   * @return the message describing the result of the operation
   */
  public String getMessage() {
    return this.message;
  }
}
