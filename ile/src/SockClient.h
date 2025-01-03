#include <string>

// extern "C" {
class SockClient {
    
int sock_fd; 
public:
    // Constructor to initialize the socket descriptor to -1
    SockClient();

    // Method to open a socket and connect to the server
    bool openSocket(const std::string ip, int port);

    // Method to send a message (string) over the socket
    bool sendMessage(const std::string message);

    // Method to close the socket
    void closeSocket();

    // Destructor to ensure socket is closed
    ~SockClient();
};
// }
