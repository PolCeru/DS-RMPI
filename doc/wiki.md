## View manager

The **view manager** is a node of the peer network that control and store the topology of the network. Each broadcast
group should only have a single view manager. When starting the network, the first two device decide who is the view
manager based on who has the lower random number (same approach of who start the connection).

The view manager should keep a list of know host of the entire networks. It's able to provide this information to all
the hosts, when actively requested or each period of time `TODO: define`

When a new user wants to connect to the group, start to broadcast a `DiscoveryMessage` into the network. When a regular
device receive this message sent a
`AdvertiseMessage` to the view manager of the group to advertise the new user.

The view manager add a new user when:
- receive a `DiscoveryMessage` from the new host
- receive a `AdvertiseMessage` from a known host with the information of the new host

> The view manager must deal with multiple `DiscoveryMessage` and `AdvertiseMessage` with single host insertion

The view manager is created as a separated thread by the communication layer, but it's also connected to the VSynchLayer

