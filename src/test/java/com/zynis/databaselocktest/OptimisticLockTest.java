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
        SomeTable someTable = new SomeTable();
        someTable.name = "sometable1";
        someTableRepository.save(someTable);

        SomeTable someTable2 = someTableRepository.findById(someTable.id).get();
        assertEquals(someTable.version, someTable2.version);

        someTable.name = "nazwa1";
        someTableRepository.save(someTable);

        someTable2.name = "nazwa2";

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            someTableRepository.save(someTable2);
        });

    }

}
