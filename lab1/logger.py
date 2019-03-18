#!/usr/bin/env python

import socket
import struct
import datetime
import sys

print("Logger starting at port 22222.")

file = "log.txt"
buff = []
if len(sys.argv) > 1:
    file = sys.argv[1]

MULTICAST_IP = "224.1.1.1"
MULTICAST_PORT = 22222

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind((MULTICAST_IP, MULTICAST_PORT))
mreq = struct.pack("4sl", socket.inet_aton(MULTICAST_IP), socket.INADDR_ANY)
sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

while True:
    buff = sock.recv(1024)
    print("%s | %s\n" % (str(datetime.datetime.now())[:-7], buff.decode('utf-8')))
    with open(file, "a+") as log_file:
    	log_file.write("%s | %s\n" % (str(datetime.datetime.now())[:-7], buff.decode('utf-8')))