# OVSDBREST ONOS APPLICATION

This application provides a ***minimal*** interface to an ovsdb device by exposing REST APIs.
The API allows to create/delete a bridge, attach/remove ports from an existing bridge, create peer patch and setup GRE tunnels.

## Install
To install the application on a running onos instance run the following steps.

- first of all, if it is not ready installed, you need to install the ovsdb driver provided by onos. On your onos root directory run:

        cd drivers/ovsdb/
        onos-app {onos-address} reinstall target/onos-drivers-ovsdb-1.7.0-SNAPSHOT.oar

- then build the source code of the ovsdbrest application through maven:

        git clone https://github.com/netgroup-polito/onos-applications
        cd onos-applications/ovsdbrest
        mvn clean install

- Finally you can install the application through the command:

        onos-app {onos-address} reinstall target/onos-app-ovsdbrest-1.7.0-SNAPSHOT.oar

(onos-address is the ip-address of onos server, for example 192.168.123.1)


## Activate
After installing the application, you can activate it through the onos cli by typing:

        app activate org.onosproject.ovsdbrest

To check that the app has been activated type log:tail from the onos cli.


## Configure
After activating the application you need to configure the ovsdb node IP. This is done by using the onos Network Configuration system.

- Send a REST request as follows:

    **POST http://{onos-address}:8181/onos/v1/network/configuration/**

    ```json
    {
    	"apps": {
    		"org.onosproject.ovsdbrest": {
    			"ovsdbrest": {
    				"nodes": [
    					{
    						"ovsdbIp": "192.168.123.2",
    						"ovsdbPort": "6632"
    					}
    				]
    			}
    		}
    	}
    }
  ```

Check your ovsdb configuration to get the correct ip and port for the ovsdb node.
The request uses basic HTTP authentication, so you need to provide onos username and password.
To verify that the configuration has been correctly pushed you can type log:tail from the onos cli.
The app will start contacting the ovsdb nodes and you should see some related logs from the onos cli.


## API

- Create/Delete bridge:

    **POST http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}**

    **DELETE http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}**

- Add/Remove a port in a bridge:

    **POST http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}**

    **DELETE http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}**

- Create patch port:

    **POST http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/patch_peer/{peer-port}**

- Create/Delete a GRE tunnel:

    **POST http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/gre/{local-ip}/{remote-ip}/{key}**

    **DELETE http://{onos-address}:8181/onos/ovsdb/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/gre**
