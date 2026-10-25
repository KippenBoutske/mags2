package nl.cita.magister.pairing

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ECDH P-256 + AES-256-GCM. P-256 is available on Android 8 (minSdk 26);
 * X25519 is not on every device in that range.
 */
object PairingCrypto {
    private val random = SecureRandom()

    fun randomSecret(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return b64url(bytes)
    }

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec("secp256r1"), random)
        return gen.generateKeyPair()
    }

    fun encodePublic(key: PublicKey): String = b64url(key.encoded)

    fun encodePrivate(key: PrivateKey): String = b64url(key.encoded)

    fun decodePublic(encoded: String): PublicKey {
        val spec = X509EncodedKeySpec(unb64url(encoded))
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    fun decodePrivate(encoded: String): PrivateKey {
        val spec = PKCS8EncodedKeySpec(unb64url(encoded))
        return KeyFactory.getInstance("EC").generatePrivate(spec)
    }

    fun deriveAesKey(privateKey: PrivateKey, peerPublic: PublicKey, oneTimeSecret: ByteArray): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(peerPublic, true)
        val shared = agreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(shared)
        digest.update(oneTimeSecret)
        return digest.digest()
    }

    fun encrypt(aesKey: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = ByteArray(12)
        random.nextBytes(nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, nonce))
        return nonce to cipher.doFinal(plaintext)
    }

    fun decrypt(aesKey: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ciphertext)
    }

    fun seal(desktopPublic: PublicKey, oneTimeSecret: String, plaintext: ByteArray): PairingWire {
        val phone = generateKeyPair()
        val key = deriveAesKey(phone.private, desktopPublic, unb64url(oneTimeSecret))
        val (nonce, ct) = encrypt(key, plaintext)
        return PairingWire(
            pk = encodePublic(phone.public),
            nonce = b64url(nonce),
            ct = b64url(ct),
        )
    }

    fun open(desktopPrivate: PrivateKey, oneTimeSecret: String, wire: PairingWire): ByteArray {
        val phonePublic = decodePublic(wire.pk)
        val key = deriveAesKey(desktopPrivate, phonePublic, unb64url(oneTimeSecret))
        return decrypt(key, unb64url(wire.nonce), unb64url(wire.ct))
    }
}
