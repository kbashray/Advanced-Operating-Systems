#include <iostream>
#include <vector>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <chrono>

class Node {
public:
    Node(int id, int numNodes)
        : id(id), numNodes(numNodes), isTerminationAcknowledged(false) {
    }

    void notifyTermination() {
        // Notify termination to all nodes by sending a special message
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        std::cout << "Node " << id << " sends termination message to all nodes" << std::endl;
        // Assume that the message is sent to all nodes
    }

    void acknowledgeTermination() {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        std::cout << "Node " << id << " acknowledges termination" << std::endl;
    }

    void receiveTerminationMessage() {
        // Upon receiving the termination message, acknowledge it
        acknowledgeTermination();
    }

    void receiveAcknowledgment(int sender) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        std::cout << "Node " << id << " received acknowledgment from Node " << sender << std::endl;
    }

    void halt() {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        std::cout << "Node " << id << " halts" << std::endl;
    }

    void run() {
        if (id == 0) {
            notifyTermination();

            // Wait for acknowledgments from other nodes
            for (int i = 1; i < numNodes; ++i) {
                receiveAcknowledgment(i);
            }

            halt();
        } else {
            receiveTerminationMessage();
        }
    }

private:
    int id;
    int numNodes;
    bool isTerminationAcknowledged;
};

int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cerr << "Usage: " << argv[0] << " <num_nodes>" << std::endl;
        return 1;
    }

    int numNodes = std::atoi(argv[1]);

    std::vector<std::thread> threads;

    for (int i = 0; i < numNodes; ++i) {
        threads.emplace_back([i, numNodes]() {
            Node node(i, numNodes);
            node.run();
        });
    }

    for (auto& thread : threads) {
        thread.join();
    }

    return 0;
}
