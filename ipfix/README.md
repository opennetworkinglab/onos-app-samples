ONOS IPFIX demo application
===========================

##Introduction
ONOS IPFIX application is the demo application that shows the concept of the OpenFlow statistics export over IPFIX protocol and possibly other similar protocols (e.g NetFlow).

Currently, application supports two scenarios of the OpenFlow statistics export:
 1. Export of the flow statistics for ONOS Reactive Forwarding application flows.
 2. Export of the switch port statistics

The intent of the application is to demonstrate the concept which can be possibly extended to export OpenFlow flow statistics for other ONOS applications, like bgprouter, sdnip, mfwd, etc. 



#User guide

###Installation
ONOS IPFIX requires at least ONOS version 1.3.

ONOS IPFIX application can be downloaded from the onos-app-samples GIT repository with the following command:
```
git clone https://gerrit.onosproject.org/onos-app-samples.git
```
Enter the ONOS IPFIX application directory and compile the application:
```
cd onos-app-samples/onos-app-ipfix
mvn clean install
```
After successful compilation of the application, it can be installed on the running ONOS instance with the following command:
```
onos-app <ip-address-of-ONOS-instance> reinstall! target/onos-app-ipfix-1.3.0-SNAPSHOT.oar
```

###General configuration
ONOS IPFIX application generally requires that user configures IP address and port of the IPFIX collector. This is configured with the following ONOS configuration commands:
```
set org.onosporject.ipfix.IpfixManager CollectorAddress <ip-address>
cfg set org.onosporject.ipfix.IpfixManager CollectorPort <udp-port>
```
IPFIX packets are transported over UDP and default port is 2055. 

###Flow statistics export for ONOS Reactive Forwarding application
The export of the Flow statistics for ONOS Reactive Forwarding application is enabled by default. It is realized over Flow Rule Listener. When the flow rule created by the ONOS reactive forwarding application is removed from ONOS, IPFIX application will collect its statistics, covert them to the appropriate IPFIX format and export them over IPFIX protocol.

Currently, ONOS IPFIX supports three IPFIX record templates that are used for exporting of these flows:

 - **MAC template** (template ID = 331) - matches only MAC addresses, VLAN and switch ports. This template is used with default configuration of the reactive forwarding application that matches only source and destination MAC address and input port. This template has following IPFIX information elements (IEs):
	- ID 130 - *exporterIPv4Address* - IPv4 address of the OpenFlow switch where flow was installed.
	- ID 131 - *exporterIPv6Address* - OpenFlow switch DPID where flow was installed is embedded in the last 8 bytes of the fields.
	- ID - 152 - *flowStartMilliseconds* - The absolute timestamp when the flow is installed by ONOS at the switch.
	- ID - 153 - *flowEndMilliseconds* - The absolute timestamp when the flow is removed by the ONOS from the switch. 
	- ID 1 - *octetDeltaCount* - number of bytes matched by the flow.
	- ID 2 - *packeDeltaCount* - number of packets matched by the flow.
	- ID 10 - *ingressInterface* - OpenFlow switch input port of the flow. 
	- ID 14 - *egressInterface* - OpenFlow switch output port from the flow rule action structure. 
	- ID 56 - *sourceMacAddress* - source MAC address matched by the flow rule.
	- ID 80 - *destinationMacAddress* - destination MAC address matched by the flow rule. 
	- ID 256 - *etherType* - Ethertype field matched by flow rule (0 if not matched) 
	- ID 14 - *vlanId* - VLAN ID matched by the flow rule (0 if not matched)

 - **IPv4 template** (template ID = 332) - matches MAC template with addition to IPv4 addresses, Protocol, DSCP and TCP/UDP port. This template will be used if the matching of the IPv4 address is enabled in the ONOS Reactive Forwarding Application. This template has following IPFIX information elements (IEs) in addition to MAC template:
	- ID 8 - *sourceIPv4Address* - source IPv4 address matched by the flow rule.
	- ID 12 - *destinationIPv4Address* - destination IPv4 address matched by the flow rule. 
	- ID 4 - *protocolIdentifier* - IPv4Protocol field matched by flow rule (255 if not matched) 
	- ID 5 - *ipClassOfService* - IPv4 ToS field matched by the flow rule, constructed from DSCP and ECN OpenFlow matching conditions (0 if not matched).
	- ID 7 - *sourceTransportPort* - source TCP/UDP port matched by the flow rule.
	- ID 11 - *destinationTransportPort* - destination TCP/UDP port matched by the flow rule.


 - **IPv6 template** (template ID = 333) - matches MAC template with addition to IPv6 addresses, Next-Header, Flow Label and TCP/UDP port. This template will be used if the matching of the IPv6 address is enabled in the ONOS Reactive Forwarding application. This template has following IPFIX information elements (IEs) in addition to MAC template:
	- ID 27 - *sourceIPv6Address* - source IPv6 address matched by the flow rule.
	- ID 28 - *destinationIPv6Address* - destination IPv6 address matched by the flow rule. 
	- ID 31 - *flowLabel* - IPv6 Flow Label field matched by flow rule (0 if not matched) 
	- ID 4 - *protocolIdentifier* - IPv4Protocol field matched by flow rule (255 if not matched) 
	- ID 5 - *ipClassOfService* - IPv4 ToS field matched by the flow rule, constructed from DSCP and ECN OpenFlow matching conditions (0 if not matched).
	- ID 7 - *sourceTransportPort* - source TCP/UDP port matched by the flow rule.
	- ID 11 - *destinationTransportPort* - destination TCP/UDP port matched by the flow rule.

User can enable matching of the IPv4 address and other IPv4 fields in ONOS Reactive Forwarding application with following ONOS commands:
```
cfg set org.onosproject.fwd.ReactiveForwarding matchIpv4Addresses true
cfg set org.onosproject.fwd.ReactiveForwarding matchIpv4Dscp true
```

User can enable matching of the IPv4 address and other IPv4 fields in ONOS Reactive Forwarding application with following ONOS commands:
```
cfg set org.onosproject.proxyarp.ProxyArp ipv6NeighborDiscovery true
cfg set org.onosproject.provider.host.impl.HostLocationProvider ipv6NeighborDiscovery true
cfg set org.onosproject.fwd.ReactiveForwarding ipv6Forwarding true
cfg set org.onosproject.fwd.ReactiveForwarding matchIpv6Addresses true
cfg set org.onosproject.fwd.ReactiveForwarding matchIpv6FlowLabel true
```

User can enable matching of the VLAN, TCP/UDP ports and ICMP type and code fields in ONOS Reactive Forwarding application with following ONOS commands:
```
cfg set org.onosproject.fwd.ReactiveForwarding matchVlan true
cfg set org.onosproject.fwd.ReactiveForwarding matchTcpUdpPorts true
cfg set org.onosproject.fwd.ReactiveForwarding matchIcmpFields true
```

To disable IPFIX export of the flow statistics for Reactive Forwarding application flows use following ONOS command:
```
cfg set org.onosproject.ipfix.IpfixManager ReactiveForwardingExport false
```

###Export of the switch port statistics
Export of the switch port statistics over IPFIX is disabled by default. 
To enable it, use following ONOS command:
```
cfg set org.onosproject.ipfix.IpfixManager PortStatsFlowExport true
```

Export of the switch port statistics is realized over DeviceListener. When ONOS updates its internal port statistics for the OpenFlow switch, IPFIX application will export port statistics over IPFIX protocol. The export is done only by the ONOS instance that is “master” for the specific OpenFlow switch. 
The exported values represent difference between switch port counters collected by ONOS in the current and the previous polling. Polling interval is controlled by the ONOS OpenFlow Device Provider which actually collects statistics and by default is very frequent on 5 seconds interval. IPFIX application will export port statistics for every ONOS update of the statistics. 
It is recommended to configure ONOS port statistics polling interval to appropriate value with the following command:
```
cfg set org.onosproject.org.provider.of.device.impl.OpenFlowDeviceProvider PortStatsPollFrequency 30
```

The export of the switch port statistics uses two IPFIX record templates:

 - **Received traffic** - Template ID 341
 - **Transmitted traffic**-  Template ID 342

These templates consist of the following information elements (IEs):

- ID 130 - *exporterIPv4Address* - IPv4 address of the OpenFlow switch
- ID 131 - *exporterIPv6Address* - OpenFlow switch DPID is embedded in the last 8 bytes of the fields
- *ingressInterface* or *egressInterface* - switch port number:
	- ID 10 - *ingressInterface* IE is used for received traffic statistics
	- ID 14 - *egressInterface* IE is used for transmitted traffic statistics
- ID 1 - *octetDeltaCount* - number of bytes received/transmitted on the port between two polling intervals
- ID 2 - *packeDeltaCount* - number of packets received/transmitted on the port between two polling intervals
- ID - 152 - *flowStartMilliseconds* - The absolute timestamp for the previous polling of the statistics
- ID - 153 - *flowEndMilliseconds* - The absolute timestamp for the current polling of the statistics

##Known shortcomings and issues
The purpose of the application is demonstration of the possibility for export of the OpenFlow statistics over IPFIX protocol. For this reason, export of IPFIX records is realized in very simplified way:

- For export of the Reactive Forwarding application flows, each flow (Data record) is exported with its corresponding Template record in a single UDP packet.
- For export of switch port statistics, for each OpenFlow switch and each traffic direction single IPFIX packet is created. This packet has Template Record for corresponding traffic direction and Data records for each port of the OpenFlow switch.
- Currently, export to only one IPFIX collector is supported.

Some of the IPFIX analyzer application will use source IP address of the IPFIX packets to identify “IPFIX exporter”. Because the ONOS IPFIX application exports IPFIX records on behalf of OpenFlow switches, IPFIX packets have ONOS controller IP address. ONOS IPFIX application uses *exporterIPv4Address* and *exporterIPv6Address* to further identify OpenFlow switch that matched the flow and on which behalf statistics are exported:

- The *exporterIPv4Address* IE has IPv4 address of the OpenFlow switch that is connected over OpenFlow channel to ONOS controller.
- The *exporterIPv6Address* IE embeds the OpenFlow switch DPID in the last 8 bytes of the information element.



