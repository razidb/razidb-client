## Introduction

This is a simple command line client for a RaziDB (A document database). This database can live on any server.
To be able to run this code and test RaziDB, I have set-up a public database server that is publicly accessible.

The CLI will automatically connect to the remote server unless changed.

## How to run this java application?

There are multiple ways you can run a java application, here you have 2 options: using an IDE or run a jar file from command line.

Let us see how to start the program.

### Start Program From IDE

As this is a CLI application, there are 2 arguments that should be added to the command running this code in you IDE. If you are
using IntelliJ IDEA, you can navigate to the run tab in the bottom bar and
change the Run Configuration. 

Add the following text to the program arguments field: `-u admin -p 123`

If you are using different IDE, please refer to your IDE's documentation and find where you can change the program arguments.

### Start Program From Command Line using Jar file (For Linux)

You need:
- Java-18 SDK installed
- Know where the Java-18 lives
- Run the following: `export JAVA_HOME=[path to Java-18 SDK]`

Now, you have everything ready, you start the program by running

`$JAVA_HOME/bin/java -jar out/artifacts/razidb_client_jar/razidb-client.jar -u admin -p 123` 



Now, You should be authenticated and allowed into an interactive shell.

Here are a couple of queries to run against a database named `maindb` and a collection named `students`:

## Read Operation

`maindb.students.find({"status":"A"})`

## Create Operation
`maindb.students.insertOne({"name":"Test User", "status": "I", "faculty": "KASIT"})`

