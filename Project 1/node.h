#ifndef NODE_H
#define NODE_H

#include <vector>
#include <thread>

class Node {
public:
    Node(int id, const std::vector<int>& neighbors, int minPerActive, int maxPerActive, int minSendDelay, int maxNumber);
    void start();
    void recordLocalStateWithTimestamp(); 
    std::string attachVectorTimestamp(const std::string& message); 
    void receiveMessage(const std::string& message);
private:
    int nodeId;
    std::vector<int> neighborList;
    bool isActive;
    int messagesSent;
    int minPerActive;
    int maxPerActive;
    int minSendDelay;
    int maxNumber;
    std::vector<int> vectorClock;
};

#endif