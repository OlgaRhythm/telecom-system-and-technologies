package org.jakuba;

import org.jakuba.service.Network;

import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Слова, которые будут передавать передатчики
        List<String> words = List.of("DOG", "CAT", "HAM", "SUN");

        // Создаём сеть:
        // 4 передатчика, 4 приёмника, коды Уолша длиной 8
        Network network = new Network(
                4,      // transmitters
                4,      // receivers
                words,  // слова для передачи
                8       // размер кода Уолша (8 строк в матрице)
        );

        // Один цикл передачи
        network.runOnce(5);

        // Ждём немного, чтобы вывод приёмников успел отобразиться
        Thread.sleep(300);

        // Завершаем работу
        network.shutdown(2);

        System.out.println("=== Simulation finished ===");
    }
}
