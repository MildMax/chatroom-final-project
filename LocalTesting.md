# Local Testing

This file shows significant log statements that show how our application handles key
algorithms and behavior for our application, how our application handles errors or invalid
input, and how the client can interact with our application. This testing was conducted
locally for simplicity.

All tests were conducted on the minimum number of components required to showcase
the important aspects of our chatroom application. As such, these tests are run on
two chat servers, two data servers, one central server, and two clients.

All images are available for further inspection under the `images` folder associated
with this submission.

Our demo video associated with this submission will show more clearly how a user can 
interact with our application.

## Normal Operation

This screenshot shows what the application looks like when run from a single machine:

![](images/normal_run.png)

## Four Main Algorithms

### Cristian's Algorithm

Cristian's algorithm may be verified in the `CristiansLogger.java` file included in this
submission. This file may be found in the `util` package.

**Central Server**

![](images/central_server_cristians.png)

**Data Server**

![](images/data_server_cristians.png)

**Chat Server**

![](images/chat_server_cristians.png)

### Pub/Sub

Our Pub/Sub algorithm may be verified in the `ConnectChatroom.java`, `ChatroomUserOperations.java`,
and `Chatroom.java` files included in this submission. These files may be found in the `chatserver` package.

These log files may be found in the chat server logs produced during execution of the program.

**Publishing**

![](images/chat_server_publish.png)

**Subscribing**

![](images/chat_server_subscribe.png)

**Unsubscribing**

![](images/chat_server_unsubscribe.png)

### 2 Phase Commit

**Create User**

![](images/2pc_create_user.png)

**Create Chatroom**

![](images/2pc_create_chatroom.png)

**Delete Chatroom**

![](images/2pc_delete_chatroom.png)

**Log Message**

![](images/2pc_log_message.png)

**Under Contention**

While it is hard to force conflicts during 2 phase commit from a single machine, we have a test file
that showcases how our server handles conflicting keys on transactions during 2 phase commit, and how
it handles issuing doAbort commands. Thus, for multiple users attempting to create the same username
at the same time, we can see how our system issues doAbort requests to participants when there is a 
collision between client requests:

![](images/2pc_under_contention.png)

### Data Replication

Each data server in the system will store data in a folder `files_<id>` where `<id>` is the
unique ID used to start the data server. The following images are pulled from `files_A`
and `files_B` for data nodes `A` and `B` respectively.

**files_<id>**

![](images/replicated_data_file_structure.png)

**users.txt**

![](images/replicated_data_users.png)

**chatroom.txt**

![](images/replicated_data_chatrooms.png)

**chatLogs directory**

![](images/replicated_data_chatlogs.png)

## Re-Establishing a Connection

Re-establishing a connection takes advantage of our load balancing behavior, which
can be examined in the `innerCreateChatroom` method in the `CentralUserOperations.java` file
under the `centralserver` package in this submission.

![](images/reestablish_connection.png)

## Client Interface

### Log in

**Success**

![](images/user_login_success.png)

**Non-Existent User**

![](images/login_nonexistent_user.png)

**Bad Password**

![](images/login_bad_password.png)

### Create User

**Success**

![](images/create_user_success.png)

**Invalid Username**

![](images/create_user_bad_username.png)

**Invalid Password**

![](images/create_user_bad_password.png)

**Duplicate Username**

![](images/create_user_duplicate_username.png)

### Joining Chatroom

**Success**

![](images/join_chatroom_success.png)

**Invalid Chatroom**

![](images/join_chatroom_bad_chatroom_name.png)

### Getting Available Chatrooms

![](images/available_chatrooms.png)

### Creating a Chatroom

**Success**

![](images/create_chatroom_success.png)

**Duplicate Chatroom**

![](images/create_chatroom_duplicate_chatroom_name.png)

**Bad Chatroom Name**

![](images/create_chatroom_bad_chatroom_name.png)

### Deleting a Chatroom

**Success**

![](images/delete_chatroom_success.png)

**Non-Existent Chatroom**

![](images/delete_chatroom_doesnt_exist.png)

**User does not own Chatroom**

![](images/delete_chatroom_unauthorized.png)

### Exit

**Prompt 1**

![](images/exit_prompt_1.png)

**Prompt 2**

![](images/exit_prompt_2.png)

### Invalid Option

**Prompt 1**

![](images/prompt_1_bad_option.png)

**Prompt 2**

![](images/prompt_2_bad_option.png)

## Invalid Server arguments

### Central Server

![](images/central_server_bad_command_line_args.png)

### Chat Server

![](images/chat_server_bad_command_line_args.png)

Note: The logger used for the Chat Server requires the ID for the server be parsed
to generate the log file. Thus, while parsing command line arguments, if an error
occurs, the chat server will instead print the error to the command line.

### Data Server

![](images/data_server_bad_command_line_args.png)

Note: The logger used for the Data Server requires the ID for the server be parsed
to generate the log file. Thus, while parsing command line arguments, if an error
occurs, the chat server will instead print the error to the command line.

### Client

![](images/client_bad_command_line_args.png)