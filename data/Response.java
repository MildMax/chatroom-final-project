package data;

import java.io.Serializable;

/**
 * centralized response class which is use dto obtain sttaus and message.
 *
 */
public class Response implements Serializable {
  private final ResponseStatus status;
  private final String message;

  public Response(ResponseStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  /**
   * status of the response.
   * @return the status of the response.
   */
  public ResponseStatus getStatus() {
    return this.status;
  }

  /**
   * to get current messahe.
   * @return the current message.
   */
  public String getMessage() {
    return this.message;
  }
}
