"""Drives the self-test console over OSC and prints what the game sends back.

    python3 tools/osc_selftest.py &      # then: ./gradlew runClient -Pselftest=true

Sends /venuesound/ch/1/fader 0.25 and /ch/02/mix/on 0 (mute strip 2) once a second to
127.0.0.1:9000 for a few minutes, and prints every feedback message received.
"""

import socket
import struct
import time


def osc(address: str, value: float) -> bytes:
    def pad(b: bytes) -> bytes:
        return b + b"\0" * (4 - len(b) % 4)
    return pad(address.encode()) + pad(b",f") + struct.pack(">f", value)


def parse(data: bytes) -> tuple[str, float]:
    end = data.index(b"\0")
    address = data[:end].decode()
    pos = (end + 4) & ~3
    tags_end = data.index(b"\0", pos)
    pos = (tags_end + 4) & ~3
    return address, struct.unpack(">f", data[pos:pos + 4])[0]


sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("127.0.0.1", 0))
sock.settimeout(0.2)
seen: dict[str, float] = {}
deadline = time.time() + 240
next_send = 0.0
while time.time() < deadline:
    if time.time() >= next_send:
        sock.sendto(osc("/venuesound/ch/1/fader", 0.25), ("127.0.0.1", 9000))
        sock.sendto(osc("/ch/02/mix/on", 0.0), ("127.0.0.1", 9000))
        next_send = time.time() + 1
    try:
        data, _ = sock.recvfrom(2048)
        address, value = parse(data)
        if seen.get(address) != value:
            seen[address] = value
            if address in ("/venuesound/ch/1/fader", "/ch/02/mix/on", "/venuesound/ch/2/mute", "/ch/01/mix/fader"):
                print(f"feedback {address} = {value:.3f}", flush=True)
    except socket.timeout:
        pass
print(f"feedback addresses seen: {len(seen)}", flush=True)
