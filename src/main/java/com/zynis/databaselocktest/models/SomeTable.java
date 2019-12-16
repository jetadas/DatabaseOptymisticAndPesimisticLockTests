package com.zynis.databaselocktest.models;

import javax.persistence.*;

@Entity
public class SomeTable {

    @Version
    public Long version;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public Long id;

    public String name;

}
