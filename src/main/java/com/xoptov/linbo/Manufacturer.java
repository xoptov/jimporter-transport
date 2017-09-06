package com.xoptov.linbo;

public class Manufacturer
{
    private int id;

    private int originId;

    private String name;

    public int getId()
    {
        return id;
    }

    public Manufacturer setId(int id)
    {
        this.id = id;

        return this;
    }

    public int getOriginId()
    {
        return originId;
    }

    public Manufacturer setOriginId(int originId)
    {
        this.originId = originId;

        return this;
    }

    public String getName()
    {
        return name;
    }

    public Manufacturer setName(String name)
    {
        this.name = name;

        return this;
    }
}
