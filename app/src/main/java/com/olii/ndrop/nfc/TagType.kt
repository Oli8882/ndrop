package com.olii.ndrop.nfc

/**
 * NDrop — TagType
 * Defines the functional role of each registered NFC tag.
 * Signature: Olii-8882
 *
 * ⚠️ Security note: Tag UIDs are used ONLY as routing keys (which feature to trigger).
 * They are NOT used as authentication tokens. Anyone with an NFC reader can clone a UID.
 */
enum class TagType(val displayName: String) {
    PARKING("Parking Tag"),
    DISCOVERY("Discovery Tag"),
    TIMER("Timer Tag"),
    UNKNOWN("Unknown Tag")
}
