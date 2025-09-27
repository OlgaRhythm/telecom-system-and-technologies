package org.jakuba.service;

import org.jakuba.model.Receiver;
import org.jakuba.model.Transmitter;
import org.jakuba.model.WalshCodeGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Network — создает и запускает набор Transmitter и Receiver, управляет Channel.
 */
public class Network {
    private final Channel channel;
    private final int[][] hadamard; // матрица Уолша (каждая строка — код)
    private final List<Transmitter> transmitters = new ArrayList<>();
    private final List<Receiver> receivers = new ArrayList<>();
    private final ExecutorService txExecutor;

    /**
     * Создаёт сеть.
     *
     * @param numTransmitters количество передатчиков
     * @param numReceivers    количество приёмников
     * @param words           список слов для передачи (если меньше чем tx — будут циклично повторяться)
     * @param walshSize       длина кода Уолша (степень двойки: 1,2,4,8,...)
     */
    public Network(int numTransmitters,
                   int numReceivers,
                   List<String> words,
                   int walshSize) {
        Objects.requireNonNull(words, "Кодовое слово не должно быть null");
        if (numTransmitters < 0 || numReceivers < 0) {
            throw new IllegalArgumentException("Количество должно быть >= 0");
        }
        if (walshSize < 1 || (walshSize & (walshSize - 1)) != 0) {
            throw new IllegalArgumentException("Размер кода Уолша должен быть степенью двойки (1,2,4,8,...)");
        }

        this.channel = new Channel();
        this.hadamard = WalshCodeGenerator.generate(walshSize);
        this.txExecutor = Executors.newFixedThreadPool(Math.max(1, numTransmitters));
        System.out.println("=== Initialization ===");

        // Создание передатчиков (не запускаем их)
        for (int i = 0; i < numTransmitters; i++) {
            String word = words.get(i % words.size());
            int[] walshCode = hadamard[i % hadamard.length].clone();
            Transmitter tx = new Transmitter(i, word, walshCode, channel);
            System.out.println("Transmitter " + i + " использует код: " + Arrays.toString(walshCode));
            transmitters.add(tx);
        }

        // Создание приёмников (подписываются в конструкторе на канал)
        for (int i = 0; i < numReceivers; i++) {
            int[] walshCode = hadamard[i % hadamard.length].clone();
            Receiver r = new Receiver(i, walshCode, channel);
            System.out.println("Receiver " + i + " использует код: " + Arrays.toString(walshCode));
            receivers.add(r);
        }

    }

    /**
     * Однократный прогон: параллельно запускает все передатчики (они вызывают channel.publish),
     * ожидает завершения публикаций и затем вызывает channel.broadcast() — суммирование сигналов и рассылку.
     *
     * @param timeoutSeconds максимальное время ожидания публикации в секундах
     * @throws InterruptedException если поток прерван
     */
    public void runOnce(long timeoutSeconds) throws InterruptedException {
        System.out.println("=== Transmitting started ===");
        if (transmitters.isEmpty()) {
            // просто broadcast пустой эфир (ничего не придёт)
            channel.broadcast();
            return;
        }

        // Для каждого передатчика вызываем transmit() в пуле
        List<Future<?>> futures = new ArrayList<>();
        for (Transmitter tx : transmitters) {
            futures.add(txExecutor.submit(tx::transmit));
        }

        // Ждём публикаций
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        for (Future<?> f : futures) {
            long timeLeft = deadline - System.nanoTime();
            if (timeLeft <= 0) {
                // таймаут
                break;
            }
            try {
                f.get(timeLeft, TimeUnit.NANOSECONDS);
            } catch (ExecutionException e) {
                // логируем и продолжаем — отдельный передатчик упал
                e.printStackTrace();
            } catch (TimeoutException e) {
                // таймаут для этого будущего — продолжаем, остальные могут быть готовы
                break;
            }
        }

        // Когда предполагается, что все передатчики опубликовали свои сигналы, собираем суммарный сигнал
        channel.broadcast();
    }

    /**
     * Останавливает internal executor (вызывать при завершении работы приложения)
     *
     * @param timeoutSeconds сколько ждать завершения, в секундах
     * @throws InterruptedException если прерывание
     */
    public void shutdown(long timeoutSeconds) throws InterruptedException {
        txExecutor.shutdown();
        if (!txExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
            txExecutor.shutdownNow();
        }
    }

}
