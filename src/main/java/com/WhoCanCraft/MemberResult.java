package com.WhoCanCraft;

import net.runelite.client.hiscore.HiscoreSkill;

class MemberResult
{
	enum Status { CAN_CRAFT, CANNOT_CRAFT, UNKNOWN }

	final Status status;
	final HiscoreSkill missingSkill;
	final int requiredLevel;
	final int currentLevel;

	private MemberResult(Status status, HiscoreSkill missingSkill, int requiredLevel, int currentLevel)
	{
		this.status = status;
		this.missingSkill = missingSkill;
		this.requiredLevel = requiredLevel;
		this.currentLevel = currentLevel;
	}

	static MemberResult canCraft()
	{
		return new MemberResult(Status.CAN_CRAFT, null, 0, 0);
	}

	static MemberResult cannotCraft(HiscoreSkill skill, int requiredLevel, int currentLevel)
	{
		return new MemberResult(Status.CANNOT_CRAFT, skill, requiredLevel, currentLevel);
	}

	static MemberResult unknown()
	{
		return new MemberResult(Status.UNKNOWN, null, 0, 0);
	}
}
