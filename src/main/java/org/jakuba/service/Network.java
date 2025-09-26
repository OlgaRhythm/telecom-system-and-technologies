package org.jakuba.service;

import org.jakuba.model.Receiver;
import org.jakuba.model.Transmitter;
import org.jakuba.model.WalshCodeGenerator;

import java.util.ArrayList;
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
        Objects.requireNonNull(words, "words must not be null");
        if (numTransmitters < 0 || numReceivers < 0) {
            throw new IllegalArgumentException("counts must be >= 0");
        }
        if (walshSize < 1 || (walshSize & (walshSize - 1)) != 0) {
            throw new IllegalArgumentException("walshSize must be a power of two (1,2,4,8,...)");
        }

        this.channel = new Channel();
        this.hadamard = WalshCodeGenerator.generate(walshSize);
        this.txExecutor = Executors.newFixedThreadPool(Math.max(1, numTransmitters));

        // Создаём приёмников (подписываются в конструкторе на канал)
        for (int i = 0; i < numReceivers; i++) {
            int[] code = hadamard[i % hadamard.length].clone();
            Receiver r = new Receiver(i, code, channel);
            receivers.add(r);
        }

        // Создаём передатчиков (не запускаем их)
        for (int i = 0; i < numTransmitters; i++) {
            String word = words.get(i % words.size());
            int[] code = hadamard[i % hadamard.length].clone();
            Transmitter tx = new Transmitter(i, word, code, channel);
            transmitters.add(tx);
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
        if (transmitters.isEmpty()) {
            // просто broadcast пустой эфир (ничего не придёт)
            channel.broadcast();
            return;
        }

        // Для каждого передатчика вызываем transmit() в пуле (не стартуем run()/sleep)
        List<Future<?>> futures = new ArrayList<>();
        for (Transmitter tx : transmitters) {
            // используем лямбду, чтобы вызвать именно transmit() и не зависеть от run()
            futures.add(txExecutor.submit(tx::transmit));
        }

        // Ждём публикаций (timeout guard)
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

        // Когда предполагается, что все передатчики опубликовали свои сигналы, собираем и трансмитим суммарный сигнал
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
