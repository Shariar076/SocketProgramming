package fileclient;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class FileClient {

    Socket clientSocket;
    String UserID;
    String recieverID;
    BufferedReader inFromUser;
    BufferedReader inFromServer;
    DataOutputStream outStream;
    DataInputStream inStream;

    public FileClient(String host, int port) {
        try {
            clientSocket = new Socket(host, port);

            inFromUser = new BufferedReader(new InputStreamReader(System.in));
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outStream = new DataOutputStream(clientSocket.getOutputStream());
            inStream = new DataInputStream(clientSocket.getInputStream());
            System.out.println("UserID : ");
            UserID = inFromUser.readLine();
            outStream.writeBytes(UserID + '\n');

            String msg = inFromServer.readLine();
            if (msg.equals("1")) {
                System.out.println("User Connected...");
                while (true) {
                    System.out.println("Press S to Send R to receive");
                    String dec = inFromUser.readLine();

                    if (dec.equals("S") || dec.equals("s")) {
                        outStream.writeBytes(dec + '\n');
                        sendFile(clientSocket);
                    } else if (dec.equals("R") || dec.equals("r")) {
                        outStream.writeBytes(dec + '\n');
                        getFile(clientSocket);
                    } else {
                        System.out.println("Undefined command");
                    }
                }

            } else {
                System.out.println("User already logged in");
            }

        } catch (IOException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getFile(Socket clientSock) throws IOException {

        inStream = new DataInputStream(clientSock.getInputStream());
        String msg = inStream.readLine();

        if (msg.equals("1")) {

            String fileName = inStream.readLine();
            long filesize = inStream.readLong();

            byte[] getBuffer = new byte[(int) filesize];
            

            System.out.println("FileName: " + fileName + " FileSize: " + filesize);
            System.out.println("Receive(Y/n)?");
            String rep = inFromUser.readLine();
            outStream.writeBytes(rep + '\n');
            if (rep.equals("Y") || rep.equals("y")) {
                FileOutputStream fos = new FileOutputStream(new File("Server" + fileName));
                int read = 0;
                int totalRead = 0;
                read = inStream.read(getBuffer, 0, (int) filesize);
                System.out.println("read " + read + " bytes.");
                fos.write(getBuffer, 0, read);
                fos.close();
                System.out.println("Recieved");

                String no = inStream.readLine();
                System.out.println("server says" + no);
                String st = inStream.readLine();
                System.out.println("server says" + st);
            }

        } else {
            System.out.println("No File is sent to you.");
        }
    }

    public void sendFile(Socket s) throws IOException {

        System.out.println("Reciever ID:");
        recieverID = inFromUser.readLine();
        outStream.writeBytes(recieverID + '\n');
        String msg = inStream.readLine();

        if (msg.equals("1")) {
            System.out.println("Insert filename: ");
            String filename = inFromUser.readLine();

            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);

            outStream.writeBytes(filename + '\n');
            outStream.writeLong(file.length());
            String msg2 = inStream.readLine();
            if (msg2.equals("1")) {
                int chunkSize = inStream.readInt();
                System.out.println("Chunk size: " + chunkSize);
                byte[] buffer = new byte[chunkSize];

                int sent = 0;
                while (fis.read(buffer) > 0) {
                    sent += buffer.length;
                    outStream.write(buffer);
                    outStream.writeBytes("chunk received" + '\n');
                    System.out.println("sent : " + sent);
                }
                fis.close();
                outStream.writeBytes("ok" + '\n');
                outStream.writeBytes("sent" + '\n');
            } else {
                System.out.println("Server cannot take any more files");
            }

        } else {
            System.out.println(msg);
            System.out.println("Receipient is Offline");
        }
    }

    public static void main(String argv[]) throws Exception {
        FileClient tc = new FileClient("localhost", 7789);
    }
}
