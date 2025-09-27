package org.jakuba.model;

import org.jakuba.service.Channel;

import java.util.Arrays;

public class Transmitter {
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

}
