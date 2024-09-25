#include <iostream>
#include <vector>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <random>
#include <chrono>
#include <queue>  // For message queue

const int MAX_NODES = 10;

class Node {
public:
    Node(int id, int numNodes)
        : id(id), numNodes(numNodes), isMarkerReceived(false), isSnapshotComplete(false), snapshotId(0), parent(-1) {
        state.resize(numNodes, 0);
        snapshot.resize(numNodes, std::vector<int>(numNodes, 0));  // Matrix to store snapshots
        snapshotQueue.resize(numNodes);  // A queue for each neighbor to collect snapshots
        visited.resize(numNodes, false);
    }

    void sendMarkerMessages() {
        for (int receiver = 0; receiver < numNodes; ++receiver) {
            if (receiver != id) {
                std::this_thread::sleep_for(std::chrono::milliseconds(randomDelay()));
                sendMarker(receiver);
            }
        }
    }

    void sendMarker(int receiver) {
        std::unique_lock<std::mutex> lock(markerMutex);
        std::cout << "Node " << id << " sends MARKER to Node " << receiver << std::endl;
    }

    void receiveMarker(int sender) {
        std::unique_lock<std::mutex> lock(markerMutex);
        std::cout << "Node " << id << " receives MARKER from Node " << sender << std::endl;

        if (!isMarkerReceived) {
            isMarkerReceived = true;
            buildSpanningTree(sender);
        }
    }

    void buildSpanningTree(int sender) {
        std::unique_lock<std::mutex> lock(treeMutex);

        // If this node has not been visited and the sender is not the parent
        if (!isVisited() && sender != parent) {
            // Mark this node as visited
            markVisited();

            // Record the sender as the parent in the spanning tree
            parent = sender;

            // Add this node as a child to the sender
            children.push_back(id);

            std::cout << "Node " << id << " builds the spanning tree with Node " << sender << std::endl;
        }
    }

    bool isVisited() const {
        std::unique_lock<std::mutex> lock(visitedMutex);
        return visited[id];
    }

    void markVisited() {
        std::unique_lock<std::mutex> lock(visitedMutex);
        visited[id] = true;
    }

    void takeSnapshot() {
        // Simulate taking a snapshot of the local state
        for (int i = 0; i < numNodes; ++i) {
            snapshot[i][snapshotId] = state[i];
        }

        std::unique_lock<std::mutex> lock(snapshotMutex);
        isSnapshotComplete = true;
        snapshotReady.notify_all();
    }

    void waitForSnapshot() {
        std::unique_lock<std::mutex> lock(snapshotMutex);
        snapshotReady.wait([this] { return isSnapshotComplete; });
    }

    void collectSnapshots() {
        // Collect snapshots from all neighbors
        for (int neighbor = 0; neighbor < numNodes; ++neighbor) {
            if (neighbor != id) {
                snapshotQueue[neighbor].push(snapshot[neighbor][snapshotId]);
            }
        }
    }

    void integrateSnapshots() {
        // Integrate collected snapshots into the node's state
        for (int i = 0; i < numNodes; ++i) {
            if (i != id) {
                state[i] += snapshotQueue[i].front();
                snapshotQueue[i].pop();
            }
        }
    }

    void simulateSnapshot() {
        state[id] += 10; // Update local state
    std::cout << "Node " << id << " updated its local state: " << state[id] << std::endl;

    // Sleep to simulate some processing time
    std::this_thread::sleep_for(std::chrono::milliseconds(randomDelay()));

    if (id == 0) {
        sendMarkerMessages();
        takeSnapshot();
    }
        
    }

    void run() {
        simulateSnapshot();
        if (id == 0) {
            waitForSnapshot();
            collectSnapshots();  // Collect snapshots from neighbors
            integrateSnapshots();  // Integrate collected snapshots
        }
    }

private:
    int id;
    int numNodes;
    std::vector<int> state;
    std::vector<std::vector<int>> snapshot;
    std::vector<std::queue<int>> snapshotQueue;
    std::vector<bool> visited;
    bool isMarkerReceived;
    bool isSnapshotComplete;
    int snapshotId;  // Unique snapshot identifier
    int parent;
    std::vector<int> children;

    std::mutex markerMutex;
    std::mutex snapshotMutex;
    std::mutex visitedMutex;
    std::mutex treeMutex;
    std::condition_variable snapshotReady;

    int randomState() {
        return std::rand() % 100;
    }

    int randomDelay() {
        return std::rand() % 500;
    }
};

int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cerr << "Usage: " << argv[0] << " <num_nodes>" << std::endl;
        return 1;
    }

    int numNodes = std::atoi(argv[1]);

    if (numNodes < 1 || numNodes > MAX_NODES) {
        std::cerr << "Invalid number of nodes. Must be between 1 and " << MAX_NODES << std::endl;
        return 1;
    }

    std::srand(std::time(nullptr));

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
