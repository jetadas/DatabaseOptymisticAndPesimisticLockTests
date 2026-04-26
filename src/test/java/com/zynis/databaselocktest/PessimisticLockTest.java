package com.zynis.databaselocktest;

import com.zynis.databaselocktest.models.SomeTable;
import com.zynis.databaselocktest.repositories.SomeTableRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PessimisticLockTest {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Autowired
    SomeTableRepository someTableRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @AfterEach
    public void afterAll() {
        someTableRepository.deleteAll();
    };

    @Test
    void realLifePessimisticLockScenario() throws Exception {
        final String recordName = "SHARED_RECORD_" + System.currentTimeMillis();
        // 1. Setup: Create a shared record
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        SomeTable setupRecord = tt.execute(status -> {
            SomeTable someTable = new SomeTable();
            someTable.name = recordName;
            SomeTable saved = someTableRepository.save(someTable);
            logger.info("Setup: Record created with ID: " + saved.id + " and Name: " + saved.name);
            return saved;
        });
        
        final Long recordId = setupRecord.id;

        logger.info("Starting real-life pessimistic lock scenario with record ID: " + recordId);

        // 2. Thread A: Holds the lock for a while
        CompletableFuture<Void> threadA = CompletableFuture.runAsync(() -> {
            try {
                tt.execute(status -> {
                    logger.info("Thread A: Attempting to lock record...");
                    SomeTable record = someTableRepository.findByIdWithLock(recordId)
                        .orElseThrow(() -> new NoSuchElementException("Record ID " + recordId + " not found in Thread A"));
                    
                    logger.info("Thread A: Record LOCKED. Simulating 3 seconds of heavy processing...");
                    try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    
                    record.name = recordName + "_UPDATED_BY_A";
                    someTableRepository.save(record);
                    logger.info("Thread A: Update done. Releasing lock (committing)...");
                    return null;
                });
            } catch (Exception e) {
                logger.severe("Thread A failed: " + e.toString());
                throw e;
            }
        });

        // Small delay to ensure Thread A starts first
        TimeUnit.MILLISECONDS.sleep(500);

        // 3. Thread B: Tries to read the same record
        long startTime = System.currentTimeMillis();
        CompletableFuture<Void> threadB = CompletableFuture.runAsync(() -> {
            try {
                tt.execute(status -> {
                    logger.info("Thread B: Attempting to lock record (should be blocked)...");
                    SomeTable record = someTableRepository.findByIdWithLock(recordId)
                        .orElseThrow(() -> new NoSuchElementException("Record ID " + recordId + " not found in Thread B"));
                    
                    logger.info("Thread B: Finally got the lock!");
                    // The record returned by findByIdWithLock should be the latest one because the lock wait ensured Thread A finished.
                    assertEquals(recordName + "_UPDATED_BY_A", record.name);
                    return null;
                });
            } catch (Exception e) {
                logger.severe("Thread B failed: " + e.toString());
                throw e;
            }
        });

        CompletableFuture.allOf(threadA, threadB).get();
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Test finished. Thread B was blocked for approximately " + duration + "ms");
        assertTrue(duration >= 2000, "Thread B should have been blocked by Thread A's lock, but duration was " + duration + "ms");
    }

    @Test
    @Transactional
    void findByNameTest() {
        SomeTable someTable = new SomeTable();
        someTable.name = "sometable1";
        someTableRepository.save(someTable);

        List<SomeTable> someTableDataList = (List<SomeTable>)someTableRepository.findByName("sometable1");
        assertEquals(someTableDataList.size(), 1);
    }


    @Test
    void pessimisticLockTest() {
        SomeTable someTable = new SomeTable();
        someTable.name = "sometable1";
        someTableRepository.save(someTable);

        assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            List<SomeTable> someTableDataList = (List<SomeTable>)someTableRepository.findByName("sometable1");
        });
    }

    @Test
    @Transactional
    void pessimisticLockWithTransactionalTest() {
        SomeTable someTable = new SomeTable();
        someTable.name = "sometable1";
        someTableRepository.save(someTable);

        List<SomeTable> someTableDataList = (List<SomeTable>)someTableRepository.findByName("sometable1");
        assertEquals(someTableDataList.size(), 1);
    }

}
