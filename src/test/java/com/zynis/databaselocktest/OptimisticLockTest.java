package com.zynis.databaselocktest;

import com.zynis.databaselocktest.models.SomeTable;
import com.zynis.databaselocktest.repositories.SomeTableRepository;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

import javax.transaction.Transactional;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OptimisticLockTest {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Autowired
    SomeTableRepository someTableRepository;

    @AfterEach
    public void afterAll() {
        someTableRepository.deleteAll();
    };

    @Test
    void amountOfSomeTableDataTest() {
        SomeTable someTable = new SomeTable();
        someTable.name = "sometable1";
        someTableRepository.save(someTable);

        SomeTable sometable2 = new SomeTable();
        sometable2.name = "sometable2";
        someTableRepository.save(sometable2);

        List<SomeTable> someTableDataList = (List<SomeTable>)someTableRepository.findAll();

        assertEquals(someTableDataList.size(), 2);
    }


    @Test
    void optimisticLockTest() {
        // 1. Create and save initial record
        SomeTable someTable = new SomeTable();
        someTable.name = "initial_name";
        someTable = someTableRepository.save(someTable);
        Long initialVersion = someTable.version;
        logger.info("Initial version: " + initialVersion);

        // 2. Simulate two different users/threads loading the same record
        // User A loads the record
        SomeTable someTableUserA = someTableRepository.findById(someTable.id).get();
        // User B loads the same record
        SomeTable someTableUserB = someTableRepository.findById(someTable.id).get();

        assertEquals(initialVersion, someTableUserA.version);
        assertEquals(initialVersion, someTableUserB.version);

        // 3. User A updates the record and saves it
        someTableUserA.name = "updated_by_A";
        someTableUserA = someTableRepository.save(someTableUserA);
        
        logger.info("Version after User A update: " + someTableUserA.version);
        // Version should have incremented
        assertEquals(initialVersion + 1, someTableUserA.version);

        // 4. User B tries to update the record using their stale version
        someTableUserB.name = "updated_by_B";
        
        // This should fail because the version in the database (initialVersion + 1)
        // is now greater than the version User B has (initialVersion).
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            someTableRepository.save(someTableUserB);
        });
        
        logger.info("Optimistic locking failure caught as expected.");
    }

}
