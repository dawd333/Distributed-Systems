CC=gcc
OPT=-Wall
LIB=-pthread

all: client.o
	$(CC) $(OPT) client.o -o client $(LIB)

clean:
	rm -f *.o client
	make all

logger:
	python logger.py

udp1:
	./client first 127.0.0.1 2222 127.0.0.1 2222 1 0
udp2:
	./client second 127.0.0.1 3333 127.0.0.1 2222 0 0
udp3:
	./client third 127.0.0.1 4444 127.0.0.1 2222 0 0

tcp1:
	./client first 127.0.0.1 2222 127.0.0.1 2222 1 1
tcp2:
	./client second 127.0.0.1 3333 127.0.0.1 2222 0 1
tcp3:
	./client third 127.0.0.1 4444 127.0.0.1 2222 0 1
