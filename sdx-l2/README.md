![GEANT logo](http://www.geant.org/Style%20Library/Geant/Images/logo.png "http://geant.org")

Software Defined eXchange - L2
==========================

SDXL2 is an application for ONOS project which can provide layer 2 connectivity between edge ports of a given SDN network.

License
=======

This sofware is licensed under the Apache License, Version 2.0.

Information can be found here:
 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Tips
==============

TBD.

SDXL2 dependencies
=============================

TBD.

SDXL2 installation
=============================

- Compile the project:

        mci

    or to skip unit tests:

        mci -Dmaven.test.skip=true

- Install in your ONOS deployment:

        onos-app $OC1 install target/SDXL2-1.0-SNAPSHOT.oar

SDXL2 cli commands
=============================

- Create a named SDX:

        add-sdxl2 $sdxname


- Delete a named SDX:

        remove-sdxl2 $sdxname


- List all the active SDXs:

        list-sdxl2s


- Create a named SDX connection point:

        add-sdxl2cp [-ce_mac] $mac $sdxname $connectionpoint $vlans $sdxcpname


- Remove a named SDX connection point:

        remove-sdxl2cp $sdxcpname


- List all the active SDX connection points or all the active SDX connection points related to an SDX:

        list-sdxl2cps [$sdxname]


- Get the information of an SDX connection point:

        sdxl2cp $sdxcpname


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