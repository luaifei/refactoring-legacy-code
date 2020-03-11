package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.junit.jupiter.api.Test;

import javax.transaction.InvalidTransactionException;

import static org.junit.jupiter.api.Assertions.*;

public class WalletTransactionTest {
    @Mocked
    IdGenerator generator;

    @Mocked
    RedisDistributedLock lock;

    @Mocked
    WalletServiceImpl walletService;

    public static class SystemMock extends MockUp<System> {
        @Mock public static long currentTimeMillis() {
            return Long.MAX_VALUE;
        }
    }

    @Test
    void should_raise_exception_given_invalid_constructor_parameters() {
        WalletTransaction invalidCase1 = new WalletTransaction("1", null,
                1L, 1L, "1");
        WalletTransaction invalidCase2 = new WalletTransaction("1", 1L,
                null, 1L, "1");
        assertThrows(InvalidTransactionException.class, invalidCase1::execute);
        assertThrows(InvalidTransactionException.class, invalidCase2::execute);
    }

    @Test
    void should_return_false_given_lock_failed() throws InvalidTransactionException {
        new SystemMock();
        new Expectations() {
            {
                lock.lock("t_fakeId");
                result = false;
            }
        };

        WalletTransaction transaction = new WalletTransaction("fakeId", 2L,
                1L, 1L, "1");
        assertFalse(transaction.execute());
    }

    @Test
    void should_return_false_if_exceed_20_days() throws InvalidTransactionException {
        new Expectations() {
            {
                lock.lock("t_fakeId");
                result = true;
            }
        };

        WalletTransaction transaction = new WalletTransaction("fakeId", 2L,
                1L, 1L, "1");

        new SystemMock();
        assertFalse(transaction.execute());
    }

    @Test
    void should_return_true_given_transfer_successfully() throws InvalidTransactionException {
        new SystemMock();
        new Expectations() {
            {
                lock.lock("t_fakeId");
                result = true;

                walletService.moveMoney("t_fakeId", 2L, 1L, 0.0);
                result = "walletTransactionId";
            }
        };

        WalletTransaction transaction = new WalletTransaction("fakeId", 2L,
                1L, 1L, "1");

        assertTrue(transaction.execute());

    }

    @Test
    void should_return_false_given_transfer_failed() throws InvalidTransactionException {
        new SystemMock();
        new Expectations() {
            {
                lock.lock("t_fakeId");
                result = true;

                walletService.moveMoney("t_fakeId", 2L, 1L, 0.0);
                result = null;
            }
        };

        WalletTransaction transaction = new WalletTransaction("fakeId", 2L,
                1L, 1L, "1");

        assertFalse(transaction.execute());
    }
}
