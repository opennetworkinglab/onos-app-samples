![GEANT logo](http://www.geant.org/Style%20Library/Geant/Images/logo.png "http://geant.org")

Software Defined eXchange - L2 (SDX-L2)
==========================

SDX-L2 is an application for ONOS project which can provide layer 2 connectivity between edge ports of a given SDN network.

License
=======

This sofware is licensed under the Apache License, Version 2.0.

Information can be found here:
 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Tips
==============

TBD.

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

SDX-L2 cli commands
=============================

- Create a named SDX:

        sdxl2-add $sdxname


- Delete a named SDX:

        sdxl2-remove $sdxname


- List all the active SDXs:

        sdxl2-list


- Create a named SDX-L2 connection point:

        sdxl2cp-add [-ce_mac] $mac $sdxname $connectionpoint $vlans $sdxcpname


- Remove a named SDX-L2 connection point:

        sdxl2cp-remove $sdxcpname


- List all the active SDX-L2 connection points or all the active SDX connection points related to an SDX:

        sdxl2cps-list [$sdxname]


- Get the information of an SDX-L2 connection point:

        sdxl2cp $sdxcpname

<!---

NOT YET MERGED!!!

- Create a VC between two connection points:

        add-sdxl2vc $sdxname $sdxcp1 $sdxcp2


- Remove a named VC:

        remove-sdxl2vc $sdxcname


- List all the active layer2 virtual circuit or all the layer2 vc related to an SDX:

        list-sdxl2vcs [$sdxname]


- Get the information of a layer 2 virtual circuit:

        sdxl2vc $sdxvcname

SDXL2 GUI
=============================

- TBD

--->