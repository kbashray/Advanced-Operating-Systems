#include "Node.h"
#include <iostream>
#include <cstdlib>
#include <ctime>
#include <chrono>

Node::Node(int id, const std::vector<int>& neighbors, int minPerActive, int maxPerActive, int minSendDelay, int maxNumber)
    : nodeId(id), neighborList(neighbors), isActive(true), messagesSent(0),
      minPerActive(minPerActive), maxPerActive(maxPerActive),
      minSendDelay(minSendDelay), maxNumber(maxNumber) {
    std::srand(static_cast<unsigned int>(std::time(nullptr)));
    vectorClock.resize(neighbors.size(), 0); // Initialize vector clock
}

void Node::start() {
    while (isActive) {
        // Checking if the maximum number of messages has been sent
        if (messagesSent >= maxNumber) {
            isActive = false;
            break;
        }

        // This is to determine the number of messages to send in this active period
        int numMessagesToSend = std::rand() % (maxPerActive - minPerActive + 1) + minPerActive;

        for (int i = 0; i < numMessagesToSend; ++i) {
            int randomNeighborIndex = std::rand() % neighborList.size();
            int destinationNode = neighborList[randomNeighborIndex];

            // Sending a message with vector timestamp
            std::string message = "Message from Node " + std::to_string(nodeId);
            message = attachVectorTimestamp(message);
            std::cout << "Node " << nodeId << " sent a message to Node " << destinationNode << std::endl;

            // Simulate message delay
            std::this_thread::sleep_for(std::chrono::milliseconds(minSendDelay));

            // Receiving a message and updating vector clock
            receiveMessage(message);
            std::cout << "Node " << nodeId << " received a message from Node " << destinationNode << std::endl;

            // Simulate message processing
            std::this_thread::sleep_for(std::chrono::milliseconds(minSendDelay));
        }

        // Record local state with vector timestamp
        recordLocalStateWithTimestamp();

        isActive = false;
        std::this_thread::sleep_for(std::chrono::milliseconds(minSendDelay));
        isActive = true;
    }
}

void Node::recordLocalStateWithTimestamp() {
    // Record local state with vector timestamp
    vectorClock[nodeId]++;
    std::cout << "Node " << nodeId << " recorded its local state with vector timestamp: ";
    for (int timestamp : vectorClock) {
        std::cout << timestamp << " ";
    }
    std::cout << std::endl;
}

std::string Node::attachVectorTimestamp(const std::string& message) {
    // Attach vector timestamp to the message
    std::string messageWithTimestamp = message + " [";
    for (int timestamp : vectorClock) {
        messageWithTimestamp += std::to_string(timestamp) + " ";
    }
    messageWithTimestamp.pop_back(); // Remove the trailing space
    messageWithTimestamp += "]";
    return messageWithTimestamp;
}

void Node::receiveMessage(const std::string& message) {
    // Extract vector timestamp from the received message
    std::string timestampStr = message.substr(message.find("[") + 1, message.find("]") - message.find("[") - 1);
    std::vector<int> receivedTimestamp;
    size_t pos = 0;
    while ((pos = timestampStr.find(" ")) != std::string::npos) {
        receivedTimestamp.push_back(std::stoi(timestampStr.substr(0, pos)));
        timestampStr.erase(0, pos + 1);
    }

    // Update vector clock based on received timestamp
    for (size_t i = 0; i < vectorClock.size(); ++i) {
        vectorClock[i] = std::max(vectorClock[i], receivedTimestamp[i]);
    }
    vectorClock[nodeId]++; // Increment local vector clock
}

// Implement verification of snapshot consistency using vector timestamps (Node 0 specific)
bool Node::verifySnapshotConsistency(const std::vector<std::vector<int>>& recordedVectorTimestamps) {
    for (int nodeId = 1; nodeId < numNodes; nodeId++) {
            if (receivedTimestamps[nodeId] != vectorClock) {
                return false; 
            }
        }
    return true; 
}
