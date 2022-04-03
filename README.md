# chatroom-final-project
Final project for CS6650 for the chatroom

## Compiling the code

```
javac -cp . centralserver/*.java chatserver/*.java client/*.java data/*.java dataserver/*.java util/*.java
```

## Starting the central server

```
java -cp . centralserver.App <register port> <chatroom port> <user port> <coordinator port>
```

example:

```
java -cp . centralserver.App 1111 1112 1113 1114
```

## Starting the data node

```
java -cp . dataserver.App <id> <central hostname> <register port> <hostname> <operations port> <participant port>
```

example for 2 data nodes:

```
java -cp . dataserver.App A localhost 1111 localhost 2222 2223
java -cp . dataserver.App B localhost 1111 localhost 3333 3334
```

## Starting the chat node

```
java -cp . chatserver.App <id> <central hostname> <register port> <hostname> <tcp port> <rmi port> <operations port>
```

example for 4 chat server nodes:

```
java -cp . chatserver.App A localhost 1111 localhost 4444 4445 4446
java -cp . chatserver.App B localhost 1111 localhost 5555 5556 5557
java -cp . chatserver.App C localhost 1111 localhost 6666 6667 6668
java -cp . chatserver.App D localhost 1111 localhost 7777 7778 7779
```