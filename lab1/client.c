#define _BSD_SOURCE
#define _DEFAULT_SOURCE

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <unistd.h>
#include <string.h>

typedef enum {
	NEW_USER,
	MESSAGE,
	INTERUPTION
} message_type;

typedef struct {
	message_type msg_type;
	int port;
	int next_port;
	int value;
} token;

int port_numbers[5] = {2222, 3333, 4444, 5555, 6666};

char *multicast_ip_address = "224.1.1.1";
int multicast_port = 22222;
int multicast_socket;

int input_socket;
int output_socket;
int tcp_accept_socket;

char *identifier;
char *ip_address;
int port;
char *next_ip_address;
int next_port;

int protocol;
int have_token;

//MULTICAST UTILITY

void multicast_get_socket(){
	if((multicast_socket = socket(AF_INET, SOCK_DGRAM, 0)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error creating socket for multicast.", errno, strerror(errno));
		exit(1);
	}
}

void multicast_send(char *message){
	struct sockaddr_in socket_address;
	socket_address.sin_family = AF_INET;
	socket_address.sin_addr.s_addr = inet_addr(multicast_ip_address);
	socket_address.sin_port = htons((uint16_t) multicast_port);

	if(sendto(multicast_socket, message, strlen(message) * sizeof(char), 0, (const struct sockaddr *) &socket_address, sizeof(socket_address)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error sending message multicast.", errno, strerror(errno));
		exit(1);
	}
}

//UDP UTILITY

void udp_get_output_socket(){
	if((output_socket = socket(AF_INET, SOCK_DGRAM, 0)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error creating socket UDP.", errno, strerror(errno));
		exit(1);
		}		
}

void udp_get_input_socket(){
	if((input_socket = socket(AF_INET, SOCK_DGRAM, 0)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error creating socket UDP.", errno, strerror(errno));
		exit(1);
	}

	struct sockaddr_in socket_address;
	socket_address.sin_family = AF_INET;
	socket_address.sin_addr.s_addr = htonl(INADDR_ANY); //get random local address in network byte order
	socket_address.sin_port = htons((uint16_t) port); //get port number in network byte order

	if(bind(input_socket, (struct sockaddr *) &socket_address, sizeof(socket_address)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error binding socket UDP.", errno, strerror(errno));
		exit(1);
	}
}

void udp_send_token(token token1){
	struct sockaddr_in socket_address;
	socket_address.sin_family = AF_INET;
	socket_address.sin_addr.s_addr = inet_addr(next_ip_address);
	socket_address.sin_port = htons((uint16_t) next_port);

	if(sendto(output_socket, &token1, sizeof(token1), 0, (const struct sockaddr *) &socket_address, sizeof(socket_address)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error sending message UDP.", errno, strerror(errno));
		exit(1);
	}
}

void udp_send_init_token(){
	token token1;
	token1.msg_type = NEW_USER;
	token1.port = port;
	token1.next_port = next_port;
	udp_send_token(token1);
}

//TCP UTILITY

void tcp_get_output_socket(){
	if((output_socket = socket(AF_INET, SOCK_STREAM, 0)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error creating socket TCP.", errno, strerror(errno));
		exit(1);
	}
}

void tcp_get_input_socket(){
	if((input_socket = socket(AF_INET, SOCK_STREAM, 0)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error creating socket TCP.", errno, strerror(errno));
		exit(1);
	}

	struct sockaddr_in socket_address;
	socket_address.sin_family = AF_INET;
	socket_address.sin_addr.s_addr = htonl(INADDR_ANY); //get random local address in network byte order
	socket_address.sin_port = htons((uint16_t) port); //get port number in network byte order

	if(bind(input_socket, (struct sockaddr *) &socket_address, sizeof(socket_address)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error binding socket TCP.", errno, strerror(errno));
		exit(1);
	}

	if(listen(input_socket, 10) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error listening socket TCP.", errno, strerror(errno));
		exit(1);
	}
}

void tcp_accept_connection(){
	if((tcp_accept_socket = accept(input_socket, NULL, NULL)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error accepting connection TCP.", errno, strerror(errno));
		exit(1);
	}
}

void tcp_connect(){
	struct sockaddr_in socket_address;
	socket_address.sin_family = AF_INET;
	socket_address.sin_addr.s_addr = htonl(INADDR_ANY);
	socket_address.sin_port = htons((uint16_t) next_port);

	if(connect(output_socket, (const struct sockaddr *) &socket_address, sizeof(socket_address)) == -1) {
		fprintf(stderr, "%s: %d %s\n", "Error during connecting TCP.", errno, strerror(errno));
		exit(1);
	}
}

void tcp_send_token(token token1){
	//always make new socket just in case new client comes
	tcp_get_output_socket();
	tcp_connect();
	if(write(output_socket, &token1, sizeof(token1)) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error writing TCP.", errno, strerror(errno));
		exit(1);
	}
	if(close(output_socket) == -1){
		fprintf(stderr, "%s: %d %s\n", "Error closing descriptor TCP.", errno, strerror(errno));
		exit(1);
	}
}

void tcp_send_init_token(){
	token token1;
	token1.msg_type = NEW_USER;
	token1.port = port;
	token1.next_port = next_port;
	tcp_send_token(token1);
}

//HANDLING INTERUPTION

void handler(int sig){
	printf("\nClient going down.\n");
	token token1;
	token1.msg_type = INTERUPTION;
	token1.port = port;
	token1.next_port = next_port;
	if(protocol){ //TCP
		tcp_send_token(token1);
		if(shutdown(input_socket, SHUT_RDWR) == -1){
			fprintf(stderr, "%s: %d %s\n", "Error during shutdown TCP.", errno, strerror(errno));
			exit(1);
		}
		if(close(input_socket) == -1){
			fprintf(stderr, "%s: %d %s\n", "Error during closing TCP.", errno, strerror(errno));
			exit(1);
		}
		if(shutdown(tcp_accept_socket, SHUT_RDWR) == -1){
			fprintf(stderr, "%s: %d %s\n", "Error during shutdown TCP.", errno, strerror(errno));
			exit(1);
		}
		if(close(tcp_accept_socket) == -1){
			fprintf(stderr, "%s: %d %s\n", "Error during closing TCP.", errno, strerror(errno));
			exit(1);
		}
	} else { //UDP
		udp_send_token(token1);
	}
	exit(0);
}

int main(int argc, char *argv[]){
	signal(SIGINT, handler);

	if(argc != 8){
		fprintf(stderr, "Wrong number of arguments.\nShould be: identifier, address, port, next_address, next_port, have_token(false=0|true=1), protocol(UDP=0|TCP=1).");
		exit(1);
	}

	identifier = argv[1];
	ip_address = argv[2];
	port = atoi(argv[3]);
	next_ip_address = argv[4];
	next_port = atoi(argv[5]);
	have_token = atoi(argv[6]);
	protocol = atoi(argv[7]);

	if(have_token != 0 && have_token != 1){
		fprintf(stderr, "Value for have_token has to be 0(false) or 1(true).");
		exit(1);
	}

	if(protocol != 0 && protocol != 1){
		fprintf(stderr, "Value for protocol has to be 0(UDP) or 1(TCP).");
		exit(1);
	}

	//initialization of sockets and if needed token
	if(protocol){ //TCP
		if(have_token){
			token token1;
			//initialization of sockets for receiving and sending
			tcp_get_input_socket();
			tcp_get_output_socket();
			//accepting connection
			tcp_accept_connection();
			//reading message from someone
			if(read(tcp_accept_socket, &token1, sizeof(token1)) == -1){
				fprintf(stderr, "%s: %d %s\n", "Error reading TCP.", errno, strerror(errno));
				exit(1);
			}
			//only a new user can send token to us since we are first
			if(token1.msg_type == NEW_USER){
				printf("TCP received new user port %d.\n", token1.port);
				next_port = token1.port;
				token1.port = port;
				token1.next_port = next_port;
				token1.msg_type = MESSAGE;
				token1.value = rand() % 1000;
				printf("TCP port %d, send %d to %d.\n", port, token1.value, token1.next_port);
				tcp_send_token(token1);
			}
		} else { //if we are not first
			tcp_get_input_socket();
			tcp_get_output_socket();
			printf("TCP port %d, send INIT token.\n", port);
			tcp_send_init_token();
		}
	} else { //UDP
		//initialization of sockets for receiving and sending
		udp_get_input_socket();
		udp_get_output_socket();
		printf("UDP port %d, send INIT token.\n", port);
		udp_send_init_token();

		if(have_token){ //if we are first and we create token
			token token1;
			token1.port = port;
			token1.next_port = next_port;
			token1.msg_type = MESSAGE;
			token1.value = rand() % 1000;
			printf("UDP port %d, send %d.\n", port, token1.value);
			udp_send_token(token1);
		}
	}
	
	multicast_get_socket(); //prepare multicast
	//network loop

	if(protocol){ //TCP
		while(1){
			token token1;
			//accepting connection
			tcp_accept_connection();
			//reading message from someone
			if(read(tcp_accept_socket, &token1, sizeof(token1)) == -1){
				fprintf(stderr, "%s: %d %s\n", "Error reading TCP.", errno, strerror(errno));
				exit(1);
			}

			switch(token1.msg_type){
				case NEW_USER:
					printf("TCP received new user port %d.\n", token1.port);
					if(token1.next_port == next_port){ //if new user wants to be after you
						next_port = token1.port;
					} else { //if new user wants to be situated somewhere else send it further
						tcp_send_token(token1);
					}
					break;
				case MESSAGE:
					if(token1.next_port == port){
						multicast_send(identifier);
						printf("TCP port %d, received %d from %d.\n", port, token1.value, token1.port);
						token1.port = port;
						int random_port = port;
						while(random_port == port){
							random_port = port_numbers[rand() % 5];
						}
						token1.next_port = random_port;
						token1.value = rand() % 1000;
						sleep(1);
						printf("TCP port %d, send %d to %d.\n", port, token1.value, token1.next_port);
					} else if(token1.port == port){
						printf("TCP port %d, port %d currently not up.\n", port, token1.next_port);
						token1.port = port;
						token1.next_port = next_port;
						token1.value = rand() % 1000;
						sleep(1);
						printf("TCP port %d, send %d to %d.\n", port, token1.value, token1.next_port);
					}
					tcp_send_token(token1);
					break;
				case INTERUPTION: //once works once not TODO
					printf("Restoring token ring.\n");
					if(next_port == token1.port){
						next_port = token1.next_port;
						token1.msg_type = MESSAGE;
						token1.value = rand() % 1000;
						tcp_get_output_socket();
					}
					tcp_send_token(token1);
					break;
			}
		}
	} else{ //UDP
		while(1){
			token token1;
			if(recvfrom(input_socket, &token1, sizeof(token1), 0, NULL, NULL) == -1){
				fprintf(stderr, "%s: %d %s\n", "Error receiving UDP.", errno, strerror(errno));
				exit(1);
			}

			switch(token1.msg_type){
				case NEW_USER:
					printf("UDP received new user port %d.\n", token1.port);
					if(token1.next_port == next_port){ //if new user wants to be after you
						next_port = token1.port;
					} else { //if new user wants to be situated somewhere else send it further
						udp_send_token(token1);
					}
					break;
				case MESSAGE:
					if(token1.next_port == port){
						multicast_send(identifier);
						printf("UDP port %d, received %d from %d.\n", port, token1.value, token1.port);
						token1.port = port;
						int random_port = port;
						while(random_port == port){
							random_port = port_numbers[rand() % 5];
						}
						token1.next_port = random_port;
						token1.value = rand() % 1000;
						sleep(1);
						printf("UDP port %d, send %d to %d.\n", port, token1.value, token1.next_port);
					} else if(token1.port == port){
						printf("UDP port %d, port %d currently not up.\n", port, token1.next_port);
						token1.port = port;
						token1.next_port = next_port;
						token1.value = rand() % 1000;
						sleep(1);
						printf("UDP port %d, send %d to %d.\n", port, token1.value, token1.next_port);
					}
					udp_send_token(token1);
					break;
				case INTERUPTION:
					printf("Restoring token ring.\n");
					if(next_port == token1.port){
						next_port = token1.next_port;
						token1.msg_type = MESSAGE;
						token1.value = rand() % 1000;
					}
					udp_send_token(token1);
					break;
			}
		}
	}
}