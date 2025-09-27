package org.jakuba.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Channel {
    private final List<Consumer<int[]>> subscribers = new CopyOnWriteArrayList<>();

    // Суммарный сигнал в эфире
    private List<int[]> buffer = new ArrayList<>();

    // Подписка
    public void subscribe(Consumer<int[]> subscriber) {
        subscribers.add(subscriber);
    }

    // Передатчики публикуют свой сигнал (он сохраняется в буфере)
    public synchronized void publish(int[] codedSignal) {
        if (!buffer.isEmpty() && codedSignal.length != buffer.get(0).length) {
            throw new IllegalArgumentException("Все сигналы должны иметь одинаковую длину!");
        }
        buffer.add(codedSignal);
    }

    // Когда все передатчики отдали сигнал,
    // складываем их и отправить в эфир
    public synchronized void broadcast() {
        if (buffer.isEmpty()) return;

        int length = buffer.get(0).length;
        int[] combined = new int[length];

        for (int[] signal : buffer) {
            for (int i = 0; i < length; i++) {
                combined[i] += signal[i];
            }
        }

        System.out.println("Полученный сигнал на канале: " + Arrays.toString(combined));

        // Рассылаем всем подписчикам
        for (Consumer<int[]> subscriber : subscribers) {
            subscriber.accept(combined);
        }

        buffer.clear(); // очищаем эфир
    }
}
