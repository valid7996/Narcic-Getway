#!/usr/bin/env python3
"""
Narcic Gateway Subscription Encryption Utility
==============================================
Encrypts subscription files (vless://, vmess://, wireguard://, etc.) using
AES-256-GCM + Deflate compression with the proprietary Narcic Gateway key.

Once encrypted into the NRGSUB1 format:
- Narcic Gateway automatically decrypts and imports the configs.
- Any other app (v2rayNG, Hiddify, sing-box, Clash, Streisand, etc.) cannot
  read, parse, or use the configurations.

Usage:
    python tools/encrypt_subscription.py input.txt [output.txt]
    python tools/encrypt_subscription.py --decrypt encrypted.txt [plain.txt]
"""

import sys
import os
import zlib
import base64
import hashlib

SECRET_PHRASE = b"NarcicGateway://ProtectedSubscription/v1@validbv7996"
AAD = b"NRG-SUB-V1"
PREFIX = "NRGSUB1:"

def get_key() -> bytes:
    return hashlib.sha256(SECRET_PHRASE).digest()

def encrypt_payload(plain_text: str) -> str:
    try:
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    except ImportError:
        print("Error: 'cryptography' package is required. Install it via: pip install cryptography")
        sys.exit(1)

    key = get_key()
    raw = plain_text.strip().encode("utf-8")
    compressed = zlib.compress(raw, level=9)
    nonce = os.urandom(12)
    aesgcm = AESGCM(key)
    ciphertext = aesgcm.encrypt(nonce, compressed, AAD)
    combined = nonce + ciphertext
    b64 = base64.urlsafe_b64encode(combined).decode("utf-8").rstrip("=")
    return f"{PREFIX}{b64}"

def decrypt_payload(encrypted_text: str) -> str:
    try:
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    except ImportError:
        print("Error: 'cryptography' package is required. Install it via: pip install cryptography")
        sys.exit(1)

    t = encrypted_text.strip()
    if t.startswith(PREFIX):
        payload = t[len(PREFIX):].strip()
    elif t.startswith("nrgsub://"):
        payload = t[len("nrgsub://"):].strip()
    else:
        raise ValueError("Invalid format: text does not start with NRGSUB1: or nrgsub://")

    padded = payload + "=" * (-len(payload) % 4)
    combined = base64.urlsafe_b64decode(padded)
    if len(combined) <= 12 + 16:
        raise ValueError("Payload too short")

    nonce = combined[:12]
    ciphertext = combined[12:]
    key = get_key()
    aesgcm = AESGCM(key)
    compressed = aesgcm.decrypt(nonce, ciphertext, AAD)
    decompressed = zlib.decompress(compressed)
    return decompressed.decode("utf-8")

def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)

    is_decrypt = False
    args = sys.argv[1:]
    if args[0] == "--decrypt":
        is_decrypt = True
        args = args[1:]

    if not args:
        print("Error: missing input file path.")
        sys.exit(1)

    input_path = args[0]
    if not os.path.exists(input_path):
        print(f"Error: file not found: {input_path}")
        sys.exit(1)

    with open(input_path, "r", encoding="utf-8") as f:
        content = f.read()

    if is_decrypt:
        result = decrypt_payload(content)
        if len(args) > 1:
            out_path = args[1]
            with open(out_path, "w", encoding="utf-8") as f:
                f.write(result)
            print(f"Decrypted successfully -> {out_path}")
        else:
            print(result)
    else:
        result = encrypt_payload(content)
        if len(args) > 1:
            out_path = args[1]
            with open(out_path, "w", encoding="utf-8") as f:
                f.write(result)
            print(f"Encrypted successfully -> {out_path}")
        else:
            out_path = input_path + ".enc"
            with open(out_path, "w", encoding="utf-8") as f:
                f.write(result)
            print(f"Encrypted successfully -> {out_path}")

if __name__ == "__main__":
    main()
