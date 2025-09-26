package org.jakuba.model;

import org.jakuba.service.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TransmitterTest {

    @Test
    void testTransmitPublishesEncodedSignal() {
        // given
        int[] walshCode = {1, -1, 1, -1};
        Channel mockChannel = mock(Channel.class);

        Transmitter transmitter = new Transmitter(1, "A", walshCode, mockChannel);

        // when
        transmitter.transmit();

        // then
        ArgumentCaptor<int[]> signalCaptor = ArgumentCaptor.forClass(int[].class);
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockChannel).publish(signalCaptor.capture());

        int[] publishedSignal = signalCaptor.getValue();

        // "A" = 65 = 01000001 → 8 бит
        // Для каждого бита длина = walshCode.length
        // Ожидаем: 8 * 4 = 32 элемента
        assertEquals(32, publishedSignal.length);

        // Проверим первые несколько значений
        // Первый бит '0' → должно быть -walshCode
        for (int i = 0; i < walshCode.length; i++) {
            assertEquals(-walshCode[i], publishedSignal[i]);
        }
    }
}
