package com.zynis.databaselocktest.repositories;

import com.zynis.databaselocktest.models.SomeTable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface SomeTableRepository extends CrudRepository<SomeTable, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Iterable<SomeTable> findByName(String name);
}
