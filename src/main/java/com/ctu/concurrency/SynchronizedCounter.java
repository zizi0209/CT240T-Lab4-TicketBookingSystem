package com.ctu.concurrency;

// GIẢI PHÁP 1: ĐỒNG BỘ HÓA BẰNG TỪ KHÓA synchronized
public class SynchronizedCounter {
    private int count = 0;

    // Khóa toàn bộ phương thức bằng Monitor Lock của đối tượng
    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
