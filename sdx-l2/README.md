![GEANT logo](http://www.geant.org/Style%20Library/Geant/Images/logo.png "http://geant.org")

Software Defined eXchange - L2 (SDX-L2)
==========================

SDX-L2 is an application for ONOS project which can provide layer 2 connectivity between edge ports of a given SDN network.


License
=======

This sofware is licensed under the Apache License, Version 2.0.

Information can be found here:
 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).


SDX-L2 dependencies
=============================

SDX-L2 implements its own ARP/NDP handler, it is important to disable the ONOS ARP/NDP handler.


SDX-L2 installation
=============================

- Compile the project:

        mci

    or to skip unit tests:

        mci -Dmaven.test.skip=true

- Install in your ONOS deployment:

        onos-app $OC1 install target/onos-app-sdx-l2-1.7.0-SNAPSHOT.oar


SDX-L2 CLI commands
=============================

General
-----------------------------

- Create a named SDX:

        sdxl2-add $sdxname


- Delete a named SDX:

        sdxl2-remove $sdxname


- List all the active SDXs:

        sdxl2-list


Connection Points (CP)
-----------------------------

- Create a named SDX-L2 CP:

        sdxl2cp-add [-ce_mac $mac] $sdxname $connectionpoint $sdxcpname [$vlans]

  where $vlans can define:
    - explicit VLANs, ranges or a combination of both: 5,10-15
    - all port: -1 (or just ignore)

 *note that CPs must have same number of VLANs in order to establish a VC*

- Remove a named SDX-L2 CP:

        sdxl2cp-remove $sdxcpname


- Get the information of an SDX-L2 CP:

        sdxl2cp $sdxcpname


- List all active SDX-L2 CPs or all active SDX CPs related to an SDX:

        sdxl2cp-list [$sdxname]


Virtual Circuits (VC)
-----------------------------

- Create a VC between two CPs:

        sdxl2vc-add $sdxname $sdxcp1 $sdxcp2


- Remove a named VC:

        sdxl2vc-remove $sdxcname


- Get the information of a L2 VC:

        sdxl2vc $sdxvcname


- List all active L2 VCs or all L2 VCs related to an SDX:

        sdxl2vc-list [$sdxname]


SDX-L2 usage examples
=============================

- Create a named SDX-L2:

        onos> sdxl2:sdxl2-add SDXL2Test
        onos> sdxl2:sdxl2-list

        SDXL2
        --------------
        SDXL2Test


- Create two CPs and list brief and detailed info:

  where the connection points are the IDs of the devices provided through the 'edge-ports' ONOS CLI command

        onos> sdxl2:sdxl2cp-add -ce_mac 00:00:00:00:00:01 SDXL2Test of:0000000000000003/2 Sw3P2 5,15-17
        onos> sdxl2:sdxl2cp-add -ce_mac 00:00:00:00:00:02 SDXL2Test of:0000000000000002/1 Sw2P1 6,19-21

        onos> sdxl2:sdxl2cp-list

        Status		SDXL2 Connection Point
        -----------------------------------------------
        ONLINE		Sw2P1
        ONLINE		Sw3P2

        onos> sdxl2:sdxl2cp Sw2P1

        Status		Connection Point		Name		Vlan IDs		CE Mac Address
        -------------------------------------------------------------------------------------------------------------
        ONLINE		of:0000000000000002/1		Sw2P1		[6, 19, 20, 21]		00:00:00:00:00:02


- Create a VC connecting the two CPs and get its details:

        onos> sdxl2:sdxl2vc-add SDXL2Test Sw2P1 Sw3P2
        onos> sdxl2:sdxl2vc-list

        Status		Virtual Circuit
        -----------------------------------------------
        ONLINE		Sw2P1-Sw3P2


        onos> sdxl2:sdxl2vc SDXL2Test:Sw2P1-Sw3P2

        Status		Connection Point		Name		Vlan IDs		CE Mac Address
        -------------------------------------------------------------------------------------------------------------
        ONLINE		of:0000000000000002/1		Sw2P1		[6, 19, 20, 21]		00:00:00:00:00:02
        ONLINE		of:0000000000000003/2		Sw3P2		[5, 15, 16, 17]		00:00:00:00:00:01


        Status		Intent
        --------------------------------------------
        ONLINE		SDXL2Test:Sw2P1-Sw3P2,4
        ONLINE		SDXL2Test:Sw3P2-Sw2P1,3
        ONLINE		SDXL2Test:Sw3P2-Sw2P1,1
        ONLINE		SDXL2Test:Sw3P2-Sw2P1,4
        ONLINE		SDXL2Test:Sw3P2-Sw2P1,2
        ONLINE		SDXL2Test:Sw2P1-Sw3P2,1
        ONLINE		SDXL2Test:Sw2P1-Sw3P2,2
        ONLINE		SDXL2Test:Sw2P1-Sw3P2,3


- Remove VC and check it exists no longer:

        onos> sdxl2:sdxl2vc-remove SDXL2Test:Sw2P1-Sw3P2
        onos> sdxl2:sdxl2vc-list


- Remove CPs and check they exist no longer:

        onos> sdxl2:sdxl2cp-remove Sw2P1
        onos> sdxl2:sdxl2cp-remove Sw3P2
        onos> sdxl2:sdxl2cp-list

- Remove SDX-L2 and check it does not exist anymore:

        onos> sdxl2:sdxl2-list SDXL2Test
        onos> sdxl2:sdxl2-list


Tips
=============================

- You should define a topology and point to your local/remote ONOS instance. Some examples:

  - Mininet
        sudo mn --controller remote,ip=127.0.0.1 --tree,2,2
  - ONOS/Mininet
        onos-start-network

- You may check the nodes available at ONOS from Mininet:

        onos> edge-ports
