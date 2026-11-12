#!/usr/bin/env python3
"""
Generates deterministic demo KYC image files for local development.
Images are encrypted with AES-256-GCM using the demo encryption key matching
application.properties (KYC_STORAGE_KEY: MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=).
"""

import io
import os
import base64
import hashlib
from pathlib import Path
from PIL import Image, ImageDraw
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

KEY_B64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
KEY = base64.b64decode(KEY_B64)
AES_GCM = AESGCM(KEY)

# Fixed nonces for deterministic encryption
NONCES = {
    "NID_FRONT": b"\x01" * 12,
    "NID_BACK":  b"\x02" * 12,
    "SELFIE":    b"\x03" * 12,
}

def make_card(text_lines, color=(30, 60, 110)):
    img = Image.new("RGB", (400, 250), color=color)
    draw = ImageDraw.Draw(img)
    draw.rectangle([10, 10, 390, 240], outline=(255, 255, 255), width=2)
    y = 30
    for line in text_lines:
        draw.text((30, y), line, fill=(255, 255, 255))
        y += 35
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=85)
    return buf.getvalue()

def main():
    repo_root = Path(__file__).resolve().parent.parent.parent
    target_dirs = [
        repo_root / "backend" / "data" / "kyc",
        repo_root / "data" / "kyc",
    ]

    for d in target_dirs:
        d.mkdir(parents=True, exist_ok=True)

    nid_front = make_card([
        "KINGDOM OF CAMBODIA",
        "NATIONAL ID CARD (FRONT)",
        "Name: Kosal Rith",
        "ID: 067890123",
        "DOB: 1995-08-20"
    ])
    nid_back = make_card([
        "KINGDOM OF CAMBODIA",
        "NATIONAL ID CARD (BACK)",
        "Expiry: 2033-05-15",
        "Address: Phnom Penh",
        "Issued: 2023-05-15"
    ], color=(40, 80, 70))
    selfie = make_card([
        "KYC VERIFICATION SELFIE",
        "Applicant: Kosal Rith",
        "Status: PENDING REVIEW",
        "Date: 2026-11-01"
    ], color=(90, 40, 70))

    docs = [
        ("7a000000-0000-0000-0000-000000000001.enc", "NID_FRONT", nid_front),
        ("7a000000-0000-0000-0000-000000000002.enc", "NID_BACK",  nid_back),
        ("7a000000-0000-0000-0000-000000000003.enc", "SELFIE",    selfie),
    ]

    for filename, doc_type, raw in docs:
        sha = hashlib.sha256(raw).hexdigest()
        nonce = NONCES[doc_type]
        encrypted = AES_GCM.encrypt(nonce, raw, None)
        out = nonce + encrypted

        for target_dir in target_dirs:
            file_path = target_dir / filename
            file_path.write_bytes(out)
            print(f"Written: {file_path} (size={len(raw)}, sha256={sha})")

if __name__ == "__main__":
    main()
