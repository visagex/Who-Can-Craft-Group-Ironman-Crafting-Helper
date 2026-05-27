package com.WhoCanCraft;
class MaterialEntry
{
	final String name;
	final int needed;
	final int inInventory;
	final int inBank;         // -1 if bank has not been opened this session
	final int inGroupStorage; // -1 if group storage has not been opened this session

	MaterialEntry(String name, int needed, int inInventory, int inBank, int inGroupStorage)
	{
		this.name = name;
		this.needed = needed;
		this.inInventory = inInventory;
		this.inBank = inBank;
		this.inGroupStorage = inGroupStorage;
	}

	int total()
	{
		int t = inInventory;
		if (inBank >= 0) t += inBank;
		if (inGroupStorage >= 0) t += inGroupStorage;
		return t;
	}

	boolean hasSufficient()
	{
		return total() >= needed;
	}
}
