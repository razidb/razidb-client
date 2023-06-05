package org.example;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import picocli.CommandLine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;


enum Operation {
    SELECT('s'),
    INSERT('i'),
    UPDATE('u'),
    DELETE('d');

    public char label;

    Operation(char label) {
        this.label = label;
    }
}

@CommandLine.Command(name = "razidb-shell", version = "1.0", mixinStandardHelpOptions = true)
public class Shell implements Runnable{
    /*
     * Interactive shell command line for database
     * */

    @CommandLine.Option(names = { "-u", "--username" }, description = "Username", required = true)
    String username;
    @CommandLine.Option(names = { "-p", "--password" }, description = "Password", required = true)
    String password;
    @CommandLine.Option(names = { "-h", "--host" }, description = "Host url", defaultValue = "localhost:4999")
    String host;

    @CommandLine.Option(names = { "-d", "--database" }, description = "Database")
    String database = "";
    @CommandLine.Option(names = { "-c", "--collection" }, description = "Collection")
    String collection = "";

    char dataType = 's';
    char operationType;

    // request related variables
    String reqData;
    byte[] reqDataBytes;
    // response related variables
    String resData;
    int resStatusCode;

    private DataOutputStream out;
    private DataInputStream in;

    boolean authenticated = false;

    JSONParser jsonParser;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Shell()).execute(args);
        System.exit(exitCode);
    }

    public Shell(){}


    @Override
    public void run() {
        /* Main function where command is executed */
        Socket socket;

        try {
//            socket = new Socket("46.101.116.182", 4999);
            socket = new Socket("localhost", 4999);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // try to authenticate the user
        try{
            requestToConnect();
            readInputStream();
        }
        catch (IOException exception){
            System.out.println("failed to authenticate: " + exception.getMessage());
        }

        if (resStatusCode == 401){
            System.out.println("access denied");
            System.exit(401);
        }
        else{
            System.out.println(username + " was authenticated");
        }

        // init JSON parser
        jsonParser = new JSONParser();
        Scanner sc= new Scanner(System.in);
        JSONObject payload = null;

        while (true){
            try{
                // get user input
                String s = sc.nextLine();
                // parse user input
                // [database].[collection].[all/find/count][({query})] / [insertOne/insertMany/update/delete][(data)]
                String[] command = s.split("\\.");
                database = command[0];
                collection = command[1];

                String finalPart = command[2];
                String[] parts = finalPart.split("\\(");
                String operation = parts[0];
                String query = parts[1].split("\\)").length > 0? parts[1].split("\\)")[0]: null;

                // log query
                System.out.println("Query: " + query);

                String mainOperation = null;
                JSONObject jsonQuery = new JSONObject();

                try{
                    jsonQuery = (JSONObject) jsonParser.parse(query);
                } catch (Exception e){
                    System.out.println("Error while parsing query");
                    System.out.println(e.getMessage());
                }

                // Selection Operation Related Queries
                if (Objects.equals(operation, "all") || Objects.equals(operation, "find")){
                    operationType = Operation.SELECT.label;
                    payload = jsonQuery;
                }
                else if (Objects.equals(operation, "insertOne") || Objects.equals(operation, "insertMany")){
                    operationType = Operation.INSERT.label;

                    payload.put("type", "document");
                    payload.put("payload", jsonQuery);
                }
                else if (Objects.equals(operation, "update")){
                    operationType = Operation.UPDATE.label;
                    payload = new JSONObject();
                    payload.put("type", "document");
                    payload.put("payload", jsonQuery);
                }
                else if (Objects.equals(operation, "delete")){
                    operationType = Operation.DELETE.label;
                    payload = new JSONObject();
                    payload.put("type", "document");
                    payload.put("payload", jsonQuery);
                }
                // index-related queries
                else if (Objects.equals(operation, "createIndex")) {
                    operationType = Operation.INSERT.label;
                    payload = new JSONObject();
                    payload.put("type", "index");
                    payload.put("payload", jsonQuery.get("fields"));
                }
                else if (Objects.equals(operation, "deleteIndex")) {
                    operationType = Operation.DELETE.label;
                    payload = new JSONObject();
                    payload.put("type", "index");
                    payload.put("payload", jsonQuery.get("fields"));
                }

                try {
                    switch (operationType){
                        case 's':
                            handleSelectQueryReq(payload);
                            break;
                        case 'i':
                            handleInsertQueryReq(payload);
                            break;
                        case 'u':
                            handleUpdateQueryReq(payload);
                            break;
                        case 'd':
                            handleDeleteQueryReq(payload);
                            break;
                        case 'e':
                            handleEndConnection();
                            // end connection
                            break;
                        default: {
                            System.out.println("ERROR: Unrecognized Operation");
                            break;
                        }
                    }
                    // Get the response from db and print it
                    readInputStream();
                    System.out.println("Query Result: ");
                    System.out.println(resData);

                } catch (Exception e) {
                    System.out.println("Something went wrong");
                    // System.out.println(e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            catch (Exception e){
                System.out.println("something went wrong" + e.getMessage());
            }

        }


    }

    // helper functions
    void readInputStream() {
        try{
            char resContentType = in.readChar();
            int resContentLength = in.readInt();
            resStatusCode = in.readInt();

            byte[] messageByte = new byte[resContentLength];
            boolean end = false;
            StringBuilder dataString = new StringBuilder(resContentLength);
            int totalBytesRead = 0;

            while(!end) {
                int currentBytesRead = in.read(messageByte);
                totalBytesRead = currentBytesRead + totalBytesRead;
                if(totalBytesRead <= resContentLength) {
                    dataString.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                } else {
                    dataString.append(
                            new String(
                                    messageByte,
                                    0,
                                    resContentLength - totalBytesRead + currentBytesRead,
                                    StandardCharsets.UTF_8
                            )
                    );
                }
                if(dataString.length()>=resContentLength) {
                    end = true;
                }
            }
            resData = dataString.toString();
        }
        catch (IOException exception){
            resStatusCode = 500;
            resData = exception.getMessage();
            System.out.println("Something went wrong: " + exception);
        }

    }

    void requestToConnect() throws IOException {
        if (authenticated)
            return;

        operationType = 'c'; // c for connect
        reqData = "{" +
            "\"username\": \"" + username + "\", " +
            "\"password\": \"" + password + "\"" +
        "}";
        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }

    void handleSelectQueryReq(JSONObject payload) throws IOException {
        reqData = "{" +
//            "\"type\": \"document\", " +
            "\"database\": \"" + database + "\"," +
            "\"collection\": \"" + collection + "\"," +
            "\"payload\":" + payload.toJSONString() +
        "}";

        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }
    void handleUpdateQueryReq(JSONObject payload) throws IOException {
        reqData = "{" +
            "\"type\": \"" + payload.get("type") + "\", " +
            "\"database\":" + database + "," +
            "\"collection\":" + collection + "," +
            "\"payload\":" + new JSONObject((Map) payload.get("payload")).toJSONString() +
        "}";

        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }
    void handleInsertQueryReq(JSONObject payload) throws IOException {
        reqData = "{" +
            " \"type\": \"" + payload.get("type") + "\", " +
            "\"database\":" + database + "," +
            "\"collection\":" + collection + "," +
            "\"payload\":" + payload.get("payload") +
        "}";

        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }
    void handleDeleteQueryReq(JSONObject payload) throws IOException {
        reqData = "{" +
            " \"type\":\n" + payload.get("type") + "\n, " +
            "\"database\":" + database + "," +
            "\"collection\":" + collection + "," +
            "\"payload\":" + payload.get("payload") +
        "}";

        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }
    void handleEndConnection() throws IOException {
        reqData = "Peace, DB!";
        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }
}
