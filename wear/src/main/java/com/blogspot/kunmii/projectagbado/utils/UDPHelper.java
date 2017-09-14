package com.blogspot.kunmii.projectagbado.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by Olakunmi on 27/10/2016.
 */

public class UDPHelper {

    int serverPort = 2407;
    InetAddress serverIP;

    static DatagramSocket datagramSocket;
    long data = 0;

    public UDPHelper( String serverIP, int serverPort) throws SocketException, UnknownHostException {
        if(datagramSocket!=null)
        {
            if(!datagramSocket.isClosed())
                datagramSocket.close();
        }
        datagramSocket = new DatagramSocket();
        this.serverPort = serverPort;
        this.serverIP = InetAddress.getByName(serverIP);
        datagramSocket.connect(this.serverIP,serverPort);
    }

    public DatagramPacket sendMessage(String message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(),message.length(),serverIP,serverPort);
        datagramSocket.send(packet);
        return packet;
    }

    public void closeSocket()
    {
        if(datagramSocket!=null)
        {
            if(!datagramSocket.isClosed())
            datagramSocket.close();
        }
    }

    public boolean isConnected()
    {
        if (datagramSocket == null)
            return false;
        return datagramSocket.isConnected();
    }

}
