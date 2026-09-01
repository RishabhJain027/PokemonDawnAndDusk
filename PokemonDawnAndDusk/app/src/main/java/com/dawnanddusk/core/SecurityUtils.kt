package com.dawnanddusk.core

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    private const val SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    /**
     * Generates a random cryptographic salt string.
     */
    fun generateSalt(length: Int = 16): String {
        val random = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(SALT_CHARS[random.nextInt(SALT_CHARS.length)])
        }
        return sb.toString()
    }

    /**
     * Hashes password with SHA-256 and salt.
     */
    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password"
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates that a password matches its salt and hash.
     */
    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val computed = hashPassword(password, salt)
        return computed.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Validates username requirements: 3-20 alphanumeric characters or underscores.
     */
    fun isValidUsername(username: String): Boolean {
        val regex = "^[a-zA-Z0-9_]{3,20}$".toRegex()
        return username.matches(regex)
    }

    /**
     * Validates password strength: minimum 6 characters.
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
