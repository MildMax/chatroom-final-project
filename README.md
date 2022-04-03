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
java -cp . centralserver.App 1234 1235 1236 1237
```

## Starting the data node

```
java -cp . dataserver.App <id> <central hostname> <register port> <hostname> <operations port> <participant port>
```

example for 2 data nodes:

```
java -cp . dataserver.App A localhost 1234 localhost 2234 2235
java -cp . dataserver.App B localhost 1234 localhost 3234 3235
```