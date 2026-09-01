package com.dawnanddusk.domain.model

enum class CreatureType(val displayName: String, val colorHex: Long) {
    NORMAL("Normal", 0xFFA8A878),
    FIRE("Fire", 0xFFF08030),
    WATER("Water", 0xFF6890F0),
    GRASS("Grass", 0xFF78C850),
    ELECTRIC("Electric", 0xFFF8D030),
    ICE("Ice", 0xFF98D8D8),
    FIGHTING("Fighting", 0xFFC03028),
    POISON("Poison", 0xFFA040A0),
    GROUND("Ground", 0xFFE0C068),
    FLYING("Flying", 0xFFA890F0),
    PSYCHIC("Psychic", 0xFFF85888),
    BUG("Bug", 0xFFA8B820),
    ROCK("Rock", 0xFFB8A038),
    GHOST("Ghost", 0xFF705898),
    DRAGON("Dragon", 0xFF7038F8),
    STEEL("Steel", 0xFFB8B8D0),
    FAIRY("Fairy", 0xFFEE99AC);

    companion object {
        fun fromString(name: String): CreatureType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
        }
    }
}

enum class Rarity(val displayName: String, val code: String, val baseCatchRate: Double, val weightPercent: Double) {
    COMMON("Common", "C", 0.70, 70.0),
    UNCOMMON("Uncommon", "I", 0.45, 20.0),
    RARE("Rare", "R", 0.25, 9.0),
    LEGENDARY("Legendary", "L", 0.08, 1.0);

    companion object {
        fun fromCode(code: String): Rarity {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: COMMON
        }
    }
}
