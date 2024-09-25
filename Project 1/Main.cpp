#include <iostream>
#include <vector>
#include "Node.h"

int main() {
    // Take input for the number of nodes
    int numNodes;
    std::cout << "Enter the number of nodes: ";
    std::cin >> numNodes;

    // Define neighbors for each node
    std::vector<std::vector<int>> neighbors(numNodes);

    // Take input for neighbor relationships
    for (int i = 0; i < numNodes; ++i) {
        std::cout << "Enter neighbors for Node " << i << " (space-separated, -1 to finish): ";
        int neighbor;
        while (true) {
            std::cin >> neighbor;
            if (neighbor == -1) {
                break;
            }
            neighbors[i].push_back(neighbor);
        }
    }

    // Defining other parameters
    int minPerActive = 1;
    int maxPerActive = 3;
    int minSendDelay = 100; 
    int maxNumber = 10;

    // Creating and starting nodes
    std::vector<std::thread> threads;
    std::vector<Node> nodes; // Store nodes in a vector to ensure their lifetime
    for (int i = 0; i < numNodes; ++i) {
        Node node(i, neighbors[i], minPerActive, maxPerActive, minSendDelay, maxNumber);
        nodes.push_back(node);
        threads.push_back(std::thread(&Node::start, &nodes[i]));
    }
    for (auto& thread : threads) {
        thread.join();
    }

    return 0;
}
