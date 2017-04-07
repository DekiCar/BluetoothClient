package com.example.dejan.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class ConnectionThread extends Thread {

    public static final int CONNECTION_SECURE = 1;
    public static final int CONNECTION_INSECURE = 2;

    private DataReceive receiver;

    private boolean running;

    private BluetoothDevice device;
    private BluetoothSocket socket;

    private OutputStream outputStream;
    private InputStream inputStream;

    public ConnectionThread(){
    }

    public synchronized boolean connectTo(BluetoothDevice device, UUID uuid, int CONN_TYPE) throws Exception{

        this.device = device;


        if(CONN_TYPE == CONNECTION_INSECURE){
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        }else if(CONN_TYPE == CONNECTION_SECURE){
            socket = device.createRfcommSocketToServiceRecord(uuid);
        }

        running = true;

        socket.connect();

        outputStream = socket.getOutputStream();
        outputStream.flush();


        inputStream = socket.getInputStream();

        receiver = null;

        return true;

    }

    public void setReceiver(DataReceive receiver){
        this.receiver = receiver;
    }

    public synchronized boolean disconnect(){

        try{
            outputStream.flush();
            outputStream.close();


            inputStream.close();

            running = false;

            socket.close();
        }catch(Exception e){
            return false;
        }
        return true;
    }

    @Override
    public void run() {

        byte [] buffer = new byte[4];
        int bufferPointer = 0;


        while(running && socket.isConnected()){

            try {
                if (inputStream.available() > 0) {

                    buffer[bufferPointer] = (byte)inputStream.read();
                    bufferPointer++;

                    if(receiver != null && bufferPointer == 4){
                        receiver.dataReceived(buffer);
                        bufferPointer = 0;
                    }

                }

            }catch(Exception e){}
        }


    }

    public void write(byte[] buff){
        try {
            outputStream.write(buff);
        } catch (IOException e) {
        }
    }

    public synchronized void writeFormat(byte code, byte value){
        byte [] buffer = new byte[2];
        buffer[0] = code;
        buffer[1] = value;
        try {
            outputStream.write(buffer);
        }catch(IOException e){
        }
    }

    public synchronized BluetoothDevice getCurrentDevice(){
        return device;
    }

    public boolean isConnected(){
        return socket.isConnected();
    }
}
