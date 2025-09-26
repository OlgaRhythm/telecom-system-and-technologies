package org.jakuba.model;

import org.jakuba.service.Channel;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class Transmitter extends Thread{
    final private int id;
    final private String word;
    final private int[] walshCode;

    final private Channel channel;

    public Transmitter(int id, String word, int[] walshCode, Channel channel) {
        this.id = id;
        this.word = word;
        this.walshCode = walshCode;
        this.channel = channel;
    }

    private int[] convertFromASCIIToBinCode() {
        return word.chars()
                .flatMap(
                        n -> {
                            String binary = String.format("%8s", Integer.toBinaryString(n))
                                    .replace(' ', '0');
                            return binary.chars().map(Character::getNumericValue);
                        }
                ).toArray();
    }

    private int[] convertByWalshCode() {
        return Arrays.stream(convertFromASCIIToBinCode())
                .flatMap(n -> Arrays.stream(walshCode)
                                    .map(x -> (n == 1) ? x : -x))
                .toArray();
    }

    public void transmit() {
        int[] codedSignal = convertByWalshCode();
        channel.publish(codedSignal);
    }

    @Override
    public void run() {
        transmit();
        Random random = new Random();
        int timeToWait = random.nextInt();

        try {
            Thread.sleep(timeToWait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
