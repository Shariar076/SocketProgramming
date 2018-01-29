/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Shariar Kabir
 */
public class FileServer implements Runnable {

    /**
     * @param args the command line arguments
     */
    public final int buffersize = 40000;//153176;
    public static FileServer thisServer = new FileServer();
    public static ArrayList<User> userList = new ArrayList<>();
    public static ArrayList<User> recipients = new ArrayList<>();
    public static ArrayList<Files> files = new ArrayList<>();
    public static byte[] buffer;
    public static int bufferEnd;
    Socket currentSocket;

    BufferedReader inToServer;
    DataOutputStream outStream;
    DataInputStream inStream;
    int id = 0;

    public FileServer(Socket sock) {
        try {
            currentSocket = sock;
            inToServer = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));
            outStream = new DataOutputStream(currentSocket.getOutputStream());
            inStream = new DataInputStream(currentSocket.getInputStream());
            buffer = new byte[153176];
            bufferEnd = 0;
        } catch (IOException ex) {
            System.out.println("server.FileServer.<init>()");
        }
    }

    private FileServer() {

    }

    public String userName(Socket socket) {
        for (User u : userList) {
            if (u.socket == socket) {
                return u.userID;
            }
        }
        return null;
    }

    public boolean userExists(String user, ArrayList<User> alist) {
        for (User u : alist) {
            if (u.userID.equals(user)) {
                return true;
            }
        }
        return false;
    }

    public Socket getSocket(String user) {
        for (User u : userList) {
            if (u.userID.equals(user)) {
                return u.socket;
            }
        }
        return null;
    }

    public void printUsers(ArrayList<User> aList) {

        for (User u : aList) {
            System.out.println(u.userID);
        }

    }

    public void writeFile(String fileName, int startInd, int filesize) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            fos.write(buffer, startInd, filesize);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getStartIndex(String file) {
        for (Files f : files) {
            if (f.fileName.equals(file)) {
                return f.startInd;
            }

        }

        return -1;
    }

    public String getFileName(String receiver) {
        for (Files f : files) {
            if (f.receiver.equals(receiver)) {
                return f.fileName;
            }

        }

        return null;
    }

    public long getFileSize(String fileName) {
        for (Files f : files) {
            if (f.fileName.equals(fileName)) {
                return f.fileSize;
            }

        }

        return -1;
    }

    public void removeUser(String userID, ArrayList<User> aList) {
        for (int i = 0; i < aList.size(); i++) {
            if (aList.get(i).userID.equals(userID)) {
                aList.remove(i);
            }
        }
    }

    public void removeFile(String fileName) {

        int stInd = getStartIndex(fileName);
        long size = getFileSize(fileName);
        for (int i = stInd; i < buffersize - size; i++) {
            buffer[i] = buffer[i + (int) size];
        }
        bufferEnd -= size;
        int fl = 0;
        for (int i = 0; i < files.size(); i++) {
            if (fl == 1) {
                System.out.println("was : " + files.get(i).startInd);
                files.get(i).startInd -= size;
                System.out.println("now : " + files.get(i).startInd);
            }
            if (files.get(i).fileName.equals(fileName)) {
                fl = 1;
            }
        }
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).fileName.equals(fileName)) {
                files.remove(i);
            }
        }
    }

    public void getFile(Socket socket) throws IOException {

        String recipientID = inToServer.readLine();
        System.out.println("Reciever ID " + recipientID);
        printUsers(userList);
        if (userExists(recipientID, userList)) {
            outStream.writeBytes("1" + '\n');

            String filename = inStream.readLine();
            long filesize = inStream.readLong();
            System.out.println("FileName" + filename + "filesize : " + filesize);

            if (bufferEnd + filesize < buffersize) {
                outStream.writeBytes("1" + '\n');
                recipients.add(new User(recipientID, getSocket(recipientID)));
                files.add(new Files(filename, recipientID, bufferEnd, filesize));

                int read = 0;
                Random random = new Random();
                int min =(int) (filesize/10);
                int chunkSize = random.nextInt(50) + min;;
                outStream.writeInt(chunkSize);
                
                long remaining = filesize;
                while ((read = inStream.read(buffer, bufferEnd, (int) Math.min(chunkSize, remaining))) > 0) {
                    String st = inStream.readLine();
                    System.out.println(st);
                    bufferEnd += read;
                    remaining -= read;
                }
                String s = inStream.readLine();
                System.out.println("Client says" + s);

                System.out.println("File recieved.");
                System.out.println("Total size: " + bufferEnd);

                String st = inStream.readLine();
                System.out.println("Client says" + st);
            } else {
                outStream.writeBytes("0" + '\n');
            }

        } else {
            outStream.writeBytes("0" + '\n');
        }

    }

    public void sendFile(Socket s) throws IOException {

        outStream = new DataOutputStream(s.getOutputStream());
        String receiver = userName(s);
        if (userExists(receiver, recipients)) {

            outStream.writeBytes("1" + '\n');

            String fileName = getFileName(receiver);

            int stInd = getStartIndex(fileName);
            int size = (int) getFileSize(fileName);
            byte[] sendbuffer = Arrays.copyOfRange(buffer, stInd, stInd + size);

            outStream.writeBytes(fileName + '\n');
            outStream.writeLong(size);
            String msg = inStream.readLine();
            System.out.println("msg in server "+msg);
            if (msg.equals("Y") || msg.equals("y")) {
                outStream.write(sendbuffer);
                outStream.flush();
                outStream.writeBytes("ok" + '\n');

                System.out.println("Sending "+fileName+" complete");
                removeFile(fileName);
                removeUser(receiver, recipients);
                outStream.writeBytes("Done" + '\n');
            } else if(msg.equals("N") || msg.equals("n")) {
                removeFile(fileName);
            }

        } else {
            outStream.writeBytes("0" + '\n');
        }
    }

    @Override
    public void run() {
        String clientID = null;
        String actDecision = null;

        try {
            inToServer = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));
            outStream = new DataOutputStream(currentSocket.getOutputStream());
            clientID = inToServer.readLine();
            if (userExists(clientID, userList)) {
                outStream.writeBytes("0" + '\n');
            } else {
                outStream.writeBytes("1" + '\n');
                userList.add(new User(clientID, currentSocket));
                printUsers(userList);
                while (true) {
                    actDecision = inToServer.readLine();
                    if (actDecision.equals("S") || actDecision.equals("s")) {
                        getFile(currentSocket);
                    } else if (actDecision.equals("R") || actDecision.equals("r")) {
                        sendFile(currentSocket);
                    } else {
                        System.out.println("...........");
                        //removeUser(clientID, userList); //Remove the user if any malicious command is found
                        //removeUser(clientID, recipients);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client " + userName(currentSocket) + " has left");
            removeUser(clientID, userList);
            removeUser(clientID, recipients);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws IOException {

        ServerSocket server = new ServerSocket(7789);
        while (true) {
            Socket current = server.accept();

            Thread t = new Thread(new FileServer(current));
            t.start();

            System.out.println("Client [" + thisServer.id + "] is now connected.");

            thisServer.id++;

        }
    }

}
