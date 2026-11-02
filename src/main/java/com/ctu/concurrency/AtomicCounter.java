package com.ctu.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

// GIẢI PHÁP 3: SỬ DỤNG BIẾN NGUYÊN TỬ AtomicInteger (LOCK-FREE)
public class AtomicCounter {
    // Sử dụng cơ chế CAS (Compare-And-Swap) ở cấp phần cứng
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet(); // Thao tác nguyên tử an toàn luồng tuyệt đối
    }

    public int getCount() {
        return count.get();
    }
}
