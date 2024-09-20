#include <iostream>
#include <cstring>  // For memset and memcpy
#include <sys/socket.h>  // For socket functions
#include <arpa/inet.h>  // For inet_addr
#include <unistd.h>  // For close

class SocketClient {
public:
    // Constructor to initialize the socket descriptor to -1
    SocketClient(){
        sock_fd = -1;
    } 

    // Method to open a socket and connect to the server
    bool openSocket(const std::string ip, int port) {
        // Create socket
        sock_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (sock_fd < 0) {
            std::cerr << "Error creating socket\n";
            return false;
        }

        // Define server address
        struct sockaddr_in server_address;
        server_address.sin_family = AF_INET;
        server_address.sin_port = htons(port);
        server_address.sin_addr.s_addr = inet_addr(ip.c_str());

        // Connect to server
        if (connect(sock_fd, (struct sockaddr*)&server_address, sizeof(server_address)) < 0) {
            std::cerr << "Error connecting to server\n";
            closeSocket();
            return false;
        }

        std::cout << "Connected to server at " << ip << ":" << port << "\n";
        return true;
    }

    // Method to send a message (string) over the socket
    bool sendMessage(const std::string message) {
        if (sock_fd < 0) {
            std::cerr << "Socket is not open\n";
            return false;
        }

        int bytes_sent = send(sock_fd, message.c_str(), message.size(), 0);
        if (bytes_sent < 0) {
            std::cerr << "Error sending message\n";
            return false;
        }

        std::cout << "Sent message: " << message << "\n";
        return true;
    }

    // Method to send a struct over the socket
    template<typename T>
    bool sendStruct(const T& data) {
        if (sock_fd < 0) {
            std::cerr << "Socket is not open\n";
            return false;
        }

        // Send the raw data of the struct
        int bytes_sent = send(sock_fd, &data, sizeof(data), 0);
        if (bytes_sent < 0) {
            std::cerr << "Error sending struct\n";
            return false;
        }

        std::cout << "Sent struct of size: " << sizeof(data) << " bytes\n";
        return true;
    }

    // Method to close the socket
    void closeSocket() {
        if (sock_fd >= 0) {
            close(sock_fd);
            sock_fd = -1;
            std::cout << "Socket closed\n";
        }
    }

    // Destructor to ensure socket is closed
    ~SocketClient() {
        closeSocket();
    }

private:
    int sock_fd;  // Socket file descriptor
};

// Example struct to send over the socket
struct ExampleStruct {
    int id;
    float value;
    char name[50];
};

int main() {
    // Create a SocketClient instance
    SocketClient client;

    // Open a socket and connect to a server
    if (!client.openSocket("127.0.0.1", 8080)) {
        return 1;
    }

    // Send a message over the socket
    client.sendMessage("Hello from C++");

    // Prepare and send a struct over the socket
    ExampleStruct data = { 1, 3.14, "TestStruct" };
    client.sendStruct(data);

    // Close the socket
    client.closeSocket();

    return 0;
}
