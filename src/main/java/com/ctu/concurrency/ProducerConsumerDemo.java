package com.ctu.concurrency;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * STEP 3: Cài đặt Mẫu Thiết kế Producer-Consumer với BlockingQueue
 * Mẫu Producer-Consumer giúp tách biệt luồng tạo dữ liệu (Producer) và luồng xử lý dữ liệu (Consumer)
 * thông qua một hàng đợi đồng bộ an toàn về luồng (Thread-safe Queue).
 */

// Đơn hàng cần xử lý
class Order {
    private final int orderId;

    public Order(int orderId) {
        this.orderId = orderId;
    }

    public int getOrderId() {
        return orderId;
    }
}

// Nhà sản xuất (Producer) - Nạp đơn hàng vào hàng đợi
class OrderProducer implements Runnable {
    private final BlockingQueue<Order> queue;

    public OrderProducer(BlockingQueue<Order> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= 5; i++) {
                Order order = new Order(i);
                queue.put(order); // Tự động chờ (block) nếu hàng đợi bị đầy
                System.out.println("📦 [Producer] Đã tạo đơn hàng #" + i);
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Người tiêu dùng (Consumer) - Lấy đơn hàng ra xử lý
class OrderConsumer implements Runnable {
    private final BlockingQueue<Order> queue;

    public OrderConsumer(BlockingQueue<Order> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Order order = queue.take(); // Tự động chờ (block) nếu hàng đợi rỗng
                System.out.println("✅ [Consumer] " + Thread.currentThread().getName() + " đã xử lý xong đơn hàng #" + order.getOrderId());
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class ProducerConsumerDemo {
    public static void main(String[] args) throws InterruptedException {
        // Hàng đợi có sức chứa tối đa 3 phần tử (Capacity = 3)
        BlockingQueue<Order> queue = new ArrayBlockingQueue<>(3);

        Thread producerThread = new Thread(new OrderProducer(queue), "Producer-1");
        Thread consumerThread1 = new Thread(new OrderConsumer(queue), "Worker-1");
        Thread consumerThread2 = new Thread(new OrderConsumer(queue), "Worker-2");

        producerThread.start();
        consumerThread1.start();
        consumerThread2.start();

        // Chờ Producer sản xuất xong 5 đơn hàng
        producerThread.join();

        // Chờ các Consumer xử lý hết các đơn hàng tồn đọng
        Thread.sleep(2000);

        // Ngắt các luồng Consumer khi hoàn tất
        consumerThread1.interrupt();
        consumerThread2.interrupt();
        System.out.println("🎉 Đã hoàn tất xử lý mô hình Producer-Consumer!");
    }
}
