package com.zynis.databaselocktest;

import com.zynis.databaselocktest.models.SomeTable;
import com.zynis.databaselocktest.repositories.SomeTableRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.transaction.Transactional;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PessimisticLockTest {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Autowired
    SomeTableRepository someTableRepository;

    @AfterEach
    public void afterAll() {
        someTableRepository.deleteAll();
    };

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
