# DS-RMPI

## Reliable Message Passing Infrastructure

This project implements a **distributed systems middleware** providing reliable message passing in peer-to-peer networks using **Virtual Synchrony (VSync)** protocol. Developed as part of the Distributed Systems course at Politecnico di Milano.

## Features

- **Virtual Synchrony Protocol**: Keeps message ordering consistent across nodes
- **Fault Tolerance**: Checkpoint-based recovery system for handling node failures
- **Dynamic Topology Management**: View manager for controlling network membership and topology changes
- **Reliable Communication**: Multi-layer architecture with reliability guarantees
- **Automatic Discovery**: Protocol for new nodes to join the network automatically
- **JSON-based Message Serialization**: Efficient message encoding/decoding

## Architecture

The middleware follows a **4-layer architecture**:

1. **Application Layer**: API for sending/receiving messages
2. **VSync Layer**: Implements Virtual Synchrony with view management and fault tolerance.  
3. **Reliability Layer**: Handles message acknowledgment, retransmission, and ordering.  
4. **Communication Layer**: Manages network I/O, discovery, and serialization.  

![System Architecture](doc/UML.svg)

### Key Components

- **Scalar Clock**: Logical timestamps for message ordering and causality.  
- **Acknowledge Map**: Tracks delivery confirmations and retransmissions.  
- **Stable Priority Blocking Queue**: Thread-safe queue ensuring deterministic message processing.  
- **Checkpoint/Recovery System**: Periodic snapshots for fault recovery.  
- **Knowledge Vector**: Tracks delivery state across all nodes for consistency.
- **View Change Protocol**: Coordinates atomic topology updates.
- **Client Handler**: Asynchronous TCP connection management.

## How It Works

1. **Node Startup**: Each node starts the middleware stack, initializing all layers
2. **Network Discovery**: New nodes broadcast discovery messages to find and join existing networks
3. **View Management**: A designated view manager maintains the network topology and coordinates changes
4. **Message Delivery**: Application messages flow through VSync → Reliability → Communication layers
5. **Fault Tolerance**: The system periodically creates checkpoints and can recover from failures
6. **Virtual Synchrony**: All nodes see the same sequence of messages in the same order, maintaining consistency

![New Host Protocol](doc/NewHostProtocol.svg)

## Testing

Comprehensive unit tests covering:

- **Message Serialization**: JSON encoding/decoding and polymorphic message types
- **Reliability Layer**: Acknowledgment protocols, retransmission, and duplicate detection  
- **Data Structures**: Thread-safe priority queues and concurrent collections
- **Manual Integration Testing**: Multi-node scenarios using Docker Compose

## Docker Support

The project includes Docker configuration for easy deployment and testing with multiple nodes.
