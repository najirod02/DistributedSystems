# Distributed Systems

This repository contains the proposed solutions of the laboratoy's exercises of the course distributed systems followed during the a.y 2024-205.

## List of contents
Here you can find the list of all proposed lab exercises:

- __Causal delivery__<br/>
    A simple chat where different chatters exchange messages on a specific topic. The problem is to make sure that we deliver the messages that belong to the same topic in the correct order while messages from different topics can be delivered in any order.

- __Snapshot exercise__<br/>
    A bank system where the banks make transactions. The problem is to make sure that whenever a central bank initialize the snaphsot, the total amount of money in each bank and in trnasit are not lost meaning that the toal amount of money in the system has to be the same.
    
    `NOTE`: In this case it is suggested to run the code with `gradle run | tee <name_file>.log`. More details in the next point.

- __Snapshot check__<br/>
    A simple java class that automatically takes in input the log file generated from the snapshot exeercise and sum ups all the money in transit. This simplify the verification phase that is, no money are lost during the snaphsot.

# Authors

Dorijan Di Zepp dorijan.dizepp@studenti.unitn.it