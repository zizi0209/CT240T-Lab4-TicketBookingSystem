package com.ctu.concurrency;

import java.util.concurrent.locks.ReentrantLock;

// GIẢI PHÁP 2: ĐỒNG BỘ HÓA BẰNG ReentrantLock (KHÓA TƯỜNG MINH)
public class LockCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock(); // Bắt đầu khóa miền tối hạn
        try {
            count++;
        } finally {
            lock.unlock(); // Luôn giải phóng khóa trong khối finally!
        }
    }

    public int getCount() {
        return count;
    }
}
