"""Minimal RCON client: python3 rcon.py <password> <command> [command ...] (port 25575)."""
import socket
import struct
import sys


def packet(sock, rid, kind, body):
    data = struct.pack("<ii", rid, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(data)) + data)
    size = struct.unpack("<i", sock.recv(4))[0]
    buf = b""
    while len(buf) < size:
        buf += sock.recv(size - len(buf))
    return buf[8:-2].decode("utf-8", "replace")


s = socket.create_connection(("127.0.0.1", 25575), timeout=20)
packet(s, 1, 3, sys.argv[1])
for i, cmd in enumerate(sys.argv[2:]):
    print("> " + cmd)
    print(packet(s, 10 + i, 2, cmd))
