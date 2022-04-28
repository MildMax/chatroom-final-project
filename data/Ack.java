package data;

/**
 * Defines the type of acknowledgements a coordinator may receive during 2 Phase Commit.
 */
public enum Ack {

    YES, // confirms requested operation succeeded
    NO, // confirms requested operation has failed
    NA // status of requested operation is not available
}
