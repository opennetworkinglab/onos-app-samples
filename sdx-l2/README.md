![GEANT logo](http://www.geant.org/Style%20Library/Geant/Images/logo.png "http://geant.org")

Software Defined eXchange - L2 (SDX-L2)
========================================

SDX-L2 is an application for ONOS project which can provide layer 2 connectivity between edge ports of a given SDN network.


License
========

This sofware is licensed under the Apache License, Version 2.0.

Information can be found here: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).


Documentation
==============

Check [SDX-L2 page](https://wiki.onosproject.org/display/ONOS/SDX-L2+application) at ONOS wiki.


Dependencies
=============

SDX-L2 implements its own ARP/NDP handler and does not require proxyarp, fwd and hostprovider apps.
It is suggested to start ONOS for this app by using ONOS_APPS=drivers,openflow-base,lldpprovider


Installation
=============

- Compile the project:

        mci

    or to skip unit tests:

        mci -Dmaven.test.skip=true

- Install in your ONOS deployment:

        onos-app $OC1 install target/onos-app-sdx-l2-1.7.0-SNAPSHOT.oar

- Verify that the application is active:

        onos> onos:apps | grep sdx-l2
          * id=79, name=org.onosproject.sdx-l2, version=1.7.0.SNAPSHOT, origin=GN4 project, category=Utility, description=SDX-L2 application to create and manage Ethernet circuits, features=[onos-app-sdx-l2], featuresRepo=mvn:org.onosproject/onos-app-sdx-l2/1.7.0-SNAPSHOT/xml/features, apps=[], permissions=[], url=https://wiki.onosproject.org/display/ONOS/SDX-L2+application


CLI commands
=============

General
--------

- Create a named SDX:

        sdxl2-add $sdxname


- Delete a named SDX:

        sdxl2-remove $sdxname


- List all the active SDXs:

        sdxl2-list


Connection Points (CP)
-----------------------

- Create a named SDX-L2 CP:

        sdxl2cp-add [-ce_mac $mac] $sdxname $connectionpoint $sdxcpname [$vlans]

  where $vlans can define:
    - explicit VLANs, ranges or a combination of both: 5,10-15
    - all port: -1 (or just ignore)

 *note that CPs must have same number of VLANs in order to establish a VC*


- Remove a named SDX-L2 CP:

        sdxl2cp-remove $sdxcpname


- Get the information of a SDX-L2 CP:

        sdxl2cp $sdxcpname


- List all active SDX-L2 CPs or all active SDX CPs related to a SDX:

        sdxl2cp-list [$sdxname]


Virtual Circuits (VC)
----------------------

- Create a VC between two CPs:

        sdxl2vc-add $sdxname $sdxcp1 $sdxcp2

  where the circuit can be based on MAC (default), VLAN or MPLS mechanisms:
    - cfg set org.onosproject.sdxl2.SdxL2Manager VirtualCircuitType {MAC|VLAN|MPLS}


- Remove a named VC:

        sdxl2vc-remove $sdxcname


- Get the information of a L2 VC:

        sdxl2vc $sdxvcname


- List all active L2 VCs or all L2 VCs related to a SDX:

        sdxl2vc-list [$sdxname]


Usage examples
===============

- Create a named SDX-L2:

        onos> sdxl2:sdxl2-add SDXL2Test
        onos> sdxl2:sdxl2-list

        SDXL2
        --------------
        SDXL2Test


- Create two CPs and list brief and detailed info:

  where the connection points are the IDs of the devices provided through the 'edge-ports' ONOS CLI command

        onos> sdxl2:sdxl2cp-add -ce_mac 00:00:00:00:00:01 SDXL2Test of:0000000000000003/1 Sw3P1 5,15-17
        onos> sdxl2:sdxl2cp-add -ce_mac 00:00:00:00:00:02 SDXL2Test of:0000000000000002/1 Sw2P1 6,19-21

        onos> sdxl2:sdxl2cp-list

        Status		SDXL2 Connection Point
        -----------------------------------------------
        ONLINE		Sw2P1
        ONLINE		Sw3P1

        onos> sdxl2:sdxl2cp Sw2P1

        Status		Connection Point		Name		Vlan IDs		CE Mac Address
        -------------------------------------------------------------------------------------------------------------
        ONLINE		of:0000000000000002/1		Sw2P1		[6, 19, 20, 21]		00:00:00:00:00:02


- Create a (MAC-based) VC connecting the two CPs and get its details:

        onos> sdxl2:sdxl2vc-add SDXL2Test Sw2P1 Sw3P1
        onos> sdxl2:sdxl2vc-list

        Status		Virtual Circuit
        -----------------------------------------------
        ONLINE		Sw2P1-Sw3P1


        onos> sdxl2:sdxl2vc SDXL2Test:Sw2P1-Sw3P1

        Status		Connection Point		Name		Vlan IDs		CE Mac Address
        -------------------------------------------------------------------------------------------------------------
        ONLINE		of:0000000000000002/1		Sw2P1		[6, 19, 20, 21]		00:00:00:00:00:02
        ONLINE		of:0000000000000003/1		Sw3P1		[5, 15, 16, 17]		00:00:00:00:00:01


        Status		Intent
        --------------------------------------------
        ONLINE		SDXL2Test:Sw2P1-Sw3P1,4
        ONLINE		SDXL2Test:Sw3P1-Sw2P1,3
        ONLINE		SDXL2Test:Sw3P1-Sw2P1,1
        ONLINE		SDXL2Test:Sw3P1-Sw2P1,4
        ONLINE		SDXL2Test:Sw3P1-Sw2P1,2
        ONLINE		SDXL2Test:Sw2P1-Sw3P1,1
        ONLINE		SDXL2Test:Sw2P1-Sw3P1,2
        ONLINE		SDXL2Test:Sw2P1-Sw3P1,3


- Create an (MPLS-based) VC connecting two new CPs and get its details:

        onos> cfg set org.onosproject.sdxl2.SdxL2Manager VirtualCircuitType MPLS
        onos> cfg get org.onosproject.sdxl2.SdxL2Manager 
            org.onosproject.sdxl2.SdxL2Manager
                name=VirtualCircuitType, type=string, value=MPLS, defaultValue=MAC, description=Tunnel mechanism for Virtual Circuits

        onos> sdxl2:sdxl2cp-add SDXL2Test of:0000000000000003/2 Sw3P2
        onos> sdxl2:sdxl2cp-add SDXL2Test of:0000000000000002/2 Sw2P2
        onos> sdxl2:sdxl2vc-add SDXL2Test Sw2P2 Sw3P2
        onos> sdxl2:sdxl2vc-list
        onos> sdxl2:sdxl2vc SDXL2Test:Sw2P2-Sw3P2


- Create a (VLAN-based) VC connecting two new CPs and get its details:

        onos> cfg set org.onosproject.sdxl2.SdxL2Manager VirtualCircuitType VLAN
        onos> cfg get org.onosproject.sdxl2.SdxL2Manager
            org.onosproject.sdxl2.SdxL2Manager
                name=VirtualCircuitType, type=string, value=VLAN, defaultValue=MAC, description=Tunnel mechanism for Virtual Circuits

        onos> sdxl2:sdxl2cp-add SDXL2Test of:0000000000000003/2 Sw3P2 10-12
        onos> sdxl2:sdxl2cp-add SDXL2Test of:0000000000000002/2 Sw2P2 10-12
        onos> sdxl2:sdxl2vc-add SDXL2Test Sw2P2 Sw3P2
        onos> sdxl2:sdxl2vc-list
        onos> sdxl2:sdxl2vc SDXL2Test:Sw2P2-Sw3P2


- Remove VCs, CPs, SDX-L2 instances or just everything.
  Deleting a CP that is part of a VC removes the VC as well. Likewise, deleting
  the whole instance removes every resource related to it:

        onos> sdxl2:sdxl2vc-remove SDXL2Test:Sw2P1-Sw3P1
        onos> sdxl2:sdxl2vc-remove SDXL2Test:Sw2P2-Sw3P2

        onos> sdxl2:sdxl2cp-remove Sw2P1
        onos> sdxl2:sdxl2cp-remove Sw3P1
        onos> sdxl2:sdxl2cp-remove Sw2P2
        onos> sdxl2:sdxl2cp-remove Sw3P2

        onos> sdxl2:sdxl2-remove SDXL2Test

        onos> sdxl2:sdxl2-clean

        onos> sdxl2:sdxl2vc-list
        onos> sdxl2:sdxl2cp-list
        onos> sdxl2:sdxl2-list


Tips
=====

- You should define a topology and point to your local/remote ONOS instance. Some examples:

  - Mininet (OVS, CPqD)

        sudo mn --topo linear,3 --controller remote,ip=127.0.0.1 --switch ovsk,protocols=OpenFlow13
        sudo mn --topo linear,3 --controller remote,ip=127.0.0.1 --switch user,protocols=OpenFlow13

  - ONOS/Mininet

        onos-start-network

- You may check the nodes available at ONOS from Mininet:

        onos> edge-ports

- Non-critical errors are described in the log. Attach to ONOS in a new terminal:

        onos> log:tail
