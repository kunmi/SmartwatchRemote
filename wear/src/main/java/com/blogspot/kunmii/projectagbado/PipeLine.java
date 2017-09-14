package com.blogspot.kunmii.projectagbado;

import com.blogspot.kunmii.projectagbado.utils.UDPHelper;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Olakunmi on 02/02/2017.
 */

public class PipeLine{
    BlockingQueue<String> queue = new ArrayBlockingQueue(1000);
    boolean start = false;
    long rest = 0;
    UDPHelper udpHelper;

    private Thread consumptionThread;

    public PipeLine(UDPHelper udpHelper)
    {
        this.udpHelper = udpHelper;
    }


    public void queData(String data)
    {
        try {
            queue.add(data);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void startConsuming(){
        start = true;

        if(consumptionThread != null)
        {
            consumptionThread.interrupt();
        }
        consumptionThread = new Thread(runnable);
        consumptionThread.start();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
                String item;
                try {
                    while(start)
                    {
                        item = queue.take();
                        if(item != null)
                        {
                            try {
                                if(udpHelper!=null)
                                udpHelper.sendMessage(item);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Thread.sleep(rest);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

    };
}
