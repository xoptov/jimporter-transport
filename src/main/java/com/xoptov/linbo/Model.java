package com.xoptov.linbo;

public class Model
{
    private int id;

    private int originId;

    private Manufacturer manufacturer;

    private String name;

    public int getId()
    {
        return id;
    }

    public Model setId(int id)
    {
        this.id = id;

        return this;
    }

    public int getOriginId()
    {
        return originId;
    }

    public Model setOriginId(int originId)
    {
        this.originId = originId;

        return this;
    }

    public Manufacturer getManufacturer()
    {
        return manufacturer;
    }

    public Model setManufacturer(Manufacturer manufacturer)
    {
        this.manufacturer = manufacturer;

        return this;
    }

    public String getName()
    {
        return name;
    }

    public Model setName(String name)
    {
        this.name = name;

        return this;
    }
}
