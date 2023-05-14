package org.example;

import picocli.CommandLine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

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

    @Override
    public void run() {
        /* Main function where command is executed */

        // try to authenticate the user
        try{
            requestToConnect();
        }
        catch (IOException exception){
            System.out.println("failed to authenticate: " + exception.getMessage());
        }

        if (resStatusCode == 401){
            System.out.println("access denied");
            System.exit(401);
        }

        while (true){
            // read input
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
        }

    }

    void requestToConnect() throws IOException {
        if (authenticated)
            return;

        operationType = 'c'; // c for connect
        reqData = "{" +
                "\"username\":\"admin\", " +
                "\"password\": \"123\"," +
                "}";
        reqDataBytes = reqData.getBytes(StandardCharsets.UTF_8);
        out.writeChar(dataType);
        out.writeChar(operationType);
        out.writeInt(reqDataBytes.length);
        out.write(reqDataBytes);
        out.flush();
    }

    void handleSelectQueryReq(){
        // TODO: implement functionality
    }
    void handleUpdateQueryReq(){
        // TODO: implement functionality
    }
    void handleCreateQueryReq(){
        // TODO: implement functionality
    }
    void handleDeleteQueryReq(){
        // TODO: implement functionality
    }
}
