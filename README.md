# Dronazon

[![Build and Test](https://github.com/mrkct/dronazon/actions/workflows/build-and-test.yaml/badge.svg)](https://github.com/mrkct/dronazon/actions/workflows/build-and-test.yaml)

Dronazon is a distributed system made of drones that simulate the delivery of packages in a city. This project was made 
as part of the 'Distributed and Pervasive Systems' course in 2021 at UNIMI. You can read the original PDF (in italian) 
explaining the assignment at [assets/Progetto_SDP_2021.pdf](assets/Progetto_SDP_2021.pdf) and you can see a small 
presentation of some handled edge cases at [assets/Presentazione.pdf](assets/Presentazione.pdf). 
This project was completed in September 2021.

The basic gist of the structure is that we have the following components in the system:
- A swarm of drones that communicate with each other using gRpc. The drones elect a leader that will assign the orders 
  to deliver
- A REST server that handles collecting statistics about the city pollution, deliveries made, battery levels etc
- An MQTT broker through which orders are communicated to the drones

## Running this project - the quick way

Install Gradle >=6.8, and an MQTT broker such as Mosquitto. Run the broker at localhost, port 8000 without 
authentication. Open 3 terminal windows and run these commands, one for each window:
- `gradle runOrderGenerator`
- `gradle runAdminServer`
- 'gradle runClient'

Open some more terminals, one for each drone you want in the system, and run in every one of the `gradle runDrone`

## Running this project - the detailed way

**Important**: Set your JDK to version 1.8: this project uses an outdated version of Jersey and newer versions of java 
simply don't work.  

After doing that install an MQTT broker on your machine: by default all services expect the broker 
to be at `localhost:8000` and require no authentication. 
After doing this you can use Gradle to run the following services:

  - `gradle runOrderGenerator` A service that simulates users making purchases. This generates a random order every 
    5 seconds and sends it to the MQTT broker
  - `gradle runAdminServer` Runs the REST server that collects stats. 
    Note that drones expect this to be running as soon as they start
  - `gradle runDrone` Runs a drone using a random port and expecting the REST server to be at `localhost:1337`. 
    Note that you can change these settings by passing arguments like this: 
    `gradle runDrone --args='<droneId> <droneGrpcPort> <serverHost> <serverPort>'`
  - `gradle runClient` A command line interface program to query the server for stats
