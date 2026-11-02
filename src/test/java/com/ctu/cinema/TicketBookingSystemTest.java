package com.ctu.cinema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử hệ thống đặt vé xem phim đa luồng (Cinema Ticket Booking System)")
class TicketBookingSystemTest {

    @Test
    @DisplayName("Kiểm tra ThreadSafeTicketBookingSystem: 100 yêu cầu đồng thời cho 50 ghế (Đúng 50 thành công / 50 thất bại)")
    void testThreadSafeTicketBookingSystem100ConcurrentRequests() throws InterruptedException {
        ThreadSafeTicketBookingSystem bookingSystem = new ThreadSafeTicketBookingSystem();
        int totalRequests = 100;
        int threadPoolSize = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        for (int i = 1; i <= totalRequests; i++) {
            final int customerId = i;
            final int targetSeat = (i % 50) + 1; // Ghế từ 1 đến 50 (mỗi ghế có 2 khách tranh nhau)
            executor.submit(() -> {
                try {
                    startLatch.await(); // Đảm bảo tất cả 100 luồng xuất phát cùng 1 thời điểm (Real concurrent burst)
                    bookingSystem.bookSeat(targetSeat, "Customer-" + customerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Kích hoạt tất cả luồng chạy đồng thời
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "Thời gian thực thi không được vượt quá 15 giây");
        executor.shutdown();

        // 1. Kiểm tra số lượng vé thành công và thất bại
        assertEquals(50, bookingSystem.getSuccessfulBookings(),
                "Tổng số vé THÀNH CÔNG phải chính xác là 50 (tương ứng 50 ghế)");
        assertEquals(50, bookingSystem.getFailedBookings(),
                "Tổng số vé THẤT BẠI phải chính xác là 50 (100 - 50)");
        assertEquals(100, bookingSystem.getSuccessfulBookings() + bookingSystem.getFailedBookings(),
                "Tổng số yêu cầu đã xử lý phải đúng 100");

        // 2. Kiểm tra trạng thái của tất cả 50 ghế: Mọi ghế đều phải được đặt và có đúng 1 khách sở hữu
        for (int seat = 1; seat <= 50; seat++) {
            assertTrue(bookingSystem.isSeatBooked(seat), "Ghế #" + seat + " phải ở trạng thái đã đặt (booked = true)");
            assertNotNull(bookingSystem.getCustomerForSeat(seat), "Ghế #" + seat + " phải có tên khách hàng sở hữu");
        }
    }

    @Test
    @DisplayName("Kiểm tra FineGrainedTicketBookingSystem: Khóa mịn từng ghế đạt chuẩn an toàn luồng")
    void testFineGrainedTicketBookingSystem100ConcurrentRequests() throws InterruptedException {
        FineGrainedTicketBookingSystem bookingSystem = new FineGrainedTicketBookingSystem();
        int totalRequests = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        for (int i = 1; i <= totalRequests; i++) {
            final int customerId = i;
            final int targetSeat = (i % 50) + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bookingSystem.bookSeat(targetSeat, "Customer-" + customerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(50, bookingSystem.getSuccessfulBookings());
        assertEquals(50, bookingSystem.getFailedBookings());
        for (int seat = 1; seat <= 50; seat++) {
            assertTrue(bookingSystem.isSeatBooked(seat));
            assertNotNull(bookingSystem.getCustomerForSeat(seat));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 51, 100})
    @DisplayName("Kiểm tra từ chối ghế không hợp lệ (seatNumber < 1 hoặc seatNumber > 50)")
    void testInvalidSeatNumbers(int invalidSeat) {
        ThreadSafeTicketBookingSystem bookingSystem = new ThreadSafeTicketBookingSystem();
        assertFalse(bookingSystem.bookSeat(invalidSeat, "Test-Customer"),
                "Ghế " + invalidSeat + " không hợp lệ phải bị từ chối");
        assertEquals(0, bookingSystem.getSuccessfulBookings());
        assertEquals(0, bookingSystem.getFailedBookings());
    }

    @Test
    @DisplayName("Minh họa lỗi Race Condition trên TicketBookingSystem (Unsafe): Xuất hiện đè ghế hoặc sai số")
    void testUnsafeTicketBookingSystemDemonstratesRaceCondition() throws InterruptedException {
        // Chạy UnsafeTicketBookingSystem để ghi nhận hiện tượng Race Condition
        TicketBookingSystem unsafeSystem = new TicketBookingSystem();
        int totalRequests = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        for (int i = 1; i <= totalRequests; i++) {
            final int customerId = i;
            final int targetSeat = (i % 50) + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    unsafeSystem.bookSeat(targetSeat, "Unsafe-Customer-" + customerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Ở phiên bản Unsafe, do race condition giữa các luồng:
        // successfulBookings thường vượt quá 50 (do nhiều khách cùng đặt 1 ghế),
        // hoặc tổng số (success + failed) < 100 do count++ bị lost update.
        System.out.println(">>> [Unsafe Test Result] Success: " + unsafeSystem.getSuccessfulBookings()
                + ", Failed: " + unsafeSystem.getFailedBookings()
                + ", Total: " + (unsafeSystem.getSuccessfulBookings() + unsafeSystem.getFailedBookings()));
    }
}
