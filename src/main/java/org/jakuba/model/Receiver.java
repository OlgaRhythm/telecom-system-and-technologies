package org.jakuba.model;

import org.jakuba.service.Channel;

import java.util.ArrayList;
import java.util.List;

public class Receiver {
    private final int id;
    private final int[] walshCode;
    private final Channel channel;

    public Receiver(int id, int[] walshCode, Channel channel) {
        this.id = id;
        this.walshCode = walshCode;
        this.channel = channel;

        // Подписываемся на общий эфир
        channel.subscribe(this::onSignalReceived);
    }

    private void onSignalReceived(int[] combinedSignal) {
        String decoded = decode(combinedSignal);
        System.out.println("Receiver " + id + " получил сообщение: " + decoded);
    }

    private String decode(int[] signal) {
        List<Integer> bits = new ArrayList<>();
        int blockSize = walshCode.length;

        for (int i = 0; i < signal.length; i += blockSize) {
            int sum = 0;
            for (int j = 0; j < blockSize; j++) {
                sum += signal[i + j] * walshCode[j];
            }
            bits.add(sum > 0 ? 1 : 0);
        }

        System.out.println("Receiver " + id + " обработал биты сообщения кодом и нашел суммы: " + bits);


        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.size(); i += 8) {
            int ascii = 0;
            for (int j = 0; j < 8 && i + j < bits.size(); j++) {
                ascii = (ascii << 1) | bits.get(i + j);
            }
            sb.append((char) ascii);
        }

        return sb.toString();
    }
}
