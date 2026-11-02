package com.ctu.concurrency;

// KHÔNG AN TOÀN LUỒNG (THREAD-UNSAFE)
public class UnsafeCounter {
    private int count = 0;

    public void increment() {
        count++; // Lỗi Race Condition tại đây (Non-atomic: Read-Modify-Write)!
    }

    public int getCount() {
        return count;
    }
}
