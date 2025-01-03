#include <iostream>
#include <cstring>  // For memset and memcpy
#include <sys/socket.h>  // For socket functions
#include <arpa/inet.h>  // For inet_addr
#include <unistd.h>  // For close
#include "SockClient.h"
#include "manzan.h"

// Constructor to initialize the socket descriptor to -1
SockClient::SockClient(){
    sock_fd = -1;
} 

// Method to open a socket and connect to the server
bool SockClient::openSocket(const std::string ip, int port) {
    // Create socket
    sock_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (sock_fd < 0) {
        DEBUG_ERROR("Error creating socket\n");
        return false;
    }

    // Define server address
    struct sockaddr_in server_address;
    server_address.sin_family = AF_INET;
    server_address.sin_port = htons(port);
    server_address.sin_addr.s_addr = inet_addr(const_cast<char*>(ip.c_str()));

    // Connect to server
    if (connect(sock_fd, (struct sockaddr*)&server_address, sizeof(server_address)) < 0) {
        DEBUG_ERROR("Error connecting to server\n");
        closeSocket();
        return false;
    }

    DEBUG_INFO("Connected to server at %s:%d\n", ip.c_str(), port); 
    return true;
}

// Method to send a message (string) over the socket
bool SockClient::sendMessage(const std::string message) {
    if (sock_fd < 0) {
        DEBUG_ERROR("Socket is not open\n");
        return false;
    }

    int bytes_sent = send(sock_fd, const_cast<char*>(message.c_str()), message.size(), 0);
    if (bytes_sent < 0) {
        DEBUG_ERROR("Error sending message\n");
        return false;
    }
    DEBUG_INFO("Sent message: %s\n", message);

    return true;
}

// Method to close the socket
void SockClient::closeSocket() {
    if (sock_fd >= 0) {
        close(sock_fd);
        sock_fd = -1;
        DEBUG_INFO("Socket closed\n");
    }
}

// Destructor to ensure socket is closed
SockClient::~SockClient() {
    closeSocket();
}
