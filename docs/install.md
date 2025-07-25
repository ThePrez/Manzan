# Install

This document will outline how to install Manzan on to IBM i. Manzan is installed through a pase shell. We do not recommend using `qsh` or `qp2term`, and instead using SSH and a bash shell.

## Which Java to Use

Use a Java version provided by IBM, which is at least version 8. When running the `java -version` command, the output should contain the string `IBM`. Ex. `IBM Semeru Runtime Certified Edition`. Otherwise, Manzan may not function properly.

## An Important Note on CCSID's

Manzan has support for all CCSIDs on IBM i. What this means is that when you decide to watch a message queue, the CCSID of that queue can be anything. Most of the time this is fine, but you should keep in mind the following:

 If the CCSID of the message queue you want to watch is `65535`, Manzan will assume the source is encoded using the **TGTCCSID** of the Manzan handler. This will be the CCSID of your user profile at the time you installed it. For this reason, it is better to **specify a CCSID on your message queues**.


## Install from GitHub

1. Make a "download" directory on IBM i by running the following from an SSH terminal:

    ```sh
    mkdir -p /opt/download
    ```

2. Download the latest binary release from the [GitHub releases](https://github.com/ThePrez/Manzan/releases) page (the file name will look like `manzan-installer-v#.jar`).

3. If you did not download to IBM i directly, transfer the `.jar` file to IBM i using technique of your choice.

4. Run the installer using:

    ```sh
    java -jar <name of jar file>
    ```

For instance, to install version `0.0.9`, the steps from an IBM i (using open source `wget`) would look like:

```bash
mkdir -p /opt/download
cd /opt/download
wget https://github.com/ThePrez/Manzan/releases/download/v0.0.9/manzan-installer-v0.0.9.jar
java -jar manzan-installer-v0.0.9.jar
```

Note: There is currently a bug in the installer where it might say `ERROR: Error running command` after the install is completed.
This error message can be ignored.

## Configuration

Before starting Manzan, several configuration files (`.ini` extension) should be configured in `/QOpenSys/etc/manzan/`. For more details, refer to the [configuration](/config/index.md) page.

## Startup

### Option 1: Service Commander

It is recommended to use [Service Commander](https://theprez.github.io/ServiceCommander-IBMi/#service-commander-for-ibm-i) to start/stop Manzan (or to have Manzan autostart). You can install it using:

```sh
yum install service-commander
```

Once installed, you can use it to start, stop, and check the status of Manzan:

```sh
## Start
sc start manzan

## Check it's running
sc check manzan

## Stop
sc stop manzan

## Check it's stopped
sc check manzan
```

### Option 2: Manual Startup

Manzan can be manually started using:

```sh
/opt/manzan/bin/manzan
```

## Next Steps

* Read more about the [configuration files](/config/index.md)
* Build your very first [handler](config/examples/file.md)