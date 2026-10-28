package com.xtrip.golemgarage.golem;

/** Special abilities that come from a part's material. */
public enum GolemTrait {
    /** Copper: slowly oxidizes and gets slower unless waxed with honeycomb. */
    OXIDIZES,
    /** Copper, electrum, signalum: more power capacity and faster recharging. */
    CONDUCTIVE,
    /** Diamond, netherite, iridium, tyrian steel: 30% chance to ignore wear. */
    HARDENED,
    /** Netherite, allthemodium, crimson steel, blaze gold: three or more of these parts make the golem fireproof. */
    FIREPROOF,
    /** Lead, osmium: harder to knock back. */
    HEAVY,
    /** Silver, azure silver: arms deal 50% more damage to undead. */
    SMITE,
    /** Uranium, lumium: the golem gives off light. */
    LUMINOUS,
    /** Enderium: teleports to its owner when left far behind. */
    ENDER,
    /** Vibranium: reflects part of melee damage back to the attacker. */
    THORNS,
    /** Unobtainium: legs walk on water (and lava when fireproof), no fall damage. */
    FEATHERWEIGHT
}
