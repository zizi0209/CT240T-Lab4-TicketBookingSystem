package com.ctu.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * STEP 2: So sánh trực quan Race Condition và 3 cơ chế đồng bộ
 * Chạy 1.000 luồng đồng thời cùng gọi increment() trên:
 * 1. UnsafeCounter
 * 2. SynchronizedCounter
 * 3. LockCounter
 * 4. AtomicCounter
 */
public class RaceConditionComparisonDemo {
    private static final int THREAD_COUNT = 1000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚀 Bắt đầu kiểm thử " + THREAD_COUNT + " luồng tăng giá trị đồng thời...\n");

        // 1. UnsafeCounter
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        runBenchmark("1. UnsafeCounter (Không đồng bộ)", unsafeCounter::increment);
        System.out.println("   -> Kết quả: " + unsafeCounter.getCount() + " / " + THREAD_COUNT
                + (unsafeCounter.getCount() < THREAD_COUNT ? " ❌ (BỊ LỖI RACE CONDITION!)" : ""));

        // 2. SynchronizedCounter
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        runBenchmark("2. SynchronizedCounter (synchronized)", syncCounter::increment);
        System.out.println("   -> Kết quả: " + syncCounter.getCount() + " / " + THREAD_COUNT + " ✅ (CHÍNH XÁC)");

        // 3. LockCounter
        LockCounter lockCounter = new LockCounter();
        runBenchmark("3. LockCounter (ReentrantLock)", lockCounter::increment);
        System.out.println("   -> Kết quả: " + lockCounter.getCount() + " / " + THREAD_COUNT + " ✅ (CHÍNH XÁC)");

        // 4. AtomicCounter
        AtomicCounter atomicCounter = new AtomicCounter();
        runBenchmark("4. AtomicCounter (AtomicInteger CAS)", atomicCounter::increment);
        System.out.println("   -> Kết quả: " + atomicCounter.getCount() + " / " + THREAD_COUNT + " ✅ (CHÍNH XÁC)");
    }

    private static void runBenchmark(String name, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        long start = System.currentTimeMillis();
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(task);
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;
        System.out.println(name + " hoàn thành trong: " + duration + " ms");
    }
}
